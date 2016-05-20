package com.spoiledmilk.ibikecph.tracking;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

import io.realm.Realm;
import io.realm.RealmResults;

import java.util.Calendar;
import java.util.Date;

/**
 * Shows summary statistics on the main map view
 */
public class TrackingStatisticsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tracking_statistics_fragment, container, false);

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
        todayTextView.setText(IbikeApplication.getString("Today"));

        TextView totalDurationTextView = (TextView) v.findViewById(R.id.totalDurationTextView);
        totalDurationTextView.setText(TrackListAdapter.durationToFormattedTime(totalDuration));

        TextView totalDistanceTextView = (TextView) v.findViewById(R.id.totalDistanceTextView);
        totalDistanceTextView.setText(String.format("%d km", Math.round(totalDistance / 1000)));

        TextView totalCaloriesTextView = (TextView) v.findViewById(R.id.totalCaloriesTextView);
        totalCaloriesTextView.setText((int) ((totalDistance / 1000) * 11) + " Cal");

        // When user clicks the statistics, open the Tracking Settings.
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: This is not DRY -- copied from LeftMenu code.
                Intent i;
                IbikePreferences settings = IbikeApplication.getSettings();
                if (!settings.getTrackingEnabled() &&
                        Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class).size() == 0) {

                    i = new Intent(getActivity(), TrackingWelcomeActivity.class);

                    // We don't want this pushed to the back stack.
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                } else {
                    i = new Intent(getActivity(), TrackingActivity.class);
                }

                startActivity(i);
            }
        });

        return v;
    }

}
