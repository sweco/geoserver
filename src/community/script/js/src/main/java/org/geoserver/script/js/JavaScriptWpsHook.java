package org.geoserver.script.js;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.script.wps.WpsHook;
import org.geotools.data.Parameter;

public class JavaScriptWpsHook extends WpsHook {

    public JavaScriptWpsHook(JavaScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getTitle(ScriptEngine engine) throws ScriptException {
        return null;
    }

    @Override
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> run(Map<String, Object> input, ScriptEngine engine)
            throws ScriptException {
        // TODO Auto-generated method stub
        return null;
    }

}
