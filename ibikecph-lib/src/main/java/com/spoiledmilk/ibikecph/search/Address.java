// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.location.Location;
import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IbikeApplication;

import java.io.Serializable;

/**
 * Contains address information used in searches.
 * <p/>
 * TODO: Abstract this so it contains an enum describing where the data came
 * from originally, instead on relying on heuristics like the ones below.
 *
 * @author jens
 */

public class Address implements Serializable {
    private boolean isCurrent = false;
    public String street;
    public String houseNumber;
    public String zip;
    public String city;
    public String name;
    public double lat;
    public double lon;
    private ILatLng location;

    public enum AddressSource {
        SEARCH, HISTORYDATA
    }

    private AddressSource addressSource;

    public ILatLng getLocation() {

        if (isCurrent) {
            if (IbikeApplication.getService().getLastValidLocation() != null)
                return new LatLng(IbikeApplication.getService().getLastValidLocation());

            if (IbikeApplication.getService().getLastKnownLocation() != null)
                return new LatLng(IbikeApplication.getService().getLastKnownLocation());
        }

        return location;
    }

    public void setLocation(ILatLng loc) {
        if (!isCurrent) {
            this.location = loc;
            this.lat = loc.getLatitude();
            this.lon = loc.getLongitude();
        }
    }


    public Address() {

    }

    public Address(ILatLng location) {
        this.location = location;
    }

    public Address(String street, String houseNumber, String zip, String city, double lat, double lon) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.zip = zip;
        this.city = city;
        this.lat = lat;
        this.lon = lon;
        this.location = new LatLng(lat, lon);
    }

    public Address(String street, String houseNumber, String zip, String city, ILatLng location) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.zip = zip;
        this.city = city;
        this.lat = location.getLatitude();
        this.lon = location.getLongitude();
        this.location = location;
    }

    public String getStreetAddress() {
        if (isCurrent) {
            return IbikeApplication.getString("current_position");
        } else {
            return this.street + " " + this.houseNumber;
        }
    }

    public String getPostCodeAndCity() {
        return this.zip + " " + this.city;
    }

    public boolean isAddress() {
        boolean ret = false;
        // if ( number != null && !number.equals("") && !number.equals("1")){
        // ret = true;
        // }
        if ((zip != null && !zip.equals("")) || (houseNumber != null && !houseNumber.equals(""))
                || (street != null && city != null && !street.equals("") && !city.equals("") && !city.equals(street))) {
            ret = true;
        }
        return ret;
    }

    public boolean isFoursquare() {
        boolean ret = true;
        if (houseNumber != null && !houseNumber.equals("")) {
            ret = false;
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        Address a = (Address) o;
        boolean ret = true;
        if (this == o) {
            ret = true;
        } else if ((a.street == null && street != null) || (a.street != null && street == null)
                || (a.street != null && street != null && !street.equals(a.street))) {
            ret = false;
        } else if ((a.city == null && city != null) || (a.city != null && city == null)
                || (a.city != null && city != null && !city.equals(a.city))) {
            ret = false;
        } else if ((a.zip == null && zip != null) || (a.zip != null && zip == null) || (a.zip != null && zip != null && !zip.equals(a.zip))) {
            ret = false;
        } else if ((a.houseNumber == null && houseNumber != null) || (a.houseNumber != null && houseNumber == null)
                || (a.houseNumber != null && houseNumber != null && !houseNumber.equals(a.houseNumber))) {
            ret = false;
        }
        return ret;
    }

    public static Address fromCurLoc() {

        // First we need an SMRoute. Let's create one from the address
        Location curLoc = IbikeApplication.getService().getLastValidLocation();

        // If we don't have a fresh GPS coordinate, go with the best that we have.
        if (curLoc == null) {
            curLoc = IbikeApplication.getService().getLastKnownLocation();
        }

        // If we still don't have a fix, we cannot do anything for them.
        if (curLoc == null) {
            return null;
        }

        Address ret = new Address(new LatLng(curLoc));
        ret.isCurrent = true;

        return ret;
    }

    public boolean isCurrentLocation() {
        return isCurrent;
    }

    public static Address fromSearchListItem(SearchListItem searchListItem) {


        Address address = new Address();


        address.setLocation(new LatLng(searchListItem.getLatitude(), searchListItem.getLongitude()));

        if (searchListItem.getZip() != null && !searchListItem.getZip().trim().equals("")) {
            address.zip = searchListItem.getZip();
        }
        if (searchListItem.getCity() != null && !searchListItem.getCity().trim().equals("")) {
            address.city = searchListItem.getCity();
        } else {
            address.city = searchListItem.getAdress();
        }

        address.street = searchListItem.getStreet();
        address.name = searchListItem.getName();
        address.houseNumber = searchListItem.getNumber();

        /*Log.d("DV", "Address, city == " + address.city);
        Log.d("DV", "Address, street == " + address.street);
        Log.d("DV", "Address, name == " + address.name);
        Log.d("DV", "Address, zip == " + address.zip);
        Log.d("DV", "Address, lat == " + address.lat);
        Log.d("DV", "Address, lon == " + address.lon);*/

        address.setAddressSource(AddressSource.SEARCH);
        return address;

    }

    public static Address fromHistoryData(HistoryData historyData) {

        Address address = new Address();

        address.name = historyData.getName();
        address.street = historyData.getAdress();
        address.setLocation(new LatLng(historyData.latitude, historyData.longitude));

        address.setAddressSource(AddressSource.HISTORYDATA);
        return address;

    }

    //fromfavoritesdata


    public AddressSource getAddressSource() {
        return addressSource;
    }

    public void setAddressSource(AddressSource addressSource) {
        this.addressSource = addressSource;
    }

}

/*
public class Address {
	public String zip = "";
	public String street = "";
	public String city = "";
	public String number = "";

	@Override
	public String toString() {
		return "street: " + street + " " + "number: " + number + " " + "city: " + city + " " + "zip: " + zip;
	}

	public boolean isAddress() {
		boolean ret = false;
		// if ( number != null && !number.equals("") && !number.equals("1")){
		// ret = true;
		// }
		if ((zip != null && !zip.equals("")) || (number != null && !number.equals(""))
				|| (street != null && city != null && !street.equals("") && !city.equals("") && !city.equals(street))) {
			ret = true;
		}
		return ret;
	}

	public boolean isFoursquare() {
		boolean ret = true;
		if (number != null && !number.equals("")) {
			ret = false;
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		Address a = (Address) o;
		boolean ret = true;
		if (this == o) {
			ret = true;
		} else if ((a.street == null && street != null) || (a.street != null && street == null)
				|| (a.street != null && street != null && !street.equals(a.street))) {
			ret = false;
		} else if ((a.city == null && city != null) || (a.city != null && city == null)
				|| (a.city != null && city != null && !city.equals(a.city))) {
			ret = false;
		} else if ((a.zip == null && zip != null) || (a.zip != null && zip == null) || (a.zip != null && zip != null && !zip.equals(a.zip))) {
			ret = false;
		} else if ((a.number == null && number != null) || (a.number != null && number == null)
				|| (a.number != null && number != null && !number.equals(a.number))) {
			ret = false;
		}
		return ret;
	}
}
*/