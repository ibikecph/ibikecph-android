// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import java.util.LinkedList;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class SMMapFragment extends MapFragmentBase implements MapListener, OnMapLongPressListener {

    LongPressGestureOverlay longPressOverlay;
    ImageView pinView;
    ItemizedIconOverlay<OverlayItem> pinB;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LOG.d("SMMap fragment onActivityCreated");
        mapView.setMapListener(this);
        setHasOptionsMenu(true);
        longPressOverlay = new LongPressGestureOverlay(this, mapView);
        mapView.getOverlays().add(longPressOverlay);
        pinView = new ImageView(IbikeApplication.getContext());
        pinView.setImageResource(R.drawable.marker_finish);
        pinView.setVisibility(View.GONE);
        mapView.addView(pinView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        pinView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getMapActivity().togglePinInfoLayoutVisibility();
                pinView.setVisibility(View.GONE);
            }
        });
        locationOverlay.setMapFragment(this);
        mapView.tracking = true;
        mapView.setMapListener(this);
        locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
        // center of Copenhagen

        if (pinB != null) {
            mapView.getOverlayManager().remove(pinB);
        }

        if (SMLocationManager.getInstance().hasValidLocation()) {
            mapView.getController().animateTo(new GeoPoint(SMLocationManager.getInstance().getLastValidLocation()));
        } else {
            mapView.getController().animateTo(new GeoPoint(locCopenhagen));
        }

    }

    public boolean getTrackingMode() {
        return mapView.tracking;
    }

    public void setTrackingMode(boolean tracking) {
        mapView.tracking = tracking;
        if (tracking && SMLocationManager.getInstance().hasValidLocation())
            mapView.getController().animateTo(new GeoPoint(SMLocationManager.getInstance().getLastValidLocation()));
    }

    boolean tileSourceSet = false;
    Location lastLoc;
    public int infoLayoutHeight;

    @Override
    public void onLocationChanged(Location location) {
        LOG.d("SMMapFragment onLocationChanged");
        lastLoc = location;
        // locationOverlay.onLocationChanged(location, locationOverlay.getMyLocationProvider());
        if (getTrackingMode()) {
            animateMap(location);
        }
        if (!tileSourceSet) {
            refreshMapTileSource(location);
            tileSourceSet = true;
        }
        mapView.lastLocation = location;
    }

    public void animateMap(Location location) {
        if (!noTracking) {
            int offsset = 0;
            if (getMapActivity().findViewById(R.id.pinInfoLayout).getVisibility() == View.VISIBLE) {
                offsset = Util.dp2px(50);
                mapView.locationArrowOffsset = offsset;
            } else {
                mapView.locationArrowOffsset = 0;
            }
            mapView.getController().animateToWithOffsset(location.getLatitude(), location.getLongitude(), offsset);// (new
            // GeoPoint(location));
        }
    }

    public Location getPinLocation() {
        return Util.locationFromGeoPoint(((MapView.LayoutParams) pinView.getLayoutParams()).geoPoint);
    }

    public void setPinLocation(Location loc) {
        positionPinView(new GeoPoint(loc));
    }

    private void positionPinView(int x, int y) {
        IGeoPoint gp = mapView.getProjection().fromPixels(x, y);
        positionPinView(gp);
    }

    private void positionPinView(IGeoPoint gp) {
        MapView.LayoutParams lParams = (MapView.LayoutParams) pinView.getLayoutParams();
        lParams.geoPoint = gp;
        lParams.alignment = MapView.LayoutParams.BOTTOM_CENTER;
        pinView.setLayoutParams(lParams);
    }

    private MapActivity getMapActivity() {
        return ((MapActivity) getActivity());
    }

    @Override
    public void onMapLongPress(final int X, final int Y) {

        if (mapView.isPinchZooming) {
            return;
        }

        if (pinB != null) {
            mapView.getOverlayManager().remove(pinB);
        }

        getMapActivity().updatePinInfo("", "");

        getMapActivity().enableAddFavourite();

        pinView.setVisibility(View.VISIBLE);
        getMapActivity().showPinInfoLayout();

        final IGeoPoint gp = mapView.getProjection().fromPixels(X, Y);

        int startPosX = 50;
        int startPosY = 50;
        positionPinView(startPosX, startPosY);

        final TranslateAnimation anim = new TranslateAnimation(X - startPosX, X - startPosX, -startPosY, Y - startPosY);
        anim.setDuration(300);

        anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                LOG.d("animation started");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                pinView.setVisibility(View.GONE);
                positionPinView(X, Y);
                placePinOverlay(new GeoPoint(Util.locationFromGeoPoint(gp).getLatitude(), Util.locationFromGeoPoint(gp).getLongitude()));
            }
        });

        anim.setDuration(200);
        pinView.startAnimation(anim);

        // This may be done in future again, so keep it commented
        // new SMHttpRequest().findNearestPoint(gp, this);
        new SMHttpRequest().findPlacesForLocation(Util.locationFromGeoPoint(gp), getMapActivity());
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    boolean noTracking = false;

    @Override
    public boolean onScroll(final ScrollEvent event) {
        if (event.getUserAction() && mapView.tracking && !(mapView.isPinchZooming || System.currentTimeMillis() - mapView.lastPinchTimestamp < 200)) {
            getMapActivity().stopTrackingUser();

        }
        return false;
    }

    @Override
    public boolean onZoom(ZoomEvent event) {
        // getMapActivity().stopTrackingUser();
        mapView.getOverlayManager().previousZoom = mapView.getOverlayManager().currentZoom;
        mapView.getOverlayManager().currentZoom = (int) event.getZoomLevel();
        mapView.invalidate();
        return false;
    }

    public void placePinOverlay(GeoPoint gp) {
        OverlayItem item = new OverlayItem("EndItem", "This is end item", gp);
        item.setMarker(getResources().getDrawable(R.drawable.marker_finish));
        List<OverlayItem> markerList = new LinkedList<OverlayItem>();
        markerList.add(item);
        if (pinB != null) {
            mapView.getOverlayManager().remove(pinB);
        }
        pinB = new ItemizedIconOverlay<OverlayItem>(getActivity(), markerList, new OnItemGestureListener<OverlayItem>() {
            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                getMapActivity().togglePinInfoLayoutVisibility();
                mapView.getOverlays().remove(pinB);
                mapView.invalidate();
                // mapView.scrollBy(0, infoLayoutHeight / 2);
                return true;
            }

            public boolean onItemLongPress(final int index, final OverlayItem item) {
                LOG.d("item long pressed!" + item.mTitle);
                return true;
            }
        });
        mapView.getOverlays().add(pinB);
    }

    @Override
    public boolean onDoubleTapZoom() {
        // getMapActivity().stopTrackingUser();
        return true;
    }

    @Override
    public boolean onPinchZoom() {
        return false;
    }

    public void onBottomViewShown() {
        if (lastLoc != null) {
            onLocationChanged(lastLoc);
        } else if (SMLocationManager.getInstance().hasValidLocation()) {
            onLocationChanged(SMLocationManager.getInstance().getLastValidLocation());
        } else if (SMLocationManager.getInstance().getLastKnownLocation() != null) {
            onLocationChanged(SMLocationManager.getInstance().getLastKnownLocation());
        }

    }

}
