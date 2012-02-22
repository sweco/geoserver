package org.geoserver.wps.surface;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class PointExtracter 
{

    public PointExtracter() {
    }

    public static Coordinate[] extract(SimpleFeatureCollection obsPoints, String attrName) throws CQLException 
    {
        Expression attrExpr = ECQL.toExpression(attrName);
        Coordinate[] pts = new Coordinate[obsPoints.size()];
        SimpleFeatureIterator obsIt = obsPoints.features();
        
        int i = 0;
        try {
            while (obsIt.hasNext()) {
                SimpleFeature feature = obsIt.next();
                
                double val = 0;
                
                try {
                    // get the observation value from the attribute
                    Object valObj = attrExpr.evaluate(feature);
                    // System.out.println(evaluate);
                    Number valNum = (Number) valObj;
                    val = valNum.doubleValue();
                }
                catch (Exception e) {
                    // just carry on for now (debugging)
                    //throw new ProcessException("Expression " + attrExpr + " failed to evaluate to a numeric value", e);
                }
                
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Coordinate p = geom.getCoordinate();
                // get the point location from the geometry
                pts[i++] = new Coordinate(p.x, p.y, val);
            }
        }
        finally {
            obsPoints.close( obsIt );
        }

        return pts;
    }
}
