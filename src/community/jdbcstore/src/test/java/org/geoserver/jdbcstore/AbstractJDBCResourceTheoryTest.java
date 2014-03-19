package org.geoserver.jdbcstore;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;

import java.sql.ResultSet;

import javax.sql.DataSource;

import org.geoserver.platform.resource.ResourceTheoryTest;
import org.junit.After;
import org.junit.experimental.theories.DataPoints;

public abstract class AbstractJDBCResourceTheoryTest extends ResourceTheoryTest {

    DatabaseTestSupport support;

    @DataPoints
    public static String[] testPaths() {
        return new String[]{"FileA","FileB", "DirC", "DirC/FileD", "DirE", "UndefF", "DirC/UndefF", "DirE/UndefF"/*, "DirE/UndefG/UndefH/UndefI"*/};
    }

    protected JDBCResourceStoreProperties mockConfig(boolean enabled, boolean init) {
        JDBCResourceStoreProperties config = createMock(JDBCResourceStoreProperties.class);
    
        expect(config.isInitDb()).andStubReturn(init);
        expect(config.isEnabled()).andStubReturn(enabled);
        expect(config.isImport()).andStubReturn(init);
        
        support.stubConfig(config);
        
        return config;
    }

    protected DataSource testDataSource() throws Exception {
        return support.getDataSource();
    }

    public AbstractJDBCResourceTheoryTest() {
        super();
    }
    
    protected void standardData() throws Exception {
        support.initialize();
        
        support.addFile("FileA", 0, "FileA Contents".getBytes());
        support.addFile("FileB", 0, "FileB Contents".getBytes());
        int c = support.addDir("DirC", 0);
        support.addFile("FileD", c, "FileD Contents".getBytes());
        support.addDir("DirE", 0);
    }
    
    Integer getInt(ResultSet rs, String column) throws Exception {
        int i = rs.getInt(column);
        if(rs.wasNull()) return null;
        return i;
    }

    @After
    public void cleanUp() throws Exception {
        support.close();
    }
}