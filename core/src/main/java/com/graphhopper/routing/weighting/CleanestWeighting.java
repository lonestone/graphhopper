package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.custom.ReadGeotiff;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates the cleanest route - independent of a vehicle as the calculation is based on the
 * distance and air quality of edges.
 * Air quality is extracted from .geotiff file, and is in this interval [0, 90]
 *
 * <p>
 *
 * @author J.Brun - Lonestone
 */
public class CleanestWeighting extends FastestWeighting {
    private PriorityWeighting pw;

    private final static Logger logger = LoggerFactory.getLogger(CleanestWeighting.class);

    public CleanestWeighting(FlagEncoder encoder, PMap map, TurnCostProvider turnCostProvider) {
        super(encoder, turnCostProvider);

        ReadGeotiff.startTimer();
        pw = new PriorityWeighting(encoder, map, turnCostProvider);
    }

    @Override
    public double getMinWeight(double distance) {
        return 0;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double priorityWeight = pw.calcEdgeWeight(edgeState,reverse);
        PointList ptList = edgeState.fetchWayGeometry(FetchMode.BASE_AND_PILLAR);

        try {
            // it's useless to take all points, because they are too close
            GHPoint3D pt = ptList.get(0);
            double airQA = ReadGeotiff.getValue(pt.lon, pt.lat);
            return priorityWeight * calcCoef(airQA);
        } catch (Exception e) {
            logger.error(e.getMessage());
            // if an error occurred, we return the weight from fastest way
            return priorityWeight;
        }
    }

    @Override
    public String getName() {
        return "cleanest";
    }

    private double calcCoef(double airQA) {
        if (airQA == 0) return 1;
        return airQA * airQA * airQA ;
    }

}
