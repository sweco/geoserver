package org.geoserver.jdbcstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

import org.geoserver.platform.resource.Paths;
import org.h2.tools.SimpleResultSet;

/**
 * Dialect support for H2
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
    
    
    /**
     * Traverses from a node back to the root to reconstruct the path 
     * @param conn
     * @param oid
     * @return
     * @throws SQLException
     */
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
    
    @Override
    public PreparedStatement getFindByPathQuery(Connection conn, int oid, String path) throws SQLException {
         PreparedStatement stmt = conn.prepareStatement("CALL path_to(?, ?)");
         stmt.setInt(1, oid);
         stmt.setString(2, path);
         
         return stmt;
    }
    
    /**
     * Traverses the path given from the context node and returns a record for the resulting node, 
     * or its closest extant ancestor.  The {@code depth} column indicates how many steps of the 
     * path were traversed. If {@code depth=path.length} then the desired node was found and if 
     * {@code depth<path.length} then it's an ancestor.
     * @param conn
     * @param oid
     * @param path
     * @return
     * @throws SQLException
     */
    public static ResultSet findByPath(Connection conn, int oid, String path) throws SQLException {
        SimpleResultSet resultRs = new SimpleResultSet();
        resultRs.addColumn("oid", Types.INTEGER, 10, 0);
        resultRs.addColumn("name", Types.VARCHAR, 255, 0);
        resultRs.addColumn("parent", Types.INTEGER, 10, 0);
        resultRs.addColumn("depth", Types.INTEGER, 10, 0);
        resultRs.addColumn("directory", Types.BOOLEAN, 1, 0);
        
        List<String> names = Paths.names(path);
        
        LinkedList<Node> result = new LinkedList<Node>();
        PreparedStatement stmt = conn.prepareStatement("SELECT oid, name, parent, content IS NULL AS directory FROM resource WHERE parent = ? AND name = ?;");
        int depth=0;
        int foundOid=-1;
        String foundName = null;
        Integer foundParent = null;
        boolean foundDirectory = false;
        try {
            Integer context=oid;
            ResultSet rs;
            
            for (String name:names) {
                
                stmt.setInt(1, context);
                stmt.setString(2, name);
                rs = stmt.executeQuery();
                if(!rs.next()) {
                    break;
                }
                
                foundOid = rs.getInt("oid");
                foundName = rs.getString("name");
                foundParent = rs.getInt("parent");
                if(rs.wasNull()) foundParent=null;
                foundDirectory = rs.getBoolean("directory");
                
                context = foundOid;
                if(rs.wasNull()) break;
                depth+=1;
            }

        } finally {
            stmt.close();
        }
        if(depth==0) {
            // Never found the first intermediate node, so grab the context node
            stmt = conn.prepareStatement("SELECT oid, name, parent, content IS NULL AS directory FROM resource WHERE oid = ?;");
            try{
                stmt.setInt(1, oid);
                ResultSet rs = stmt.executeQuery();
                if(!rs.next()) {
                    throw new IllegalStateException("Could not find context node "+oid);
                }
                foundOid = rs.getInt("oid");
                foundName = rs.getString("name");
                foundParent = rs.getInt("parent");
                if(rs.wasNull()) foundParent=null;
                foundDirectory = rs.getBoolean("directory");
            } finally {
                stmt.close();
            }
        }
        resultRs.addRow(foundOid, foundName, foundParent, depth, foundDirectory);
        return resultRs;
        
    }
}
