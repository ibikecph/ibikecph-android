// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.break_rote;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class MetroData implements ITransportationInfo {

	private static final String filename = "metro_timetable.json";

	private int time;
	private String arrivalTime;
	private String destinationTime;

	public MetroData(String arrivalTime, String destinationTime, int time) {
		this.arrivalTime = arrivalTime;
		this.destinationTime = destinationTime;
		this.time = time;
	}

	@Override
	public int getTime() {
		return time;
	}

	@Override
	public String getArrivalTime() {
		return arrivalTime;
	}

	@Override
	public String getDestinationTime() {
		return destinationTime;
	}

	public static boolean hasLine(String line, Context context) {
		boolean ret = false;
		String bufferString = Util.stringFromJsonAssets(context, "stations/" + filename);
		JsonNode actualObj = Util.stringToJsonNode(bufferString);
		JsonNode lines = actualObj.get("lines");
		for (int i = 0; i < lines.size(); i++) {
			JsonNode lineJson = lines.get(i);
			if (lineJson.get("name").asText().equalsIgnoreCase(line)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	public static ArrayList<ITransportationInfo> getNext3Arrivals(String line, String startStation, String endStation, Context context) {
		ArrayList<ITransportationInfo> ret = new ArrayList<ITransportationInfo>();
		String bufferString = Util.stringFromJsonAssets(context, "stations/" + filename);
		JsonNode actualObj = Util.stringToJsonNode(bufferString);
		JsonNode lines = actualObj.get("lines");
		for (int i = 0; i < lines.size(); i++) {
			JsonNode lineJson = lines.get(i);
			if (lineJson.get("name").asText().contains(line)) {
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DAY_OF_WEEK);
				if (day == 1)
					day = 6;
				else
					day -= 2;
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				int period = 0, endTime = 0; // startTime
				try {
					if ((day == 4 || day == 5) && hour >= 0 && hour <= 7) { // Friday and Saturday night
						period = lineJson.get("timetable").get("friday_saturday_night").get(0).get("period").asInt();
						endTime = lineJson.get("timetable").get("friday_saturday_night").get(0).get("end_time").asInt();
					} else if ((day < 4 || day == 6) && hour >= 0 && hour <= 5) { // other days at night
						JsonNode timetableNode = lineJson.get("timetable");
						JsonNode timetableNodeContainer = timetableNode.get("sunday_thursday_night");
						period = timetableNodeContainer.get(0).get("period").asInt();
						endTime = timetableNodeContainer.get(0).get("end_time").asInt();
					} else {
						JsonNode nodeRushour = lineJson.get("timetable").get("weekdays_rushour");
						JsonNode nodeOutsideRushour = lineJson.get("timetable").get("weekdays_outside_rushour");
						if ((day >= 0 && day < 5) && hour >= nodeRushour.get(0).get("start_time").asInt()
								&& hour < nodeRushour.get(0).get("end_time").asInt()) {
							endTime = nodeRushour.get(0).get("end_time").asInt();
							period = nodeRushour.get(0).get("period").asInt();
						} else if ((day >= 0 && day < 5) && hour >= nodeRushour.get(1).get("start_time").asInt()
								&& hour < nodeRushour.get(1).get("end_time").asInt()) {
							endTime = nodeRushour.get(1).get("end_time").asInt();
							period = nodeRushour.get(1).get("period").asInt();
						} else if (hour >= nodeOutsideRushour.get(0).get("start_time").asInt()
								&& hour < nodeOutsideRushour.get(0).get("end_time").asInt()) {
							endTime = nodeOutsideRushour.get(0).get("end_time").asInt();
							period = nodeOutsideRushour.get(0).get("period").asInt();
						} else {
							endTime = nodeOutsideRushour.get(1).get("end_time").asInt();
							period = nodeOutsideRushour.get(1).get("period").asInt();
						}

					}
					int currentHour = hour;
					int currentMinute = minute;
					int count = 0;
					period *= getTimeBetweenStations(line, startStation, endStation, context);
					while (count < 3) {
						for (int k = 0; k < 60 && count < 3; k += period % 60) {
							if (k >= currentMinute) {
								currentMinute = k % 60;
								currentHour += period / 60;
								String arrivalTime = (currentHour < 10 ? "0" + currentHour : "" + currentHour) + ":"
										+ (currentMinute < 10 ? "0" + currentMinute : "" + currentMinute);
								int destHour = currentHour + period / 60;
								int destMinute = currentMinute + period % 60;
								if (destMinute > 59) {
									destHour += destMinute / 60;
									destMinute = destMinute % 60;
								}
								String destTime = (destHour < 10 ? "0" + destHour : "" + destHour) + ":"
										+ (destMinute < 10 ? "0" + destMinute : "" + destMinute);
								ret.add(new MetroData(arrivalTime, destTime, period));
								count++;
							}
						}
						currentMinute = 0;
						if ((++currentHour) >= endTime)
							break;
					}
				} catch (Exception e) {
					LOG.e(e.getLocalizedMessage());
				}
				break;
			}
		}
		return ret;
	}

	private static int getTimeBetweenStations(String line, String startStation, String endStation, Context context) {
		int ret = 1;
		try {
			String bufferString = Util.stringFromJsonAssets(context, "stations/metro_stations.json");
			JsonNode actualObj = Util.stringToJsonNode(bufferString);
			JsonNode stationsNode = line.contains("M1") ? actualObj.get("M1") : actualObj.get("M2");
			int i1 = 0, i2 = 1;
			for (int i = 0; i < stationsNode.size(); i++) {
				if (stationsNode.get(i).get("name").asText().equals(startStation))
					i1 = i;
				else if (stationsNode.get(i).get("name").asText().equals(endStation))
					i2 = i;
			}
			ret = Math.abs(i2 - i1);
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
		return ret;
	}
}
