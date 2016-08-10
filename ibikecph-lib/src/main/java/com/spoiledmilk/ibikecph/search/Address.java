// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.location.Location;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.favorites.FavoriteListItem;

import java.io.Serializable;
import java.util.regex.Pattern;

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
    private String street;
    private String houseNumber;
    private String zip;
    private String city;
    private String name;
    private LatLng location;

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public boolean hasHouseNumber() {
        return this.houseNumber != null && !this.houseNumber.equals("");
    }

    public boolean hasZip() {
        return this.zip != null && !this.zip.equals("");
    }

    public boolean hasCity() {
        return this.city != null && !this.city.equals("");
    }

    public boolean hasStreet() {

        return this.street != null && !this.street.equals("");

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullAddress() {
        String result = "";

        if(hasStreet()) {
            result += getStreet();
            if(hasHouseNumber()) {
                result += " " + getHouseNumber();
            }
        }

        if(!result.isEmpty() && (hasZip() || hasCity())) {
            result += ", ";
        }

        if(hasZip() && hasCity()) {
            result += getZip() + " " + getCity();
        } else if (hasCity()) {
            result += getCity();
        }
        return result;
    }

    public void setFullAddress(String fullAddress) {
        Address parsedAddress = AddressParser.parseAddressRegex(fullAddress);
        street = parsedAddress.street;
        houseNumber = parsedAddress.houseNumber;
        zip = parsedAddress.zip;
        city = parsedAddress.city;
    }

    public enum Source {
        SEARCH, HISTORYDATA, FOURSQUARE, FAVORITE
    }

    private Source source;


    public LatLng getLocation() {

        if (isCurrent) {
            if (IBikeApplication.getService().getLastValidLocation() != null)
                return new LatLng(IBikeApplication.getService().getLastValidLocation());
        }

        return location;
    }

    public void setLocation(LatLng loc) {
        if (!isCurrent) {
            this.location = loc;
        }
    }

    public Address() {

    }

    public Address(LatLng location) {
        this.location = location;
    }

    public Address(String street, String houseNumber, String zip, String city, double lat, double lon) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.zip = zip;
        this.city = city;
        this.location = new LatLng(lat, lon);
    }

    public Address(String street, String houseNumber, String zip, String city, LatLng location) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.zip = zip;
        this.city = city;
        this.location = location;
    }

    public String getStreetAddress() {
        if (!isCurrent) {
            if (this.name != null && this.houseNumber != null && !this.name.trim().isEmpty() && !this.houseNumber.trim().isEmpty()) {
                return this.name + " " + this.houseNumber;
            } else if (this.name != null && !this.name.trim().isEmpty()) {
                return this.name;
            } else if (this.street != null && this.houseNumber != null && !this.street.trim().equals("") && !this.houseNumber.trim().equals("")) {
                //Remove "null" from the strings received from Foursquare (occurs often if you search for Tivoli)
                if (this.street.contains("null")) {
                    this.street = this.street.replace("null", "");
                }
                return this.street + " " + this.houseNumber;
            } else if (this.street != null && !this.street.trim().equals("")) {
                if (this.street.contains("null")) {
                    this.street = this.street.replace("null", "");
                }
                return this.street;
            }
        }
        return "";
    }

    public String getPostCodeAndCity() {

        if (this.zip != null & this.city != null && !this.zip.trim().equals("") && !this.city.trim().equals("")) {
            //Sometimes both zip and city contains zip AND city.. if so, only return city.
            if (this.zip.equals(this.city)) {
                return this.city;
            } else {
                return this.zip + " " + this.city;
            }
        } else if (this.city != null && !this.city.trim().equals("")) {
            return this.city;
        }
        return "";
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

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o instanceof Address) {
            Address a = (Address) o;
            boolean sameStreet = getStreet() == null ?
                                 a.getStreet() == null :
                                 getStreet().equals(a.getStreet());
            boolean sameCity = getCity() == null ?
                               a.getCity() == null :
                               getCity().equals(a.getCity());
            boolean sameZip = getZip() == null ?
                              a.getZip() == null :
                              getZip().equals(a.getZip());
            boolean sameHouseNumber = getHouseNumber() == null ?
                                      a.getHouseNumber() == null :
                                      getHouseNumber().equals(a.getHouseNumber());
            // Do they all match up?
            return sameStreet && sameCity && sameZip && sameHouseNumber;
        } else {
            return false;
        }
    }

    public static Address fromCurLoc() {

        // First we need an SMRoute. Let's create one from the address
        Location curLoc = IBikeApplication.getService().getLastValidLocation();

        // If we don't have a fresh GPS coordinate, go with the best that we have.
        if (curLoc == null) {
            curLoc = IBikeApplication.getService().getLastValidLocation();
        }

        // If we still don't have a fix, we cannot do anything for them.
        if (curLoc == null) {
            return null;
        }

        Address ret = new Address(new LatLng(curLoc));
        ret.isCurrent = true;

        return ret;
    }

    public static Address fromFullAddress(String fullAddress) {
        return AddressParser.parseAddressRegex(fullAddress);
    }

    public boolean isCurrentLocation() {
        return isCurrent;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    /**
     * Get a simple string to display the value of the address. It simply returns the primary
     * display string.
     * @return
     */
    public String getDisplayName() {
        return getPrimaryDisplayString();
    }

    public boolean hasSpecialName() {
        return this.name != null && !this.name.equals("");
    }


    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Generates the secondary string that is shown below the primary display string.
     * @return
     */
    public String getPrimaryDisplayString() {
        if(isCurrent) {
            return IBikeApplication.getString("current_position");
        } else {
            if(getName() != null && !getName().isEmpty()) {
                return getName();
            } else if(getStreet() != null && !getStreet().isEmpty()) {
                String result = getStreet();
                if(!result.isEmpty() && getHouseNumber() != null && !getHouseNumber().isEmpty()) {
                    result += " " + getHouseNumber();
                }
                return result;
            } else {
                return "?";
            }
        }
    }

    /**
     * Generates the secondary string that is shown below the primary display string.
     * @return
     */
    public String getSecondaryDisplayString() {
        String result = "";
        if(getName() != null && !getName().isEmpty() && getStreet() != null && !getStreet().isEmpty()) {
            // Move the address to the secondary string
            result += getStreet();
            if(!result.isEmpty() && getHouseNumber() != null && !getHouseNumber().isEmpty()) {
                result += " " + getHouseNumber();
            }
        }
        boolean hasZip = getZip() != null && !getZip().isEmpty();
        boolean hasCity = getCity() != null && !getCity().isEmpty();
        if(!result.isEmpty() && (hasZip || hasCity)) {
            result += ", ";
        }
        if (hasZip) {
            result += getZip();
        }
        if (hasZip && hasCity) {
            result += " ";
        }
        if (hasCity) {
            result += getCity();
        }
        return result;
    }

}
