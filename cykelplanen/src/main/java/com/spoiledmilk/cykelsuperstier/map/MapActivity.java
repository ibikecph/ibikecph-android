// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import android.os.Bundle;

public class MapActivity extends com.spoiledmilk.ibikecph.map.MapActivity {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        this.mapView.setZoom(10.0f);
	}

    @Override
    public void onStart() {
        super.onStart();
    }

	@Override
	public void onResume() {
		super.onResume();
	}

}
