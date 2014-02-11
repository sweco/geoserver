package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.operator.FileStoreDescriptor;

import org.springframework.util.FileSystemUtils;

/**
 * Implementation of ResourceStore backed by the file system.
 */
public class FileSystemResourceStore implements ResourceStore {
    
    private File baseDirectory;

    public FileSystemResourceStore(File resourceDirectory) {
        if (resourceDirectory == null) {
            throw new NullPointerException("root resource directory required");
        }
        if( resourceDirectory.isFile()){
            throw new IllegalArgumentException("Directory required, file present at this location " + resourceDirectory );
        }
        if( !resourceDirectory.exists()){
            boolean create = resourceDirectory.mkdirs();
            if( !create ){
                throw new IllegalArgumentException("Unable to create directory " + resourceDirectory );                
            }            
        }
        if( resourceDirectory.exists() && resourceDirectory.isDirectory() ){
            this.baseDirectory = resourceDirectory;
        }
        else {
            throw new IllegalArgumentException("Unable to acess directory " + resourceDirectory );            
        }
    }
    
    private static File file( File file, String path ){
        for( int index = 0; index != -1; index = path.indexOf('/') ){
            String next = path.substring(0,index);
            file = new File( file, next );
            
            path = path.substring(index+1);
        }
        file = new File( file, path );
        
        return file;
    }
    
    @Override
    public Resource get(String path) {
        return new FileSystemResource( path );
    }
    
    /**
     * Direct implementation of Resource.
     * <p>
     * This implementation is a stateless data object, acting as a simple handle around a File.
     */
    class FileSystemResource implements Resource {
        String path;
        File file;
        
        public FileSystemResource(String path) {
            this.path = path;
            this.file = FileSystemResourceStore.file( baseDirectory, path );
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String name() {
            int split = path.lastIndexOf('/');
            if( split == -1 ){
                return path;
            }
            else {
                return path.substring(split);
            }
        }

        @Override
        public InputStream in() {
            File file = file();
            if( !file.exists() ){
                throw new IllegalStateException("File not found "+path);
            }
            try {
                return new FileInputStream( file );
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("File not found "+path);
            }
        }

        @Override
        public OutputStream out() {
            File file = file();
            if( !file.exists() ){
                try {
                    boolean created = file.createNewFile();
                    if( !created ){
                        // file was already available?!?
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot create "+path);
                }
            }
            try {
                return new FileOutputStream( file );
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot access "+path);
            }
        }

        @Override
        public File file() {
            return file;
        }
        @Override
        public long lastmodified() {
            return file.lastModified();
        }

        @Override
        public Resource getParent() {
            int split = path.lastIndexOf('/');
            if (split == -1 ){
                return FileSystemResourceStore.this.get(""); // root
            }
            else {
                return FileSystemResourceStore.this.get( path.substring(0,split) );
            }
        }

        @Override
        public List<Resource> list() {
            String array[] = file.list();
            if( array == null ){
                return null; // not a directory
            }            
            List<Resource> list = new ArrayList<Resource>( array.length );
            for( String filename : array ){
                list.add( FileSystemResourceStore.this.get( path+"/"+filename) );
            }
            return list;
        }
        @Override
        public Type getType() {
            if( !file.exists() ){
                return Type.UNDEFINED;
            }
            else if (file.isDirectory()){
                return Type.DIRECTORY;
            }
            else if (file.isFile()){
                return Type.RESOURCE;
            }
            else {
                throw new IllegalStateException("Path does not represent a configuration resource: "+path );                
            }
        }
    }
}
