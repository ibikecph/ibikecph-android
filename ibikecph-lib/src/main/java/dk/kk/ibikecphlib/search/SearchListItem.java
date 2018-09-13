// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import dk.kk.ibikecphlib.util.Util;

/**
 * An abstract class outlying the foundation of a list item in the search
 * box in the apps.
 * 
 * TODO: Figure out what the different properties mean. Name/Street/Address.
 * @author jens
 *
 */
public abstract class SearchListItem {

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void setFullAddress(String addressFullAddress) {
		this.address.setFullAddress(addressFullAddress);
	}

	public enum nodeType {
		CURRENT_POSITION, FAVORITE, HISTORY, KORTFOR
	};

	protected JsonNode jsonNode;

	public nodeType getType() {
		return type;
	}

	protected nodeType type;
	protected double distance = 0;

	protected Address address = new Address();

	public SearchListItem(JsonNode jsonNode, nodeType type) {
		this.jsonNode = jsonNode;
		this.type = type;
	}

	public SearchListItem(nodeType type) {
		this.jsonNode = null;
		this.type = type;
	}

	public abstract int getOrder();

	public String getSource() {
		return address.getSource().toString();
	}

	public abstract String getSubSource();

	public int getIconResourceId() {
		return 0;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double d) {
		this.distance = d;
	}

	public String getOneLineName() {
		String result = "";
		String primary = address.getPrimaryDisplayString();
		if(primary != null && !primary.isEmpty()) {
			result += primary;
		}
		String secondary = address.getSecondaryDisplayString();
		if(secondary != null && !secondary.isEmpty()) {
			if(!result.isEmpty()) {
				result += ", ";
			}
			result += secondary;
		}
		return result;
	}

	public JsonNode getJsonNode() {
		return jsonNode;
	}

	public static SearchListItem instantiate(JsonNode node) {
		SearchListItem ret = null;
		if (node.has("properties")) {
			ret = new KortforsyningenListItem(node);
		}
		return ret;
	}

	public String getPrimaryDisplayString() {
		return address.getPrimaryDisplayString();
	}

	public String getSecondaryDisplayString() {
		return address.getSecondaryDisplayString();
	}

}
