package dk.kk.ibikecphlib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.map.handlers.IBCMapHandler;
import dk.kk.ibikecphlib.map.overlays.RouteOverlay;
import dk.kk.ibikecphlib.map.states.NavigatingState;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.util.Util;

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

    public static IBCMarker destinationPreviewMarker;

    protected RouteOverlay routeOverlay;

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
     * Show a journey that consists of one or more routes.
     * @param state
     */
    public void showRoute(Route route) {
        clearRouteOverlay();
        routeOverlay = new RouteOverlay(this, route);
        addOverlay(routeOverlay);
    }

    /**
     * Zoom the map to a route, with a default 20% padding
     * @param route the route to be zoomed to
     */
    public void zoomToRoute(Route route) {
        zoomToRoute(route, 0.2f);
    }

    /**
     * Zoom the map to a route of legs, with a default padding around the path.
     * @param route the route to be zoomed to
     * @param padding additional padding around the route
     */
    public void zoomToRoute(Route route, float padding) {
        List<LatLng> points = new ArrayList<>();
        for(Location location: route.getPoints()) {
            points.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        BoundingBox boundingBox = BoundingBox.fromLatLngs(points);
        double north = boundingBox.getLatNorth();
        double east = boundingBox.getLonEast();
        double west = boundingBox.getLonWest();
        double south = boundingBox.getLatSouth();
        double latitudeDiff = Math.abs(north - south) * padding;
        double longitudeDiff = Math.abs(east - west) * padding;
        // Adding padding
        ArrayList<LatLng> paddedWaypoints = new ArrayList<>();
        LatLng ne = new LatLng(north + latitudeDiff, east + longitudeDiff);
        LatLng sw = new LatLng(south - latitudeDiff, west - longitudeDiff);
        paddedWaypoints.add(ne);
        paddedWaypoints.add(sw);
        zoomToBoundingBox(BoundingBox.fromLatLngs(paddedWaypoints), true, true, false, true);
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
        removeDestinationPreviewMarker();
        destinationPreviewMarker = new IBCMarker(a.getStreetAddress(), a.getPostCodeAndCity(), a.getLocation(), MarkerType.ADDRESS);
        Icon markerIcon = new Icon(this.getResources().getDrawable(R.drawable.marker));
        destinationPreviewMarker.setIcon(markerIcon);
        addMarker(destinationPreviewMarker);
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

    public void removeDestinationPreviewMarker() {
        if (destinationPreviewMarker != null) {
            this.getOverlays().remove(destinationPreviewMarker);
            this.removeMarker(destinationPreviewMarker);
            destinationPreviewMarker = null;
        }
    }

    @Override
    public void clear() {
        removeDestinationPreviewMarker();
        clearRouteOverlay();
        super.clear();
    }

    /**
     * Removes all route overlays and markers from the map
     */
    public void clearRouteOverlay() {
        if(routeOverlay != null) {
            routeOverlay.onDetach(this);
            removeOverlay(routeOverlay);
            routeOverlay = null;
        }
    }
}
