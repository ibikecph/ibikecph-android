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

public class STrainData implements ITransportationInfo {

	private String arrivalTime;
	private String destinationTime;
	private int time;

	private static final String filename = "timetable_strain.json";
	private static final int DEPARTURE = 0;
	private static final int ARRIVAL = 1;
	private static final int NOT_SET = -1;
	private static int direction = NOT_SET;

	public STrainData(String arrivalTime, String destinationTime, int time) {
		this.arrivalTime = arrivalTime;
		this.destinationTime = destinationTime;
		this.time = time;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public String getDestinationTime() {
		return destinationTime;
	}

	public void setDestinationTime(String destinationTime) {
		this.destinationTime = destinationTime;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public static ArrayList<ITransportationInfo> getNext3Arrivals(String stationFrom, String stationTo, String line, Context context) {
		ArrayList<ITransportationInfo> ret = new ArrayList<ITransportationInfo>();
		try {
			String bufferString = Util.stringFromJsonAssets(context, "stations/" + filename);
			JsonNode actualObj = Util.stringToJsonNode(bufferString);
			JsonNode lines = actualObj.get("timetable");

			// find the data for the current transportation line
			for (int i = 0; i < lines.size(); i++) {
				JsonNode lineJson = lines.get(i);
				String[] sublines = line.split(",");
				for (int ii = 0; ii < sublines.length; ii++) {
					if (lineJson.get("line").asText().equalsIgnoreCase(sublines[ii].trim())) {
						int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
						if (day == 1)
							day = 6;
						else
							day -= 2;
						int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						int minute = Calendar.getInstance().get(Calendar.MINUTE);
						JsonNode stationData = null;
						if ((day == 4 || day == 5) && isFridaySaturdayNight(lineJson.get("night-after-friday-saturday"), hour)) {
							// night after Friday or Saturday
							stationData = lineJson.get("night-after-friday-saturday");
						} else if (day < 5) {
							// weekdays
							stationData = lineJson.get("weekdays");
						} else {
							// weekend
							stationData = lineJson.get("weekend");
						}
						JsonNode stations = stationData.get("data");
						int[] AStationMinutes = null, BStationMinutes = null;
						// find the data for the start and end station and choose the direction
						direction = NOT_SET;
						for (int j = 0; j < stations.size(); j++) {
							JsonNode st = stations.get(j);
							if (st.get("station").asText().equalsIgnoreCase(stationFrom) && direction == NOT_SET && AStationMinutes == null) {
								direction = DEPARTURE;
								AStationMinutes = getMinutesArray(st.get("departure").asText());
							} else if (st.get("station").asText().equalsIgnoreCase(stationFrom) && AStationMinutes == null) {
								AStationMinutes = getMinutesArray(st.get("arrival").asText());
								break;
							} else if (st.get("station").asText().equalsIgnoreCase(stationTo) && direction == NOT_SET
									&& BStationMinutes == null) {
								direction = ARRIVAL;
								BStationMinutes = getMinutesArray(st.get("departure").asText());
							} else if (st.get("station").asText().equalsIgnoreCase(stationTo) && BStationMinutes == null) {
								BStationMinutes = getMinutesArray(st.get("arrival").asText());
								break;
							}
						}
						if (AStationMinutes == null || BStationMinutes == null)
							continue;
						JsonNode subLines = direction == DEPARTURE ? stationData.get("departure") : stationData.get("arrival");
						JsonNode subLine = subLines.get(0);
						if (hasTrain(hour, subLine.get("night").asText(), subLine.get("day").asText())) {
							int count = 0;
							for (int k = 0; k < AStationMinutes.length; k++) {
								if (minute <= AStationMinutes[k]) {
									int arrivalHour = hour;
									int time = BStationMinutes[k] - AStationMinutes[k];
									if (AStationMinutes[k] > BStationMinutes[k]) {
										arrivalHour++;
										time = 60 - AStationMinutes[k] + BStationMinutes[k];
									}
									ret.add(new STrainData((hour < 10 ? "0" + hour : "" + hour) + ":"
											+ (AStationMinutes[k] < 10 ? "0" + AStationMinutes[k] : "" + AStationMinutes[k]),
											(arrivalHour < 10 ? "0" + arrivalHour : "" + arrivalHour) + ":"
													+ (BStationMinutes[k] < 10 ? "0" + BStationMinutes[k] : "" + BStationMinutes[k]), time));
									if (++count == 3)
										break;
								}
							}
							// second pass to get the times for the next hour
							hour++;
							for (int k = 0; k < AStationMinutes.length && count < 3; k++) {
								int arrivalHour = hour;
								int time = BStationMinutes[k] - AStationMinutes[k];
								if (AStationMinutes[k] > BStationMinutes[k]) {
									arrivalHour++;
									time = 60 - AStationMinutes[k] + BStationMinutes[k];
								}
								ret.add(new STrainData((hour < 10 ? "0" + hour : "" + hour) + ":"
										+ (AStationMinutes[k] < 10 ? "0" + AStationMinutes[k] : "" + AStationMinutes[k]),
										(arrivalHour < 10 ? "0" + arrivalHour : "" + arrivalHour) + ":"
												+ (BStationMinutes[k] < 10 ? "0" + BStationMinutes[k] : "" + BStationMinutes[k]), time));
							}
						}
						return ret;
					}
				}
			}
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}

		return ret;
	}

	private static int[] getMinutesArray(String array) {
		int[] ret;
		String[] splitted = array.split(" ");
		ret = new int[splitted.length];
		for (int i = 0; i < splitted.length; i++) {
			try {
				ret[i] = Integer.parseInt(splitted[i]);
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
		if (ret != null && ret.length > 0) {
			for (int j = 0; j < ret.length - 1; j++)
				for (int k = j + 1; k < ret.length; k++) {
					if (ret[j] > ret[k]) {
						int temp = ret[j];
						ret[j] = ret[k];
						ret[k] = temp;
					}
				}
		}
		return ret;
	}

	private static boolean hasTrain(int hours, String nightString, String dayString) {
		boolean ret = false;
		int endNight = 24, startNight = 0, endDay = 24, startDay = 0;
		try {
			String[] splittedNight = nightString.split(" ");
			endNight = Integer.parseInt(splittedNight[0].split("\\.")[0]);
			startNight = Integer.parseInt(splittedNight[1].split("\\.")[0]);
			if (startNight > endNight) {
				int temp = startNight;
				startNight = endNight;
				endNight = temp;
			}
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
		try {
			String[] splittedDay = dayString.split(" ");
			endDay = Integer.parseInt(splittedDay[1].split("\\.")[0]);
			startDay = Integer.parseInt(splittedDay[0].split("\\.")[0]);
			if (startDay > endDay) {
				int temp = startDay;
				startDay = endDay;
				endDay = temp;
			}
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
		if ((hours >= startNight && hours < endNight) || (hours >= startDay && hours < endDay))
			ret = true;
		return ret;
	}

	private static boolean isFridaySaturdayNight(JsonNode jsonNode, int hour) {
		boolean ret = false;
		try {
			String nightString = jsonNode.get("departure").get(0).get("night").asText();
			int startHour = Integer.parseInt((nightString.split(" ")[0]).split("\\.")[0]);
			int endHour = Integer.parseInt((jsonNode.get("departure").get(0).get("night").asText().split(" ")[1]).split("\\.")[0]);
			if (hour >= startHour || hour < endHour)
				ret = true;
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
		return ret;
	}

	public static boolean hasLine(String line, Context context) {
		String bufferString = Util.stringFromJsonAssets(context, "stations/" + filename);
		JsonNode actualObj = Util.stringToJsonNode(bufferString);
		JsonNode lines = actualObj.get("timetable");
		String[] sublines = line.split(",");
		for (int i = 0; i < lines.size(); i++) {
			JsonNode lineJson = lines.get(i);
			for (int j = 0; j < sublines.length; j++) {
				if (lineJson.get("line").asText().equalsIgnoreCase(sublines[j].trim())) {
					return true;
				}
			}
		}
		return false;
	}
}
