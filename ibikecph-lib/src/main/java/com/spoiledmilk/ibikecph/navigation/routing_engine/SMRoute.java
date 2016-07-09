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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO: This code comes from previous vendor. It's a mess. /jc
public class SMRoute implements SMHttpRequestListener, LocationListener {

    public static float DESTINATION_METRES_THRESHOLD = 40.0f;
    public static int DESTINATION_SECONDS_THRESHOLD = 3;

    public static final float MAX_DISTANCE_FROM_PATH = 30.0f;
    public static final float MIN_DISTANCE_FOR_RECALCULATION = MAX_DISTANCE_FROM_PATH;
    public static final int TO_START_STATION = 0;
    public static final int TO_END_STATION = 1;
    public static final int TO_DESTINATION = 2;

    public List<Location> waypoints;

    /**
     * Turn instructions on the route that the user has passed
     */
    protected List<SMTurnInstruction> pastTurnInstructions;

    /**
     * Turn instructions from the next upcoming instruction to the last
     */
    protected ArrayList<SMTurnInstruction> upcomingTurnInstructions;

    public List<Location> visitedLocations;
    float distanceLeft;
    float tripDistance;
    float averageSpeed;
    float caloriesBurned;
    public Location locationStart;
    public Location locationEnd;
    public boolean recalculationInProgress;
    int estimatedArrivalTime, arrivalTime;
    int estimatedRouteDistance = -1;
    String routeChecksum;
    String destinationHint;
    public Location lastCorrectedLocation;
    public double lastCorrectedHeading;
    public int lastVisitedWaypointIndex;
    public float distanceFromRoute;
    private String viaStreets;
    private Location lastRecalcLocation;
    public String startStationName, endStationName;
    public SMTurnInstruction station1, station2;
    public boolean reachedDestination = false;
    public int waypointStation1 = -1, waypointStation2 = -1;
    private RouteType type;

    public Address startAddress, endAddress;

    protected List<SMRouteListener> listeners = new CopyOnWriteArrayList<>();

    public enum TransportationType {
        BIKE, M, S, WALK, TOG, BUS, IC, LYN, REG, EXB, NB, TB, F;

        /**
         * Translates a transportation type into displayable string.
         * TODO: Implement proper translations of all types available
         * @return
         */
        public String toDisplayString() {
            int vehicleId = getVehicleId();
            if(vehicleId > 0) {
                String languageKey = "vehicle_" + vehicleId;
                return IBikeApplication.getString(languageKey);
            } else {
                return null;
            }
        }

        public int getVehicleId() {
            switch (this) {
                case BIKE:
                    return 1;
                case WALK:
                    return 2;
                case F:
                    return 3;
                case IC:
                case LYN:
                case M:
                case REG:
                case S:
                case TOG:
                    return 4;
            }
            return 0;
        }

        /**
         * Returns a drawable representing the particular type of transportation
         * TODO: Consider implementing the two sizes available as drawables
         * @return
         */
        public int getDrawableId() {
            if (this == SMRoute.TransportationType.BIKE) {
                return R.drawable.route_bike;
            } else if (this == SMRoute.TransportationType.M) {
                return R.drawable.route_metro_direction;
            } else if (this == SMRoute.TransportationType.S) {
                return R.drawable.route_s_direction;
            } else if (this == SMRoute.TransportationType.TOG) {
                return R.drawable.route_train_direction;
            } else if (this == SMRoute.TransportationType.WALK) {
                return R.drawable.route_walking_direction;
            } else if (this == SMRoute.TransportationType.IC) {
                return R.drawable.route_train_direction;
            } else if (this == SMRoute.TransportationType.LYN) {
                return R.drawable.route_train_direction;
            } else if (this == SMRoute.TransportationType.REG) {
                return R.drawable.route_train_direction;
            } else if (this == SMRoute.TransportationType.BUS) {
                return R.drawable.route_bus_direction;
            } else if (this == SMRoute.TransportationType.EXB) {
                return R.drawable.route_bus_direction;
            } else if (this == SMRoute.TransportationType.NB) {
                return R.drawable.route_bus_direction;
            } else if (this == SMRoute.TransportationType.TB) {
                return R.drawable.route_bus_direction;
            } else if (this == SMRoute.TransportationType.F) {
                return R.drawable.route_ship_direction;
            } else if (this == SMRoute.TransportationType.WALK) {
                return R.drawable.route_walking_direction;
            } else {
                return 0;
            }
        }

