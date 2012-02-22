package org.geotools.coverage.grid;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.measure.unit.Unit;
import javax.media.jai.RasterFactory;

import org.geotools.coverage.GridSampleDimension;
import org.geotools.factory.Hints;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;

/**
 * Additional RenderedImage creation methods,
 * which supplement the ones available in {@link GridCoverageFactory}.
 * In particular, these methods allow creation of a RenderedImage 
 * from a raster without requiring an (unnecessary) grid envelope.
 * 
 * @author mdavis
 *
 */
public class RenderedImageFactoryTemp {

    public RenderedImageFactoryTemp() {
    }

    /**
     * Constructs a raster from the specified matrix of values.
     *
     * @param matrix   The matrix data in a {@code [row][column]} layout 
     *                  (row-major or YX order).
     * @return The new raster.
     *
     */
    public WritableRaster createRaster(final float[][] matrix) {
        int width = 0;
        int height = matrix.length;
        for (int j = 0; j < height; j++) {
            final float[] row = matrix[j];
            if (row != null) {
                if (row.length > width) {
                    width = row.length;
                }
            }
        }
        final WritableRaster raster;
        // Need to use JAI raster factory, since WritableRaster
        // does not supports TYPE_FLOAT as of J2SE 1.5.0_06.
        raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int j = 0; j < height; j++) {
            int i = 0;
            final float[] row = matrix[j];
            if (row != null) {
                for (; i < row.length; i++) {
                    raster.setSample(i, j, 0, row[i]);
                }
            }
            for (; i < width; i++) {
                raster.setSample(i, j, 0, Float.NaN);
            }
        }
        return raster;
    }

    /**
     * Constructs an image from the specified {@linkplain WritableRaster raster}. 
     * A default color palette is built from the minimal and
     * maximal values found in the raster.
     *
     * @param raster   The data raster (may be floating point numbers). {@linkplain Float#NaN NaN}
     *                 values are mapped to a transparent color.
     * @return The new image.
     */
    public RenderedImage create(final WritableRaster raster) {
        return create(raster, null, null, null, null, null);
    }

    /**
     * Constructs an image from the specified {@linkplain WritableRaster raster}.
     *     
     * @param raster      The data (may be floating point numbers). {@linkplain Float#NaN NaN}
     *                    values are mapped to a transparent color.
     * @param minValues   The minimal value for each band in the raster, or {@code null}
     *                    for computing it automatically.
     * @param maxValues   The maximal value for each band in the raster, or {@code null}
     *                    for computing it automatically.
     * @param units       The units of sample values, or {@code null} if unknown.
     * @param colors      The colors to use for values from {@code minValues} to {@code maxValues}
     *                    for each bands, or {@code null} for a default color palette. If non-null,
     *                    each arrays {@code colors[b]} may have any length; colors will be
     *                    interpolated as needed.
     * @param hints       An optional set of rendering hints, or {@code null} if none. Those hints
     *                    will not affect the image to be created. However, they may affect
     *                    the grid coverage to be returned by <code>{@link GridCoverage2D#geophysics
     *                    geophysics}(false)</code>, i.e. the view to be used at rendering time. The
     *                    optional {@link Hints#SAMPLE_DIMENSION_TYPE SAMPLE_DIMENSION_TYPE} hint
     *                    specifies the {@link SampleDimensionType} to be used at rendering time,
     *                    which can be one of {@link SampleDimensionType#UNSIGNED_8BITS UNSIGNED_8BITS}
     *                    or {@link SampleDimensionType#UNSIGNED_16BITS UNSIGNED_16BITS}.
     * @return The new image.
     *
     */
    public RenderedImage create(final WritableRaster raster, final double[] minValues,
            final double[] maxValues, final Unit<?> units, final Color[][] colors,
            final RenderingHints hints) {
        final GridSampleDimension[] bands = RenderedSampleDimension.create("dummy", raster,
                minValues, maxValues, units, colors, hints);
        final ColorModel model = bands[0].getColorModel(0, bands.length, raster.getSampleModel()
                .getDataType());
        return new BufferedImage(model, raster, false, null);
    }

}
