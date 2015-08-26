package com.spoiledmilk.ibikecph.tracking;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
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
    Context context;

    public TrackListAdapter(Context context) {
        // Get all tracks from the DB
        Realm realm = Realm.getInstance(context);
        this.tracks = realm.allObjects(Track.class);

        // We want to see the newest track first
        this.tracks.sort("timestamp", false);

        this.context = context;

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
     *
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
        trackDurationView.setText(durationToFormattedTime(track.getDuration()));

        // SET THE DISTANCE LABEL
        TextView lengthView = (TextView) rowView.findViewById(R.id.trackLengthView);
        try {
            double distance = track.getLength();

            lengthView.setText(String.format("%.1f km", (distance / 1000)));
        } catch (NullPointerException e) {
            lengthView.setText("-1");
        }

        // SET THE TIME LABELS
        TextView trackTimeSpanView = (TextView) rowView.findViewById(R.id.trackTimeSpanView);

        Date start = track.getLocations().first().getTimestamp();
        Date end = track.getLocations().last().getTimestamp();
        trackTimeSpanView.setText(dt.format(start) + " – " + dt.format(end));

        //trackTimeSpanView.setText("test-state");

        // Open the TrackMapView when clicking on a track
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(IbikeApplication.getContext(), TrackMapView.class);
                i.putExtra("track_position", position);
                context.startActivity(i);
            }
        });

        // Bring up a menu on long press, to let the user delete.
        rowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                String[] options = {IbikeApplication.getString("Delete")};

                builder.setTitle(IbikeApplication.getString("Delete"))
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("JC", "Deleted a track");
                                Track t = tracks.get(position);

                                Realm realm = Realm.getInstance(context);
                                realm.beginTransaction();
                                //Kald DB-metode der sender ID til server her hvis ID > 0
                                t.removeFromRealm();
                                realm.commitTransaction();

                                notifyDataSetInvalidated();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

                return false;
            }
        });

        // SET THE GEOLOCATION LABELS
        TextView geoFromLabel = (TextView) rowView.findViewById(R.id.trackGeoFromView);
        TextView geoToLabel = (TextView) rowView.findViewById(R.id.trackGeoToView);

        geoFromLabel.setText(track.getStart());
        geoToLabel.setText(track.getEnd());

        // SET THE TEENYWEENY "TO" LABEL
        /*
        TextView trackToTextView = (TextView) rowView.findViewById(R.id.trackToTextView);
        trackToTextView.setText(" " + IbikeApplication.getString("to"));
        */
        return rowView;
    }

    public static String durationToFormattedTime(double seconds) {
        return String.format(IbikeApplication.getString("hour_minute_format"), (int) (seconds / 60 / 60), (int) ((seconds / 60) % 60));
    }

    @Override
    public View getHeaderView(int i, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) IbikeApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View headerView = inflater.inflate(R.layout.track_list_row_header_view, parent, false);

        TextView headerHeader = (TextView) headerView.findViewById(R.id.dayView);


        long timestamp = this.getHeaderId(i);

        // Don't show a header if the route was from today
        long today = getDayFromTimestamp(new Date());
        long yesterday = today - 24 * 60 * 60 * 1000;

        if (timestamp != today && timestamp != yesterday) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(timestamp));
            headerHeader.setText(headerFormat.format(cal.getTime()));

        } else if (timestamp == today) {
            headerHeader.setText(IbikeApplication.getString("Today"));
        } else if (timestamp == yesterday) {
            headerHeader.setText(IbikeApplication.getString("Yesterday"));
        }

        return headerView;
    }

    @Override
    public long getHeaderId(int i) {
        long routeTimestamp = getDayFromTimestamp(this.getItem(i).getTimestamp());
        return routeTimestamp;
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
