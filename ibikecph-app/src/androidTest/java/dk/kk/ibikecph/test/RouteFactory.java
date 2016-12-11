package dk.kk.ibikecph.test;

import android.location.Location;
import android.util.Log;

import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

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

    enum LegType {
        STRAIGHT,
        HALF_CIRCULAR
    }

    static Location getOffsetLocation(Location start, double horizontalDistance, double verticalDistance) {
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

    static Leg generateLeg(List<Location> points, List<TurnInstruction> instructions, double distance) {
        Leg leg = new Leg();
        leg.getPoints().addAll(points);
        leg.getSteps().addAll(instructions);
        leg.setDistance(distance);
        return leg;
    }

    static Leg generateStraightLeg(double distance) {
        return generateStraightLeg(distance, DEFAULT_DEPARTURE);
    }

    static Leg generateStraightLeg(double distance, Location departure) {
        double stepSize = distance / 10.0; // 10 points along the line
        // Create the points
        List<Location> points = new LinkedList<>();
        for(double d = 0.0; d <= distance; d += stepSize) {
            Location l = getOffsetLocation(departure, 0.0, d);
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
        departInstruction.setDistance((float) (distance/2.0));
        instructions.add(departInstruction);

        TurnInstruction middleInstruction = new TurnInstruction();
        middleInstruction.setLocation(middlePoint);
        middleInstruction.setType(TurnInstruction.Type.TURN);
        middleInstruction.setDistance((float) (distance/2.0));
        instructions.add(middleInstruction);

        TurnInstruction arrivalInstruction = new TurnInstruction();
        arrivalInstruction.setLocation(lastPoint);
        arrivalInstruction.setType(TurnInstruction.Type.ARRIVE);
        instructions.add(arrivalInstruction);

        return generateLeg(points, instructions, distance);
    }

    static Leg generateHalfCircularLeg(double distance, Location departure) {
        double radius = distance / (2.0 * Math.PI);
        double stepSize = distance / 40.0; // 10 points on each of the four half-arcs
        // Create the points
        List<Location> points = new LinkedList<>();
        for(double d = 0.0; d <= distance; d += stepSize) {
            double ratio = d / distance;
            double radians = 2 * Math.PI * ratio;

            double horizontalOffset = 0.0 - (radius * Math.sin(radians));
            double verticalOffset;
            if(ratio < 0.5)  {
                // First half circle
                verticalOffset = radius * Math.cos(radians) - radius;
            } else {
                // Second half circle
                verticalOffset = radius * Math.cos(radians - Math.PI) - 3.0 * radius;
            }

            Location l = getOffsetLocation(departure, horizontalOffset, verticalOffset);
            points.add(l);
        }
        Location departureLocation = points.get(0);
        Location threeFourthsLocation = points.get((points.size()-1) / 4 * 3);
        Location arrivalLocation = points.get(points.size()-1);

        // Create three instructions along the way
        List<TurnInstruction> instructions = new ArrayList<>();

        TurnInstruction departInstruction = new TurnInstruction();
        departInstruction.setLocation(departureLocation);
        departInstruction.setType(TurnInstruction.Type.DEPART);
        departInstruction.setDistance((float) (distance/4.0*3.0));
        instructions.add(departInstruction);

        TurnInstruction threeFourthsInstruction = new TurnInstruction();
        threeFourthsInstruction.setLocation(threeFourthsLocation);
        threeFourthsInstruction.setType(TurnInstruction.Type.CONTINUE);
        threeFourthsInstruction.setDistance((float) (distance/4.0));
        instructions.add(threeFourthsInstruction);

        TurnInstruction arrivalInstruction = new TurnInstruction();
        arrivalInstruction.setLocation(arrivalLocation);
        arrivalInstruction.setType(TurnInstruction.Type.ARRIVE);
        instructions.add(arrivalInstruction);

        return generateLeg(points, instructions, distance);
    }

    static Route generateStraightRoute(double distance) {
        return generateStraightRoute(distance, 1);
    }

    static Route generateStraightRoute(double distance, int legs) {
        return generateRoute(distance, legs, LegType.STRAIGHT);
    }

    static Route generateHalfCircularRoute(double distance, int legs) {
        return generateRoute(distance, legs, LegType.HALF_CIRCULAR);
    }

    static Route generateRoute(double distance, int legs, LegType legType) {
        if(legs < 1) {
            throw new RuntimeException("Expected at least one leg");
        }

        Route route = new Route(RouteType.FASTEST);

        Location start = DEFAULT_DEPARTURE;
        for(int l = 0; l < legs; l++) {
            Leg leg;
            if(legType.equals(LegType.STRAIGHT)) {
                leg = generateStraightLeg(distance / legs, start);
            } else if(legType.equals(LegType.HALF_CIRCULAR)) {
                leg = generateHalfCircularLeg(distance / legs, start);
            } else {
                throw new RuntimeException("Unexpected leg type: " + legType);
            }
            route.getLegs().add(leg);
            start = leg.getEndLocation();
        }

        Address startAddress = new Address();
        startAddress.setName("Start of mock route");
        startAddress.setLocation(Util.locationToLatLng(route.getStartLocation()));
        route.setStartAddress(startAddress);

        Address endAddress = new Address();
        endAddress.setName("End of mock route");
        endAddress.setLocation(Util.locationToLatLng(route.getEndLocation()));
        route.setEndAddress(endAddress);

        return route;
    }

}
