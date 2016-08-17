package com.spoiledmilk.ibikecph.map.overlays;

import android.graphics.Canvas;
import android.location.Location;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.IBCMarker;
import com.spoiledmilk.ibikecph.map.MarkerType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;
import com.spoiledmilk.ibikecph.navigation.routing_engine.RouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType;

import java.util.ArrayList;
import java.util.List;

/**
 * An overlay showing a journey of routes on the MapView
 * Created by kraen on 03-07-16.
 */
public class RouteOverlay extends Overlay implements LocationListener, RouteListener {

    protected Route route;

    protected RoutePathOverlay path;
    protected RoutePathOverlay startingWalkPath;
    protected RoutePathOverlay endingWalkPath;

    protected RouteMarkerOverlay markersOverlay;

    protected List<Overlay> overlays = new ArrayList<>();

    protected MapView mapView;

    public RouteOverlay(MapView mapView, Route route) {
        this.mapView = mapView;
        this.route = route;

        // Placing this route overlay underneath the users location
        setOverlayIndex(1);

        routeChanged();

        // We want this object to listen for changes to the location, so it can adjust the walk
        // paths accordingly. This is deregistered again when onDetach is called.
        BikeLocationService.getInstance().addLocationListener(this);
        route.addListener(this);
    }

    private Icon getTransportationTypeIcon(Leg leg) {
        if(leg != null) {
            switch(leg.getTransportType()) {
                case BIKE:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_bike));
                case IC:
                case LYN:
                case REG:
                case TOG:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_train));
                case BUS:
                case EXB:
                case NB:
                case TB:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_bus));
                case M:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_metro));
                case S:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_s));
                case F:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_boat));
                case WALK:
                    return new Icon(mapView.getResources().getDrawable(R.drawable.marker_walk));
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private void updateStartingWalkPath(Location currentUserLocation) {
        if(startingWalkPath != null) {
            startingWalkPath.clearPath();
            // The starting walk path should only be shown if no turn instructions has been passed.
            if(route.hasState() && !route.getState().hasStarted()) {
                Location routeStart = route.getStartLocation();
                startingWalkPath.addPoint(new LatLng(currentUserLocation));
                startingWalkPath.addPoint(new LatLng(routeStart));
            }
            // Invalidating the mapView makes it blink - and it does not improve the redrawing rate
            // mapView.invalidate();
        }
    }

    @Override
    protected void draw(Canvas c, MapView mapView, boolean shadow) {
        for(Overlay overlay: overlays) {
            if(overlay.isEnabled()) {
                if (overlay instanceof RoutePathOverlay) {
                    ((RoutePathOverlay) overlay).draw(c, mapView, false);
                    ((RoutePathOverlay) overlay).draw(c, mapView, true);
                } else if (overlay instanceof RouteMarkerOverlay) {
                    ((RouteMarkerOverlay) overlay).draw(c, mapView, false);
                    ((RouteMarkerOverlay) overlay).draw(c, mapView, true);
                } else {
                    // Can't draw an overlay without a draw method accessible
                    throw new RuntimeException("Drawing a " +
                            overlay.getClass().getSimpleName() +
                            " overlay is not supported.");
                }
            }
        }
    }

    @Override
    public void onDetach(MapView mapView) {
        super.onDetach(mapView);
        route.removeListener(this);
        BikeLocationService.getInstance().removeLocationListener(this);
        for(Overlay overlay: overlays) {
            overlay.onDetach(mapView);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updateStartingWalkPath(location);
    }


    @Override
    public void routeChanged() {
        // Remove all overlays
        overlays.clear();

        // Add a path overlay for every leg in the route
        for (Leg leg : route.getLegs()) {
            RoutePathOverlay overlay = new LegOverlay(mapView, leg);
            overlays.add(overlay);
        }

        // Create a walk path, from the users current location to the route.
        // TODO: Fix the walk path for journeys computed with the breaking route API
        if (route.getLegs().size() > 0) {
            startingWalkPath = new RoutePathOverlay(mapView.getContext(), TransportationType.WALK);
            updateStartingWalkPath(BikeLocationService.getInstance().getLastValidLocation());
            overlays.add(startingWalkPath);
        }

        // Create a walk path, from the last point in the overlay to the location the user tapped.
        endingWalkPath = new RoutePathOverlay(mapView.getContext(), TransportationType.WALK);
        endingWalkPath.addPoint(new LatLng(route.getEndLocation()));
        endingWalkPath.addPoint(route.getRealEndLocation());
        overlays.add(endingWalkPath);

        markersOverlay = new RouteMarkerOverlay(mapView);

        LatLng journeyStart = new LatLng(route.getStartLocation());
        IBCMarker journeyStartMarker = new IBCMarker("", "", journeyStart, MarkerType.PATH_ENDPOINT);
        journeyStartMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
        markersOverlay.addItem(journeyStartMarker);

        // Add the end location of the last route as a marker.
        LatLng journeyEnd = new LatLng(route.getRealEndLocation());
        IBCMarker journeyEndMarker = new IBCMarker("", "", journeyEnd, MarkerType.PATH_ENDPOINT);
        journeyEndMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));
        markersOverlay.addItem(journeyEndMarker);

        boolean first = true;
        for (Leg leg : route.getLegs()) {
            if (first) {
                first = false;
                continue; // Skip the first route, as it has the start marker
            }
            // Let's only show a marker if the transportation type needs it
            Icon icon = getTransportationTypeIcon(leg);
            if (icon != null) {
                Location legStartLocation = leg.getStartLocation();
                LatLng legStartLatLng = new LatLng(legStartLocation);
                IBCMarker legStartMarker = new IBCMarker("", "", legStartLatLng, MarkerType.PATH_ENDPOINT);
                legStartMarker.setIcon(icon);
                markersOverlay.addItem(legStartMarker);
            }
        }
        overlays.add(markersOverlay);

        mapView.invalidate();
    }
}
