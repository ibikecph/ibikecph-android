// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.Serializable;

/**
 * Contains address information used in searches.
 * 
 * TODO: Abstract this so it contains an enum describing where the data came
 * from originally, instead on relying on heuristics like the ones below.
 * @author jens
 *
 */

public class Address implements Serializable {
    public String street;
    public String houseNumber;
    public String zip;
    public String city;
    public double lat;
    public double lon;

    public ILatLng getLocation() {
        return location;
    }

    private ILatLng location;

    public Address() {

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
        return this.street + " " + this.houseNumber;
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