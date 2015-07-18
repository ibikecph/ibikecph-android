package com.spoiledmilk.ibikecph.tracking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.persist.Track;
import io.realm.Realm;
import io.realm.RealmResults;

import java.util.Calendar;
import java.util.Date;

/**
 * Shows summary statistics on the main map view
 */
public class TrackingInfoPaneFragment extends InfoPaneFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_tracking_statistics, container, false);

        // Get ridden distance and duration for tracks today: Start up by getting a Date object representing midnight
        Date midnight;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        midnight = cal.getTime();

        // We use that Date object to get all Tracks whose timestamps are set after midnight
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        RealmResults<Track> tracks = realm.where(Track.class).greaterThan("timestamp", midnight).findAll();

        double totalDistance = 0;
        double totalDuration = 0;

        for (Track t : tracks) {
            totalDistance += t.getLength();
            totalDuration += t.getDuration();
        }

        // Set the labels
        TextView todayTextView = (TextView) v.findViewById(R.id.todayTextView);
        todayTextView.setText(IbikeApplication.getString("Today").toLowerCase());

        TextView totalDurationTextView = (TextView) v.findViewById(R.id.totalDurationTextView);
        totalDurationTextView.setText(TrackListAdapter.durationToFormattedTime(totalDuration) );

        TextView totalDistanceTextView = (TextView) v.findViewById(R.id.totalDistanceTextView);
        totalDistanceTextView.setText(String.format("%d km", Math.round(totalDistance/1000)));

        return v;
    }

}
