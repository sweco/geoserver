package org.geoserver.script.js;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.js.engine.RhinoScriptEngine;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;

public class JavaScriptAppHook extends AppHook {

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    public JavaScriptAppHook(ScriptPlugin plugin) {
        super(plugin);
    }
    
    @Override
    public void run(Request request, Response response, ScriptEngine engine)
            throws ScriptException, IOException {
        Invocable invocable = (Invocable) engine;
        Object exportsObj = engine.get("exports");
        Scriptable exports = null;
        if (exportsObj instanceof Scriptable) {
            exports = (Scriptable) exportsObj;
        } else {
            throw new ScriptException("Couldn't get exports for app.");
        }
        Context cx = RhinoScriptEngine.enterContext();
        Scriptable jsgiRequest = null;
        try {
            jsgiRequest = getJsgiRequest(request, response, cx);
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        Object jsgiResponse = null;
        try {
            jsgiResponse = invocable.invokeMethod(exports, "app", jsgiRequest);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
        handleResponse(jsgiResponse, response);
    }
    
    void handleResponse(Object jsgiResponse, Response response) throws ScriptException {
        JavaScriptPlugin jsPlugin = (JavaScriptPlugin) plugin;

        if (!(jsgiResponse instanceof Scriptable)) {
            throw new ScriptException("bad return from jsgi app");
        } else {
            Scriptable jsgi = (Scriptable) jsgiResponse;

            // set response status
            int status  = 200;
            Object statusObj = jsgi.get("status", jsgi);
            if (statusObj instanceof Integer) {
                status = (Integer) statusObj;
            }
            response.setStatus(new Status(status));
            
            // set response headers
            Object headersObj = jsgi.get("headers", jsgi);
            Form responseHeaders = (Form) response.getAttributes().get("org.restlet.http.headers");
            if (responseHeaders == null) {
                responseHeaders = new Form();
                response.getAttributes().put("org.restlet.http.headers", responseHeaders);
            }
            if (headersObj instanceof ScriptableObject) {
                ScriptableObject headers = (ScriptableObject) headersObj;
                for (Object id : headers.getIds()) {
                    String name = id.toString();
                    String value = headers.get(name, headers).toString();
                    responseHeaders.add(name, value);
                }
            }
            
            // TODO: deal with body
            Object bodyObj = jsgi.get("body", jsgi);
            if (bodyObj instanceof Scriptable) {
                Scriptable body = (Scriptable) bodyObj;
                Object forEachObj = body.get("forEach", body);
                if (forEachObj instanceof Function) {
                    Function forEach = (Function) forEachObj;
                    Object[] args = {"writer"};
                    Context cx = RhinoScriptEngine.enterContext();
                    try {
                        forEach.call(cx, jsPlugin.global, body, args);
                    } finally {
                        Context.exit();
                    }
                    MediaType mediaType;
                    String type = responseHeaders.getFirstValue("content-type", true);
                    if (type == null) {
                        mediaType = MediaType.TEXT_PLAIN;
                    } else {
                        mediaType = new MediaType(type);
                    }
                    response.setEntity(new OutputRepresentation(mediaType) {
                        
                        @Override
                        public void write(OutputStream outputStream) throws IOException {
                            outputStream.write(("hello world").getBytes());
                        }
                        
                    });
                }
            }
            
        }
        
    }

    /**
     * Generates a JavaScript object that conforms to the JSGI spec.
     * http://wiki.commonjs.org/wiki/JSGI/Level0/A/Draft2
     * 
     * @param request
     * @param cx
     * @return
     */
    Scriptable getJsgiRequest(Request request, Response response, Context cx) {

        JavaScriptPlugin jsPlugin = (JavaScriptPlugin) plugin;
        Scriptable jsgiRequest = cx.newObject(jsPlugin.global);
        Reference ref = request.getResourceRef();

        jsgiRequest.put("method", jsgiRequest, request.getMethod().toString());

        jsgiRequest.put("scriptName", jsgiRequest, ref.getLastSegment());

        List<String> seg = new ArrayList<String>(ref.getSegments().subList(4, ref.getSegments().size()));
        seg.add(0, "");
        jsgiRequest.put("pathInfo", jsgiRequest, StringUtils.join(seg.toArray(), "/"));
        
        jsgiRequest.put("queryString", jsgiRequest, ref.getQuery());
        
        jsgiRequest.put("host", jsgiRequest, ref.getHostDomain());
        
        jsgiRequest.put("port", jsgiRequest, ref.getHostPort());
        
        jsgiRequest.put("scheme", jsgiRequest, ref.getScheme());

        try {
            jsgiRequest.put("input", jsgiRequest, Context.javaToJS(request.getEntity().getStream(), jsPlugin.global));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }

        Scriptable headers = cx.newObject(jsPlugin.global);
        Form requestHeaders = (Form) request.getAttributes().get("org.restlet.http.headers");
        for (String name : requestHeaders.getNames()) {
            String value = requestHeaders.getFirstValue(name, true);
            headers.put(name.toLowerCase(), headers, value);
        }
        jsgiRequest.put("headers", jsgiRequest, headers);

        // create jsgi object
        Scriptable jsgiObject = cx.newObject(jsPlugin.global);
        int readonly = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
        Scriptable version = cx.newArray(jsPlugin.global, new Object[] {Integer.valueOf(0), Integer.valueOf(3)});
        ScriptableObject.defineProperty(jsgiObject, "version", version, readonly);
        ScriptableObject.defineProperty(jsgiObject, "multithread", Boolean.TRUE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "multiprocess", Boolean.FALSE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "async", Boolean.TRUE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "runOnce", Boolean.FALSE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "cgi", Boolean.FALSE, readonly);
        jsgiRequest.put("jsgi", jsgiRequest, jsgiObject);

        // create env object
        Scriptable env = cx.newObject(jsPlugin.global);
        env.put("servletRequest", env, Context.javaToJS(request, jsPlugin.global));
        env.put("servletResponse", env, Context.javaToJS(response, jsPlugin.global));
        jsgiRequest.put("env", jsgiRequest, env);
        
        return jsgiRequest;
    }

}
