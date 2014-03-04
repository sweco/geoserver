package org.geoserver.jdbcstore;

import static org.easymock.classextension.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
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

public class PostgresJDBCResourceTheoryTest extends ResourceTheoryTest {

    JDBCResourceStore store;
    Connection conn;
    
    @DataPoints
    public static String[] testPaths() {
        return new String[]{"FileA","FileB", "DirC", "DirC/FileD", "DirE", "UndefF", "DirC/UndefF", "DirE/UndefF"/*, "DirE/UndefG/UndefH/UndefI"*/};
    }

    @Override
    protected Resource getResource(String path) throws Exception{
        return store.get(path);
    }
    
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
    PreparedStatement insert;
    
    @Before
    public void setUp() throws Exception {
        ds = testDataSource();
        conn = ds.getConnection();
        insert = conn.prepareStatement("INSERT INTO resource (name, parent, content) VALUES (?, ?, ?) RETURNING oid;");
        standardData();
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        store = new JDBCResourceStore(ds, config);
    }
    
    @After
    public void tearDown() throws Exception {
        conn.close();
        
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        conn = ds.getConnection();
        try {
            ResultSet rs = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
            
            boolean result = false;
            while(rs.next()) {
                result=true;
                System.out.printf("%s\n", rs.getString("TABLE_NAME"));
            }
            assertThat(result, describedAs("connection closed", is(false)));
        } finally {
            conn.close();
        }
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
    }

    void printTable() throws Exception{
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * from resource;");
        System.out.println();
        System.out.println("---");
        System.out.printf("\t%s\t%s\t%s\t%s\t%s\n", "oid", "name", "parent", "last_modified", "directory");
        System.out.println("---");
        try {
            while(rs.next()) {
                System.out.printf("\t%d\t%s\t%d\t%s\t%s\n", rs.getInt("oid"), rs.getString("name"), getInt(rs,"parent"), rs.getTimestamp("last_modified"), rs.getBlob("content")==null);
            }
        } finally {
            rs.close();
        }
    }
    
    Integer getInt(ResultSet rs, String column) throws Exception {
        int i = rs.getInt(column);
        if(rs.wasNull()) return null;
        return i;
    }
}
