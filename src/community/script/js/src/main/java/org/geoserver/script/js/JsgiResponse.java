package org.geoserver.script.js;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import javax.script.ScriptException;

import org.geoserver.script.js.engine.RhinoScriptEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;

public class JsgiResponse {

    private int status = 200;
    private ScriptableObject headers;
    private Scriptable body;
    private Function forEach;
    
    static ThreadLocal<OutputStream> OUTPUT_STREAM = new ThreadLocal<OutputStream>();
    
    public JsgiResponse(Scriptable obj) {

        // extract status
        Object statusObj = obj.get("status", obj);
        if (statusObj instanceof Integer) {
            status = (Integer) statusObj;
        }
        
        // extract headers
        Object headersObj = obj.get("headers", obj);
        if (headersObj instanceof ScriptableObject) {
            headers = (ScriptableObject) headersObj;
        }
        
        // extract body
        Object bodyObj = obj.get("body", obj);
        if (bodyObj instanceof Scriptable) {
            body = (Scriptable) bodyObj;
            Object forEachObj = body.get("forEach", body);
            if (forEachObj instanceof Function) {
                forEach = (Function) forEachObj;
            }
        }
        
        if (forEach == null) {
            throw new RuntimeException("JSGI app must return an object with a 'body' member that has a 'forEach' function.");
        }

    }
    
    public void commit(Response response, final Scriptable scope) throws SecurityException, NoSuchMethodException {

        // set response status
        response.setStatus(new Status(status));
        
        // set response headers
        Form responseHeaders = (Form) response.getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            response.getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        if (headers != null) {
            for (Object id : headers.getIds()) {
                String name = id.toString();
                String value = headers.get(name, headers).toString();
                responseHeaders.add(name, value);
            }
        }
        
        // write response body
        MediaType mediaType;
        String type = responseHeaders.getFirstValue("content-type", true);
        if (type == null) {
            mediaType = MediaType.TEXT_PLAIN;
        } else {
            mediaType = new MediaType(type);
        }
        
        Method writeMethod = getClass().getDeclaredMethod("write", Context.class, Scriptable.class, Object[].class, Function.class);
        final FunctionObject writeFunc = new FunctionObject("bodyWriter", writeMethod, scope);
        
        response.setEntity(new OutputRepresentation(mediaType) {
            
            @Override
            public void write(OutputStream outputStream) throws IOException {
                Context cx = RhinoScriptEngine.enterContext();
                Object[] args = {writeFunc};
                OUTPUT_STREAM.set(outputStream);
                try {
                    forEach.call(cx, scope, body, args);
                } finally {
                    Context.exit();
                    outputStream.close();
                    OUTPUT_STREAM.remove();
                }
            }
            
        });
    }
    
    public static Object write(Context cx, Scriptable thisObj, Object[] args, Function func) throws ScriptException {
        String msg = (String) args[0];
        OutputStream outputStream = OUTPUT_STREAM.get();
        try {
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            throw new ScriptException("Failed to write to body.");
        }
        return null;
    }


}
