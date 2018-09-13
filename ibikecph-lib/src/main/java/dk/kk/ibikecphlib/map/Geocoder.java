package dk.kk.ibikecphlib.map;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;

import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteRequester;
import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteResponse;
import dk.kk.ibikecphlib.navigation.routing_engine.RegularRouteRequester;
import dk.kk.ibikecphlib.navigation.routing_engine.RouteRequester;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.util.LOG;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.net.Uri;

import org.apache.http.Header;

/**
 * Created by jens on 5/30/15.
 */
public class Geocoder {

    public interface GeocoderCallback {
        void onSuccess(Address address);
        void onFailure();
    }

    public interface RouteCallback {
        void onSuccess(Route route);
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
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("kortforsyningen.kms.dk")
                .appendPath("search")
                .appendQueryParameter("servicename", "RestGeokeys_v2")
                .appendQueryParameter("hits", "1")
                .appendQueryParameter("method", "nadresse")
                .appendQueryParameter("geop", String.format(Locale.US, "%f,%f", location.getLongitude(), location.getLatitude()) )
                .appendQueryParameter("georef", "EPSG:4326")
                .appendQueryParameter("georad", "50")
                .appendQueryParameter("outgeoref", "EPSG:4326")
                .appendQueryParameter("login", "ibikecph")
                .appendQueryParameter("password", "Spoiledmilk123")
                .appendQueryParameter("geometry", "false");

        String url = builder.build().toString();
        LOG.d("Geocoding url: " + url);


        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, org.json.JSONObject response) {
                if (statusCode != 200) {
                    callback.onFailure();
                } else {
                    try {
                        LOG.d("Got response: " + response);

                        JSONArray features = response.getJSONArray("features");
                        JSONObject feature = features.getJSONObject(0);
                        JSONObject properties = feature.getJSONObject("properties");

                        String vejnavn = properties.getString("vej_navn");
                        String husnr = properties.getString("husnr");
                        String postnr = properties.getString("postdistrikt_kode");
                        String by = properties.getString("postdistrikt_navn");

                        Address address = new Address( vejnavn, husnr, postnr, by , location.getLatitude(), location.getLongitude());

                        callback.onSuccess(address);
                    } catch (JSONException e) {
                        LOG.d( "Exception: " + e);
                        callback.onFailure();
                    }
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
