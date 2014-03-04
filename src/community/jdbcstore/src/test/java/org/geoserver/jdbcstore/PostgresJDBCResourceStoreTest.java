package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Optional;

public class PostgresJDBCResourceStoreTest {
    
    JDBCResourceStoreProperties mockConfig(boolean enabled, boolean init) {
        JDBCResourceStoreProperties config = createMock(JDBCResourceStoreProperties.class);
        expect(config.getInitScript()).andStubReturn(JDBCResourceStore.class.getResource("init.postgres.sql"));
        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:postgresql://localhost:5432/jdbcstoretest"));
        expect(config.isInitDb()).andStubReturn(init);
        expect(config.isEnabled()).andStubReturn(enabled);
        expect(config.isImport()).andStubReturn(init);
        expect(config.getJndiName()).andStubReturn(Optional.<String>absent());
        expect(config.getProperty(eq("username"))).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("username"), (String)anyObject())).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("password"))).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("password"), (String)anyObject())).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("driverClassName"))).andStubReturn("org.postgresql.Driver");
        expect(config.getProperty(eq("driverClassName"), (String)anyObject())).andStubReturn("org.postgresql.Driver");
        
        return config;
    }
    
    DataSource testDataSource() throws Exception {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName("localhost");
        ds.setDatabaseName("jdbcstoretest");
        ds.setPortNumber(5432);
        ds.setUser("jdbcstore");
        ds.setPassword("jdbcstore");
        
        // Ensure the database is empty
        Connection conn = ds.getConnection();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP SCHEMA IF EXISTS public CASCADE;");
            stmt.execute("CREATE SCHEMA public;");
            stmt.execute("GRANT ALL ON SCHEMA public TO postgres;");
            stmt.execute("GRANT ALL ON SCHEMA public TO public;");
            stmt.execute("COMMENT ON SCHEMA public IS 'standard public schema';");
        } finally {
            conn.close();
        }
        
        return ds;
    }
    
    
    DataSource ds;
    Connection conn;
    PreparedStatement insert;
    
    @Before
    public void setUp() throws Exception {
        ds = testDataSource();
        conn = ds.getConnection();
        insert = conn.prepareStatement("INSERT INTO resource (name, parent, content) VALUES (?, ?, ?) RETURNING oid;");
    }
    
    @After
    public void cleanUp() throws Exception {
        try{
            insert.close();
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testInitializeEmptyDB() throws Exception {
        JDBCResourceStoreProperties config = mockConfig(true, true);
        replay(config);
        
        @SuppressWarnings("unused")
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        // Check that the database has a resource table with a root record
        
        ResultSet rs = conn.createStatement().executeQuery("SELECT * from resource where oid = 0");
        
        assertThat(rs.next(), describedAs("found root record",is(true)));
        assertThat(rs.getString("name"), equalTo(""));
        rs.getInt("parent");
        assertThat(rs.wasNull(), is(true));
        assertThat(rs.getBlob("content"), nullValue());
        assertThat(rs.next(), describedAs("only one root",is(false)));
    }
    
    private int addFile(String name, int parent, byte[] content) throws Exception {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, content);
        ResultSet rs = insert.executeQuery();
        if(rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test file "+name);
        }
    }
    private int addDir(String name, int parent) throws Exception  {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, null);
        ResultSet rs = insert.executeQuery();
        if(rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test directory "+name);
        }
    }
    
    void preInit() throws Exception {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
        
        Util.runScript(JDBCResourceStore.class.getResource("init.postgres.sql"), template.getJdbcOperations(), null);
    }
    
    void standardData() throws Exception {
        preInit();
        
        addFile("FileA", 0, "FileA Contents".getBytes());
        addFile("FileB", 0, "FileB Contents".getBytes());
        int c = addDir("DirC", 0);
        addFile("FileD", c, "FileD Contents".getBytes());
        addDir("DirE", 0);
        int f = addDir("DirF", c);
        int g = addDir("DirG", f);
        addFile("FileH", g, "FileH Contents".getBytes());
    }
    
    @Test
    public void testAcceptInitializedDB() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        @SuppressWarnings("unused")
        ResourceStore store = new JDBCResourceStore(ds, config);
        {
            // Check that the database has a resource table with a root record
            
            ResultSet rs = conn.createStatement().executeQuery("SELECT * from resource where oid = 0");
            
            assertThat(rs.next(), describedAs("found root record",is(true)));
            assertThat(rs.getString("name"), equalTo(""));
            rs.getInt("parent");
            assertThat(rs.wasNull(), is(true));
            assertThat(rs.getBlob("content"), nullValue());
            assertThat(rs.next(), describedAs("only one root",is(false)));
        }
        {
            // Check that the database has one of the child nodes
            
            ResultSet rs = conn.createStatement().executeQuery("SELECT * from resource where parent = 0 and name='FileA'");
            
            assertThat(rs.next(), describedAs("found child FileA",is(true)));
            assertThat(rs.getString("name"), equalTo("FileA"));
            assertThat(rs.getInt("parent"), equalTo(0));
            assertThat(rs.wasNull(), is(false));
            assertThat(rs.getBinaryStream("content"), not(nullValue()));
            assertThat(rs.getInt("oid"), not(equalTo(0)));
        }
    }
    
    @Test
    public void testInitializeDatabaseWithIrrelevantTable() throws Exception {
        conn.createStatement().execute("CREATE TABLE foo (oid INTEGER PRIMARY KEY);");

        JDBCResourceStoreProperties config = mockConfig(true, true);
        replay(config);
        
        @SuppressWarnings("unused")
        ResourceStore store = new JDBCResourceStore(ds, config);
        {
            // Check that the database has a resource table with a root record
            
            ResultSet rs = conn.createStatement().executeQuery("SELECT * from resource where oid = 0");
            
            assertThat(rs.next(), describedAs("found root record",is(true)));
            assertThat(rs.getString("name"), equalTo(""));
            rs.getInt("parent");
            assertThat(rs.wasNull(), is(true));
            assertThat(rs.getBlob("content"), nullValue());
            assertThat(rs.next(), describedAs("only one root",is(false)));
        }
        
    }
    
    @Test
    public void testBasicResourceQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        Resource r = store.get("FileA");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, resource());
        
    }
    @Test
    public void testBasicDirectoryQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        Resource r = store.get("DirE");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, directory());
        
    }
    
    @Test
    public void testBasicUndefinedQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        Resource r = store.get("DoesntExist");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, undefined());
        
    }
    
    @Test
    public void testLongQuery() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        Resource r = store.get("DirC/DirF/DirG/FileH");
        
        assertThat(r, not(nullValue()));
        
        assertThat(r, resource());
        
    }
    @Test
    public void testBasicRead() throws Exception {
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        ResourceStore store = new JDBCResourceStore(ds, config);
        
        Resource r = store.get("FileA");
        
        byte[] expected = "FileA Contents".getBytes();
        
        InputStream in = r.in();
        try {
            byte[] result = new byte[expected.length];
            assertThat(in.read(result), describedAs("file contents same length",equalTo(expected.length)));
            assertThat(result, equalTo(expected));
            assertThat(in.read(), describedAs("stream is empty",equalTo(-1)));
        } finally {
            in.close();
        }
        
    }

}
