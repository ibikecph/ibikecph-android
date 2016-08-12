// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO: This code comes from previous vendor. It's a mess. /jc
public abstract class SMRoute implements SMHttpRequestListener, LocationListener {

    public static float DESTINATION_METRES_THRESHOLD = 40.0f;
    public static int DESTINATION_SECONDS_THRESHOLD = 3;

    public static final float MAX_DISTANCE_FROM_PATH = 30.0f;
    public static final float MIN_DISTANCE_FOR_RECALCULATION = MAX_DISTANCE_FROM_PATH;

    int lastVisitedPointIndex = 0;

    /**
     * Turn instructions on the route that the user has passed
     */
    protected List<SMTurnInstruction> pastTurnInstructions = new LinkedList<>();

    /**
     * Turn instructions from the next upcoming instruction to the last
     */
    protected ArrayList<SMTurnInstruction> upcomingTurnInstructions = new ArrayList<>();

    public List<Location> visitedLocations;
    float averageSpeed;
    float caloriesBurned;
    public Location locationStart;
    public Location locationEnd;
    public boolean recalculationInProgress;
    protected int estimatedDuration, estimatedDurationLeft;
    protected float estimatedDistance, estimatedDistanceLeft;
    protected String routeChecksum;
    protected String destinationHint;
    public Location lastCorrectedLocation;
    public double lastCorrectedHeading;
    public float distanceFromRoute;
    protected Location lastRecalcLocation;
    public String startStationName, endStationName;
    public boolean reachedDestination = false;
    public int waypointStation1 = -1, waypointStation2 = -1;
    private RouteType type;

    public Address startAddress, endAddress;

    protected List<SMRouteListener> listeners = new CopyOnWriteArrayList<>();

    public String getDestinationHint() {
        return destinationHint;
    }

    public void setLastRecalcLocation(Location lastRecalcLocation) {
        this.lastRecalcLocation = lastRecalcLocation;
    }

    // Variables for breakRoute
    public TransportationType transportType;
    public String description = null;
    // TODO: Convert these into Date objects
    public long departureTime, arrivalTime;

    public SMRoute(Location start, Location end, RouteType type) {
        init();
        locationStart = start;
        locationEnd = end;
        this.type = type;
    }

    public void init() {
        estimatedDistanceLeft = -1;
        caloriesBurned = -1;
        averageSpeed = -1;
        recalculationInProgress = false;
        lastRecalcLocation = Util.locationFromCoordinates(0, 0);
        reachedDestination = false;
        waypointStation1 = -1;
        waypointStation2 = -1;

        transportType = TransportationType.BIKE;
        description = null;
        departureTime = -1;
        arrivalTime = -1;
    }

    public boolean isPublicTransportation() {
        return TransportationType.isPublicTransportation(transportType);
    }

    public void removeListeners() {
        this.listeners.clear();
    }

