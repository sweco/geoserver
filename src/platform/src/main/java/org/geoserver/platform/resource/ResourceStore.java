/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;


/**
 * Used to manage configuration storage (file system, test harness, or database blob).
 * <p>
 * InputStream used to access configuration information:
 * <pre><code>
 * Properties properties = new Properties();
 * properties.load( resourceStore.get("module/configuration.properties").in() );
 * </code></pre>
 * 
 * An OutputStream is provided for storage (Resources will be created as needed):
 * <pre><code>
 * Properties properties = new Properties();
 * properties.put("hello","world");
 * OutputStream out = resourceStore.get("module/configuration.properties").out();
 * properties.store( out, null );
 * out.close();
 * </code></pre>
 * 
 * Resources can also be extracted to a file if needed.
 * <pre><code>
 * File file = resourceStore.get("module/logo.png");
 * BufferedImage img = ImageIO.read( file );
 * </code></pre>
 * 
 * The ResourceStore acts as the Resource for the {@link Paths#BASE} directory.
 * 
 * @see Resources
 * @see Resource
 */
public interface ResourceStore extends Resource {
    /**
     * Path based resource access.
     * 
     * The returned Resource acts as a handle, and may be UNDEFINED. In general Resources are created
     * in a lazy fashion when used for the first time.
     * 
     * @param path 
     * @return Resource at the indicated location
     * @throws
     */
    Resource get(String path);
    
    /**
     * ResoruceStore acts as {@link Paths#BASE} directory.
     * 
     * @return {@link Paths#BASE}
     */
    @Override
    public String getPath();
    
    /**
     * Parent <code>null</code> for {@link Paths#BASE}.
     * 
     * @return null as ResourceStore acts as the {@link Paths#BASE} directory.
     */
    @Override
    public Resource getParent();
}