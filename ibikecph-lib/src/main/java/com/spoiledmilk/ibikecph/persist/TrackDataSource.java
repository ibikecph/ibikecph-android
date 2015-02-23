package com.spoiledmilk.ibikecph.persist;

import android.content.Context;

import java.util.Date;

import io.realm.RealmResults;

public class TrackDataSource extends DataSource {

    public TrackDataSource(Context context) {
        super(context);
    }

    public Track createTrack(String start, String end, double length) {
        realm.beginTransaction();

        Track track = realm.createObject(Track.class);

        track.setStart(start);
        track.setEnd(end);
        track.setLength(length);

        realm.commitTransaction();

        return track;
    }

    public TrackLocation createTrackLocation(
            double latitude,
            double longitude,
            double altitude,
            double horizontalAccuracy,
            double verticalAccuracy
    ) {
        Date timestamp = new Date();

        realm.beginTransaction();

        TrackLocation location = realm.createObject(TrackLocation.class);

        location.setTimestamp(timestamp);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);
        location.setHorizontalAccuracy(horizontalAccuracy);
        location.setVerticalAccuracy(verticalAccuracy);

        realm.commitTransaction();

        return location;
    }

    public void addLocationToTrack(Track track, TrackLocation location) {
        realm.beginTransaction();

        track.getLocations().add(location);

        realm.commitTransaction();
    }

    public RealmResults<Track> getAllTracks() {
        return realm.where(Track.class).findAll();
    }

    public RealmResults<TrackLocation> getAllLocationsFromTrack(Track track) {
        // where().findAll() to make it a RealmResult and not RealmList
        return track.getLocations().where().findAll();
    }
}
