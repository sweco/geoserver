/* Sweco */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class LayerSecurityFinder extends AbstractCatalogFinder {

    public LayerSecurityFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String layer = getAttribute(request, "layer");
        
        if ( layer != null) { 
            if (ws != null && catalog.getLayerByName( ws + ":" + layer ) == null) {
                throw new RestletException(String.format("No such layer %s in workspace %s", 
                		layer, ws), Status.CLIENT_ERROR_NOT_FOUND );
            }
            if (ws == null && catalog.getLayerByName( layer ) == null) {
                throw new RestletException("No such layer " + layer, Status.CLIENT_ERROR_NOT_FOUND);
            }
        }

        if ( layer == null && request.getMethod() == Method.GET ) {
            return new LayerSecurityListResource(getContext(), request, response, catalog);
        }

        if (layer == null) {
            throw new RestletException("No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND);
        }

        return new LayerSecurityResource(getContext(), request, response, catalog);
    }

}
