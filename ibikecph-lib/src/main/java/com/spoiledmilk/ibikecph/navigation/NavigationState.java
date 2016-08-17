package com.spoiledmilk.ibikecph.navigation;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.states.NavigatingState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteResponse;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.RegularRouteRequester;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMGPSUtil;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The state of a users navigation along a Route.
 * Created by kraen on 16-08-16.
 */
public class NavigationState implements LocationListener {

    protected static final float DESTINATION_REACHED_THRESHOLD = 10.0f;
    protected static final float DISTANCE_TO_ROUTE_THRESHOLD = 20.0f;

    /**
     * The state of the map used to visualize the navigation.
     */
    protected final NavigatingState mapState;

    /**
     * The currently active route.
     */
    protected Route route;

    /**
     * The leg within the currently active route, that is currently active.
     */
    protected Leg leg;

    /**
     * An index in to the current leg's list of steps.
     */
    protected int stepIndex;

    /**
     * Are we in the progress of recalculating the route?
     */
    protected boolean isRecalculating;

    /**
     * Constructs a new NavigationState, takes a Navigating(Map)State as argument.
     * @param mapState the state of the map, used to visualize the navigation.
     */
    public NavigationState(NavigatingState mapState) {
        this.mapState = mapState;
    }

    public List<TurnInstruction> getUpcomingSteps() {
        List<TurnInstruction> result = new LinkedList<>();
        if(route != null && leg != null) {
            // Add the current legs upcoming steps.
            List<TurnInstruction> stepsInLeg = leg.getSteps();
            if(stepIndex >= 0 && stepIndex <= stepsInLeg.size()) {
                List<TurnInstruction> upcomingStepsInLeg = stepsInLeg.subList(stepIndex, stepsInLeg.size());
                result.addAll(upcomingStepsInLeg);
            }
            // Add all steps of subsequent legs.
            int activeLegIndex = route.getLegs().indexOf(leg);
            if(activeLegIndex >= 0 && activeLegIndex+1 <= route.getLegs().size()-1) {
                List<Leg> upcomingLegs = route.getLegs().subList(activeLegIndex+1, route.getLegs().size()-1);
                for(Leg leg: upcomingLegs) {
                    result.addAll(leg.getSteps());
                }
            }
        }
        return result;
    }

