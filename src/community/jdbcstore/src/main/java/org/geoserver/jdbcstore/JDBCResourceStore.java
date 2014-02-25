/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.NotImplementedException;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resource.Type;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
    
    public JDBCResourceStore(DataSource ds, JDBCResourceStoreProperties config) {
        this.ds = ds;
        this.config = config;
        
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
    
    private static File file( File file, String path ){
        for( String item : Paths.names(path) ){
            file = new File( file, item );           
        }
        
        return file;
    }
    
    @Override
    public Resource get(String path) {
        throw new NotImplementedException();
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

        @Override
        public String path() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public String name() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public InputStream in() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public OutputStream out() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
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
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }

        @Override
        public Resource parent() {
            // TODO Auto-generated method stub
            throw new NotImplementedException();
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
            // TODO Auto-generated method stub
            throw new NotImplementedException();
        }
     
    }
    
    private void initEmptyDB(DataSource ds) throws IOException {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
        
        Util.runScript(config.getInitScript(), template.getJdbcOperations(), null);
    }
}
