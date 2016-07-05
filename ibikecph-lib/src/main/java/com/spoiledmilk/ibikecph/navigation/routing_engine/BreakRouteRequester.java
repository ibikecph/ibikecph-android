package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kraen on 26-06-16.
 */
public class BreakRouteRequester extends AsyncTask<Void, Void, Boolean> {

    protected ILatLng start, end;
    protected Geocoder.RouteCallback callback;
    protected String token;

    protected static int RETRY_DELAY = 1000;
    protected static int RETRY_TIMEOUT = 30000;

    protected BreakRouteResponse response;

    public BreakRouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback) {
        this.start = start;
        this.end = end;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            HttpURLConnection connection = null;
            try {
                String dataString = String.format(Locale.US,
                        "loc[]=%.6f,%.6f&loc[]=%.6f,%.6f",
                        start.getLatitude(),
                        start.getLongitude(),
                        end.getLatitude(),
                        end.getLongitude());

                URL url = new URL(Config.API_BREAK_ROUTE);
                Log.d("BreakRouteRequester", "Requesting " + url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept", "application/vnd.ibikecph.v1");
                connection.getOutputStream().write(dataString.getBytes("UTF-8"));
                if(connection.getResponseCode() != 200) {
                    throw new RuntimeException("Unexpected status code: " + connection.getResponseCode());
                }
                JsonNode root = Util.getJsonObjectMapper().readValue(connection.getInputStream(), JsonNode.class);
                if(root == null || !root.has("token") || !root.get("token").isTextual()) {
                    throw new RuntimeException("Unexpected JSON response");
                }
                token = root.get("token").asText();
                startPollingForCompletion();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Malformed URL when requesting break route", e);
            } catch (ProtocolException e) {
                throw new RuntimeException("Strange protocol when requesting break route", e);
            } catch (IOException e) {
                throw new RuntimeException("IO error when requesting break route", e);
            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
            }
        } catch (Exception e) {
            Log.e("BreakRouteRequester", "Error requesting journey", e);
            return false;
        }
        return true;
    }

    protected void startPollingForCompletion() throws InterruptedException {
        Date start = new Date();
        Date now = new Date();
        while (now.getTime()-start.getTime() < RETRY_TIMEOUT && response == null) {
            Thread.sleep(RETRY_DELAY);
            response = pollForCompletion();
            now = new Date();
        }
    }

    protected BreakRouteResponse pollForCompletion() {
        Log.d("BreakRouteRequester", "Polling the backend for completion of a broken route!");
        HttpURLConnection connection = null;
        try {
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            String urlString = String.format("%s/%s", Config.API_BREAK_ROUTE, encodedToken);
            URL url = new URL(urlString);
            Log.d("BreakRouteRequester", "Requesting " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.ibikecph.v1");
            if(connection.getResponseCode() == 422) {
                Log.d("BreakRouteRequester", "The journey wasn't ready yet");
                // It's not ready yet
                return null;
            } else if(connection.getResponseCode() == 200) {
                Log.d("BreakRouteRequester", "Journey is ready!");
                ArrayNode json = Util.getJsonObjectMapper().readValue(connection.getInputStream(), ArrayNode.class);
                return new BreakRouteResponse(json);
            } else {
                throw new RuntimeException("Unexpected status code: " + connection.getResponseCode());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL when requesting break route", e);
        } catch (ProtocolException e) {
            throw new RuntimeException("Strange protocol when requesting break route", e);
        } catch (IOException e) {
            throw new RuntimeException("IO error when requesting break route", e);
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if(success && response != null) {
            // parseResponse(response.getJsonNode());
            callback.onSuccess(response);
        } else {
            if(response != null) {
                Log.e("BreakRouteRequest", "Break route error, with result: " + response.toString());
            }
            callback.onFailure();
        }
    }

    /* *
     * Parses the response from the journey API and stores information in statics on the Geocoder
     * TODO: Stop using this parser, as the Journey parses the JSON on initialization.
     * @deprecated This should be re-implemented to not store information in fields on the Geocoder
     * @param node The response from calling the journey API.
     * /
    protected void parseResponse(JsonNode node) {
        Log.d("BreakRouteRequester", "Parsing response: " + node.toString());
        // Make route objects for each route piece in each route suggestion.
        SMRoute route;
        Geocoder.arrayLists = new ArrayList<>(); // One array for each route-suggestion, which contains x route pieces
        ArrayList<SMRoute> smRoutesArr; // route pieces

        Geocoder.totalTime = new ArrayList<>();
        Geocoder.totalBikeDistance = new ArrayList<>();
        Geocoder.totalDistance = new ArrayList<>();
        Geocoder.arrivalTime = new ArrayList<>();
        Geocoder.from = new ArrayList<>();
        Geocoder.to = new ArrayList<>();

        for (int i = 0; i < response.getJsonNode().size(); i++) {
            smRoutesArr = new ArrayList<>();
            for (int j = 0; j < node.get(i).path("journey").size(); j++) {
                int viaPointsSize = node.get(i).path("journey").get(j).path("via_points").size();
                Location loc1 = Util.locationFromCoordinates(node.get(i).path("journey").get(j).path("via_points").get(0).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(0).get(1).asDouble());
                Location loc2 = Util.locationFromCoordinates(node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(1).asDouble());

                route = new SMRoute(loc1, loc2, node.get(i).path("journey").get(j), RouteType.BREAK);

                // Add the route piece to the route-array
                smRoutesArr.add(route);

                Address start = new Address();
                Address end = new Address();

                start.setStreet(node.get(i).path("journey").get(j).path("route_name").get(0).textValue());
                start.setLocation(new LatLng(node.get(i).path("journey").get(j).path("via_points").get(0).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(0).get(1).asDouble()));
                end.setStreet(node.get(i).path("journey").get(j).path("route_name").get(1).textValue());
                end.setLocation(new LatLng(node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(0).asDouble(), node.get(i).path("journey").get(j).path("via_points").get(viaPointsSize - 1).get(1).asDouble()));

                smRoutesArr.get(j).startAddress = start;
                smRoutesArr.get(j).endAddress = end;

            }
            // Add the route pieces-array to the route-suggestion-array
            Geocoder.arrayLists.add(smRoutesArr);

            Geocoder.totalDistance.add(node.get(i).path("journey_summary").path("total_distance").asInt());
            Geocoder.totalTime.add(node.get(i).path("journey_summary").path("total_time").asInt());
            Geocoder.totalBikeDistance.add(node.get(i).path("journey_summary").path("total_bike_distance").asInt());
            Geocoder.arrivalTime.add(node.get(i).path("journey").get(node.get(i).path("journey").size() - 1).path("route_summary").path("arrival_time").asLong());
            Geocoder.from.add(node.get(i).path("journey").get(0).path("route_name").get(0).textValue());
            //to.add(node.get(i).path("journey").get(node.get(i).path("journey").size() - 1).path("route_name").get(1).textValue());
        }
    }
    */
}
