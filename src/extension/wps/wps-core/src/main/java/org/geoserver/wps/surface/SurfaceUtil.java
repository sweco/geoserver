package org.geoserver.wps.surface;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterFactory;

/**
 * Utility classes for working with surfaces and rasters representing them
 * @author mdavis
 *
 */
public class SurfaceUtil 
{
    /**
     * Constructs a raster from the specified matrix of values,
     * in column-major (XY) order.
     * The input matrix is assumed to be non-ragged
     * (i.e. each column has the same height).
     * The logic supports inverting the Y-dimension,
     * in order to map from a world space to conventional image space.
     *
     * @param matrix   The matrix data in a {@code [column][row]} layout 
     *                  (column-major or XY order).
     * @return The new raster.
     *
     */
    public static WritableRaster createRaster(final float[][] matrix, boolean invertY) {
        // find maximum column length for the height
        int height = matrix[0].length;
        int width = matrix.length;
        // Need to use JAI raster factory, since WritableRaster
        // does not supports TYPE_FLOAT as of J2SE 1.5.0_06.
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int x = 0; x < width; x++) {
            final float[] col = matrix[x];
            for (int y = 0; y < col.length; y++) {
                int yy = y;
                // invert y axis
                if (invertY)
                    yy = height - 1 - y;
                raster.setSample(x, yy, 0, col[y]);
            }
        }
        return raster;
    }

    public static float[][] upsample(float[][] src, int width, int height)
    {
        int srcWidth = src.length;
        int srcHeight = src[0].length;
        
        float[][] dest = new float[width][height];
        
        for (int i = 0; i < width; i++) {
            int isrc = i * srcWidth / width; 
            for (int j = 0; j < height; j++) {
                int jsrc = j * srcHeight / height; 
                dest[i][j] = src[isrc][jsrc];
            }
        }
        return dest;
    }
}
