package org.geoserver.script.js;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.script.ScriptPlugin;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

public class JavaScriptPluginTest extends ScriptIntTestSupport {
    
    JavaScriptPlugin getPlugin() {
        JavaScriptPlugin plugin = null;
        List<ScriptPlugin> plugins = getScriptManager().getPlugins();
        for (ScriptPlugin candidate : plugins) {
            if (candidate instanceof JavaScriptPlugin) {
                plugin = (JavaScriptPlugin) candidate;
                break;
            }
        }
        return plugin;
    }

    /**
     * Test method for {@link org.geoserver.script.js.JavaScriptPlugin#getModulePaths()}.
     * @throws URISyntaxException 
     */
    public void testGetModulePaths() throws URISyntaxException {
        JavaScriptPlugin plugin = getPlugin();
        List<String> paths = plugin.getModulePaths();
        assertTrue("got some paths", paths.size() > 0);
        for (String path : paths) {
            URI uri = new URI(path);
            assertTrue("absolute URI", uri.isAbsolute());
            String scheme = uri.getScheme();
            if (scheme.equals("file")) {
                File file = new File(uri.getPath());
                assertTrue("path is directory", file.isDirectory());
                assertTrue("directory exists", file.exists());
            }
        }
    }

    /**
     * Test method for {@link org.geoserver.geoscript.javascript.JavaScriptModules#require()}.
     */
    public void testRequireGeoScript() {
        JavaScriptPlugin plugin = getPlugin();
        Scriptable exports = plugin.require("geoscript");
        Object geomObj = exports.get("geom", exports);
        assertTrue("geom in exports", geomObj instanceof Scriptable);
        Object projObj = exports.get("proj", exports);
        assertTrue("proj in exports", projObj instanceof Scriptable);
    }

    /**
     * Test method for {@link org.geoserver.geoscript.javascript.JavaScriptModules#require()}.
     */
    public void testRequireGeoServer() {
        JavaScriptPlugin plugin = getPlugin();
        Scriptable exports = plugin.require("geoserver");
        Object catalogObj = exports.get("catalog", exports);
        assertTrue("catalog in exports", catalogObj instanceof Scriptable);
        Object processObj = exports.get("process", exports);
        assertTrue("process in exports", processObj instanceof Scriptable);
    }

    /**
     * Test for catalog access through the geoserver.js module.
     * @throws ScriptException 
     */
    public void testGeoServerCatalogNamespaces() throws ScriptException {
        
        ScriptEngine engine = getScriptManager().createNewEngine("js");

        // get list of namespaces in catalog
        Object result = engine.eval("require('geoserver/catalog').namespaces");
        assertTrue(result instanceof NativeArray);
        NativeArray array = (NativeArray) result;
        assertEquals("correct number of namespaces", 5, array.getLength());
        Scriptable obj = (Scriptable) array.get(0);
        assertEquals("first namespace alias", "cite", obj.get("alias", obj));
        assertEquals("first namespace uri", "http://www.opengis.net/cite", obj.get("uri", obj));
    }

}
