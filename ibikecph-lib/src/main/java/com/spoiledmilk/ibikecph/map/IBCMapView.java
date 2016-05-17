package com.spoiledmilk.ibikecph.map;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.map.handlers.IBCMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

import java.lang.reflect.InvocationTargetException;
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

    public enum MapViewState {
        DEFAULT,
        TRACK_DISPLAY,
        NAVIGATION_OVERVIEW
    }

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

    public void showRoute(final FavoritesData fd) {
        Address a = Address.fromFavoritesData(fd);
        showRoute(a);
    }

    public void showRoute(final Address destination) {
        showRoute(null, destination);
    }

    public void showRoute(final Address givenSource, final Address givenDestination) {
        Address source = givenSource;
        Address destination = givenDestination;

        // Remove the address marker, because the route draws its own end marker.
        if (this.currentAddressMarker != null) {
            this.removeMarker(this.currentAddressMarker);
            this.currentAddressMarker = null;
        }

        // If no source address is provided, assume current location
        if (givenSource == null || givenDestination == null) {
            Address loc = Address.fromCurLoc();

            // If we don't have a GPS coordinate, we cannot get the current address. Let the user know and return.
            if (loc == null) {
                Toast.makeText(
                        IbikeApplication.getContext(),
                        IbikeApplication.getString("error_no_gps_location"),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (givenSource == null) {
                source = loc;
            }

            if (givenDestination == null) {
                destination = null;
            }
        }

        final Address finalDestination = destination;
        final Address finalSource = source;


        Log.d("DV_break", "Calling Geocoder.getRoute");
        Geocoder.getRoute(source.getLocation(), destination.getLocation(), new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                Log.d("DV_break", "IBCMapView, onSuccess!");
                route.startStationName = finalSource.getStreetAddress();
                route.endStationName = finalDestination.getStreetAddress();
                route.startAddress = finalSource;
                route.endAddress = finalDestination;
                if (route.endAddress.getAddressSource() == Address.AddressSource.FAVORITE) {
                    route.endAddress.setHouseNumber("");
                }

                Log.d("DV_break", "IBCMapView, calling showRoute!");
                showRoute(route);
            }

            @Override
            public void onSuccess(boolean isBreak) {

            }

            @Override
            public void onFailure() {
                Log.d("DV_break", "IBCMapView, onFailure!");
            }

        }, null, RouteType.FASTEST);


    }


    public IBCMapHandler getMapHandler() {
        // We can always cast this to an IBCMapHandler as it's checked to be one when setting it.
        return (IBCMapHandler) currentListener;
    }

    public BaseMapActivity getParentActivity() {
        return parentActivity;
    }

    /**
     * Adds a GPS location dot.
     */
    public void addUserLocationOverlay() {
        this.setUserLocationEnabled(true);
        this.getUserLocationOverlay().setDrawAccuracyEnabled(true);
        this.getUserLocationOverlay().enableFollowLocation();
        this.getUserLocationOverlay().setPersonBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.tracking_dot));
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

    /*
    public void showAddressFromFavorite(Address a) {
        showAddressInfoPane(a);
        removeAddressMarker();
    }
    */

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

    public void removeAllMarkersOfType(MarkerType t) {
        for (IBCMarker m : markers) {

            if (m.getType() == t) {
                this.removeMarker(m);
            }
        }
    }

}
