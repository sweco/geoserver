package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.easymock.classextension.EasyMock.*;

import org.junit.Before;
import org.junit.Test;

public class PostgresJDBCResourceStoreTest extends AbstractJDBCResourceStoreTest{
    
    @Before
    public void setUp() throws Exception {
        support = new PostgresTestSupport();
    }
    

}
