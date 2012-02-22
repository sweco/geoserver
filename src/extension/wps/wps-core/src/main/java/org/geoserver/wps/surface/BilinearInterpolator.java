package org.geoserver.wps.surface;

/**
 * Interpolates a grid to a grid of different dimensions using bilinear interpolation.
 * <p>
 * NO_DATA cell values are supported in the input grid.  
 * They are handled by using a value of NO_DATA if any input cell is NO_DATA.
 * This is simple and fast, but does make the data edges look a bit ragged.
 * <p>
 * Reference: http://en.wikipedia.org/wiki/Bilinear_interpolation.
 * 
 * @author mdavis
 * 
 */
public class BilinearInterpolator {
    
    /**
     * Interpolates a grid to a grid of different dimensions
     * using bilinear interpolation.
     * <p>
     * NO_DATA cell handling is done by using a value of 
     * NO_DATA if any input cell is NO_DATA.
     * <p>
     * Reference: http://en.wikipedia.org/wiki/Bilinear_interpolation.
     * 
     * @param src the source grid
     * @param width the width of the destination grid
     * @param height the height of the destination grid
     * @param noDataValue the NO_DATA value
     * @return
     */
    public static float[][] interpolate(final float[][] src, final int width, final int height, final float noDataValue)
    {
        int srcWidth = src.length;
        int srcHeight = src[0].length;
        
        float[][] dest = new float[width][height];
        
        float xRatio = ((float) srcWidth) / width ;
        float yRatio = ((float) srcHeight) / height ;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                float x = xRatio * i;
                float y = yRatio * j;
                int ix = (int) x;
                int iy = (int) y;
                float xfrac = x - ix;
                float yfrac = y - iy;

                float val;
                if (ix+1 < srcWidth && iy+1 < srcHeight) {
                    float v00 = src[ix][iy];
                    float v10 = src[ix + 1][iy];
                    float v01 = src[ix][iy + 1];
                    float v11 = src[ix + 1][iy + 1];
                    if (v00 == noDataValue 
                            || v10 == noDataValue
                            || v01 == noDataValue
                            || v11 == noDataValue) {
                        val = noDataValue;
                        }
                    else {
                        val = ( v00*(1-xfrac)*(1-yfrac) +  v10*(xfrac)*(1-yfrac) +
                                v01*(yfrac)*(1-xfrac)   +  v11*(xfrac*yfrac)
                                ) ;
                    }
                }
                else {
                    val = src[ix][iy];
                }
                dest[i][j] = val;
            }
        }
        return dest;
    }

    
}
