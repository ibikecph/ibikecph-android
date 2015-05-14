package com.spoiledmilk.ibikecph.tracking;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import io.realm.Realm;
import io.realm.RealmResults;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 3/22/15.
 */
public class TrackListAdapter extends BaseAdapter implements StickyListHeadersAdapter {
    RealmResults<Track> tracks;

    SimpleDateFormat dt = new SimpleDateFormat("HH:mm");
    SimpleDateFormat headerFormat = new SimpleDateFormat(IbikeApplication.getString("track_list_date_format"));

    public TrackListAdapter(Context context) {
        // Get all tracks from the DB
        Realm realm = Realm.getInstance(context);
        this.tracks = realm.allObjects(Track.class);

        // We want to see the newest track first
        this.tracks.sort("timestamp", false);


    }


    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public Track getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return tracks.get(position).getTimestamp().getTime();
    }

    /**
     * Called for each Track. Establishes a View for that particular track to be put
     * into the list of tracks in the TrackingActivity.
     * @param position
     * @param convertView
     * @param parent
     * @return A view to put into the TrackListView
     */
    public View getView(final int position, final View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) IbikeApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.track_list_row_view, parent, false);

        final Track track = this.getItem(position);

        // SET THE DURATION LABEL
        TextView trackDurationView = (TextView) rowView.findViewById(R.id.trackDurationView);
        trackDurationView.setText( durationToFormattedTime(track.getDuration()) );

        // SET THE DISTANCE LABEL
        TextView lengthView = (TextView) rowView.findViewById(R.id.trackLengthView);
        try {
            double distance = track.getLength();

            lengthView.setText( String.valueOf(((int) Math.round(distance / 1000))) + " km" );
        } catch(NullPointerException e) {
            lengthView.setText("-1");
        }

        // SET THE TIME LABELS
        TextView trackTimeSpanView = (TextView) rowView.findViewById(R.id.trackTimeSpanView);
        Date start = track.getLocations().first().getTimestamp();
        Date end = track.getLocations().last().getTimestamp();

        trackTimeSpanView.setText(dt.format(start) + " – " + dt.format(end));

        // Open the TrackMapView when clicking on a track
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(IbikeApplication.getContext(), TrackMapView.class);
                i.putExtra("track_position", position);
                convertView.getContext().startActivity(i);
            }
        });

        // SET THE GEOLOCATION LABELS
        TextView geoFromLabel = (TextView) rowView.findViewById(R.id.trackGeoFromView);
        TextView geoToLabel = (TextView) rowView.findViewById(R.id.trackGeoToView);

        geoFromLabel.setText(track.getStart());
        geoToLabel.setText(track.getEnd());

        // SET THE TEENYWEENY "TO" LABEL
        TextView trackToTextView = (TextView) rowView.findViewById(R.id.trackToTextView);
        trackToTextView.setText(" " + IbikeApplication.getString("to"));

        return rowView;
    }

    public static String durationToFormattedTime(double seconds) {
        return String.format(IbikeApplication.getString("hour_minute_format"), (int)(seconds/60/60), (int)(seconds % 60));
    }

    @Override
    public View getHeaderView(int i, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) IbikeApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.track_list_row_header_view, parent, false);

        TextView headerHeader = (TextView) headerView.findViewById(R.id.dayView);


        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(getDayFromTimestamp(this.getItem(i).getTimestamp())));

        headerHeader.setText(headerFormat.format(cal.getTime()));

        return headerView;
    }

    @Override
    public long getHeaderId(int i) {

        return getDayFromTimestamp(this.getItem(i).getTimestamp());
    }

    public long getDayFromTimestamp(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime().getTime();
    }
}
