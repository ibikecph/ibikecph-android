package com.spoiledmilk.ibikecph.persist;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;
import io.realm.RealmObject;

public class TrackLocation extends RealmObject {

    private Date timestamp;
    private double latitude;
    private double longitude;
    private double altitude;
    private double horizontalAccuracy;
    private double verticalAccuracy;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getHorizontalAccuracy() {
        return horizontalAccuracy;
    }

    public void setHorizontalAccuracy(double horizontalAccuracy) {
        this.horizontalAccuracy = horizontalAccuracy;
    }

    public double getVerticalAccuracy() {
        return verticalAccuracy;
    }

    public void setVerticalAccuracy(double verticalAccuracy) {
        this.verticalAccuracy = verticalAccuracy;
    }

    public static TrackLocation fromLocation(Location l) {
        TrackLocation tl = new TrackLocation();
        tl.setLatitude(l.getLatitude());
        tl.setLongitude(l.getLongitude());
        tl.setTimestamp(new Date(l.getTime()));
        tl.setAltitude(l.getAltitude());

        // This is potentially bad. We don't have a measure of the horizontal and vertical accuracies, but we do have
        // one for the accuracy all in all. We just set that for both fields.
        tl.setHorizontalAccuracy(l.getAccuracy());
        tl.setVerticalAccuracy(l.getAccuracy());

        return tl;
    }
}
