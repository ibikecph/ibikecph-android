package dk.kk.ibikecph.test;

import android.support.test.runner.AndroidJUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by kraen on 17-08-16.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationTest {

    Route route;

    @Before
    public void setUp() throws Exception {
        route = new Route(RouteType.FASTEST);
        /*
        String jsonString = "{}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readValue(jsonString, JsonNode.class);
        route.parseFromJson(json);
        */
    }

    @Test
    public void testRouteCreation() throws Exception {

    }
}