        public static boolean isPublicTransportation(TransportationType type) {
            return (type != null &&
                    type != TransportationType.BIKE &&
                    type != TransportationType.WALK);
        }

        public boolean isPublicTransportation() {
            return isPublicTransportation(this);
        }
    }

    // Variables for breakRoute
    public TransportationType transportType;
    public String description = null;
    public long departureTime;

    public SMRoute(Location start, Location end, JsonNode routeJSON, RouteType type) {
        init();
        locationStart = start;
        locationEnd = end;
        this.type = type;

        // TODO: Require a JSON coming from outside
        if (routeJSON == null) {
            throw new RuntimeException("The routeJSON must have been set before init is called.");
            // new SMHttpRequest().getRoute(start, end, null, this);
        } else {
            setupRoute(routeJSON);
        }
    }

    public void init() {
        distanceLeft = -1;
        tripDistance = -1;
        caloriesBurned = -1;
        averageSpeed = -1;
        lastVisitedWaypointIndex = -1;
        recalculationInProgress = false;
        lastRecalcLocation = Util.locationFromCoordinates(0, 0);
        reachedDestination = false;
        waypointStation1 = -1;
        waypointStation2 = -1;

        transportType = TransportationType.BIKE;
        description = null;
        departureTime = -1;
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

    public void addListener(SMRouteListener listener) {
        if(listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // SMRequestOSRMListener callback

    @Override
    public void onResponseReceived(int requestType, Object response) {
        switch (requestType) {
            case SMHttpRequest.REQUEST_GET_ROUTE:
                JsonNode jsonRoot = ((RouteInfo) response).jsonRoot;
                if (jsonRoot == null || jsonRoot.path("status").asInt(-1) != 0) {
                    emitRouteNotFound();
                } else {
                    setupRoute(jsonRoot);
                    emitStartRoute();
                }
                break;
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
    protected void emitRouteUpdated() {
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

    public Location getStartLocation() {
        if (waypoints != null && waypoints.size() > 0)
            return waypoints.get(0);
        return null;
    }

    public LatLng getRealStartLocation() {
        if(startAddress != null) {
            return startAddress.getLocation();
        } else {
            return new LatLng(getStartLocation());
        }
    }

    public Location getEndLocation() {
        if (waypoints != null && waypoints.size() > 0)
            return waypoints.get(waypoints.size() - 1);
        return null;
    }

    public LatLng getRealEndLocation() {
        if (locationEnd != null && endAddress != null) {
            return endAddress.getLocation();
        } else {
            return new LatLng(getEndLocation());
        }
    }

    public List<Location> getWaypoints() {
        return waypoints;
    }

    public ArrayList<SMTurnInstruction> getUpcomingTurnInstructions() {
        return upcomingTurnInstructions;
    }

    public List<SMTurnInstruction> getPastTurnInstructions() {
        return pastTurnInstructions;
    }

    private void setupRoute(JsonNode jsonRoot) {
        boolean ok = parseFromJson(jsonRoot);
        if (ok) {
            tripDistance = 0.0f;
            if (IBikeApplication.getService().hasValidLocation()) {
                updateDistances(IBikeApplication.getService().getLastValidLocation());
                emitRouteUpdated();
            }
        }
    }

    boolean parseFromJson(JsonNode jsonRoot) {
        Log.d("SMRoute", "parseFromJson() json = " + jsonRoot);
        synchronized (this) {
            if (jsonRoot == null) {
                return false;
            }

            if(jsonRoot.get("route_summary") != null) {
                if(jsonRoot.get("route_summary").get("type") != null) {
                    String transportType = jsonRoot.get("route_summary").path("type").textValue();
                    if (transportType != null) {
                        this.transportType = SMRoute.TransportationType.valueOf(transportType);
                    }
                }
                if(jsonRoot.get("route_summary").get("name") != null) {
                    description = jsonRoot.get("route_summary").get("name").asText();
                }
                if(jsonRoot.get("route_summary").get("departure_time") != null) {
                    departureTime = jsonRoot.get("route_summary").get("departure_time").asLong();
                }
            }

            waypoints = decodePolyline(jsonRoot.path("route_geometry").textValue(), jsonRoot.path("route_summary").path("type").textValue());

            if (waypoints == null || waypoints.size() < 2) {
                return false;
            }

            upcomingTurnInstructions = new ArrayList<>();
            pastTurnInstructions = new LinkedList<>();
            visitedLocations = new ArrayList<>();
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
                int previousLengthInMeters = 0;
                String previousLengthWithUnit = "";
                boolean isFirst = true;

                for (JsonNode instructionNode : routeInstructionsArr) {
                    SMTurnInstruction instruction = new SMTurnInstruction(instructionNode);

                    // FIXME: This is a hack to deal with an error in the journey API
                    // Instead of fixing it here -
                    // If the driving direction is to get off public transportation we'll override
                    // the way point index to the last wayout. Fix it on the server instead.
                    if(isPublicTransportation() && instruction.drivingDirection == SMTurnInstruction.TurnDirection.GetOffPublicTransportation) {
                        instruction.waypointsIndex = waypoints.size()-1;
                    }

                    // Save length to next turn with units so we don't have to generate it each
                    // time It's formatted just the way we like it
                    instruction.lengthInMeters = previousLengthInMeters;
                    instruction.lengthWithUnit = previousLengthWithUnit;
                    previousLengthInMeters = instructionNode.get(2).asInt();
                    previousLengthWithUnit = instructionNode.get(5).asText();
                    instruction.fixedLengthWithUnit = Util.formatDistance(previousLengthInMeters);
                    // Derive the instructions location from its index in the waypoints
                    if (instruction.waypointsIndex >= 0 &&
                        instruction.waypointsIndex < waypoints.size()) {
                        // Set the location from the waypoints
                        instruction.loc = waypoints.get(instruction.waypointsIndex);
                    }

                    // Generate a special description if this is the first instruction on the route.
                    if (isFirst) {
                        instruction.generateStartDescriptionString();
                        isFirst = false;
                    } else {
                        instruction.generateDescriptionString();
                    }

                    // If the vehicle was not given by the journey API, just choose the general
                    // transportation type of the route.
                    if(instruction.transportType == null) {
                        instruction.transportType = transportType;
                    }

                    upcomingTurnInstructions.add(instruction);
                }
            }

            // TODO: Consider if this whole block is even needed ...
            /*
            if (isPublicTransportation()) {
                double minimalStartDistance = Double.MAX_VALUE;
                double minimalEndDistance = Double.MAX_VALUE;

                // Find the waypoints closest to the start and end stations
                for (int i = 0; i < waypoints.size(); i++) {
                    if (locationStart.distanceTo(waypoints.get(i)) < minimalStartDistance) {
                        minimalStartDistance = locationStart.distanceTo(waypoints.get(i));
                        waypointStation1 = i;
                    }
                    if (locationEnd.distanceTo(waypoints.get(i)) < minimalEndDistance) {
                        minimalEndDistance = locationEnd.distanceTo(waypoints.get(i));
                        waypointStation2 = i;
                    }
                }

                Iterator<SMTurnInstruction> it2 = upcomingTurnInstructions.iterator();
                float distToStart = Float.MAX_VALUE, distToEnd = Float.MAX_VALUE;
                while (it2.hasNext()) {
                    SMTurnInstruction smt = it2.next();
                    if (smt.loc.distanceTo(locationStart) < distToStart && smt.waypointsIndex <= waypointStation1) {
                        distToStart = smt.loc.distanceTo(locationStart);
                        station1 = smt;
                    }
                    if (smt.loc.distanceTo(locationEnd) < distToEnd && smt.waypointsIndex <= waypointStation2) {
                        distToEnd = smt.loc.distanceTo(locationEnd);
                        station2 = smt;
                    }
                }

                int startIndex = upcomingTurnInstructions.indexOf(station1);
                int endIndex = upcomingTurnInstructions.indexOf(station2);
                while (startIndex < endIndex - 1) {
                    upcomingTurnInstructions.remove(startIndex + 1);
                    startIndex = upcomingTurnInstructions.indexOf(station1);
                    endIndex = upcomingTurnInstructions.indexOf(station2);
                }
            }
            */

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
                for (int i = 1; i < upcomingTurnInstructions.size() - 1; i++) {
                    SMTurnInstruction inst = upcomingTurnInstructions.get(i);
                    if (inst.lengthInMeters > longestStreet) {
                        longestStreet = inst.lengthInMeters;
                        viaStreets = upcomingTurnInstructions.get(i - 1).wayName;
                    }
                }
            }

            lastVisitedWaypointIndex = 0;

        }

        Log.d("DV_break", "SMRoute, returning true");
        return true;
    }

    /**
     * Decoder for the Encoded Polyline Algorithm Format
     * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Encoded Polyline Algorithm Format</a>
     */
    List<Location> decodePolyline(String encodedString, String type) {
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
            Location loc;
            if (type != null && !type.equals("BIKE")) {
                loc = Util.locationFromCoordinates((double) lat / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LATITUDE_RP, (double) lng / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LONGITUDE_RP);
            } else {
                loc = Util.locationFromCoordinates((double) lat / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LATITUDE, (double) lng / com.spoiledmilk.ibikecph.util.Config.GEOMETRY_DIGITS_LONGITUDE);
            }
            locations.add(loc);

        }

        return locations;
    }

    /**
     * Returns the estimated amount of seconds to the destination.
     * @return
     */
    public float getEstimatedArrivalTime() {
        return arrivalTime;
    }


    public int getEstimatedDistance() {
        return estimatedRouteDistance;
    }

    double distancePassed = 0;
    Location lastLocation;

    /**
     * Updates the route when a user has visited a particular location.
     * TODO: Clean this up - a lot ...
     * @param loc
     */
    public void visitLocation(Location loc) {
        // Accumulating the distance passed
        // TODO: Consider doing this when the user actually passes a way point and use its location.
        if (lastLocation != null && loc != null) {
            distancePassed += loc.distanceTo(lastLocation);
        }

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

        // Calculate the distance to the end location.
        // TODO: Calculate this in a non-euclidean way
        double distanceToFinish = loc.distanceTo(getEndLocation());

        arrivalTime = Math.round(estimatedArrivalTime * distanceLeft / estimatedRouteDistance);

        // Calculate the average speed and update the ETA
        // A bike travels approximately 5 meters per second
        double speed = loc.getSpeed() > 0 ? loc.getSpeed() : 5;

        int timeToFinish = 100;
        if (speed > 0) {
            timeToFinish = (int) (distanceToFinish / speed);
        }

        /*
        double destinationRadiusPublic = 300;
        double leaveLastPublicInfoRadius = 300; // Display two informations in fragment until we are further away than this
        double leavingLastPublicRadius = 300; // Display "get on transport xx on xx" until we are this distance away, then change to "get off on xx"
        */

        // TODO: Reimplement a proper behaviour when leaving or changing public transportation
        /*
        if (type == RouteType.BREAK) {
            try {
                if (NavigationMapHandler.routePos == Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).size() - 1) {
                    if (!isPublicTransportation(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType)) {
                        //destRadius += "Next stop is final destination and not public, setting distanceToFinish to = ";
                        destinationRadius = 40.0;
                    } else {
                        //destRadius += "Next stop is final destination and public, setting distanceToFinish to = ";
                        destinationRadius = destinationRadiusPublic;
                    }
                } else if (isPublicTransportation(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos + 1).transportType)) {
                    //destRadius += "Next stop is public, setting distanceToFinish to = ";
                    destinationRadius = destinationRadiusPublic;
                }

                //Location of the last public when next step is leaving the public station
                if (NavigationMapHandler.routePos > 0) {
                    int pos = NavigationMapHandler.routePos - 1;
                    //Log.d("DV", "checking with pos = " + pos);
                    //If last was a public transport type
                    if (isPublicTransportation(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(pos).transportType)) {
                        Log.d("DV", "previous transport type was = " + Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(pos).transportType);
                        Location location = Util.locationFromCoordinates(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLatitude(),
                                Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLongitude());
                        double distance = location.distanceTo(lastLocation);
                        if (distance <= leaveLastPublicInfoRadius) {
                            NavigationMapHandler.displayExtraField = true;
                            NavigationMapHandler.isPublicTransportation = false;
                            NavigationMapHandler.displayGetOffAt = false;
                            //Log.d("DV", "distance to lastLocation = " + distance);
                        } else {
                            NavigationMapHandler.displayExtraField = false;
                        }

                        //If current is a public transport type
                        if (isPublicTransportation(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType)) {
                            Log.d("DV", "transport type with current routepos is = " + Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType);
                            location = Util.locationFromCoordinates(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLatitude(),
                                    Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLongitude());
                            distance = location.distanceTo(lastLocation);
                            if (distance >= leavingLastPublicRadius) {
                                //Log.d("DV", "2distance to lastLocation = " + distance);
                                //Log.d("DV", "Setting displayGetOffAt");
                                NavigationMapHandler.isPublicTransportation = false;
                                NavigationMapHandler.displayGetOffAt = true;
                            } else {
                                NavigationMapHandler.isPublicTransportation = true;
                            }

                        }
                        //Location of the last public when next step is leaving the public station with a public transport type
                    } else if (isPublicTransportation(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType)) {
                        Log.d("DV", "transport type with current routepos is = " + Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType);
                        Location location = Util.locationFromCoordinates(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLatitude(),
                                Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.get(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos - 1).waypoints.size() - 1).getLongitude());
                        double distance = location.distanceTo(lastLocation);
                        if (distance >= leavingLastPublicRadius) {
                            //Log.d("DV", "2distance to lastLocation = " + distance);
                            //Log.d("DV", "Setting displayGetOffAt");
                            NavigationMapHandler.isPublicTransportation = false;
                            NavigationMapHandler.displayGetOffAt = true;
                        } else {
                        }

                    }
                }
            } catch (Exception ex) {
                Log.d("DV", "Next stop exception = " + ex.getMessage());
            }
        }
        */

        // TODO: Consider if this check is need - maybe we only want to reach the destination once.
        if (!reachedDestination) {
            // Are we close to the finish?
            if (distanceToFinish < DESTINATION_METRES_THRESHOLD ||
                timeToFinish <= DESTINATION_SECONDS_THRESHOLD) {
                // Move all future turn instructions to the past instructions
                pastTurnInstructions.addAll(upcomingTurnInstructions);
                upcomingTurnInstructions.clear();
                // Remove this route as a listner of location updates.
                IBikeApplication.getService().removeLocationListener(this);
                // Declare that we've reached the destination
                // TODO: Make this a derived method of the fact that we have no more turn instructions
                reachedDestination = true;
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
            if (!approachingFinish() && listeners.size() > 0 && isTooFarFromRoute(loc, maximalDistance)) {
                recalculateRoute(loc);
                return;
            }
        }

        int closestWaypointIndex = -1;
        double minD = Double.MAX_VALUE;
        if (upcomingTurnInstructions != null && upcomingTurnInstructions.size() > 0) {
            if (waypoints != null && waypoints.size() > 0) {
                // find the closest waypoint
                for (int i = lastVisitedWaypointIndex; i < waypoints.size(); i++) {
                    Location waypoint = waypoints.get(i);
                    if (loc.distanceTo(waypoint) < minD) {
                        minD = loc.distanceTo(waypoint);
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
                    double d = loc.distanceTo(instruction.loc);
                    if (closestWaypointIndex < instruction.waypointsIndex) {
                        // future instruction, stop the loop
                        instruction.lastD = loc.distanceTo(instruction.loc);
                        break;
                    } else if (closestWaypointIndex > instruction.waypointsIndex) {
                        // we have definitely passed the instruction
                        it.remove();
                        pastTurnInstructions.add(instruction);
                    } else if (d < instruction.getTransitionDistance() && (!instruction.plannedForRemoving || d > instruction.lastD)) {
                        // we are approaching the instruction
                        LOG.d("routing debug instruction planned for removing = " + instruction.fullDescriptionString + " d = "
                                + loc.distanceTo(instruction.loc));
                        instruction.plannedForRemoving = true;
                    } else {
                        if (d >= instruction.getTransitionDistance() && (instruction.plannedForRemoving || d > instruction.lastD)) {
                            // remove the instruction
                            LOG.d("routing debug removing the instruction " + instruction.fullDescriptionString);
                            it.remove();
                            pastTurnInstructions.add(instruction);
                        }
                    }
                    instruction.lastD = loc.distanceTo(instruction.loc);

                }
            }

        }

        updateDistances(loc);
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
        return isNear && upcomingTurnInstructions.size() == 1;
    }

    private void updateDistances(Location loc) {
        if (tripDistance < 0.0) {
            tripDistance = 0.0f;
        }
        if (visitedLocations.size() > 0) {
            tripDistance += loc.distanceTo(visitedLocations.get(visitedLocations.size() - 1));
        }

        if (distanceLeft < 0.0) {
            distanceLeft = estimatedRouteDistance;
        } else if (upcomingTurnInstructions.size() > 0) {
            // Calculate distance from location to the next turn
            SMTurnInstruction nextTurn = upcomingTurnInstructions.get(0);
            nextTurn.lengthInMeters = (int) calculateDistanceToNextTurn(loc);
            if (nextTurn.plannedForRemoving && nextTurn.lengthInMeters < 10) {
                nextTurn.lengthInMeters = 0;
            }
            nextTurn.lengthWithUnit = Util.formatDistance(nextTurn.lengthInMeters);

            distanceLeft = nextTurn.lengthInMeters;
            // Calculate distance from next turn to the end of the route
            for (int i = 1; i < upcomingTurnInstructions.size(); i++) {
                distanceLeft += upcomingTurnInstructions.get(i).lengthInMeters;
            }
        }
    }

    private float calculateDistanceToNextTurn(Location loc) {
        if (upcomingTurnInstructions.size() == 0)
            return 0.0f;

        SMTurnInstruction nextTurn = upcomingTurnInstructions.get(0);

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
    // LOG.d("waypoint = " + loc.getLatitude() + " , " + loc.getLongitude() + "");
    // }
    // LOG.d("///////////////////////////////////////////");
    // }


    public float getTripDistance() {
        return tripDistance;
    }

    public int getEstimatedRouteDistance() {
        return estimatedRouteDistance;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.visitLocation(location);
    }

    public RouteType getType() {
        return type;
    }

    public float getDistanceLeft() {
        return distanceLeft;
    }

    public JsonNode getObject() {

        JsonNode actualObj = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            actualObj = mapper.readTree(loadJSONFromAsset());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return actualObj;

    }

    public String loadJSONFromAsset() {
        String json = null;
        try {

            InputStream is = IBikeApplication.getContext().getAssets().open("Dummy_JSON.js");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
}
