package com.spoiledmilk.ibikecph.persist;

import io.realm.RealmList;
import io.realm.RealmObject;

public class Track extends RealmObject {

    private RealmList<TrackLocation> locations;
    private String start;
    private String end;
    private double length;

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
}
