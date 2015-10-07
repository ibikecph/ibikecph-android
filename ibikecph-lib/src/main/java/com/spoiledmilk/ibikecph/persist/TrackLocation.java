package com.spoiledmilk.ibikecph.persist;

import android.location.Location;
import io.realm.RealmObject;

import java.util.Date;

public class TrackLocation extends RealmObject {

    private Date timestamp;
    private double latitude;
    private double longitude;
    private double altitude;
    private double horizontalAccuracy;
    private double verticalAccuracy;
    private int activityOrdinal;

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

    public int getActivityOrdinal() {
        return activityOrdinal;
    }

    public void setActivityOrdinal(int activityOrdinal) {
        this.activityOrdinal = activityOrdinal;
    }

    public double getDistanceTo(TrackLocation thereTrackLocation) {
        Location here = new Location("TrackLocation");
        here.setLatitude(this.getLatitude());
        here.setLongitude(this.getLongitude());

        Location there = new Location("TrackLocation");
        there.setLatitude(thereTrackLocation.getLatitude());
        there.setLongitude(thereTrackLocation.getLongitude());

        return here.distanceTo(there);
    }

}
