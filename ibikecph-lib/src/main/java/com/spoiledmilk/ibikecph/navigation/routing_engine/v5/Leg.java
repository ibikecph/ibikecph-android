package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import android.location.Location;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Route consists of multiple legs of multiple steps
 * Created by kraen on 10-08-16.
 */
public class Leg {

    protected String summary;

    protected List<TurnInstruction> upcomingSteps = new ArrayList<>();
    protected List<TurnInstruction> passedSteps = new ArrayList<>();

    protected TransportationType transportType;

    protected long departureTime;

    protected long arrivalTime;

    protected List<Location> points = new ArrayList<>();

    protected String destinationHint;

    protected float distance;

    public static final double GEOMETRY_SCALING = 1e5;

    public Leg(JsonNode legNode) {
        // An index into the points that will make sure that
        int pointIndex = 0;

        JsonNode stepsJson = legNode.get("steps");
        if (stepsJson != null && stepsJson.size() > 0) {
            boolean isFirst = true;
            int distanceToNextInstruction = 0;

            for (JsonNode stepNode: stepsJson) {
                TurnInstruction instruction = new TurnInstruction(stepNode);
                // Sets the distance from the previous instruction
                instruction.setDistance(distanceToNextInstruction);
                distanceToNextInstruction = (int) Math.round(stepNode.get("distance").asDouble());

                pointIndex = getPointIndex(instruction, pointIndex);
                instruction.setPointsIndex(pointIndex);

                // Generate a special description if this is the first instruction on the route.
                if (isFirst) {
                    instruction.generateStartDescriptionString();
                    isFirst = false;
                } else {
                    instruction.generateDescriptionString();
                }

                // If the vehicle was not given by the journey API, just choose the general
                // transportation type of the route.
                if(instruction.transportType == null && transportType != null) {
                    instruction.transportType = transportType;
                } else if(instruction.transportType == null) {
                    instruction.transportType = TransportationType.BIKE;
                }

                if(instruction.getDescription() == null) {
                    instruction.setDescription(summary);
                }

                upcomingSteps.add(instruction);
            }
        }
    }

    /**
     * Iterating the points and returns the closes after pointIndex to the location of the
     * instruction provided as argument.
     * @param instruction The instruction that the index is determined for.
     * @param pointIndex Any offset from which the search should start from.
     * @return The index into the points array, closest to the location of the instruction.
     */
    protected int getPointIndex(SMTurnInstruction instruction, int pointIndex) {
        int result = pointIndex;
        float minimalDistance = Float.MAX_VALUE;
        for(int i = pointIndex; i < points.size(); i++) {
            float distance = instruction.getLocation().distanceTo(points.get(i));
            if(distance < minimalDistance) {
                minimalDistance = distance;
                result = i;
            }
        }
        return result;
    }

    public List<Location> getPoints() {
        return points;
    }

    /**
     * Decoder for the Encoded Polyline Algorithm Format
     * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Encoded Polyline Algorithm Format</a>
     */
    public static List<Location> decodePolyline(String encodedString) {
        if (encodedString == null)
            return null;

        byte[] bytes;
        try {
            bytes = encodedString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.e("decodePolyline() UnsupportedEncodingException", e);
            return null;
        }

        int len = encodedString.length();

        int lat = 0, lng = 0;

        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < len; ) {
            for (int k = 0; k < 2; k++) {

                int delta = 0;
                int shift = 0;

                byte c;
                do {
                    c = (byte) (bytes[i++] - 63);
                    delta |= (c & 0x1f) << shift;
                    shift += 5;
                } while ((c & 0x20) != 0);

                delta = ((delta & 0x1) != 0) ? ((~delta >> 1) | 0x80000000) : (delta >> 1);
                if (k == 0)
                    lat += delta;
                else
                    lng += delta;
            }
            Location loc = Util.locationFromCoordinates((double) lat / GEOMETRY_SCALING, (double) lng / GEOMETRY_SCALING);
            locations.add(loc);
        }

        return locations;
    }

    public void decodePointsFromPolyline(String geometry) {
        points = decodePolyline(geometry);
    }

    /**
     * Get the first point of in the leg.
     * @return the start location
     */
    public Location getStartLocation() {
        if(points.size() > 0) {
            return points.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get the last point of in the leg.
     * @return the end location
     */
    public Location getEndLocation() {
        if(points.size() > 0) {
            return points.get(points.size()-1);
        } else {
            return null;
        }
    }

    /**
     * Get the estimated distance left of the leg
     * @return the distance in metres
     */
    public float getEstimatedDistanceLeft() {
        float result = 0f;
        for(TurnInstruction step: upcomingSteps) {
            result += step.distance;
        }
        return result;
    }

    /**
     * Get the estimated total distance of the leg
     * TODO: Consider deriving this from step instructions instead
     * @return the distance in metres
     */
    public float getEstimatedDistance() {
        return distance;
    }
}
