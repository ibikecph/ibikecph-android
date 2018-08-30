// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.geometry.LatLng;
import dk.kk.ibikecphlib.R;

/**
 * Represents search results from Kortforsyningen.
 * 
 * TODO: Wrap out the OIORest stuff into its own class.
 * Maybe resurrect this one that was removed becasuse it was unused:
 * https://github.com/ibikecph/ibikecph-lib-android/blob/master/IBikeCPHLib/src/com/spoiledmilk/ibikecph/search/OriestData.java
 * @author jens
 *
 */
public class KortforsyningenListItem extends SearchListItem {

	private int iconRes;
	private boolean isPlace;

	public KortforsyningenListItem(JsonNode jsonNode) {
		super(jsonNode, nodeType.KORTFOR);

		String streetName = "";
		if (jsonNode.get("properties").has("vej_navn")) {
			streetName = jsonNode.get("properties").get("vej_navn").asText();
		} else if (jsonNode.get("properties").has("stednavneliste")) {
			streetName = parseStedNavneListe(jsonNode.get("properties").get("stednavneliste"));
		}

		String number = "";
		if (jsonNode.get("properties").has("husnr")) {
			number = jsonNode.get("properties").get("husnr").asText();
		}

		String municipalityName = "";
		String municipalityCode = "";
		if (jsonNode.get("properties").has("postdistrikt_navn")) {
			municipalityName = jsonNode.get("properties").get("postdistrikt_navn").asText();
		} else if (jsonNode.get("properties").has("kommune_navn")) {
			municipalityName = jsonNode.get("properties").get("kommune_navn").asText();
		}
		if (jsonNode.get("properties").has("postdistrikt_kode")) {
			municipalityCode = jsonNode.get("properties").get("postdistrikt_kode").asText();

		}

		address.setStreet(streetName);
		address.setHouseNumber(number);
		address.setCity(municipalityName);
		address.setZip(municipalityCode);

		if (jsonNode.get("properties").has("afstand_afstand")) {
			distance = jsonNode.get("properties").get("afstand_afstand").asDouble();
		} else {
			throw new RuntimeException("Expected a distance from the API");
		}

		if (jsonNode.has("geometry") && !jsonNode.get("geometry").isNull() && jsonNode.get("geometry").has("ymin")) {
			Log.d("", "geometry = " + jsonNode.get("geometry"));
			double latitude = jsonNode.get("geometry").get("ymin").asDouble();
			if (jsonNode.get("geometry").has("ymax")) {
				latitude += jsonNode.get("geometry").get("ymax").asDouble();
				latitude /= 2;
			}
			double longitude = jsonNode.get("geometry").get("xmin").asDouble();
			if (jsonNode.get("geometry").has("xmax")) {
				longitude += jsonNode.get("geometry").get("xmax").asDouble();
				longitude /= 2;
			}
			address.setLocation(new LatLng(latitude, longitude));
		} else if (jsonNode.has("geometry") && jsonNode.get("geometry").has("coordinates")
				&& jsonNode.get("geometry").get("coordinates").size() > 1) {
			double latitude = jsonNode.get("geometry").get("coordinates").get(1).asDouble();
			double longitude = jsonNode.get("geometry").get("coordinates").get(0).asDouble();
			address.setLocation(new LatLng(latitude, longitude));
		}

		if (jsonNode.has("bbox") && jsonNode.get("bbox").size() > 3) {
			double latitude = (jsonNode.get("bbox").get(1).asDouble() + jsonNode.get("bbox").get(3).asDouble()) / 2;
			double longitude = (jsonNode.get("bbox").get(0).asDouble() + jsonNode.get("bbox").get(2).asDouble()) / 2;
			address.setLocation(new LatLng(latitude, longitude));
		}

		if (jsonNode.get("properties").has("kategori")) {
			// it's a place
			isPlace = true;
			iconRes = R.drawable.search_location_icon;

		} else {
			isPlace = false;
			iconRes = R.drawable.search_magnify_icon;
		}
	}

	/**
	 * Parses the list of place names (stednavneliste) and returns the first official or the last
	 * element of any status or null if no element was found.
	 * @param placesNode
	 * @return
     */
	protected String parseStedNavneListe(JsonNode placesNode) {
		if(!placesNode.isArray()) {
			throw new RuntimeException("Expected properties field 'stednavneliste' to be an array");
		}
		String lastName = null;
		for(JsonNode placeNode: placesNode) {
			if(placeNode.has("status") && placeNode.has("navn")) {
				String placeName = placeNode.get("navn").asText();
				String placeNameStatus = placeNode.get("status").asText();
				if(placeNameStatus.equals("officielt")) {
					return placeName;
				}
				lastName = placeName;
			}
		}
		return lastName;
	}

	public KortforsyningenListItem(String street, String num) {
		super(null, nodeType.KORTFOR);
		address.setStreet(street);
		address.setHouseNumber(num);
	}

	public boolean isPlace() {
		return isPlace;
	}

	public int getOrder() {
		return 2;
	}

	public String getSubSource() {
		return null;
	}

	@Override
	public int getIconResourceId() {
		return iconRes;
	}

	public boolean hasCoordinates() {
		return address.getLocation() != null;
	}

}
