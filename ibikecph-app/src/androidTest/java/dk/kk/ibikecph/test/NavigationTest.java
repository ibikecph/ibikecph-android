package dk.kk.ibikecph.test;

import android.location.Location;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.navigation.NavigationState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static dk.kk.ibikecph.test.RouteFactory.DEFAULT_DEPARTURE;
import static dk.kk.ibikecph.test.RouteFactory.generateStraightRoute;

/**
 * Created by kraen on 17-08-16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NavigationTest {

    static String LOG_TAG = NavigationTest.class.getSimpleName();
    static double STRAIGHT_DISTANCE = 600.0;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSimpleRouteCreation() {
        Route route = generateStraightRoute(STRAIGHT_DISTANCE);

        Log.d(LOG_TAG, "Created a route: " + route.toString());
        Assert.assertNotEquals(0, route.getLegs().size());
        Assert.assertNotEquals(0, route.getLegs().get(0).getPoints().size());

        Log.d(LOG_TAG, "Route has " + route.getLegs().get(0).getPoints().size() + " points");
        Log.d(LOG_TAG, "Route starts " + route.getStartLocation());
        Log.d(LOG_TAG, "Route ends " + route.getEndLocation());

        List<Location> points = route.getLegs().get(0).getPoints();

        NavigationState state = new NavigationState();
        state.setRoute(route);
        Log.d(LOG_TAG, "Route has " + state.getUpcomingSteps().size() + " upcoming steps");

        BikeLocationService locationService = BikeLocationService.getInstance();
        // Let's position ourselves at the beginning of the route
        Location startLocation = RouteFactory.getOffsetLocation(points.get(0), -10.0, -10.0);
        locationService.onLocationChanged(startLocation);

        double distance = state.getBikingDistance();
        Assert.assertEquals("Distance is right", STRAIGHT_DISTANCE + (Math.sqrt(2) * 10.0), distance, 0.25);
    }

    public static void followRoute(NavigationState state, double distanceToRoute) {
        Location location;
        // Calculate what the distance should be per axis to achieve the desired distance to route.
        double perAxisOffsetToRoute = 0.0 - Math.sqrt(Math.pow(distanceToRoute, 2) / 2.0);

        // Emulate location changes
        BikeLocationService locationService = BikeLocationService.getInstance();
        double lastDistance = Double.MAX_VALUE;

        final double MAXIMAL_EXPECTED_DISTANCE = NavigationState.getBikingDistance(state.getRoute()) + distanceToRoute;

        // Follow the points instead of just moving along in a straight line
        for(Location p: state.getRoute().getPoints()) {
            location = RouteFactory.getOffsetLocation(p, perAxisOffsetToRoute, perAxisOffsetToRoute);
            // Move to this new location
            locationService.onLocationChanged(location);
            // Calculate the biking distance left
            double distanceLeft = state.getBikingDistance();
            Log.d(LOG_TAG, "Moving up on the map, now at " + location + " distance is " + distanceLeft);

            // Let's assert that we move towards the destination
            String msg = "Distance is increasing (" + lastDistance + "m before " + distanceLeft + "m now)";
            Assert.assertTrue(msg, lastDistance >= distanceLeft);

            // Let's assert that we never move more than MAXIMAL_EXPECTED_DISTANCE away
            msg = "Distance was too far way from the route (" + MAXIMAL_EXPECTED_DISTANCE + "m expected " + distanceLeft + "m now)";
            Assert.assertTrue(msg, MAXIMAL_EXPECTED_DISTANCE >= distanceLeft);

            // Next iteration ..
            // Save the current distance for next iteration
            lastDistance = distanceLeft;
        }

        Assert.assertTrue(state.isDestinationReached());
    }

    @Test
    public void testMultilegRouteCreation() {
        final int LEGS = 3;
        // Create a three-legged straight route
        Route route = generateStraightRoute(STRAIGHT_DISTANCE, LEGS);

        for(Location l: route.getPoints()) {
            Log.d(LOG_TAG, l.getLatitude() + "," + l.getLongitude());
        }

        Assert.assertEquals(LEGS, route.getLegs().size());
        // 10 points + arrival per leg
        Assert.assertEquals(11 * LEGS, route.getPoints().size());

        NavigationState state = new NavigationState();
        state.setRoute(route);

        Log.d(LOG_TAG, "Starting navigation on a " + state.getBikingDistance() + "m long route");
        // Let's start at a distance to the route
        final double DISTANCE_TO_ROUTE = -1.0;
        Location location = RouteFactory.getOffsetLocation(DEFAULT_DEPARTURE, DISTANCE_TO_ROUTE, DISTANCE_TO_ROUTE);
        final double MAXIMAL_EXPECTED_DISTANCE = Math.sqrt(Math.abs(DISTANCE_TO_ROUTE)*2) + STRAIGHT_DISTANCE;

        // Emulate location changes
        final double STEP_SIZE = 5.0;
        BikeLocationService locationService = BikeLocationService.getInstance();
        double lastDistance = Double.MAX_VALUE;
        // Cannot use the followRoute method as this test is trying to move more independently from
        // the points on the route, to provoke an error
        while(!state.isDestinationReached()) {
            // Move to this new location
            locationService.onLocationChanged(location);
            // Calculate the biking distance left
            double distanceLeft = state.getBikingDistance();
            Log.d(LOG_TAG, "Moving up on the map, now at " + location + " distance is " + distanceLeft);

            // Let's assert that we move towards the destination
            String msg = "Distance is increasing (" + lastDistance + "m before " + distanceLeft + "m now)";
            Assert.assertTrue(msg, lastDistance >= distanceLeft);
            // Let's assert that we never move more than MAXIMAL_EXPECTED_DISTANCE away
            msg = "Distance was too far way from the route (" + MAXIMAL_EXPECTED_DISTANCE + "m expected " + distanceLeft + "m now)";
            Assert.assertTrue(msg, MAXIMAL_EXPECTED_DISTANCE >= distanceLeft);

            // Next iteration ..
            // Save the current distance for next iteration
            lastDistance = distanceLeft;
            // Advance upwards on the map ... 5 metres at a time
            location = RouteFactory.getOffsetLocation(location, 0.0, STEP_SIZE);
        }
    }

    @Test
    public void testHalfCircularRouteCreation() {
        final int LEGS = 3;
        // Create a three-legged straight route
        Route route = RouteFactory.generateHalfCircularRoute(STRAIGHT_DISTANCE, LEGS);

        for(Location l: route.getPoints()) {
            Log.d(LOG_TAG, l.getLatitude() + "," + l.getLongitude());
        }

        Assert.assertEquals(LEGS, route.getLegs().size());
        // 10 points per half + arrival point on LEGS circles
        Assert.assertEquals(41 * LEGS, route.getPoints().size());

        NavigationState state = new NavigationState();
        state.setRoute(route);

        Log.d(LOG_TAG, "Starting navigation on a " + state.getBikingDistance() + "m circular route");
        followRoute(state, 1.0);
    }
}
