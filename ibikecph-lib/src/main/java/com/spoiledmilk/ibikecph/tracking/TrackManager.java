package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;

import java.util.ArrayList;

/**
 * Created by jens on 3/13/15.
 */
public class TrackManager {

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
}
