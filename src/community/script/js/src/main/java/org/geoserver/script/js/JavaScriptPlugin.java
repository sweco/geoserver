package org.geoserver.script.js;

import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.js.engine.javascript.RhinoScriptEngineFactory;

public class JavaScriptPlugin extends ScriptPlugin {

    private static final long serialVersionUID = 1L;

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

}
