// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import com.spoiledmilk.cykelsuperstier.LeftMenu;

public class MapActivity extends com.spoiledmilk.ibikecph.map.MapActivity {

    /*
    // TODO: Refactor this into the super MapActivity and keep the actual overlays a part of CykelPlanen
    private void plotOverlays() {
        Log.d("JC", "MapActivity.plotOverlays");

        // Remove all overlays first.
        this.mapView.removeAllMarkersOfType(MarkerType.OVERLAY);

        // Plot the service stations.
        if (CykelPlanenApplication.getSettings().getOverlay(OverlayType.SERVICE)) {
            // Initialize the overlays
            ArrayList<ServiceStationMarker> serviceStations = ServiceStationMarker.getServiceStationMarkersFromJSON();

            for (IBCMarker m : serviceStations) {
                this.mapView.addMarker(m);
            }
        }

        // Plot the Supercykelsti
        if (CykelPlanenApplication.getSettings().getOverlay(OverlayType.PATH)) {
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
    */

    @Override
    protected com.spoiledmilk.ibikecph.LeftMenu createLeftMenu() {
        return new LeftMenu();
    }

}
