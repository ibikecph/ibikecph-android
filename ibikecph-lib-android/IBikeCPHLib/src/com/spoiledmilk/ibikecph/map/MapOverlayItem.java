// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class MapOverlayItem extends OverlayItem {

	public MapOverlayItem(String aUid, String aTitle, String aDescription, GeoPoint aGeoPoint, Drawable aMarker, HotspotPlace aHotspotPlace) {
		super(aUid, aTitle, aDescription, aGeoPoint);
		this.setMarker(aMarker);
		this.setMarkerHotspot(aHotspotPlace);
	}

	public void draw(Canvas canvas) {
		//
	}
}
