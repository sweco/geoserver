/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.surface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Interpolates a surface across a regular grid from an irregular set of data points using the Barnes Interpolation technique.
 * <p>
 * Barnes Interpolation is a surface estimating technique which uses an initial pass to provide a smoothed estimate of the surface, and optionally
 * further refinement passes to improve the surface to make it more closely match the observation values.
 * <ul>
 * <li>The initial pass produces an averaged (smoothed) surface, using a summation of exponential (Gaussian) decay functions around each observation
 * point.
 * <li>Subsequent refinement passes compute an error surface using the delta between the previous estimated surface and the observations. The error
 * surface is added to the previous estimate surface to refined the estimate (by reducing the delta to the observations).
 * <p>
 * For the first pass, the estimated value at each grid point is:
 * 
 * <pre>
 * Eg = sum(wi * oi) / sum(wi)
 * </pre>
 * 
 * where
 * <ul>
 * <li>Eg is the estimated surface value at the grid point
 * <li>wi is the weight value for the i'th observation point (see below for definition)
 * <li>oi is the value of the i'th observation point
 * </ul>
 * <p>
 * The weight (decay) function used is:
 * 
 * <pre>
 * wi = exp(-di &circ; 2 / L &circ; 2 * c)
 * </pre>
 * 
 * where:
 * <ul>
 * <li>wi is the <b>weight</b> of the i'th observation point value
 * <li>di is the <b>distance</b> from the grid point being estimated to the i'th observation point
 * <li>L is the <b>length scale</b>, which is determined by the observation spacing and the natural scale of the phenomena being measured. The length
 * scale is in the units of the coordinate system of the data grid. It will likely need to be empirically estimated.
 * <li>c is the <b>convergence factor</b>, which controls how much refinement takes place during each refinement step. In the first pass the
 * convergence is automatically set to 1. For subsequent passes a value in the range 0.2 - 0.3 is usually effective.
 * </ul>
 * During refinement passes the estimate at each grid point is computed as:
 * 
 * <pre>
 *  Eg' = Eg + sum( wi * (oi - Ei) ) / sum( wi )
 * </pre>
 * 
 * To optimize performance for large input datasets, it is necessary to only provide the data points which affect the surface interpolation within the
 * specified output extent. In order to avoid "edge effects", the provided data points should be taken from an area somewhat larger than the output
 * extent. The current best advice for determining the size of the extended data window is to use a buffer value of 2L.
 * <p>
 * Since the visual quality and accuracy of the estimation is lower far from valid observations, the algorithm allows limiting the extent of the
 * computed surface. The computed surface is limited in extent by using the concept of <b>supported grid points</b>. Grid points are supported by the
 * input observations if they are within a specified distance of a specified number of observation points. Grid points which are not supported are not
 * computed and are output as NO_DATA values.
 * <p>
 * <b>References</b>
 * <ol>
 * <li>Barnes, S. L (1964). "A technique for maximizing details in numerical weather-map analysis". <i>Journal of Applied Meterology</i> 3 (4):
 * 396Ð409
 * </ol>
 * 
 * @author mdavis
 * 
 */
public class BarnesSurfaceInterpolator {

    /**
     * The default grid cell value used to indicate no data was computed for that cell
     */
    public static final float DEFAULT_NO_DATA_VALUE = -999;

    private static final double INTERNAL_NO_DATA = Double.NaN;

    // =========== Input parameters
    /**
     * These parameters control which grid points are considered to be supported, i.e. have enough nearby observation points to be reasonably
     * estimated.
     * 
     * A grid point is supported if it has:
     * 
     * count(obs within maxObservationDistance) >= minObservationCount
     * 
     * Using these parameters is optional, but recommended, since estimating grid points which are far from any observations can produce unrealistic
     * surfaces.
     */
    private int minObservationCount = 2;

    private double maxObservationDistance = 0.0;

    private double convergenceFactor = 0.3;

    private double lengthScale = 0.0;

    private int passCount = 1;

    private Coordinate[] inputObs;

    // ============= Internal parameters (could be exposed)
    private float noDataValue = DEFAULT_NO_DATA_VALUE;

    // ============= Computed parameters
    // private double effectiveRadius;

    /**
     * Indicates whether estimated grid points are filtered based on distance from observations
     */
    private boolean useObservationMask;

    // ============ Working data
    private float[] estimatedObs;

    /**
     * Creates a Barnes Interpolator over a specified dataset of observation values. The observation data is provided as an array of
     * {@link Coordinate} values, where the X,Y ordinates are the observation location, and the Z ordinate contains the observation value.
     * 
     * @param data the observed data values
     */
    public BarnesSurfaceInterpolator(Coordinate[] observationData) {
        this.inputObs = observationData;
    }

    /**
     * Sets the number of passes performed during Barnes interpolation. 
     * 
     * @param passCount the number of estimation passes to perform (1 or more)
     */
    public void setPassCount(int passCount) {
        if (passCount < 1)
            return;
        this.passCount = passCount;
    }

