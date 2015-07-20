package com.spoiledmilk.cykelsuperstier.map;

import android.graphics.Color;
import com.mapbox.mapboxsdk.overlay.PathOverlay;

/**
 * Created by jens on 7/20/15.
 */
public class SupercykelstiPathOverlay extends PathOverlay {

    public SupercykelstiPathOverlay() {
        // TODO: Find a suitable color for this one
        super(Color.argb(0.5, 255, 102, 0), 5);

    }

}
