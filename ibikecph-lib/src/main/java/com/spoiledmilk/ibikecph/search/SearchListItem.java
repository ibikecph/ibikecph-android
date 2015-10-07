// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import java.util.LinkedList;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * An abstract class outlying the foundation of a list item in the search
 * box in the apps.
 * 
 * TODO: Figure out what the different properties mean. Name/Street/Address.
 * @author jens
 *
 */
public abstract class SearchListItem {

	public enum nodeType {
		CURRENT_POSITION, FAVORITE, HISTORY, KORTFOR, FOURSQUARE
	};

	public static final int POINTS_EXACT_NAME = 20;
	public static final int POINTS_EXACT_ADDRESS = 10;
	public static final int POINTS_PART_NAME = 1;
	public static final int POINTS_PART_ADDRESS = 1;
	public static final int MINIMUM_PASS_LENGTH = 3;

	protected JsonNode jsonNode;

	public nodeType getType() {
		return type;
	}

	protected nodeType type;
	protected double latitude = -1, longitude = -1;
	private int relevance = 0;
	protected double distance = 0;
	protected String number = "";

	public SearchListItem(JsonNode jsonNode, nodeType type) {
		this.jsonNode = jsonNode;
		this.type = type;
	}

	public SearchListItem(nodeType type) {
		this.jsonNode = null;
		this.type = type;
	}

	public abstract String getName();

	public abstract String getAdress();

	public abstract String getStreet();

	public abstract int getOrder();

	public abstract String getZip();

	public abstract String getCity();

	public abstract String getCountry();

	public abstract String getSource();

	public abstract String getSubSource();

	public abstract int getIconResourceId();

	public int getRelevance() {
		return relevance;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getGeocodeUrl() {
		return "";
	}

	public static int pointsForName(String name, String address, String srchString) {
		LinkedList<String> terms = new LinkedList<String>();

		String srchStringSplit[] = srchString.split(" ");

		for (String str : srchStringSplit) {
			if (!terms.contains(str))
				terms.add(str);
		}

		int total = 0;

		int points = Util.numberOfOccurenciesOfString(name, srchString);

		if (points > 0) {
			total += points * POINTS_EXACT_NAME;
		} else {
			for (String str : terms) {
				points = Util.numberOfOccurenciesOfString(name, str);
				if (points > 0) {
					total += points * POINTS_PART_NAME;
				}
			}
		}

		points = Util.numberOfOccurenciesOfString(address, srchString);

		if (points > 0) {
			total += points * POINTS_EXACT_ADDRESS;
		} else {
			for (String str : terms) {
				points = Util.numberOfOccurenciesOfString(address, str);
				if (points > 0) {
					total += points * POINTS_PART_NAME;
				}
			}
		}

		return total;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public SearchListItem setRelevance(String srchString) {
		relevance = 0;
		if (getName().toLowerCase(Locale.US).contains(srchString.toLowerCase(Locale.US)))
			relevance += 20;
		if (getAdress().toLowerCase(Locale.US).contains(srchString.toLowerCase(Locale.US)))
			relevance += 10;
		for (String s : srchString.split("[\\p{P} \\t\\n\\r]")) {
			if (s.toLowerCase(Locale.US).contains(srchString.toLowerCase(Locale.US)))
				relevance++;
		}
		return this;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double d) {
		this.distance = d;
	}

	public abstract String getFormattedNameForSearch();

	public abstract String getOneLineName();

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public JsonNode getJsonNode() {
		return jsonNode;
	}

	public static SearchListItem instantiate(JsonNode node) {
		SearchListItem ret = null;
		if (node.has("location")) {
			ret = new FoursquareData(node);
		} else if (node.has("properties")) {
			ret = new KortforData(node);
		}
		return ret;
	}

}
