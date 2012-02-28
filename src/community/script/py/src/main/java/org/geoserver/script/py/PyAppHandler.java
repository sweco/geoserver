/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.script.app.AppHandler;
import org.geotools.util.logging.Logging;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFunction;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

/**
 * Python app handler.
 * 
 * <p>
 * This app handler adapts the incoming request into a WSGI request requiring the app script to 
 * implement a WSGI interface via a function named "app". See 
 * {@linkplain http://en.wikipedia.org/wiki/Web_Server_Gateway_Interface} for more details about 
 * WSGI.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PyAppHandler extends AppHandler {

    static Logger LOGGER = Logging.getLogger(PyAppHandler.class);
    static ThreadLocal<WSGIResponse> RESPONSE = new ThreadLocal<WSGIResponse>();

    @Override
    public void run(Reader script, ScriptEngine engine, Request request, Response response) 
        throws ScriptException {

        engine.eval(script);

        Object obj = engine.get("app");
        if (obj == null) {
            throw new RuntimeException("No 'app' function defined");
        }

        if (!(obj instanceof PyFunction)) {
            throw new RuntimeException("'app must be a function, found a " + obj.toString());
        }

        PyFunction app = (PyFunction) obj;
       
        WSGIResponse wr = new WSGIResponse(); 
        RESPONSE.set(wr);

        Object ret = app.__call__(new PyObject[]{createEnviron(request), createStartResponse()});
        if (ret != null) {

            String contentType = wr.headers.get("content-type");
            if (contentType == null) {
                contentType = "text/plain";
            }

            MediaType mediaType = new MediaType(contentType);
            
            if (ret instanceof PyString) {
                response.setEntity(ret.toString(), mediaType);
            }
            else if (ret instanceof PyList) {
                final PyList list = (PyList) ret;
                response.setEntity(new OutputRepresentation(mediaType) {
                    
                    @Override
                    public void write(OutputStream outputStream) throws IOException {
                        for (Iterator i = list.iterator(); i.hasNext();) {
                            outputStream.write(i.next().toString().getBytes());
                            if (i.hasNext()) {
                                outputStream.write('\n');
                            }
                        }
                    }
                });
            }
            else if (ret instanceof PyIterator) {
                final PyIterator iter = (PyIterator) ret;
                response.setEntity(new OutputRepresentation(mediaType) {

                    @Override
                    public void write(OutputStream outputStream) throws IOException {
                        for (Iterator i = iter.iterator(); i.hasNext();) {
                            outputStream.write(i.next().toString().getBytes());
                            outputStream.write('\n');
                        }
                    }
                });
            }
            else {
                LOGGER.warning( "Unsure how to handle " + ret + ". Resorting to outputing string " +
                    "representation.");
                response.setEntity(ret.toString(), mediaType);
            }
        }
        
        RESPONSE.remove();
    }

    /**
     * Creates the environ object which is a dictionary with the following entries:
     * <pre>
     * REQUEST_METHOD
     *       The HTTP request method, such as "GET" or "POST". This cannot ever be 
     *       an empty string, and so is always required.
     * SCRIPT_NAME
     *       The initial portion of the request URL's "path" that corresponds to the
     *       application object, so that the application knows its virtual "location"
     *       . This may be an empty string, if the application corresponds to the 
     *       "root" of the server.
     * PATH_INFO
     *       The remainder of the request URL's "path", designating the virtual 
     *       "location" of the request's target within the application. This may be
     *        an empty string, if the request URL targets the application root and 
     *       does not have a trailing slash.
     * QUERY_STRING
     *       The portion of the request URL that follows the "?", if any. May be 
     *       empty or absent.
     * CONTENT_TYPE
     *       The contents of any Content-Type fields in the HTTP request. May be 
     *       empty or absent.
     * CONTENT_LENGTH
     *       The contents of any Content-Length fields in the HTTP request. May be 
     *       empty or absent.
     * SERVER_NAME, SERVER_PORT
     *       When combined with SCRIPT_NAME and PATH_INFO, these variables can be 
     *       used to complete the URL. Note, however, that HTTP_HOST, if present, 
     *       should be used in preference to SERVER_NAME for reconstructing the 
     *       request URL. See the URL Reconstruction section below for more detail.
     *       SERVER_NAME and SERVER_PORT can never be empty strings, and so are 
     *       always required.
     * SERVER_PROTOCOL
     *       The version of the protocol the client used to send the request. 
     *       Typically this will be something like "HTTP/1.0" or "HTTP/1.1" and may 
     *       be used by the application to determine how to treat any HTTP request
     *       headers. (This variable should probably be called REQUEST_PROTOCOL, 
     *       since it denotes the protocol used in the request, and is not 
     *       necessarily the protocol that will be used in the server's response. 
     *       However, for compatibility with CGI we have to keep the existing name.)
     * HTTP_ Variables
     *       Variables corresponding to the client-supplied HTTP request headers 
     *       (i.e., variables whose names begin with "HTTP_"). The presence or 
     *       absence of these variables should correspond with the presence or 
     *       absence of the appropriate HTTP header in the request.
     * 
     * </pre>
     * @param request
     * @return
     */
    PyObject createEnviron(Request request) {
        
        PyDictionary environ = new PyDictionary();
        
        environ.put("REQUEST_METHOD", request.getMethod().toString());
        
        Reference ref = request.getResourceRef();
        environ.put("SCRIPT_NAME", ref.getLastSegment());
        
        Reference pref = ref.getParentRef();
        environ.put("PATH_INFO", pref.toString().substring(
            request.getRootRef().toString().length(), pref.toString().length()-1 ));
        
        environ.put("QUERY_STRING", request.getResourceRef().getQuery());

        //TODO: fill in rest of parameters
        return environ;
    }

    /**
     * Creates the start_response object.
     */
    PyFunction createStartResponse() {
        return new PyFunction(new PyStringMap(), new PyObject[]{}, Py.newJavaCode(getClass(), "start_response"));
    }

    public static Object start_response(PyObject[] objs, String[] values) {
        PyString status = (PyString) objs[0];
        int space = status.toString().indexOf(' ');
        
        WSGIResponse r = RESPONSE.get();
        if (space != -1) {
            r.code = status.toString().substring(0, space);
            r.message = status.toString().substring(space+1);
        }
        else {
            r.code = status.toString();
        }
        
        if (objs.length > 1) {
            PyList headers = (PyList) objs[1];
            for (Iterator i = headers.iterator(); i.hasNext();) {
                PyTuple tup = (PyTuple) i.next();
                r.headers.put(tup.get(0).toString(), tup.get(1).toString());
            }
        }
        return null;
    }

    static class WSGIResponse {
        String code;
        String message;
        Map<String,String> headers = new CaseInsensitiveMap(new TreeMap<String, String>());
    }
}
