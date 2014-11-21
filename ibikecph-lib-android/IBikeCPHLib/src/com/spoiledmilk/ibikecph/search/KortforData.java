// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.R;

public class KortforData extends SearchListItem {

	private String name;
	private String street;
	private String zipCode;
	private int iconRes;
	private boolean isPlace;
	private String city;
	private boolean hasCoordinates = false;

	public KortforData(JsonNode jsonNode) {
		super(jsonNode, nodeType.KORTFOR);

		String streetName = "";
		if (jsonNode.get("properties").has("vej_navn")) {
			streetName = jsonNode.get("properties").get("vej_navn").asText();
		} else if (jsonNode.get("properties").has("navn")) {
			streetName = jsonNode.get("properties").get("navn").asText();
		}
		if (jsonNode.get("properties").has("husnr")) {
			number = jsonNode.get("properties").get("husnr").asText();
		}
		String municipalityName = "";
		String municipalityCode = "";
		if (jsonNode.get("properties").has("postdistrikt_navn")) {
			municipalityName = jsonNode.get("properties").get("postdistrikt_navn").asText();
		} else if (jsonNode.get("properties").has("kommune_navn")) {
			municipalityName = jsonNode.get("properties").get("kommune_navn").asText();
		}
		if (jsonNode.get("properties").has("postdistrikt_kode")) {
			municipalityCode = jsonNode.get("properties").get("postdistrikt_kode").asText();

		} 
		// else if (jsonNode.get("properties").has("region_kode")) {// "sogn_kode"
		// municipalityCode = jsonNode.get("properties").get("region_kode").asText();
		// }
		street = streetName;
		city = municipalityName;
		name = streetName;
		zipCode = municipalityCode;
		distance = 0;
		if (jsonNode.get("properties").has("afstand_afstand")) {
			distance = jsonNode.get("properties").get("afstand_afstand").asDouble();
		}

		if (jsonNode.has("geometry") && !jsonNode.get("geometry").isNull() && jsonNode.get("geometry").has("ymin")) {
			Log.d("", "geometry = " + jsonNode.get("geometry"));
			latitude = jsonNode.get("geometry").get("ymin").asDouble();
			if (jsonNode.get("geometry").has("ymax")) {
				latitude += jsonNode.get("geometry").get("ymax").asDouble();
				latitude /= 2;
			}
			longitude = jsonNode.get("geometry").get("xmin").asDouble();
			if (jsonNode.get("geometry").has("xmax")) {
				longitude += jsonNode.get("geometry").get("xmax").asDouble();
				longitude /= 2;
			}
			hasCoordinates = true;
		}

		else if (jsonNode.has("geometry") && jsonNode.get("geometry").has("coordinates")
				&& jsonNode.get("geometry").get("coordinates").size() > 1) {
			hasCoordinates = true;
			latitude = jsonNode.get("geometry").get("coordinates").get(1).asDouble();
			longitude = jsonNode.get("geometry").get("coordinates").get(0).asDouble();
		}

		if (jsonNode.has("bbox") && jsonNode.get("bbox").size() > 3) {
			hasCoordinates = true;
			latitude = (jsonNode.get("bbox").get(1).asDouble() + jsonNode.get("bbox").get(3).asDouble()) / 2;
			longitude = (jsonNode.get("bbox").get(0).asDouble() + jsonNode.get("bbox").get(2).asDouble()) / 2;
		}

		if (jsonNode.get("properties").has("kategori")) {
			// it's a place
			isPlace = true;
			iconRes = R.drawable.search_location_icon;

		} else {
			isPlace = false;
			iconRes = R.drawable.search_magnify_icon;
		}
	}

	public KortforData(String street, String num) {
		super(null, nodeType.KORTFOR);
		this.street = street;
		this.name = street;// + " " + num;
	}

	public boolean isPlace() {
		return isPlace;
	}

	public String getName() {
		return name;
	}

	public String getAdress() {
		return name;
	}

	public String getStreet() {
		return street;
	}

	public int getOrder() {
		return 2;
	}

	public String getZip() {
		return zipCode;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return "";
	}

	public String getSource() {
		String ret = "";
		ret = "autocomplete";
		return ret;
	}

	public String getSubSource() {
		String ret = "";
		ret = "oiorest";
		return ret;
	}

	public int getRelevance(String srchString) {
		int ret = -1;
		String tmp = jsonNode.get("vejnavn").get("navn").asText() + " " + jsonNode.get("husnr").asText() + ", "
				+ jsonNode.get("postnummer").get("nr").asText() + " " + jsonNode.get("kommune").get("navn").asText() + ", Danmark";
		ret = pointsForName(tmp, tmp, srchString);
		return ret;
	}

	@Override
	public int getIconResourceId() {
		return iconRes;
	}

	@Override
	public String getFormattedNameForSearch() {
		String ret = name;
		if (getZip() != null && !getZip().equals("")) {
			ret += "\n" + getZip();
		}
		if (getCity() != null && !getCity().equals("")) {
			ret += " " + getCity();
		}
		return ret;
	}

	@Override
	public String getOneLineName() {
		String ret = name;
		if (getZip() != null && !getZip().equals("")) {
			ret += ", " + getZip();
		}
		if (getCity() != null && !getCity().equals("")) {
			ret += " " + getCity();
		}
		return ret;
	}

	@Override
	public String toString() {
		return street + ", " + getZip() + " " + city;
	}

	public boolean hasCoordinates() {
		return hasCoordinates;
	}

	public void setHasCoordinates(boolean hasCoordinates) {
		this.hasCoordinates = hasCoordinates;
	}

}
