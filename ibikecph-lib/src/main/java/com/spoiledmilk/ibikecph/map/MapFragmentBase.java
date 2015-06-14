// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaledTilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.SMMyLocationNewOverlay;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class MapFragmentBase extends Fragment implements SMLocationListener {
	public MapView mapView;
	public SMMyLocationNewOverlay locationOverlay;
	protected ResourceProxy resourceProxy;
	protected Location lastLocation = null;
	public static final Location locCopenhagen = Util.locationFromCoordinates(55.675455, 12.566643);
	public TextView textStationInfo;
	public OverlaysManager overlaysManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LOG.d("MapFragmentBase onCreate");
		textStationInfo = new TextView(getActivity());
		textStationInfo.setBackgroundResource(R.drawable.rounded_rectangle_22);
		textStationInfo.setTypeface(IbikeApplication.getNormalFont());
		textStationInfo.setTextColor(Color.BLACK);
		textStationInfo.setGravity(Gravity.CENTER);
		textStationInfo.setPadding(Util.dp2px(5), Util.dp2px(5), Util.dp2px(5), Util.dp2px(5));
		textStationInfo.setVisibility(View.GONE);
		textStationInfo.setTextSize(16);
		textStationInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				textStationInfo.setVisibility(View.GONE);
			}
		});
		overlaysManager = new OverlaysManager(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		LOG.d("MapFragment onCreateView");
		try {
			resourceProxy = new ResourceProxy(inflater.getContext().getApplicationContext());
			mapView = new MapView(inflater.getContext(), 256, resourceProxy);
			mapView.setMaxZoomLevel(20);
			mapView.setMinZoomLevel(6);
			mapView.getOverlayManager().defaultZoom = (int) IbikePreferences.DEFAULT_ZOOM_LEVEL;
		} catch (Exception e) {
			LOG.d(e.getLocalizedMessage());
		}
		mapView.setUseSafeCanvas(true);
		//setHardwareAccelerationOff();
		mapView.addView(textStationInfo);

		return mapView;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		// Turn off hardware acceleration here, or in manifest
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		LOG.d("MapFragment onActivityCreated");
		if (locationOverlay == null)
			locationOverlay = new SMMyLocationNewOverlay(getActivity(), new GpsMyLocationProvider(getActivity()), mapView);
		mapView.setBuiltInZoomControls(false);
		mapView.setMultiTouchControls(true);
		mapView.getOverlays().add(locationOverlay);
		mapView.getController().setZoom(IbikePreferences.DEFAULT_ZOOM_LEVEL);
		locationOverlay.enableMyLocation(new GpsMyLocationProvider(getActivity()));
		locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
		overlaysManager.loadOverlaysData(getActivity());
	}

	boolean isReturnFromLock = false;
	int lastScrollX, lastScrollY;

	@Override
	public void onPause() {
		SMLocationManager.getInstance().removeUpdates();
		this.locationOverlay.disableMyLocation();
		super.onPause();
		PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		boolean isScreenOn = powerManager.isScreenOn();
		if (!isScreenOn) {
			isReturnFromLock = true;
			lastScrollX = mapView.getScrollX();
			lastScrollY = mapView.getScrollY();
			LOG.d("SMMapFragment onPause, scroll = " + lastScrollX + ", " + lastScrollY);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		LOG.d("MapFragmentBase onResume");
		this.locationOverlay.enableMyLocation(this.locationOverlay.getMyLocationProvider());
		SMLocationManager locManager = SMLocationManager.getInstance();
		locManager.init(getActivity(), this);
		if (locManager.hasValidLocation()) {
			refreshMapTileSource(locManager.getLastValidLocation());
		} else {
			mapView.setTileSource(TileSourceFactory.getTileSource(TileSourceFactory.IBIKECPH.name()));
		}
		if (!SMLocationManager.getInstance().isGPSEnabled()) {
			launchGPSDialog();
		}
		ScaledTilesOverlay scaledTilesOverlay = new ScaledTilesOverlay(mapView.getTileProvider(), getActivity());
		mapView.getOverlayManager().setScaledTilesOverlay(scaledTilesOverlay);
		PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		boolean isScreenOn = powerManager.isScreenOn();
		if (isReturnFromLock && isScreenOn) {
			isReturnFromLock = false;
			if (mapView.tracking) {
				Location loc = SMLocationManager.getInstance().hasValidLocation() ? SMLocationManager.getInstance().getLastValidLocation()
						: SMLocationManager.getInstance().getLastKnownLocation();
				if (loc != null) {
					onLocationChanged(loc);
				}
			} else {
				mapView.scrollTo(lastScrollX, lastScrollY);
			}
		}

	}

	private void launchGPSDialog() {
		FragmentManager fm = getActivity().getFragmentManager();
		NoGPSDialog noGPSGialog = new NoGPSDialog();
		noGPSGialog.show(fm, "no_gps");
	}

	@Override
	public void onLocationChanged(Location location) {
		mapView.getController().animateTo(new GeoPoint(location));
	}

	protected void refreshMapTileSource(Location newLock) {
		if (lastLocation == null || lastLocation.distanceTo(newLock) / 1000 > 20) {
			SMLocationManager locManager = SMLocationManager.getInstance();
			Location loc = newLock == null ? locManager.getLastValidLocation() : newLock;
			if (loc != null) {
				float distanceFromCopenhagenKm = loc.distanceTo(locCopenhagen) / 1000.0f;
				if (distanceFromCopenhagenKm > 1000) {
					mapView.setTileSource(TileSourceFactory.getTileSource(Config.ALTERNATE_TILESOURCE));
				}

				else {
					mapView.setTileSource(TileSourceFactory.getTileSource(TileSourceFactory.IBIKECPH.name()));
				}
				lastLocation = loc;
			} else
				mapView.setTileSource(TileSourceFactory.getTileSource(TileSourceFactory.IBIKECPH.name()));

		}
	}

}
