package org.geoserver.wps.surface;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.RenderedImageFactoryTemp;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.filter.Filter;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Stopwatch;

/**
 * A Process that computes a GridCoverage that is an interpolated surface over a set of irregular data points
 * using Barnes Analysis.
 * <p>
 * The implementation allows limiting the radius of influence of observations, in order to 
 * prevent extrapolation into unsupported areas, and to increase performance (by reducing
 * the number of observations considered).
 * <p>
 * It allows the surface raster to be computed at a lower resolution than the output coverage,
 * to improve performance.  Interpolation is used during upsampling to maintain visual quality.
 * <p>
 * This process can be used as a RenderingTransformation, since it
 * implements the {@link #invertQuery(Map, Query, GridGeometry)} method.
 * The query is rewritten to expand the query BBOX,
 * to ensure that enough data points are queried to make the 
 * computed surface stable under panning and zooming.  
 * 
 * <h3>Parameters</h3>
 * <i>M = mandatory, O = optional</i>
 * <p>
 * <ul>
 * <li><b>data</b> (M) - the FeatureCollection containing the point observations
 * <li><b>valueAttr</b> (M)- the attribute containing the observation value
 * <li><b>scale</b> (M) - the Length Scale for the interpolation.  In units of the input data CRS.
 * <li><b>convergence</b> (O) - the convergence factor for refinement.  Between 0 and 1 (values below 0.4 are safest).  (Default = 0.3)
 * <li><b>passes</b> (O) - the number of passes to compute.  1 or greater. (Default = 2) 
 * <li><b>minObservations</b> (O) - The minimum number of observations required to support a grid cell. (Default = 2)
 * <li><b>maxObservationDistance</b> (O) - The maximum distance to an observation for it to support a grid cell.  0 means all observations are used. (Default = 0)
 * <li><b>queryBufferDistance</b> (O) - The distance to expand the query envelope by. Larger values provide a more stable surface.  (Default = Length scale)
 * <li><b>noDatavalue</b> (O) - The NO_DATA value to use for unsupported grid cells in the output coverage.  (Default = -999)
 * <li><b>pixelsPerCell</b> (O) - The pixels-per-cell value determines the resolution of the computed grid. 
 * Larger values improve performance, but degrade appearance. (Default = 1)
 * </ul>
 * <p>
 * @author mdavis
 *
 */
@DescribeProcess(title = "BarnesSurface", description = "Interpolates a surface over a set of irregular data points using Barnes Analysis.")
public class BarnesSurfaceProcess implements GSProcess {

    /**------------------------
     * Per-request state.
     * RenderingTransformation processes are created on a per-SLD basis,
     * so state with a shorter lifetime must be stored in a thread-local.
     * --------------------------
     */
    private static ThreadLocal<GridGeometry> threadGridGeom = new ThreadLocal<GridGeometry>() {
    };

    /**--------------------------
     * Per-SLD state
     * --------------------------
     */
    private double lengthScale = 0.0;

    private double convergenceFactor = 0.3;
      
    private int passes  = 2;

    private int minObservationCount = 2;

    private double maxObservationDistance = 0.0;
    
    private double queryBufferDistance = 0;

    private float noDataValue = -999;

    private int pixelsPerCell = 1;

    @DescribeResult(name = "result", description = "The interpolated raster surface ")
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", description = "The features for the point observations to interpolate") SimpleFeatureCollection obs,
            @DescribeParameter(name = "valueAttr", description = "The feature attribute that contains the observed value", min=1, max=1) String valueAttr,
            @DescribeParameter(name = "scale", description = "The length scale to use for the interpolation", min=1, max=1) Double argScale,
            @DescribeParameter(name = "convergence", description = "The convergence factor for the interpolation", min=0, max=1) Double argConvergence,
            @DescribeParameter(name = "passes", description = "The number of passes to compute", min=0, max=1) Integer argPasses,
            @DescribeParameter(name = "minObservations", description = "The minimum number of observations required to support a grid cell", min=0, max=1) Integer argMinObsCount,
            @DescribeParameter(name = "maxObservationDistance", description = "The maximum distance to a supporting observation", min=0, max=1) Double argMaxObsDistance,
            @DescribeParameter(name = "queryBufferDistance", description = "The distance by which to expand the query window", min=0, max=1) Double argQueryBufferDist,
            @DescribeParameter(name = "noDatavalue", description = "The value to use for NO_DATA cells", min=0, max=1) Integer argNoDataValue,
            @DescribeParameter(name = "pixelsPerCell", description = "The number of pixels per grid cell", min=0, max=1) Integer argResolution,
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
        lengthScale = argScale;
        if (argConvergence != null) convergenceFactor = argConvergence;
        if (argPasses != null) passes = argPasses;
        if (argMinObsCount != null) minObservationCount = argMinObsCount;
        if (argMaxObsDistance != null) maxObservationDistance = argMaxObsDistance;
        if (argNoDataValue != null) noDataValue = argNoDataValue;
        if (argQueryBufferDist != null) {
            queryBufferDistance = argQueryBufferDist;
        }
        else {
            queryBufferDistance = heuristicQueryBufferDistance();
        }
        if (argResolution != null) {
            pixelsPerCell = argResolution;
            // ensure value is at least 1
            if (pixelsPerCell < 1)
                pixelsPerCell = 1;
        }
        
