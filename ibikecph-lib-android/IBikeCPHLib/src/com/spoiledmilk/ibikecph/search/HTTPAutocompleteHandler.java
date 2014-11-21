// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class HTTPAutocompleteHandler {

	static final String FOURSQUARE_ID = "AFXG5WVI4UTINRGVJZ52ZAWRK454EN4J3FZRJB03J4ZMXQX1";
	static final String FOURSQUARE_SECRET = "D2EU4WKSQ2WHQGOK4FJVNRDZUJ4S4YTZVBO1FM4V03NRJWYK";
	static final double GEOCODING_SEARCH_RADIUS = 50000.0;
	static final String PLACES_SEARCH_RADIUS = "20000";
	static final String FOURSQUARE_SEARCH_RADIUS = "20000";
	static final String PLACES_LANGUAGE = "da";
	static final String OIOREST_SEARCH_RADIUS = "50";
	static final String OIOREST_AUTOCOMPLETE_SEARCH_RADIUS = "20000";
	static final String OIOREST_GET_ADDRESS_URL = "http://geo.oiorest.dk/adresser/";
	static final String FOURSQUARE_CATEGORIES = "4d4b7104d754a06370d81259,4d4b7105d754a06372d81259,4d4b7105d754a06373d81259,4d4b7105d754a06374d81259,4d4b7105d754a06376d81259,4d4b7105d754a06377d81259,4d4b7105d754a06375d81259,5032891291d4c4b30a586d68,4f2a210c4b9023bd5841ed28,4d4b7105d754a06378d81259,4bf58dd8d48988d1ed931735,4e4c9077bd41f78e849722f9,4bf58dd8d48988d12d951735,4bf58dd8d48988d1fa931735,4bf58dd8d48988d1fc931735,4e74f6cabd41c4836eac4c31,4bf58dd8d48988d1ef941735,4f4530164b9074f6e4fb00ff,4bf58dd8d48988d129951735";
	static final String FOURSQUARE_LIMIT = "10";

	public static JsonNode getOiorestGeocode(String urlString, String houseNumber) {
		JsonNode rootNode = performGET(urlString);
		JsonNode ret = null;
		if (rootNode != null && rootNode.size() != 0) {
			ret = rootNode.get(0);
			for (int i = 0; i < rootNode.size(); i++) {
				if (rootNode.get(i).has("husnr") && rootNode.get(i).get("husnr").asText().toLowerCase(Locale.US).equals(houseNumber)) {
					ret = rootNode.get(i);
					break;
				}
			}
		}
		if (ret != null && ret.has("husnr")) {
			LOG.d("Geocode succesfull, searched number = " + houseNumber + " foundNumber = " + ret.get("husnr").asText());
		}
		return ret;

	}

	public static JsonNode getOiorestAddress(double lat, double lon) {
		JsonNode rootNode = performGET(OIOREST_GET_ADDRESS_URL + lat + "," + lon + ".json");
		return rootNode;

	}

	public static List<JsonNode> getOiorestAutocomplete(String searchText) {
		String urlString;
		List<JsonNode> list = null;
		try {
			urlString = "http://geo.oiorest.dk/adresser.json?q=" + URLEncoder.encode(searchText, "UTF-8") + "&maxantal=50";
			JsonNode rootNode = performGET(urlString);
			list = Util.JsonNodeToList(rootNode);
		} catch (UnsupportedEncodingException e) {

			LOG.e(e.getLocalizedMessage());
		}

		return list;

	}

	@SuppressLint("NewApi")
	public static List<JsonNode> getFoursquareAutocomplete(Address address, Context context, Location location) {

		double lat;
		double lon;
		try {
			lat = location.getLatitude();
			lon = location.getLongitude();
		} catch (NullPointerException e) {
			lat = -1.0;
			lon = -1.0;
		}

		String urlString;
		List<JsonNode> list = null;
		try {
			String query = URLEncoder.encode(normalizeToAlphabet(address.street), "UTF-8");

			String near = null;
			if (address.zip != null && !address.zip.equals("")) {
				if (address.city != null && !address.city.equals("")) {
					near = address.zip + " " + address.city;
				} else {
					near = address.zip + ", Denmark";
				}
			} else {
				if (address.city != null && !address.city.equals("")) {
					near = address.city;
				}
			}

			if (near != null && !near.equals("")) {
				urlString = "https://api.foursquare.com/v2/venues/search?intent=browse&near=" + URLEncoder.encode(near, "UTF-8")
						+ "&client_id=" + FOURSQUARE_ID + "&client_secret=" + FOURSQUARE_SECRET + "&query="
						+ URLEncoder.encode(query, "UTF-8") + "&v=20130301&radius=" + FOURSQUARE_SEARCH_RADIUS + "&limit="
						+ FOURSQUARE_LIMIT + "&categoryId=" + FOURSQUARE_CATEGORIES;
			} else {
				urlString = "https://api.foursquare.com/v2/venues/search?intent=browse&ll=" + lat + "," + lon + "&client_id="
						+ FOURSQUARE_ID + "&client_secret=" + FOURSQUARE_SECRET + "&query=" + URLEncoder.encode(query, "UTF-8") + "&v="
						+ 20130301 + "&radius=" + FOURSQUARE_SEARCH_RADIUS + "&limit=" + FOURSQUARE_LIMIT + "&categoryId="
						+ FOURSQUARE_CATEGORIES;
			}

			JsonNode rootNode = performGET(urlString);

			if (rootNode != null && rootNode.has("response")) {
				if (rootNode.get("response").has("minivenues")) {
					list = Util.JsonNodeToList(rootNode.get("response").get("minivenues"));
				} else if (rootNode.get("response").has("venues")) {
					list = Util.JsonNodeToList(rootNode.get("response").get("venues"));
				}
			}

		} catch (UnsupportedEncodingException e) {
			LOG.e(e.getLocalizedMessage());
		}

		return list;
	}

	public static JsonNode performGET(String urlString) {
		JsonNode ret = null;

		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, 20000);
		HttpConnectionParams.setSoTimeout(myParams, 20000);
		HttpClient httpclient = new DefaultHttpClient(myParams);
		HttpGet httpget = null;

		URL url = null;

		try {

			url = new URL(urlString);
			httpget = new HttpGet(url.toString());
			LOG.d("Request " + url.toString());
			httpget.setHeader("Content-type", "application/json");
			HttpResponse response = httpclient.execute(httpget);
			String serverResponse = EntityUtils.toString(response.getEntity());
			LOG.d("Response " + serverResponse);
			ret = Util.stringToJsonNode(serverResponse);

		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}
		return ret;
	}

	public static List<JsonNode> getKortforsyningenAutocomplete(Location currentLocation, Address address) {
		String urlString;
		List<JsonNode> list = new ArrayList<JsonNode>();
		try {

			urlString = "http://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=";

			if ((address.number == null || address.number.equals("")) && (address.zip == null || address.zip.equals(""))
					&& (address.city == null || address.city.equals("") || address.city.equals(address.street))) {
				urlString += "vej"; // street search
			} else {
				urlString += "adresse"; // address search
			}

			urlString += "&vejnavn=*" + URLEncoder.encode(address.street, "UTF-8") + "*";

			// urlString = "http://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=adresse&vejnavn=*"
			// + URLEncoder.encode(address.street, "UTF-8") + "*";

			if (!(address.number == null || address.number.equals(""))) {
				urlString += "&husnr=" + address.number;
			}

			urlString += "&geop=" + Util.limitDecimalPlaces(currentLocation.getLongitude(), 6) + "" + ","
					+ Util.limitDecimalPlaces(currentLocation.getLatitude(), 6) + ""
					+ "&georef=EPSG:4326&outgeoref=EPSG:4326&login=ibikecph&password=Spoiledmilk123&hits=10";

			if (address.zip != null & !address.zip.equals("")) {
				urlString = urlString + "&postnr=" + address.zip;
			}
			if (address.city != null & !address.city.equals("") && !address.city.equals(address.street)) {
				// urlString = urlString + "&by=" + URLEncoder.encode(address.city.trim(), "UTF-8") + "*";
				urlString = urlString + "&postdist=*" + URLEncoder.encode(address.city.trim(), "UTF-8") + "*";
			}

			urlString += "&geometry=true";

			JsonNode rootNode = performGET(urlString);
			if (rootNode.has("features")) {
				JsonNode features = rootNode.get("features");
				for (int i = 0; i < features.size(); i++) {
					if (features.get(i).has("properties")
							&& (features.get(i).get("properties").has("vej_navn") || features.get(i).get("properties").has("navn")))
						list.add(features.get(i));
				}
			}
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}
		return list;
	}

	public static List<JsonNode> getKortforsyningenPlaces(Location currentLocation, Address address) {
		String urlString;
		List<JsonNode> list = new ArrayList<JsonNode>();
		if (address.city == null || address.city.equals("")) {
			address.city = address.street;
		}
		try {

			urlString = "http://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=sted&stednavn=" + "*"
					+ URLEncoder.encode(address.street, "UTF-8") + "*&geop=" + ""
					+ Util.limitDecimalPlaces(currentLocation.getLongitude(), 6) + "," + ""
					+ Util.limitDecimalPlaces(currentLocation.getLatitude(), 6)
					+ "&georef=EPSG:4326&outgeoref=EPSG:4326&login=ibikecph&password=Spoiledmilk123&hits=10&distinct=true";
			if (address.city != null & !address.city.equals("") && !address.street.equals(address.street)) {
				// urlString = urlString + "&by=" + URLEncoder.encode(address.city.trim(), "UTF-8") + "*";
				// urlString += "&postdist=" + URLEncoder.encode(address.city.trim(), "UTF-8");// + "*";
			}
			JsonNode rootNode = performGET(urlString);
			if (rootNode.has("features")) {
				JsonNode features = rootNode.get("features");
				for (int i = 0; i < features.size(); i++) {
					if (features.get(i).has("properties") && features.get(i).get("properties").has("navn"))
						list.add(features.get(i));
				}
			}
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}
		return list;
	}

	public static JsonNode getKortforsyningenGeocode(String url) {
		JsonNode rootNode = performGET(url);
		return rootNode;
	}

	public static String normalizeToAlphabet(String s) {
		String ret = new String(s);
		ret = ret.replaceAll("ø|Ø", "o");
		ret = ret.replaceAll("å|Å", "a");
		ret = ret.replaceAll("æ|Æ", "ae");
		return ret;
	}
}
