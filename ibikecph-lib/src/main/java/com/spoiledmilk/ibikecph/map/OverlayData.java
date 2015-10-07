// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.location.Location;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Model class for overlay icons on the map view.
 * @author jens
 *
 */
public class OverlayData implements Comparable<OverlayData> {
	public String name = "";
	public String line = "";
	public double lattitude;
	public double longitude;
	public Location loc;
	public int iconResource;
	//public ItemizedIconOverlay<OverlayItem> markerOverlay;
	public double bikeDistance = Double.MAX_VALUE;
	public ArrayList<OverlayData> BStations;

	public OverlayData(String name, String line, double lattitude, double longitude, int iconResource) {
		this.name = name;
		this.line = line;
		this.lattitude = lattitude;
		this.longitude = longitude;
		this.iconResource = iconResource;
		loc = Util.locationFromCoordinates(lattitude, longitude);
	}

	@Override
	public OverlayData clone() {
		OverlayData ret = new OverlayData(name, line, lattitude, longitude, iconResource);
		return ret;
	}

	public String toString() {
		return name;
	}

	@Override
	public int compareTo(OverlayData arg) {
		return (int) (bikeDistance - arg.bikeDistance);
	}

	public ArrayList<String> getBStationsString() {
		ArrayList<String> ret = new ArrayList<String>();
		if (BStations != null) {
			Iterator<OverlayData> it = BStations.iterator();
			while (it.hasNext())
				ret.add(it.next().toString());
		}
		return ret;
	}
}
