package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.ResourceStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class H2JDBCResourceStoreTest {
    
    @Test
    public void testInitializeEmptyDB() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        Connection conn = ds.getConnection();
        try {
            ResourceStore store = new JDBCResourceStore(ds, "TEST", "INFORMATION_SCHEMA", "init.h2.sql");
            
            // Check that the database has a resource table with a root record
            
            ResultSet rs = conn.createStatement().executeQuery("SELECT * from resource where oid = 0");
            
            assertThat(rs.next(), describedAs("found root record",is(true)));
            assertThat(rs.getString("name"), equalTo(""));
            rs.getInt("parent");
            assertThat(rs.wasNull(), is(true));
            assertThat(rs.getBlob("content"), nullValue());
            assertThat(rs.next(), describedAs("only one root",is(false)));
        } finally {
            conn.close();
        }
        
    }
    
    private int addFile(String name, int parent, byte[] content, PreparedStatement insert) throws Exception {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, content);
        insert.execute();
        ResultSet rs = insert.getGeneratedKeys();
        if(rs.next()) {
            return rs.getInt(1);
        } else {
            throw new IllegalStateException("Could not add test file "+name);
        }
    }
    private int addDir(String name, int parent, PreparedStatement insert) throws Exception  {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, null);
        insert.execute();
        ResultSet rs = insert.getGeneratedKeys();
        if(rs.next()) {
            return rs.getInt(1);
        } else {
            throw new IllegalStateException("Could not add test directory "+name);
        }
    }

    @Test
    public void testAcceptInitializedDB() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        Connection conn = ds.getConnection();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
        
        Util.runScript(JDBCResourceStore.class.getResource("init.h2.sql"), template.getJdbcOperations(), null);
        
        PreparedStatement insert = conn.prepareStatement("INSERT INTO resource (name, parent, content) VALUES (?, ?, ?)");
        
        try{
            addFile("FileA", 0, "FileA Contents".getBytes(), insert);
            addFile("FileB", 0, "FileB Contents".getBytes(), insert);
            int c = addDir("DirC", 0, insert);
            addFile("FileD", c, "FileD Contents".getBytes(), insert);
            addDir("DirE", 0, insert);
        } finally {
            insert.close();
        }

        try {
            ResourceStore store = new JDBCResourceStore(ds, "TEST", "INFORMATION_SCHEMA", "init.h2.sql");
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
                assertThat(rs.getBlob("content"), not(nullValue()));
                assertThat(rs.getInt("oid"), not(equalTo(0)));
            }
        } finally {
            conn.close();
        }
        
    }
    
    @Test
    public void testInitializeDatabaseWithIrrelevantTable() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        
        Connection conn = ds.getConnection();
        try {
            conn.createStatement().execute("CREATE TABLE foo (oid INTEGER PRIMARY KEY);");
            ResourceStore store = new JDBCResourceStore(ds, "TEST", "INFORMATION_SCHEMA", "init.h2.sql");
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
        } finally {
            conn.close();
        }
        
    }
    
}
