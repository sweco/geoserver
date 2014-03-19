package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Test;

public class H2JDBCResourceStoreTest extends AbstractJDBCResourceStoreTest {
    
    @Before
    public void setUp() throws Exception {
        support = new H2TestSupport();
    }
    
}
