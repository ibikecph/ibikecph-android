package com.spoiledmilk.ibikecph.map;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.Util;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by jens on 5/30/15.
 */
public class Geocoder {

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<ArrayList<SMRoute>> arrayLists;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<Integer> totalTime;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<Integer> totalDistance;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<Integer> totalBikeDistance;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<Long> arrivalTime;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<String> from;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ArrayList<String> to;

    public interface GeocoderCallback {
        public void onSuccess(Address address);

        public void onFailure();
    }

    public interface RouteCallback {
        void onSuccess(SMRoute route);
        void onSuccess(BreakRouteRequester.BreakRouteResponse breakRouteResponse);
        void onFailure();
    }

    /**
     * Returns an Address for a given LatLng.
     *
     * @param location
     * @param callback
     */
    public static void getAddressForLocation(final ILatLng location, final GeocoderCallback callback) {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = String.format(Locale.US, "%s/%f,%f.json", Config.GEOCODER, location.getLatitude(), location.getLongitude());
        client.get(url, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, org.apache.http.Header[] headers, org.json.JSONObject response) {
                Marker m;

                try {
                    Address address = new Address(
                            ((JSONObject) response.get("vejnavn")).getString("navn"),
                            response.getString("husnr"),
                            ((JSONObject) response.get("postnummer")).getString("nr"),
                            ((JSONObject) response.get("postnummer")).getString("navn"),
                            location.getLatitude(),
                            location.getLongitude());

                    callback.onSuccess(address);
                    m = new IBCMarker(address.getStreetAddress(), address.getPostCodeAndCity(), (LatLng) location, MarkerType.ADDRESS);
                } catch (JSONException e) {
                    callback.onFailure();
                    m = new IBCMarker("Ukendt position", "", (LatLng) location, MarkerType.ADDRESS);
                }
            }
        });
    }

    /**
     * Returns a route from the OSRM server
     *
     * @param start
     * @param end
     * @param callback
     */
    public static void getRoute(final ILatLng start, final ILatLng end, final RouteCallback callback, final RouteType type) {
        AsyncHttpClient client = new AsyncHttpClient();

        // OSRM directive to not ignore small road fragments
        int z = 18;
        String baseURL;
        final String url;

        switch (type) {
            case CARGO:
                baseURL = Config.OSRM_SERVER_CARGO;
                break;
            case GREEN:
                baseURL = Config.OSRM_SERVER_GREEN;
                break;
            case BREAK:
                baseURL = Config.API_BREAK_ROUTE;
                break;
            case FASTEST:
            default:
                baseURL = Config.OSRM_SERVER_DEFAULT;
                break;
        }

        if (type == RouteType.BREAK) {
            url = String.format(Locale.US,
                                "%s?loc[]=%.6f,%.6f&loc[]=%.6f,%.6f",
                                baseURL,
                                start.getLatitude(),
                                start.getLongitude(),
                                end.getLatitude(),
                                end.getLongitude());

            final SMHttpRequest.RouteInfo[] ri = new SMHttpRequest.RouteInfo[1];

            new AsyncTask<String, Integer, String>() {
                @Override
                protected String doInBackground(String... strings) {
                    ri[0] = new SMHttpRequest.RouteInfo(HttpUtils.get(url, true), Util.locationFromGeoPoint(start), Util.locationFromGeoPoint(end));
                    return null;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    if (ri[0] == null || ri[0].jsonRoot == null || ri[0].jsonRoot.path("status").asInt(-1) != 0) {
                        // Log.d("DV", "jsonRoot = " + ri.jsonRoot);

                        if (ri[0] != null && ri[0].jsonRoot != null) {
                            Log.d("DV_break", "ri != null");
                            int amountOfRoutes = ri[0].jsonRoot.size(); // Gets the amount of routes.
                            MapActivity.breakRouteJSON = ri[0].jsonRoot;
                            MapActivity.obsInt.set(amountOfRoutes); // Set the amount of route suggestions in order to display this amount in the fragmentAdapter

                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode node = null;
                            try {
                                node = mapper.readTree(ri[0].jsonRoot.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            // Make route objects for each route piece in each route suggestion.
                            SMRoute route;
                            arrayLists = new ArrayList<ArrayList<SMRoute>>(); // One array for each route-suggestion, which contains x route pieces
                            ArrayList<SMRoute> smRoutesArr; // route pieces

                            for (int i = 0; i < amountOfRoutes; i++) {
                                smRoutesArr = new ArrayList<SMRoute>();
                                for (int j = 0; j < node.get(i).path("journey").size(); j++) {
                                    route = new SMRoute();
                                    route.transportType = node.get(i).path("journey").get(j).path("route_summary").path("type").textValue();

                                    // Add the route piece to the route-array
                                    smRoutesArr.add(route);
                                    Log.d("DV_break", "Route type = " + route.transportType);
                                }
                                // Add the route pieces-array to the route-suggestion-array
                                arrayLists.add(smRoutesArr);
                            }

                            totalTime = new ArrayList<Integer>();
                            totalBikeDistance = new ArrayList<Integer>();
                            totalDistance = new ArrayList<Integer>();
                            arrivalTime = new ArrayList<Long>();
                            from = new ArrayList<String>();
                            to = new ArrayList<String>();

                            for (int i = 0; i < arrayLists.size(); i++) {
                                for (int j = 0; j < arrayLists.get(i).size(); j++) {

                                    int viaPointsSize = node.get(i).path("journey").get(j).path("via_points").size();
                                    Location loc1 = Util.locationFromCoordinates(node.get(i).path("journey").get(j).path("via_points").get(0).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(0).get(1).asDouble());
                                    Location loc2 = Util.locationFromCoordinates(node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(1).asDouble());

                                    Address start = new Address();
                                    Address end = new Address();

                                    start.setStreet(node.get(i).path("journey").get(j).path("route_name").get(0).textValue());
                                    start.setLocation(new LatLng(node.get(i).path("journey").get(j).path("via_points").get(0).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(0).get(1).asDouble()));
                                    end.setStreet(node.get(i).path("journey").get(j).path("route_name").get(1).textValue());
                                    end.setLocation(new LatLng(node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(1).asDouble()));

                                    arrayLists.get(i).get(j).init(loc1, loc2, routeListener, node.get(i).path("journey").get(j), type);
                                    arrayLists.get(i).get(j).startAddress = start;
                                    arrayLists.get(i).get(j).endAddress = end;

                                }

                                totalDistance.add(node.get(i).path("journey_summary").path("total_distance").asInt());
                                totalTime.add(node.get(i).path("journey_summary").path("total_time").asInt());
                                totalBikeDistance.add(node.get(i).path("journey_summary").path("total_bike_distance").asInt());
                                arrivalTime.add(node.get(i).path("journey").get(node.get(i).path("journey").size() - 1).path("route_summary").path("arrival_time").asLong());
                                from.add(node.get(i).path("journey").get(0).path("route_name").get(0).textValue());
                                //to.add(node.get(i).path("journey").get(node.get(i).path("journey").size() - 1).path("route_name").get(1).textValue());
                            }
                            callback.onSuccess(true);
                        } else {
                            callback.onFailure();
                        }
                    }

                }
            }.execute();
        } else {
            url = String.format(Locale.US, "%s/viaroute?z=%d&alt=false&loc=%.6f,%.6f&loc=%.6f,%.6f&instructions=true",
                    baseURL,
                    z,
                    start.getLatitude(),
                    start.getLongitude(),
                    end.getLatitude(),
                    end.getLongitude());

            client.get(url, new JsonHttpResponseHandler() {
                public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
                    // Convert the JSONObject into a Jackson JsonNode to make it compatible with SMRoute
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(response.toString());

                        SMRoute route = new SMRoute();
                        route.init(Util.locationFromGeoPoint(start), Util.locationFromGeoPoint(end), node, type);
                        // Pass the route back to the caller
                        callback.onSuccess(route);
                    } catch (IOException e) {
                        // Couldn't parse the JSON. We pass the exception to the onFailure handler.
                        onFailure(statusCode, headers, null);
                    }
                }

                public void onFailure(int statusCode, Header[] headers, JSONObject response) {
                    callback.onFailure();
                }
            });
        }
    }
}
