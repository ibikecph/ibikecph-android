package com.spoiledmilk.ibikecph.persist;

import io.realm.RealmList;
import io.realm.RealmObject;

import java.util.Date;

public class Track extends RealmObject {

    private RealmList<TrackLocation> locations;
    private String start;
    private String end;
    private double length;
    private double duration;
    private Date timestamp;

    private boolean hasBeenGeocoded;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public RealmList<TrackLocation> getLocations() {
        return locations;
    }

    public void setLocations(RealmList<TrackLocation> locations) {
        this.locations = locations;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public boolean getHasBeenGeocoded() {
        return hasBeenGeocoded;
    }

    public void setHasBeenGeocoded(boolean hasBeenGeocoded) {
        this.hasBeenGeocoded = hasBeenGeocoded;
    }
}
