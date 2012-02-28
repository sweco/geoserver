package org.geoserver.script;

import javax.script.ScriptEngine;

import org.geoserver.test.GeoServerTestSupport;

public class ScriptManagerTest extends GeoServerTestSupport {

    ScriptManager scriptMgr;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        scriptMgr = new ScriptManager(getDataDirectory());
    }

    public void testGetEngineManager() throws Exception {
        ScriptEngine engine = scriptMgr.getEngineManager().getEngineByName("JavaScript");
        engine.eval("print ('Hello');");
    }
}
