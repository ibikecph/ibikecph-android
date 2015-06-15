// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

public class HistoryData extends SearchListItem {

	private int id;
	private String name;
	private String address;
	private String startDate;
	private String endDate;
	private String source;
	private String subSource;

	public HistoryData(int id, String name, String address, String startDate, String endDate, String source, String subSource,
			double latitude, double longitude) {
		super(nodeType.HISTORY);
		this.id = id;
		this.name = name;
		this.address = address;
		this.startDate = startDate;
		this.endDate = endDate;
		this.source = source;
		this.subSource = subSource;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public HistoryData(String name, double latitude, double longitude) {
		super(nodeType.HISTORY);
		this.id = -1;
		this.name = name;
		this.address = "";
		this.startDate = "";
		this.endDate = "";
		this.source = "history";
		this.subSource = "history";
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public HistoryData(String address, String name, double latitude, double longitude) {
		super(nodeType.HISTORY);
		this.address = address;
		this.name = name;
		this.startDate = "";
		this.endDate = "";
		this.source = "history";
		this.subSource = "history";
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getAdress() {
		if (address == null || address.length() == 0)
			return name;
		else
			return address;
	}

	@Override
	public String getStreet() {
		return address;
	}

	@Override
	public int getOrder() {
		return 1;
	}

	@Override
	public String getZip() {
		return address;
	}

	@Override
	public String getCity() {
		return "";
	}

	@Override
	public String getCountry() {
		return "";
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getSubSource() {
		return subSource;
	}

	public int getId() {
		return id;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	@Override
	public int getIconResourceId() {
		return -1;
	}

	@Override
	public String getFormattedNameForSearch() {
		return name;
	}

	@Override
	public String getOneLineName() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;

	}

	public void setAddress(String address) {
		this.address = address;

	}
}
