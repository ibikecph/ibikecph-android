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

        final Track track = this.getItem(position);

        // SET THE DURATION LABEL
        TextView trackDurationView = (TextView) rowView.findViewById(R.id.trackDurationView);
        trackDurationView.setText( durationToFormattedTime(track.getDuration()) );

        // SET THE DISTANCE LABEL
        TextView lengthView = (TextView) rowView.findViewById(R.id.trackLengthView);
        TextView trackLengthUnitTextView = (TextView) rowView.findViewById(R.id.trackLengthUnitTextView);

        try {
            double distance = track.getLength();

            lengthView.setText( String.valueOf(((int) Math.round(distance / 1000))));
        } catch(NullPointerException e) {
            lengthView.setText("-1 m");
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
                getContext().startActivity(i);
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



    public static String durationToFormattedTime(double minutes) {
        return String.format(IbikeApplication.getString("hour_minute_format"), (int)(minutes/60), (int)(minutes % 60));
    }
}
