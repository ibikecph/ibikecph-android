package com.spoiledmilk.ibikecph.tracking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;

import java.util.List;

/**
 * Created by jens on 3/22/15.
 */
public class TrackListAdapter extends ArrayAdapter<Track> {

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
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) IbikeApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.track_list_row_view, parent, false);

        TextView titleView = (TextView) rowView.findViewById(R.id.trackTextView);
        TextView lengthView = (TextView) rowView.findViewById(R.id.trackLengthView);

        Track track = this.getItem(position);

        titleView.setText("Track " + position);

        try {
            lengthView.setText(TrackManager.getDistanceOfTrack(track) + " m");
        } catch(NullPointerException e) {
            lengthView.setText("-1 m");
        }

        return rowView;
    }

}
