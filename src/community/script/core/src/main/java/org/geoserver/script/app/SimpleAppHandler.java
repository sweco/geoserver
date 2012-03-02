/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.geoserver.script.ScriptPlugin;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * A simple app handler that simply passes the raw request/response objects into the script engine.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class SimpleAppHandler extends AppHandler {

    public SimpleAppHandler(ScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public void run(Reader script, ScriptEngine engine, Request request, Response response) 
        throws ScriptException {

        Bindings b = engine.getBindings(ScriptContext.ENGINE_SCOPE); 
        b.put("request", request);
        b.put("response", response);

        engine.eval(script);
    }

}
