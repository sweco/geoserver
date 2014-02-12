package org.geoserver.platform.resource;

import static org.junit.Assert.*;

import org.junit.Test;

import static  org.geoserver.platform.resource.Paths.*;

public class PathsTest {
    
    final String BASE = "";
    final String DIRECTORY = "directory";
    final String FILE = "directory/file.txt";
    final String SIDECAR = "directory/file.prj";
    final String FILE2 = "directory/file2.txt";
    final String SUBFOLDER = "directory/folder";
    final String FILE3 = "directory/folder/file3.txt";
    
    @Test
    public void pathTest(){
        
        assertEquals(2,names("a/b").size());
        assertEquals(1,names("a/").size());
        assertEquals(1,names("a").size());
        assertEquals(0,names("").size());
        
        assertEquals( BASE, path(""));
        assertEquals("directory/file.txt",
                path("directory","file.txt"));        
        assertEquals( "directory/folder/file3.txt",
                path("directory/folder","file3.txt"));
        
        // handling invalid values        
        assertNull( path( (String) null)); // edge case
        assertEquals( "foo", path("foo/"));
    }
    @Test
    public void parentTest() {
        assertEquals( DIRECTORY, parent(FILE));
        assertEquals( BASE, parent(DIRECTORY));
        assertNull( parent(BASE));
        
        // handling invalid values
        assertNull( null, parent(null));
        assertEquals( "foo", parent("foo/"));
    }
    @Test
    public void naming() {
        assertEquals("file.txt", name("directory/file.txt"));
        assertEquals("txt", extension("directory/file.txt"));
        
        assertEquals("directory/file.txt", sidecar("directory/file","txt"));
        assertEquals("directory/file.prj", sidecar("directory/file.txt","prj"));
    }

}
