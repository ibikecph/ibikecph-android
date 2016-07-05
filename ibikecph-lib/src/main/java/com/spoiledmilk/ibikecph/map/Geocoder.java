package com.spoiledmilk.ibikecph.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Config;
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

        if (type == RouteType.BREAK) {
            BreakRouteRequester requester = new BreakRouteRequester(start, end, callback);
            requester.execute();
        } else {
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
                    baseURL = Config.OSRM_SERVER_FAST;
                    break;
                default:
                    baseURL = Config.OSRM_SERVER_DEFAULT;
                    break;
            }
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
