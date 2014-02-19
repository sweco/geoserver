/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.apache.commons.lang.SystemUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * Access to resources in GeoServer including configuration information and unmanaged cache or log files.
 * <p>
 * The loader maintains a search path in which it will use to look up resources.
 * <ul>
 * <li>Configuration is accessed using {@link ResourceStore#get(String)} which provides stream based access. If required configuration can be unpacked
 * into a file in the data directory. The most common example is for use as a template.
 * <li>Files in the data directory can also be used as a temporary cache. These files should be considered temporary and may need to be recreated
 * (when upgrading or for use on different nodes in a cluster).</li>
 * <li>
 * </ul>
 * The {@link #baseDirectory} is a member of this path. Files and directories created by the resource loader are made relative to
 * {@link #baseDirectory}.
 * </p>
 * <p>
 * 
 * <pre>
 * <code>
 * File dataDirectory = ...
 * GeoServerResourceLoader loader = new GeoServerResourceLoader( dataDirectory );
 * loader.addSearchLocation( new File( "/WEB-INF/" ) );
 * loader.addSearchLocation( new File( "/data" ) );
 * ...
 * Resource catalog = loader.get("catalog.xml");
 * File log = loader.find("logs/geoserver.log");
 * </code>
 * </pre>
 * 
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * 
 */
public class GeoServerResourceLoader extends DefaultResourceLoader implements ApplicationContextAware, ResourceStore {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /** "path" for resource lookups */
    Set<File> searchLocations;
    
    /** Mode used during transition to Resource use to verify functionality */ 
    private enum Compatibility {
        /** Traditional File Logic */
        FILE,
        /** Supplied ResourceStore used for file access */
        RESOURCE,
        /** File and Resource Logic compared, exception if inconsistent. */
        DUAL };
    
    private Compatibility mode = Compatibility.FILE;
    
    /**
     * ResourceStore used for configuration resources.
     * 
     * Initially this is configured to access resources in the base directory, however spring may inject an external implementation (jdbc database
     * blob, github, ...).
     */
    ResourceStore resources;

    /**
     * Base directory used to access unmanaged files.
     */
    File baseDirectory;

    /**
     * Creates a new resource loader (with no base directory).
     * <p>
     * Used to construct a GeoServerResourceLoader for test cases (and is unable to create resources from relative paths.
     * </p>
     */
    public GeoServerResourceLoader() {
        searchLocations = new TreeSet<File>();
        baseDirectory = null;
        resources = Resources.EMPTY;
    }

    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    @SuppressWarnings("unchecked")
    public GeoServerResourceLoader(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.resources = new FileSystemResourceStore( baseDirectory );
        
        setSearchLocations((Set<File>) Collections.EMPTY_SET);
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (baseDirectory == null) {
            //lookup the data directory
            if (applicationContext instanceof WebApplicationContext) {
                String data = lookupGeoServerDataDirectory(
                        ((WebApplicationContext)applicationContext).getServletContext());
                if (data != null) {
                    setBaseDirectory(new File(data)); 
                }
            }
        }
        if( resources == Resources.EMPTY ){
            // lookup the configuration resources
            if( baseDirectory != null ){
                resources = new FileSystemResourceStore( baseDirectory );
            }
        }
        
        // add additional lookup locations
        if (baseDirectory != null) {
            addSearchLocation(new File(baseDirectory, "data"));
        }

        if (applicationContext instanceof WebApplicationContext) {
            ServletContext servletContext = 
                ((WebApplicationContext)applicationContext).getServletContext();
            if (servletContext != null) {
                String path = servletContext.getRealPath("WEB-INF");
                if (path != null) {
                    addSearchLocation(new File(path));
                }
                path = servletContext.getRealPath("/");
                if (path != null) {
                    addSearchLocation(new File(path));
                }
            }
        }
        if( LOGGER.isLoggable(Level.INFO)){
            if( searchLocations.size() > 1 ){
                StringBuilder msg = new StringBuilder();
                
                msg.append("Search Location base directory: ");
                msg.append( baseDirectory );
                msg.append( SystemUtils.LINE_SEPARATOR );
                msg.append("Search Location resource store: ");
                msg.append(resources);
                msg.append( SystemUtils.LINE_SEPARATOR );
                for( File location : searchLocations ){
                    msg.append("Search Location additional dir: ");
                    msg.append( location );
                    msg.append( SystemUtils.LINE_SEPARATOR );
                }
                LOGGER.log(Level.INFO, msg.toString() );
            }
        }
    }
    
    /**
     * Adds a location to the path used for resource lookups.
     *
     * @param A directory containing resources.
     */
    public void addSearchLocation(File searchLocation) {
        searchLocations.add(searchLocation);
    }

    /**
     * Sets the search locations used for resource lookups.
     * 
     * The {@link #baseDirectory} is always incuded in {@link #searchLocations}.
     *
     * @param searchLocations A set of {@link File}.
     */
    public void setSearchLocations(Set<File> searchLocations) {
        this.searchLocations = new HashSet<File>(searchLocations);

        //always add the base directory
        if (baseDirectory != null) {
            this.searchLocations.add(baseDirectory);
        }
    }

    /**
     * @return The base directory.
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the base directory.
     * 
     * The base directory is included in {@link #searchLocations}.
     *
     * @param baseDirectory
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;

        searchLocations.add(baseDirectory);
    }

    @Override
    public Resource get(String path) {
        return resources.get(path);
    }
    @Override
    public boolean move(String path, String target) {
        return resources.move(path, target);
    }
    @Override
    public boolean remove(String path) {
        return resources.remove( path );
    }
    
    /**
     * Performs file lookup.
     *
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link ResourceStore#get(String)} for file access
     */
    public File find( String location ) throws IOException {
        Resource resource = get( Paths.convert(location) );
        return Resources.findFile( resource );

//        File file = find( null, location );
//        return check( file, check );
    }
    
    /**
     * Performs a resource lookup.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     * @deprecated Use {@link ResourceStore#get(String)} for file access
     */
    public File find( String... location ) throws IOException {
        Resource resource = get( Paths.path(location) );
        return Resources.findFile( resource );
        //return find( null, location );
    }

    /**
     * Performs a resource lookup, optionally specifying a containing directory.
     * <p>
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre> 
     * </p>
     * @param parentFile The parent directory, may be null.
     * @param location The components of the path of the resource to lookup.
     * 
     * @return The file handle representing the resource, or null if the
     *  resource could not be found.
     *  
     * @throws IOException Any I/O errors that occur.
     * @deprecated Use {@link Resource#get(String)} for file access
     */
    public File find( File parentFile, String... location ) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return Resources.findFile( resource );
        
        //return find( parent, concat( location ) );
    }

    /**
     * Performs a resource lookup, optionally specifying the containing directory.
     *
     * @param parentFile The containing directory, optionally null. 
     * @param location The name of the resource to lookup, can be absolute or
     * relative.
     *
     * @return The file handle representing the resource, or null if the
     * resource could not be found.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link Resource#get(String)} for file access
     */
    public File find(File parentFile, String location) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return Resources.findFile( resource );
        
//        if (LOGGER.isLoggable(Level.FINEST)) {
//            LOGGER.finest("Looking up resource " + location + " with parent " 
//                + (parent != null ? parent.getPath() : "null"));
//        }
//        File file = parent != null ? new File(parent,location) : new File(location);
//        if (file.isAbsolute()) {
//            return file.exists() ? file : null; // unable to perform ResourceStore lookup
//        }
//        
//        File check = null;
//        if( parent == null || baseDirectory.equals(parent)){
//            Resource resource = resources.get( Paths.convert(location) );
//            switch ( resource.getType()) {
//            case DIRECTORY:
//                check = resource.dir();
//                break;
//                
//            case RESOURCE:
//                check = resource.file();
//                break;
//            default:
//                check = null;
//                break;
//            }
//        }
//        else {
//            String path = Paths.convert(baseDirectory,parent,location);
//            Resource resource = resources.get( path );
//            switch ( resource.getType()) {
//            case DIRECTORY:
//                check = resource.dir();
//                break;
//                
//            case RESOURCE:
//                check = resource.file();
//                break;
//            default:
//                check = null;
//                break;
//            } 
//        }        
//        
//        if (file.exists()) {
//            return check( file, check );
//        }
//        
//        
//        //try a relative url if no parent specified
//        if ( parent == null ) {
//            for (Iterator<File> f = searchLocations.iterator(); f.hasNext();) {
//                File base = (File) f.next();
//                file = new File(base, location);
//
//                try {
//                    if (file.exists()) {
//                        return check( file, check );
//                    }
//                } catch (SecurityException e) {
//                    LOGGER.warning("Failed attemp to check existance of " + file.getAbsolutePath());
//                }
//            }
//        }
//        else {
//            //try relative to base dir
//            file = new File(baseDirectory, file.getPath());
//            if (file.exists()) {
//                return check( file, check );
//            }
//        }
//        
//
//        //look for a generic resource if no parent specified
//        if ( parent == null ) {
//            org.springframework.core.io.Resource resource = getResource(location);
//    
//            if (resource.exists()) {
//                return resource.getFile();
//            }
//        }
//
//        return null;
    }
    
    /**
     * Helper method to build up a file path from components.
     */
    String concat( String... location ) {
        StringBuffer loc = new StringBuffer();
        for ( int i = 0; i < location.length; i++ ) {
            loc.append( location[i] ).append( File.separator );
        }
        loc.setLength(loc.length()-1);
        return loc.toString();
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#dir()} for directory access
     */
    public File findOrCreateDirectory( String location ) throws IOException {
        Resource resource = get( Paths.convert(location) );        
        return resource.dir(); // will create directory as needed
        
        //return findOrCreateDirectory(null,location);
    }

    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     *  @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#dir()} for directory access
     */
    public File findOrCreateDirectory( String... location ) throws IOException {
        Resource resource = get( Paths.path(location) );
        return resource.dir(); // will create directory as needed
        
        //return findOrCreateDirectory(null,location);
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *  find or create.
     *  @deprecated Use {@link Resource#get(String)} and {@link Resource#dir()} for directory access
     */
    public File findOrCreateDirectory( File parentFile, String... location ) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return resource.dir(); // will create directory as needed
        //return findOrCreateDirectory(parent, concat(location));
    }
    
    /**
     * Performs a directory lookup, creating the file if it does not exist.
     * 
     * @param parent The containing directory, may be null.
     * @param location The location of the directory to find or create.
     * 
     * @return The file handle.
     * 
     * @throws IOException If any i/o errors occur.
     * @deprecated Use {@link Resource#get(String)} and {@link Resource#dir()} for directory access
     */
    public File findOrCreateDirectory( File parentFile, String location ) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.convert( location ));
        return resource.dir(); // will create directory as needed
        
//        File dir = find( parent, location );
//        if ( dir != null ) {
//            if ( !dir.isDirectory() ) {
//                //location exists, but is a file
//                throw new IllegalArgumentException( "Location '" + location + "' specifies a file");
//            }
//            
//            return dir;
//        }
//        
//        //create it
//        return createDirectory( parent, location );
    }
    
    /**
     * Creates a new directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     *
     * @throws IOException
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#dir()} for directory access
     */
    public File createDirectory(String location) throws IOException {
        Resource resource = get( Paths.convert(location) );
        return Resources.createNewDirectory(resource);
        //return createDirectory(null,location);
    }

    /**
     * Creates a new directory specifying components of the location.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#dir()} for directory access
     */
    public File createDirectory(String... location) throws IOException {
        Resource resource = get( Paths.path(location) );
        return Resources.createNewDirectory(resource);
        // return createDirectory(null,location);
    }
    
    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     * <p>
     * Calls through to {@link #createDirectory(String)}
     * </p>
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to create
     * @return newly created directory
     * @deprecated Use {@link Resource#get(String)} and {@link Resource#dir()} for directory access
     */
    public File createDirectory(File parentFile, String... location) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return Resources.createNewDirectory(resource);
        // return createDirectory(parent,concat(location));
    }
    
    /**
     * Creates a new directory, optionally specifying a containing directory.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a file, an IOException is thrown.
     * </p>
     * @param parentFile The containing directory, may be null.
     * @param location Location of directory to create, either absolute or
     * relative.
     *
     * @return The file handle of the created directory.
     * @deprecated Use {@link Resource#get(String)} and {@link Resource#dir()} for directory access
     * @throws IOException
     */
    public File createDirectory(File parentFile, String location) throws IOException {
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return Resources.createNewDirectory(resource);
//        // resource lookup
//        File check;
//        if( parentFile == null || baseDirectory.equals(parent)){
//            Resource resource = resources.get( Paths.convert(location) );
//            check = resource.dir(); // mkdir if needed          
//        }
//        else {
//            String path = Paths.convert(baseDirectory,parentFile,location);
//            Resource resource = resources.get( path );
//            check = resource.dir(); // mkdir if needed            
//        }
//        if( mode == Compatibility.RESOURCE ){
//            return check;
//        }
//        // traditional lookup
//        
//        File file = find(parentFile,location);
//        if (file != null) {
//            if (!file.isDirectory()) {
//                String msg = location + " already exists and is not directory";
//                throw new IOException(msg);
//            }
//        }
//
//        file = parent != null ? new File(parentFile,location) : new File(location);
//
//        if (file.isAbsolute()) {
//            file.mkdirs();
//
//            return check(file,check);
//        }
//
//        //no base directory set, cannot create a relative path
//        if (baseDirectory == null) {
//             String msg = "No base location set, could not create directory: " + location;
//             throw new IOException(msg);
//        }
//
//        if (parent != null && parentFile.getPath().startsWith(baseDirectory.getPath())) {
//            //parent contains base directory path, make relative to it
//            file = new File(parentFile, location);
//        }
//        else {
//            //base relative to base directory
//            file = parent != null ? new File(new File(baseDirectory, parentFile.getPath()), location)
//                : new File(baseDirectory, location);
//        }
//
//        file.mkdirs();
//        return check(file,check);
    }

    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(String)}.
     * </p>
     * 
     * @param location The components of the location.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#file()} for file access
     */
    public File createFile(String ...location) throws IOException {
        Resource resource = get( Paths.path(location) );
        return Resources.createNewFile( resource );
        
        //return createFile( concat(location) );
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     *
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#file()} for file access
     */
    public File createFile(String location) throws IOException {
        Resource resource = get( Paths.convert(location) );
        return Resources.createNewFile( resource );
        //return createFile(null,location);
    }
    
    /**
     * Creates a new file.
     * <p>
     * Calls through to {@link #createFile(File, String)}
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link Resource#get(String)} and {@link Resource#file()} for file access
     */
    public File createFile(File parentFile, String... location) throws IOException{
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.path( location ));
        return Resources.createNewFile(resource);
        //return createFile(parentFile,concat(location));
    }
    
    /**
     * Creates a new file.
     * <p>
     * Relative paths are created relative to {@link #baseDirectory}.
     * </p>
     * If {@link #baseDirectory} is not set, an IOException is thrown.
     * </p>
     * <p>
     * If <code>location</code> already exists as a directory, an IOException is thrown.
     * </p>
     * @param location Location of file to create, either absolute or relative.
     * @param parent The containing directory for the file.
     * 
     * @return The file handle of the created file.
     *
     * @throws IOException In the event of an I/O error.
     * @deprecated Use {@link ResourceStore#get(String)} and {@link Resource#file()} for file access
     */
    public File createFile(File parentFile, String location) throws IOException{
        Resource parent = get( Paths.convert(getBaseDirectory(), parentFile ) );
        Resource resource = parent.get( Paths.convert( location ));
        return Resources.createNewFile(resource);
        
//        File check = null;
//        // resource lookup
//        if( parent == null || baseDirectory.equals(parent)){
//            Resource resource = resources.get( Paths.convert(location) );
//            check = resource.file(); // create if needed          
//        }
//        else {
//            String path = Paths.convert(baseDirectory,parentFile,location);
//            Resource resource = resources.get( path );
//            check = resource.file(); // create if needed            
//        }
//        if( mode == Compatibility.RESOURCE ){
//            return check;
//        }
//        // traditional lookup
//        File file = find(parentFile,location);
//
//        if (file != null) {
//            if (file.isDirectory()) {
//                String msg = location + " already exists and is a directory";
//                throw new IOException(msg);
//            }
//            return check( file, check );
//        }
//
//        if ( parent == null ) {
//            //no base directory set, cannot create a relative path
//            if (baseDirectory == null) {
//                String msg = "No base location set, could not create file: " + location;
//                throw new IOException(msg);
//            }
//
//            file = new File(baseDirectory, location);
//            if (!file.getParentFile().exists()) {
//                file.getParentFile().mkdirs();
//            }
//            file.createNewFile();
//        }
//        return check( file, check );
    }
    
    /**
     * Copies a resource located on the classpath to a specified path.
     * <p>
     * The <tt>resource</tt> is obtained from teh context class loader of the 
     * current thread. When the <tt>to</tt> parameter is specified as a relative
     * path it is considered to be relative to {@link #getBaseDirectory()}.
      </p>
     * 
     * @param classpathResource The classpath content to copy
     * @param resource The destination resource to copy to.
     */
    public void copyFromClassPath( String classpathResource, String resource ) throws IOException {
        Resource res = get(Paths.convert(resource));
        copyFromClassPath( classpathResource, res.file() );        
//        File target = new File( to );
//        if ( !target.isAbsolute() ) {
//            target = new File( getBaseDirectory(), to );
//        }
//        
//        copyFromClassPath(resource, target);
    }
    
    /**
     * Copies a resource from the classpath to a specified file.
     * 
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     */
    public void copyFromClassPath( String classpathResource, File target ) throws IOException {
        copyFromClassPath( classpathResource, target, null );
    }
    
    /**
     * Copies a resource relative to a particular class from the classpath to the specified file. 
     * 
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     * @param scope Class used as base for classpathResource
     */
    
    public void copyFromClassPath( String classpathResource, File target, Class<?> scope ) throws IOException {
        InputStream is = null; 
        OutputStream os = null;
        byte[] buffer = new byte[4096];
        int read;

        try{
            // Get the resource
            if (scope == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);    
                if(is==null) {
                    throw new IOException("Could not load " + classpathResource + " from scope "+
                            Thread.currentThread().getContextClassLoader().toString()+".");
                }
            } else {
                is = scope.getResourceAsStream(classpathResource);
                if(is==null) {
                    throw new IOException("Could not load " + classpathResource + " from scope "+
                                 scope.toString()+".");
                }
            }
    
            // Write it to the target
            try {
                os = new FileOutputStream(target);
                while((read = is.read(buffer)) > 0)
                    os.write(buffer, 0, read);
            } catch (FileNotFoundException targetException) {
                throw new IOException("Can't write to file " + target.getAbsolutePath() + 
                        ". Check write permissions on target folder for user " + System.getProperty("user.name"));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error trying to copy logging configuration file", e);
            }
        } finally {
            // Clean up
            try {
                if(is != null){
                    is.close();
                }
                if(os != null){
                    os.close();
                }
            } catch(IOException e) {
                // we tried...
            }
        }
    }
    
    /**
     * Determines the location of the geoserver data directory based on the following lookup
     * mechanism:
     *  
     * 1) Java environment variable
     * 2) Servlet context variable
     * 3) System variable 
     *
     * For each of these, the methods checks that
     * 1) The path exists
     * 2) Is a directory
     * 3) Is writable
     * 
     * @param servContext The servlet context.
     * @return String The absolute path to the data directory, or <code>null</code> if it could not
     * be found. 
     */
    public static String lookupGeoServerDataDirectory(ServletContext servContext) {
        
        final String[] typeStrs = { "Java environment variable ",
                "Servlet context parameter ", "System environment variable " };

        final String[] varStrs = { "GEOSERVER_DATA_DIR", "GEOSERVER_DATA_ROOT" };

        String dataDirStr = null;
        String msgPrefix = null;
        
        // Loop over variable names
        for (int i = 0; i < varStrs.length && dataDirStr == null; i++) {
            
            // Loop over variable access methods
            for (int j = 0; j < typeStrs.length && dataDirStr == null; j++) {
                String value = null;
                String varStr = new String(varStrs[i]);
                String typeStr = typeStrs[j];

                // Lookup section
                switch (j) {
                case 0:
                    value = System.getProperty(varStr);
                    break;
                case 1:
                    value = servContext.getInitParameter(varStr);
                    break;
                case 2:
                    value = System.getenv(varStr);
                    break;
                }

                if (value == null || value.equalsIgnoreCase("")) {
                    LOGGER.finer("Found " + typeStr + varStr + " to be unset");
                    continue;
                }

                
                // Verify section
                File fh = new File(value);

                // Being a bit pessimistic here
                msgPrefix = "Found " + typeStr + varStr + " set to " + value;

                if (!fh.exists()) {
                    LOGGER.fine(msgPrefix + " , but this path does not exist");
                    continue;
                }
                if (!fh.isDirectory()) {
                    LOGGER.fine(msgPrefix + " , which is not a directory");
                    continue;
                }
                if (!fh.canWrite()) {
                    LOGGER.fine(msgPrefix + " , which is not writeable");
                    continue;
                }

                // Sweet, we can work with this
                dataDirStr = value;
            }
        }
        
        // fall back to embedded data dir
        if(dataDirStr == null)
            dataDirStr = servContext.getRealPath("/data");
        
        return dataDirStr;
    }
    
    /**
     * Compatibility check returning appropriate file for current {@link #mode}.
     * @param fileReference File reference produced from GeoServerResourceLoader
     * @param resourceReference File produced from ResourceStore
     * @return checked file reference
     */
    File check(File fileReference, File resourceReference) {
        if (fileReference != null && resourceReference != null && fileReference != null && fileReference.equals(resourceReference)) {
            try {
                String path1 = fileReference == null ? "" : fileReference.getCanonicalPath();
                String path2 = resourceReference == null ? "" : resourceReference
                        .getCanonicalPath();
                StringBuilder msg = new StringBuilder();

                int match = -1;
                for (int i = 0; i < path1.length() && i < path2.length(); i++) {
                    if (path1.charAt(i) != path2.charAt(i)) {
                        break;
                    } else {
                        match = i;
                    }
                }
                msg.append("Inconsistent File Path ");
                if (match != -1) {
                    msg.append(path1.substring(0, match));
                }
                msg.append(System.getProperty("line.separator"));
                msg.append("GeoResource File: ");
                msg.append(path1);
                msg.append(match == -1 ? path1 : path1.substring(match));
                msg.append("   Resource File: ");
                msg.append(match == -1 ? path2 : path2.substring(match));

                if (mode == Compatibility.DUAL) {
                    throw new IllegalStateException(msg.toString());
                } else {
                    LOGGER.fine(msg.toString());
                }
            } catch (IOException e) {
                // unable to check
            }
        }
        return mode == Compatibility.FILE ? fileReference : resourceReference;
    }

}
