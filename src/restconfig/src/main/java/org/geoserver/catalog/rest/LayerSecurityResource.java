/* Sweco */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class LayerSecurityResource extends AbstractResource {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");

    protected Catalog catalog;

    public LayerSecurityResource(Context context, Request request, Response response,
         Catalog catalog) {
        super(context, request, response);
        this.catalog = catalog;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new JsonFormat());
        return formats;
    }

    public boolean allowGet() {
        return getAttribute("layer") != null;
    }
    
    @Override
    public final void handleGet() {
        String workspace = getAttribute("workspace");
        String layer = getAttribute("layer");
        LOGGER.info("LayerSecurityResource GET: workspace=" + workspace + ", layer=" + layer);
        
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        List<DataAccessRule> rules = dao.getRules();
        StringBuilder json = new StringBuilder("{\"rules\":[");
        for (int i=0; i<rules.size(); i++) {
            DataAccessRule rule = rules.get(i);
            if (i>0) {
                json.append(",");
            }
            json.append("{ \"workspace:\"").append('"').append(rule.getWorkspace()).append("\",");
            json.append("\"layer:\"").append('"').append(rule.getLayer()).append("\",");
            json.append("\"mode:\"").append('"').append(rule.getAccessMode().name()).append("\",");
            json.append("\"roles:\"[");
            Iterator roleIter = rule.getRoles().iterator();
            for (int r=0; roleIter.hasNext(); r++) {
                String role = (String) roleIter.next();
                if (r>0) {
                    json.append(",");
                }
                json.append("{\"role:\"").append(role).append("}");
            }
            json.append("] }");
        }
        json.append("]}");
        getResponse().setEntity(json.toString(), MediaType.APPLICATION_JSON);
        getResponse().setStatus( Status.SUCCESS_OK );
        LOGGER.info("LayerSecurityResource GET: done");
    }

    @Override
    public boolean allowPost() {
        return getAttribute("layer") != null;
    }

    // TODO: JSON/body/correct object for groups
    @Override
    public final void handlePost() {
        // TODO: revisit: String workspace = getAttribute("workspace");
        String layer = getAttribute("layer");
        String groupsParam = RESTUtils.getQueryStringValue(getRequest(), "groups");
        LOGGER.info("LayerSecurityResource POST: layer=" + layer + ", groups=" + groupsParam);
        String[] groupArray = StringUtils.split(groupsParam,  ",");
        Set<String> groups = new HashSet<String>();
        for (String group : groupArray) {
            group = StringUtils.trimToNull(group);
            if (group != null) {
                groups.add(group);
            }
        }
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();

        // TODO: revisit:
        List<String> workspaces = new ArrayList<String>();
        workspaces.add("Sweref");
        workspaces.add("TU");
        
        for (String workspace : workspaces) {
	        DataAccessRule rule = new DataAccessRule(workspace, layer, AccessMode.READ, groups);
	        dao.addRule(rule);
        }
        store(dao);
        LOGGER.info("LayerSecurityResource POST: done");
    }

	@Override
    public boolean allowPut() {
        return false;
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("layer") != null;
    }
    
    @Override
    public final void handleDelete() {
        String workspace = getAttribute("workspace");
        String layer = getAttribute("layer");
        LOGGER.info("LayerSecurityResource DELETE: workspace=" + workspace + ", layer=" + layer);
        
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        List<DataAccessRule> rules = dao.getRules();
        try {
	        for (DataAccessRule rule : rules) {
	        	boolean equals = workspace == null || workspace.equals(rule.getWorkspace());
				if (equals && layer.equals(rule.getLayer())) {
					LOGGER.info("                      DELETE: match: workspace=" + workspace + ", layer=" + layer + ", mode=" + rule.getAccessMode() + ", roles=" + rule.getRoles());
					dao.removeRule(rule);
				}
			}
        } finally {
        	store(dao);
        }
        LOGGER.info("LayerSecurityResource DELETE: done");
    }
    
    private void store(DataAccessRuleDAO dao) {
        try {
        	dao.storeRules();
        	getResponse().setStatus( Status.SUCCESS_OK );
        } catch (IOException e) {
        	// TODO: log
        	getResponse().setStatus( Status.SERVER_ERROR_INTERNAL );
        }
	}

    private final class JsonFormat extends DataFormat {

		protected JsonFormat() {
			super(MediaType.APPLICATION_JSON);
		}
    	
    }
    
}
