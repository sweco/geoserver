package org.geoserver.jdbcstore;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.geoserver.platform.resource.Paths;

import com.google.common.base.Joiner;

/**
 * Dialect support for Postgres
 * @author Kevin Smith, Boundless
 *
 */
public class PostgresDialect implements Dialect {
    
    @Override
    public PreparedStatement getPathToQuery(Connection conn, int oid)
            throws SQLException {
        String sql = "WITH RECURSIVE path(oid, name, parent, depth) AS (\n    SELECT oid, name, parent, 0 FROM resource WHERE oid=?\n  UNION ALL\n    SELECT cur.oid, cur.name, cur.parent, rec.depth+1\n      FROM resource AS cur, path AS rec\n      WHERE cur.oid=rec.parent\n  )\nSELECT oid, name FROM path;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, oid);
        return stmt;
    }
    
    @Override
    public PreparedStatement getFindByPathQuery(Connection conn, int oid,
            String path) throws SQLException {
        List<String> namesList = Paths.names(path);
        String sql = "WITH RECURSIVE path(oid, name, parent, depth, directory) AS ( SELECT oid, name, parent, 0, true FROM resource WHERE oid=? UNION ALL SELECT cur.oid, cur.name, cur.parent, rec.depth+1, content IS NULL FROM resource AS cur, path AS rec WHERE cur.parent=rec.oid AND cur.name=(string_to_array(?, '/'))[depth+1]) SELECT name,oid,parent,depth,directory FROM path ORDER BY depth DESC LIMIT 1;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, 0);
        String normalizedPath = Joiner.on('/').join(namesList);
        
        
        //Array names = conn.createArrayOf("varchar", namesList.toArray());
        //stmt.setArray(2, names);
        stmt.setString(2, normalizedPath);
        
        return stmt;
    }

}
