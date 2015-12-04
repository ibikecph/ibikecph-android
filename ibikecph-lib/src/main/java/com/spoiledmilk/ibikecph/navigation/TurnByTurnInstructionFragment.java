package com.spoiledmilk.ibikecph.navigation;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

import java.text.SimpleDateFormat;

/**
 * Created by jens on 7/11/15.
 */
public class TurnByTurnInstructionFragment extends Fragment {
    private NavigationMapHandler parent;
    private ImageView imgDirectionIcon;
    private TextView textDistance;
    private TextView textWayname, textLastWayName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
        this.parent.setTurnByTurnFragment(this);
    }

    public void onResume() {
        super.onResume();
        this.parent.setTurnByTurnFragment(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.parent.setTurnByTurnFragment(this);
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.instruction_top_view, container, false);
        this.imgDirectionIcon = (ImageView) v.findViewById(R.id.imgDirectionIcon);
        this.textDistance = (TextView) v.findViewById(R.id.textDistance);
        this.textWayname = (TextView) v.findViewById(R.id.textWayname);
        this.textLastWayName = (TextView) v.findViewById(R.id.textLastWayName);

        render();

        return v;
    }

    public void updateTurn(boolean firstElementRemoved) {
        this.render();
    }

    public void render() {
        // If the size=0, we've actually already arrived, but render() is called before NavigationMapHandler gets its
        // reachedDestination() callback from the SMRoute. Blame somebody else...
        if (this.parent.getRoute().getTurnInstructions().size() == 0) {
            Log.d("DV", "render, getRoute size == 0");
            return;
        }

        SMTurnInstruction turn = this.parent.getRoute().getTurnInstructions().get(0);
        this.textWayname.setText(turn.wayName);
        this.textDistance.setText(turn.lengthInMeters + " m");
        this.imgDirectionIcon.setImageResource(turn.getBlackDirectionImageResource());

    }

    public void renderForBreakRoute(SMRoute route) {
        if (route.getTurnInstructions().size() == 0) {
            Log.d("DV", "render, getRoute size == 0");
            return;
        }

        SMTurnInstruction turn = route.getTurnInstructions().get(0);

        // Display the extra field until we have left the public station
        if (NavigationMapHandler.displayExtraField) {
            this.textLastWayName.setVisibility(View.VISIBLE);
            this.textLastWayName.setText("AAA"); //fix
        } else {
            this.textLastWayName.setVisibility(View.GONE);
        }

        // Display which public to get on
        if (NavigationMapHandler.isPublic) {
            String fromTakeTo = "";
            String depatureTime = "";
            try {
                String from = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(0).textValue();
                String take = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("name").textValue();
                String to = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(1).textValue();
                fromTakeTo = IbikeApplication.getString("direction_18");
                fromTakeTo = fromTakeTo.replace("%@", "%s");
                fromTakeTo = String.format(fromTakeTo, from, take, to);

                depatureTime = timeStampFormat(MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("departure_time").asLong());
            } catch (Exception ex) {
            }
            this.textWayname.setText(fromTakeTo);
            this.textDistance.setText(depatureTime); //set time instead of m when next stop is public
            // Display which public is the next stop when we have left the current public station
        } else if (NavigationMapHandler.displayGetOffAt) {
            String getOffAt = "";
            String arrivalTime = "";
            try {
                String to = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(1).textValue();
                getOffAt = IbikeApplication.getString("direction_19");
                getOffAt = getOffAt.replace("%@", "%s");
                getOffAt = String.format(getOffAt, to);
                arrivalTime = timeStampFormat(MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("arrival_time").asLong());
            } catch (Exception ex) {
            }

            this.textWayname.setText(getOffAt);
            this.textDistance.setText(arrivalTime); //set time instead of m when left radius of start public station
        } else {
            this.textWayname.setText(turn.wayName);
            this.textDistance.setText(turn.lengthInMeters + " m");
        }


        this.imgDirectionIcon.setImageResource(turn.getBlackDirectionImageResource());
    }

    public void reachedDestination() {
        Log.d("DV", "turnbyturn reacheddestination");
        this.textWayname.setText(IbikeApplication.getString("direction_15"));
        this.textDistance.setText("");
        this.imgDirectionIcon.setImageResource(R.drawable.flag);
    }

    public String timeStampFormat(long seconds) {

        String time;
        seconds = seconds * 1000;

        // 24-hour format
        if (MapActivity.format) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            //sdf.setTimeZone(TimeZone.getDefault());
            time = sdf.format(seconds).toString();
        }
        // 12-hour format
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            //sdf.setTimeZone(TimeZone.getDefault());
            time = sdf.format(seconds).toString();
        }

        return time;
    }
}
