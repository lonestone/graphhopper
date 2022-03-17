package com.graphhopper.routing.weighting.custom;

import com.sun.media.imageioimpl.common.PackageUtil;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ReadGeotiff {

    private static GeoTiffReader reader;
    private static GridCoverage2D grid;
    private static Raster gridData;
    private static boolean timerOn = false;
    private final static Logger logger = LoggerFactory.getLogger(ReadGeotiff.class);

    public static void startTimer() {
        if (!timerOn)
        {
            timerOn = true;
            System.out.println("Start new geotiff timer");
            long x = new Date().getTime();
            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    try {
                        updateGeotiff();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (FactoryException e) {
                        e.printStackTrace();
                    }
                }
            },0,3600000); // 1hr

            try {
                setVendorName(PackageUtil.class, "lonestone", "lonestone", "lonestone");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static double getValue(double lon, double lat) throws Exception {
        try {
            GridGeometry2D gg = grid.getGridGeometry();
            DirectPosition2D posWorld = new DirectPosition2D(lon, lat);
            GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

            // envelope is the size in the target projection
            double[] pixel = new double[1];
            double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);

            // 0.54 is determinated by test between this geotiff reader and with geoserver requests
            return data[0] > 0 ? data[0] * 0.54  : 0;
        }catch (Exception e)
        {
            // If catched, its because pixel is outside geotiff
            return 0;
        }
    }

    public static void updateGeotiff() throws IOException, FactoryException {
        logger.info("Updating geotiff");

        // get geotiff stream
        // todo: go to env
        URL url = new URL("https://api.naonair.org/geoserver/aireel/wms?service=WMS&version=1.1.0&request=GetMap&layers=aireel%3Aaireel_indic_7m_atmo_deg&bbox=-1.9272281569407010,47.0913666061233300,-1.3443056421867574,47.3612095285679118&srs=EPSG%3A4326&styles&format=image%2Fgeotiff8&width=6144&height=2640");
        //URL url = new URL("https://data.airpl.org/geoserver/aireel/wms?service=WMS&version=1.1.0&request=GetMap&layers=aireel%3Aaireel_indic_7m_atmo_deg&bbox=-1.9272281569407010,47.0913666061233300,-1.3443056421867574,47.3612095285679118&srs=EPSG%3A4326&styles&format=image%2Fgeotiff&width=6144&height=2640");
        InputStream in = url.openStream();

        // convert to reader and load to RAM
        String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
        CoordinateReferenceSystem crs2 = CRS.parseWKT(EPSG4326);
        reader = new GeoTiffReader(in, new Hints(Hints.CRS, crs2));
        grid = reader.read(null);
        gridData = grid.getRenderedImage().getData();
        in.close();

        logger.info("Update geotiff OK");
    }

    // set vendorname, else Geotiff library could not be get .geotiff from web
    // see https://stackoverflow.com/questions/7051603/jai-vendorname-null/18495658#18495658
    private static void setVendorName(Class<?> PackageUtil, String vendor, String version, String specTitle)
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
