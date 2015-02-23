// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

public class Config {
	//public static String serverUrl = "http://ibikecph-staging.herokuapp.com/api";

	public static String serverUrl = "https://www.ibikecph.dk/api";
	public static final String API_SERVER_LOGIN = serverUrl + "/login";
	public static final String API_SERVER_REGISTER = serverUrl + "/users";
	public static final String GOOGLE_API_KEY = "AIzaSyAZwBZgYS-61R-gIvp4GtnekJGGrIKh0Dk";
//	public static final String OSRM_SERVER_BICYCLE = "http://routes.ibikecph.dk";
//	public static final String OSRM_SERVER_CARGO = "http://routes.ibikecph.dk/cargo";
//	public static final String OSRM_SERVER_FAST = "http://routes.ibikecph.dk/fast";
//	public static final String OSRM_SERVER_GREEN = "http://routes.ibikecph.dk/green";
	
	//New server
	public static final String OSRM_SERVER_FAST = "http://routes.ibikecph.dk/v1.1/fast";
	public static final String OSRM_SERVER_GREEN = "http://routes.ibikecph.dk/v1.1/green";
	public static final String OSRM_SERVER_CARGO = "http://routes.ibikecph.dk/v1.1/cargo";
	
	public static boolean GREEN_ROUTES_ENABLED = true;

    public static final double GEOMETRY_DIGITS_LATITUDE = 1e6;
    public static final double GEOMETRY_DIGITS_LONGITUDE = 1e6;
	
    public static final String OSRM_SERVER_DEFAULT = "http://routes.ibikecph.dk/v1.1/fast";
    
	public static String OSRM_SERVER = OSRM_SERVER_DEFAULT;
	public static final String GEOCODER = "http://geo.oiorest.dk/adresser";
	// TODO uncomment this before submission
	public static final String ALTERNATE_TILESOURCE = "IBikeCPH";
//	public static final String ALTERNATE_TILESOURCE = "CycleMap";
	public static final String USER_AGENT = "IBikeCPH/1.1";
	public static boolean LOG_ENABLED = true;
	public static final String HOCKEY_APP_ID = "f145bf4833683cfaa1744bf799eee64b";
	public static final boolean HOCKEY_UPDATES_ENABLED = true;
	public static final boolean ANALYTICS_ENABLED = false;
	
	public static boolean EXTENDED_PULL_TOUCH = true;
	public static boolean TTS_ENABLED = true;

    // TODO: get real urls
    public static String TRACKING_TERMS_URL = "http://google.com";
    public static String TRACKING_USAGE_URL = "http://google.com";
}
