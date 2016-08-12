package com.spoiledmilk.ibikecph.map.overlays;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType;

import java.util.ArrayList;
import java.util.List;

/**
 * An overlay showing a journey of routes on the MapView
 * Created by kraen on 03-07-16.
 */
public class JourneyOverlay extends Overlay implements LocationListener {

    protected Journey journey;

    protected RoutePathOverlay path;
    protected RoutePathOverlay startingWalkPath;
    protected RoutePathOverlay endingWalkPath;

    protected JourneyMarkerOverlay markersOverlay;

    protected List<Overlay> overlays = new ArrayList<>();

    protected MapView mapView;

    public JourneyOverlay(MapView mapView, Journey journey) {
        this.mapView = mapView;
        this.journey = journey;

        // Placing this route overlay underneath the users location
        setOverlayIndex(1);

        // Add a path overlay for every route in the journey
        for(SMRoute route: journey.getRoutes()) {
            RoutePathOverlay overlay = new RouteOverlay(mapView, route);
            overlays.add(overlay);
        }

        // Crate a walk path, from the users current location to the route.
        // TODO: Fix the walk path for journeys computed with the breaking route API
        if(journey.getRoutes().size() > 0 && journey.getRoutes().get(0).startAddress.isCurrentLocation()) {
            startingWalkPath = new RoutePathOverlay(mapView.getContext(), TransportationType.WALK);
            updateStartingWalkPath(BikeLocationService.getInstance().getLastValidLocation());
            overlays.add(startingWalkPath);
        }

        // Create a walk path, from the last point in the overlay to the location the user tapped.
        endingWalkPath = new RoutePathOverlay(mapView.getContext(), TransportationType.WALK);
        if(journey.getRoutes().size() > 0) {
            SMRoute lastRoute = journey.getRoutes().get(journey.getRoutes().size()-1);
            endingWalkPath.addPoint(new LatLng(lastRoute.getEndLocation()));
            endingWalkPath.addPoint(lastRoute.getRealEndLocation());
        }
        overlays.add(endingWalkPath);

        markersOverlay = new JourneyMarkerOverlay(mapView);
        if(journey.getRoutes().size() > 0) {
            // Add the start location of the first route as a marker.
            SMRoute firstRoute = journey.getRoutes().get(0);
            LatLng journeyStart = new LatLng(firstRoute.getStartLocation());
            IBCMarker journeyStartMarker = new IBCMarker("", "", journeyStart, MarkerType.PATH_ENDPOINT);
            journeyStartMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
            markersOverlay.addItem(journeyStartMarker);

            // Add the end location of the last route as a marker.
            SMRoute lastRoute = journey.getRoutes().get(journey.getRoutes().size()-1);
            LatLng journeyEnd = new LatLng(lastRoute.getRealEndLocation());
            IBCMarker journeyEndMarker = new IBCMarker("", "", journeyEnd, MarkerType.PATH_ENDPOINT);
            journeyEndMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));
            markersOverlay.addItem(journeyEndMarker);
        }

        boolean first = true;
        for(SMRoute route: journey.getRoutes()) {
            if(first) {
                first = false;
                continue; // Skip the first route, as it has the start marker
            }
            // Let's only show a marker if the transportation type needs it
            Icon icon = getTransportationTypeIcon(route);
            if(icon != null) {
                Location routeStartLocation = route.getStartLocation();
                LatLng routeStartLatLng = new LatLng(routeStartLocation);
                IBCMarker journeyEndMarker = new IBCMarker("", "", routeStartLatLng, MarkerType.PATH_ENDPOINT);
                journeyEndMarker.setIcon(icon);
                markersOverlay.addItem(journeyEndMarker);
            }
        }
        overlays.add(markersOverlay);

        // We want this object to listen for changes to the location, so it can adjust the walk
        // paths accordingly. This is deregistered again when onDetach is called.
        BikeLocationService.getInstance().addLocationListener(this);
    }

    private Icon getTransportationTypeIcon(SMRoute route) {
        if(route != null) {
            switch(route.transportType) {
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
        if(journey.getRoutes().size() > 0 && startingWalkPath != null) {
            startingWalkPath.clearPath();
            SMRoute firstRoute = journey.getRoutes().get(0);
            // The starting walk path should only be shown if no turn instructions has been passed.
            if(firstRoute.getWaypoints().size() > 0 && firstRoute.getPastTurnInstructions().size() == 0) {
                Location routeStart = firstRoute.getWaypoints().get(0);
                startingWalkPath.addPoint(new LatLng(currentUserLocation));
                startingWalkPath.addPoint(new LatLng(routeStart));
            }
        }
    }

    @Override
    protected void draw(Canvas c, MapView mapView, boolean shadow) {
        for(Overlay overlay: overlays) {
            if(overlay.isEnabled()) {
                if (overlay instanceof RoutePathOverlay) {
                    ((RoutePathOverlay) overlay).draw(c, mapView, false);
                    ((RoutePathOverlay) overlay).draw(c, mapView, true);
                } else if (overlay instanceof JourneyMarkerOverlay) {
                    ((JourneyMarkerOverlay) overlay).draw(c, mapView, false);
                    ((JourneyMarkerOverlay) overlay).draw(c, mapView, true);
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
        BikeLocationService.getInstance().removeLocationListener(this);
        for(Overlay overlay: overlays) {
            overlay.onDetach(mapView);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updateStartingWalkPath(location);
        mapView.invalidate();
    }
}