    public void removeListener(SMRouteListener listener) {
        if(listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public abstract void addListener(SMRouteListener listener);

    @Override
    public void onResponseReceived(int requestType, Object response) {
        switch (requestType) {
            case SMHttpRequest.REQUEST_GET_ROUTE:
                throw new RuntimeException("Cannot use the REQUEST_GET_ROUTE anymore.");
            case SMHttpRequest.REQUEST_GET_RECALCULATED_ROUTE:
                final JsonNode jRoot = ((RouteInfo) response).jsonRoot;

                int statusCode = jRoot == null || jRoot.path("status") == null ?
                                 -1 :
                                 jRoot.path("status").asInt();
                // OSRM v3 has status 0 on success, v4.9 has 200
                if (statusCode != 200 && statusCode != 0) {
                    emitServerError();
                    recalculationInProgress = false;
                    return;
                }

                final Handler h = new Handler();
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        boolean ok = parseFromJson(jRoot);

                        if (ok) {
                            h.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lastLocation != null) {
                                        visitLocation(lastLocation);
                                    }
                                    if (visitedLocations != null && visitedLocations.size() > 0) {
                                        // visitLocation(visitedLocations.get(visitedLocations.size() - 1));
                                    }
                                    if (IBikeApplication.getService().hasValidLocation()) {
                                        updateDistances(IBikeApplication.getService().getLastValidLocation());
                                    }
                                    recalculationInProgress = false;
                                    emitRouteRecalculationDone();
                                    emitRouteUpdated();
                                }
                            });
                        } else {
                            h.post(new Runnable() {
                                @Override
                                public void run() {
                                    recalculationInProgress = false;
                                    emitServerError();
                                }
                            });
                        }

                    }
                });
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
        }
    }

    /**
     * Emits that the route has updated.
     */
    public void emitRouteUpdated() {
        for(SMRouteListener listener: listeners) {
            listener.updateRoute();
        }
    }

    /**
     * Emits that the route recalculation is done.
     */
    protected void emitRouteRecalculationDone() {
        for(SMRouteListener listener: listeners) {
            listener.routeRecalculationDone();
        }
    }

    /**
     * Emits that a server error occurred when fetching the route.
     */
    protected void emitServerError() {
        for(SMRouteListener listener: listeners) {
            listener.serverError();
        }
    }

    /**
     * Emits that the route is ready to be started
     */
    protected void emitStartRoute() {
        for(SMRouteListener listener: listeners) {
            listener.startRoute();
        }
    }

    /**
     * Emits that no route was found
     */
    protected void emitRouteNotFound() {
        for(SMRouteListener listener: listeners) {
            listener.routeNotFound();
        }
    }

    /**
     * Emits that the routes destination was reached
     */
    protected void emitDestinationReached() {
        for(SMRouteListener listener: listeners) {
            listener.reachedDestination();
        }
    }

    /**
     * Emits that the user has diverged too much from the route, and a recalculation has started
     */
    protected void emitRouteRecalculationStarted() {
        for(SMRouteListener listener: listeners) {
            listener.routeRecalculationStarted();
        }
    }

    public abstract Location getStartLocation();

    public abstract LatLng getRealStartLocation();

    public abstract Location getEndLocation();

    public abstract LatLng getRealEndLocation();

    public abstract List<Location> getPoints();

    public abstract List<SMTurnInstruction> getUpcomingTurnInstructions();

    public abstract List<SMTurnInstruction> getPastTurnInstructions();

    public abstract boolean parseFromJson(JsonNode jsonRoot);

    /**
     * Returns the estimated amount of seconds to the destination.
     * @return
     */
    public abstract float getEstimatedDuration();

    /**
     * Get the estimated amount of seconds to the destination.
     * @return duration in seconds
     */
    public abstract float getEstimatedDurationLeft();

    /**
     * Get the estimated total distance
     * @return distance in metres
     */
    public abstract float getEstimatedDistance();

    Location lastLocation;

    /**
     * Updates the route when a user has visited a particular location.
     * TODO: Clean this up - a lot ...
     * @param loc
     */
    public void visitLocation(Location loc) {
        // TODO: Consider moving this to the end of the method for semantics
        lastLocation = loc;
        visitedLocations.add(loc);

        // Let's do nothing if there are no more turn instructions.
        if (upcomingTurnInstructions.size() <= 0 && type != RouteType.BREAK) {
            return;
        }

        // Let's do nothing if a recalculation is in progress.
        if (recalculationInProgress) {
            return;
        }

        updateDistances(loc);

        estimatedDurationLeft = Math.round(estimatedDistanceLeft * estimatedDuration / estimatedDistance);

        // TODO: Consider if this check is need - maybe we only want to reach the destination once.
        if (!reachedDestination) {
            // Are we close to the finish? Or have no more instructions?
            if (estimatedDistanceLeft < DESTINATION_METRES_THRESHOLD ||
                estimatedDurationLeft <= DESTINATION_SECONDS_THRESHOLD ||
                upcomingTurnInstructions.isEmpty()) {
                // Move all future turn instructions to the past instructions
                pastTurnInstructions.addAll(upcomingTurnInstructions);
                upcomingTurnInstructions.clear();
                // Remove this route as a listner of location updates.
                IBikeApplication.getService().removeLocationListener(this);
                // Declare that we've reached the destination
                // TODO: Make this a derived method of the fact that we have no more turn instructions
                reachedDestination = true;
                // Let's update to reflect that we do not estimate any more time or distance
                estimatedDistanceLeft = 0;
                estimatedDurationLeft = 0;
                // Tell any listener that we have arrived.
                emitDestinationReached();
                return;
            }
        }

        // Check if we went too far from the calculated route and, if so,
        // recalculate route. The maximum allowed distance depends on location's accuracy.
        float maximalDistance = MAX_DISTANCE_FROM_PATH + loc.getAccuracy() / 3;

        // Only recalculate if we're walking or biking or transportation type is irrelevant
        if (transportType == TransportationType.BIKE || transportType == TransportationType.WALK) {
            boolean navigationStarted = pastTurnInstructions.size() > 0;
            if (navigationStarted && !approachingFinish() && listeners.size() > 0 && isTooFarFromRoute(loc, maximalDistance)) {
                recalculateRoute(loc);
                return;
            }
        }

        int closestWaypointIndex = -1;
        double minD = Double.MAX_VALUE;
        if (upcomingTurnInstructions != null && upcomingTurnInstructions.size() > 0) {
            List<Location> points = getPoints();
            if (points.size() > 0) {
                // find the closest waypoint
                for (int i = lastVisitedPointIndex; i < points.size(); i++) {
                    Location point = points.get(i);
                    if (loc.distanceTo(point) < minD) {
                        minD = loc.distanceTo(point);
                        closestWaypointIndex = i;
                    }
                }
            }
        }

        if (closestWaypointIndex > -1 && !recalculationInProgress) {
            synchronized (SMRoute.this) {

                Iterator<SMTurnInstruction> it = upcomingTurnInstructions.iterator();
                // int i = 0;
                while (it.hasNext()) {
                    SMTurnInstruction instruction = it.next();
                    double d = loc.distanceTo(instruction.location);
                    if (closestWaypointIndex > instruction.getPointsIndex()) {
                        // We have definitely passed the instruction - remove it
                        it.remove();
                        pastTurnInstructions.add(instruction);
                    } else if (!instruction.plannedForRemoval && d < instruction.getTransitionDistance()) {
                        // We are approaching the instruction - should be removed when moving away from it.
                        Log.d("SMRoute", "Approaching the instruction " + instruction + " (removing it soon)");
                        instruction.plannedForRemoval = true;
                    } else if (instruction.plannedForRemoval && d >= instruction.getTransitionDistance()) {
                        // Now we are moving away from the instruction
                        Log.d("SMRoute", "Passed the instruction " + instruction + " (removing it)");
                        it.remove();
                        pastTurnInstructions.add(instruction);
                    }
                }
            }

        }

        emitRouteUpdated();
    }

    public void recalculateRoute(Location loc) {
        // We need a proper end location and a current location
        Location end = getEndLocation();
        if (loc == null || end == null)
            return; // TODO: Consider throwing an error instead

        // Let's not recalculate twice at the same time
        if (recalculationInProgress) {
            return;
        }

        // Distance throttling the calculation of new routes
        float distance = loc.distanceTo(lastRecalcLocation);
        if (distance < MIN_DISTANCE_FOR_RECALCULATION) {
            return;
        }
        lastRecalcLocation = loc;

        // Let's start the recalculation
        recalculationInProgress = true;
        emitRouteRecalculationStarted();

        new SMHttpRequest().getRecalculatedRoute(loc, end, null, routeChecksum, null, destinationHint, this.type, this);
    }

    boolean checkLocation(Location loc, float maxDistance) {
        SMTurnInstruction nextTurn = upcomingTurnInstructions.get(Math.min(upcomingTurnInstructions.size() - 1, 2));
        if (nextTurn != null) {
            if (!isTooFarFromRouteSegment(loc, null, nextTurn, maxDistance)) {
                return false;
            }
        }
        return true;
    }

    public boolean isTooFarFromRoute(Location loc, float maxDistance) {
        if (upcomingTurnInstructions.size() > 0) {
            SMTurnInstruction currentTurn = upcomingTurnInstructions.get(0);
            lastCorrectedLocation = new Location(loc);

            // TODO: Consider if this condition will ever be true?
            if (pastTurnInstructions.size() < 0) {
                // lastCorrectedHeading = SMGPSUtil.bearingBetween(location,
                // currentTurn.location);
                // We have passed no turns. Check if we have managed to get on
                // the route somehow.
                if (currentTurn != null) {
                    double currentDistanceFromStart = loc.distanceTo(currentTurn.location);
                    LOG.d("Current distance from start: " + currentDistanceFromStart);
                    if (currentDistanceFromStart > maxDistance) {
                        return checkLocation(loc, maxDistance);
                    }
                }
                return false;
            }

            distanceFromRoute = Float.MAX_VALUE;
            return checkLocation(loc, maxDistance);
        }
        return false;
    }

    private boolean isTooFarFromRouteSegment(Location loc, SMTurnInstruction turnA, SMTurnInstruction turnB, double maxDistance) {
        double min = Float.MAX_VALUE;

        List<Location> points = getPoints();

        for (int i = lastVisitedPointIndex; i < turnB.getPointsIndex(); i++) {
            try {
                Location a = points.get(i);
                Location b = points.get(i + 1);
                double d = SMGPSUtil.distanceFromLineInMeters(loc, a, b);
                if (d < 0.0)
                    continue;
                if (d <= min) {
                    min = d;
                    lastVisitedPointIndex = i;
                }
                if (min < 2) {
                    // Close enough :)
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }

        if (min <= maxDistance && min < distanceFromRoute) {
            distanceFromRoute = (float) min;

            Location a = points.get(lastVisitedPointIndex);
            Location b = points.get(lastVisitedPointIndex + 1);
            Location coord = SMGPSUtil.closestCoordinate(loc, a, b);
            if (a.distanceTo(b) > 0.0f) {
                lastCorrectedHeading = SMGPSUtil.bearingBetween(a, b);
            }

            if (visitedLocations != null && visitedLocations.size() > 0) {
                lastCorrectedLocation = new Location(loc);
                lastCorrectedLocation.setLatitude(coord.getLatitude());
                lastCorrectedLocation.setLongitude(coord.getLongitude());
            }
        }

        return min > maxDistance;
    }

    private boolean approachingFinish() {
        boolean isNear = false;
        if (locationEnd != null && lastLocation != null) {
            isNear = locationEnd.distanceTo(lastLocation) <= 20;
        }
        return isNear && upcomingTurnInstructions.size() == 1;
    }

    public void updateDistances(Location loc) {
        if (estimatedDistanceLeft < 0.0) {
            estimatedDistanceLeft = estimatedDistance;
        }

        if (upcomingTurnInstructions.size() > 0) {
            // Calculate distance from location to the next turn
            SMTurnInstruction nextTurn = upcomingTurnInstructions.get(0);
            nextTurn.distance = calculateDistanceToNextTurn(loc);

            estimatedDistanceLeft = nextTurn.distance;
            // Calculate distance from next turn to the end of the route
            for (int i = 1; i < upcomingTurnInstructions.size(); i++) {
                estimatedDistanceLeft += upcomingTurnInstructions.get(i).distance;
            }
        }
    }

    protected float calculateDistanceToNextTurn(Location loc) {
        if (upcomingTurnInstructions.size() == 0)
            return 0.0f;

        SMTurnInstruction nextTurn = upcomingTurnInstructions.get(0);

        // If first turn still hasn't been reached, return linear distance to
        // it.
        if (pastTurnInstructions.size() == 0)
            return loc.distanceTo(nextTurn.location);

        int firstIndex = lastVisitedPointIndex >= 0 ? lastVisitedPointIndex + 1 : 0;
        float distance = 0.0f;
        List<Location> points = getPoints();
        if (firstIndex < points.size()) {
            distance = loc.distanceTo(points.get(firstIndex));
            if (nextTurn.getPointsIndex() <= points.size()) {
                for (int i = firstIndex; i < nextTurn.getPointsIndex(); i++) {
                    double d = points.get(i).distanceTo(points.get(i + 1));
                    distance += d;
                }
            }
        }

        return distance;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.visitLocation(location);
    }

    public RouteType getType() {
        return type;
    }

    public abstract float getEstimatedDistanceLeft();
    /*{
        return estimatedDistanceLeft;
    }*/

}
