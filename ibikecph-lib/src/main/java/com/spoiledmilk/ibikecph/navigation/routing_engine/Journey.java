package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.v5.Route;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.spoiledmilk.ibikecph.navigation.routing_engine.v5.TurnInstruction.translateStepName;

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
    public float estimatedDistance, totalBikeDistance;
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
            Route route = new Route(start, end, RouteType.BREAK);
            route.parseFromJson(routeNode, Route.OsrmVersion.V4);

            // Set the start and end addresses on the route.
            if(!routeNode.get("route_name").isArray() || routeNode.get("route_name").size() != 2) {
                throw new RuntimeException("Expected route_name to be an array with two elements");
            }

            JsonNode routeSummaryNode = routeNode.get("route_summary");
            if(routeSummaryNode != null) {
                String startName = translateStepName(routeSummaryNode.get("start_point").textValue());
                route.startAddress = new Address();
                route.startAddress.setName(startName);
                route.startAddress.setLocation(new LatLng(start.getLatitude(), start.getLongitude()));

                String endName = translateStepName(routeSummaryNode.get("end_point").textValue());
                route.endAddress = new Address();
                route.endAddress.setName(endName);
                route.endAddress.setLocation(new LatLng(end.getLatitude(), end.getLongitude()));
            }

            // Add the route to the routes of the journey.
            routes.add(route);
        }

        // TODO: Consider overwriting the street of the first routes startAddress and the last
        // routes endAddress as they should match the start and end that was requested

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
     * Get the estimated distance of any type of transportation
     * @return
     */
    public float getEstimatedDistance() {
        return getEstimatedDistance(false);
    }

    /**
     * Get the estimated distance of a specific type of transportation.
     * @return
     */
    public float getEstimatedDistance(boolean nonPublicOnly) {
        float estimatedDistance = 0;
        // Calculate the accumulated estimated distance.
        for(SMRoute route: getRoutes()) {
            if(!nonPublicOnly || !route.transportType.isPublicTransportation()) {
                estimatedDistance += route.getEstimatedDistance();
            }
        }
        return estimatedDistance;
    }

    public float getEstimatedDistanceLeft() {
        return getEstimatedDistanceLeft(false);
    }

    public float getEstimatedDistanceLeft(boolean nonPublicOnly) {
        float distanceLeft = 0;
        // Calculate the accumulated distance left.
        for(SMRoute route: getRoutes()) {
            if(!nonPublicOnly || !route.transportType.isPublicTransportation()) {
                distanceLeft += route.getEstimatedDistanceLeft();
            }
        }
        return distanceLeft;
    }

    /**
     * Returns the estimated duration of the entire journey
     * @return
     */
    public int getEstimatedDuration() {
        Date departure = new Date(); // Let's assume we can depart, right away
        return (int) (getArrivalTime().getTime() - departure.getTime()) / 1000;
    }

    /**
     * @deprecated Do not differentiate on duration left or duration
     * @return
     */
    public int getEstimatedDurationLeft() {
        return getEstimatedDuration();
    }

    public Date getArrivalTime() {
        // Find the last non-public routes - only these can have their arrival time
        // improved by the user biking faster
        int nonPublicDurationLeft = 0;
        Date earliestDeparture = new Date(); // Let's assume now
        // Looping backwards in routes
        for(int r = getRoutes().size()-1; r >= 0; r--) {
            SMRoute route = getRoutes().get(r);
            if(route.isPublicTransportation() && route.arrivalTime != -1) {
                // The last public transportation
                earliestDeparture = new Date(route.arrivalTime * 1000);
                break;
            } else {
                nonPublicDurationLeft += route.getEstimatedDurationLeft();
            }
        }
        Calendar c = Calendar.getInstance();
        c.setTime(earliestDeparture);
        c.add(Calendar.SECOND, nonPublicDurationLeft);
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
