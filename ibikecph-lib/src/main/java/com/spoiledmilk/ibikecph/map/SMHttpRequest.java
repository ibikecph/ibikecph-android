// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Implements the API calls to search for routes, find nearest points, etc.
 * Also has some model stuff embedded.
 * @author jens
 *
 */
public class SMHttpRequest {

    protected static final int GEOCODER_SEARCH_RADIUS = 50;

    public static final int REQUEST_GET_ROUTE = 2;
    public static final int REQUEST_FIND_NEAREST_LOC = 3;
    public static final int REQUEST_FIND_PLACES_FOR_LOC = 4;
    public static final int REQUEST_GET_RECALCULATED_ROUTE = 5;

    static Handler handler;

    private RouteInfo z10Route = null;

    static class Result {
        Object response;
        SMHttpRequestListener listener;

        public Result(Object response, SMHttpRequestListener listener) {
            this.response = response;
            this.listener = listener;
        }
    }

    // TODO: There is also a com.spoiledmilk.ibikecph.search.Address!
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

    public void getRoute(final Location start, final Location end, List<Location> viaPoints, final SMHttpRequestListener listener) {
        getRoute(start, end, viaPoints, null, null, null, listener, REQUEST_GET_ROUTE, 18, false);
    }

    public void getRecalculatedRoute(final Location start, final Location end, List<Location> viaPoints, String chksum,
            final SMHttpRequestListener listener) {
        getRoute(start, end, viaPoints, chksum, null, null, listener, REQUEST_GET_RECALCULATED_ROUTE, 18, false);
    }

    public void getRecalculatedRoute(final Location start, final Location end, List<Location> viaPoints, final String chksum, final String startHint,
            final String hint, final SMHttpRequestListener listener) {
        getRoute(start, end, viaPoints, chksum, startHint, hint, listener, REQUEST_GET_RECALCULATED_ROUTE, 18, false);
    }

    public void getRoute(final Location start, final Location end, final List<Location> viaPoints, final String chksum, final String startHint,
            final String hint, final SMHttpRequestListener listener, final int msgType, final int z, final boolean isFromZ10) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url;
                if (startHint != null) {
                    url = String.format(Locale.US, "%s/viaroute?z=" + z + "&alt=false&loc=%.6f,%.6f&hint=" + startHint + "", Config.OSRM_SERVER,

                    start.getLatitude(), start.getLongitude());
                } else {
                    url = String.format(Locale.US, "%s/viaroute?z=" + z + "&alt=false&loc=%.6f,%.6f", Config.OSRM_SERVER, start.getLatitude(),
                            start.getLongitude());
                }

                if (viaPoints != null) {
                    Iterator<Location> it = viaPoints.iterator();
                    while (it.hasNext()) {
                        Location loc = it.next();
                        url += String.format(Locale.US, "&loc=%.6f,%.6f", loc.getLatitude(), loc.getLongitude());
                    }
                }

                if (chksum != null) {
                    if (hint != null) {
                        url += String.format(Locale.US, "&loc=%.6f,%.6f&hint=%s&instructions=true&checksum=%s", end.getLatitude(),
                                end.getLongitude(), hint, chksum);
                    } else {
                        url += String.format(Locale.US, "&loc=%.6f,%.6f&instructions=true&checksum=%s", end.getLatitude(), end.getLongitude(), chksum);
                    }
                } else
                    url += String.format(Locale.US, "&loc=%.6f,%.6f&instructions=true", end.getLatitude(), end.getLongitude());
                LOG.d("Routes request = " + url);
                RouteInfo ri = new RouteInfo(HttpUtils.get(url), start, end);
                if (ri == null || ri.jsonRoot == null || ri.jsonRoot.path("status").asInt(-1) != 0) {
                    // try to get the route with the z = 10
                    if (!isFromZ10)
                        getRouteZ10(start, end, viaPoints, chksum, startHint, hint, listener, msgType);
                    else
                        sendMsg(msgType, z10Route, listener);
                } else
                    // Route found
                    sendMsg(msgType, ri, listener);

