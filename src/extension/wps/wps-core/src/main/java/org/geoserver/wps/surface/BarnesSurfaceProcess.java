/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.surface;

import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A Process that uses Barnes Analysis to compute an interpolated surface 
 * over a set of irregular data points as a GridCoverage.
 * <p>
 * The implementation allows limiting the radius of influence of observations, in order to 
 * prevent extrapolation into unsupported areas, and to increase performance (by reducing
 * the number of observations considered).
 * <p>
 * The surface grid can be computed at a lower resolution than the requested output image.
 * The grid is upsampled to match the required image size.  
 * Interpolation is used during upsampling to maintain visual quality.
 * This gives a large improvement in performance, with minimal impact on visual quality for small cell sizes.
 * <p>
 * This process can be used as a RenderingTransformation, since it
 * implements the <tt>invertQuery(... Query, GridGeometry)</tt> method.
 * 
 * The query is rewritten to expand the query BBOX,
 * to ensure that enough data points are queried to make the 
 * computed surface stable under panning and zooming.  
 * To ensure that the computed surface is stable 
 * under zooming and panning (i.e. does not display obvious edge effects), 
 * the query BBOX must be expanded to include a larger data area.
 * The expansion distance depends on the 
 * length scale, convergence factor, and data spacing 
 * in a complex way, so must be manually determined.
 * It does NOT depend on the output window extent.
 * (A good heuristic is to set it expand by at least the length scale.)
 * 
 * <h3>Parameters</h3>
 * <i>M = mandatory, O = optional</i>
 * <p>
 * <ul>
 * <li><b>data</b> (M) - the FeatureCollection containing the point observations
 * <li><b>valueAttr</b> (M)- the feature type attribute containing the observed surface value
 * <li><b>scale</b> (M) - the Length Scale for the interpolation.  In units of the input data CRS.
 * <li><b>convergence</b> (O) - the convergence factor for refinement.  Between 0 and 1 (values below 0.4 are safest).  (Default = 0.3)
 * <li><b>passes</b> (O) - the number of passes to compute.  1 or greater. (Default = 2) 
 * <li><b>minObservations</b> (O) - The minimum number of observations required to support a grid cell. (Default = 2)
 * <li><b>maxObservationDistance</b> (O) - The maximum distance to an observation for it to support a grid cell.  0 means all observations are used. (Default = 0)
 * <li><b>noDatavalue</b> (O) - The NO_DATA value to use for unsupported grid cells in the output coverage.  (Default = -999)
 * <li><b>pixelsPerCell</b> (O) - The pixels-per-cell value determines the resolution of the computed grid. 
 * Larger values improve performance, but degrade appearance. (Default = 1)
 * <li><b>queryBuffer</b> (O) - The distance to expand the query envelope by. Larger values provide a more stable surface.  (Default = 0)
 * <li><b>destBBOX</b> (M) - The destination bounding box (env var = <tt>wms_bbox</tt>)
 * <li><b>imageWidth</b> (M) - The final image width (env var = <tt>wms_width</tt>)
 * <li><b>imageHeight</b> (M) - The final image height (env var = <tt>wms_height</tt>)
 * </ul>
 * If used as a RenderingTransformation, the destination image parameters can be obtained 
 * in the SLD by using the <tt>env</tt> function, with the variable names as specified above.
 * <p>
 * The output of the process is a {@linkplain GridCoverage2D} with a single band, containing cells with
 * values in the same domain as the input observation field specified by <code>valueAttr</code>.
 * <p>
 * @author mdavis
 *
 */
@DescribeProcess(title = "BarnesSurface", description = "Uses Barnes Analysis to compute an interpolated surface over a set of irregular data points aa a GridCoverage.")
public class BarnesSurfaceProcess implements GSProcess {

    // no process state is defined, since RenderingTransformation processes must be stateless
    