    public boolean hasStarted() {
        // We have to have at least one leg in the route and an active leg.
        if(route.getLegs().size() > 0 && leg != null) {
            // If started, the stepIndex has increased or we are not on the first leg.
            boolean onFirstLeg = route.getLegs().get(0).equals(leg);
            return !onFirstLeg || stepIndex > 0;
        } else {
            return false;
        }
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        // Set the current route
        if(this.route != null) {
            BikeLocationService.getInstance().removeLocationListener(this);
        }
        this.route = route;
        if(this.route != null) {
            this.route.setState(this);
            restartNavigation();
            BikeLocationService.getInstance().addLocationListener(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        boolean isDestinationReached = isDestinationReached(location);
        if(!isDestinationReached && !isRecalculating) {
            boolean inPublicTransportation = leg.getTransportType().isPublicTransportation();
            boolean hasLeftRoute = hasLeftRoute(location);
            if(!inPublicTransportation && hasLeftRoute) {
                recalculate(location);
            } else {
                updateStepIndex(location);
            }
        } else if(isDestinationReached) {
            Log.d("NavigationState", "Destination reached!");
            BikeLocationService.getInstance().removeLocationListener(this);
            emitDestinationReached();
        }
    }

    /**
     * Updates the stepIndex to reflect what navigation the user should do next
     * @param location the current location of the user
     */
    protected void updateStepIndex(Location location) {
        Location nearestPoint = leg.getNearestPoint(location);
        int nearestPointIndex = leg.getPoints().indexOf(nearestPoint);

        // Update the stepIndex to reflect what navigation the user should do next
        for(int s = stepIndex; s < leg.getSteps().size(); s++) {
            TurnInstruction step = leg.getSteps().get(s);
            float distance = location.distanceTo(step.getLocation());
            if(step.getPointsIndex() <= nearestPointIndex && distance > step.getTransitionDistance()) {
                // We have passed the step, let's increase the stepIndex
                setStepIndex(s+1);
            }
        }
    }

    protected void setStepIndex(int newStepIndex) {
        if(newStepIndex > leg.getSteps().size()-1) {
            // The step index has exeded the steps in the current route leg
            // We should change to the next leg
            int legIndex = route.getLegs().indexOf(leg);
            if(legIndex < route.getLegs().size()-1) {
                // Change leg
                leg = route.getLegs().get(legIndex+1);
                stepIndex = 0;
            } else {
                // This was the final leg of the route
                emitDestinationReached();
            }
        } else {
            stepIndex = newStepIndex;
        }
    }

    protected void recalculate(Location location) {
        Log.d("NavigationState", "Recalculating route");
        isRecalculating = true;
        emitRouteRecalculationStarted();

        Geocoder.RouteCallback callback = new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(Route route) {
                if(NavigationState.this.route.getType().equals(RouteType.BREAK)) {
                    // Replace the current leg with the only leg from the server
                    if(route.getLegs().size() == 1) {
                        Leg recalculatedLeg = route.getLegs().get(0);
                        NavigationState.this.route.replaceLeg(leg, recalculatedLeg);
                        restartNavigation(recalculatedLeg);
                    } else {
                        throw new RuntimeException("Expected a single leg in the new route.");
                    }
                } else {
                    restartNavigation();
                }
                isRecalculating = false;
                emitRouteRecalculationCompleted();
            }

            @Override
            public void onSuccess(BreakRouteResponse breakRouteResponse) {
                throw new RuntimeException("Got an unexpected break route response.");
            }

            @Override
            public void onFailure() {
                isRecalculating = false;
                emitServerError();
                throw new RuntimeException("Got an unexpected failure.");
            }
        };

        RegularRouteRequester requester;
        if(route.getType().equals(RouteType.BREAK)) {
            // Recalculate the current leg only
            LatLng start = new LatLng(location);
            LatLng end = new LatLng(leg.getEndLocation());
            requester = new RegularRouteRequester(start, end, callback, RouteType.FASTEST);
            if(location.hasBearing()) {
                requester.setBearing(location.getBearing());
            }
            requester.execute();
        } else {
            LatLng start = new LatLng(location);
            LatLng end = new LatLng(route.getRealEndLocation());
            requester = new RegularRouteRequester(start, end, callback, route.getType());
            requester.setRoute(route);
            requester.execute();
        }
    }

    /**
     * Restarts the navigation, from a particular leg in the route
     * @param leg the leg to start from
     */
    protected void restartNavigation(Leg leg) {
        // Reset the navigation within the current leg.
        stepIndex = 0;
        this.leg = leg;
    }

    /**
     * Restarts the navigation, from the first leg in the route
     */
    protected void restartNavigation() {
        if(getRoute().getLegs().size() > 0) {
            restartNavigation(getRoute().getLegs().get(0));
        } else {
            throw new RuntimeException("Cannot restart navigation on a route without legs");
        }
    }

    /**
     * Determines if the navigating user has left the route.
     * @param location the current location of the user.
     * @return true if the user has left the route, false otherwise.
     */
    protected boolean hasLeftRoute(Location location) {
        float maximalDistanceAllowed = DISTANCE_TO_ROUTE_THRESHOLD + location.getAccuracy() / 3;
        return distanceToRoute(location) > maximalDistanceAllowed;
    }

    /**
     * Determines the users distance to the route.
     * @param location the current location of the user.
     * @return the distance in metres to the route.
     */
    protected float distanceToRoute(Location location) {
        Location pointA = null;
        float minimalDistance = Float.MAX_VALUE;
        for(Leg leg: route.getLegs()) {
            for(Location pointB: leg.getPoints()) {
                if(pointA != null) {
                    float distance = distanceToSegment(location, pointA, pointB);
                    if(distance < minimalDistance) {
                        minimalDistance = distance;
                    }
                }
                pointA = pointB;
            }
        }
        return minimalDistance;
    }

    /**
     * Calculate the distance of the user to a line segment
     * @param location the users location
     * @param pointA first endpoint of the segment
     * @param pointB second endpoint of the segment
     * @return the distance to the line segment in metres
     */
    protected float distanceToSegment(Location location, Location pointA, Location pointB) {
        return (float) SMGPSUtil.distanceFromLineInMeters(location, pointA, pointB);
    }

    /**
     * A list of NavigationStateListener waiting for notifications from the users navigation.
     */
    protected List<NavigationStateListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Removes all listeners
     */
    public void removeListeners() {
        this.listeners.clear();
    }

    /**
     * Removes a particular listener
     * @param listener the listener to be removed
     */
    public void removeListener(NavigationStateListener listener) {
        if(listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**
     * Adds a listener
     * @param listener the listener to add
     */
    public void addListener(NavigationStateListener listener) {
        if(listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Emits that the route recalculation is done.
     */
    protected void emitRouteRecalculationCompleted() {
        for(NavigationStateListener listener: listeners) {
            listener.routeRecalculationCompleted();
        }
    }

    /**
     * Emits that a server error occurred when fetching the route.
     */
    protected void emitServerError() {
        for(NavigationStateListener listener: listeners) {
            listener.serverError();
        }
    }

    /**
     * Emits that the route is ready to be started
     */
    protected void emitStartRoute() {
        for(NavigationStateListener listener: listeners) {
            listener.navigationStarted();
        }
    }

    /**
     * Emits that no route was found
     */
    protected void emitRouteNotFound() {
        for(NavigationStateListener listener: listeners) {
            listener.routeNotFound();
        }
    }

    /**
     * Emits that the routes destination was reached
     */
    protected void emitDestinationReached() {
        for(NavigationStateListener listener: listeners) {
            listener.destinationReached();
        }
    }

    /**
     * Emits that the user has diverged too much from the route, and a recalculation has started
     */
    protected void emitRouteRecalculationStarted() {
        for(NavigationStateListener listener: listeners) {
            listener.routeRecalculationStarted();
        }
    }

    public boolean isDestinationReached(Location location) {
        // Are we navigating on the last leg of the route?
        int legIndex = route.getLegs().indexOf(leg);
        boolean lastLeg = legIndex == route.getLegs().size()-1;
        // Are we navigating towards the last step of the route?
        boolean lastStep = stepIndex == leg.getSteps().size()-1;
        // Are we close enough to the location of the last step of the route?
        TurnInstruction step = leg.getSteps().get(stepIndex);
        boolean closeEnough = location.distanceTo(step.getLocation()) <= DESTINATION_REACHED_THRESHOLD;
        // All at once?
        return lastLeg && lastStep && closeEnough;
    }

    public TurnInstruction getNextStep() {
        if(route != null && leg != null && stepIndex >= 0 && stepIndex < leg.getSteps().size()) {
            return leg.getSteps().get(stepIndex);
        } else {
            return null;
        }
    }

    public Leg getLeg() {
        return leg;
    }

    public float getDistanceToStep(TurnInstruction step) {
        Location location = BikeLocationService.getInstance().getLastValidLocation();
        return leg.getDistanceToStep(location, step);
    }

    /**
     * Get the estimated distance left of the entire journey.
     * @return distance in metres
     */
    public float getEstimatedDistanceLeft(boolean nonPublicOnly) {
        float distanceLeft = 0;
        // Sum over the current and all future legs
        for(int l = route.getLegs().indexOf(leg); l < route.getLegs().size(); l++) {
            Leg leg = route.getLegs().get(l);
            if(!nonPublicOnly || !leg.getTransportType().isPublicTransportation()) {
                if(this.leg.equals(leg)) {
                    distanceLeft += leg.getEstimatedDistanceLeft(stepIndex);
                } else {
                    distanceLeft += leg.getDistance();
                }
            }
        }
        return distanceLeft;
    }

    public double getEstimatedDurationLeft() {
        double result = 0;
        for(int l = route.getLegs().indexOf(leg); l < route.getLegs().size(); l++) {
            result += getEstimatedDurationLeft(leg);
        }
        return result;
    }

    public double getEstimatedDurationLeft(Leg leg) {
        if(leg.getTransportType().isPublicTransportation()) {
            if(this.leg.equals(leg)) {
                Date now = new Date();
                return Math.max(0, leg.getArrivalTime() - now.getTime());
            } else {
                return leg.getArrivalTime() - leg.getDepartureTime();
            }
        } else {
            double distance;
            // What is the distance remaining on the leg?
            if(this.leg.equals(leg)) {
                distance = leg.getEstimatedDistanceLeft(stepIndex);
            } else {
                distance = leg.getDistance();
            }
            // Adjusting for the average speed on this type of route.
            if(route.getType().equals(RouteType.CARGO)) {
                return distance / Route.AVERAGE_CARGO_BIKING_SPEED;
            } else {
                return distance / Route.AVERAGE_BIKING_SPEED;
            }
        }
    }

    public Date getArrivalTime() {
        return getArrivalTime(route, this);
    }

    public static Date getArrivalTime(Route route) {
        return getArrivalTime(route, null);
    }

    public static Date getArrivalTime(Route route, NavigationState state) {
        // Find the last non-public legs - only these can have their arrival time
        // improved by the user biking faster
        Date earliestDeparture = new Date(); // Let's assume now

        // Calculate the index to which we should calculate down to.
        int earliestLegIndex;
        if(state.getLeg() != null) {
            earliestLegIndex = route.getLegs().indexOf(state.getLeg());
        } else {
            earliestLegIndex = 0;
        }

        // Looping backwards in routes, summing up the estimated duration.
        int nonPublicDurationLeft = 0;
        for(int r = route.getLegs().size()-1; r >= earliestLegIndex; r--) {
            Leg leg = route.getLegs().get(r);
            long arrivalTime = leg.getArrivalTime();
            if(leg.getTransportType().isPublicTransportation() && arrivalTime > 0) {
                // The last public transportation
                earliestDeparture = new Date(arrivalTime * 1000);
                break;
            } else {
                if(state != null) {
                    nonPublicDurationLeft += state.getEstimatedDurationLeft(leg);
                } else {
                    nonPublicDurationLeft += route.getDuration(leg);
                }
            }
        }

        Calendar c = Calendar.getInstance();
        c.setTime(earliestDeparture);
        c.add(Calendar.SECOND, nonPublicDurationLeft);
        return c.getTime();
    }

    public static double getEstimatedDistance(boolean nonPublicOnly, Route route) {
        float estimatedDistance = 0;
        // Calculate the accumulated estimated distance.
        for(Leg leg: route.getLegs()) {
            if(!nonPublicOnly || !leg.getTransportType().isPublicTransportation()) {
                estimatedDistance += leg.getDistance();
            }
        }
        return estimatedDistance;
    }
}
