package com.spoiledmilk.ibikecph.map;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.util.Config;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by jens on 5/30/15.
 */
public class Geocoder {

    public interface GeocoderCallback {
        public void onSuccess(OverviewMapHandler.Address address);
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
}
