package com.spoiledmilk.ibikecph;

import android.widget.ArrayAdapter;

public class LeftMenuItem {

	private String labelID;
	private String icon;
	
	public  LeftMenuItem(String labelID, String icon) {
		this.labelID = labelID;
		this.icon = icon;
	}
	
	public LeftMenuItem(String labelID) {
		this(labelID, null);
	}

	public String getLabelID() {
		return labelID;
	}

	public String getIcon() {
		return icon;
	}
	
}
