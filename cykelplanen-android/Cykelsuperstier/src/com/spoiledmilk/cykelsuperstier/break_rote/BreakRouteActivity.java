// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.break_rote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.IssuesAdapter;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.map.OverlayData;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class BreakRouteActivity extends Activity implements
		SMHttpRequestListener {

	public static final int RESULT_ROUTE_BROKEN = 100;

	TextView textTitle;
	TextView textDistance;
	TextView textDistanceKm;
	TexturedButton btnBreak;
	String source = "";
	String destination = "";
	int distance = 0;
	TextView textAAddress;
	TextView textBAddress;
	Button btnRejseplanen;
	TextView textARegion;
	TextView textBRegion;
	TextView textDistance1;
	TextView textDistance2;
	Spinner spinner1;
	Spinner spinner2;
	Location startLoc, endLoc;
	AlertDialog dialog = null;
	protected ArrayList<OverlayData> metroStations;
	protected ArrayList<OverlayData> sTrainStations;
	protected ArrayList<OverlayData> localTrainStations;
	protected ArrayList<OverlayData> sortedStations;
	// protected ArrayList<OverlayData> BStations;
	OverlayData selectedStationA = null;
	OverlayData selectedStationB = null;
	double aDistance = -1, bDistance, aTime, bTime;
	double newDistance;
	int newMinDistance = Integer.MAX_VALUE, minDistance = 0;
	boolean isInitalSearch = true;
	int iconId = -1;
	int timeToAStation = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_break_route);
		textTitle = (TextView) findViewById(R.id.textTitle);
		textDistance = (TextView) findViewById(R.id.textDistance);
		textDistanceKm = (TextView) findViewById(R.id.textDistanceKm);
		btnBreak = (TexturedButton) findViewById(R.id.btnBreak);
		btnBreak.setBackgroundResource(R.drawable.btn_blue_selector);
		btnBreak.setTextureResource(R.drawable.btn_pattern_repeteable);
		btnBreak.setTextColor(Color.WHITE);
		btnBreak.setTextSize(getResources().getDimension(
				R.dimen.btn_break_text_size));
		textAAddress = (TextView) findViewById(R.id.textAAddress);
		textBAddress = (TextView) findViewById(R.id.textBAddress);
		btnRejseplanen = (Button) findViewById(R.id.btnRejseplanen);
		textARegion = (TextView) findViewById(R.id.textARegion);
		textBRegion = (TextView) findViewById(R.id.textBRegion);
		textDistance1 = (TextView) findViewById(R.id.textDistance1);
		textDistance2 = (TextView) findViewById(R.id.textDistance2);
		spinner1 = (Spinner) findViewById(R.id.spinner1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		Bundle data = getIntent().getExtras();
		if (data != null) {
			source = data.getString("source");
			destination = data.getString("destination");
			distance = data.getInt("distance");
			startLoc = Util.locationFromCoordinates(
					data.getDouble("start_lat"), data.getDouble("start_lon"));
			endLoc = Util.locationFromCoordinates(data.getDouble("end_lat"),
					data.getDouble("end_lon"));
		}

		findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		parseStationsFromJson();
		ArrayList<String> stations = sortAndGetStationStrings();
		IssuesAdapter dataAdapter = new IssuesAdapter(this, stations,
				R.layout.list_row_stations, R.layout.spinner_layout);
		spinner1.setAdapter(dataAdapter);
		if (sortedStations.size() > 0) {
			selectedStationA = sortedStations.get(0);
		} else {
			showRouteNoBreakDialog();
		}

		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				aDistance = -1;
				findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
				selectedStationA = sortedStations.get(position);
				if (metroStations.contains(selectedStationA)) {
					((ImageView) findViewById(R.id.imgStation1))
							.setImageResource(R.drawable.metro_logo_pin);
					((ImageView) findViewById(R.id.imgStation2))
							.setImageResource(R.drawable.metro_logo_pin);
					iconId = R.drawable.metro_logo_pin;
					setBikeDistance(selectedStationA, metroStations);
				} else if (localTrainStations.contains(selectedStationA)) {
					((ImageView) findViewById(R.id.imgStation1))
							.setImageResource(R.drawable.local_train_icon_blue);
					((ImageView) findViewById(R.id.imgStation2))
							.setImageResource(R.drawable.local_train_icon_blue);
					iconId = R.drawable.local_train_icon_blue;
					setBikeDistance(selectedStationA, localTrainStations);
				} else {
					((ImageView) findViewById(R.id.imgStation1))
							.setImageResource(R.drawable.list_subway_icon);
					((ImageView) findViewById(R.id.imgStation2))
							.setImageResource(R.drawable.list_subway_icon);
					iconId = R.drawable.list_subway_icon;
					setBikeDistance(selectedStationA, sTrainStations);
				}

				IssuesAdapter dataAdapterB = new IssuesAdapter(
						BreakRouteActivity.this, selectedStationA
								.getBStationsString(),
						R.layout.list_row_stations, R.layout.spinner_layout);
				spinner2.setAdapter(dataAdapterB);

				if (selectedStationA.BStations == null
						|| selectedStationA.BStations.size() == 0)
					showRouteNoBreakDialog();
				else {
					selectedStationB = selectedStationA.BStations.get(0);
					new SMHttpRequest().getRoute(selectedStationA.loc,
							startLoc, null, BreakRouteActivity.this);

				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// your code here
			}
		});
		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				if (selectedStationA.BStations == null
						|| selectedStationA.BStations.size() == 0)
					showRouteNoBreakDialog();
				else {
					selectedStationB = selectedStationA.BStations.get(position);
					// aDistance = -1;
					findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
					new SMHttpRequest().getRoute(selectedStationB.loc, endLoc,
							null, BreakRouteActivity.this);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// your code here
			}

		});
	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
	}

	private ArrayList<String> sortAndGetStationStrings() {
		sortedStations = new ArrayList<OverlayData>();
		Iterator<OverlayData> it = metroStations.iterator();
		while (it.hasNext()) {
			OverlayData od = it.next();
			setBikeDistance(od, metroStations);
			if (od.bikeDistance <= distance)
				sortedStations.add(od);
		}
		it = sTrainStations.iterator();
		while (it.hasNext()) {
			OverlayData od = it.next();
			setBikeDistance(od, sTrainStations);
			if (od.bikeDistance <= distance) {
				sortedStations.add(od);
				if (od.name.contains("Hellerup"))
					LOG.d("Hellerup added");
			}
		}
		it = localTrainStations.iterator();
		while (it.hasNext()) {
			OverlayData od = it.next();
			setBikeDistance(od, localTrainStations);
			if (od.bikeDistance <= distance)
				sortedStations.add(od);
		}
		Collections.sort(sortedStations);
		ArrayList<String> ret = new ArrayList<String>();
		Iterator<OverlayData> it2 = sortedStations.iterator();
		while (it2.hasNext()) {
			OverlayData temp = it2.next();
			LOG.d("sorted bike distance = " + temp.bikeDistance);
			ret.add(temp.toString());
		}
		return ret;
	}

	private void setBikeDistance(OverlayData od, ArrayList<OverlayData> stations) {
		double distanceA = od.loc.distanceTo(startLoc);
		OverlayData stationB = getNearestDestinationStation(od, stations);
		if (stationB != null) {
			od.bikeDistance = distanceA + stationB.loc.distanceTo(endLoc);
		} else
			od.bikeDistance = Double.MAX_VALUE;
	}

	private OverlayData getNearestDestinationStation(OverlayData od,
			ArrayList<OverlayData> stations) {
		OverlayData ret = null;
		od.BStations = new ArrayList<OverlayData>();
		double distance = Double.MAX_VALUE;
		Iterator<OverlayData> it = stations.iterator();
		while (it.hasNext()) {
			OverlayData curr = it.next();
			if (curr == od)
				continue;
			String[] sourceLines = od.line.split(",");
			for (int i = 0; i < sourceLines.length; i++) {
				if (curr.line.contains(sourceLines[i].trim())
						|| sourceLines[i].contains(curr.line)
						|| curr.line.contains(sourceLines[i])) {
					if ((ret == null && curr != od)
							|| (curr.loc.distanceTo(endLoc) < distance && curr != od)) {
						distance = curr.loc.distanceTo(endLoc);
						ret = curr;
					}
					OverlayData temp = curr.clone();
					temp.bikeDistance = curr.loc.distanceTo(endLoc);
					if (temp.bikeDistance <= distance)
						od.BStations.add(temp);
					break;
				}
			}
		}
		Collections.sort(od.BStations);
		return ret;
	}

	@Override
	public void onResponseReceived(int requestType, Object response) {
		switch (requestType) {
		case SMHttpRequest.REQUEST_GET_ROUTE:
			RouteInfo ri = (RouteInfo) response;
			JsonNode jsonRoot = null;
			if (ri == null || (jsonRoot = ri.jsonRoot) == null
					|| jsonRoot.path("status").asInt(-1) != 0) {
				showRouteNoBreakDialog();
			} else {
				if (aDistance < 0) {
					aDistance = jsonRoot.get("route_summary")
							.get("total_distance").asDouble() / 1000d;
					LOG.d("aTime = "
							+ jsonRoot.get("route_summary").get("total_time")
									.asInt() / 60);
					aTime = jsonRoot.get("route_summary").get("total_time")
							.asInt() / 60;
					new SMHttpRequest().getRoute(
							selectedStationA.BStations.get(0).loc, endLoc,
							null, this);
				} else {
					bDistance = jsonRoot.get("route_summary")
							.get("total_distance").asDouble() / 1000d;
					LOG.d("bTime = "
							+ jsonRoot.get("route_summary").get("total_time")
									.asInt() / 60);
					bTime = jsonRoot.get("route_summary").get("total_time")
							.asInt() / 60;
					BreakRouteActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							LOG.d("break route aDistance + bDistance = "
									+ (aDistance + bDistance) + " distance = "
									+ distance);
							LOG.d("break route aStation = "
									+ selectedStationA.name + " to "
									+ selectedStationB.name);

							if (aDistance + bDistance > distance / 1000f)
								// try with another station
								if (isInitalSearch
										&& spinner1.getSelectedItemPosition() < 15
										&& sortedStations.size() > spinner1
												.getSelectedItemPosition() + 1)
									// force the new route calculation by
									// changing the selected item
									spinner1.setSelection(spinner1
											.getSelectedItemPosition() + 1);
								else
									showRouteNoBreakDialog();
							else {
								// broken route found
								isInitalSearch = false;
								updateBreakRouteInfo(aDistance, aTime,
										bDistance, bTime);
							}
						}
					});
				}
			}
			break;
		default:
			updateBreakRouteInfo(Double.MAX_VALUE, Double.MAX_VALUE,
					Double.MAX_VALUE, Double.MAX_VALUE);
		}
	}

	private void showRouteNoBreakDialog() {
		if (dialog == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(CykelsuperstierApplication.getString("no_route"));
			builder.setMessage(CykelsuperstierApplication
					.getString("cannot_broken"));
			builder.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					dialog.dismiss();
					finish();
					overridePendingTransition(R.anim.slide_in_left,
							R.anim.slide_out_right);
				}
			});
			dialog = builder.create();
			dialog.setCancelable(false);
		}
		dialog.show();
	}

	public void updateBreakRouteInfo(double aDistance, double aTime,
			double bDistance, double bTime) {
		textDistance1.setText("" + Util.limitDecimalPlaces(aDistance, 1)
				+ " km.      " + (int) aTime + " min.");
		textDistance2.setText("" + Util.limitDecimalPlaces(bDistance, 1)
				+ " km.      " + (int) bTime + " min.");
		timeToAStation = (int) aTime;
		newDistance = aDistance + bDistance;
		textDistanceKm.setText(" " + Util.limitDecimalPlaces(newDistance, 1)
				+ " km");
		// if (distance < (aDistance + bDistance) * 1000)
		// showRouteNoBreakDialog();
		// else {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String url = null;
				if (startLoc != null) {
					url = "http://geo.oiorest.dk/adresser/"
							+ Util.limitDecimalPlaces(startLoc.getLatitude(), 6)
							+ ","
							+ Util.limitDecimalPlaces(startLoc.getLongitude(),
									6) + ".json";
				}
				JsonNode nodeAddr = url != null ? HttpUtils.getFromServer(url)
						: null;
				String address1 = "";
				if (nodeAddr != null && nodeAddr.has("vejnavn")
						&& nodeAddr.get("vejnavn").has("navn"))
					address1 += nodeAddr.get("vejnavn").get("navn").asText();
				if (nodeAddr != null && nodeAddr.has("husnr"))
					address1 += " " + nodeAddr.get("husnr");
				url = null;
				if (endLoc != null) {
					url = "http://geo.oiorest.dk/adresser/"
							+ Util.limitDecimalPlaces(endLoc.getLatitude(), 6)
							+ ","
							+ Util.limitDecimalPlaces(endLoc.getLongitude(), 6)
							+ ".json";
				}
				nodeAddr = url != null ? HttpUtils.getFromServer(url) : null;
				String address2 = "";
				if (nodeAddr != null && nodeAddr.has("vejnavn")
						&& nodeAddr.get("vejnavn").has("navn"))
					address2 += nodeAddr.get("vejnavn").get("navn").asText();
				if (nodeAddr != null && nodeAddr.has("husnr"))
					address2 += " " + nodeAddr.get("husnr");
				String municipality2 = "";
				if (nodeAddr != null && nodeAddr.has("kommune")
						&& nodeAddr.get("kommune").has("navn")
						&& nodeAddr.get("postnummer").has("nr"))
					municipality2 += nodeAddr.get("postnummer").get("nr")
							.asText()
							+ " "
							+ nodeAddr.get("kommune").get("navn").asText();
				final String tmpAddres1 = address1;
				final String tmpAddres2 = address2;
				final String tmpMunicipality2 = municipality2;
				BreakRouteActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateAddresses(tmpAddres1, tmpAddres2,
								tmpMunicipality2);
					}
				});

			}
		}).start();
		// }
	}

	private void updateAddresses(final String address1, final String address2,
			final String municipality2) {
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		if (address1 != null && address1 != "")
			textARegion.setText(address1);
		if (address2 != null && address2 != "")
			textBAddress.setText(address2);
		if (municipality2 != null && municipality2 != "")
			textBRegion.setText(municipality2);
	}

	public void onBtnBackClick(View v) {
		finish();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
	}

	public void onBtnBreakClick(View v) {
		if (selectedStationA.BStations == null
				|| selectedStationA.BStations.size() == 0
				|| ((sortedStations.get(spinner1.getSelectedItemPosition()).loc
						.getLatitude() == selectedStationA.BStations
						.get(spinner2.getSelectedItemPosition()).loc
						.getLatitude()) && (sortedStations.get(spinner1
						.getSelectedItemPosition()).loc.getLongitude()) == selectedStationA.BStations
						.get(spinner2.getSelectedItemPosition()).loc
						.getLongitude()))
			showRouteNoBreakDialog();
		else {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("start_station_lat", sortedStations
					.get(spinner1.getSelectedItemPosition()).loc.getLatitude());
			resultIntent
					.putExtra("start_station_lon", sortedStations.get(spinner1
							.getSelectedItemPosition()).loc.getLongitude());
			resultIntent.putExtra("end_station_lat", selectedStationA.BStations
					.get(spinner2.getSelectedItemPosition()).loc.getLatitude());
			resultIntent
					.putExtra("end_station_lon", selectedStationA.BStations
							.get(spinner2.getSelectedItemPosition()).loc
							.getLongitude());
			resultIntent
					.putExtra("start_stat_name", sortedStations.get(spinner1
							.getSelectedItemPosition()).name);
			resultIntent.putExtra("end_stat_name", selectedStationA.BStations
					.get(spinner2.getSelectedItemPosition()).name);
			resultIntent.putExtra("distance",
					Util.limitDecimalPlaces(newDistance, 1));
			resultIntent.putExtra("iconId", iconId);
			setResult(RESULT_ROUTE_BROKEN, resultIntent);
			finish();
			overridePendingTransition(R.anim.slide_in_left,
					R.anim.slide_out_right);
		}
	}

	private void initStrings() {
		textTitle.setTypeface(CykelsuperstierApplication.getBoldFont());
		textTitle.setText(CykelsuperstierApplication
				.getString("break_route_title"));
		textDistance.setTypeface(CykelsuperstierApplication.getBoldFont());
		textDistance.setText(CykelsuperstierApplication
				.getString("break_route_header_title"));
		textDistanceKm.setTypeface(CykelsuperstierApplication.getNormalFont());
		textDistanceKm.setText(" " + (distance / 1000) + " km");
		btnBreak.setTypeface(CykelsuperstierApplication.getNormalFont());
		btnBreak.setText(CykelsuperstierApplication
				.getString("btn_break_route"));
		textAAddress.setTypeface(CykelsuperstierApplication.getBoldFont());
		textAAddress.setText(source);
		textBAddress.setTypeface(CykelsuperstierApplication.getBoldFont());
		textBAddress.setText(destination);
		btnRejseplanen.setTypeface(CykelsuperstierApplication.getBoldFont());
		btnRejseplanen.setText("      "
				+ CykelsuperstierApplication.getString("route_plan_button"));
		textARegion.setTypeface(CykelsuperstierApplication.getNormalFont());
		textBRegion.setTypeface(CykelsuperstierApplication.getNormalFont());
		textDistance1.setTypeface(CykelsuperstierApplication.getNormalFont());
		textDistance2.setTypeface(CykelsuperstierApplication.getNormalFont());
	}

	public void onBtnRejsplanenClick(View v) {
		Intent i = new Intent(this, TransportationActivity.class);
		i.putExtra("fromStation", selectedStationA.name);
		i.putExtra("toStation", selectedStationA.BStations.get(0).name);
		String line = selectedStationA.line;
		i.putExtra("line", line);
		i.putExtra("lineB", selectedStationA.BStations.get(0).line);
		i.putExtra("destX", selectedStationB.longitude);
		i.putExtra("destY", selectedStationB.lattitude);
		i.putExtra("timeToAStation", timeToAStation);
		startActivity(i);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	private void parseStationsFromJson() {
		metroStations = new ArrayList<OverlayData>();
		sTrainStations = new ArrayList<OverlayData>();
		localTrainStations = new ArrayList<OverlayData>();
		try {
			String stationsStr = Util.stringFromJsonAssets(this,
					"stations/stations.json");
			JSONArray stationsJson = (new JSONObject(stationsStr))
					.getJSONArray("stations");
			for (int i = 0; i < stationsJson.length(); i++) {
				JSONObject stationJson = (JSONObject) stationsJson.get(i);
				if (!stationJson.has("coords"))
					continue;
				String[] coords = stationJson.getString("coords").split("\\s+");
				String type = stationJson.getString("type");
				if (type.equals("service")) {
					continue;
				} else if (type.equals("metro")) {
					metroStations.add(new OverlayData(stationJson
							.getString("name"), stationJson.getString("line"),
							Double.parseDouble(coords[1]), Double
									.parseDouble(coords[0]),
							R.drawable.metro_logo_pin));
				} else if (type.equals("s-train")) {
					sTrainStations.add(new OverlayData(stationJson
							.getString("name"), stationJson.getString("line"),
							Double.parseDouble(coords[1]), Double
									.parseDouble(coords[0]),
							R.drawable.list_subway_icon));
				} else if (type.equals("local-train")) {
					localTrainStations.add(new OverlayData(stationJson
							.getString("name"), stationJson.getString("line"),
							Double.parseDouble(coords[1]), Double
									.parseDouble(coords[0]),
							R.drawable.list_subway_icon));
				}
			}
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
	}

}
