package com.spoiledmilk.ibikecph;

/**
 * A menu item for the navigation drawer in the I BIKE CPH apps.
 * @author jens
 *
 */
public class LeftMenuItem {


    private String labelID;
	private int iconResource;
	private String handler;
	
	/**
	 * Constructs a menu item for the left menu. 
	 * @param labelID The dictionary ID for the text to put on the button
	 * @param iconResource A reference to a drawable that should be drawn as a thumbnail.
	 * @param handler A String denoting the function to be called in the LeftMenu class as a handler.
	 */
	public  LeftMenuItem(String labelID, int iconResource, String handler) {
		this.labelID = labelID;
		this.iconResource = iconResource;
		this.handler = handler;
	}
	
	public LeftMenuItem(String labelID) {
		this(labelID, -1, null);
	}
	
    public LeftMenuItem(String labelID, String handler) {
		this(labelID, -1, handler);
	}

	public String getLabelID() {
		return labelID;
	}

	public int getIconResource() {
		return iconResource;
	}
	
	public String getHandler() {
		return handler;
	}

    public void setLabelID(String labelID) {
        this.labelID = labelID;
    }
}
