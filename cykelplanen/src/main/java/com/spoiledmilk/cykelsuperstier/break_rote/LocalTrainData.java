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

public class LocalTrainData implements ITransportationInfo {

	private int time = 0;
	private String arrivalTime = "";
	private String destinationTime = "";

	private static final int DEPARTURE = 0;
	private static final int ARRIVAL = 1;

	public LocalTrainData(String arrivalTime, String destinationTime, int time) {
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
		String lineNumber = line.split(" ")[0];
		String jsonStr = Util.stringFromJsonAssets(context, "stations/local-trains-timetable.json");
		JsonNode root = Util.stringToJsonNode(jsonStr);
		JsonNode lines = root.get("local-trains");
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).get("line").asText().trim().equalsIgnoreCase(lineNumber.trim()))
				return true;
		}
		return false;
	}

	public static ArrayList<ITransportationInfo> getNext3Arrivals(String fromStation, String toStation, String line, Context context) {
		ArrayList<ITransportationInfo> ret = new ArrayList<ITransportationInfo>();
		String lineNumber = line.split(" ")[0];
		String jsonStr = Util.stringFromJsonAssets(context, "stations/local-trains-timetable.json");
		JsonNode root = Util.stringToJsonNode(jsonStr);
		JsonNode lines = root.get("local-trains");
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).get("line").asText().trim().equalsIgnoreCase(lineNumber.trim())) {
				JsonNode stationsArray = lines.get(i).get("stations");
				int direction = -1, index1 = -1, index2 = -1;
				for (int j = 0; j < stationsArray.size(); j++) {
					if (stationsArray.get(j).asText().trim().equalsIgnoreCase(fromStation.trim()) && index1 < 0) {
						index1 = j;
						if (direction < 0)
							direction = DEPARTURE;
						else
							break;
					}
					if (stationsArray.get(j).asText().trim().equalsIgnoreCase(toStation.trim()) && index2 < 0) {
						index2 = j;
						if (direction < 0)
							direction = ARRIVAL;

						else
							break;
					}
				}
				if (direction < 0 || index1 < 0 || index2 < 0)
					break;
				JsonNode timetableNodeContainer = direction == DEPARTURE ? lines.get(i).get("departure") : lines.get(i).get("arrival");
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DAY_OF_WEEK);
				if (day == 1)
					day = 6;
				else
					day -= 2;
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				JsonNode timeTableNode;
				if (day >= 0 && day <= 4) {
					timeTableNode = timetableNodeContainer.get("weekdays");
					if (timeTableNode != null)
						getTimesForWeekdays(hour, minute, ret, timeTableNode, index1, index2, stationsArray.size());
				} else if (day == 5) { // Saturday
					timeTableNode = timetableNodeContainer.get("weekend").get("saturday");
					if (timeTableNode != null) {
						JsonNode dataNodeArray = timetableNodeContainer.get("weekend").get("data-table");
						getTimesForWeekend(hour, minute, ret, timeTableNode, index1, index2, stationsArray.size(), dataNodeArray);
					}
				} else { // Saturday
					timeTableNode = timetableNodeContainer.get("weekend").get("sunday");
					if (timeTableNode != null) {
						JsonNode dataNodeArray = timetableNodeContainer.get("weekend").get("data-table");
						getTimesForWeekend(hour, minute, ret, timeTableNode, index1, index2, stationsArray.size(), dataNodeArray);
					}
				}
				if (timeTableNode == null)
					break;

				break;
			}
		}
		return ret;
	}

	private static void getTimesForWeekdays(int hour, int minute, ArrayList<ITransportationInfo> times, JsonNode timeTableNode, int index1,
			int index2, int numOfStations) {
		ArrayList<TimeData> allTimes = new ArrayList<TimeData>();
		for (int i = 0; i < timeTableNode.size(); i++) {
			JsonNode intervalNode = timeTableNode.get(i);
			try {
				double currentTime = ((double) hour) + ((double) minute) / 100d;
				double startInterval = intervalNode.get("start-time").asDouble();
				double endInterval = intervalNode.get("end-time").asDouble();
				if (endInterval < 1.0)
					endInterval += 24.0;
				if (currentTime >= startInterval && currentTime <= endInterval) {
					JsonNode dataNode = intervalNode.get("data");
					int[] minutesArray = new int[dataNode.size()];
					for (int j = 0; j < minutesArray.length; j++) {
						minutesArray[j] = dataNode.get(j).asInt();
					}
					int currentHour = hour;
					int currentIndex = 0;
					int count = 0;
					while (currentHour <= endInterval && count < 3 && currentIndex < minutesArray.length) {
						double time1 = minutesArray[currentIndex + index1];
						double time2 = minutesArray[currentIndex + index2];
						if (time1 < 0 || time2 < 0)
							continue;
						time1 /= 100d;
						time1 += currentHour;
						time2 /= 100d;
						time2 += currentHour;
						if (time2 < time1)
							time2 += 1.0;
						allTimes.add(new TimeData(time1, time2));
						currentHour++;
						count++;
						currentIndex += numOfStations;
					}
				}
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}

		// sort the list
		for (int i = 0; i < allTimes.size() - 1; i++) {
			for (int j = i + 1; j < allTimes.size(); j++) {
				TimeData td1 = allTimes.get(i);
				TimeData td2 = allTimes.get(j);
				if (td1.stationAtime > td2.stationAtime) {
					allTimes.set(i, td2);
					allTimes.set(j, td1);
				}
			}
		}

		LOG.d("sorted times = " + allTimes.toString());

		// return the 3 results
		for (int i = 0; i < 3 && i < allTimes.size(); i++) {
			TimeData temp = allTimes.get(i);
			int time = 0;
			int minutes1 = (int) ((temp.stationAtime - (double) ((int) temp.stationAtime)) * 100);
			int minutes2 = (int) ((temp.stationBtime - (double) ((int) temp.stationBtime)) * 100);
			time = (((int) temp.stationBtime) - ((int) temp.stationAtime)) * 60;
			if (minutes2 >= minutes1)
				time = minutes2 - minutes1;
			else {
				time += 60 - minutes1 + minutes2;
				time -= 60;
			}
			String formattedTime1 = (((int) temp.stationAtime) < 10 ? "0" + ((int) temp.stationAtime) : "" + ((int) temp.stationAtime))
					+ ":" + (minutes1 < 10 ? "0" + minutes1 : "" + minutes1);
			String formattedTime2 = (((int) temp.stationBtime) < 10 ? "0" + ((int) temp.stationBtime) : "" + ((int) temp.stationBtime))
					+ ":" + (minutes2 < 10 ? "0" + minutes2 : "" + minutes2);
			times.add(new LocalTrainData(formattedTime1, formattedTime2, time));
		}

	}

	private static void getTimesForWeekend(int hour, int minute, ArrayList<ITransportationInfo> times, JsonNode timeTableNode, int index1,
			int index2, int numOfStations, JsonNode dataNodeArray) {
		ArrayList<TimeData> allTimes = new ArrayList<TimeData>();
		for (int i = 0; i < timeTableNode.size(); i++) {
			JsonNode intervalNode = timeTableNode.get(i);
			try {
				double currentTime = ((double) hour) + ((double) minute) / 100d;
				double startInterval = intervalNode.get("start-time").asDouble();
				double endInterval = intervalNode.get("end-time").asDouble();
				if (endInterval < 1.0)
					endInterval += 24.0;
				if (currentTime >= startInterval && currentTime <= endInterval) {
					JsonNode dataNode = dataNodeArray.get(i);
					int[] minutesArray = new int[dataNode.size()];
					for (int j = 0; j < minutesArray.length; j++) {
						minutesArray[j] = dataNode.get(j).asInt();
					}
					int currentHour = hour;
					int currentIndex = 0;
					int count = 0;
					while (currentHour <= endInterval && count < 3 && currentIndex < minutesArray.length) {
						double time1 = minutesArray[currentIndex + index1];
						double time2 = minutesArray[currentIndex + index2];
						if (time1 < 0 || time2 < 0)
							continue;
						time1 /= 100d;
						time1 += currentHour;
						time2 /= 100d;
						time2 += currentHour;
						if (time2 < time1)
							time2 += 1.0;
						allTimes.add(new TimeData(time1, time2));
						currentHour++;
						count++;
						currentIndex += numOfStations;
					}
				}
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}

		// sort the list
		for (int i = 0; i < allTimes.size() - 1; i++) {
			for (int j = i + 1; j < allTimes.size(); j++) {
				TimeData td1 = allTimes.get(i);
				TimeData td2 = allTimes.get(j);
				if (td1.stationAtime > td2.stationAtime) {
					allTimes.set(i, td2);
					allTimes.set(j, td1);
				}
			}
		}

		LOG.d("sorted times = " + allTimes.toString());

		// return the 3 results
		for (int i = 0; i < 3 && i < allTimes.size(); i++) {
			TimeData temp = allTimes.get(i);
			int time = 0;
			int minutes1 = (int) ((temp.stationAtime - (double) ((int) temp.stationAtime)) * 100);
			int minutes2 = (int) ((temp.stationBtime - (double) ((int) temp.stationBtime)) * 100);
			time = (((int) temp.stationBtime) - ((int) temp.stationAtime)) * 60;
			if (minutes2 >= minutes1)
				time = minutes2 - minutes1;
			else {
				time += 60 - minutes1 + minutes2;
				time -= 60;
			}
			String formattedTime1 = (((int) temp.stationAtime) < 10 ? "0" + ((int) temp.stationAtime) : "" + ((int) temp.stationAtime))
					+ ":" + (minutes1 < 10 ? "0" + minutes1 : "" + minutes1);
			String formattedTime2 = (((int) temp.stationBtime) < 10 ? "0" + ((int) temp.stationBtime) : "" + ((int) temp.stationBtime))
					+ ":" + (minutes2 < 10 ? "0" + minutes2 : "" + minutes2);
			times.add(new LocalTrainData(formattedTime1, formattedTime2, time));
		}

	}

	static class TimeData {
		public double stationAtime;
		public double stationBtime;

		public TimeData(double i, double j) {
			stationAtime = i;
			stationBtime = j;
		}

		public String toString() {
			return stationAtime + " - " + stationBtime;
		}
	}

}
