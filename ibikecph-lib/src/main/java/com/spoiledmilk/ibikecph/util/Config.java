// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

public class Config {
    // TODO: Consider using build variants for this
    //public static String API_URL = "https://ibikecph-staging.herokuapp.com/api";
    public static String API_URL = "https://www.ibikecph.dk/api";
    public static final String API_SERVER_LOGIN = API_URL + "/login";
    public static final String API_SERVER_REGISTER = API_URL + "/users";
    public static final String API_SERVER_ADD_PASSWORD = API_SERVER_REGISTER + "/add_password";
    public static final String API_SERVER_HAS_PASSWORD = API_SERVER_REGISTER + "/has_password";
    public static final String API_SERVER_CHANGE_PASSWORD = API_SERVER_REGISTER + "/change_password";
    public static final String API_UPLOAD_TRACKS = API_URL + "/tracks";


    // routing
    public static final String OSRM_SERVER_FAST = "http://routes.ibikecph.dk/v1.1/fast";
    public static final String OSRM_SERVER_GREEN = "http://routes.ibikecph.dk/v1.1/green";
    public static final String OSRM_SERVER_CARGO = "http://routes.ibikecph.dk/v1.1/cargo";
    public static final String OSRM_SERVER_BREAK = API_URL + "/journey";

    public static boolean GREEN_ROUTES_ENABLED = true;

    public static final double GEOMETRY_DIGITS_LATITUDE = 1e6;
    public static final double GEOMETRY_DIGITS_LONGITUDE = 1e6;

    public static final double GEOMETRY_DIGITS_LATITUDE_RP = 1e5;
    public static final double GEOMETRY_DIGITS_LONGITUDE_RP = 1e5;

    public static final String OSRM_SERVER_DEFAULT = OSRM_SERVER_FAST;

    public static String OSRM_SERVER = OSRM_SERVER_DEFAULT;

    public static final String GEOCODER = "http://geo.oiorest.dk/adresser";

    // TODO uncomment this before submission
    public static final String ALTERNATE_TILESOURCE = "IBikeCPH";
//	public static final String ALTERNATE_TILESOURCE = "CycleMap";
    // TODO: No - Let's make it unnecessary to uncomment that before submission.

    public static final String USER_AGENT = "IBikeCPH/1.1";

    public static boolean LOG_ENABLED = true;

    public static final String HOCKEY_APP_ID = "d10e63f39cdbe7e8e3af275207388b2a";
    public static final boolean HOCKEY_UPDATES_ENABLED = true;

    public static boolean EXTENDED_PULL_TOUCH = true;
    public static boolean TTS_ENABLED = true;

    public static String TRACKING_TERMS_URL = "https://www.ibikecph.dk/terms";
    public static String TRACKING_USAGE_URL = "https://www.ibikecph.dk/about/privacy";

    public static String TRACKING_TERMS_JSON_URL = API_URL + "/terms";
}
