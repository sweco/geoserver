/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.Reader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Handles "app" requests.
 * <p>
 * This class is responsible for adapting a raw http request into something that can be handled by
 * an app script. For example, a python extension could transform the request/response into a WSGI
 * request/response to be handled by the underlying app script.
 * </p>
 * <p>
 * Instances of this class must be thread safe.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class AppHandler {

    /**
     * Handles a request.
     * 
     * @param script The app script.
     * @param engine The script engine of the appropriate type.
     * @param request The http request.
     * @param response The http response.
     * 
     */
    public abstract void run(Reader script, ScriptEngine engine, Request request, Response response)
        throws ScriptException;

}
