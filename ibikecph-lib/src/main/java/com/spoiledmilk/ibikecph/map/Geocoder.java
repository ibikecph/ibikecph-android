package com.spoiledmilk.ibikecph.map;

import android.util.Log;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.Util;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by jens on 5/30/15.
 */
public class Geocoder {

    public interface GeocoderCallback {
        public void onSuccess(OverviewMapHandler.Address address);
        public void onFailure();
    }

    public interface RouteCallback {
        public void onSuccess(SMRoute route);
        public void onFailure();
    }

    /**
     * Returns an Address for a given LatLng.
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
                    OverviewMapHandler.Address address = new OverviewMapHandler.Address(
                            ((JSONObject) response.get("vejnavn")).getString("navn"),
                            response.getString("husnr"),
                            ((JSONObject) response.get("postnummer")).getString("nr"),
                            ((JSONObject) response.get("postnummer")).getString("navn"),
                            location.getLatitude(),
                            location.getLongitude());

                    callback.onSuccess(address);
                    m = new Marker(address.getStreetAddress(), address.getPostCodeAndCity(), (LatLng) location);
                } catch (JSONException e) {
                    callback.onFailure();
                    m = new Marker("Ukendt position", "", (LatLng) location);
                }
            }
        });
    }

    /**
     * Returns a route from the OSRM server
     * @param start
     * @param end
     * @param callback
     */
    public static void getRoute(final ILatLng start, final ILatLng end, final RouteCallback callback, final SMRouteListener routeListener) {
        AsyncHttpClient client = new AsyncHttpClient();

        // OSRM directive to not ignore small road fragments
        int z = 18;

        String url = String.format(Locale.US, "%s/viaroute?z=" + z + "&alt=false&loc=%.6f,%.6f&loc=%.6f,%.6f&instructions=true",
                Config.OSRM_SERVER,
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
                    route.init(Util.latLngToLocation(start), Util.latLngToLocation(end), routeListener, node);

                    callback.onSuccess(route);

                } catch(IOException e) {
                    // Couldn't parse the JSON. We pass the exception to the onFailure handler.
                    onFailure(statusCode, headers, null);
                }
            }

            public void onFailure(int statusCode, Header[] headers, JSONObject response) {
                callback.onFailure();
            }
        });

        Log.d("JC", url);
    }
}
