package org.geoserver.jdbcstore;

import static org.easymock.classextension.EasyMock.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.geoserver.jdbcconfig.internal.Util;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Optional;

public class H2TestSupport implements DatabaseTestSupport {
    
    JDBCResourceStore store;
    JdbcDataSource ds;
    Connection conn;
    PreparedStatement insert;
    
    public H2TestSupport() throws Exception {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        conn = ds.getConnection();
        try {
            insert = conn.prepareStatement("INSERT INTO resource (name, parent, content) VALUES (?, ?, ?)");
        } finally {
            if(insert==null) conn.close();
        }
    }
    
    @Override
    public void stubConfig(JDBCResourceStoreProperties config) {
        expect(config.getInitScript()).andStubReturn(JDBCResourceStore.class.getResource("init.h2.sql"));
        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:h2:mem:test"));
        expect(config.getJndiName()).andStubReturn(Optional.<String>absent());
        expect(config.getProperty(eq("driverClassName"))).andStubReturn("org.h2.Driver");
        expect(config.getProperty(eq("driverClassName"), (String)anyObject())).andStubReturn("org.postgresql.Driver");
    }
    
    @Override
    public void initialize() throws Exception  {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
        
        Util.runScript(JDBCResourceStore.class.getResource("init.h2.sql"), template.getJdbcOperations(), null);

    }
    
    @Override
    public int addFile(String name, int parent, byte[] content) throws SQLException  {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, content);
        insert.execute();
        ResultSet rs = insert.getGeneratedKeys();
        if(rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test file "+name);
        }
    }
    
    @Override
    public int addDir(String name, int parent) throws SQLException  {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, null);
        insert.execute();
        ResultSet rs = insert.getGeneratedKeys();
        if(rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test directory "+name);
        }
    }
    
    @Override
    public int getRoot() {
        return 0;
    }
    
    @Override
    public DataSource getDataSource() {
        return ds;
    }
    
    @Override
    public Connection getConnection() throws SQLException { 
        return conn;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }

}
