package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;

import com.mapbox.mapboxsdk.overlay.Overlay;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class can create any selectable overlay
 * Created by kraen on 21-05-16.
 */
public class SelectableOverlayFactory {

    private static SelectableOverlayFactory ourInstance = new SelectableOverlayFactory();

    public static SelectableOverlayFactory getInstance() {
        return ourInstance;
    }

    protected List<DownloadedOverlay> downloadedOverlays = new ArrayList<>();

    protected IBikePreferences preferences;

    boolean overlaysLoaded = false;

    public interface OnOverlaysLoadedListener {
        void onOverlaysLoaded(List<SelectableOverlay> selectableOverlays);
    }

    protected List<OnOverlaysLoadedListener> overlaysLoadedListeners = new ArrayList<>();

    private SelectableOverlayFactory() {
        // Add new downloaded overlays to this list
        downloadedOverlays.add(new GreenPathsOverlay());
        downloadedOverlays.add(new HarborRingOverlay());
    }

    /**
     * Sets the preferences on the overlay factory, to be used when selecting or deselecting the
     * various overlays.
     * @param preferences The applications preferences object.
     */
    public void setPreferences(IBikePreferences preferences) {
        this.preferences = preferences;
    }

    public void loadOverlays(Context context) throws IOException {
        for(DownloadedOverlay downloadedOverlay: downloadedOverlays) {
            downloadedOverlay.load(context);
            // Make sure the overlays adhere to the selection when just initialized
            boolean selected = isSelected(downloadedOverlay);
            for(Overlay overlay: downloadedOverlay.getOverlays()) {
                overlay.setEnabled(selected);
            }
        }
    }

    public List<SelectableOverlay> getSelectableOverlays() {
        List<SelectableOverlay> result = new ArrayList<>();
        result.addAll(downloadedOverlays);
        // Add any other selectable overlays
        return result;
    }

    /**
     * Checks if an overlay is selected for drawing on the map or not.
     * @param overlay The overlay to check
     * @return if true, the overlay is selected and should be drawn.
     */
    public boolean isSelected(SelectableOverlay overlay) {
        return preferences.getOverlay(overlay);
    }

    /**
     * Sets if an overlay is selected and should be visible on the map.
     * TODO: Consider toggling the actual overlays if enabled instead of using the observer pattern.
     * @param selectableOverlay The overlay that should have its selection updated.
     * @param selected if true, the overlay should be drawn.
     */
    public void setSelected(SelectableOverlay selectableOverlay, boolean selected) {
        preferences.setOverlay(selectableOverlay, selected);
        for(Overlay overlay: selectableOverlay.getOverlays()) {
            overlay.setEnabled(selected);
        }
    }

    /**
     * Adds a listner, that will get notified when all the overlays has loaded.
     * @param listener
     */
    public void addOnOverlaysLoadedListener(OnOverlaysLoadedListener listener) {
        overlaysLoadedListeners.add(listener);
        // If the loaded event was already fired - notify directly
        if(overlaysLoaded) {
            listener.onOverlaysLoaded(getSelectableOverlays());
        }
    }

    /**
     * Removes a listner, that will get notified when all the overlays has loaded.
     * @param listener
     */
    public void removeOnOverlaysLoadedListener(OnOverlaysLoadedListener listener) {
        overlaysLoadedListeners.remove(listener);
    }

    /**
     * * Notified all the listners that all the overlays has loaded.
     * @param selectableOverlays
     */
    public void notifyOnOverlaysLoadedListeners(List<SelectableOverlay> selectableOverlays) {
        overlaysLoaded = true;
        for(OnOverlaysLoadedListener listener: overlaysLoadedListeners) {
            listener.onOverlaysLoaded(selectableOverlays);
        }
    }
}
