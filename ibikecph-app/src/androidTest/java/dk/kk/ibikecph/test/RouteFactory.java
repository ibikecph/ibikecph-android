package dk.kk.ibikecph.test;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class creates fictitious routes to be used when testing the navigation.
 * Created by kraen on 08-12-16.
 */

public class RouteFactory {

    static String LOG_TAG = RouteFactory.class.getSimpleName();
    static String LOCATION_PROVIDER = "RouteFactory mock location";
    static Location DEFAULT_DEPARTURE = new Location(LOCATION_PROVIDER);
    static {
        // Somewhere on "Amager Fælled"
        DEFAULT_DEPARTURE.setLatitude(55.602736);
        DEFAULT_DEPARTURE.setLongitude(12.547932);
    }
    static double EARTHS_RADIUS = 6378137;

    static Location getOffesetLocation(Location start, double horizontalDistance, double verticalDistance) {
        Location result = new Location(LOCATION_PROVIDER);

        double latitudeDifference = 180.0 / Math.PI * verticalDistance / EARTHS_RADIUS;
        result.setLatitude(start.getLatitude() + latitudeDifference);

        double longitudeDifference = 180.0 / Math.PI * horizontalDistance / EARTHS_RADIUS / Math.cos(start.getLatitude());
        result.setLongitude(start.getLongitude() + longitudeDifference);

        return result;
    }

    /*
    static String generateRouteJson(List<Location> points, double distance) {
        StringWriter sw = new StringWriter();
        JsonFactory jsonFactory = new JsonFactory();

        try {
            JsonGenerator json = jsonFactory.createGenerator(sw);
            generateRouteJson(points, distance, json);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error writing json", e);
        }

        return sw.toString();
    }

    static void generateRouteJson(List<Location> points, double distance, JsonGenerator json) throws IOException {
        json.writeStartObject();

        json.writeFieldName("legs");
        json.writeStartArray();
        generateLegJson(points, distance, json);
        json.writeEndArray();

        json.writeEndObject();
    }

    static void generateLegJson(List<Location> points, double distance, JsonGenerator json) throws IOException {
        // Let's create a single leg object
        json.writeStartObject();

        // Steps
        json.writeFieldName("steps");
        json.writeStartArray();
        for(Location p: points) {
            json.
        }
        json.writeEndArray();

        // Distance
        json.writeNumberField("distance", distance);

        json.writeEndObject();
    }
    */

    static Route generateRoute(List<Location> points, List<TurnInstruction> instructions, double distance) {
        Route route = new Route(RouteType.FASTEST);

        Leg leg = new Leg();
        leg.getPoints().addAll(points);
        leg.getSteps().addAll(instructions);
        leg.setDistance(distance);

        route.getLegs().add(leg);

        return route;
    }

    static Route generateStraightRoute(double distance, double stepSize) {
        // Create the points
        List<Location> points = new LinkedList<>();
        points.add(DEFAULT_DEPARTURE);
        for(double d = 0.0; d < distance; d += stepSize) {
            Location l = getOffesetLocation(DEFAULT_DEPARTURE, d, 0.0);
            points.add(l);
        }
        Location firstPoint = points.get(0);
        Location middlePoint = points.get(points.size() / 2);
        Location lastPoint = points.get(points.size()-1);

        // Create three instructions along the way
        List<TurnInstruction> instructions = new ArrayList<>();

        TurnInstruction departInstruction = new TurnInstruction();
        departInstruction.setLocation(firstPoint);
        departInstruction.setType(TurnInstruction.Type.DEPART);
        instructions.add(departInstruction);

        TurnInstruction middleInstruction = new TurnInstruction();
        middleInstruction.setLocation(middlePoint);
        middleInstruction.setType(TurnInstruction.Type.TURN);
        middleInstruction.setDistance((float) (distance/2.0));
        instructions.add(middleInstruction);

        TurnInstruction arrivalInstruction = new TurnInstruction();
        arrivalInstruction.setLocation(lastPoint);
        arrivalInstruction.setType(TurnInstruction.Type.ARRIVE);
        arrivalInstruction.setDistance((float) (distance/2.0));
        instructions.add(arrivalInstruction);

        Route route = generateRoute(points, instructions, distance);

        Address startAddress = new Address();
        startAddress.setName("Start of mock route");
        startAddress.setLocation(Util.locationToLatLng(DEFAULT_DEPARTURE));
        route.setStartAddress(startAddress);

        Address endAddress = new Address();
        endAddress.setName("End of mock route");
        endAddress.setLocation(Util.locationToLatLng(lastPoint));
        route.setEndAddress(endAddress);

        return route;
    }

}
