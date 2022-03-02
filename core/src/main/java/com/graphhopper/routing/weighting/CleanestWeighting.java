package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.custom.ReadGeotiff;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Calculates the cleanest route - independent of a vehicle as the calculation is based on the
 * distance and air quality of edges.
 * Air quality is extracted from .geotiff file, and is in this interval [0, 90]
 *
 * <p>
 *
 * @author Lonestone
 */

// TODO : Extends FastestWeighting plutôt ?
public class CleanestWeighting extends AbstractWeighting {
    private FastestWeighting fw;
    private final static String url = "C:\\D\\Sources\\tutorial\\src\\main\\java\\org\\geotools\\tutorial\\quickstart\\aireal_90_4326.tif";
    private  File tiffFile = new File(url);
    private  GeoTiffReader reader;
    private  GridCoverage2D grid;
    private  Raster gridData;
    private Double min ;
    private Double max ;

    public CleanestWeighting(FlagEncoder encoder, TurnCostProvider turnCostProvider) {
        super(encoder, turnCostProvider);

        fw = new FastestWeighting(encoder, turnCostProvider);
        min = Double.valueOf(10000);
        max = Double.valueOf(-1);

        // look existing
        String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
        try {
            CoordinateReferenceSystem crs2 = CRS.parseWKT(EPSG4326);
            reader =   new GeoTiffReader(new FileInputStream(( tiffFile)), new Hints(Hints.CRS, crs2));

            grid =reader.read(null);
            gridData = grid.getRenderedImage().getData();

        } catch (DataSourceException e) {
            // e.printStackTrace();
        } catch (FileNotFoundException e) {
        } catch (FactoryException e) {
            //   e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getMinWeight(double distance) {
        return 0;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double s = fw.calcEdgeWeight(edgeState, reverse);
        if (s < min) min = s;
        if (s > max) max = s;

        PointList ptList = edgeState.fetchWayGeometry(FetchMode.TOWER_ONLY);

        // TODO : médiane de plusieurs points
        GHPoint3D pt = ptList.get(0);
        try {
            double x = ReadGeotiff.getValue(pt.lon, pt.lat, grid, gridData);
            double coef = 1;
            if (x < 10) coef = 1.1;
            else if (x < 30) coef = 2;
            else if (x < 60) coef = 5;
            else if (x <= 90) coef = 10;
            double finalWeight =  s * (coef);
            return finalWeight;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 0;
        }
    }

    @Override
    public String getName() {
        return "cleanest";
    }
}
