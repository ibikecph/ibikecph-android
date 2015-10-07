// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.location.Location;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;

/**
 * The "Current Position" item in the search list.
 * @author jens
 *
 */
public class CurrentLocation extends SearchListItem {

	public CurrentLocation() {
		super(nodeType.CURRENT_POSITION);
	}

	public double getLatitude() {
		Location start = SMLocationManager.getInstance().getLastKnownLocation();
		latitude = -1;
		if (start != null) {
			latitude = start.getLatitude();
		}
		return latitude;
	}

	public double getLongitude() {
		Location start = SMLocationManager.getInstance().getLastKnownLocation();
		longitude = -1;
		if (start != null) {
			longitude = start.getLongitude();
		}
		return longitude;
	}

	@Override
	public String getName() {
		return IbikeApplication.getString("current_position");
	}

	@Override
	public String getAdress() {
		String ret = "";
		try {
			ret = jsonNode.get("vejnavn").get("navn").asText() + " "
					+ jsonNode.get("husnr").asText() + ", "
					+ jsonNode.get("postnummer").get("nr").asText() + " "
					+ jsonNode.get("kommune").get("navn").asText()
					+ ", Danmark";
		} catch (Exception e) {

		}
		return ret;
	}

	@Override
	public String getStreet() {
		return "";
	}

	@Override
	public int getOrder() {
		return -1;
	}

	@Override
	public String getZip() {
		return "";
	}

	@Override
	public String getCity() {
		return "";
	}

	@Override
	public String getCountry() {
		return "";
	}

	@Override
	public String getSource() {
		return "";
	}

	@Override
	public String getSubSource() {
		return "";
	}

	@Override
	public int getIconResourceId() {
		return -1;
	}

	@Override
	public String getFormattedNameForSearch() {
		return getName();
	}

	@Override
	public String getOneLineName() {
		return getName();
	}

}
