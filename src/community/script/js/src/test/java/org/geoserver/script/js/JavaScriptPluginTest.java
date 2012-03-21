package org.geoserver.script.js;

import javax.script.ScriptEngine;

import org.geoserver.script.ScriptIntTestSupport;

public class JavaScriptPluginTest extends ScriptIntTestSupport {
    
    public void testSanity() throws Exception {
        ScriptEngine engine = getScriptManager().createNewEngine("js");
        engine.eval("print('hello world');");
    }

}
