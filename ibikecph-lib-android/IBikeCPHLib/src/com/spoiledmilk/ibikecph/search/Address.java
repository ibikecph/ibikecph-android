// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

/**
 * Contains address information used in searches.
 * 
 * TODO: Abstract this so it contains an enum describing where the data came
 * from originally, instead on relying on heuristics like the ones below.
 * @author jens
 *
 */
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