    /**
     * Sets the length scale for the interpolation weighting function. The length scale is determined from the distance between the observation
     * points, as well as the scale of the phenomena which is being measured.
     * <p>
     * 
     * 
     * @param lengthScale
     */
    public void setLengthScale(double lengthScale) {
        this.lengthScale = lengthScale;
    }

    /**
     * Sets the convergence factor used during refinement passes. The value should be in the range [0,1]. Empirically, values between 0.2 - 0.3 are
     * most effective. Smaller values tend to make the interpolated surface too "jittery". Larger values produce less refinement effect.
     * 
     * @param convergenceFactor the factor determining how much to refine the surface estimate
     */
    public void setConvergenceFactor(double convergenceFactor) {
        this.convergenceFactor = convergenceFactor;
    }

    /**
     * Sets the maximum distance from an observation for a grid point to be supported by that observation. Empirically determined; a reasonable
     * starting point is between 1.5 and 2 times the Length scale. If the value is 0 (which is the default), all grid points are considered to be
     * supported, and will thus be computed.
     * 
     * @param maxObsDistance the maximum distance from an observation for a supported grid point
     */
    public void setMaxObservationDistance(double maxObsDistance) {
        this.maxObservationDistance = maxObsDistance;
    }

    /**
     * Sets the minimum number of in-range observations which are required for a grid point to be supported. The default is 2.
     * 
     * @param minObsCount the minimum in-range observation count for supported grid points
     */
    public void setMinObservationCount(int minObsCount) {
        this.minObservationCount = minObsCount;
    }

    /**
     * Sets the NO_DATA value used to indicate that a grid cell was not computed. This value should be distinct from any potential data value.
     * 
     * @param noDataValue the value to use to represent NO_DATA.
     */
    public void setNoData(float noDataValue) {
        this.noDataValue = noDataValue;
    }

    /**
     * Computes the estimated values for a regular grid of cells. The area covered by the grid is specified by an {@link Envelope}. The size of the
     * grid is specified by the cell count for the grid width (X) and height (Y).
     * 
     * @param srcEnv the area covered by the grid
     * @param xSize the width of the grid
     * @param ySize the height of the grid
     * 
     * @return the computed grid of estimated data values (in row-major order)
     */
    public float[][] computeSurface(Envelope srcEnv, int xSize, int ySize) {
        // not currently used
        // effectiveRadius = effectiveRadius(minimumWeight, influenceRadius);

        useObservationMask = minObservationCount > 0 && maxObservationDistance > 0.0;

        float[][] grid = new float[xSize][ySize];
        GridTransform trans = new GridTransform(srcEnv, xSize, ySize);

        grid = estimatedGrid(grid, trans);

        if (passCount > 1) {
            /**
             * First refinement pass requires observation points to be estimated as well
             */
            estimatedObs = computeEstimatedObservations();
            refineGrid(grid, trans);
            
            /**
             * For subsequent refinement passes, refine observations then recompute
             */
            for (int i = 3; i <= passCount; i++) {
                estimatedObs = refineEstimatedObservations(estimatedObs);
                refineGrid(grid, trans);              
            }
        }
        return grid;
    }

    private float[] computeEstimatedObservations() {
        float[] estimate = new float[inputObs.length];
        for (int i = 0; i < inputObs.length; i++) {
            Coordinate dp = inputObs[i];
            float est = (float) estimatedValue(dp.x, dp.y);
            if (! Float.isNaN(est))
                estimate[i] = est;
            else
                estimate[i] = (float) inputObs[i].z;
        }
        return estimate;
    }

    private float[] refineEstimatedObservations(float[] currEst) {
        float[] estimate = new float[inputObs.length];
        for (int i = 0; i < inputObs.length; i++) {
            Coordinate dp = inputObs[i];
            float del = (float) refinedDelta(dp.x, dp.y, convergenceFactor);
            if (! Float.isNaN(del))
                estimate[i] = (float) currEst[i] + del;
            else
                estimate[i] = (float) inputObs[i].z;
        }
        return estimate;
    }

    /**
     * Computes an interpolated surface for a given window in the data space.
     * 
     * @param srcEnv
     * @param xSize
     * @param ySize
     * @return a grid with indices [X][Y]
     */
    private float[][] estimatedGrid(float[][] grid, GridTransform trans) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                double x = trans.x(i);
                double y = trans.y(j);

                grid[i][j] = (float) noDataValue;
                if (useObservationMask && !isSupportedGridPt(x, y))
                    continue;

