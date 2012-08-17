package org.geoserver.script.js;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.js.engine.RhinoScriptEngine;
import org.geoserver.script.js.engine.RhinoScriptEngineFactory;
import org.geoserver.script.wps.WpsHook;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.tools.shell.Global;

public class JavaScriptPlugin extends ScriptPlugin {

    private static final long serialVersionUID = 1L;
    private Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    private File libRoot;
    public Global global;
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
        global = new Global();
        Context cx = RhinoScriptEngine.enterContext();
        try {
            global.initStandardObjects(cx, true);
        } finally {
            Context.exit();
        }
    }
    
    @Override
    public void initScriptEngine(ScriptEngine engine) {
        super.initScriptEngine(engine);
        Require require = createRequire();
        Bindings scope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        scope.put("require", require);
        scope.put("LOGGER", LOGGER);
        Context cx = RhinoScriptEngine.enterContext();
        try {
            scope.put("exports", cx.newObject(global));
        } finally {
            Context.exit();
        }
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
            require = rb.createRequire(cx, global);
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
                    cx, global, global, new String[] {locator});
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
    
    public Object callFunction(Function function, Object[] args) {
        Context cx = RhinoScriptEngine.enterContext();
        Object result = null;
        try {
            result = function.call(cx, global, global, args);
        } finally {
            Context.exit();
        }
        return result;
    }

    public Scriptable mapToJsObject(Map<String,Object> map) {
        Context cx = RhinoScriptEngine.enterContext();
        Scriptable obj;
        try {
            obj = cx.newObject(global);
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                obj.put(entry.getKey(), 
                        obj, 
                        Context.javaToJS(entry.getValue(), global));
            }
        } finally { 
            Context.exit();
        }
        return obj;
    }

    public Map<String, Object> jsObjectToMap(Scriptable obj) {
        Object[] ids = obj.getIds();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Object idObj : ids) {
            String id = (String)idObj;
            Object value = obj.get(id, obj);

            if (value instanceof Wrapper) {
                map.put(id, ((Wrapper)value).unwrap());
            } else if (value instanceof Function) {
                // ignore functions?
            } else {
                map.put(id, value);
            }
        }
        return map;
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
    
    @Override
    public WpsHook createWpsHook() {
        return new JavaScriptWpsHook(this);
    }
    
    @Override
    public FunctionHook createFunctionHook() {
        return new JavaScriptFunctionHook(this);
    }
    
    @Override
    public AppHook createAppHook() {
        return new JavaScriptAppHook(this);
    }

}
