package org.geoserver.jdbcstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;

import org.h2.tools.SimpleResultSet;

/**
 * Utility methods for working with H2
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class H2Dialect implements Dialect{

    static class Node {
        int oid;
        String name;
    }
    
    @Override
    public PreparedStatement getPathToQuery(Connection conn, int oid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("CALL path_to(?)");
        stmt.setInt(1, oid);
        
        return stmt;
    }
    
    public static ResultSet pathTo(Connection conn, int oid) throws SQLException {
        SimpleResultSet resultRs = new SimpleResultSet();
        resultRs.addColumn("oid", Types.INTEGER, 10, 0);
        resultRs.addColumn("name", Types.VARCHAR, 255, 0);
        
        LinkedList<Node> result = new LinkedList<Node>();
        PreparedStatement stmt = conn.prepareStatement("SELECT parent, oid, name FROM resource WHERE oid = ?;");
        try {
            Integer foundParent=oid;
            String foundName;
            while(true){
                Node p = new Node();
                
                stmt.setInt(1, foundParent);
                ResultSet rs = stmt.executeQuery();
                if(!rs.next()) {
                    throw new SQLException("Could not find resource "+foundParent+" while generating path of resource "+oid);
                }
                
                foundParent = rs.getInt("parent");
                if(rs.wasNull()) foundParent=null;
                p.name = rs.getString("name");
                p.oid = rs.getInt("oid");
                
                result.addFirst(p);
                
                if(foundParent==null) break;
            }
            
            for(Node node: result) {
                resultRs.addRow(node.oid, node.name);
            }
            return resultRs;
        } finally {
            stmt.close();
        }
    }
}
