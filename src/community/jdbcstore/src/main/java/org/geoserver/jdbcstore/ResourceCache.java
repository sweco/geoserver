package org.geoserver.jdbcstore;

import java.io.File;
import java.io.IOException;

import org.geoserver.platform.resource.Resource;

public interface ResourceCache {
    public File cache(Resource res) throws IOException;
}
