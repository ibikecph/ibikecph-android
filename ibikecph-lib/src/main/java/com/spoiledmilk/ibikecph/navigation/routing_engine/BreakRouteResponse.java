package com.spoiledmilk.ibikecph.navigation.routing_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.spoiledmilk.ibikecph.search.Address;

import java.util.ArrayList;
import java.util.List;

/**
 * A Class that holds the response as it comes from the break route journey API.
 * Created by kraen on 27-06-16.
 */
public class BreakRouteResponse {

    protected ArrayNode jsonNodes;

    protected List<Journey> alternativeJourneys = new ArrayList<>();

    public BreakRouteResponse(ArrayNode jsonNodes) {
        this.jsonNodes = jsonNodes;
        parseJson();
    }

    protected void parseJson() {
        alternativeJourneys.clear();
        for(JsonNode journeyNode: jsonNodes) {
            Journey journey = new Journey(journeyNode);
            alternativeJourneys.add(journey);
        }
    }

    public ArrayNode getJsonNode() {
        return jsonNodes;
    }

    public Journey getJourney(int position) {
        return alternativeJourneys.get(position);
    }

    /**
     * Sets the startAddress on all alternative journeys in the response.
     * TODO: Consider moving this to a Journey constructor, passed along when calling Geocoder
     * @param startAddress
     */
    public void setStartAddress(Address startAddress) {
        for(Journey journey: alternativeJourneys) {
            journey.startAddress = startAddress;
        }
    }

    /**
     * Sets the endAddress on all alternative journeys in the response.
     * TODO: Consider moving this to a Journey constructor, passed along when calling Geocoder
     * @param endAddress
     */
    public void setEndAddress(Address endAddress) {
        for(Journey journey: alternativeJourneys) {
            journey.endAddress = endAddress;
        }
    }
}
