// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.break_rote;

import com.spoiledmilk.ibikecph.util.LOG;

public class TimetableData {

	private String departureTime;
	private String arrivalTime;

	public TimetableData(String departureTime, String arrivalTime) {
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	@Override
	public String toString() {
		return departureTime + " > " + arrivalTime;
	}

	public String getTime() {
		LOG.d("Rejsplanen departure = " + departureTime + " arrival = " + arrivalTime);
		String ret = "";
		try {
			String[] splitted = departureTime.split(":");
			int departureHour = Integer.parseInt(splitted[0]);
			int departureMinute = Integer.parseInt(splitted[1]);
			splitted = arrivalTime.split(":");
			int arrivalHour = Integer.parseInt(splitted[0]);
			int arrivalMinute = Integer.parseInt(splitted[1]);
			int timeMinute = 0;
			int timeHour = 0;
			if (arrivalHour > departureHour)
				timeHour = arrivalHour - departureHour;
			if (arrivalMinute >= departureMinute)
				timeMinute = arrivalMinute - departureMinute;
			else {
				timeHour--;
				timeMinute = 60 - departureMinute + arrivalMinute;
			}
			ret = (timeHour < 10 ? "0" + timeHour : "" + timeHour) + ":" + (timeMinute < 10 ? "0" + timeMinute : "" + timeMinute);
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
		return ret;
	}

}
