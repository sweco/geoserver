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

    /**
     * Prepares a query to find the path from the root to the specified node 
     * @param conn
     * @param oid OID of the node to find a path to
     * @return PreparedStatement which will return an ordered result set with a {@code name} column and a row for each step in the path, starting with the root.
     * @throws SQLException
     */
    public abstract PreparedStatement getPathToQuery(Connection conn, int oid)
            throws SQLException;

    /**
     * Prepares a query to traverse a path to a node
     * @param conn
     * @param oid OID of the node to start the traversal at
     * @param path the path to traverse
     * @return PreparedStatement which will return a single row which is the node furhtest along the path which exists and the columns {@code oid}, {@code name}, {@code parent}, {@code depth}, {@code directory}, where depth is how many steps were traversed, and directory is whether {@code content IS NULL}.
     * @throws SQLException
     */
    public abstract PreparedStatement getFindByPathQuery(Connection conn, int oid,
            String path) throws SQLException;
    
}
