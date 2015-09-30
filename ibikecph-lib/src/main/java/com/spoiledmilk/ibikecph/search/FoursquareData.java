// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.location.Location;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.Util;

public class FoursquareData extends SearchListItem {

    private String name = "";
    private String address = "";
    private String street = "";
    private String zip = "";
    private String city;

    public FoursquareData(JsonNode jsonNode) {
        super(jsonNode, nodeType.FOURSQUARE);
        if (SMLocationManager.getInstance().hasValidLocation()) {
            distance = SMLocationManager
                    .getInstance()
                    .getLastValidLocation()
                    .distanceTo(
                            Util.locationFromCoordinates(jsonNode.get("location").get("lat").asDouble(), jsonNode.get("location")
                                    .get("lng").asDouble()));
        }

        name = jsonNode.has("name") ? jsonNode.get("name").asText() : "";
        if (jsonNode.has("location") && jsonNode.get("location").has("address")) {
            street = jsonNode.get("location").get("address").asText();// .replaceAll("\\s",
            // "");
        }
        if (jsonNode.has("location") && jsonNode.get("location").has("postalCode")) {
            zip = jsonNode.get("location").get("postalCode").asText();// .replaceAll("\\s",
            // "");
        }
        if (jsonNode.has("location") && jsonNode.get("location").has("city")) {
            city = jsonNode.get("location").get("city").asText();// .replaceAll("\\s",
            // "");
        }
        address = street + ", " + zip + " " + city;
        if (jsonNode.has("location") && jsonNode.get("location").has("lat") && jsonNode.get("location").has("lng")) {
            latitude = jsonNode.get("location").get("lat").asDouble();
            longitude = jsonNode.get("location").get("lng").asDouble();
        }
    }

    public String getName() {
        return name;
    }

    public String getAdress() {
        return address;
    }

    public String getStreet() {
        return street;
    }

    public int getOreder() {
        return 2;
    }

    public String getZip() {
        String ret = "";
        if (type == nodeType.KORTFOR) {
            ret = jsonNode.get("postnummer").get("nr").asText();

        } else if (type == nodeType.FOURSQUARE) {
            ret = zip;
        }
        return ret;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        String ret = "";
        if (jsonNode.has("location") && jsonNode.get("location").has("country"))
            ret = jsonNode.get("location").get("country").asText();// .replaceAll("\\s",
        // "");
        return ret;
    }

    public String getSource() {
        String ret = "";
        ret = "autocomplete";
        return ret;
    }

    public String getSubSource() {
        String ret = "";
        ret = "foursquare";
        return ret;
    }

    public int getOrder() {
        return 2;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.search_location_icon;
    }

    public String getFormattedAddress() {
        String address = getAdress();
        int index1 = address.indexOf(",");
        if (index1 > 0)
            address = address.substring(0, index1);
        int index2 = -1;
        for (int i = 0; i < address.length(); i++) {
            if (address.charAt(i) == '0' || address.charAt(i) == '1' || address.charAt(i) == '2' || address.charAt(i) == '3'
                    || address.charAt(i) == '4' || address.charAt(i) == '5' || address.charAt(i) == '6' || address.charAt(i) == '7'
                    || address.charAt(i) == '8' || address.charAt(i) == '9') {
                index2 = i;
                break;
            }
        }
        if (index2 > 0)
            address = address.substring(0, index2);
        // address = address.replaceAll("[0-9]", "");
        address += " , " + getCity() + " , " + getCountry();
        return address;
    }

    @Override
    public String getFormattedNameForSearch() {
        return name + (getAdress() != null && !getAdress().equals("") ? "\n" + getZip() + " " + getAdress() : "");
    }

    @Override
    public String getOneLineName() {
        return getName() + (getAdress() != null && !getAdress().equals("") ? ", " + getZip() + ", " + getAdress() : "");
    }

    public static double distance(Location loc, JsonNode jsonNode) {
        double ret = Double.MAX_VALUE;
        if (jsonNode.has("location") && jsonNode.get("location").has("lat") && jsonNode.get("location").has("lng")) {
            double latitude = jsonNode.get("location").get("lat").asDouble();
            double longitude = jsonNode.get("location").get("lng").asDouble();
            ret = loc.distanceTo(Util.locationFromCoordinates(latitude, longitude));
        }
        return ret;
    }

}
