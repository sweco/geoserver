package org.geoserver.platform.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.platform.resource.Resource.Type;

/**
 * Utility methods for working with {@link ResourceStore}.
 * 
 * These methods are suitable for static import and are intended automate common tasks.
 * 
 * @author Jody Garnett
 */
public class Resources {
    /**
     * Search for resources using pattern and last modified time.
     * 
     * @param resource
     * @param lastModified
     * @return list of modified resoruces
     */
    List<Resource> search(Resource resource, long lastModified) {
        if (resource.getType() == Type.DIRECTORY) {
            ArrayList<Resource> results = new ArrayList<Resource>();
            for( Resource child : resource.list() ){
                switch ( child.getType()) {
                case RESOURCE:
                    if( child.lastmodified() > lastModified ){
                        results.add( child );
                    }                    
                    break;

                default:
                    break;
                }
            }
            return results;
        }
        return Collections.emptyList();
    }
}