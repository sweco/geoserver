package org.geoserver.wps.surface;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A gridded coordinate system defined by an envelope and a X & Y size.
 * 
 * @author mdavis
 *
 */
public class GridSystem {
        
        private Envelope env;
        private int xSize;
        private int ySize;
        private double dx;
        private double dy;
        
        public GridSystem(Envelope env, int xSize, int ySize)
        {
                this.env = env;
                this.xSize = xSize;
                this.ySize = ySize;
                dx = env.getWidth() / (xSize - 1);
                dy = env.getHeight() / (ySize - 1);
        }
        
        public int numCellX() { return xSize; }
        public int numCellY() { return ySize; }
        
        public double x(int i)
        {
                if (i >= xSize - 1) return env.getMaxX();
                return env.getMinX() + i * dx;
        }
        
        public double y(int i)
        {
                if (i >= ySize - 1) return env.getMaxY();
                return env.getMinY() + i * dy;
        }
        
        public int i(double x)
        {
                if (x >= env.getMaxX()) return xSize - 1;
                if (x < env.getMinX()) return -1;
                int i = (int) ((x - env.getMinX()) / dx);
                // have already check x is in bounds, so ensure returning a valid value
                if (i >= xSize) i = xSize - 1;
                return i;
        }
        
        public int j(double y)
        {
                if (y >= env.getMaxY()) return ySize - 1;
                if (y < env.getMinY()) return -1;
                int j = (int) ((y - env.getMinY()) / dy);
                // have already check x is in bounds, so ensure returning a valid value
                if (j >= ySize) j = ySize - 1;
                return j;
        }

}
