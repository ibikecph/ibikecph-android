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
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

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

    public static final double GEOMETRY_SCALING_V4 = 1e6;
    public static final double GEOMETRY_SCALING_V5 = 1e5;

    public static final float MAX_DISTANCE_FROM_PATH = 30.0f;
    public static final float MIN_DISTANCE_FOR_RECALCULATION = MAX_DISTANCE_FROM_PATH;

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
    public int lastVisitedWaypointIndex;
    public float distanceFromRoute;
    protected String viaStreets;
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

        public enum DrawableSize {
            SMALL,
            LARGE
        }

        public int getDrawableId() {
            return getDrawableId(DrawableSize.LARGE);
        }

        /**
         * Returns a drawable representing the particular type of transportation
         * TODO: Consider implementing the two sizes available as drawables
         * @return
         */
        public int getDrawableId(DrawableSize size) {
            if (this == SMRoute.TransportationType.BIKE) {
                return R.drawable.route_bike; // TODO: Add a large version
            } else if (this == SMRoute.TransportationType.M) {
                return size == DrawableSize.LARGE ?
                       R.drawable.route_metro_direction :
                       R.drawable.route_metro;
            } else if (this == SMRoute.TransportationType.S) {
                return size == DrawableSize.LARGE ?
                       R.drawable.route_s_direction :
                       R.drawable.route_s;
            } else if (this == SMRoute.TransportationType.TOG) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_train_direction :
                        R.drawable.route_train;
            } else if (this == SMRoute.TransportationType.WALK) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_walking_direction :
                        R.drawable.route_walk;
            } else if (this == SMRoute.TransportationType.IC) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_train_direction :
                        R.drawable.route_train;
            } else if (this == SMRoute.TransportationType.LYN) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_train_direction :
                        R.drawable.route_train;
            } else if (this == SMRoute.TransportationType.REG) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_train_direction :
                        R.drawable.route_train;
            } else if (this == SMRoute.TransportationType.BUS) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_bus_direction :
                        R.drawable.route_bus;
            } else if (this == SMRoute.TransportationType.EXB) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_bus_direction :
                        R.drawable.route_bus;
            } else if (this == SMRoute.TransportationType.NB) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_bus_direction :
                        R.drawable.route_bus;
            } else if (this == SMRoute.TransportationType.TB) {
                return size == DrawableSize.LARGE ?
                        R.drawable.route_bus_direction :
                        R.drawable.route_bus;
            } else if (this == SMRoute.TransportationType.F) {
                return R.drawable.route_ship_direction; // TODO: Add a large version
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
        lastVisitedWaypointIndex = -1;
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
                    parseJsonNode(jsonRoot);
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

    public void parseJsonNode(JsonNode jsonRoot) {
        boolean ok = parseFromJson(jsonRoot);
        if (ok) {
            if (IBikeApplication.getService().hasValidLocation()) {
                updateDistances(IBikeApplication.getService().getLastValidLocation());
                emitRouteUpdated();
            }
        }
    }

    public boolean parseFromJson(JsonNode jsonRoot) {
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
                // This will be the name of the train, metro or bus line
                if(jsonRoot.get("route_summary").get("name") != null) {
                    description = jsonRoot.get("route_summary").get("name").asText();
                }
                if(jsonRoot.get("route_summary").get("departure_time") != null) {
                    departureTime = jsonRoot.get("route_summary").get("departure_time").asLong();
                }
                if(jsonRoot.get("route_summary").get("arrival_time") != null) {
                    arrivalTime = jsonRoot.get("route_summary").get("arrival_time").asLong();
                }
            }

            waypoints = decodePolyline(jsonRoot.path("route_geometry").textValue(), this.transportType);

            if (waypoints == null || waypoints.size() < 2) {
                return false;
            }

            upcomingTurnInstructions = new ArrayList<>();
            pastTurnInstructions = new LinkedList<>();
            visitedLocations = new ArrayList<>();
            if(departureTime > 0 && arrivalTime > 0) {
                Log.d("SMRoute", "Overriding duration from difference in arrival and departure");
                // Let's calculate the estimated duration from the difference in departure and
                // arrival time to account for the change of vehicle
                estimatedDuration = (int)(arrivalTime - departureTime);
            } else {
                estimatedDuration = jsonRoot.path("route_summary").path("total_time").asInt();
            }
            estimatedDurationLeft = estimatedDuration;

            estimatedDistance = jsonRoot.path("route_summary").path("total_distance").asInt();
            estimatedDistanceLeft = estimatedDistance;

            routeChecksum = null;
            destinationHint = null;

            // TODO: Consider if this is used any
            if (!jsonRoot.path("hint_data").path("checksum").isMissingNode()) {
                routeChecksum = jsonRoot.path("hint_data").path("checksum").asText();
            }

            /*
            // Disabled hinting when getting a OSRMv4 hint, as these should never be sent when
            // recalculating with OSRMv5.
            JsonNode hint_locations = jsonRoot.path("hint_data").path("locations");
            if (hint_locations != null && !hint_locations.isMissingNode() && hint_locations.size() > 0) {
                destinationHint = jsonRoot.path("hint_data").path("locations").get(hint_locations.size() - 1).asText();
            }
            */

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
                    if (isPublicTransportation() && instruction.drivingDirection == SMTurnInstruction.TurnDirection.GetOffPublicTransportation) {
                        instruction.waypointsIndex = waypoints.size() - 1;
                    }

                    // Save length to next turn with units so we don't have to generate it each
                    // time It's formatted just the way we like it
                    // TODO: Fix this ugly hack when the journey API return more consistent data
                    if (transportType.isPublicTransportation()) {
                        instruction.distance = instructionNode.get(2).asInt();
                    } else {
                        instruction.distance = previousLengthInMeters;
                        previousLengthInMeters = instructionNode.get(2).asInt();
                    }
                    // Derive the instructions location from its index in the waypoints
                    if (instruction.waypointsIndex >= 0 &&
                        instruction.waypointsIndex < waypoints.size()) {
                        // Set the location from the waypoints
                        instruction.location = waypoints.get(instruction.waypointsIndex);
                        // Log.d("SMRoute", instruction.name + " is at " + instruction.location);
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
     * Decoder for the Encoded Polyline Algorithm Format
     * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Encoded Polyline Algorithm Format</a>
     */
    public static List<Location> decodePolyline(String encodedString, TransportationType type) {
        // Log.d("SMRoute", "Decoding a polyline: " + encodedString);
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
            // BIKE and WALK is returned as a OSRVv4 polyline (which should be devided by 10^6)
            double scaleFactor = GEOMETRY_SCALING_V4;
            if(type != TransportationType.BIKE && type != TransportationType.WALK) {
                scaleFactor = GEOMETRY_SCALING_V5;
            }
            Location loc = Util.locationFromCoordinates((double) lat / scaleFactor, (double) lng / scaleFactor);
            locations.add(loc);

        }

        return locations;
    }

    /**
     * Returns the estimated amount of seconds to the destination.
     * @return
     */
    public float getEstimatedDuration() {
        return estimatedDuration;
    }

    /**
     * Returns the estimated amount of seconds to the destination.
     * @return
     */
    public float getEstimatedDurationLeft() {
        return estimatedDurationLeft;
    }


    public float getEstimatedDistance() {
        return estimatedDistance;
    }

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
                    double d = loc.distanceTo(instruction.location);
                    if (closestWaypointIndex < instruction.waypointsIndex) {
                        // future instruction, stop the loop
                        instruction.lastD = loc.distanceTo(instruction.location);
                        break;
                    } else if (closestWaypointIndex > instruction.waypointsIndex) {
                        // we have definitely passed the instruction
                        it.remove();
                        pastTurnInstructions.add(instruction);
                    } else if (d < instruction.getTransitionDistance() && (!instruction.plannedForRemoving || d > instruction.lastD)) {
                        // we are approaching the instruction
                        LOG.d("routing debug instruction planned for removing = " + instruction.fullDescriptionString + " d = "
                                + loc.distanceTo(instruction.location));
                        instruction.plannedForRemoving = true;
                    } else {
                        if (d >= instruction.getTransitionDistance() && (instruction.plannedForRemoving || d > instruction.lastD)) {
                            // remove the instruction
                            LOG.d("routing debug removing the instruction " + instruction.fullDescriptionString);
                            it.remove();
                            pastTurnInstructions.add(instruction);
                        }
                    }
                    instruction.lastD = loc.distanceTo(instruction.location);

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

    protected void updateDistances(Location loc) {
        if (estimatedDistanceLeft < 0.0) {
            estimatedDistanceLeft = estimatedDistance;
        }

        if (upcomingTurnInstructions.size() > 0) {
            // Calculate distance from location to the next turn
            SMTurnInstruction nextTurn = upcomingTurnInstructions.get(0);
            nextTurn.distance = calculateDistanceToNextTurn(loc);
            if (nextTurn.plannedForRemoving && nextTurn.distance < 10) {
                nextTurn.distance = 0;
            }

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

    @Override
    public void onLocationChanged(Location location) {
        this.visitLocation(location);
    }

    public RouteType getType() {
        return type;
    }

    public float getEstimatedDistanceLeft() {
        return estimatedDistanceLeft;
    }

}
