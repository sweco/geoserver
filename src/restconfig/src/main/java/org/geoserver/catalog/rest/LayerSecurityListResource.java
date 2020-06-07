/* Sweco */
package org.geoserver.catalog.rest;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class LayerSecurityListResource extends AbstractCatalogListResource {

    protected LayerSecurityListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, DataAccessRule.class, catalog);
    }

    // TODO: does not work - does not serialize OK since there are no identifiers
    // skip this
    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute("workspace");
        LOGGER.fine( "GET all layer security tuples" + ws != null ? " in workspace " + ws : "");

        List<DataAccessRule> result;
        List<DataAccessRule> items = getItems();
        if (ws != null) {
        	result = new ArrayList<DataAccessRule>();
        	for (DataAccessRule item : items) {
        		if (ws.equals(item.getWorkspace())) {
        			result.add(item);
        		}
        	}
        } else {
        	result = items;
        }
        return result;
    }

    protected List<DataAccessRule> getItems() {
        return DataAccessRuleDAO.get().getRules();
    }

}
