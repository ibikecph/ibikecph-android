// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.map;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import dk.kk.ibikecphlib.persist.TrackLocation;
import dk.kk.ibikecphlib.util.Config;
import dk.kk.ibikecphlib.util.HttpUtils;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dk.kk.ibikecphlib.persist.TrackLocation;
import dk.kk.ibikecphlib.util.Util;

/**
 * Implements the API calls to search for routes, find nearest points, etc.
 * Also has some model stuff embedded.
 * @deprecated Use the RegularRouteRequester instead
 * @author jens
 */
public class SMHttpRequest {

    /**
     * @Deprecated Use the RouteRequester classes instead of this.
     */
    public static final int REQUEST_GET_ROUTE = 2;
    public static final int REQUEST_FIND_NEAREST_LOC = 3;
    public static final int REQUEST_FIND_PLACES_FOR_LOC = 4;
    public static final int REQUEST_GET_RECALCULATED_ROUTE = 5;

    static Handler handler;

    static class Result {
        Object response;
        SMHttpRequestListener listener;

        public Result(Object response, SMHttpRequestListener listener) {
            this.response = response;
            this.listener = listener;
        }
    }

    // TODO: There is also a dk.kk.ibikecph.search.Address!
    public static class Address {
        public String street;
        public String houseNumber;
        public String zip;
        public String city;
        public double lat;
        public double lon;

        public Address(String street, String houseNumber, String zip, String city, double lat, double lon) {
            this.street = street;
            this.houseNumber = houseNumber;
            this.zip = zip;
            this.city = city;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static class RouteInfo {
        public JsonNode jsonRoot;
        public Location start;
        public Location end;

        public RouteInfo(JsonNode jsonRoot, Location start, Location end) {
            this.jsonRoot = jsonRoot;
            this.start = start;
            this.end = end;
        }
    }

    public SMHttpRequest() {

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Result res = (Result) msg.obj;
                if (res == null)
                    return false;

                switch (msg.what) {
                    case REQUEST_GET_ROUTE:
                        break;
                    case REQUEST_FIND_NEAREST_LOC:
                        break;
                    case REQUEST_FIND_PLACES_FOR_LOC:
                        break;
                    case REQUEST_GET_RECALCULATED_ROUTE:
                        break;
                }

                if (res.listener != null)
                    res.listener.onResponseReceived(msg.what, res.response);

                return true;
            }
        });
    }

    public static void findPlacesForLocation(final ILatLng loc, final SMHttpRequestListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = String.format(Locale.US, "%s/%f,%f.json", "deprecated", loc.getLatitude(), loc.getLongitude()); // ,%d

                // GEOCODER_SEARCH_RADIUS
                JsonNode response = HttpUtils.get(url, false);
                Address a = null;
                if (response != null) {
                    if (response.size() > 0) {
                        a = new Address(response.path("vejnavn").path("navn").asText(), response.path("husnr").asText(), response.path("postnummer")
                                .path("nr").asText(), response.path("kommune").path("navn").asText(), loc.getLatitude(), loc.getLongitude());
                    } else {
                        a = new Address(Util.limitDecimalPlaces(loc.getLatitude(), 4) + "\n" + Util.limitDecimalPlaces(loc.getLongitude(), 4), "",
                                "", "", loc.getLatitude(), loc.getLongitude());
                    }
                } else {
                    a = new Address(Util.limitDecimalPlaces(loc.getLatitude(), 4) + "\n" + Util.limitDecimalPlaces(loc.getLongitude(), 4), "", "",
                            "", loc.getLatitude(), loc.getLongitude());
                }
                sendMsg(REQUEST_FIND_PLACES_FOR_LOC, a, listener);
            }
        }).start();
    }

    public static void findPlacesForLocation(final Location loc, final SMHttpRequestListener listener) {
        findPlacesForLocation(new LatLng(loc), listener);
    }


    public void findPlacesForLocation(final TrackLocation loc, final SMHttpRequestListener listener) {
        Location lloc = new Location("SMHttpRequest");
        lloc.setLatitude(loc.getLatitude());
        lloc.setLongitude(loc.getLongitude());
        findPlacesForLocation(lloc, listener);
    }

    public static void sendMsg(int what, Object response, SMHttpRequestListener listener) {
        if (listener != null) {
            Message msg = new Message();
            msg.what = what;
            msg.obj = new Result(response, listener);
            handler.sendMessage(msg);
        }
    }
}
