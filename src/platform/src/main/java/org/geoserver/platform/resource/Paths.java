package org.geoserver.platform.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for handling Resource paths in a consistent fashion.
 * <p>
 * This utility class is primarily aimed at implementations of ResourceStore and may be helpful
 * when writing test cases. These methods are suitable for static import.
 * <p>
 * Resource paths are consistent with file URLs. The base location is represented with "", relative paths are not supported.
 * 
 * @author Jody Garnett
 */
public class Paths {
    /**
     * Path to base resource.
     */
    public static final String BASE = "";

    static String parent( String path ){
        if( path == null ){
            return null;
        }
        int last = path.lastIndexOf('/');
        if( last == -1 ){
            if( BASE.equals(path) ){
                return null;
            }
            else {
                return BASE;
            }
        }
        else {
            return path.substring(0,last);
        }
    }
    
    static String name( String path ){
        if( path == null ){
            return null;
        }
        int last = path.lastIndexOf('/');
        if( last == -1 ){            
            return path; // top level resource
        }
        else {
            String item = path.substring(last+1);
            return item;
        }
    }

    static String extension( String path ){
        String name = name(path);
        if( name == null ){
            return null;
        }
        int last = name.lastIndexOf('.');
        if( last == -1 ){
            return null; // no extension
        }
        else {
            return name.substring(last+1);
        }
    }
    
    static String sidecar( String path, String extension){
        if( extension == null ){
            return null;
        }
        int last = path.lastIndexOf('.');
        if( last == -1 ){
            return path+"."+extension;
        }
        else {
            return path.substring(0,last)+"."+extension;
        }
    }
    
    /**
     * Path construction.
     * 
     * @param path items
     * @return path
     */
    public static String path( String... path ){
        if( path == null || (path.length == 1 && path[0] == null)){
            return null;
        }
        ArrayList<String> names = new ArrayList<String>();
        for( String item : path ){
            names.addAll( names( item ) );
        }
        
        StringBuilder buf = new StringBuilder();
        final int LIMIT = names.size();
        for( int i=0; i<LIMIT; i++) {
            String item = names.get(i);
            if( item != null ){
                buf.append(item);
                if(i<LIMIT-1){
                    buf.append("/");
                }
            }
        }
        return buf.toString();
    }
    public static List<String> names(String path){
        if( path == null ){
            return Collections.emptyList();
        }
        int index=0;
        int split = path.indexOf('/');
        if( split == -1){
            return Collections.singletonList(path);
        }
        ArrayList<String> names = new ArrayList<String>(3);
        String item;
        do {
            item = path.substring(index,split);
            if( item != "/"){
                names.add( item );
            }
            index = split+1;
            split = path.indexOf('/', index);
        }
        while( split != -1);
        item = path.substring(index);
        if( item != null && item.length()!=0 && item != "/"){
            names.add( item );
        }
        
        return names;
    }
}
