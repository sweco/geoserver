package org.geoserver.jdbcstore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

public class SimpleResourceCache implements ResourceCache {
    File base;
    
    public SimpleResourceCache(File base) {
        this.base=base;
    }
    
    void cacheData(Resource res, File file) throws IOException {
        assert res.getType()==Type.RESOURCE;
        OutputStream out = new FileOutputStream(file);
        try {
            InputStream in = res.in();
            try {
                IOUtils.copy(in, out);
            } finally {
                in.close();
            }
        } finally {
            out.close();
        }
    }
    
    void cacheChildren(Resource res, File file) {
        assert res.getType()==Type.DIRECTORY;
        throw new UnsupportedOperationException();
    }
    
    @Override
    public File cache(Resource res) throws IOException {
        String path = res.path();
        long mtime = res.lastmodified();
        File cached = new File(base, path);
        Resource.Type type = res.getType();
        if(!cached.exists() || cached.lastModified()>mtime) {
            switch (type) {
            case RESOURCE:
                cached.getParentFile().mkdirs();
                cacheData(res, cached);
                break;
            case DIRECTORY:
                cached.mkdirs();
                cacheChildren(res, cached);
                break;
            case UNDEFINED:
                cached.getParentFile().mkdirs();
                break;
            }
        }
        return cached;
    }
}
