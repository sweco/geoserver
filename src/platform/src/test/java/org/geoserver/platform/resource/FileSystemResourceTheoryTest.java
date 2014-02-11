package org.geoserver.platform.resource;

import java.io.File;

import org.geoserver.platform.Resource;
import org.geoserver.platform.ResourceTheoryTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceTheoryTest extends ResourceTheoryTest {

    FileSystemResourceStore store;
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    
    @DataPoints
    public static String[] testPaths() {
        return new String[]{"A","B", "C", "C/D", "E", "F", "C/F", "E/F", "E/G/H/I"};
    }

    @Override
    protected Resource getResource(String path) throws Exception{
        return store.get(path);
    }
    
    @Before
    public void setUp() throws Exception {
        folder.newFile("A");
        folder.newFile("B");
        File c = folder.newFolder("C");
        (new File(c, "D")).createNewFile();
        folder.newFolder("E");
        store = new FileSystemResourceStore(folder.getRoot());
    }
}
