package com.graphhopper.routing.weighting.custom;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
public class ReadGeotiff {

    private final static String url = "C:\\D\\Sources\\tutorial\\src\\main\\java\\org\\geotools\\tutorial\\quickstart\\aireal_90_4326.tif";

    public static double getValue(double x, double y, GridCoverage2D grid, Raster gridData) throws Exception {
        try {
            GridGeometry2D gg = grid.getGridGeometry();
            DirectPosition2D posWorld = new DirectPosition2D(x, y);
            GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

            // envelope is the size in the target projection
            double[] pixel = new double[1];
            double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
            return data[0] > 0 ? data[0] / 15 : 0;
        }catch (Exception e)
        {
            // If catch, its because pixel is outside geotiff
            return 0;
        }
    }

}
