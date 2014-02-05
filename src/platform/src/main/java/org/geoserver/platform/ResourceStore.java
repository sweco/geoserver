/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

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
 * @see Resources
 */
public interface ResourceStore {
    /**
     * Path based resource access.
     * 
     * @param path 
     * @return resource
     */
    Resource get(String path);
}