        Coordinate[] pts = null;
        try {
            pts = PointExtracter.extract(obs, valueAttr);
        } catch (CQLException e) {
            throw new ProcessException(e);
        }

        /**---------------------------------------------
         * Do the processing
         * ---------------------------------------------
         */
        Stopwatch sw = new Stopwatch();
        GridCoverage2D grid = computeSurfaceCoverage(pts);
        System.out.println("**************  Barnes Surface computed in " + sw.getTimeString());
        
        /**---------------------------------------------
         * Clean up after invocation
         * ---------------------------------------------
         */
        threadGridGeom.remove();
        
        return grid;
    }

    /**
     * To ensure that the computed surface is stable under zooming
     * and panning, the query BBOX must be expanded
     * to avoid obvious edge effects.
     * The expansion distance depends on the 
     * length scale, convergence factor, and data spacing in some complex way.
     * It does NOT depend on the output window extent.
     * It is chosen heuristically here.
     * 
     * @return the distance by which to expand the query BBOX
     */
    private double heuristicQueryBufferDistance()
    {
        return lengthScale;
    }
    

    private GridGeometry getQueryGridGeometry()
    {
        return threadGridGeom.get();
    }
    
    public GridCoverage2D computeSurfaceCoverage(Coordinate[] pts) {
        
        GridEnvelope gridEnv = getQueryGridGeometry().getGridRange();

        // image starts at 0, so add one to get true dimensions
        int width = gridEnv.getHigh(0) + 1;
        int height = gridEnv.getHigh(1) + 1;

        /*
         * // might be more efficient to write directly to a raster? WritableRaster raster = createDummyRaster(width, height);
         * RenderedImageFactoryTemp rif = new RenderedImageFactoryTemp(); RenderedImage img = rif.create(raster);
         */

        //float[][] matrix = createPointSymbolMatrixSafe(pts, width, height);
        float[][] matrix;
        try {
            matrix = createBarnesMatrix(pts, width, height);
        } catch (MismatchedDimensionException e) {
            throw new ProcessException(e);
        } catch (TransformException e) {
            throw new ProcessException(e);
        }

        // create the actual grid coverage to return
        return createGridCoverage(matrix);
    }

    private GridCoverage2D createGridCoverage(float[][] matrix) {
        RenderedImageFactoryTemp rif = new RenderedImageFactoryTemp();
        RenderedImage img = rif.create(SurfaceUtil.createRaster(matrix, true));

        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);
        CharSequence name = "Process Results";
        // GridCoverage2D grid = gcf.create(name, raster, resBounds);
        GridGeometry2D gridGeom2D = (GridGeometry2D) getQueryGridGeometry();
        GridCoverage2D grid = gcf.create(name, img, gridGeom2D, null, null, null);

        return grid;
    }

    private WritableRaster createDummyRaster(int width, int height) {
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width,
                height, 1, null);

        // zero out the raster
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, 0);
            }
        }

        // set a dummy set of values
        for (int y = height / 3; y < (2 * height) / 3; y++) {
            for (int x = width / 3; x < (2 * width) / 3; x++) {
                raster.setSample(x, y, 0, 5);
            }
        }
        return raster;
    }


    private float[][] createBarnesMatrix(Coordinate[] pts, int width, int height)
            throws MismatchedDimensionException, TransformException {
        BarnesInterpolator interp = new BarnesInterpolator(pts);
        interp.setLengthScale(lengthScale);
        interp.setConvergenceFactor(convergenceFactor);
        interp.setPassCount(passes);
        interp.setMinObservationCount(minObservationCount);
        interp.setMaxObservationDistance(maxObservationDistance);
        interp.setNoData(noDataValue);

        Envelope env = queryEnvelope(getQueryGridGeometry());
        float[][] grid = interp.computeSurface(env, width / pixelsPerCell, height / pixelsPerCell);
        float[][] outGrid = grid;
        if (pixelsPerCell > 1) {
            outGrid = BilinearInterpolator.interpolate(grid, width, height, noDataValue);
        }
        return outGrid;
    }

    private Envelope queryEnvelope(GridGeometry gridGeom) throws MismatchedDimensionException, TransformException
    {
        MathTransform gridToCRS = gridGeom.getGridToCRS();
        GridEnvelope gridEnv = gridGeom.getGridRange();
        
        DirectPosition2D src = new DirectPosition2D();
        src.setLocation(gridEnv.getLow(0), gridEnv.getLow(1));
        DirectPosition2D crsLow = new DirectPosition2D();
        gridToCRS.transform(src, crsLow);
        
        src.setLocation(gridEnv.getHigh(0), gridEnv.getHigh(1));
        DirectPosition2D crsHigh = new DirectPosition2D();
        gridToCRS.transform(src, crsHigh);

         return new Envelope(crsLow.x, crsHigh.x, crsHigh.y, crsLow.y);
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
     * 
     * @param targetQuery
     * @param gridGeometry
     * @return The transformed query
     */
    public Query invertQuery(Map<String, Object> input, Query targetQuery, GridGeometry gridGeometry)
            throws ProcessException {
        // find the output grid extent
        threadGridGeom.set(gridGeometry);

        // set the decimation hint to ensure points are read
        Hints hints = targetQuery.getHints();
        //Object gdo = hints.get(Hints.GEOMETRY_DISTANCE);
        hints.put(Hints.GEOMETRY_DISTANCE, 0.0);

        Filter expandedFilter = expandBBox(targetQuery.getFilter(), queryBufferDistance);
        targetQuery.setFilter(expandedFilter);
        
        // clear properties to force all attributes to be read
        // (required because the SLD processor cannot see the value attribute specified in the transformation)
        // TODO: set the properties to read only the value attribute
        targetQuery.setProperties(null);

        return targetQuery;
    }

    private Filter expandBBox(Filter filter, double distance) {
        return (Filter) filter.accept(
                new BBOXExpandingFilterVisitor(distance, distance, distance, distance), null);
    }

    /*
     
    // Test code to generate synthetic surfaces
     
    private float[][] createDummyMatrix(int width, int height) {
        float[][] matrix = new float[height][width];

        // set a dummy set of values
        for (int y = height / 3; y < (2 * height) / 3; y++) {
            for (int x = y; x < (2 * width) / 3; x++) {
                matrix[y][x] = 5;
            }
        }
        return matrix;
    }

    private float[][] createPointSymbolMatrixSafe(Coordinate[] pts, int width, int height) {
        try {
            return createPointSymbolMatrix(pts, width, height);
        } catch (MismatchedDimensionException e) {
            throw new ProcessException(e);
        } catch (TransformException e) {
            throw new ProcessException(e);
        }
    }

    private float[][] createPointSymbolMatrix(Coordinate[] pts, int width, int height)
            throws MismatchedDimensionException, TransformException {
        float[][] matrix = new float[height][width];

        // set NODATA values
        // zero out the raster
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matrix[y][x] = -999;
            }
        }

        MathTransform crsToGrid;
        try {
            crsToGrid = getQueryGridGeometry().getGridToCRS().inverse();
        } catch (NoninvertibleTransformException e) {
            throw new ProcessException(e);
        }

        DirectPosition2D src = new DirectPosition2D();
        DirectPosition2D dst = new DirectPosition2D();
        // set a dummy set of values
        for (Coordinate p : pts) {
            src.setLocation(p.x, p.y);
            crsToGrid.transform(src, dst);
            int x = (int) dst.getOrdinate(0);
            int y = (int) dst.getOrdinate(1);

            // fill a box around point
            for (int i = -5; i < 5; i++) {
                for (int j = -5; j < 5; j++) {
                    int xx = x + i;
                    int yy = y + j;
                    if (yy >= 0 && yy < height && xx >= 0 && xx < width)
                        matrix[yy][xx] = (float) p.z;
                }
            }
        }
        return matrix;
    }
*/
}
