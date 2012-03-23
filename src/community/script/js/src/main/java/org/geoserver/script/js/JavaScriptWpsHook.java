package org.geoserver.script.js;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.script.wps.WpsHook;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.InternationalString;

public class JavaScriptWpsHook extends WpsHook {

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    public JavaScriptWpsHook(JavaScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getTitle(ScriptEngine engine) throws ScriptException {
        Scriptable process = getProcess(engine);
        Object titleObj = process.get("title", process);
        String title;
        if (titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            LOGGER.warning("Process missing required title.");
            // TODO provide process name
            title = "Untitled";
        }
        return title;
    }
    
    @Override
    public String getDescription(ScriptEngine engine) throws ScriptException {
        Scriptable process = getProcess(engine);
        Object descriptionObj = process.get("description", process);
        String description = null;
        if (descriptionObj instanceof String) {
            description = (String) descriptionObj;
        }
        return description;
    }

    @Override
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        Scriptable process = getProcess(engine);
        Scriptable inputs = (Scriptable) process.get("inputs", process);
        return getParametersFromObject(inputs);
    }

    @Override
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {
        Scriptable process = getProcess(engine);
        Scriptable outputs = (Scriptable) process.get("outputs", process);
        return getParametersFromObject(outputs);
    }

    private Parameter<?> getParameterFromField(String id, Scriptable field) {
        Object _field = field.get("_field", field);
        AttributeDescriptor descriptor = (AttributeDescriptor) ((Wrapper) _field).unwrap();

        String title = null;
        Object titleObj = field.get("title", field);
        if (titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            // TODO: make process identifier available
            LOGGER.warning("Field '" + id + "' from process missing required title.");
            title = id;
        }
        
        InternationalString descriptionObj = descriptor.getType().getDescription();
        String description;
        if (descriptionObj != null) {
            description = descriptionObj.toString();
        } else {
            // spec says optional, but required for Parameter
            description = id;
        }
        
        @SuppressWarnings("unchecked")
        Parameter<?> parameter = new Parameter<Object>(
            id,
            (Class<Object>) descriptor.getType().getBinding(),
            title,
            description
        );
        return parameter;
    }
    
    private Map<String, Parameter<?>> getParametersFromObject(Scriptable obj) {
        Map<String, Parameter<?>> parameters = new HashMap<String, Parameter<?>>();
        for (Object key : obj.getIds()) {
            String id = (String) key;
            Scriptable field = (Scriptable) obj.get(id, obj);
            parameters.put(id, getParameterFromField(id, field));
        }
        return parameters;
    }
    
    @Override
    public Map<String, Object> run(Map<String, Object> input, ScriptEngine engine)
            throws ScriptException {

        Map<String,Object> results = null;
        
        JavaScriptPlugin jsPlugin = (JavaScriptPlugin) plugin;

        Scriptable exports = jsPlugin.require("geoserver/process");
        Object executeWrapperObj = exports.get("execute", exports);
        Function executeWrapper;
        if (executeWrapperObj instanceof Function) {
            executeWrapper = (Function) executeWrapperObj;
        } else {
            throw new ScriptException(
                    "Can't find execute method in geoserver/process module.");
        }
        
        Object[] args = {getProcess(engine), jsPlugin.mapToJsObject(input)};
        Object result = jsPlugin.callFunction(executeWrapper, args);
        results = jsPlugin.jsObjectToMap((Scriptable)result);

        return results;
    }

    private Scriptable getProcess(ScriptEngine engine) {
        Object exportsObj = engine.get("exports");
        Scriptable exports = null;
        if (exportsObj instanceof Scriptable) {
            exports = (Scriptable) exportsObj;
        } else {
            throw new RuntimeException("Couldn't get exports for process.");
        }
        Object processObj = exports.get("process", exports);
        Scriptable process = null;
        if (processObj instanceof Scriptable) {
            process = (Scriptable) processObj;
        } else {
            throw new RuntimeException("Missing 'process' export.");
        }
        return process;
    }

}
