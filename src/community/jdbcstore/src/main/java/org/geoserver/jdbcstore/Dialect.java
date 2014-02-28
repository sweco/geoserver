package org.geoserver.jdbcstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Support for specific DBMS back ends
 * 
 * @author Kevin Smith, Boundless
 *
 */
public interface Dialect {

    public abstract PreparedStatement getPathToQuery(Connection conn, int oid)
            throws SQLException;
    
}
