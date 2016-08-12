package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * This route models a route from the OSRMv5 or the IBikeCPH journey API.
 * Created by kraen on 12-07-16.
 */
public class Route extends SMRoute {

    /**
     * The assumed biking speed in metres per second.
     */
    private static final float AVERAGE_BIKING_SPEED = 15f * 1000f / 3600f;

    /**
     * The assumed cargo biking speed in metres per second.
     */
    private static final float AVERAGE_CARGO_BIKING_SPEED = 10f * 1000f / 3600f;

    protected List<Leg> legs = new ArrayList<>();

    public Route(Location start, Location end, RouteType type) {
        super(start, end, type);
    }

    @Override
    public Location getStartLocation() {
        if(legs.size() > 0) {
            Leg firstLeg = legs.get(0);
            return firstLeg.getStartLocation();
        } else {
            return null;
        }
    }

    @Override
    public LatLng getRealStartLocation() {
        return startAddress.getLocation();
    }

    @Override
    public Location getEndLocation() {
        if(legs.size() > 0) {
            Leg lastLeg = legs.get(legs.size()-1);
            return lastLeg.getEndLocation();
        } else {
            return null;
        }
    }

    @Override
    public LatLng getRealEndLocation() {
        return endAddress.getLocation();
    }

    @Override
    public List<Location> getPoints() {
        List<Location> result = new ArrayList<>();
        for(Leg leg: legs) {
            result.addAll(leg.getPoints());
        }
        return result;
    }

    @Override
    public boolean parseFromJson(JsonNode rootNode) {
        synchronized (this) {
            if (rootNode == null) {
                return false;
            }

            // Relevant when parsing routes from the journey API
            if(rootNode.get("route_summary") != null) {
                if(rootNode.get("route_summary").get("type") != null) {
                    String transportType = rootNode.get("route_summary").path("type").textValue();
                    if (transportType != null) {
                        this.transportType = TransportationType.valueOf(transportType);
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

            upcomingTurnInstructions.clear();
            pastTurnInstructions.clear();
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
            // TODO: This is actually a decimal number
            estimatedDistance = routeNode.get("distance").asInt();
            estimatedDistanceLeft = estimatedDistance;

            // These are no longer available
            routeChecksum = null;
            destinationHint = null;

            JsonNode waypointsNode = rootNode.get("waypoints");
            if (waypointsNode.isArray() && waypointsNode.size() >= 2) {
                destinationHint = waypointsNode.get(waypointsNode.size()-1).get("hint").asText();
            }

            if (routeNode.get("legs") == null ||
                !routeNode.get("legs").isArray() ||
                routeNode.get("legs").size() == 0) {
                throw new RuntimeException("Expected at least one item in the routes 'legs' field");
            }

            for(JsonNode legNode: routeNode.get("legs")) {
                Leg leg = new Leg(legNode);
                legs.add(leg);
            }

            if(legs.size() == 1) {
                Leg onlyLeg = legs.get(0);
                if(onlyLeg.getPoints().size() == 0) {
                    onlyLeg.decodePointsFromPolyline(routeNode.get("geometry").textValue());
                }
            }

        }
        return true;
    }


    @Override
    public void recalculateRoute(final Location location) {
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
        requester.setRoute(this);
        if(location.hasBearing()) {
            requester.setBearing(location.getBearing());
        }
        requester.execute();
    }

    @Override
    public float getEstimatedDuration() {
        float result = 0f;
        for(Leg leg: legs) {
            if(transportType.isPublicTransportation()) {
               result += leg.arrivalTime - leg.departureTime;
            } else if(getType().equals(RouteType.CARGO)) {
                result += leg.getEstimatedDistance() / AVERAGE_CARGO_BIKING_SPEED;
            } else {
                result += leg.getEstimatedDistance() / AVERAGE_BIKING_SPEED;
            }
        }
        return result;
    }

    @Override
    public float getEstimatedDurationLeft() {
        if(transportType.isPublicTransportation() && pastTurnInstructions.isEmpty()) {
            return arrivalTime - departureTime;
        } else if(transportType.isPublicTransportation() && !pastTurnInstructions.isEmpty()) {
            // If past instructions exists the user has departed
            Date now = new Date();
            return Math.max(arrivalTime - now.getTime(), 0f);
        } else if(getType().equals(RouteType.CARGO)) {
            return getEstimatedDistanceLeft() / AVERAGE_CARGO_BIKING_SPEED;
        } else {
            return getEstimatedDistanceLeft() / AVERAGE_BIKING_SPEED;
        }
    }

    @Override
    public List<SMTurnInstruction> getUpcomingTurnInstructions() {
        List<SMTurnInstruction> result = new LinkedList<>();
        for(Leg leg: legs) {
            result.addAll(leg.upcomingSteps);
        }
        return result;
    }

    @Override
    public List<SMTurnInstruction> getPastTurnInstructions() {
        List<SMTurnInstruction> result = new LinkedList<>();
        for(Leg leg: legs) {
            result.addAll(leg.passedSteps);
        }
        return result;
    }

    @Override
    public float getEstimatedDistance() {
        float result = 0f;
        for(Leg leg: legs) {
            result += leg.getEstimatedDistance();
        }
        return result;
    }

    @Override
    public float getEstimatedDistanceLeft() {
        float result = 0f;
        for(Leg leg: legs) {
            result += leg.getEstimatedDistanceLeft();
        }
        return result;
    }

    @Override
    public void addListener(SMRouteListener listener) {
        if(listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
}
