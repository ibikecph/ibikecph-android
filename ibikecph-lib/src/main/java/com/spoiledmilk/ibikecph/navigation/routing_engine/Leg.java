package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A Route consists of multiple legs of multiple steps
 * Created by kraen on 10-08-16.
 */
public class Leg {

    protected String summary;

    protected List<TurnInstruction> steps = new ArrayList<>();

    protected TransportationType transportType = TransportationType.BIKE;

    protected long departureTime;

    protected long arrivalTime;

    protected List<Location> points = new ArrayList<>();

    protected String destinationHint;

    protected double distance;

    public static final double GEOMETRY_SCALING = 1e5;

    public Leg(JsonNode legNode) {
        if(legNode.has("type")) {
            String typeString = legNode.get("type").asText();
            transportType = TransportationType.valueOf(typeString);
        }

        if(legNode.has("departure_time")) {
            departureTime = legNode.get("departure_time").asLong();
        }
        if(legNode.has("arrival_time")) {
            arrivalTime = legNode.get("arrival_time").asLong();
        }
        if(legNode.has("summary")) {
            summary = legNode.get("summary").asText();
        }

        steps.clear();
        JsonNode stepsJson = legNode.get("steps");
        if (stepsJson != null && stepsJson.size() > 0) {
            for (JsonNode stepNode: stepsJson) {
                TurnInstruction instruction = new TurnInstruction(stepNode);
                // If the vehicle was not given by the journey API, just choose the general
                // transportation type of the leg.
                if(instruction.transportType == null && transportType != null) {
                    instruction.transportType = transportType;
                } else if(instruction.transportType == null) {
                    instruction.transportType = TransportationType.BIKE;
                }

                if(instruction.getDescription() == null) {
                    instruction.setDescription(summary);
                }

                // If the transportation type is public
                if(transportType.isPublicTransportation()) {
                    // And the instruction is departure or arrival - add time directly on them
                    if(instruction.getType().equals(TurnInstruction.Type.DEPART)) {
                        instruction.setTime(departureTime);
                        if(summary != null && !summary.isEmpty()) {
                            instruction.setDescription(summary);
                        }
                    } else if(instruction.getType().equals(TurnInstruction.Type.ARRIVE)) {
                        instruction.setTime(arrivalTime);
                    }
                }

                steps.add(instruction);
            }
        }

        distance = legNode.get("distance").asDouble();

        if(legNode.has("destination_hint")) {
            destinationHint = legNode.get("destination_hint").asText();
        } else {
            destinationHint = null;
        }

        // If the leg has geometry directly on it, let's decode points from there.
        if(legNode.has("geometry") && legNode.get("geometry").isTextual()) {
            decodePointsFromPolyline(legNode.get("geometry").asText());
        }
    }

    /**
     * Iterating the points and returns the closes after pointIndex to the location of the
     * instruction provided as argument.
     * @param instruction The instruction that the index is determined for.
     * @param pointIndex Any offset from which the search should start from.
     * @return The index into the points array, closest to the location of the instruction.
     */
    protected int getPointIndex(TurnInstruction instruction, int pointIndex) {
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
            throw new RuntimeException("Cannot get end location of a leg without points");
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
            throw new RuntimeException("Cannot get end location of a leg without points");
        }
    }

    /**
     * Get the estimated distance left of the leg
     * @return the distance in metres
     */
    public double getEstimatedDistanceLeft(int stepIndex) {
        float result = 0f;
        for(int s = stepIndex; s < steps.size(); s++) {
            TurnInstruction step = steps.get(s);
            result += step.distance;
        }
        return result;
    }

    public double getDistance() {
        return distance;
    }

    public TransportationType getTransportType() {
        return transportType;
    }

    public Address getStartAddress() {
        if(steps.size() > 0) {
            return steps.get(0).toAddress();
        } else {
            return null;
        }
    }

    public Address getEndAddress() {
        if(steps.size() > 0) {
            return steps.get(steps.size()-1).toAddress();
        } else {
            return null;
        }
    }

    public long getDepartureTime() {
        return departureTime;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public List<TurnInstruction> getSteps() {
        return steps;
    }

    public Location getNearestPoint(Location location) {
        float minimalDistance = Float.MAX_VALUE;
        Location result = null;
        for(Location p: points) {
            float distance = location.distanceTo(p);
            if(distance < minimalDistance) {
                minimalDistance = distance;
                result = p;
            }
        }
        return result;
    }

    public TurnInstruction getStepAfterPointIndex(int pointIndex) {
        for(TurnInstruction step: steps) {
            if(step.getPointsIndex() > pointIndex) {
                return step;
            }
        }
        return null;
    }

    /**
     * Updates the pointIndex field on all steps in the route.
     * This should be called after all points and steps has been added
     */
    public void updateStepPointIndices() {
        if(!points.isEmpty() && !steps.isEmpty()) {
            int pointIndex = 0;
            for(TurnInstruction step: steps) {
                pointIndex = getPointIndex(step, pointIndex);
                step.setPointsIndex(pointIndex);
            }
        } else {
            throw new RuntimeException("Got a route without points or steps");
        }
    }

    /**
     * Calculates the distance along the points path of the route, until the step
     * @param location the current location to calculate distance from
     * @param step the step to calculate the distance to
     * @return distance in metres
     */
    public float getDistanceToStep(Location location, TurnInstruction step) {
        Location nearestPoint = getNearestPoint(location);
        int nearestPointIndex = points.indexOf(nearestPoint);
        // Let the first point to calculate euclidean distance to be the next, as the nearest
        // might be behind the user. Respecting bounds.
        nearestPointIndex = Math.min(nearestPointIndex+1, points.size()-1);
        // Calculate euclidean distance to this first point.
        Location pointA = points.get(nearestPointIndex);
        float result = location.distanceTo(pointA);
        // Sum the distances of all points from the nearest point up until the location of the step
        for(int p = points.indexOf(pointA); p <= step.getPointsIndex(); p++) {
            Location pointB = points.get(p);
            result += pointA.distanceTo(pointB);
            // Use this pointB as pointA for the next segment
            pointA = pointB;
        }
        return result;
    }
}
