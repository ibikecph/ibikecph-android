// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import com.fasterxml.jackson.databind.JsonNode;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dk.kk.ibikecphlib.util.Util;

public class HTTPAutocompleteHandler {

	static final double GEOCODING_SEARCH_RADIUS = 50000.0;
	static final String PLACES_SEARCH_RADIUS = "20000";
	static final String PLACES_LANGUAGE = "da";

	@SuppressLint("NewApi")

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
			// TODO: Get this from Config
			urlString = "https://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=";

			if (
					(address.hasHouseNumber()) &&
					(address.hasZip()) &&
					(address.getCity() == null || address.getCity().equals("") || address.getCity().equals(address.getStreet()))) {
				urlString += "vej"; // street search
			} else {
				urlString += "adresse"; // address search
			}

            // TODO: Removed a wildcard in the beginning of the search query.
			urlString += "&vejnavn=" + URLEncoder.encode(address.getStreet(), "UTF-8") + "*";
			// urlString = "https://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=adresse&vejnavn=*"
			// + URLEncoder.encode(address.street, "UTF-8") + "*";

			if (address.hasHouseNumber()) {
				urlString += "&husnr=" + address.getHouseNumber() + "*";
			}

			urlString += "&geop=" + Util.limitDecimalPlaces(currentLocation.getLongitude(), 6) + "" + ","
					+ Util.limitDecimalPlaces(currentLocation.getLatitude(), 6) + ""
					+ "&georef=EPSG:4326&outgeoref=EPSG:4326&login=ibikecph&password=Spoiledmilk123&hits=10";

			if (address.hasZip()) {
				urlString = urlString + "&postnr=" + address.getZip();
			}
			if (address.hasCity() && !address.getCity().equals(address.getStreet())) {
				// urlString = urlString + "&by=" + URLEncoder.encode(address.city.trim(), "UTF-8") + "*";
				urlString = urlString + "&postdist=*" + URLEncoder.encode(address.getCity().trim(), "UTF-8") + "*";
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
		} catch (UnsupportedEncodingException e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}
		return list;
	}

	public static List<JsonNode> getKortforsyningenPlaces(Location currentLocation, Address address) {
		String urlString;
		List<JsonNode> list = new ArrayList<JsonNode>();
		if (!address.hasCity()) {
			// TODO: Seriously
			address.setCity(address.getStreet());
		}
		try {
            // TODO: Removed a wildcard
			String stednavn = URLEncoder.encode(address.getStreet(), "UTF-8") + "*";
			urlString = "https://kortforsyningen.kms.dk/?servicename=RestGeokeys_v2&method=stedv2&stednavn="
					+ stednavn + "&geop=" + ""
					+ Util.limitDecimalPlaces(currentLocation.getLongitude(), 6) + "," + ""
					+ Util.limitDecimalPlaces(currentLocation.getLatitude(), 6)
					+ "&georef=EPSG:4326&outgeoref=EPSG:4326&login=ibikecph&password=Spoiledmilk123&hits=10";

			// &distinct=true gave an error from the API
			// DB_SQL_PROBLEM. Error executing SQL: ERROR: syntax error at or near "stednavn" Position: 453

			JsonNode rootNode = performGET(urlString);
			if (rootNode.has("features")) {
				JsonNode features = rootNode.get("features");
				for (JsonNode featureNode: features) {
					if (featureNode.has("properties") &&
						featureNode.get("properties").has("stednavneliste") &&
						featureNode.get("properties").size() > 0)
						list.add(featureNode);
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
		if(s == null) {
			return "";
		} else {
			return s.replaceAll("ø|Ø", "o").replaceAll("å|Å", "a").replaceAll("æ|Æ", "ae");
		}
	}
}
