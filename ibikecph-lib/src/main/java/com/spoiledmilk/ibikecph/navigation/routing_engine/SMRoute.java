// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction.TurnDirection;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class SMRoute implements SMHttpRequestListener {

	public static final int MAX_DISTANCE_FROM_PATH = 20;
    public static final int MIN_DISTANCE_FOR_RECALCULATION = 20;
    public static final int TO_START_STATION = 0;
    public static final int TO_END_STATION = 1;
    public static final int TO_DESTINATION = 2;
    public static final double DISTANCE_TO_REMOVE_TURN = 20d;

    public int routePhase = TO_START_STATION;
    public boolean approachingTurn;
    public double distanceFromStart;
    SMRouteListener listener;
    public List<Location> waypoints;
    public List<SMTurnInstruction> allTurnInstructions;
    public List<SMTurnInstruction> pastTurnInstructions; // turn instructions
                                                         // from
    // first to the last passed
    // turn
    public ArrayList<SMTurnInstruction> turnInstructions; // turn instruction
                                                          // from next
    // to the last
    public List<Location> visitedLocations;
    float distanceLeft;
    float tripDistance;
    float averageSpeed;
    float caloriesBurned;
    public Location locationStart;
    public Location locationEnd;
    public boolean recalculationInProgress;
    float estimatedArrivalTime, arrivalTime;
    int estimatedRouteDistance = -1;
    String routeChecksum;
    String destinationHint;
    public Location lastCorrectedLocation;
    public double lastCorrectedHeading;
    public int lastVisitedWaypointIndex;
    public float distanceFromRoute;
    private String viaStreets;
    private Location lastRecalcLocation;
    public Location startStation, endStation;
    public String startStationName, endStationName;
    public int stationIcon;
    public boolean isRouteBroken = false;
    public SMTurnInstruction station1, station2;
    public JsonNode jsonRoot, jsonRoot2;
    public int startStatIndex = 0, endStatIndex = 0;
    public boolean reachedDestination = false;
    public int waypointStation1 = -1, waypointStation2 = -1;

    public SMRoute() {
        init();
    }

    public void init() {
        distanceLeft = -1;
        tripDistance = -1;
        caloriesBurned = -1;
        averageSpeed = -1;
        approachingTurn = false;
        lastVisitedWaypointIndex = -1;
        recalculationInProgress = false;
        lastRecalcLocation = Util.locationFromCoordinates(0, 0);
        allTurnInstructions = new ArrayList<SMTurnInstruction>();
        reachedDestination = false;
        waypointStation1 = -1;
        waypointStation2 = -1;
    }

    public void init(Location start, Location end, SMRouteListener listener, JsonNode routeJSON) {
        init();

        locationStart = start;
        locationEnd = end;
        setListener(listener);
        if (routeJSON == null) {
            LOG.d("SMRoute init() jsonRoot is null, trying to get route from here...");
            new SMHttpRequest().getRoute(start, end, null, this);
        } else {
            setupRoute(routeJSON);
        }
    }

    public void init(Location start, Location end, Location startStation, Location endStation, SMRouteListener listener, JsonNode routeJSON) {
        init();
        locationStart = start;
        locationEnd = end;
        setListener(listener);
        setupBrokenRoute(routeJSON);
    }

    public void setListener(SMRouteListener listener) {
        this.listener = listener;
    }

    // SMRequestOSRMListener callback

    @Override
    public void onResponseReceived(int requestType, Object response) {
        switch (requestType) {
            case SMHttpRequest.REQUEST_GET_ROUTE:
                JsonNode jsonRoot = ((RouteInfo) response).jsonRoot;
                if (jsonRoot == null || jsonRoot.path("status").asInt(-1) != 0) {
                    if (listener != null)
                        listener.routeNotFound();
                } else {
                    setupRoute(jsonRoot);
                    if (listener != null)
                        listener.startRoute();
                }
                break;
            case SMHttpRequest.REQUEST_GET_RECALCULATED_ROUTE:
                final JsonNode jRoot = ((RouteInfo) response).jsonRoot;

                if (jRoot == null || jRoot.path("status").asInt() != 0) {
                    if (listener != null) {
                        listener.serverError();
                    }
                    recalculationInProgress = false;
                    return;
                }

                final Handler h = new Handler();
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        boolean ok = parseFromJson(jRoot, null, isRouteBroken);

//                        logWaypoints();

                        if (ok) {
                            approachingTurn = false;
                            h.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lastLocation != null) {
                                        visitLocation(lastLocation);
                                    }
                                    if (visitedLocations != null && visitedLocations.size() > 0) {
                                        // visitLocation(visitedLocations.get(visitedLocations.size() - 1));
                                    }
                                    if (SMLocationManager.getInstance().hasValidLocation()) {
                                        updateDistances(SMLocationManager.getInstance().getLastValidLocation());
                                    }
                                    if (listener != null) {
                                        listener.routeRecalculationDone();
                                        listener.updateRoute();
                                    }
                                    recalculationInProgress = false;
                                }
                            });
                        } else {
                            h.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null)
                                        listener.serverError();
                                    recalculationInProgress = false;
                                }
                            });
                        }

                    }
                });
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
        }
    }

    public Location getStartLocation() {
        if (waypoints != null && waypoints.size() > 0)
            return waypoints.get(0);
        return null;
    }

    public Location getEndLocation() {
        if (waypoints != null && waypoints.size() > 0)
            return waypoints.get(waypoints.size() - 1);
        return null;
    }

    public List<Location> getWaypoints() {
        return waypoints;
    }

    public ArrayList<SMTurnInstruction> getTurnInstructions() {
        return turnInstructions;
    }

    private void setupRoute(JsonNode jsonRoot) {
        LOG.d("SMRoute setupRoute()");

        boolean ok = parseFromJson(jsonRoot, null, false);
        if (ok) {
            approachingTurn = false;
            tripDistance = 0.0f;

            if (SMLocationManager.getInstance().hasValidLocation()) {
                updateDistances(SMLocationManager.getInstance().getLastValidLocation());
            }
        }
    }

    private void setupBrokenRoute(JsonNode jsonRoot) {
        boolean ok = parseFromJson(jsonRoot, null, true);
        if (ok) {
            approachingTurn = false;
            tripDistance = 0.0f;
            if (SMLocationManager.getInstance().hasValidLocation()) {
                updateDistances(SMLocationManager.getInstance().getLastValidLocation());
            }
        }
    }

    boolean parseFromJson(JsonNode jsonRoot, SMRouteListener listener, boolean isBrokenRoute) {
        Log.d("SMRoute", "parseFromJson() json = " + jsonRoot);
        synchronized (this) {
            if (jsonRoot == null) {
                return false;
            }
            waypoints = decodePolyline(jsonRoot.path("route_geometry").textValue());

            if (waypoints.size() < 2) {
                return false;
            }

            turnInstructions = new ArrayList<SMTurnInstruction>();
            pastTurnInstructions = new LinkedList<SMTurnInstruction>();
            visitedLocations = new ArrayList<Location>();
            estimatedArrivalTime = jsonRoot.path("route_summary").path("total_time").asInt();
            arrivalTime = estimatedArrivalTime;
            distancePassed = 0d;
            if (estimatedRouteDistance < 0)
                estimatedRouteDistance = jsonRoot.path("route_summary").path("total_distance").asInt();

            routeChecksum = null;
            destinationHint = null;

            if (!jsonRoot.path("hint_data").path("checksum").isMissingNode()) {
                routeChecksum = jsonRoot.path("hint_data").path("checksum").asText();
            }

            JsonNode hint_locations = jsonRoot.path("hint_data").path("locations");
            if (hint_locations != null && !hint_locations.isMissingNode() && hint_locations.size() > 0) {
                destinationHint = jsonRoot.path("hint_data").path("locations").get(hint_locations.size() - 1).asText();
            }

            JsonNode routeInstructionsArr = jsonRoot.path("route_instructions");
            if (routeInstructionsArr != null && routeInstructionsArr.size() > 0) {
                int prevlengthInMeters = 0;
                String prevlengthWithUnit = "";
                boolean isFirst = true;
                for (JsonNode instructionNode : routeInstructionsArr) {
                    SMTurnInstruction instruction = new SMTurnInstruction();

                    String[] arr = instructionNode.get(0).asText().split("-");
                    if (arr.length < 1)
                        continue;
                    int pos = Integer.valueOf(arr[0]);
                    if (pos <= 17) {
                        instruction.drivingDirection = TurnDirection.values()[pos];
                        if (arr.length > 1 && arr[1] != null) {
                            instruction.ordinalDirection = arr[1];
                        } else {
                            instruction.ordinalDirection = "";
                        }

                        instruction.wayName = instructionNode.get(1).asText();
                        if (instruction.wayName.matches("\\{.+\\:.+\\}"))
                            instruction.wayName = IbikeApplication.getString(instruction.wayName);
                        instruction.wayName = instruction.wayName.replaceAll("&#39;", "'");
                        instruction.lengthInMeters = prevlengthInMeters;
                        prevlengthInMeters = instructionNode.get(2).asInt();
                        instruction.timeInSeconds = instructionNode.get(4).asInt();
                        instruction.lengthWithUnit = prevlengthWithUnit;
                        if (instructionNode.size() > 8) {
                            instruction.vehicle = instructionNode.get(8).asInt();
                        }
                        /**
                         * Save length to next turn with units so we don't have to generate it each time It's formatted just the way we like it
                         */
                        instruction.fixedLengthWithUnit = Util.formatDistance(prevlengthInMeters);
                        prevlengthWithUnit = instructionNode.get(5).asText();
                        instruction.directionAbrevation = instructionNode.get(6).asText();
                        instruction.azimuth = (float) instructionNode.get(7).asDouble();

                        if (isFirst) {
                            instruction.generateStartDescriptionString();
                            isFirst = false;
                        } else {
                            instruction.generateDescriptionString();
                        }
                        instruction.generateFullDescriptionString();
                        int position = instructionNode.get(3).asInt();
                        instruction.waypointsIndex = position;
                        if (waypoints != null && position >= 0 && position < waypoints.size())
                            instruction.loc = waypoints.get(position);
                        turnInstructions.add(instruction);
                    }
                }
            }
            if (isRouteBroken && turnInstructions != null) {

                double dStat1 = Double.MAX_VALUE, dStat2 = Double.MAX_VALUE;
                for (int i = 0; i < waypoints.size(); i++) {
                    if (startStation.distanceTo(waypoints.get(i)) < dStat1) {
                        dStat1 = startStation.distanceTo(waypoints.get(i));
                        waypointStation1 = i;
                    }
                    if (endStation.distanceTo(waypoints.get(i)) < dStat2) {
                        dStat2 = endStation.distanceTo(waypoints.get(i));
                        waypointStation2 = i;
                    }
                }

                Iterator<SMTurnInstruction> it2 = turnInstructions.iterator();
                float distToStart = Float.MAX_VALUE, distToEnd = Float.MAX_VALUE;
                while (it2.hasNext()) {
                    SMTurnInstruction smt = it2.next();
                    if (smt.loc.distanceTo(startStation) < distToStart && smt.waypointsIndex <= waypointStation1) {
                        distToStart = smt.loc.distanceTo(startStation);
                        station1 = smt;
                    }
                    if (smt.loc.distanceTo(endStation) < distToEnd && smt.waypointsIndex <= waypointStation2) {
                        distToEnd = smt.loc.distanceTo(endStation);
                        station2 = smt;
                    }
                }

                station1.convertToStation(startStationName, stationIcon);
                station2.convertToStation(endStationName, stationIcon);
                int startIndex = turnInstructions.indexOf(station1);
                int endIndex = turnInstructions.indexOf(station2);
                while (startIndex < endIndex - 1) {
                    turnInstructions.remove(startIndex + 1);
                    startIndex = turnInstructions.indexOf(station1);
                    endIndex = turnInstructions.indexOf(station2);
                }
            }

            int longestStreet = 0;
            viaStreets = "";

            int n = jsonRoot.path("route_name").size();
            if (n > 0) {
                int i = 0;
                for (JsonNode streetNode : jsonRoot.path("route_name")) {
                    i++;
                    viaStreets += streetNode.asText() + (i == n ? "" : ", ");
                }
            }
            if (viaStreets == null || viaStreets.trim().equals("")) {
                for (int i = 1; i < turnInstructions.size() - 1; i++) {
                    SMTurnInstruction inst = turnInstructions.get(i);
                    if (inst.lengthInMeters > longestStreet) {
                        longestStreet = inst.lengthInMeters;
                        viaStreets = turnInstructions.get(i - 1).wayName;
                    }
                }
            }

            lastVisitedWaypointIndex = 0;
        }
        return true;
    }

    /*
     * Decoder for the Encoded Polyline Algorithm Format https://developers.google .com/maps/documentation/utilities/polylinealgorithm
     */
    List<Location> decodePolyline(String encodedString) {
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
        for (int i = 0; i < len;) {
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
            Location loc = Util.locationFromCoordinates((double) lat / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LATITUDE, (double) lng / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LONGITUDE);
            locations.add(loc);
        }

        return locations;
    }

    public float getEstimatedArrivalTime() {
        return arrivalTime;
    }

    public int getEstimatedDistance() {
        return estimatedRouteDistance;
    }

    ArrayList<Double> speedData = new ArrayList<Double>();
    final int SPEEDS_COUNT = 20;
    int speedIndex = 0;
    double distancePassed = 0;
    Location lastLocation;

    // Turn by Turn
    public void visitLocation(Location loc) {
        if (lastLocation != null && loc != null) {
            distancePassed += loc.distanceTo(lastLocation);
        }

        lastLocation = loc;

        visitedLocations.add(loc);

        if (turnInstructions.size() <= 0)
            return;

        if (recalculationInProgress) {
            return;
        }

        // Check if we are finishing:
        double distanceToFinish = loc.distanceTo(getEndLocation());

        arrivalTime = distanceLeft * estimatedArrivalTime / estimatedRouteDistance;

        // Calculate the average speed and update the ETA
        double speed = loc.getSpeed() > 0 ? loc.getSpeed() : 5;

        int timeToFinish = 100;
        if (speed > 0) {
            timeToFinish = (int) (distanceToFinish / speed); // A bike travels approximately 5 meters per second
        }

        // are we close to the finish (< 10m or 3s left)?
        if (distanceToFinish < 10.0 || timeToFinish <= 3) {
            LOG.d("finishing in " + distanceToFinish + " m and " + timeToFinish + " s");
            if (turnInstructions.size() == 1) {
                // if there was only one instruction left go through usual
                // channels
                approachingTurn = false;
                // removeTurn();
                if (listener != null) {
                    reachedDestination = true;
                    listener.reachedDestination();
                }
                return;
            } else {
                // we have somehow skipped most of the route (going through a
                // park or unknown street)
                if (listener != null) {
                    reachedDestination = true;
                    listener.reachedDestination();
                }
                return;
            }
        }

        // Check if we went too far from the calculated route and, if so,
        // recalculate route
        // max allowed distance depends on location's accuracy
        int maxD = loc.getAccuracy() > 0.0 ? (int) (loc.getAccuracy() / 3 + 20) : MAX_DISTANCE_FROM_PATH;
        if (!approachingFinish() && listener != null && isTooFarFromRoute(loc, maxD)) {
            approachingTurn = false;
            recalculateRoute(loc, false);
            return;
        }

        int closestWaypointIndex = -1;
        double minD = Double.MAX_VALUE;
        Location projectedLoc = null;
        if (turnInstructions != null && turnInstructions.size() > 0) {
            // find the projected location on the route segment
            // check only a couple of the first instructions
            // for (int i = 0; i < turnInstructions.size() - 1 && i < 3; i++) {
            // double d = SMGPSUtil.distanceFromLineInMeters(loc, turnInstructions.get(i).loc, turnInstructions.get(i +
            // 1).loc);
            // if (d < minD && d < 20) {
            // projectedLoc = SMGPSUtil.closestCoordinate(loc, turnInstructions.get(i).loc, turnInstructions.get(i +
            // 1).loc);
            // }
            // }
            // minD = Double.MAX_VALUE;

            // if (lastCorrectedLocation != null && lastCorrectedLocation.distanceTo(loc) < 30) {
            // projectedLoc = lastCorrectedLocation;
            // }

            projectedLoc = loc;

            if (projectedLoc != null) {
                if (waypoints != null && waypoints.size() > 0) {
                    // find the closest waypoint
                    for (int i = lastVisitedWaypointIndex; i < waypoints.size(); i++) {
                        Location waypoint = waypoints.get(i);
                        if (projectedLoc.distanceTo(waypoint) < minD) {
                            minD = projectedLoc.distanceTo(waypoint);
                            closestWaypointIndex = i;
                        }
                    }
                }
            }
        }

        try {
            LOG.d("routing debug closestWaypointIndex = " + closestWaypointIndex);
            LOG.d("routing debug loc = " + Util.locationString(loc));
            LOG.d("routing debug instructions[0] = " + turnInstructions.get(0).fullDescriptionString + " loc = "
                    + Util.locationString(turnInstructions.get(0).loc) + " waypoint index = " + turnInstructions.get(0).waypointsIndex);
        } catch (Exception e) {

        }

        if (closestWaypointIndex > -1 && !recalculationInProgress) {
            synchronized (SMRoute.this) {

                Iterator<SMTurnInstruction> it = turnInstructions.iterator();
                // int i = 0;
                while (it.hasNext()) {
                    SMTurnInstruction instruction = it.next();
                    double d = loc.distanceTo(instruction.loc);
                    if (closestWaypointIndex < instruction.waypointsIndex) {
                        // future instruction, stop the loop
                        instruction.lastD = loc.distanceTo(instruction.loc);
                        break;
                    } else if (closestWaypointIndex > instruction.waypointsIndex) {
                        // we have definetly passed the instruction
                        it.remove();
                        pastTurnInstructions.add(instruction);
                        allTurnInstructions.add(instruction);
                    } else if (d < 10d && (!instruction.plannedForRemoving || d > instruction.lastD)) {
                        // we are approaching the instruction
                        LOG.d("routing debug instruction planned for removing = " + instruction.fullDescriptionString + " d = "
                                + loc.distanceTo(instruction.loc));
                        instruction.plannedForRemoving = true;
                    } else {
                        if (d >= 10d && (instruction.plannedForRemoving || d > instruction.lastD)) {
                            // remove the instruction
                            LOG.d("routing debug removing the instruction " + instruction.fullDescriptionString);
                            it.remove();
                            pastTurnInstructions.add(instruction);
                            allTurnInstructions.add(instruction);
                        }
                    }
                    instruction.lastD = loc.distanceTo(instruction.loc);

                }
            }

        }

        updateDistances(loc);

    }

    public void recalculateRoute(Location loc, boolean isBicycleTypeChanged) {
        if (recalculationInProgress) {
            return;
        }
        float distance = loc.distanceTo(lastRecalcLocation);
        if (distance < MIN_DISTANCE_FOR_RECALCULATION && !isBicycleTypeChanged) {
            return;
        }
        LOG.d("Recalculating route, distance: " + distance);
        lastRecalcLocation = loc;
        recalculationInProgress = true;
        if (listener != null) {
            listener.routeRecalculationStarted();
        }
        Location end = getEndLocation();
        if (loc == null || end == null)
            return;

        // Uncomment code below if previous part of the route needs to be
        // displayed.
        // NSMutableArray *viaPoints = [NSMutableArray array];
        // for (SMTurnInstruction *turn in self.pastTurnInstructions)
        // [viaPoints addObject:turn.loc];
        // [viaPoints addObject:loc];
        // [r getRouteFrom:((CLLocation *)[self.waypoints
        // objectAtIndex:0]).coordinate to:end.coordinate via:viaPoints];

        if (!isRouteBroken
                || (isRouteBroken && (((pastTurnInstructions != null && pastTurnInstructions.contains(station1)) || loc.distanceTo(endStation) < loc
                        .distanceTo(startStation))))) {
            routePhase = TO_DESTINATION;
            isRouteBroken = false;
            new SMHttpRequest().getRecalculatedRoute(loc, end, null, routeChecksum, null, destinationHint, this);
        } else if (isRouteBroken) {
            List<Location> viaList = new LinkedList<Location>();
            viaList.add(startStation);
            viaList.add(endStation);
            new SMHttpRequest().getRecalculatedRoute(loc, end, viaList, null, null, null, this);
        }
    }

    public boolean getRouteForNewBicycleType(Location loc) {
        if (recalculationInProgress) {
            return false;
        }
        lastRecalcLocation = loc;
        recalculationInProgress = true;
        Location end = getEndLocation();
        if (loc == null || end == null) {
            return false;
        }
        new SMHttpRequest().getRecalculatedRoute(loc, end, null, null, null, null, this);
        return true;
    }

    boolean checkLocation(Location loc, float maxDistance) {
        SMTurnInstruction currentTurn = turnInstructions.get(0);
        SMTurnInstruction nextTurn = turnInstructions.get(Math.min(turnInstructions.size() - 1, 2));
        if (nextTurn != null) {
            if (!isTooFarFromRouteSegment(loc, null, nextTurn, maxDistance)) {
                if (lastVisitedWaypointIndex > currentTurn.waypointsIndex) {
                    // removeTurn();
                    approachingTurn = true;
                }

                return false;
            }
        }
        return true;
    }

    public boolean isTooFarFromRoute(Location loc, int maxDistance) {
        if (turnInstructions.size() > 0) {
            SMTurnInstruction currentTurn = turnInstructions.get(0);
            lastCorrectedLocation = new Location(loc);

            if (pastTurnInstructions.size() < 0) {
                // lastCorrectedHeading = SMGPSUtil.bearingBetween(loc,
                // currentTurn.loc);
                // We have passed no turns. Check if we have managed to get on
                // the route somehow.
                if (currentTurn != null) {
                    double currentDistanceFromStart = loc.distanceTo(currentTurn.loc);
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

        for (int i = lastVisitedWaypointIndex; i < turnB.waypointsIndex; i++) {
            try {
                Location a = waypoints.get(i);
                Location b = waypoints.get(i + 1);
                double d = SMGPSUtil.distanceFromLineInMeters(loc, a, b);
                if (d < 0.0)
                    continue;
                if (d <= min) {
                    min = d;
                    lastVisitedWaypointIndex = i;
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

            Location a = waypoints.get(lastVisitedWaypointIndex);
            Location b = waypoints.get(lastVisitedWaypointIndex + 1);
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
        return isNear && turnInstructions.size() == 1;
    }

    // private void removeTurn() {
    // if (listener == null) {
    // return;
    // }
    //
    // if (turnInstructions.size() > 0) {
    // pastTurnInstructions.add(turnInstructions.get(0));
    // allTurnInstructions.add(turnInstructions.get(0));
    // turnInstructions.remove(0);
    // listener.updateTurn(true);
    // if (isRouteBroken && station1 != null && station2 != null) {
    // if (pastTurnInstructions.contains(station2)) {
    // isRouteBroken = false;
    // routePhase = TO_DESTINATION;
    // } else if (pastTurnInstructions.contains(station1))
    // routePhase = TO_END_STATION;
    // }
    // if (turnInstructions.size() == 0) {
    // reachedDestination = true;
    // listener.reachedDestination();
    // }
    // }
    // }

    private void updateDistances(Location loc) {
        if (tripDistance < 0.0) {
            tripDistance = 0.0f;
        }
        if (visitedLocations.size() > 0) {
            tripDistance += loc.distanceTo(visitedLocations.get(visitedLocations.size() - 1));
        }

        if (distanceLeft < 0.0) {
            distanceLeft = estimatedRouteDistance;
        } else if (turnInstructions.size() > 0) {
            // calculate distance from location to the next turn
            SMTurnInstruction nextTurn = turnInstructions.get(0);
            nextTurn.lengthInMeters = (int) calculateDistanceToNextTurn(loc);
            if (nextTurn.plannedForRemoving && nextTurn.lengthInMeters < 10) {
                nextTurn.lengthInMeters = 0;
            }
            // LOG.d("turn " + nextTurn.descriptionString + " distance = " +
            // nextTurn.lengthInMeters);
            nextTurn.lengthWithUnit = Util.formatDistance(nextTurn.lengthInMeters);
            // turnInstructions.set(0, nextTurn);
            distanceLeft = nextTurn.lengthInMeters;

            // calculate distance from next turn to the end of the route
            for (int i = 1; i < turnInstructions.size(); i++) {
                distanceLeft += turnInstructions.get(i).lengthInMeters;
            }
        }
        if (listener != null) {
            listener.updateRoute();
        }
    }

    private float calculateDistanceToNextTurn(Location loc) {
        if (turnInstructions.size() == 0)
            return 0.0f;

        SMTurnInstruction nextTurn = turnInstructions.get(0);

        // If first turn still hasn't been reached, return linear distance to
        // it.
        if (pastTurnInstructions.size() == 0)
            return loc.distanceTo(nextTurn.loc);

        int firstIndex = lastVisitedWaypointIndex >= 0 ? lastVisitedWaypointIndex + 1 : 0;
        float distance = 0.0f;
        if (firstIndex < waypoints.size()) {
            distance = loc.distanceTo(waypoints.get(firstIndex));
            if (nextTurn.waypointsIndex <= waypoints.size()) {
                for (int i = firstIndex; i < nextTurn.waypointsIndex; i++) {
                    double d = waypoints.get(i).distanceTo(waypoints.get(i + 1));
                    distance += d;
                }
            }
        }

        return distance;
    }

    public String getViaStreets() {
        return viaStreets;
    }

    // public void logWaypoints() {
    // Iterator<Location> it = waypoints.iterator();
    // while (it.hasNext()) {
    // Location loc = it.next();
    // LOG.d("waypoint = " + loc.getLatitude() + " , " + loc.getLongitude() + "\n");
    // }
    // LOG.d("///////////////////////////////////////////");
    // }

}
