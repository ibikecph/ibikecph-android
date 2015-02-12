// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.os.Parcel;
import android.os.Parcelable;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.SearchListItem;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A model class keeping information of a favorite.
 * @author jens
 *
 */
public class FavoritesData extends SearchListItem implements Parcelable {

	public static final String favHome = "home";
	public static final String favWork = "work";
	public static final String favFav = "favorite";
	public static final String favSchool = "school";

	private int id = -1;
	private String name;
	private String address;
	private static String source = "favourites";
	private String subSource;
	private int apiId;

	public FavoritesData(String name, String address, String subSource,
			double lattitude, double longitude, int apiId) {
		super(nodeType.FAVORITE);
		this.name = name;
		this.address = address;
		this.subSource = subSource;
		this.latitude = lattitude;
		this.longitude = longitude;
		this.apiId = apiId;
	}

	public FavoritesData(int id, String name, String address, String subSource,
			double lattitude, double longitude, int apiId) {
		super(nodeType.FAVORITE);
		this.id = id;
		this.name = name;
		this.address = address;
		this.subSource = subSource;
		this.latitude = lattitude;
		this.longitude = longitude;
		this.apiId = apiId;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getAdress() {
		if (address == null || address.length() == 0)
			return name;
		else
			return address;
	}

	public void setAdress(String address) {
		this.address = address;
	}

	@Override
	public String getStreet() {
		return address;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public String getZip() {
		return address;
	}

	@Override
	public String getCity() {
		return address;
	}

	@Override
	public String getCountry() {
		return address;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getSubSource() {
		return subSource;
	}

	public void setSubSource(String subSource) {
		this.subSource = subSource;
	}

	@Override
	public int getIconResourceId() {
		int ret = R.drawable.fav_star_grey_small;
		if (subSource.equals(favHome))
			ret = R.drawable.fav_home_grey;
		else if (subSource.equals(favWork))
			ret = R.drawable.fav_work_grey;
		else if (subSource.equals(favSchool))
			ret = R.drawable.fav_school_grey;
		return ret;
	}

	public int getPadding() {
		int ret = 0;
		if (subSource.equals(favHome))
			ret = Util.dp2px(2);
		else if (subSource.equals(favWork))
			ret = Util.dp2px(2);
		else if (subSource.equals(favSchool))
			ret = Util.dp2px(8);
		return ret;
	}

	public int getId() {
		return id;
	}

	public FavoritesData(Parcel in) {
		super(nodeType.FAVORITE);
		String[] data = new String[6];
		in.readStringArray(data);
		try {
			this.id = Integer.parseInt(data[0]);
		} catch (Exception e) {

		}
		this.name = data[1];
		this.address = data[2];
		this.subSource = data[3];
		
		// This is a horrible hack. /jc
		this.latitude = Double.parseDouble(data[4]);
		this.longitude = Double.parseDouble(data[5]);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] { this.id + "", this.name, this.address, this.subSource, Double.toString(this.latitude), Double.toString(this.longitude) });
	}

	public static final Parcelable.Creator<FavoritesData> CREATOR = new Parcelable.Creator<FavoritesData>() {
		public FavoritesData createFromParcel(Parcel in) {
			return new FavoritesData(in);
		}

		public FavoritesData[] newArray(int size) {
			return new FavoritesData[size];
		}
	};

	public int getApiId() {
		return apiId;
	}

	public void setApiId(int apiId) {
		this.apiId = apiId;
	}

	public void setId(long id) {
		this.id = (int) id;
	}

	@Override
	public String toString() {
		return name + "\n";
	}

	@Override
	public String getFormattedNameForSearch() {
		return name;
	}

	@Override
	public String getOneLineName() {
		return getName();
	}

}
