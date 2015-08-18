// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import android.os.Bundle;
import android.util.Log;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.LeftMenu;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.IBCMarker;
import com.spoiledmilk.ibikecph.map.MarkerType;
import com.spoiledmilk.ibikecph.map.OverlayType;

import java.util.ArrayList;

public class MapActivity extends com.spoiledmilk.ibikecph.map.MapActivity {
    LeftMenu leftMenu;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!IbikeApplication.getService().hasValidLocation()) {
            super.mapView.setCenter(new LatLng(55.74, 12.424));
            super.mapView.setZoom(11.3f);
        }

        plotOverlays();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

	@Override
	public void onResume() {
		super.onResume();

        plotOverlays();
	}

    private void plotOverlays() {
        Log.d("JC", "MapActivity.plotOverlays");

        // Remove all overlays first.
        this.mapView.removeAllMarkersOfType(MarkerType.OVERLAY);

        // Plot the service stations.
        if (CykelsuperstierApplication.getSettings().getOverlay(OverlayType.SERVICE)) {
            // Initialize the overlays
            ArrayList<ServiceStationMarker> serviceStations = ServiceStationMarker.getServiceStationMarkersFromJSON();

            for (IBCMarker m : serviceStations) {
                this.mapView.addMarker(m);
            }
        }

        // Plot the Supercykelsti
        if (CykelsuperstierApplication.getSettings().getOverlay(OverlayType.PATH)) {
            ArrayList<SupercykelstiPathOverlay> paths = SupercykelstiPathOverlay.getSupercykelstiPathsFromJSON();

            for (SupercykelstiPathOverlay path : paths) {
                Log.d("JC", "Adding SuperCykelSti");
                this.mapView.getOverlays().add(path);
            }
        } else {
            for (Overlay o : this.mapView.getOverlays()) {
                if (o instanceof SupercykelstiPathOverlay) {
                    this.mapView.getOverlays().remove(o);
                }
            }
        }
        this.mapView.invalidate();
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
