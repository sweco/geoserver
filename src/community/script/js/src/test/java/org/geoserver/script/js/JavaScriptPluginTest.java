package org.geoserver.script.js;

import javax.script.ScriptEngine;

import org.geoserver.script.ScriptTestSupport;

public class JavaScriptPluginTest extends ScriptTestSupport {
    
    public void testSanity() throws Exception {
        ScriptEngine engine = scriptMgr.createNewEngine("js");
        engine.eval("print('hello sane world');");
    }

}
