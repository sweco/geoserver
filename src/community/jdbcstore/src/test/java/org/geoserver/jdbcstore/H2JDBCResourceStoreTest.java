package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Optional;

public class H2JDBCResourceStoreTest {
    
    JDBCResourceStoreProperties mockConfig(boolean enabled, boolean init) {
        JDBCResourceStoreProperties config = createMock(JDBCResourceStoreProperties.class);
        expect(config.getInitScript()).andStubReturn(JDBCResourceStore.class.getResource("init.h2.sql"));
        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:h2:mem:test"));
        expect(config.isInitDb()).andStubReturn(init);
        expect(config.isEnabled()).andStubReturn(enabled);
        expect(config.isImport()).andStubReturn(init);
        expect(config.getJndiName()).andStubReturn(Optional.<String>absent());
        
        return config;
    }
    
    @Test
    public void testInitializeEmptyDB() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        Connection conn = ds.getConnection();
        JDBCResourceStoreProperties config = mockConfig(true, true);
        replay(config);
        
        try {
            ResourceStore store = new JDBCResourceStore(ds, config);
            
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
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
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
        
        JDBCResourceStoreProperties config = mockConfig(true, true);
        replay(config);
        
        Connection conn = ds.getConnection();
        try {
            conn.createStatement().execute("CREATE TABLE foo (oid INTEGER PRIMARY KEY);");
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
        } finally {
            conn.close();
        }
        
    }
    
    @Test
    public void testBasicResourceQuery() throws Exception {
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
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
            ResourceStore store = new JDBCResourceStore(ds, config);
            
            Resource r = store.get("FileA");
            
            assertThat(r, not(nullValue()));
            
            assertThat(r, resource());
            
        } finally {
            conn.close();
        }
        
    }
    @Test
    public void testBasicDirectoryQuery() throws Exception {
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
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
            ResourceStore store = new JDBCResourceStore(ds, config);
            
            Resource r = store.get("DirE");
            
            assertThat(r, not(nullValue()));
            
            assertThat(r, directory());
            
        } finally {
            conn.close();
        }
        
    }
    
    @Test
    public void testBasicUndefinedQuery() throws Exception {
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
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
            ResourceStore store = new JDBCResourceStore(ds, config);
            
            Resource r = store.get("DoesntExist");
            
            assertThat(r, not(nullValue()));
            
            assertThat(r, undefined());
        } finally {
            conn.close();
        }
        
    }
    
    @Test
    public void testLongQuery() throws Exception {
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
            int f = addDir("DirF", c, insert);
            int g = addDir("DirG", f, insert);
            addFile("FileH", g, "FileH Contents".getBytes(), insert);
        } finally {
            insert.close();
        }
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
            ResourceStore store = new JDBCResourceStore(ds, config);
            
            Resource r = store.get("DirC/DirF/DirG/FileH");
            
            assertThat(r, not(nullValue()));
            
            assertThat(r, resource());
        } finally {
            conn.close();
        }
        
    }
    @Test
    public void testBasicRead() throws Exception {
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
        
        JDBCResourceStoreProperties config = mockConfig(true, false);
        replay(config);
        
        try {
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
            
        } finally {
            conn.close();
        }
        
    }

}
