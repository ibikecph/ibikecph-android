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
import com.spoiledmilk.ibikecph.navigation.routing_engine.RouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMGPSUtil;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The state of a users navigation along a Route.
 * Created by kraen on 16-08-16.
 */
public class NavigationState implements LocationListener, RouteListener {

    protected static final float DESTINATION_REACHED_THRESHOLD = 10.0f;

    protected static final float DISTANCE_TO_ROUTE_THRESHOLD = 20.0f;

    /**
     * The assumed biking speed in metres per second.
     */
    public static final float AVERAGE_BIKING_SPEED = 15f * 1000f / 3600f;

    /**
     * The assumed cargo biking speed in metres per second.
     */
    public static final float AVERAGE_CARGO_BIKING_SPEED = 10f * 1000f / 3600f;

    /**
     * How often may the route be recalculated at maximum.
     * Unit is milliseconds.
     */
    private static final int RECALCULATE_THROTTLE = 10000; // Once every 10 secs

    /**
     * The currently active route.
     */
    protected Route route;

    /**
     * Are we in the progress of recalculating the route?
     */
    protected boolean isRecalculating;

    /**
     * The last time the route was recalculated.
     */
    protected long lastRecalculateTime;

    /**
     * A linked list of all upcoming steps across legs
     */
    protected List<TurnInstruction> upcomingSteps = new LinkedList<>();

    /**
     * A linked list of all steps that has been visited.
     * A step may be considered both visited and upcoming at the same time, when approaching it.
     */
    protected Set<TurnInstruction> visitedSteps = new HashSet<>();

    /**
     * All points along the route, across all it's legs
     */
    protected List<Location> points = new LinkedList<>();

    /**
     * A map which translates a step to the index of the closest point in the list of points
     */
    protected Map<TurnInstruction, Integer> stepToPointIndex = new HashMap<>();

    /**
     * A map which translates a step to the previous distance
     */
    protected Map<TurnInstruction, Double> stepToDistance = new HashMap<>();

    protected Location lastKnownLocation;

    /**
     * Constructs a new NavigationState
     */
    public NavigationState() {

    }

    public List<TurnInstruction> getUpcomingSteps() {
        return upcomingSteps;
    }

    /**
     * Checks if the navigation has started by checking if the next upcoming step is the first.
     *
     * @return true if the navigation has started.
     */
    public boolean hasStarted() {
        return visitedSteps.contains(getFirstStep());
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
        // Update the last known location
        lastKnownLocation = location;
        boolean isDestinationReached = isDestinationReached();
        if(!isDestinationReached && !isRecalculating) {
            TurnInstruction nextStep = getNextStep();
            boolean inPublicTransportation = nextStep.getTransportType().isPublicTransportation();
            boolean hasLeftRoute = hasLeftRoute(location);
            long now = new Date().getTime();
            boolean tooSoon = (now - lastRecalculateTime) <= RECALCULATE_THROTTLE;
            if(!inPublicTransportation && hasLeftRoute && !tooSoon) {
                recalculate(location);
            } else {
                updateUpcomingSteps(location);
            }
        } else if(isDestinationReached) {
            Log.d("NavigationState", "Destination reached!");
            BikeLocationService.getInstance().removeLocationListener(this);
            emitDestinationReached();
        }
    }

    /**
     * Updates the upcoming steps to reflect what navigation the user should do next
     * @param location the current location of the user
     */
    protected void updateUpcomingSteps(Location location) {
        synchronized (this) {
            int nearestPointIndex = getNearestPointIndex(location);
            Iterator<TurnInstruction> upcomingStepsIterator = upcomingSteps.iterator();
            while(upcomingStepsIterator.hasNext()) {
                TurnInstruction step = upcomingStepsIterator.next();
                Double previousDistance = stepToDistance.get(step);
                // At what index within the points list is this step nearest?
                int stepsPointIndex = stepToPointIndex.get(step);

                double transitionDistance = step.getTransitionDistance();
                double euclideanDistance = step.getLocation().distanceTo(location);

                // Save this new distance in the step to distance map
                stepToDistance.put(step, transitionDistance);

                // Are we moving away from the step's location?
                boolean isMovingAway = previousDistance != null &&
                                       previousDistance < euclideanDistance;

                if(stepsPointIndex < nearestPointIndex) {
                    // We have moved beyond this instruction already
                    if(upcomingStepsIterator.hasNext()) {
                        // Let's never remove the last upcoming step
                        upcomingStepsIterator.remove();
                    }
                    visitedSteps.add(step);
                } else if(euclideanDistance < transitionDistance) {
                    // We are close enough to consider the step visited
                    visitedSteps.add(step);
                } else if(visitedSteps.contains(step) && isMovingAway) {
                    // We have visited the step and are now moving away from it
                    if(upcomingStepsIterator.hasNext()) {
                        // Let's never remove the last upcoming step
                        upcomingStepsIterator.remove();
                    }
                }
            }
        }
    }

