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
        // http://routes.ibikecph.dk/v5/green/route/v1/profile/
        // 12.53059,55.70903;12.45839,55.68029
        // ?overview=full
        // &geometries=polyline
        // &hints=;
        // &steps=false
        // &alternatives=false

        // OSRM directive to not ignore small road fragments
        int z = 18;
        String baseURL;

        switch (type) {
            case CARGO:
                baseURL = Config.OSRM_SERVER_CARGO;
                break;
            case GREEN:
                baseURL = Config.OSRM_SERVER_GREEN;
                break;
            case FASTEST:
                baseURL = Config.OSRM_SERVER_FAST;
                break;
            default:
                baseURL = Config.OSRM_SERVER_DEFAULT;
                break;
        }
        String url = String.format(
            Locale.US,
            "%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=polyline&hints=;&steps=true&alternatives=false",
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
