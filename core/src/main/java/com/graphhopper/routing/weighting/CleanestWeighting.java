package com.graphhopper.routing.weighting;

import com.ctc.wstx.io.SystemId;
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
public class CleanestWeighting extends PriorityWeighting {
    private final static Logger logger = LoggerFactory.getLogger(CleanestWeighting.class);
    private final static int edgePenaltyFactor = 1000;

    public CleanestWeighting(FlagEncoder encoder, PMap map, TurnCostProvider turnCostProvider) {
        super(encoder, map, turnCostProvider);

        ReadGeotiff.startTimer();
    }

    @Override
    public double getMinWeight(double distance) {
        return 1;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        // use inherited class to calc base edge weight
        double baseWeight = super.calcEdgeWeight(edgeState,reverse);
        PointList ptList = edgeState.fetchWayGeometry(FetchMode.BASE_AND_PILLAR);

        try {
            // it's useless to take all points to compute an avg/mode, because they are too close
            GHPoint3D pt = ptList.get(0);
            double airQA = ReadGeotiff.getValue(pt.lon, pt.lat);
            double distance = edgeState.getDistance();

            return baseWeight + calcCoef(airQA);
        } catch (Exception e) {
            logger.error(e.getMessage());
            // if an error occurred, we return the weight from inherited class
            return baseWeight;
        }
    }

    @Override
    public String getName() {
        return "cleanest";
    }

    // airQA is na integer in interval [0;90], so we return a coef in interval ]0;edgePenaltyFactor]
    private double calcCoef(double airQA) {
        return ((200-1)/(90-0)) * airQA + 1;
    }

}
