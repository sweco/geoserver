/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import javax.script.ScriptEngine;

import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * App resource that handles an app request.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AppResource extends Resource {

    ScriptManager scriptMgr;
    File script;

    public AppResource(File script, ScriptManager scriptMgr, Request request, Response response) {
        super(null, request, response);
        this.scriptMgr = scriptMgr;
        this.script = script;
    }
    
    @Override
    public void handleGet() {
        try {
            ScriptEngine eng = scriptMgr.createNewEngine(script);
            if (eng == null) {
                throw new RestletException(format("Script engine for %s not found", script.getName()), 
                    Status.CLIENT_ERROR_BAD_REQUEST);
            }

            //look up the app handler
            AppHandler handler = scriptMgr.lookupAppHandler(script);
            if (handler == null) {
                //TODO: fall back on default
                throw new RestletException(format("No handler found for %s", script.getPath()), 
                    Status.SERVER_ERROR_INTERNAL);
            }

            Reader in = new BufferedReader(new FileReader(script));
            try {
                handler.run(in, eng, getRequest(), getResponse());
            }
            finally {
                in.close();
            }
        } 
        catch (Exception e) {
            throw new RestletException("Error executing script " + script.getName(), 
                Status.SERVER_ERROR_INTERNAL, e);
        }
    }
}
