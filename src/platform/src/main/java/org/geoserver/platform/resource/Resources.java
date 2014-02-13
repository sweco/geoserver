package org.geoserver.platform.resource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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
    
    private static final class NullResourceStore extends NullResource implements ResourceStore {
        final long MODIFIED = System.currentTimeMillis();
        private NullResourceStore(){
            super("");            
        }
        @Override
        public Resource get(final String resourcePath) {
            if( Paths.BASE.equals( resourcePath )){
                return this;
            }
            return new NullResource(resourcePath);
        }
        @Override
        public long lastmodified() {
            return MODIFIED;
        }
        public String toString() {
            return "Resources.EMPTY";
        }
    }    
    private static class NullResource implements Resource {
        String path;

        private NullResource(String resourcePath) {
            path = resourcePath;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String name() {
            return Paths.name( path );
        }

        @Override
        public InputStream in() {
            throw new IllegalStateException("Unable to read from ResourceStore.EMPTY");
        }

        @Override
        public OutputStream out() {
            throw new IllegalStateException("Unable to write to ResourceStore.EMPTY");
        }

        @Override
        public File file() {
            throw new IllegalStateException("No file access to ResourceStore.EMPTY");
        }

        @Override
        public long lastmodified() {
            return EMPTY.lastmodified();
        }

        @Override
        public Resource getParent() {
            return EMPTY.get(Paths.parent(path));
        }
        @Override
        public Resource get(final String resourcePath) {
            return EMPTY.get( Paths.path( path, resourcePath) );
        }
        @Override
        public List<Resource> list() {
            return null;
        }

        @Override
        public Type getType() {
            return Type.UNDEFINED;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Resource other = (Resource) obj;
            if (path == null) {
                if (other.getPath() != null)
                    return false;
            } else if (!path.equals(other.getPath()))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return path;
        }
    }
    /**
     * Empty placeholder for ResourceStore.
     * <p>
     * Empty placeholder intended for test cases (used as spring context default when a base directory is not provided).
     * This implementation prevents client code from requiring null checks on {@link ResourceStore#get(String)}. IllegalStateException
     * are thrown by in(), out() and file() which are the usual methods clients require error handling.  
     */
    public static ResourceStore EMPTY = new NullResourceStore();
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