package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by kraen on 11-07-16.
 */
public class RegularRouteRequester extends com.spoiledmilk.ibikecph.navigation.routing_engine.RegularRouteRequester {

    public RegularRouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback, RouteType type) {
        super(start, end, callback, type);
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
            "%s/route/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=polyline&hints=;&steps=true&alternatives=false",
            baseURL,
            start.getLongitude(),
            start.getLatitude(),
            end.getLongitude(),
            end.getLatitude()
        );

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e("RegularRouteRequester", "Error generating the url", e);
            return null;
        }
    }

    @Override
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
        route.emitRouteUpdated();
    }
}
