package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.*;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by kraen on 12-07-16.
 */
public class Route extends SMRoute {

    public Route(Location start, Location end, RouteType type) {
        super(start, end, type);
    }

    public enum OsrmVersion {
        V4,
        V5
    }

    public boolean parseFromJson(JsonNode rootNode, OsrmVersion version) {
        if(version == OsrmVersion.V4) {
            return super.parseFromJson(rootNode);
        } else {
            return parseFromJson(rootNode);
        }
    }

    @Override
    public boolean parseFromJson(JsonNode rootNode) {
        // Log.d("Route", "parseFromJson() json = " + rootNode);
        synchronized (this) {
            if (rootNode == null) {
                return false;
            }

            // Relevant when parsing routes from the journey API
            if(rootNode.get("route_summary") != null) {
                if(rootNode.get("route_summary").get("type") != null) {
                    String transportType = rootNode.get("route_summary").path("type").textValue();
                    if (transportType != null) {
                        this.transportType = SMRoute.TransportationType.valueOf(transportType);
                    }
                }
                if(rootNode.get("route_summary").get("name") != null) {
                    description = rootNode.get("route_summary").get("name").asText();
                }
                if(rootNode.get("route_summary").get("departure_time") != null) {
                    departureTime = rootNode.get("route_summary").get("departure_time").asLong();
                }
                if(rootNode.get("route_summary").get("arrival_time") != null) {
                    arrivalTime = rootNode.get("route_summary").get("arrival_time").asLong();
                }
                // TODO: Remove this hack once the server responds with proper timestamps
                if(departureTime > 0 && arrivalTime > 0) {
                    Date now = new Date();
                    boolean daylight = TimeZone.getTimeZone("Europe/Copenhagen").inDaylightTime(now);
                    if(daylight) {
                        int anHour = 60 * 60;
                        departureTime -= anHour;
                        arrivalTime -= anHour;
                    }
                }
            }

            if (rootNode.get("routes") == null ||
                !rootNode.get("routes").isArray() ||
                rootNode.get("routes").size() != 1) {
                throw new RuntimeException("Expected a single element in the 'routes' field");
            }
            JsonNode routeNode = rootNode.get("routes").get(0);
            // Decode the polyline geometry of the primary route
            waypoints = decodePolyline(routeNode.get("geometry").textValue(), null);

            if (waypoints == null || waypoints.size() < 2) {
                return false;
            }

            upcomingTurnInstructions = new ArrayList<>();
            pastTurnInstructions = new LinkedList<>();
            visitedLocations = new ArrayList<>();
            if(departureTime > 0 && arrivalTime > 0) {
                Log.d("Route", "Overriding duration from difference in arrival and departure");
                // Let's calculate the estimated duration from the difference in departure and
                // arrival time to account for the change of vehicle
                estimatedDuration = (int)(arrivalTime - departureTime);
            } else {
                estimatedDuration = routeNode.get("duration").asInt();
            }
            estimatedDurationLeft = estimatedDuration;
            distancePassed = 0d;
            // TODO: This is actually a decimal number
            estimatedDistance = routeNode.get("distance").asInt();

            // These are no longer available
            routeChecksum = null;
            destinationHint = null;

            JsonNode waypointsNode = rootNode.get("waypoints");
            if (waypointsNode.isArray() && waypointsNode.size() >= 2) {
                destinationHint = waypointsNode.get(waypointsNode.size()-1).get("hint").asText();
            }

            if (routeNode.get("legs") == null ||
                !routeNode.get("legs").isArray() ||
                routeNode.get("legs").size() != 1) {
                throw new RuntimeException("Expected a single item in the routes 'legs' field");
            }

            // An index into the waypoints that will make sure that
            int waypointIndex = 0;

            JsonNode stepsJson = routeNode.get("legs").get(0).get("steps");
            if (stepsJson != null && stepsJson.size() > 0) {
                boolean isFirst = true;
                int distanceToNextInstruction = 0;

                for (JsonNode stepNode: stepsJson) {
                    TurnInstruction instruction = new TurnInstruction(stepNode);
                    // Sets the distance from the previous instruction
                    instruction.setDistance(distanceToNextInstruction);
                    distanceToNextInstruction = (int) Math.round(stepNode.get("distance").asDouble());

                    instruction.waypointsIndex = getWaypointIndex(instruction, waypointIndex);
                    waypointIndex = instruction.waypointsIndex;

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
                        instruction.setDescription(description);
                    }

                    upcomingTurnInstructions.add(instruction);
                }
            }
            lastVisitedWaypointIndex = 0;
        }
        return true;
    }

    /**
     * Iterating the waypoints and returns the closes after waypointIndex to the location of the
     * instructions provided
     * @param instruction
     * @param waypointIndex
     * @return
     */
    protected int getWaypointIndex(SMTurnInstruction instruction, int waypointIndex) {
        int result = waypointIndex;
        float minimalDistance = Float.MAX_VALUE;
        for(int i = waypointIndex; i < waypoints.size(); i++) {
            float distance = instruction.getLocation().distanceTo(waypoints.get(i));
            if(distance < minimalDistance) {
                minimalDistance = distance;
                result = i;
            }
        }
        return result;
    }


    /**
     * Decoder for the Encoded Polyline Algorithm Format
     * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Encoded Polyline Algorithm Format</a>
     */
    public static List<Location> decodePolyline(String encodedString, TransportationType type) {
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

        List<Location> locations = new ArrayList<Location>();
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
            Location loc = Util.locationFromCoordinates((double) lat / GEOMETRY_SCALING_V5, (double) lng / GEOMETRY_SCALING_V5);
            locations.add(loc);
        }

        return locations;
    }

    @Override
    public void recalculateRoute(Location location) {
        // We need a proper end location and a current location
        Location end = getEndLocation();
        if (location == null || end == null)
            return;

        // Let's not recalculate twice at the same time
        if (recalculationInProgress) {
            return;
        }

        // Distance throttling the calculation of new routes
        float distance = location.distanceTo(lastRecalcLocation);
        if (distance < MIN_DISTANCE_FOR_RECALCULATION) {
            return;
        }
        lastRecalcLocation = location;

        // Let's start the recalculation
        recalculationInProgress = true;
        emitRouteRecalculationStarted();

        RegularRouteRequester requester = new RegularRouteRequester(new LatLng(location), new LatLng(end),
        new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                recalculationInProgress = false;
            }

            @Override
            public void onSuccess(BreakRouteResponse breakRouteResponse) {
                recalculationInProgress = false;
                throw new RuntimeException("Unexpected break route result");
            }

            @Override
            public void onFailure() {
                recalculationInProgress = false;
                throw new RuntimeException("Failed to fetch the recalculated route");
            }
        }, getType());
        requester.setDestinationHint(destinationHint);
        requester.setRoute(this);
        if(location.hasBearing()) {
            requester.setBearing(location.getBearing());
        }
        requester.execute();
    }
}
