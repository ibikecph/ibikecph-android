// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.SearchListItem;
import com.spoiledmilk.ibikecph.util.Util;
import com.spoiledmilk.ibikecph.search.Address;

/**
 * A model class keeping information of a favorite.
 * @author spoiled milk
 *
 */
public class FavoriteListItem extends SearchListItem implements Parcelable {

    // TODO: Heard about Enumerable?
	public static final String favHome = "home";
	public static final String favWork = "work";
	public static final String favFav = "favorite";
	public static final String favSchool = "school";

	private int id = -1;
	private static String source = "favourites";
	private String subSource;
	private int apiId;

	public FavoriteListItem(String name, String address, String subSource,
							double latitude, double longitude, int apiId) {
		super(nodeType.FAVORITE);
		this.address = Address.fromFullAddress(address);
		this.address.setName(name);
		this.address.setLocation(new LatLng(latitude, longitude));
		this.subSource = subSource;
		this.apiId = apiId;
	}

	public FavoriteListItem(int id, String name, String address, String subSource,
							double latitude, double longitude, int apiId) {
		super(nodeType.FAVORITE);
		this.id = id;
		this.address = Address.fromFullAddress(address);
		this.address.setName(name);
		this.address.setLocation(new LatLng(latitude, longitude));
		this.subSource = subSource;
		this.apiId = apiId;
	}

    public static FavoriteListItem fromAddress(Address a) {
        FavoriteListItem fd = new FavoriteListItem(
                a.getName(),
                a.getFullAddress(),
                "Address",
                a.getLocation().getLatitude(),
                a.getLocation().getLongitude(),
                -1);
        return fd;
    }

	@Override
	public int getOrder() {
		return 0;
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
		return R.drawable.fav_star;
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

	public FavoriteListItem(Parcel in) {
		super(nodeType.FAVORITE);
		String[] data = new String[7];
		in.readStringArray(data);
		try {
			this.id = Integer.parseInt(data[0]);
		} catch (Exception e) {

		}
		this.address = Address.fromFullAddress(data[2]);
		this.address.setName(data[1]);
		this.subSource = data[3];
		
		// This is a horrible hack. /jc
		this.address.setLocation(new LatLng(Double.parseDouble(data[4]), Double.parseDouble(data[5])));
		this.apiId = Integer.parseInt(data[6]);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] { 
				Integer.toString(this.id), 
				this.getAddress().getName(),
				this.getAddress().getFullAddress(),
				this.subSource, 
				Double.toString(this.address.getLocation().getLatitude()),
				Double.toString(this.address.getLocation().getLongitude()),
				Integer.toString(this.apiId) 
		});
	}

	public static final Parcelable.Creator<FavoriteListItem> CREATOR = new Parcelable.Creator<FavoriteListItem>() {
		public FavoriteListItem createFromParcel(Parcel in) {
			return new FavoriteListItem(in);
		}

		public FavoriteListItem[] newArray(int size) {
			return new FavoriteListItem[size];
		}
	};

	public int getApiId() {
		return apiId;
	}

	public void setId(long id) {
		this.id = (int) id;
	}

}
