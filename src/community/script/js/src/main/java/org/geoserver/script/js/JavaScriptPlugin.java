package org.geoserver.script.js;

import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.js.engine.RhinoScriptEngineFactory;

public class JavaScriptPlugin extends ScriptPlugin {

    private static final long serialVersionUID = 1L;

    protected JavaScriptPlugin() {
        super("js", RhinoScriptEngineFactory.class);
    }
    
    @Override
    public void init(ScriptManager scriptMgr) throws Exception {
        super.init(scriptMgr);
        scriptMgr.getEngineManager().registerEngineExtension("js", new RhinoScriptEngineFactory());
    }
    @Override
    public String getId() {
        return "javascript";
    }

    @Override
    public String getDisplayName() {
        return "JavaScript";
    }

}
