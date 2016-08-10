// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.ibikecph.search;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class HistoryListItem extends SearchListItem {

	private int id;
	private String startDate;
	private String endDate;
	private String subSource;

	public HistoryListItem(int id, String name, String address, String startDate, String endDate, double latitude, double longitude) {
		super(nodeType.HISTORY);
		this.id = id;
		this.address = Address.fromFullAddress(address);
		this.address.setName(name);
		this.address.setLocation(new LatLng(latitude, longitude));
		this.address.setSource(Address.Source.HISTORYDATA);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public HistoryListItem(int id, String name, String address, String startDate, String endDate, String source, String subSource, double latitude, double longitude) {
		this(id, name, address, startDate, endDate, latitude, longitude);
		this.subSource = subSource;
	}

	@Override
	public int getOrder() {
		return 1;
	}

	@Override
	public String getSubSource() {
		return this.subSource;
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
}
