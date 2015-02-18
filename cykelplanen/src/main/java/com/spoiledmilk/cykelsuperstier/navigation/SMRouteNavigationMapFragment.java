// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.navigation;

import org.osmdroid.views.overlay.PathOverlay;

import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Bundle;

public class SMRouteNavigationMapFragment extends com.spoiledmilk.ibikecph.navigation.SMRouteNavigationMapFragment {

	public int overlaysShown = 0;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		pathOverlay2 = new PathOverlay(getRouteColor(), getActivity());

		if ((overlaysShown & 1) > 0)
			overlaysManager.drawBikeRoutes(getActivity());
		if ((overlaysShown & 2) > 0)
			overlaysManager.drawServiceStations(getActivity());
		if ((overlaysShown & 4) > 0)
			overlaysManager.drawsTrainStations(getActivity());
		if ((overlaysShown & 8) > 0)
			overlaysManager.drawMetroStations(getActivity());
		if ((overlaysShown & 16) > 0)
			overlaysManager.drawlocalTrainStations(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	protected int getRouteColor() {
		return Color.rgb(6, 59, 104);
	}

}