                float est = (float) estimatedValue(x, y);
                if (!Float.isNaN(est))
                    grid[i][j] = est;
            }
        }
        return grid;
    }

    private float[][] refineGrid(float[][] grid, GridTransform trans) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                double x = trans.x(i);
                double y = trans.y(j);

                // skip NO_DATA values
                if (grid[i][j] == noDataValue)
                    continue;

                float del = (float) refinedDelta(x, y, convergenceFactor);
                /*
                 // DEBUGGING
                if (del < 0) {
                    float d = (float) refinedDelta(x, y, convergenceFactor);
                }
                */
                if (! Float.isNaN(del))
                    grid[i][j] = grid[i][j] + del;
            }
        }
        return grid;
    }

    private boolean isSupportedGridPt(double x, double y) {
        int count = 0;
        for (int i = 0; i < inputObs.length; i++) {
            double dist = distance(x, y, inputObs[i]);
            if (dist <= maxObservationDistance)
                count++;
        }
        return count >= minObservationCount;
    }

    private double distance(double x, double y, Coordinate p) {
        double dx = x - p.x;
        double dy = y - p.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Computes the initial estimate for a grid point.
     * 
     * @param x the x ordinate of the grid point location
     * @param y the y ordinate of the grid point location
     * @return the estimated value, or INTERNAL_NO_DATA if the grid cell is not supported
     */
    private double estimatedValue(double x, double y) {
        Coordinate p = new Coordinate(x, y);

        double sumWgtVal = 0;
        double sumWgt = 0;
        int dataCount = 0;
        for (int i = 0; i < inputObs.length; i++) {
            double wgt = weight(p, inputObs[i], lengthScale);
            /**
             * Skip observation if unusable due to too great a distance
             */
            if (Double.isNaN(wgt))
                continue;

            sumWgtVal += wgt * inputObs[i].z;
            sumWgt += wgt;
            dataCount++;
        }
        /**
         * If grid point is not supported, return NO_DATA
         */
        if (dataCount < minObservationCount)
            return INTERNAL_NO_DATA;
        return sumWgtVal / sumWgt;
    }

    /**
     * Computes a refinement delta, which is added to a grid point estimated value
     * to refine the estimate.
     * 
     * @param x the x ordinate of the grid point location
     * @param y the y ordinate of the grid point location
     * @param convergenceFactor the convergence factor
     * @return the refinement delta value, or INTERNAL_NO_DATA if the grid cell is not supported
     */
    private double refinedDelta(double x, double y, double convergenceFactor) {
        Coordinate p = new Coordinate(x, y);

        double sumWgtVal = 0;
        double sumWgt = 0;
        int dataCount = 0;
        for (int i = 0; i < inputObs.length; i++) {
            double wgt = weight(p, inputObs[i], lengthScale, convergenceFactor);
            /**
             * Check if observation is unusable (e.g. due to too great a distance)
             */
            if (Double.isNaN(wgt))
                continue;

            sumWgtVal += wgt * (inputObs[i].z - estimatedObs[i]);
            sumWgt += wgt;
            dataCount++;
        }
        /**
         * If grid point is not supported, return NO_DATA
         */
        if (dataCount < minObservationCount)
            return INTERNAL_NO_DATA;
        return sumWgtVal / sumWgt;
    }

    private double weight(Coordinate dataPt, Coordinate gridPt, double lengthScale) {
        return weight(gridPt, dataPt, lengthScale, 1.0);
    }

    private double weight(Coordinate dataPt, Coordinate gridPt, double lengthScale,
            double convergenceFactor) {
        double dist = dataPt.distance(gridPt);
        return weight(dist, lengthScale, convergenceFactor);
    }

    private double weight(double dist, double lengthScale, double convergenceFactor) {
        /**
         * MD - using an effective radius is problematic.
         * 
         * The effective radius grows as a log function of the cutoff weight, so even for very small cutoff weight values, the effective radius is
         * only a few times the size of the influence radius.
         * 
         * Also, dropping observation terms from the estimate results in very drastic (discontinuous) changes at distances around the effective
         * radius. (Probably because beyond that distance there are very few terms (maybe only 2) contributing to the estimate, so there is no
         * smoothing effect from incorporating many estimates)
         * 
         * So - don't use effectiveRadius.
         * 
         * Or, maybe it's ok as long as a observation mask is used as well, since the effect only occurs at large distances from observation points?
         */
        /*
         * if (dist > effectiveRadius) return INTERNAL_NO_DATA; //
         */
        double dr = dist / lengthScale;
        double w = Math.exp(-(dr * dr / convergenceFactor));
        // if (dist > cutoffRadius) System.out.println(w);
        return w;
    }

    /**
     * Computes effective radius which is determined by the specified cutoff weight and the radius of the decay function.
     * 
     * @param cutoffWeight
     * @param radius
     * @return
     */
    private double effectiveRadius(double cutoffWeight, double radius) {
        double cutoffFactor = Math.sqrt(-Math.log(cutoffWeight));
        double effRadius = radius * cutoffFactor;
        double w = weight(effRadius, radius, 1.0);
        System.out.println(cutoffWeight + "   " + w);
        return effRadius;
    }
    
    private static class GridTransform {

        private Envelope env;

        private int xSize;

        private int ySize;

        private double dx;

        private double dy;

        public GridTransform(Envelope env, int xSize, int ySize) {
            this.env = env;
            this.xSize = xSize;
            this.ySize = ySize;
            dx = env.getWidth() / (xSize - 1);
            dy = env.getHeight() / (ySize - 1);
        }

        public double x(int i) {
            if (i >= xSize - 1)
                return env.getMaxX();
            return env.getMinX() + i * dx;
        }
        public double y(int i)
        {
                if (i >= ySize - 1) return env.getMaxY();
                return env.getMinY() + i * dy;
        }

    }
}