    @DescribeResult(name = "result", description = "The interpolated surface as a raster")
    public GridCoverage2D execute(
            
            // process data
            @DescribeParameter(name = "data", description = "Features containing the point observations to be interpolated") SimpleFeatureCollection obs,
            
            // process parameters
            @DescribeParameter(name = "valueAttr", description = "Featuretype attribute containing the observed surface value", min=1, max=1) String valueAttr,
            @DescribeParameter(name = "scale", description = "Length scale to use for the interpolation", min=1, max=1) Double argScale,
            @DescribeParameter(name = "convergence", description = "Convergence factor for the interpolation (default: 0.3)", min=0, max=1) Double argConvergence,
            @DescribeParameter(name = "passes", description = "Number of passes to compute (default: 2)", min=0, max=1) Integer argPasses,
            @DescribeParameter(name = "minObservations", description = "Minimum number of observations required to support a grid cell (default: 2)", min=0, max=1) Integer argMinObsCount,
            @DescribeParameter(name = "maxObservationDistance", description = "Maximum distance to a supporting observation (default: 0)", min=0, max=1) Double argMaxObsDistance,
            @DescribeParameter(name = "noDatavalue", description = "Value to use for NO_DATA cells (default: -999)", min=0, max=1) Integer argNoDataValue,
            @DescribeParameter(name = "pixelsPerCell", description = "Number of pixels per grid cell (default =: 1)", min=0, max=1) Integer argPixelsPerCell,
            
            // query modification parameters
            @DescribeParameter(name = "queryBuffer", description = "Distance by which to expand the query window (default: 0)", min=0, max=1) Double argQueryBuffer,

            // output image parameters
            @DescribeParameter(name = "destBBOX", description = "Destination bounding box") ReferencedEnvelope argDestEnv,
            @DescribeParameter(name = "imageWidth", description = "Final image width") Integer argImageWidth,
            @DescribeParameter(name = "imageHeight", description = "Final image height") Integer argImageHeight,
            
            ProgressListener monitor) throws ProcessException {

        /**---------------------------------------------
         * Check that process arguments are valid
         * ---------------------------------------------
         */
        if (valueAttr == null || valueAttr == "") {
            throw new IllegalArgumentException("Value attribute was not specified");
        }

        /**---------------------------------------------
         * Set up required information from process arguments.
         * ---------------------------------------------
         */
        double lengthScale = argScale;
        double convergenceFactor = argConvergence != null ? argConvergence : 0.3;
        int passes = argPasses != null ? argPasses : 2;
        int minObsCount = argMinObsCount != null ? argMinObsCount : 2;
        double maxObsDistance = argMaxObsDistance != null ? argMaxObsDistance : 0.0;
        float noDataValue = argNoDataValue != null ? argNoDataValue : -999;
        int pixelsPerCell = 1;
        if (argPixelsPerCell != null && argPixelsPerCell > 1) {
            pixelsPerCell = argPixelsPerCell;
        }
        int imageWidth = argImageWidth;
        int imageHeight = argImageHeight;
        int gridWidth = imageWidth;
        int gridHeight = imageHeight;
        if (pixelsPerCell > 1) {
            gridWidth = imageWidth / pixelsPerCell;
            gridHeight = imageHeight / pixelsPerCell;
        }
        
        /**---------------------------------------------
         * Convert the input data
         * ---------------------------------------------
         */
        Coordinate[] pts = null;
        try {
            pts = extract(obs, valueAttr);
        } catch (CQLException e) {
            throw new ProcessException(e);
        }

        /**---------------------------------------------
         * Do the processing
         * ---------------------------------------------
         */
        //Stopwatch sw = new Stopwatch();
        // interpolate the surface at the specified resolution
        float[][] barnesGrid = createBarnesGrid(pts, lengthScale, convergenceFactor, passes, minObsCount, maxObsDistance, noDataValue, argDestEnv, gridWidth, gridHeight);
        
        // flip now, since grid size may be smaller
        barnesGrid = flipXY(barnesGrid);
        
        // upsample to output resolution if necessary
        float[][] outGrid = barnesGrid;
        if (pixelsPerCell > 1)
            outGrid = upsample(barnesGrid, noDataValue, imageWidth, imageHeight);
        
        // convert to the GridCoverage2D required for output
        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage2D gridCov = gcf.create("Process Results", outGrid, argDestEnv);
        
        //System.out.println("**************  Barnes Surface computed in " + sw.getTimeString());
        
        return gridCov;
    }    

