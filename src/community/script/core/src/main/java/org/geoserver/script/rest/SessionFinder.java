package org.geoserver.script.rest;

import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class SessionFinder extends Finder {

    ScriptManager scriptMgr;

    public SessionFinder(ScriptManager scriptMgr) {
        this.scriptMgr = scriptMgr;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (request.getAttributes().containsKey("ext")) {
            String ext = (String) request.getAttributes().get("ext");
            if (!scriptMgr.hasEngineForExtension(ext)) {
                throw new RestletException(
                    "Unsupported language: " + ext, Status.CLIENT_ERROR_NOT_FOUND);
            }
        }
        if (request.getAttributes().containsKey("sessson")) {
            try {
                long session = Long.valueOf((String)request.getAttributes().get("session"));
                if (scriptMgr.findSession(session) == null) {
                    throw new RestletException(
                        "No such session: " + session, Status.CLIENT_ERROR_NOT_FOUND);
                }
            }
            catch(NumberFormatException e) {
                throw new RestletException(
                    "Session must be numeric", Status.CLIENT_ERROR_NOT_FOUND, e);
            }
        }
        return new SessionResource(scriptMgr, request, response);
    }
}
