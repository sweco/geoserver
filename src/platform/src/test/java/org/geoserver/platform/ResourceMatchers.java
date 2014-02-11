package org.geoserver.platform;

import org.hamcrest.Matcher;

public class ResourceMatchers {
    public static Matcher<Resource> exists() {
        return new ResourceExists();
    }
    
    public static Matcher<Resource> notExists() {
        return new ResourceNotExists();
    }
    
    public static Matcher<Resource> leaf() {
        return new ResourceIsLeaf();
    }
}