                // String response =
                // "{\"version\": 0.3,\"status\":0,\"status_message\": \"Found route between points\",\"route_geometry\": \"swyrI}jpkAJu@RuAJq@PoALs@l@qEVgBn@qEJw@v@cGUMq@c@AAIEUOYSKG]USMCCYQCAi@]OK}@e@IEEC_@SIGSOIGOKcAk@MIqAy@XsBFe@NgARyARuAN_AJq@^aCFg@h@kD?Cn@yDFa@Lu@DoAu@Co@KKCUIQIOKOI]Y\",\"route_instructions\": [[\"10\",\"Dyrl�gevej\",109,0,26,\"109m\",\"E\",112],[\"1\",\"Kastanievej\",282,5,76,\"282m\",\"E\",111],[\"7\",\"H.C. �rsteds Vej\",368,10,97,\"368m\",\"N\",20],[\"3\",\"Forchhammersvej\",412,35,109,\"412m\",\"E\",112],[\"7\",\"Vodroffsvej\",124,50,19,\"124m\",\"N\",2],[\"15\",\"\",0,58,0,\"\",\"N\",0.0]],\"route_summary\":{\"total_distance\":1297,\"total_time\":347,\"start_point\":\"service\",\"end_point\":\"Vodroffsvej\"},\"alternative_geometries\": [],\"alternative_instructions\":[],\"alternative_summaries\":[],\"route_name\":[\"H.C. �rsteds Vej\",\"Forchhammersvej\"],\"alternative_names\":[[\"\",\"\"]],\"via_points\":[[55.67882,12.54079 ],[55.68029,12.55526 ]],\"hint_data\": {\"checksum\":1952171896, \"locations\": [\"V5xqALEAAABNAAAAAAAAAAAAAAAAAPA_ivVUAL8iEwB\", \"QEEpACEAAAAsAAAATQAAACl-jExyHNc_HfZUAGYoEwB\"]},\"transactionId\": \"OSRM Routing Engine JSON Descriptor (v0.3)\"}";

            }
        }).start();
    }

    public void getRouteZ10(final Location start, final Location end, final List<Location> viaPoints, final String chksum, final String startHint,
            final String hint, final SMHttpRequestListener listener, final int msgType) {
        z10Route = null;
        String url;
        if (startHint != null) {
            url = String.format(Locale.US, "%s/viaroute?z=10&alt=false&loc=%.6f,%.6f&hint=" + startHint + "", Config.OSRM_SERVER,

            start.getLatitude(), start.getLongitude());
        } else {
            url = String.format(Locale.US, "%s/viaroute?z=10&alt=false&loc=%.6f,%.6f", Config.OSRM_SERVER, start.getLatitude(), start.getLongitude());
        }

        if (viaPoints != null) {
            Iterator<Location> it = viaPoints.iterator();
            while (it.hasNext()) {
                Location loc = it.next();
                url += String.format(Locale.US, "&loc=%.6f,%.6f", loc.getLatitude(), loc.getLongitude());
            }
        }

        if (chksum != null) {
            if (hint != null) {
                url += String.format(Locale.US, "&loc=%.6f,%.6f&hint=%s&instructions=true&checksum=%s", end.getLatitude(), end.getLongitude(), hint,
                        chksum);
            } else {
                url += String.format(Locale.US, "&loc=%.6f,%.6f&instructions=true&checksum=%s", end.getLatitude(), end.getLongitude(), chksum);
            }
        } else
            url += String.format(Locale.US, "&loc=%.6f,%.6f&instructions=true", end.getLatitude(), end.getLongitude());

        RouteInfo ri = new RouteInfo(HttpUtils.get(url), start, end);
        z10Route = ri;
        if (ri == null || ri.jsonRoot == null || ri.jsonRoot.path("status").asInt(-1) != 0) {
            // Can't find the route
            sendMsg(msgType, ri, listener);
        } else {
            // try again with the z = 18 and a checksum hint
            String checksum = "", endHint = "", hintStart = "";

            if (ri.jsonRoot.has("hint_data") && ri.jsonRoot.get("hint_data").has("checksum"))
                checksum = ri.jsonRoot.get("hint_data").get("checksum").asText();
            if (ri.jsonRoot.has("hint_data") && ri.jsonRoot.get("hint_data").has("locations")) {
                JsonNode locations = ri.jsonRoot.get("hint_data").get("locations");
                hintStart = locations.get(0).asText();
                endHint = locations.get(locations.size() - 1).asText();
            }
            getRoute(start, end, viaPoints, checksum, hintStart, endHint, listener, msgType, 18, true);
        }
    }

    /*
    public void findNearestPoint(final IGeoPoint loc, final SMHttpRequestListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = String.format(Locale.US, "%s/nearest?loc=%.6f,%.6f", Config.OSRM_SERVER, loc.getLatitudeE6() / (float) 1E6,
                        loc.getLongitudeE6() / (float) 1E6);
                JsonNode response = HttpUtils.get(url);
                Location loc = null;
                if (response != null && response.path("mapped_coordinate").get(0) != null && response.path("mapped_coordinate").get(1) != null) {
                    LOG.d("response: " + response.toString());
                    loc = Util.locationFromCoordinates(response.path("mapped_coordinate").get(0).asDouble(), response.path("mapped_coordinate")
                            .get(1).asDouble());
                }
                sendMsg(REQUEST_FIND_NEAREST_LOC, (Object) loc, listener);
            }
        }).start();
    }
*/
    public void findPlacesForLocation(final Location loc, final SMHttpRequestListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = String.format(Locale.US, "%s/%f,%f.json", Config.GEOCODER, loc.getLatitude(), loc.getLongitude()); // ,%d
                
                // GEOCODER_SEARCH_RADIUS
                JsonNode response = HttpUtils.get(url);
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

    public void sendMsg(int what, Object response, SMHttpRequestListener listener) {
        if (listener != null) {
            Message msg = new Message();
            msg.what = what;
            msg.obj = new Result(response, listener);
            handler.sendMessage(msg);
        }
    }
}