    /**
     * Flips an XY matrix along the X=Y axis, and inverts the Y axis.
     * Used to convert from "map orientation" into the "image orientation"
     * used by GridCoverageFactory.
     * The surface interpolation is done on an XY grid, with Y=0 being the bottom of the space.
     * GridCoverages are stored in an image format, in a YX grid with 0 being the top.
     * 
     * @param grid the grid to flip
     * @return the flipped grid
     */
    private float[][] flipXY(float[][] grid)
    {
        int xsize = grid.length;
        int ysize = grid[0].length;

        float[][] grid2 = new float[ysize][xsize];
        for (int ix = 0; ix < xsize; ix++) {
            for (int iy = 0; iy < ysize; iy++) {
                int iy2 = ysize - iy - 1;
                grid2[iy2][ix] = grid[ix][iy];
            }
        }
        return grid2;
    }
    
    private float[][] createBarnesGrid(Coordinate[] pts, 
            double lengthScale,
            double convergenceFactor,
            int passes,
            int minObservationCount,
            double maxObservationDistance,
            float noDataValue,
            Envelope destEnv,
            int width, int height)
    {
        BarnesSurfaceInterpolator barnesInterp = new BarnesSurfaceInterpolator(pts);
        barnesInterp.setLengthScale(lengthScale);
        barnesInterp.setConvergenceFactor(convergenceFactor);
        barnesInterp.setPassCount(passes);
        barnesInterp.setMinObservationCount(minObservationCount);
        barnesInterp.setMaxObservationDistance(maxObservationDistance);
        barnesInterp.setNoData(noDataValue);

        float[][] grid = barnesInterp.computeSurface(destEnv, width, height);
        
        return grid;
    }

    private float[][] upsample(float[][] grid,
            float noDataValue, 
            int width,
            int height) {
        BilinearInterpolator bi = new BilinearInterpolator(grid, noDataValue);
        float[][] outGrid = bi.interpolate(width, height, true);
        return outGrid;
    }

    /**
     * Given a target query and a target grid geometry 
     * returns the query to be used to read the input data of the process involved in rendering. In
     * this process this method is used to:
     * <ul>
     * <li>determine the extent & CRS of the output grid
     * <li>expand the query envelope to ensure stable surface generation
     * <li>modify the query hints to ensure point features are returned
     * </ul>
     * Note that in order to pass validation, all parameters named here must also appear 
     * in the parameter list of the <tt>execute</tt> method,
     * even if they are not used there.
     * 
     * @param valueAttr the feature type attribute that contains the observed surface value
     * @param argQueryBuffer the distance by which to expand the query window
     * @param targetQuery the query used against the data source
     * @param targetGridGeometry the grid geometry of the destination image
     * @return The transformed query
     */
    public Query invertQuery(
            @DescribeParameter(name = "valueAttr", description = "The feature type attribute that contains the observed surface value", min=1, max=1) String valueAttr,
            @DescribeParameter(name = "queryBuffer", description = "The distance by which to expand the query window", min=0, max=1) Double argQueryBuffer,
            Query targetQuery, GridGeometry targetGridGeometry)
            throws ProcessException {
        
        // default is no expansion
        double queryBuffer = 0;
        if (argQueryBuffer != null) {
            queryBuffer = argQueryBuffer;
        }

        targetQuery.setFilter(expandBBox(targetQuery.getFilter(), queryBuffer));
        
        // clear properties to force all attributes to be read
        // (required because the SLD processor cannot see the value attribute specified in the transformation)
        // TODO: set the properties to read only the specified value attribute
        targetQuery.setProperties(null);
        
        // set the decimation hint to ensure points are read
        Hints hints = targetQuery.getHints();
        hints.put(Hints.GEOMETRY_DISTANCE, 0.0);

        return targetQuery;
    }

    private Filter expandBBox(Filter filter, double distance) {
        return (Filter) filter.accept(
                new BBOXExpandingFilterVisitor(distance, distance, distance, distance), null);
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
            obsIt.close();
        }

        Coordinate[] pts = CoordinateArrays.toCoordinateArray(ptList);
        return pts;
    }

}
