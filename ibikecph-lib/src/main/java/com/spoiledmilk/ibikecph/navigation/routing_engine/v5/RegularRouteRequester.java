package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.RouteRequester;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by kraen on 11-07-16.
 */
public class RegularRouteRequester extends RouteRequester {

    private Float bearing = null;
    protected RouteType type;
    protected SMRoute route;

    public void setRoute(SMRoute route) {
        this.route = route;
    }

    public RegularRouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback, RouteType type) {
        super(start, end, callback);
        this.type = type;
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

    protected URL generateURL() {
        String baseURL = Config.OSRMv5_SERVER;
        switch (type) {
            case CARGO:
                baseURL += "/cargo";
                break;
            case GREEN:
                baseURL += "/green";
                break;
            case FASTEST:
            default:
                baseURL += "/fast";
                break;
        }

        String url = String.format(
            Locale.US,
            "%s/route/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=polyline&steps=true&alternatives=false",
            baseURL,
            start.getLongitude(),
            start.getLatitude(),
            end.getLongitude(),
            end.getLatitude()
        );

        if(bearing != null) {
            // Tells ORSM to start the route facing in the users direction +- 20 deg
            // and allows OSRM to end the route from any direction
            url += "&bearings=" + Math.round(bearing) + ",20;0,180";
        }

        if(route != null && route.getDestinationHint() != null) {
            url += "&hints=;" + route.getDestinationHint();
        }

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e("RegularRouteRequester", "Error generating the url", e);
            return null;
        }
    }

    protected SMRoute parseJSON(JsonNode node) {
        if (route == null) {
            route = new Route(Util.locationFromGeoPoint(start), Util.locationFromGeoPoint(end), type);
        }
        route.parseFromJson(node);
        return route;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if(success && route != null) {
            callback.onSuccess(route);
            route.emitRouteUpdated();
        } else {
            callback.onFailure();
        }
    }

    public void setBearing(float bearing) {
        if(bearing < 0 || bearing > 360) {
            throw new RuntimeException("Expected a non-negative bearing below 360 degrees");
        }
        this.bearing = bearing;
    }
}
