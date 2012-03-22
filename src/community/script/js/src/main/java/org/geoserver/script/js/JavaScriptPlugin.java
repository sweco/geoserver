package org.geoserver.script.js;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.js.engine.RhinoScriptEngine;
import org.geoserver.script.js.engine.RhinoScriptEngineFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.tools.shell.Global;

public class JavaScriptPlugin extends ScriptPlugin {

    private static final long serialVersionUID = 1L;
    
    private File libRoot;
    private Global sharedGlobal;
    private RequireBuilder requireBuilder;

    protected JavaScriptPlugin() {
        super("js", RhinoScriptEngineFactory.class);
    }
    
    @Override
    public String getId() {
        return "javascript";
    }

    @Override
    public String getDisplayName() {
        return "JavaScript";
    }
    
    @Override
    public void init(ScriptManager scriptMgr) throws Exception {
        super.init(scriptMgr);
        scriptMgr.getEngineManager().registerEngineExtension("js", new RhinoScriptEngineFactory());
        libRoot = scriptMgr.getLibRoot("js");
        sharedGlobal = new Global();
        Context cx = RhinoScriptEngine.enterContext();
        try {
            sharedGlobal.initStandardObjects(cx, true);
        } finally {
            Context.exit();
        }
    }
    
    @Override
    public void initScriptEngine(ScriptEngine engine) {
        super.initScriptEngine(engine);
        Require require = createRequire();
        Bindings scope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        scope.put("require", require);
    }
    
    /**
     * Create a new require function using the shared global.
     * @return
     */
    private Require createRequire() {
        Require require = null;
        RequireBuilder rb = getRequireBuilder();
        Context cx = RhinoScriptEngine.enterContext();
        try {
            require = rb.createRequire(cx, sharedGlobal);
        } finally {
            Context.exit();
        }
        return require;
    }

    /**
     * Creates and returns a shared require builder.  This allows loaded
     * modules to be cached.  The require builder is constructed with a module
     * provider that reloads modules only when they have changed on disk (with
     * a 60 second interval).  This require builder will be configured with
     * the module paths returned by {@link getModulePahts()}.
     * 
     * @return a shared require builder
     */
    private RequireBuilder getRequireBuilder() {
        if (requireBuilder == null) {
            synchronized (this) {
                if (requireBuilder == null) {
                    requireBuilder = new RequireBuilder();
                    requireBuilder.setSandboxed(false);
                    List<String> modulePaths = getModulePaths();
                    List<URI> uris = new ArrayList<URI>();
                    if (modulePaths != null) {
                        for (String path : modulePaths) {
                            try {
                                URI uri = new URI(path);
                                if (!uri.isAbsolute()) {
                                    // call resolve("") to canonify the path
                                    uri = new File(path).toURI().resolve("");
                                }
                                if (!uri.toString().endsWith("/")) {
                                    // make sure URI always terminates with slash to
                                    // avoid loading from unintended locations
                                    uri = new URI(uri + "/");
                                }
                                uris.add(uri);
                            } catch (URISyntaxException usx) {
                                throw new RuntimeException(usx);
                            }
                        }
                    }
                    requireBuilder.setModuleScriptProvider(
                            new SoftCachingModuleScriptProvider(
                                    new UrlModuleSourceProvider(uris, null)));
                }
            }
        }
        return requireBuilder;
    }

    /**
     * Evaluate a JavaScript module to get its exports.
     * 
     * @param locator
     * @return the exports object from the module
     */
    public Scriptable require(String locator) {
        Scriptable exports = null;
        Require require = createRequire();
        Context cx = RhinoScriptEngine.enterContext();
        try {
            Object exportsObj = require.call(
                    cx, sharedGlobal, sharedGlobal, new String[] {locator});
            if (exportsObj instanceof Scriptable) {
                exports = (Scriptable) exportsObj;
            } else {
                throw new RuntimeException(
                        "Failed to locate exports in module: " + locator);
            }
        } finally { 
            Context.exit();
        }
        return exports;
    }

    /**
     * Returns a list of paths to JavaScript modules.  This includes modules
     * bundled with this extension in addition to modules in the "scripts/lib/js"
     * directory of the data dir.
     */
    public List<String> getModulePaths() {
        // GeoScript modules
        URL geoscriptModuleUrl = getClass().getClassLoader().getResource("geoscript.js");
        String geoscriptModulePath;
        try {
            geoscriptModulePath = geoscriptModuleUrl.toURI().toString();
            geoscriptModulePath = geoscriptModulePath.substring(0, 
                    geoscriptModulePath.lastIndexOf("geoscript.js"));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating GeoScript module path.", e);
        }
        
        // GeoServer modules
        URL geoserverModuleUrl = getClass().getResource("modules");
        String geoserverModulePath;
        try {
            geoserverModulePath = geoserverModuleUrl.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating GeoServer module path.", e);
        }

        // User modules
        String userModulePath = libRoot.toURI().toString();

        return (List<String>) Arrays.asList(geoscriptModulePath, geoserverModulePath, userModulePath);
    }

}
