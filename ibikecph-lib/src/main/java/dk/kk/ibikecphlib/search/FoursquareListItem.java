// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.navigation.routing_engine.SMLocationManager;
import dk.kk.ibikecphlib.util.Util;

import dk.kk.ibikecphlib.util.Util;

public class FoursquareListItem extends SearchListItem {

    public FoursquareListItem(JsonNode jsonNode) {
        super(jsonNode, nodeType.FOURSQUARE);
        address.setSource(Address.Source.FOURSQUARE);

        if (SMLocationManager.getInstance().hasValidLocation()) {
            distance = SMLocationManager
                    .getInstance()
                    .getLastValidLocation()
                    .distanceTo(
                            Util.locationFromCoordinates(jsonNode.get("location").get("lat").asDouble(), jsonNode.get("location")
                                    .get("lng").asDouble()));
        }

        String name = jsonNode.has("name") ? jsonNode.get("name").asText() : "";
        address.setName(name);
        JsonNode locationNode = jsonNode.get("location");
        if(locationNode != null) {
            if (locationNode.has("address")) {
                String fullAddress = locationNode.get("address").asText();
                address.setStreet(AddressParser.addresWithoutNumber(fullAddress));
                address.setHouseNumber(AddressParser.numberFromAddress(fullAddress));
            }
            if (locationNode.has("postalCode")) {
                String zip = locationNode.get("postalCode").asText();
                address.setZip(zip);
            }
            if (locationNode.has("city")) {
                String city = locationNode.get("city").asText();
                address.setCity(city);
            }
        }

        if (jsonNode.has("location") && jsonNode.get("location").has("lat") && jsonNode.get("location").has("lng")) {
            double latitude = jsonNode.get("location").get("lat").asDouble();
            double longitude = jsonNode.get("location").get("lng").asDouble();
            address.setLocation(new LatLng(latitude, longitude));
        }
    }

    @Override
    public String getSubSource() {
        return null;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.search_location_icon;
    }

}
