// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import android.os.Bundle;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.cykelsuperstier.LeftMenu;

public class MapActivity extends com.spoiledmilk.ibikecph.map.MapActivity {

    LeftMenu leftMenu;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.mapView.setCenter(new LatLng(55.74, 12.424));
        super.mapView.setZoom(11.3f);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

	@Override
	public void onResume() {
		super.onResume();
	}

    @Override
    protected LeftMenu getLeftMenu() {
        if (leftMenu == null) {
            return leftMenu = new LeftMenu();
        } else {
            return leftMenu;
        }
    }
}
