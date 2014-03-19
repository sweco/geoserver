package org.geoserver.jdbcstore;

import static org.easymock.classextension.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceTheoryTest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class H2JDBCResourceTheoryTest extends AbstractJDBCResourceTheoryTest {

    JDBCResourceStore store;
    
    @Override
    protected Resource getResource(String path) throws Exception{
        return store.get(path);
    }
    
    @Before
    public void setUp() throws Exception {
        support = new H2TestSupport();
        
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        store = new JDBCResourceStore(support.getDataSource(), config);
    }

}
