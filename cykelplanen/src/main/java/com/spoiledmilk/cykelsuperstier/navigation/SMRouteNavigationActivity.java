// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.navigation;

import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.SplashActivity;
import com.spoiledmilk.cykelsuperstier.break_rote.BreakRouteActivity;
import com.spoiledmilk.cykelsuperstier.controls.InstructionsPagerAdapter;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.util.Util;

public class SMRouteNavigationActivity extends com.spoiledmilk.ibikecph.navigation.SMRouteNavigationActivity implements
		SMHttpRequestListener {

	boolean isPathSelected = false;
	boolean isServiceSelected = false;
	boolean isStrainSelected = false;
	boolean isMetroSelected = false;
	boolean isLocalTrainSelected = false;
	TextView textPath;
	TextView textService;
	TextView textStrain;
	TextView textMetro;
	TextView textLocalTrain;
	Location startStat;
	Location endStat;
	SMRouteNavigationMapFragment mapFragment = null;
	int originalRouteDistance = -1;
	AlertDialog dialog;
	String aStationName;
	String bStationName;
	double distance;
	int stationIconId = -1;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		textPath = (TextView) findViewById(R.id.textPath);
		textService = (TextView) findViewById(R.id.textService);
		textStrain = (TextView) findViewById(R.id.textStrain);
		textMetro = (TextView) findViewById(R.id.textMetro);
		textLocalTrain = (TextView) findViewById(R.id.textLocalTrain);

		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("overlays")) {
			((SMRouteNavigationMapFragment) getMapFragment()).overlaysShown = getIntent().getIntExtra("overlays", 0);
			int overlaysShown = getIntent().getIntExtra("overlays", 0);
			if ((overlaysShown & 1) > 0) {
				isPathSelected = true;
				findViewById(R.id.pathContainer).setBackgroundColor(Color.rgb(236, 104, 0));
				((ImageView) findViewById(R.id.imgCheckbox1)).setImageResource(R.drawable.check_in_orange);
				((ImageView) findViewById(R.id.imgPath)).setImageResource(R.drawable.bike_icon_white);
				textPath.setTextColor(Color.WHITE);
			}
			if ((overlaysShown & 2) > 0) {
				isServiceSelected = true;
				findViewById(R.id.serviceContainer).setBackgroundColor(Color.rgb(236, 104, 0));
				((ImageView) findViewById(R.id.imgCheckbox2)).setImageResource(R.drawable.check_in_orange);
				((ImageView) findViewById(R.id.imgService)).setImageResource(R.drawable.service_pump_icon_white);
				textService.setTextColor(Color.WHITE);
			}
			if ((overlaysShown & 4) > 0) {
				isStrainSelected = true;
				findViewById(R.id.strainContainer).setBackgroundColor(Color.rgb(236, 104, 0));
				((ImageView) findViewById(R.id.imgCheckbox3)).setImageResource(R.drawable.check_in_orange);
				((ImageView) findViewById(R.id.imgStrain)).setImageResource(R.drawable.s_togs_icon_white);
				textStrain.setTextColor(Color.WHITE);
			}
			if ((overlaysShown & 8) > 0) {
				isMetroSelected = true;
				findViewById(R.id.metroContainer).setBackgroundColor(Color.rgb(236, 104, 0));
				((ImageView) findViewById(R.id.imgCheckbox4)).setImageResource(R.drawable.check_in_orange);
				((ImageView) findViewById(R.id.imgMetro)).setImageResource(R.drawable.metro_icon_white);
				textMetro.setTextColor(Color.WHITE);
			}
			if ((overlaysShown & 16) > 0) {
				isLocalTrainSelected = true;
				findViewById(R.id.localTrainContainer).setBackgroundColor(Color.rgb(236, 104, 0));
				((ImageView) findViewById(R.id.imgCheckbox5)).setImageResource(R.drawable.check_in_orange);
				((ImageView) findViewById(R.id.imgLocalTrain)).setImageResource(R.drawable.local_train_icon_white);
				textLocalTrain.setTextColor(Color.WHITE);
			}
		}

	}

	protected Class<?> getSplashActivityClass() {
		return SplashActivity.class;
	}

	@Override
	public void onResume() {
		super.onResume();
		((Button) findViewById(R.id.btnStart)).setText("");
		textPath.setTypeface(CykelsuperstierApplication.getNormalFont());
		textPath.setText(CykelsuperstierApplication.getString("marker_type_1"));
		textService.setTypeface(CykelsuperstierApplication.getNormalFont());
		textService.setText(CykelsuperstierApplication.getString("marker_type_2"));
		textStrain.setTypeface(CykelsuperstierApplication.getNormalFont());
		textStrain.setText(CykelsuperstierApplication.getString("marker_type_3"));
		textMetro.setTypeface(CykelsuperstierApplication.getNormalFont());
		textMetro.setText(CykelsuperstierApplication.getString("marker_type_4"));
		textLocalTrain.setTypeface(CykelsuperstierApplication.getNormalFont());
		textLocalTrain.setText(CykelsuperstierApplication.getString("marker_type_5"));

	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
	}

	@Override
	protected boolean hasDarkImage() {
		return false;
	}

	@Override
	protected SMRouteNavigationMapFragment getMapFragment() {
		if (mapFragment == null)
			return mapFragment = new SMRouteNavigationMapFragment();
		else
			return mapFragment;
	}

	@Override
	protected InstructionsPagerAdapter getPagerAdapter() {
		return new InstructionsPagerAdapter(getSupportFragmentManager(), mapFragment, this);
	}

	protected int getPullHandeBackground() {
		return Color.WHITE;
	}

	@Override
	protected InstructionListAdapter getInstructionsAdapter() {
		if (adapter == null) {
			adapter = new InstructionListAdapter(this, R.layout.direction_top_cell, mapFragment.route);
		}
		return (InstructionListAdapter) adapter;
	}

	public void onPathContainerClick(View v) {
		v.setBackgroundColor(isPathSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox1))
				.setImageResource(isPathSelected ? R.drawable.check_field : R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgPath)).setImageResource(isPathSelected ? R.drawable.bike_icon_gray : R.drawable.bike_icon_white);
		textPath.setTextColor(isPathSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isPathSelected)
			getMapFragment().overlaysManager.removeBikeRoutes();
		else
			getMapFragment().overlaysManager.drawBikeRoutes(this);
		isPathSelected = !isPathSelected;
	}

	public void onServiceContainerClick(View v) {
		v.setBackgroundColor(isServiceSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox2)).setImageResource(isServiceSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgService)).setImageResource(isServiceSelected ? R.drawable.service_pump_icon_gray
				: R.drawable.service_pump_icon_white);
		textService.setTextColor(isServiceSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isServiceSelected)
			getMapFragment().overlaysManager.removeServiceStations();
		else
			getMapFragment().overlaysManager.drawServiceStations(this);
		isServiceSelected = !isServiceSelected;
	}

	public void onStrainContainerClick(View v) {
		v.setBackgroundColor(isStrainSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox3)).setImageResource(isStrainSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgStrain)).setImageResource(isStrainSelected ? R.drawable.s_togs_icon
				: R.drawable.s_togs_icon_white);
		textStrain.setTextColor(isStrainSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isStrainSelected)
			getMapFragment().overlaysManager.removesTrainStations();
		else
			getMapFragment().overlaysManager.drawsTrainStations(this);
		isStrainSelected = !isStrainSelected;
	}

	public void onMetroContainerClick(View v) {
		v.setBackgroundColor(isMetroSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox4)).setImageResource(isMetroSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgMetro)).setImageResource(isMetroSelected ? R.drawable.metro_icon : R.drawable.metro_icon_white);
		textMetro.setTextColor(isMetroSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isMetroSelected)
			getMapFragment().overlaysManager.removeMetroStations();
		else
			getMapFragment().overlaysManager.drawMetroStations(this);
		isMetroSelected = !isMetroSelected;
	}

	public void onLocalTrainContainerClick(View v) {
		v.setBackgroundColor(isLocalTrainSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox5)).setImageResource(isLocalTrainSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgLocalTrain)).setImageResource(isLocalTrainSelected ? R.drawable.local_train_icon_gray
				: R.drawable.local_train_icon_white);
		textLocalTrain.setTextColor(isLocalTrainSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isLocalTrainSelected)
			getMapFragment().overlaysManager.removelocalTrainStations();
		else
			getMapFragment().overlaysManager.drawlocalTrainStations(this);
		isLocalTrainSelected = !isLocalTrainSelected;
	}

	public void onBtnBreakRouteClick(View v) {
		Intent i = new Intent(SMRouteNavigationActivity.this, BreakRouteActivity.class);
		if (originalRouteDistance < 0)
			originalRouteDistance = mapFragment.route.getEstimatedDistance();
		i.putExtra("distance", originalRouteDistance);
		i.putExtra("source", mapFragment.source);
		i.putExtra("destination", mapFragment.destination);
		i.putExtra("start_lat", mapFragment.startLocation.getLatitude());
		i.putExtra("start_lon", mapFragment.startLocation.getLongitude());
		i.putExtra("end_lat", mapFragment.endLocation.getLatitude());
		i.putExtra("end_lon", mapFragment.endLocation.getLongitude());
		startActivityForResult(i, 1);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == BreakRouteActivity.RESULT_ROUTE_BROKEN) {
			if (data != null && data.getExtras() != null) {
				Bundle args = data.getExtras();
				startStat = Util.locationFromCoordinates(args.getDouble("start_station_lat"), args.getDouble("start_station_lon"));
				endStat = Util.locationFromCoordinates(args.getDouble("end_station_lat"), args.getDouble("end_station_lon"));
				aStationName = args.getString("start_stat_name");
				bStationName = args.getString("end_stat_name");
				distance = args.getDouble("distance");
				stationIconId = args.getInt("iconId");
				getMapFragment().jsonRoot = null;
				List<Location> viaList = new LinkedList<Location>();
				viaList.add(startStat);
				viaList.add(endStat);
				new SMHttpRequest().getRoute(getMapFragment().startLocation, getMapFragment().endLocation, viaList, this);
			}
		}
	}

	@Override
	public void onResponseReceived(int requestType, Object response) {
		switch (requestType) {
		case SMHttpRequest.REQUEST_GET_ROUTE:
			RouteInfo ri = (RouteInfo) response;
			JsonNode jsonRoot = null;
			if (ri == null || (jsonRoot = ri.jsonRoot) == null || jsonRoot.path("status").asInt(-1) != 0) {
				showRouteNoBreakDialog(this, dialog);
			} else {
				getMapFragment().restartRoute(startStat, endStat, jsonRoot, aStationName, bStationName, stationIconId);
				instructionList.setAdapter(getInstructionsAdapter());
				((TextView) overviewLayout.findViewById(R.id.overviewDistanceAndVia)).setText(distance + "km, "
						+ IbikeApplication.getString("via") + " " + aStationName);
			}
			break;
		}
	}

	public static void showRouteNoBreakDialog(final Context context, AlertDialog dialog) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(CykelsuperstierApplication.getString("no_route"));
		builder.setMessage(CykelsuperstierApplication.getString("cannot_broken"));
		builder.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int arg1) {
				dialog.dismiss();
			}
		});
		dialog = builder.create();
		dialog.setCancelable(false);
		dialog.show();
	}

	@Override
	public int getOverlaysShown() {
		int ret = 0;
		if (isPathSelected)
			ret |= 1;
		if (isServiceSelected)
			ret |= 2;
		if (isStrainSelected)
			ret |= 4;
		if (isMetroSelected)
			ret |= 8;
		if (isLocalTrainSelected)
			ret |= 16;
		return ret;
	}

	@Override
	protected int getListItemHeight() {
		return Util.dp2px(70);
	}
}