    private int getNearestPointIndex(Location location) {
        return getNearestPointIndex(location, 0);
    }

    private int getNearestPointIndex(Location location, int fromIndex) {
        float minimalDistance = Float.MAX_VALUE;
        int nearestPointIndex = 0;
        for(int p = fromIndex; p < points.size(); p++) {
            Location point = points.get(p);
            float distance = location.distanceTo(point);
            if(distance < minimalDistance) {
                minimalDistance = distance;
                nearestPointIndex = p;
            }
        }
        return nearestPointIndex;
    }

    protected void recalculate(final Location location) {
        Log.d("NavigationState", "Recalculating route");
        isRecalculating = true;
        lastRecalculateTime = new Date().getTime();
        final Leg currentLeg = getCurrentLeg();

        emitRouteRecalculationStarted();

        Geocoder.RouteCallback callback = new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(Route route) {
                if (NavigationState.this.route.getType().equals(RouteType.BREAK)) {
                    // Replace the current leg with the only leg from the server
                    if (route.getLegs().size() == 1) {
                        Leg recalculatedLeg = route.getLegs().get(0);
                        NavigationState.this.route.replaceLeg(currentLeg, recalculatedLeg);
                        restartNavigation();
                        updateUpcomingSteps(location);
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
        if (route.getType().equals(RouteType.BREAK)) {
            // Recalculate the current leg only
            LatLng start = new LatLng(location);
            LatLng end = new LatLng(currentLeg.getEndLocation());
            requester = new RegularRouteRequester(start, end, callback, RouteType.FASTEST);
            if (location.hasBearing()) {
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
     * Restarts the navigation, from the first leg in the route
     */
    protected void restartNavigation() {
        lastRecalculateTime = new Date().getTime();
        upcomingSteps.clear();
        upcomingSteps.addAll(route.getSteps());
        points.clear();
        points.addAll(route.getPoints());
        updateStepPointIndices();
    }

    /**
     * Determines if the navigating user has left the route.
     * @param location the current location of the user.
     * @return true if the user has left the route, false otherwise.
     */
    protected boolean hasLeftRoute(Location location) {
        float maximalDistanceAllowed = DISTANCE_TO_ROUTE_THRESHOLD + location.getAccuracy() / 3;
        double distanceToRoute = distanceToRoute(location);
        Log.d("NavigationState", "distanceToRoute = " + distanceToRoute);
        return distanceToRoute > maximalDistanceAllowed;
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

    public boolean isDestinationReached() {
        if(upcomingSteps.isEmpty()) {
            // It is actually very unlikely that queue of steps will get completely empty
            return true;
        } else {
            TurnInstruction finalStep = upcomingSteps.get(upcomingSteps.size()-1);
            return visitedSteps.contains(finalStep);
        }
    }

    public TurnInstruction getNextStep() {
        if(upcomingSteps.size() == 0) {
            return null;
        } else {
            return upcomingSteps.get(0);
        }
    }

    /**
     * Calculates the distance from the users location to a particular step.
     * TODO: Implement a non-euclidean calculation, that follows the geometry / points of the route.
     * @param step the particular step
     * @return
     */
    public static float getDistanceToStep(TurnInstruction step) {
        Location location = BikeLocationService.getInstance().getLastValidLocation();
        return step.getLocation().distanceTo(location);
    }

    /**
     * Estimates the duration left, as a difference in the current time and projected arrival time.
     * @return duration in seconds
     */
    public double getBikingDuration() {
        return getBikingDuration(route.getType(), upcomingSteps, null);
    }

    /**
     * Estimates the duration left, as a difference in the current time and projected arrival time.
     * @param route the route to calculate the duration for
     * @return duration in seconds
     */
    public static double getBikingDuration(Route route) {
        return getBikingDuration(route.getType(), route.getSteps(), null);
    }

    /**
     * Estimate the total duration, on a particular leg.
     * This will not take into account that navigation might have started.
     * @param leg the leg that we want to calculate duration for
     * @return duration in seconds
     */
    public static double getBikingDuration(Route route, Leg leg) {
        return getBikingDuration(route.getType(), leg.getSteps(), null);
    }

    /**
     * Estimates the duration left, as a sum over all legs
     * @param type the type of route that we want to calculate duration for, vehicle affects speed.
     * @param steps the list of steps that the user is expected to take
     * @return duration in seconds
     */
    public static double getBikingDuration(RouteType type, List<TurnInstruction> steps, Location location) {
        double distance = getBikingDistance(steps, location);
        // The type of route affects the speed at which the user can travel.
        if(type.equals(RouteType.CARGO)) {
            return distance / AVERAGE_CARGO_BIKING_SPEED;
        } else {
            return distance / AVERAGE_BIKING_SPEED;
        }
    }

    public double getBikingDistance() {
        return getBikingDistance(upcomingSteps, lastKnownLocation);
    }

    public static double getBikingDistance(Route route) {
        return getBikingDistance(route.getSteps(), null);
    }

    public static double getBikingDistance(List<TurnInstruction> steps, Location location) {
        double distance = 0.0;

        // If a current location is known, we take into account the distance to the next step too.
        if(steps.size() > 0 && location != null) {
            distance += steps.get(0).getLocation().distanceTo(location);
        }

        // Sum over the distance of every step.
        for(TurnInstruction step: steps) {
            if(!step.getTransportType().isPublicTransportation()) {
                distance += step.getDistance();
            }
        }

        return distance;
    }

    public Date getArrivalTime() {
        return getArrivalTime(route, this);
    }

    public static Date getArrivalTime(Route route) {
        return getArrivalTime(route, null);
    }

    /**
     * Calculate the projected arrival time.
     * @param route a route to project arrival time for
     * @param state the state of navigation, if any.
     * @return the projected time of arrival.
     */
    public static Date getArrivalTime(Route route, NavigationState state) {
        List<TurnInstruction> upcomingSteps;
        if(state != null) {
            upcomingSteps = state.getUpcomingSteps();
        } else {
            upcomingSteps = route.getSteps();
        }

        TurnInstruction firstStepAffectingArrival = getFirstStepAffectingArrival(upcomingSteps);
        Log.d("NavigationState", "firstStepAffectingArrival.getTime() = " + firstStepAffectingArrival.getTime());

        // Figure out when the earliest departure can happen
        Date earliestDeparture;
        if(firstStepAffectingArrival != null && firstStepAffectingArrival.getTime() > 0) {
            earliestDeparture = new Date(firstStepAffectingArrival.getTime() * 1000);
        } else {
            earliestDeparture = new Date();
        }

        // Traverse the list of upcoming non-public steps, summing over the distance
        int firstStepAffectingArrivalIndex = upcomingSteps.indexOf(firstStepAffectingArrival);
        // Get the list of steps affecting arrival (those after the last leg with public transport).
        List<TurnInstruction> stepsAffectingArrival = upcomingSteps.subList(firstStepAffectingArrivalIndex, upcomingSteps.size()-1);

        Location location = null;
        if(firstStepAffectingArrivalIndex == 0) {
            // We are on the route and can affect the arrival time with our location
            location = BikeLocationService.getInstance().getLastValidLocation();
        }
        double duration = getBikingDuration(route.getType(), stepsAffectingArrival, location);

        Calendar c = Calendar.getInstance();
        c.setTime(earliestDeparture);
        c.add(Calendar.SECOND, (int) Math.round(duration));
        return c.getTime();
    }

    private static TurnInstruction getFirstStepAffectingArrival(List<TurnInstruction> upcomingSteps) {
        TurnInstruction lastNonPublicStep = null;
        // Traverse backwards through upcoming steps to find the last, that does not use a public
        // transportation type
        for(int s = upcomingSteps.size()-1; s >= 0; s--) {
            TurnInstruction step = upcomingSteps.get(s);
            if(step.getTransportType().isPublicTransportation()) {
                return lastNonPublicStep;
            } else {
                lastNonPublicStep = step;
            }
        }
        return lastNonPublicStep;
    }

    /**
     * Updates the pointIndex field on all steps in the route.
     * This should be called after all points and steps has been added
     */
    public void updateStepPointIndices() {
        stepToPointIndex.clear();
        if(!points.isEmpty() && !upcomingSteps.isEmpty()) {
            int pointIndex = 0;
            for(TurnInstruction step: upcomingSteps) {
                pointIndex = getNearestPointIndex(step.getLocation(), pointIndex);
                stepToPointIndex.put(step, pointIndex);
            }
        } else {
            throw new RuntimeException("Got a route without points or steps");
        }
    }

    @Override
    public void routeChanged() {
        updateStepPointIndices();
        // Load all turn instructions into the internal unified list of upcoming instructions
        // Fast forward to the nearest step on the route
    }

    /**
     * Loops through legs to find the one that has the next upcoming step in it
     * @return
     */
    public Leg getCurrentLeg() {
        TurnInstruction nextStep = getNextStep();
        for(Leg leg: route.getLegs()) {
            for(TurnInstruction step: leg.getSteps()) {
                if(step.equals(nextStep)) {
                    return leg;
                }
            }
        }
        return null;
    }

    public TurnInstruction getFirstStep() {
        if(route != null) {
            List<Leg> legs = route.getLegs();
            if(legs.size() > 0) {
                List<TurnInstruction> firstLegsSteps = legs.get(0).getSteps();
                if(firstLegsSteps.size() > 0) {
                    return firstLegsSteps.get(0);
                }
            }
        }
        return null;
    }
}
