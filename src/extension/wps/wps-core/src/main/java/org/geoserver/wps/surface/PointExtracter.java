package org.geoserver.wps.surface;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;

public class PointExtracter 
{

    public PointExtracter() {
    }

    public static Coordinate[] extract(SimpleFeatureCollection obsPoints, String attrName) throws CQLException 
    {
        Expression attrExpr = ECQL.toExpression(attrName);
        List<Coordinate> ptList = new ArrayList<Coordinate>();
        SimpleFeatureIterator obsIt = obsPoints.features();
        
        int i = 0;
        try {
            while (obsIt.hasNext()) {
                SimpleFeature feature = obsIt.next();
                
                double val = 0;
                
                try {
                    // get the observation value from the attribute (if non-null)
                    Object valObj = attrExpr.evaluate(feature);
                    if (valObj != null) {
                        // System.out.println(evaluate);
                        Number valNum = (Number) valObj;
                        val = valNum.doubleValue();
                        
                        // get the point location from the geometry
                        Geometry geom = (Geometry) feature.getDefaultGeometry();
                        Coordinate p = geom.getCoordinate();
                        Coordinate pobs = new Coordinate(p.x, p.y, val);
                        ptList.add(pobs);
                    }
                }
                catch (Exception e) {
                    // just carry on for now (debugging)
                    //throw new ProcessException("Expression " + attrExpr + " failed to evaluate to a numeric value", e);
                }
            }
        }
        finally {
            obsPoints.close( obsIt );
        }

        Coordinate[] pts = CoordinateArrays.toCoordinateArray(ptList);
        return pts;
    }
}
