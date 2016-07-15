package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by kraen on 10-07-16.
 */
public class RegularRouteRequester extends RouteRequester {

    protected RouteType type;
    protected SMRoute route;

    public RegularRouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback, RouteType type) {
        super(start, end, callback);
        this.type = type;
    }

    public void setRoute(SMRoute route) {
        this.route = route;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        HttpURLConnection connection = null;
        try {
            URL url = generateURL();
            Log.d("RegularRouteRequester", "Requesting " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.ibikecph.v1");

            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Unexpected status code: " + connection.getResponseCode());
            }
            JsonNode node = Util.getJsonObjectMapper().readValue(connection.getInputStream(), JsonNode.class);
            parseJSON(node);
            return true;
        } catch (Exception e) {
            Log.e("RegularRouteRequester", "Error requesting route", e);
            return false;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    protected SMRoute parseJSON(JsonNode node) {
        if (route == null) {
            route = new SMRoute(Util.locationFromGeoPoint(start),
                                Util.locationFromGeoPoint(end),
                                type);
        }
        route.parseFromJson(node);
        return route;
    }

    protected URL generateURL() {
        // OSRM directive to not ignore small road fragments
        int z = 18;
        String baseURL;

        switch (type) {
            case CARGO:
                baseURL = Config.OSRMv4_SERVER + "/cargo";
                break;
            case GREEN:
                baseURL = Config.OSRMv4_SERVER + "/green";
                break;
            case FASTEST:
            default:
                baseURL = Config.OSRMv4_SERVER + "/fast";
                break;
        }
        String url = String.format(Locale.US, "%s/viaroute?z=%d&alt=false&loc=%.6f,%.6f&loc=%.6f,%.6f&instructions=true",
                baseURL,
                z,
                start.getLatitude(),
                start.getLongitude(),
                end.getLatitude(),
                end.getLongitude());

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e("RegularRouteRequester", "Error generating the url", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if(success && route != null) {
            callback.onSuccess(route);
        } else {
            callback.onFailure();
        }
    }
}
