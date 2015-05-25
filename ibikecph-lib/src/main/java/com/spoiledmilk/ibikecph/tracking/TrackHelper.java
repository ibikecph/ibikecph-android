package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import android.util.Log;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;

import java.util.ArrayList;

/**
 * Created by jens on 3/13/15.
 */
public class TrackHelper implements SMHttpRequestListener {

    private static TrackHelper instance;
    private Track _track = null;

    public TrackHelper(Track track) {
        this._track = track;
    }

    public void geocodeTrack() {
        Log.d("JC", "Geocoding Track " + this._track.hashCode());

        // Get a geolocation for the start and end points.
        new SMHttpRequest().findPlacesForLocation(_track.getLocations().first(), this);
        new SMHttpRequest().findPlacesForLocation(_track.getLocations().last(), this);
    }

    @Override
    public void onResponseReceived(int requestType, Object response) {

        // We got an answer from the geocoder, put it on the most recent route
        if (requestType == SMHttpRequest.REQUEST_FIND_PLACES_FOR_LOC) {
            SMHttpRequest.Address address = (SMHttpRequest.Address) response;

            // First off, the geocoder sometimes lies! If it has no access to the internet, it just returns the
            // coordinates as if they were the address! We need to filter out those cases. If it contains a \n it's
            // just a set of coordinates. Ain't nobody got time for that; cop out and wait until we have internet
            // access again. We assume the geocoder doesn't provide addresses with newlines, naturally.
            // TODO: We're not taking care of the tracks that the geocoder *cannot* give a proper answer for (i.e. outside DK)

            if (address.street.contains("\n")) return;

            /**
             * OK, we don't know if this answer relates to the start or the end of the route, so we have to cross-check.
             * We take the lat/lon from the Address object and check whether it's closest to the start or the end. This
             * creates some problems for tracks that begins and ends on the same spot, so we make an additional check to
             * see if the route is circular first. If it is, we set the start and end geotag at the same time.
             */

            // Get the most recent track
            Realm realm = Realm.getInstance(IbikeApplication.getContext());
            realm.beginTransaction();

            TrackLocation start = _track.getLocations().first();
            TrackLocation end = _track.getLocations().last();

            // First convert tne starts and ends to Location objects
            Location startLocation = new Location("TrackingManager");
            startLocation.setLatitude(start.getLatitude());
            startLocation.setLongitude(start.getLongitude());

            Location endLocation = new Location("TrackingManager");
            endLocation.setLatitude(end.getLatitude());
            endLocation.setLongitude(end.getLongitude());

            boolean routeIsCircular = startLocation.distanceTo(endLocation) < 20;

            Location geocodedLocation = new Location("TrackingManager");
            geocodedLocation.setLatitude(address.lat);
            geocodedLocation.setLongitude(address.lon);

            // Figure out whether the geocoded position is closest to start or end, and set the appropriate field on
            // the Track object.
            double distanceToStart = startLocation.distanceTo(geocodedLocation);
            double distanceToEnd = endLocation.distanceTo(geocodedLocation);

            if (routeIsCircular || distanceToStart < distanceToEnd) {
                _track.setStart(address.street);
            }

            if (routeIsCircular || distanceToEnd < distanceToStart) {
                _track.setEnd(address.street);
            }

            // The current track has been geocoded if both the start and end have been set
            if (!_track.getStart().isEmpty() && !_track.getEnd().isEmpty()) {
                _track.setHasBeenGeocoded(true);
                Log.d("JC", "Track " + this._track.hashCode()+" geocoded.");
                realm.commitTransaction();
            } else {
                realm.commitTransaction();
            }
        }
    }

    public static double getDistanceOfTrack(Track t)  {
        double result = 0;

        ArrayList<Location> locations = new ArrayList<Location>();

        if (t.getLocations() == null) return -1;

        for (TrackLocation l : t.getLocations()) {
            Location tmpl = new Location("TrackingActivity");
            tmpl.setLongitude(l.getLongitude());
            tmpl.setLatitude(l.getLatitude());

            locations.add(tmpl);
        }

        for (int i = 0; i < locations.size()-1; i++) {
            result += locations.get(i).distanceTo(locations.get(i + 1));
        }

        return result;
    }

    public static void ensureAllTracksGeocoded() {
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        //realm.beginTransaction();

        for (Track t : Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class)) {
            if (!t.getHasBeenGeocoded()) {
                TrackHelper th = new TrackHelper(t);
                th.geocodeTrack();
            }
        }
    }
}
