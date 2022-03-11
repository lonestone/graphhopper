package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.custom.ReadGeotiff;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import com.sun.media.imageioimpl.common.PackageUtil;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.Raster;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Date;

/**
 * Calculates the cleanest route - independent of a vehicle as the calculation is based on the
 * distance and air quality of edges.
 * Air quality is extracted from .geotiff file, and is in this interval [0, 90]
 *
 * <p>
 *
 * @author Lonestone
 */

public class CleanestWeighting extends FastestWeighting {
    private FastestWeighting fw;

    private final static Logger logger = LoggerFactory.getLogger(CleanestWeighting.class);
    private static GeoTiffReader reader;
    private static GridCoverage2D grid;
    private static Raster gridData;
    private Double min;
    private Double max;
    private static boolean updateInProgress = false;
    private static Date lastGeotiffMaj = new Date(0);

    public CleanestWeighting(FlagEncoder encoder, TurnCostProvider turnCostProvider) {
        super(encoder, turnCostProvider);

        try {
            setVendorName(PackageUtil.class, "lonestone", "lonestone", "lonestone");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        fw = new FastestWeighting(encoder, turnCostProvider);
        min = Double.valueOf(10000);
        max = Double.valueOf(-1);

        try {
            // look existing
            if (reader == null && !updateInProgress) {
                logger.info("Initial .geotiff loading");
                updateGeotiff();
            }
            synchronized (reader) {
                long diff = new Date().getTime() - lastGeotiffMaj.getTime();
                if (diff > 5000 && !updateInProgress) {
                    lastGeotiffMaj = new Date();
                    updateGeotiff();
                }
            }
        } catch (DataSourceException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
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
        double fastestWeight = fw.calcEdgeWeight(edgeState, reverse);
        if (fastestWeight < min) min = fastestWeight;
        if (fastestWeight > max) max = fastestWeight;

        PointList ptList = edgeState.fetchWayGeometry(FetchMode.TOWER_ONLY);

        // TODO : médiane de plusieurs points ?
        GHPoint3D pt = ptList.get(0);
        try {
            double airQA = ReadGeotiff.getValue(pt.lon, pt.lat, grid, gridData);
            // TODO : delete that
            if (airQA > 0) {
                System.out.println((airQA));
            }
            return fastestWeight * calcCoef(airQA);
        } catch (Exception e) {
            logger.error(e.getMessage());
            // if an error occurred, we return the weight from fastest way
            return fastestWeight;
        }
    }

    @Override
    public String getName() {
        return "cleanest";
    }

    private double calcCoef(double airQA) {
        double coef = 1;
        if (airQA < 10) coef = 1.1;
        else if (airQA < 30) coef = 2;
        else if (airQA < 60) coef = 5;
        else if (airQA <= 90) coef = 10;

        return coef;
    }

    private static void updateGeotiff() throws IOException, FactoryException {
        updateInProgress = true;
        logger.info("Updating geotiff");

        // get geotiff stream
        URL url = new URL("https://data.airpl.org/media/aireel/aireel_indic_7m_202201311500_atmo.tif");
        InputStream in = url.openStream();

        // convert to reader and load to RAM
        String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
        CoordinateReferenceSystem crs2 = CRS.parseWKT(EPSG4326);
        reader = new GeoTiffReader(in, new Hints(Hints.CRS, crs2));
        grid = reader.read(null);
        gridData = grid.getRenderedImage().getData();
        in.close();

        logger.info("Update geotiff OK");
        lastGeotiffMaj = new Date();
        updateInProgress = false;
    }

    // set vendorname, else Geotiff library could not be get .geotiff from web
    // see https://stackoverflow.com/questions/7051603/jai-vendorname-null/18495658#18495658
    public static void setVendorName(Class<?> PackageUtil, String vendor, String version, String specTitle)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field vendorField = PackageUtil.getDeclaredField("vendor");
        vendorField.setAccessible(true);
        vendorField.set(null, vendor);

        Field versionField = PackageUtil.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(null, version);

        Field specTitleField = PackageUtil.getDeclaredField("specTitle");
        specTitleField.setAccessible(true);
        specTitleField.set(null, specTitle);
    }
}
