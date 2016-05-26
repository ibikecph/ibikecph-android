package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;

import com.mapbox.mapboxsdk.overlay.Overlay;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class can create any togglable overlay
 * Created by kraen on 21-05-16.
 */
public class TogglableOverlayFactory {

    private static TogglableOverlayFactory ourInstance = new TogglableOverlayFactory();

    /**
     * Get the singleton instance.
     * @return
     */
    public static TogglableOverlayFactory getInstance() {
        return ourInstance;
    }

    protected List<DownloadedOverlay> downloadedOverlays = new ArrayList<>();

    protected IBikePreferences preferences;

    boolean overlaysLoaded = false;

    public interface OnOverlaysLoadedListener {
        void onOverlaysLoaded(List<TogglableOverlay> togglableOverlays);
    }

    protected List<OnOverlaysLoadedListener> overlaysLoadedListeners = new ArrayList<>();

    private TogglableOverlayFactory() {
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

    /**
     * Loads all overlays from the local file system, or downloads them from the remote server.
     * Call this on a different thread than the UI thread.
     * Not forcing is default.
     * @param context Used to access local files
     * @throws IOException
     */
    public void loadOverlays(Context context) throws IOException {
        loadOverlays(context, true);
    }

    /**
     * Loads all overlays from the local file system, or downloads them from the remote server.
     * Call this on a different thread than the UI thread.
     * @param context Used to access local files
     * @param forced Always wipe the local copy before downloading.
     * @throws IOException
     */
    public void loadOverlays(Context context, boolean forced) throws IOException {
        for(DownloadedOverlay downloadedOverlay: downloadedOverlays) {
            downloadedOverlay.load(context, forced);
            // Make sure the overlays adhere to the selection when just initialized
            boolean selected = isSelected(downloadedOverlay);
            for(Overlay overlay: downloadedOverlay.getOverlays()) {
                overlay.setEnabled(selected);
            }
        }
    }

    /**
     * Get a list of all the overlays that are togglable by the user.
     * @return
     */
    public List<TogglableOverlay> getTogglableOverlays() {
        List<TogglableOverlay> result = new ArrayList<>();
        for(DownloadedOverlay overlay: downloadedOverlays) {
            if(overlay.getOverlays().size() > 0) {
                // If it actually has overlays
                result.add(overlay);
            }
        }
        // Add any other selectable overlays
        return result;
    }

    /**
     * Checks if an overlay is selected for drawing on the map or not.
     * @param overlay The overlay to check
     * @return if true, the overlay is selected and should be drawn.
     */
    public boolean isSelected(TogglableOverlay overlay) {
        return preferences.getOverlay(overlay);
    }

    /**
     * Sets if an overlay is selected and should be visible on the map.
     * @param togglableOverlay The overlay that should have its selection updated.
     * @param selected if true, the overlay should be drawn.
     */
    public void setSelected(TogglableOverlay togglableOverlay, boolean selected) {
        preferences.setOverlay(togglableOverlay, selected);
        for(Overlay overlay: togglableOverlay.getOverlays()) {
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
            listener.onOverlaysLoaded(getTogglableOverlays());
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
     * * Notified all the listeners that all the overlays has loaded.
     * @param togglableOverlays
     */
    public void notifyOnOverlaysLoadedListeners(List<TogglableOverlay> togglableOverlays) {
        overlaysLoaded = true;
        for(OnOverlaysLoadedListener listener: overlaysLoadedListeners) {
            listener.onOverlaysLoaded(togglableOverlays);
        }
    }
}
