// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import com.spoiledmilk.ibikecph.BuildConfig;

public class Config {

    public static String BASE_URL;
    public static String API_URL;
    public static String API_SERVER_LOGIN;
    public static String API_SERVER_REGISTER;
    public static String API_SERVER_ADD_PASSWORD;
    public static String API_SERVER_HAS_PASSWORD;
    public static String API_SERVER_CHANGE_PASSWORD;
    public static String API_UPLOAD_TRACKS;
    public static String API_BREAK_ROUTE;

    // routing
    public static final String OSRMv5_SERVER = "https://routes.ibikecph.dk/v5";
    public static final String OSRMv4_SERVER = "https://routes.ibikecph.dk/v1.1";

    public static boolean GREEN_ROUTES_ENABLED = true;

    public static final String GEOCODER = "http://geo.oiorest.dk/adresser";

    // TODO uncomment this before submission
    public static final String ALTERNATE_TILESOURCE = "IBikeCPH";
//	public static final String ALTERNATE_TILESOURCE = "CycleMap";
    // TODO: No - Let's make it unnecessary to uncomment that before submission.

    public static final String USER_AGENT = "IBikeCPH/1.1";

    public static boolean LOG_ENABLED = true;

    public static boolean EXTENDED_PULL_TOUCH = true;
    public static boolean TTS_ENABLED = true;

    public static String ABOUT_TERMS_URL_DA = "https://www.ibikecph.dk/terms";
    public static String ABOUT_TERMS_URL_EN = "https://www.ibikecph.dk/en/terms";
    /*
    public static String ABOUT_PRIVACY_URL_DA = "https://www.ibikecph.dk/about/privacy";
    public static String ABOUT_PRIVACY_URL_EN = "https://www.ibikecph.dk/en/about/privacy";
    */

    public static String TRACKING_TERMS_JSON_URL;

    /**
     * Generates the API URLs from a base URL, the reason for this method is that we want to control
     * which API is used based on a build variant.
     * @param baseUrl
     */
    public static void generateUrls(final String baseUrl) {
        BASE_URL = baseUrl;
        API_URL = BASE_URL + "/api";
        API_SERVER_LOGIN = API_URL + "/login";
        API_SERVER_REGISTER = API_URL + "/users";
        API_SERVER_ADD_PASSWORD = API_SERVER_REGISTER + "/add_password";
        API_SERVER_HAS_PASSWORD = API_SERVER_REGISTER + "/has_password";
        API_SERVER_CHANGE_PASSWORD = API_SERVER_REGISTER + "/change_password";
        API_UPLOAD_TRACKS = API_URL + "/tracks";
        API_BREAK_ROUTE = API_URL + "/journeys";
        TRACKING_TERMS_JSON_URL = API_URL + "/terms";
    }
}
