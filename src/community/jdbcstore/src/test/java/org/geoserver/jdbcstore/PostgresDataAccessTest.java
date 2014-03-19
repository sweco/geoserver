package org.geoserver.jdbcstore;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.platform.resource.ResourceMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.Optional;

// TODO Experimenting with reading/writing content in Postgres. Delete this when done.
public class PostgresDataAccessTest {
    DatabaseTestSupport support;
    
    @Before
    public void setUp() throws Exception {
        support = new PostgresTestSupport();
    }
    @After
    public void tearDown() throws Exception {
        support.close();
    }
    
    void init(String type) throws Exception {
        Connection conn = support.getConnection();
        Statement stmt = conn.createStatement();
        try {
            /* -- Make new Postgres servers play nicely with the old JDBC driver
             * 
             * ALTER DATABASE jdbcstoretest SET bytea_output = escape;
             * 
             * 
             */
            /*
            CREATE TABLE resource
            (
                    oid serial NOT NULL,
                    name character varying NOT NULL,
                    parent integer,
                    last_modified timestamp without time zone NOT NULL DEFAULT timezone('UTC'::text, now()),
                    content bytea,
                    CONSTRAINT resource_pkey PRIMARY KEY (oid),
                    CONSTRAINT resource_parent_fkey FOREIGN KEY (parent)
                        REFERENCES resource (oid)
                        ON UPDATE RESTRICT ON DELETE CASCADE,
                    CONSTRAINT resource_parent_name_key UNIQUE (parent, name),
                    CONSTRAINT resource_only_one_root_check CHECK (parent IS NOT NULL OR oid = 0)
                  );
                  
                  */
            stmt.execute("CREATE TABLE resource\n            (\n                    oid serial NOT NULL,\n                    name character varying NOT NULL,\n                    parent integer,\n                    last_modified timestamp without time zone NOT NULL DEFAULT timezone('UTC'::text, now()),\n                    content "+type+",\n                    CONSTRAINT resource_pkey PRIMARY KEY (oid),\n                    CONSTRAINT resource_parent_fkey FOREIGN KEY (parent)\n                        REFERENCES resource (oid)\n                        ON UPDATE RESTRICT ON DELETE CASCADE,\n                    CONSTRAINT resource_parent_name_key UNIQUE (parent, name),\n                    CONSTRAINT resource_only_one_root_check CHECK (parent IS NOT NULL OR oid = 0)\n                  );");
            stmt.execute("CREATE INDEX resource_parent_name_idx ON resource (parent NULLS FIRST, name NULLS FIRST);");
            
            stmt.execute("INSERT INTO resource (oid, name, parent, content) VALUES (0, '', NULL, NULL);");
        } finally {
            stmt.close();
        }
    }
    

    @Test 
    public void testTypeBinaryPutGetAsBinaryStream() throws Exception {
        init("bytea");
        
        Connection conn = support.getConnection();
        
        int oid;
        {
            Statement stmt = conn.createStatement();
            try {
                ResultSet rs = stmt.executeQuery("INSERT INTO resource(name, parent, content) VALUES ('test', 0, null) RETURNING oid;");
                try {
                    assertTrue(rs.next());
                    oid=rs.getInt(1);
                    assertFalse(rs.wasNull());
                } finally {
                    rs.close();
                }
            } finally {
                stmt.close();
            }
        }
        
        byte[] expected = {42, 24, 64, 90};
        
        {
            PreparedStatement stmt = conn.prepareStatement("UPDATE resource SET content = ? WHERE oid = ?;");
            try {
                InputStream is = new ByteArrayInputStream(expected);
                stmt.setBinaryStream(1, is, expected.length);
                stmt.setInt(2, oid);
                stmt.execute();
            } finally {
                stmt.close();
            }
        }
        
        {
            Statement stmt = conn.createStatement();
            try {
                ResultSet rs = stmt.executeQuery("SELECT content FROM resource WHERE oid = "+oid+";");
                
                assertTrue(rs.next());
                InputStream is = rs.getBinaryStream(1);
                assertThat(is, notNullValue());
                
                try {
                    assertTrue(streamContains(is, expected));
                } finally {
                    is.close();
                }
                
            } finally {
                stmt.close();
            }
        }

    }
    
    boolean streamContains(InputStream is, byte[] expected) throws Exception {
        StringBuilder error = new StringBuilder();
        for(byte exb:expected) {
            int resb = is.read();
            
            if(resb != (exb&0xFF)) {
                error.append("[")
                    .append(String.format("%02x", exb&0xFF))
                    .append("|")
                    .append(String.format("%02x", resb)).append("]");
                System.out.println(error.toString());
                return false;
            }
            error.append(String.format("%02x", exb&0xFF));
        }
        int resb = is.read();
        if(resb!=-1){
            error.append("[")
            .append(String.format("%02x", -1&0xFF))
            .append("|")
            .append(String.format("%02x", resb)).append("]");
        System.out.println(error.toString());
        return false;
        }
        return true;
    }
}
