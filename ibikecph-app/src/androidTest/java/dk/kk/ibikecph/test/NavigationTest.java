package dk.kk.ibikecph.test;

import android.location.Location;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.navigation.NavigationState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import dk.kk.ibikecph.IBikeCPHApplication;

import static dk.kk.ibikecph.test.RouteFactory.generateStraightRoute;

/**
 * Created by kraen on 17-08-16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NavigationTest {

    static String LOG_TAG = NavigationTest.class.getSimpleName();
    static double STRAIGHT_DISTANCE = 500.0;

    Route route;

    @Before
    public void setUp() throws Exception {
        route = generateStraightRoute(STRAIGHT_DISTANCE, 10.0);
    }

    @Test
    public void testRouteCreation() {
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
        Location startLocation = RouteFactory.getOffesetLocation(points.get(0), -10.0, -10.0);
        locationService.onLocationChanged(startLocation);

        Log.d(LOG_TAG, "Distance when starting is " + state.getBikingDistance());
        Assert.assertEquals(STRAIGHT_DISTANCE + (Math.sqrt(2) * 10.0), state.getBikingDistance(), 0.25);
    }
}
