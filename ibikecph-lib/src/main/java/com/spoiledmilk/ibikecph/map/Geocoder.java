package com.spoiledmilk.ibikecph.map;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteRequester;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteResponse;
import com.spoiledmilk.ibikecph.navigation.routing_engine.v5.RegularRouteRequester;
import com.spoiledmilk.ibikecph.navigation.routing_engine.RouteRequester;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Config;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by jens on 5/30/15.
 */
public class Geocoder {

    public interface GeocoderCallback {
        void onSuccess(Address address);
        void onFailure();
    }

    public interface RouteCallback {
        void onSuccess(SMRoute route);
        void onSuccess(BreakRouteResponse breakRouteResponse);
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
                try {
                    Address address = new Address(
                            ((JSONObject) response.get("vejnavn")).getString("navn"),
                            response.getString("husnr"),
                            ((JSONObject) response.get("postnummer")).getString("nr"),
                            ((JSONObject) response.get("postnummer")).getString("navn"),
                            location.getLatitude(),
                            location.getLongitude());

                    callback.onSuccess(address);
                } catch (JSONException e) {
                    callback.onFailure();
                }
            }
        });
    }

    /**
     * Requests a route from the OSRM server and calls the callback when done or failed
     * @param start
     * @param end
     * @param callback
     * @param type
     */
    public static void getRoute(final ILatLng start, final ILatLng end, final RouteCallback callback, final RouteType type) {
        RouteRequester requester;
        if (type == RouteType.BREAK) {
            requester = new BreakRouteRequester(start, end, callback);
        } else {
            requester = new RegularRouteRequester(start, end, callback, type);
        }
        requesters.add(requester);
        requester.execute();
    }

    protected static List<RouteRequester> requesters = new ArrayList<>();

    public static void cancelRequests() {
        for(RouteRequester requester: requesters) {
            if(!requester.isCancelled()) {
                requester.cancel(true);
            }
        }
    }
}
