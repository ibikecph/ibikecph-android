package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.location.LocationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.search.Address;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A journey consisting of multiple smaller routes.
 * The result of a call to the break route journey API.
 * TODO: Implement a route listener that adds and removes itself when route destinations are reached.
 * Created by kraen on 27-06-16.
 */
public class Journey {

    protected List<SMRoute> routes = new ArrayList<>();

    protected JsonNode journeyNode;

    public int estimatedDuration;
    public int totalBikeDistance;
    public int estimatedDistance;
    public long arrivalTime;

    protected Address startAddress;
    protected Address endAddress;

    public Journey(JsonNode journeyNode) {
        this.journeyNode = journeyNode;
        parseJson();
    }

    public Journey(SMRoute route) {
        routes.add(route);

        estimatedDistance = route.estimatedDistance;
        estimatedDuration = route.estimatedDuration;
        totalBikeDistance = estimatedDistance;

        startAddress = route.startAddress;
        endAddress = route.endAddress;
    }

    public Address getStartAddress() {
        return startAddress;
    }

    public Address getEndAddress() {
        return endAddress;
    }

    public List<SMRoute> getRoutes() {
        return routes;
    }

    protected void parseJson() {
        if(!this.journeyNode.isObject() ||
           !this.journeyNode.has("journey") ||
           !this.journeyNode.get("journey").isArray() ||
           this.journeyNode.get("journey").size() == 0) {
            throw new RuntimeException("Expected a journey array field in the JSON object.");
        }
        for(JsonNode routeNode: this.journeyNode.get("journey")) {
            Location start = parseViaPoint(routeNode.get("via_points").get(0));
            Location end = parseViaPoint(routeNode.get("via_points").get(routeNode.get("via_points").size() - 1));
            SMRoute route = new SMRoute(start, end, routeNode, RouteType.BREAK);

            // Set the start and end addresses on the route.
            if(!routeNode.get("route_name").isArray() || routeNode.get("route_name").size() != 2) {
                throw new RuntimeException("Expected route_name to be an array with two elements");
            }

            route.startAddress = new Address();
            route.startAddress.setStreet(routeNode.get("route_name").get(0).textValue());
            route.startAddress.setLocation(new LatLng(start.getLatitude(), start.getLongitude()));

            route.endAddress = new Address();
            route.endAddress.setStreet(routeNode.get("route_name").get(1).textValue());
            route.endAddress.setLocation(new LatLng(end.getLatitude(), end.getLongitude()));

            // Add the route to the routes of the journey.
            routes.add(route);
        }

        JsonNode summary = journeyNode.get("journey_summary");
        estimatedDistance = summary.get("total_distance").asInt();
        estimatedDuration = summary.get("total_time").asInt();
        totalBikeDistance = summary.get("total_bike_distance").asInt();
        JsonNode lastRouteNode = journeyNode.get("journey").get(journeyNode.get("journey").size()-1);
        arrivalTime = lastRouteNode.get("route_summary").get("arrival_time").asInt();
    }

    protected Location parseViaPoint(JsonNode viaPoint) {
        Location result = new Location(LocationManager.GPS_PROVIDER);
        if(!viaPoint.isArray() || viaPoint.size() != 2) {
            throw new RuntimeException("Expected every via point to be an array with two elements.");
        }
        result.setLatitude(viaPoint.get(0).asDouble());
        result.setLongitude(viaPoint.get(1).asDouble());
        return result;
    }

    /**
     * Returns the estimated duration of the entire journey
     * @return
     */
    public int getEstimatedDuration() {
        int estimatedArrivalTime = 0;
        for(SMRoute route: getRoutes()) {
            estimatedArrivalTime += route.estimatedDuration;
        }
        return estimatedArrivalTime;
    }

    public float getEstimatedDistance() {
        float estimatedDistance = 0;
        // Calculate the accumulated estimated distance.
        for(SMRoute route: getRoutes()) {
            estimatedDistance += route.getEstimatedDistance();
        }
        return estimatedDistance;
    }

    public float getEstimatedDistanceLeft() {
        float distanceLeft = 0;
        // Calculate the accumulated distance left.
        for(SMRoute route: getRoutes()) {
            distanceLeft += route.getEstimatedDistanceLeft();
        }
        return distanceLeft;
    }

    public int getEstimatedDurationLeft() {
        int durationLeft = 0;
        // Calculate the accumulated distance left.
        for(SMRoute route: getRoutes()) {
            durationLeft += route.getEstimatedDurationLeft();
        }
        return durationLeft;
    }

    public Date getArrivalTime() {
        int durationLeft = getEstimatedDurationLeft();
        // Set the ETA label
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, durationLeft);
        return c.getTime();
    }

    public SMTurnInstruction getUpcomingInstruction() {
        return getUpcomingInstruction(0);
    }

    public SMTurnInstruction getUpcomingInstruction(int offset) {
        if(offset < 0) {
            throw new RuntimeException("Expected a non-negative offset");
        }
        List<SMTurnInstruction> allUpcomingInstructions = new ArrayList<>();
        for(SMRoute route: routes) {
            List<SMTurnInstruction> instructions = route.getUpcomingTurnInstructions();
            // If we are simply looking for the first, let's just return it right away
            if(offset == 0 && instructions.size() > 0) {
                return instructions.get(0);
            } else {
                allUpcomingInstructions.addAll(instructions);
            }
        }
        // Return the turn instruction
        if(offset < allUpcomingInstructions.size()) {
            return allUpcomingInstructions.get(offset);
        } else {
            return null;
        }
    }
}
