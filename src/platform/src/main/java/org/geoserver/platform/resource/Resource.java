/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Resource used for configuration storage.
 * 
 * Resources represent {@link Type#DIRECTORY}, {@link Type#RESOURCE} and {@link Type#UNDEFINED} content and is primarily used to manage configuration
 * information.
 * 
 * Resource creation is handled in a lazy fashion, simply use {@link #file()} or {@link #out()} and the resource will be created as required. In a
 * similar fashion setting up a child resource will create any required parent directories.
 */
public interface Resource {
    /**
     * Resource path used by {@link ResourceStore.get}.
     * 
     * @return resource path
     */
    String getPath();

    /**
     * Name of the resource denoted by {@link #getPath()} . This is the last name in the path name sequence corresponding to {@link File#getName()}.
     * 
     * @return Resource name
     */
    String name();

    /**
     * Steam access to resource contents.
     * 
     * @return stream access to resource contents.
     */
    InputStream in();

    /**
     * Steam access to resource contents.
     * 
     * @return stream acecss to resource contents.
     */
    OutputStream out();

    /**
     * File access to resource contents.
     * 
     * The resource may need to be unpacked into the GeoServer data directory prior to use. Do not assume the file exists before calling this method.
     * 
     * @return file access to resource contents.
     */
    File file();

    /**
     * Time this resource was last modified.
     * 
     * @see File#lastModified()
     * 
     * @return time resource was last modified
     */
    long lastmodified();

    /**
     * Resource parent, or null for ResourceStore base diretory.
     * 
     * @see File#getParentFile()
     * @return Resource located parent path, or null ResourceStore base directory
     */
    Resource getParent();

    /**
     * List of directory contents.
     * 
     * @see File#listFiles()
     * @return List of directory contents, or null if this resource is not a directory
     */
    List<Resource> list();

    /**
     * Enumeration indicating kind of resource used.
     */
    public enum Type {
        /**
         * Resource directory (contents available using {@link Resource#list()}).
         * 
         * @see File#isDirectory()
         */
        DIRECTORY,
        /**
         * Resource used for content. Content access available through {@link Resource#in()} and {@link Resource#out()}.
         */
        RESOURCE,

        /**
         * Undefined resource.
         * 
         * @see File#exists()
         */
        UNDEFINED
    };

    /**
     * Resource type.
     * 
     * @see File#exists()
     * @see File#isDirectory()
     * @see File#isFile()
     * @return
     */
    Type getType();

    // mkdirs() not needed, just create a child resource
    // boolean mkdirs();
    // createNew() not needed, just start writing the contents
    // boolean createNew();
}
