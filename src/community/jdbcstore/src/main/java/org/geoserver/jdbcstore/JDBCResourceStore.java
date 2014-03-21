/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.commons.lang.NotImplementedException;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resource.Type;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Preconditions;

/**
 * Implementation of ResourceStore backed by a JDBC DataSource.
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class JDBCResourceStore implements ResourceStore {
    
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(JDBCResourceStore.class);
    
    private DataSource ds;
    private JDBCResourceStoreProperties config;
    private NamedParameterJdbcOperations template;
    
    private Dialect dialect;
    
    public JDBCResourceStore(DataSource ds, JDBCResourceStoreProperties config) {
        this.ds = ds;
        this.config = config;
        template = new NamedParameterJdbcTemplate(ds);
        
        // TODO: need to set this properly
        if("org.h2.Driver".equals(config.getProperty("driverClassName"))){
            dialect = new H2Dialect();
        } else {
            dialect = new PostgresDialect();
        }
        
        Connection c;
        try {
            c = ds.getConnection();
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Could not connect to provided DataSource.",ex);
        }
        try {
            if(config.isInitDb()) {
                LOGGER.log(Level.INFO, "Initializing Resource Store Database.");
                initEmptyDB(ds);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not initialize resource database.",ex);
        }
        finally {
            try {
                c.close();
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Error while closing connection.",ex);
            }
        }
    }

    
    @Override
    public Resource get(String path) {
        List<String> namesList = Paths.names(path);
        
        Connection c;
        try {
            c = ds.getConnection();
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Could not connect to DataSource.",ex);
        } 
        try {
            PreparedStatement stmt = dialect.getFindByPathQuery(c, 0, path);
            
            LOGGER.log(Level.INFO, stmt.toString());
            ResultSet rs = stmt.executeQuery();
            
            if(rs.next()) {
                // Found something
                
                // Should only have found one entry
                assert(rs.isLast()); 
                
                int depth = rs.getInt("depth");
                
                if(depth==namesList.size()) {
                    // Found the node
                    String name = (String) Field.NAME.getValue(rs);
                    int oid = (Integer) Field.OID.getValue(rs);
                    int parent = (Integer) Field.PARENT.getValue(rs);
                    boolean directory= (Boolean) Field.DIRECTORY.getValue(rs);
                    
                    return new JDBCResource(oid, directory?Type.DIRECTORY:Type.RESOURCE, name, parent);
                } else if (depth==namesList.size()-1) {
                    // Found the immediate parent
                    int parent = (Integer) Field.OID.getValue(rs);
                    String name = namesList.get(namesList.size()-1);
                    return new JDBCResource(-1, Type.UNDEFINED, name, parent);
                } else {
                    throw new NotImplementedException("Adding non-existing intermediate directories not supported");
                }
            } else {
                throw new IllegalStateException("Could not find a node along path "+path+" from root.  This should not happen as the root should always exist.");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
        finally {
            try {
                c.close();
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Error while closing connection.",ex);
            }
        }
    }
    Resource get(int oid) {
        Object[] results = query(new OIDSelector(oid), Field.OID, Field.DIRECTORY, Field.NAME, Field.PARENT);
        if(results==null) {
            return null;
        } else {
            int foundOid = (Integer)results[0];
            assert foundOid == oid;
            Type type = ((Boolean)results[1])?Type.DIRECTORY:Type.RESOURCE;
            String name = (String)results[2];
            int parent;
            if(results[3]==null) {
                parent = -1;
            } else {
                parent = (Integer)results[3];
            }
            return new JDBCResource(oid, type, name, parent);
        }
    }
    
    @Override
    public boolean remove(String path) {
        throw new NotImplementedException();
    }
    
    @Override
    public boolean move(String path, String target) {
        throw new NotImplementedException();
    }
    @Override
    public String toString() {
        return "ResourceStore "+ds;
    }
    
    /**
     * Direct implementation of Resource.
     * <p>
     * This implementation is a stateless data object, acting as a simple handle around a File.
     */
    class JDBCResource implements Resource {
        int oid;
        int parent;
        Type type;
        String name;
        
        public JDBCResource(int oid, Type type, String name, int parent) {
            super();
            assert((oid<0)==(type==Type.UNDEFINED)); // Must have an oid xor be undefined
            assert((parent<0)==(oid==0)); // Must have a parent xor be the root element
            this.oid = oid;
            this.type = type;
            this.name = name;
            this.parent = parent;
        }

        @Override
        public String path() {
            List<String> names;
            if(type==Type.UNDEFINED) {
                names = findPath(this.parent);
                names.add(name);
            } else {
                names = findPath(this.oid);
            }
            return Paths.path(names.toArray(new String[names.size()]));
        }

        @Override
        public String name() {
            if(type!=Type.UNDEFINED)
                name = (String) query(new OIDSelector(oid), Field.NAME)[0];
            
            return name;
        }
        
        @SuppressWarnings({ "unchecked", "resource" })
        private <T extends Closeable> T getStream(Class<T> clazz) {
            assert(clazz==InputStream.class || clazz==OutputStream.class);
            
            Connection c;
            try {
                c = ds.getConnection();
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Could not connect to DataSource.",ex);
            }
            try {
                PreparedStatement stmt = c.prepareStatement("SELECT content FROM resource WHERE oid=?;");
                stmt.setInt(1, oid);
                LOGGER.log(Level.INFO, stmt.toString());
                ResultSet rs = stmt.executeQuery();
                
                if(rs.next()) {
                    // Found something
                    
                    // Should only have found one entry
                    assert(rs.isLast()); 
                } else {
                    throw new IllegalStateException("Could not find resource "+oid);
                }
                Blob content = rs.getBlob("content");
                
                T stream;
                if(clazz==InputStream.class) {
                    stream = (T) new InputStreamWrapper(content.getBinaryStream(), c);
                } else {
                    stream = (T) new OutputStreamWrapper(content.setBinaryStream(1), c);
                }
                
                return stream;
            } 
            // Want to be sure that either the wrapped stream is returned, or the connection is
            // closed, but not both
            catch (Error er) {
                try {
                    c.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Exception while closing connection after error", ex);
                }
                throw er;
            } catch (SQLException ex) {
                try {
                    c.close();
                } catch (SQLException ex2) {
                    LOGGER.log(Level.SEVERE, "Exception while closing connection after exception", ex2);
                }
                throw new IllegalStateException("Could not get stream for resource", ex);
            } catch (RuntimeException ex) {
                try {
                    c.close();
                } catch (SQLException ex2) {
                    LOGGER.log(Level.SEVERE, "Exception while closing connection after exception", ex2);
                }
                throw ex;
            }

        }
        
        private void makeResource() {
            if(type==Type.RESOURCE) return;
            if(type==Type.DIRECTORY) throw new IllegalStateException("Directory when resource expected.");
            try {
                Connection c;
                try {
                    c = ds.getConnection();
                } catch (SQLException ex) {
                    throw new IllegalArgumentException("Could not connect to DataSource.",ex);
                }
                try {
                    PreparedStatement stmt = c.prepareStatement("INSERT INTO resource (name, parent, content) VALUES (?, ?, ?);");
                    stmt.setString(1,name);
                    stmt.setInt(2, parent);
                    Blob content = c.createBlob();
                    stmt.setBlob(3,content);
                    stmt.execute();
                    
                    ResultSet rs = stmt.getGeneratedKeys();
                    if(rs.next()) {
                        oid = rs.getInt(1);
                        type = Type.RESOURCE;
                    } else {
                        throw new IllegalStateException("Did not get OID for new resource");
                    }
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Could not create resource "+name+" in "+parent, ex);
            }
        }
        
        @Override
        public InputStream in() {
            makeResource();
            return getStream(InputStream.class);
        }

        @Override
        public OutputStream out() {
            makeResource();
            return getStream(OutputStream.class);
        }

        @Override
        public File file() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public File dir() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public long lastmodified() {
            Timestamp t = (Timestamp) query(new OIDSelector(oid), Field.LAST_MODIFIED)[0];
            return t.getTime();
        }

        @Override
        public Resource parent() {
            Object[] result = query(new OIDSelector(oid), Field.PARENT);
            if (result == null) {
                assert type==Type.UNDEFINED;
                // use the stored parent
            } else if(result[0]==null) {
                assert oid==0;
                assert parent==-1;
                return null;
            } else {
                parent = (Integer) result[0];
            }
            return JDBCResourceStore.this.get(parent);
        }

        @Override
        public Resource get(String resourcePath) {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public List<Resource> list() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + oid;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            JDBCResource other = (JDBCResource) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (oid != other.oid)
                return false;
            if (type != other.type)
                return false;
            return true;
        }

        private JDBCResourceStore getOuterType() {
            return JDBCResourceStore.this;
        }

        @Override
        public Lock lock() {
            // FIXME
            throw new UnsupportedOperationException();
        }
        
        
    }
    
    static class InputStreamWrapper extends ProxyInputStream {
        Connection conn;
        public InputStreamWrapper(InputStream proxy, Connection conn) {
            super(proxy);
            this.conn = conn;
        }
        
        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new IOException("Exception while closing connection",ex);
                }
            }
        }
    }
    
    static class OutputStreamWrapper extends ProxyOutputStream {
    Connection conn;
        public OutputStreamWrapper(OutputStream proxy, Connection conn) {
            super(proxy);
            this.conn = conn;
        }
        
        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new IOException("Exception while closing connection",ex);
                }
            }
        }
    }
    
    enum Field {
        OID("oid","oid") {
            @Override
            Object getValue(ResultSet rs) throws SQLException {
                return rs.getInt(fieldName);
            }
        },
        NAME("name", "name") {
            @Override
            Object getValue(ResultSet rs) throws SQLException {
                return rs.getString(fieldName);
            }
        },
        PARENT("parent","parent") {
            @Override
            Object getValue(ResultSet rs) throws SQLException {
                int i = rs.getInt(fieldName);
                if(rs.wasNull()) return null;
                return i;
            }
        },
        LAST_MODIFIED("last_modified","last_modified") {
            @Override
            Object getValue(ResultSet rs) throws SQLException {
                return rs.getTimestamp(fieldName);
            }
        },
        DIRECTORY("directory","content IS NULL AS directory") {
            @Override
            Object getValue(ResultSet rs) throws SQLException {
                return rs.getBoolean(fieldName);
            }
        }
       ;
        final String fieldName;
        final String fieldExpression;
        Field(String name, String expression) {
            this.fieldName = name;
            this.fieldExpression = expression;
        }
        abstract Object getValue(ResultSet rs) throws SQLException;
    }
    
    static interface Selector {
        StringBuilder appendCondition(StringBuilder sb);
    }
    
    static class OIDSelector implements Selector {
        int oid;
        OIDSelector(int i) {
            oid=i;
        }
        @Override
        public StringBuilder appendCondition(StringBuilder sb) {
            sb.append("oid = ");
            sb.append(oid);
            return sb;
        }
        
    }
    
    static class PathSelector implements Selector {
        String[] path;
        int oid;
        PathSelector(int oid, String... path) {
            this.path = path;
        }
        static Selector parse(int oid, String path) {
            return new PathSelector(oid, Paths.names(path).toArray(new String[]{}));
        }
        
        // FIXME do this more safely rather than inserting the strings directly
        StringBuilder oidQuery(StringBuilder builder, int i) {
            assert(i>=0);
            assert(i<path.length);
            
            if(i>0) {
                builder.append("SELECT oid FROM resource WHERE parent=(");
                oidQuery(builder, i-1);
                builder.append(") and name='");
                builder.append(path[i]);
                builder.append("'");
            } else {
                builder.append("SELECT oid FROM resource WHERE parent=");
                builder.append(oid);
                builder.append(" and name='");
                builder.append(path[i]);
                builder.append("'");
            }
            
            return builder;
        }
        
        @Override
        public StringBuilder appendCondition(StringBuilder sb) {
            sb.append("oid = (");
            oidQuery(sb, path.length-1);
            sb.append(")");
            return sb;
        }
        
    }
    
    
    
    List<String> findPath(int oid) {
        Connection c;
        try {
            c = ds.getConnection();
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Could not connect to DataSource.",ex);
        } 
        try {
            PreparedStatement stmt = dialect.getPathToQuery(c, oid);
            LOGGER.log(Level.INFO, "Looking up path: {0}", stmt);
            ResultSet rs = stmt.executeQuery();
            
            List<String> result = new LinkedList<String>();
            
            while(rs.next()) {
                int foundOid = rs.getInt("oid");
                String foundName = rs.getString("name");
                
                if(foundOid==0) {
                    assert(rs.isFirst());
                } else {
                    result.add(foundName);
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
        finally {
            try {
                c.close();
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Error while closing connection.",ex);
            }
        }

    }

    Object[] query(Selector sel, Field... fields) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("SELECT ");
        {
            int i=0;
            for(Field field:fields) {
                if(i++>0) builder.append(", ");
                builder.append(field.fieldExpression);
            }
        }
        builder.append(" FROM resource WHERE ");
        sel.appendCondition(builder);
        builder.append(";");
        
        Connection c;
        
        try {
            c = ds.getConnection();
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Could not connect to DataSource.",ex);
        } 
        try {
            Statement stmt = c.createStatement();
            LOGGER.log(Level.INFO, builder.toString());
            ResultSet rs = stmt.executeQuery(builder.toString());
            
            if(rs.next()) {
                // Found something
                
                // Should only have found one entry
                assert(rs.isLast()); 
                
                Object[] result = new Object[fields.length];
                
                for(int i = 0; i< fields.length; i++) {
                    result[i] = fields[i].getValue(rs);
                }
                return result;
            } else {
                // Nothing there yet
                return null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
        finally {
            try {
                c.close();
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Error while closing connection.",ex);
            }
        }
    }
    
    private void initEmptyDB(DataSource ds) throws IOException {
        
        Util.runScript(config.getInitScript(), template.getJdbcOperations(), null);
    }
}
