package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kraen on 26-06-16.
 */
public class BreakRouteRequester extends RouteRequester {

    protected String token;

    protected static int RETRY_DELAY = 1000;
    protected static int RETRY_TIMEOUT = 30000;

    protected BreakRouteResponse response;

    public BreakRouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback) {
        super(start, end, callback);
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
                ObjectNode json = Util.getJsonObjectMapper().readValue(connection.getInputStream(), ObjectNode.class);
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
            callback.onSuccess(response);
        } else {
            if(response != null) {
                Log.e("BreakRouteRequest", "Break route error, with result: " + response.toString());
            }
            callback.onFailure();
        }
    }

}
