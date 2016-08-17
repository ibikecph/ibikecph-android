// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.NavigationState;
import com.spoiledmilk.ibikecph.search.Address;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Route is intended as an immutable data structure. It captures the users future journey along
 * a list of locations (called steps). The user is given instructions on a subset (called steps) of
 * these locations.
 */
public class Route {

    /**
     * The assumed biking speed in metres per second.
     */
    public static final float AVERAGE_BIKING_SPEED = 15f * 1000f / 3600f;

    /**
     * The assumed cargo biking speed in metres per second.
     */
    public static final float AVERAGE_CARGO_BIKING_SPEED = 10f * 1000f / 3600f;

    /**
     * A route has one or more legs, each of which might have a different transportation type and
     * possibly fixed departure and arrival times.
     */
    protected List<Leg> legs = new ArrayList<>();

    /**
     * A hint that enables the OSRM server to recalculate the route, faster
     */
    protected String destinationHint;

    /**
     * What type of route did the user ask for?
     */
    protected RouteType type;

    /**
     * The start address that the route departs from.
     */
    protected Address startAddress;

    /**
     * The end address that the route arrives at.
     */
    protected Address endAddress;

    /**
     * The state of navigation for the route, if any.
     * This object holds information on which leg and step is coming up.
     */
    protected NavigationState state;

    public String getDestinationHint() {
        return destinationHint;
    }

    public String description = null;

    public Route(RouteType type) {
        this.type = type;
    }

    protected List<RouteListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Removes all listeners from the Route.
     */
    public void removeListeners() {
        this.listeners.clear();
    }

    /**
     * Removes a particular listener from the route.
     * @param listener the listener to be removed
     */
    public void removeListener(RouteListener listener) {
        if(listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**
     * Adds a listener to the route.
     * @param listener the listener to add.
     */
    public void addListener(RouteListener listener) {
        if(listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Emits that the route has updated.
     */
    public void emitRouteChanged() {
        for(RouteListener listener: listeners) {
            listener.routeChanged();
        }
    }

    public Location getStartLocation() {
        if(legs.size() > 0) {
            Leg firstLeg = legs.get(0);
            return firstLeg.getStartLocation();
        } else {
            throw new RuntimeException("Cannot get the start location of a route without legs");
        }
    }

    public LatLng getRealStartLocation() {
        return startAddress.getLocation();
    }

    public Location getEndLocation() {
        if(legs.size() > 0) {
            Leg lastLeg = legs.get(legs.size()-1);
            return lastLeg.getEndLocation();
        } else {
            throw new RuntimeException("Cannot get the end location of a route without legs");
        }
    }

    public LatLng getRealEndLocation() {
        return endAddress.getLocation();
    }

    public List<Location> getPoints() {
        List<Location> result = new ArrayList<>();
        for(Leg leg: legs) {
            result.addAll(leg.getPoints());
        }
        return result;
    }

    public boolean parseFromJson(JsonNode routeNode) {
        synchronized (this) {
            if (routeNode == null) {
                return false;
            }

            if (routeNode.get("legs") == null ||
                !routeNode.get("legs").isArray() ||
                routeNode.get("legs").size() == 0) {
                throw new RuntimeException("Expected at least one item in the routes 'legs' field");
            }

            legs.clear();
            for(JsonNode legNode: routeNode.get("legs")) {
                Leg leg = new Leg(legNode);
                legs.add(leg);
            }

            // Do we have just one leg?
            if(legs.size() == 1) {
                // Does this only leg have no points and do the route as a whole have a geometry?
                Leg onlyLeg = legs.get(0);
                if(onlyLeg.getPoints().size() == 0 && routeNode.has("geometry")) {
                    // Then let's decode this only legs points from the overall route geometry.
                    onlyLeg.decodePointsFromPolyline(routeNode.get("geometry").textValue());
                }
            }

            for(Leg leg: legs) {
                // Update the pointIndex on all steps
                leg.updateStepPointIndices();
            }
        }
        return true;
    }

    public RouteType getType() {
        return type;
    }

    public double getDuration(Leg leg) {
        // If departure and arrival time is specified, the duration is the difference
        if(leg.getDepartureTime() > 0 && leg.getArrivalTime() > 0) {
            return leg.arrivalTime - leg.departureTime;
        } else if(getType().equals(RouteType.CARGO)) {
            return leg.getDistance() / AVERAGE_CARGO_BIKING_SPEED;
        } else {
            return leg.getDistance() / AVERAGE_BIKING_SPEED;
        }
    }

    /**
     * Get the estimated distance of a specific type of transportation.
     * @return distance in metres
     */
    public double getEstimatedDistance(boolean nonPublicOnly) {
        return NavigationState.getEstimatedDistance(nonPublicOnly, this);
    }

    public void setStartAddress(Address startAddress) {
        this.startAddress = startAddress;
    }

    public void setEndAddress(Address endAddress) {
        this.endAddress = endAddress;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public Address getStartAddress() {
        return startAddress;
    }

    public Address getEndAddress() {
        return endAddress;
    }

    /**
     * Returns the estimated duration of the entire journey
     * @return duration in seconds
     */
    public int getDuration() {
        int result = 0;
        for(Leg leg: legs) {
            result += getDuration(leg);
        }
        return result;
    }

    public Date getArrivalTime() {
        return NavigationState.getArrivalTime(this);
    }

    /**
     * Does the route currently have a navigation state?
     * @return true if the route currently has a navigation state associated, false otherwise.
     */
    public boolean hasState() {
        return state != null;
    }

    public NavigationState getState() {
        return state;
    }

    public void setState(NavigationState state) {
        this.state = state;
    }

    public void replaceLeg(Leg oldLeg, Leg newLeg) {
        if(legs.contains(oldLeg)) {
            int oldLegIndex = legs.indexOf(oldLeg);
            legs.set(oldLegIndex, newLeg);
        } else {
            throw new RuntimeException("Was asked to replace a leg that was not a part of the route");
        }
    }
}
