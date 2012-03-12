/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.surface;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.spatial.BBOX;

/**
 * A {@link DuplicatingFilterVisitor} which expands the {@link BBOX} of the filter
 * by given distances for each box edge.
 * 
 * @author mdavis
 *
 */
class BBOXExpandingFilterVisitor extends DuplicatingFilterVisitor {
    private double expandMinX;

    private double expandMaxX;

    private double expandMinY;

    private double expandMaxY;

    /**
     * Creates a new expanding filter.
     * 
     * @param expandMinX the distance to expand the box X dimension leftwards
     * @param expandMaxX the distance to expand the box X dimension rightwards
     * @param expandMinY the distance to expand the box Y dimension downwards
     * @param expandMaxY the distance to expand the box Y dimension upwards
     */
    public BBOXExpandingFilterVisitor(double expandMinX, double expandMaxX, double expandMinY,
            double expandMaxY) {
        this.expandMinX = expandMinX;
        this.expandMaxX = expandMaxX;
        this.expandMinY = expandMinY;
        this.expandMaxY = expandMaxX;
    }
    
    /**
     * Expands the BBOX in the Filter.
     * 
     */
    @SuppressWarnings("deprecation")
    @Override
    public Object visit(BBOX filter, Object extraData) {
        // no need to change the property name
       Expression propertyName = filter.getExpression1();
       
       /**
        * Using the deprecated methods since they are too useful...
        */
        double minx = filter.getMinX();
        double miny = filter.getMinY();
        double maxx = filter.getMaxX();
        double maxy = filter.getMaxY();
        String srs = filter.getSRS();
        
        return getFactory(extraData).bbox(propertyName, 
                minx - expandMinX, miny - expandMaxX,
                maxx + expandMinY, maxy + expandMaxY, 
                srs);
    }

}