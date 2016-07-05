package com.spoiledmilk.ibikecph.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.handlers.IBCMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is the main class for maps in the I Bike CPH apps. It extends MapView from Mapbox but uses the tiles from the
 * City of Copenhagen tileservers. It serves some different purposes, including
 * * Showing a regular map of the city
 * * Helping in navigation
 * * Showing past tracks
 * These different contexts have different demands for the map, so to accomodate this without cluttering it all together
 * in the same class, we implement some different implementations of the MapViewListener, that are (re-)assigned on
 * every state change of the map.
 */
public class IBCMapView extends MapView {

    public static IBCMarker currentAddressMarker;
    private CopyOnWriteArrayList<IBCMarker> markers = new CopyOnWriteArrayList<IBCMarker>();

    protected List<Overlay> routeOverlays = new ArrayList<>();


    private MapViewListener currentListener;
    private BaseMapActivity parentActivity;

    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase tileProvider, Handler tileRequestCompleteHandler, AttributeSet attrs) {
        super(aContext, tileSizePixels, tileProvider, tileRequestCompleteHandler, attrs);
    }

    public IBCMapView(Context aContext) {
        super(aContext);
    }

    public IBCMapView(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
    }

    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase aTileProvider) {
        super(aContext, tileSizePixels, aTileProvider);
    }

    @Override
    public void setMapViewListener(MapViewListener listener) {
        if(!(listener instanceof IBCMapHandler)) {
            String msg = String.format("The listener of an %s must be a %s, got %s",
                                       IBCMapView.class.getSimpleName(),
                                       IBCMapHandler.class.getSimpleName(),
                                       listener.getClass().getSimpleName());
            throw new IllegalArgumentException(msg);
        }
        super.setMapViewListener(listener);
        currentListener = listener;
    }

    public void setMapViewListener(Class<? extends IBCMapHandler> mapHandlerClass) {
        try {
            IBCMapHandler mapHandler = mapHandlerClass.getConstructor(IBCMapView.class).newInstance(this);
            setMapViewListener(mapHandler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Do some initializations that are always needed
     */
    public void init(BaseMapActivity parent) {
        WebSourceTileLayer ws = new WebSourceTileLayer("ibikecph", "https://tiles.ibikecph.dk/tiles/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(17);
        this.parentActivity = parent;

        this.setTileSource(ws);
        this.setCenter(new LatLng(Util.COPENHAGEN));
        this.setZoom(17);
        this.setMaxZoomLevel(19);
    }

    /**
     * Starts routing. This is a two-stage process, in which we first show the route to the user. Then they press "Go"
     * and we zoom to the first instruction.
     *
     * @param route
     */
    public void showRoute(SMRoute route) {
        if(currentListener instanceof NavigationMapHandler) {
            ((NavigationMapHandler) getMapHandler()).showRouteOverview(route);
        } else {
            throw new RuntimeException("Cannot show route with the current listner.");
        }
    }

    public void showMultipleRoutes() {
        if(currentListener instanceof NavigationMapHandler) {
            ((NavigationMapHandler) getMapHandler()).showRouteOverviewPieces(0);
        } else {
            throw new RuntimeException("Cannot show route with the current listner.");
        }
    }

    public IBCMapHandler getMapHandler() {
        // We can always cast this to an IBCMapHandler as it's checked to be one when setting it.
        return (IBCMapHandler) currentListener;
    }

    public BaseMapActivity getParentActivity() {
        return parentActivity;
    }

    @Override
    public MapView setUserLocationEnabled(boolean value) {
        MapView result = super.setUserLocationEnabled(value);
        if (value) {
            getUserLocationOverlay().setDrawAccuracyEnabled(true);

            Bitmap person = BitmapFactory.decodeResource(this.getResources(), R.drawable.tracking_dot);
            getUserLocationOverlay().setPersonBitmap(person);
        }
        return result;
    }

    /**
     * Removed the stored GPS overlay from the map view.
     */
    public void removeUserLocationOverlay() {
        setUserLocationEnabled(false);
    }

    public void showAddress(Address a) {
        removeAddressMarker();

        IBCMarker m = new IBCMarker(a.getStreetAddress(), a.getPostCodeAndCity(), a.getLocation(), MarkerType.ADDRESS);
        Icon markerIcon = new Icon(this.getResources().getDrawable(R.drawable.marker));
        m.setIcon(markerIcon);

        this.currentAddressMarker = m;

        this.addMarker(m);

        // Invalidate the view so the marker gets drawn.
        this.invalidate();
    }

    @Override
    public MapView setCenter(ILatLng aCenter, boolean userAction) {
        MapView result = super.setCenter(aCenter, userAction);
        // Make sure we are no longer following the user when we explicitly set the center.
        // TODO: Consider implementing this using a listener pattern.
        setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.NONE);
        return result;
    }

    @Override
    public MapView setUserLocationTrackingMode(UserLocationOverlay.TrackingMode mode) {
        MapView result = super.setUserLocationTrackingMode(mode);
        // TODO: Consider implementing this using a listener pattern.
        if (getParentActivity() instanceof MapActivity) {
            MapActivity activity = (MapActivity) getParentActivity();
            Log.d("MapView", "Set center was called - calling updateCompassIcon");
            activity.updateCompassIcon();
        }
        return result;
    }

    @Override
    public void selectMarker(Marker marker) {
        //curHandler.onTapMarker(this, marker);
        return;
    }

    public void removeAddressMarker() {
        if (currentAddressMarker != null) {
            this.getOverlays().remove(currentAddressMarker);
            this.removeMarker(currentAddressMarker);
            currentAddressMarker = null;
        }
    }

    public IBCMarker addMarker(IBCMarker m) {
        super.addMarker(m);
        markers.add(m);
        return m;
    }

    public void removeMarker(IBCMarker m) {
        markers.remove(m);
        super.removeMarker(m);
    }

    public void removeAllMarkers() {
        for (IBCMarker m : markers) {
            this.removeMarker(m);
        }
    }

    /**
     * Adds a PathOverlay representing a route to the map.
     * @param path
     */
    public void addRouteOverlay(PathOverlay path) {
        routeOverlays.add(path);
        addOverlay(path);
    }

    /**
     * Adds a PathOverlay representing a route to the map.
     */
    public void removeAllRouteOverlays() {
        // Remove all route overlays from the map and from the list of overlays.
        getOverlays().removeAll(routeOverlays);
        routeOverlays.removeAll(routeOverlays);
    }

}
