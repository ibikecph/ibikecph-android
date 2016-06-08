package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;

import com.mapbox.mapboxsdk.overlay.Overlay;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class can create any togglable overlay
 * Created by kraen on 21-05-16.
 */
public class TogglableOverlayFactory {

    protected static TogglableOverlayFactory ourInstance = new TogglableOverlayFactory();
    protected IBikeApplication application;

    /**
     * Get the singleton instance.
     * @param application
     * @return
     */
    public static TogglableOverlayFactory getInstance(IBikeApplication application) {
        ourInstance.setApplication(application);
        return ourInstance;
    }

    /**
     * Get the singleton instance.
     * @return
     */
    public static TogglableOverlayFactory getInstance() {
        if(ourInstance.getApplication() == null) {
            throw new RuntimeException("Must call the getInstance method with a application once");
        }
        return ourInstance;
    }

    protected List<TogglableOverlay> overlays = new ArrayList<>();

    protected IBikePreferences preferences;

    boolean overlaysLoaded = false;

    public void setApplication(IBikeApplication application) {
        this.application = application;
        overlays.clear();
        for(Class<? extends TogglableOverlay> overlayClass: application.getTogglableOverlayClasses()) {
            try {
                overlays.add(overlayClass.newInstance());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load a togglable overlay", e);
            }
        }
        this.preferences = application.getSettings();
    }

    public IBikeApplication getApplication() {
        return application;
    }

    public interface OnOverlaysLoadedListener {
        void onOverlaysLoaded(List<TogglableOverlay> togglableOverlays);
    }

    protected List<OnOverlaysLoadedListener> overlaysLoadedListeners = new ArrayList<>();

    private TogglableOverlayFactory() { }

    /**
     * Loads all overlays from the local file system, or downloads them from the remote server.
     * Call this on a different thread than the UI thread.
     * Not forcing is default.
     * @throws IOException
     */
    public void loadOverlays() throws IOException {
        loadOverlays(true);
    }

    /**
     * Loads all overlays from the local file system, or downloads them from the remote server.
     * Call this on a different thread than the UI thread.
     * @param forced Always wipe the local copy before downloading.
     * @throws IOException
     */
    public void loadOverlays(boolean forced) throws IOException {
        for(TogglableOverlay togglableOverlay: overlays) {
            // If the togglable overlay is actually a downloaded overlay
            if(togglableOverlay instanceof DownloadedOverlay) {
                DownloadedOverlay downloadedOverlay = (DownloadedOverlay) togglableOverlay;
                downloadedOverlay.load(application, forced);
                // Make sure the overlays adhere to the selection when just initialized
                boolean selected = isSelected(downloadedOverlay);
                for(Overlay overlay: downloadedOverlay.getOverlays()) {
                    overlay.setEnabled(selected);
                }
            }
        }
    }

    /**
     * Get a list of all the overlays that are togglable by the user.
     * @return
     */
    public List<TogglableOverlay> getTogglableOverlays() {
        List<TogglableOverlay> result = new ArrayList<>();
        for(TogglableOverlay overlay: overlays) {
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
