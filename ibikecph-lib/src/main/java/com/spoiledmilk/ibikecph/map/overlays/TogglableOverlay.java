package com.spoiledmilk.ibikecph.map.overlays;

import android.graphics.Color;
import android.graphics.Paint;

import com.mapbox.mapboxsdk.overlay.Overlay;

import java.util.List;

/**
 * Any overlay that can be selected from the menu in the OverlayActivity must implement this
 * interface.
 * Created by kraen on 21-05-16.
 */
public interface TogglableOverlay {

    /**
     * The name of the overlay, used when presented in the user interface.
     * @return
     */
    String getName();

    /**
     * The paint used when presenting the overlay in the user interface.
     * @return
     */
    Paint getPaint();

    /**
     * Getter for all the actual MapBox overlays that this togglable overlay is composed of.
     * @return
     */
    List<Overlay> getOverlays();

    /**
     * Has the user selected this overlay?
     * @return
     */
    boolean isSelected();

    /**
     * Call this with selected = true, when the user selects this in the UI.
     * @param selected
     */
    void setSelected(boolean selected);
}
