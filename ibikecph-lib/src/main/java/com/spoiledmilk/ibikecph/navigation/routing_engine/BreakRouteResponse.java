package com.spoiledmilk.ibikecph.navigation.routing_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.search.Address;

import java.util.ArrayList;
import java.util.List;

/**
 * A Class that holds the response as it comes from the break route journey API.
 * Created by kraen on 27-06-16.
 */
public class BreakRouteResponse {

    protected ObjectNode jsonNodes;

    protected List<Route> alternativeRoutes = new ArrayList<>();

    public BreakRouteResponse(ObjectNode jsonNodes) {
        this.jsonNodes = jsonNodes;
        parseJson(jsonNodes);
    }

    protected void parseJson(ObjectNode jsonNodes) {
        alternativeRoutes.clear();
        if(!jsonNodes.has("routes") || !jsonNodes.get("routes").isArray()) {
            throw new RuntimeException("Expected a 'routes' field of type array");
        }
        for(JsonNode routeNode: jsonNodes.get("routes")) {
            Route route = new Route(RouteType.BREAK);
            route.parseFromJson(routeNode);
            alternativeRoutes.add(route);
        }
    }

    public ObjectNode getJsonNode() {
        return jsonNodes;
    }

    public Route getRoute(int position) {
        return alternativeRoutes.get(position);
    }

    /**
     * Sets the startAddress on all alternative journeys in the response.
     * @param startAddress the start address
     */
    public void setStartAddress(Address startAddress) {
        for(Route route: alternativeRoutes) {
            route.setStartAddress(startAddress);
        }
    }

    /**
     * Sets the endAddress on all alternative journeys in the response.
     * @param endAddress the start address
     */
    public void setEndAddress(Address endAddress) {
        for(Route route: alternativeRoutes) {
            route.setEndAddress(endAddress);
        }
    }
}
