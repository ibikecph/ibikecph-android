// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.location.Location;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;

/**
 * The "Current Position" item in the search list.
 * @author jens
 *
 */
public class CurrentLocationListItem extends SearchListItem {

	public CurrentLocationListItem() {
		super(nodeType.CURRENT_POSITION);
	}

	@Override
	public int getOrder() {
		return -1;
	}

	@Override
	public String getSubSource() {
		return null;
	}

	@Override
	public int getIconResourceId() {
		return -1;
	}

}
