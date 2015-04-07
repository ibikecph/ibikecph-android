package com.spoiledmilk.ibikecph.tracking;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by jens on 3/22/15.
 */
public class TrackListAdapter extends ArrayAdapter<Track> {

    SimpleDateFormat dt = new SimpleDateFormat("HH:mm");

    public TrackListAdapter(Context context, int textViewResourceId, List<Track> tracks) {
        super(context, textViewResourceId, tracks);
    }


    /**
     * Called for each Track. Establishes a View for that particular track to be put
     * into the list of tracks in the TrackingActivity.
     * @param position
     * @param convertView
     * @param parent
     * @return A view to put into the TrackListView
     */
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) IbikeApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.track_list_row_view, parent, false);

        //TextView titleView = (TextView) rowView.findViewById(R.id.trackTextView);


        final Track track = this.getItem(position);

        // SET THE DURATION LABEL
        TextView trackDurationView = (TextView) rowView.findViewById(R.id.trackDurationView);
        trackDurationView.setText( String.valueOf( (int) (track.getDuration() / 60 )));

        // SET THE DISTANCE LABEL
        TextView lengthView = (TextView) rowView.findViewById(R.id.trackLengthView);
        TextView trackLengthUnitTextView = (TextView) rowView.findViewById(R.id.trackLengthUnitTextView);
        TextView distLabel = (TextView) rowView.findViewById(R.id.distLabel);

        distLabel.setText(IbikeApplication.getString("Dist"));
        try {
            double distance = track.getLength();

            lengthView.setText( String.valueOf(((int) Math.round(distance / 1000))));
        } catch(NullPointerException e) {
            lengthView.setText("-1 m");
        }

        // SET THE SPEED LABEL
        TextView speedLabel = (TextView) rowView.findViewById(R.id.speedLabel);
        TextView trackSpeedView = (TextView) rowView.findViewById(R.id.trackSpeedView);

        speedLabel.setText(IbikeApplication.getString("Speed"));
        int speed = -1;
        if (track.getDuration() != 0)
            speed = (int) Math.round((track.getLength() / track.getDuration()) * 3.6);
        trackSpeedView.setText(String.valueOf(speed));

        // SET THE GEOLOCATION LABELS
        TextView geoFromLabel = (TextView) rowView.findViewById(R.id.trackGeoFromView);
        TextView geoToLabel = (TextView) rowView.findViewById(R.id.trackGeoToView);

        geoFromLabel.setText(track.getStart());
        geoToLabel.setText(track.getEnd());

        // SET THE TIME LABELS
        TextView timeLabel = (TextView) rowView.findViewById(R.id.timeLabel);
        TextView trackTimeSpanView = (TextView) rowView.findViewById(R.id.trackTimeSpanView);
        Date start = track.getLocations().first().getTimestamp();
        Date end = track.getLocations().last().getTimestamp();

        timeLabel.setText(IbikeApplication.getString("Time"));
        trackTimeSpanView.setText(dt.format(start) + " – " + dt.format(end));

        // Open the TrackMapView when clicking on a track
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(IbikeApplication.getContext(), TrackMapView.class);
                i.putExtra("track_position", position);
                getContext().startActivity(i);
            }
        });

        return rowView;
    }


}
