package com.spoiledmilk.ibikecph.navigation;

import android.app.Fragment;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
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
    private ImageView imgDirectionIcon, imgDirectionIconXtra;
    private TextView textDistance;
    private TextView textWayname, textLastWayNameXtra;
    private RelativeLayout XtraView;

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

        this.XtraView = (RelativeLayout) v.findViewById(R.id.XtraView);
        this.imgDirectionIconXtra = (ImageView) v.findViewById(R.id.imgDirectionIconXtra);
        this.textLastWayNameXtra = (TextView) v.findViewById(R.id.textLastWayNameXtra);

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
            this.XtraView.setVisibility(View.VISIBLE);
            this.textLastWayNameXtra.setText(NavigationMapHandler.getOffAt);
            getType(NavigationMapHandler.lastType, this.imgDirectionIconXtra);
        } else {
            this.XtraView.setVisibility(View.GONE);
        }

        // Display which public to get on
        if (NavigationMapHandler.isPublic) {
            String fromTakeTo = "";
            String depatureTime = "";
            try {
                String from = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(0).textValue();
                String take = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("name").textValue();
                String to = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(1).textValue();
                fromTakeTo = IBikeApplication.getString("direction_18");
                fromTakeTo = fromTakeTo.replace("%@", "%s");
                fromTakeTo = String.format(fromTakeTo, from, take, to);
                depatureTime = timeStampFormat(MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("departure_time").asLong());
                getType(MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("type").textValue(), this.imgDirectionIcon);
            } catch (Exception ex) {
                Log.d("DV", "TurnByTurn exception ispublic, ex = " + ex.getMessage());
            }
            this.textWayname.setText(fromTakeTo);
            this.textDistance.setText(depatureTime); //set time instead of m when next stop is public
            // Display which public is the next stop when we have left the current public station
        } else if (NavigationMapHandler.displayGetOffAt) {
            String arrivalTime = "";
            try {
                String to = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_name").get(1).textValue();
                NavigationMapHandler.getOffAt = IBikeApplication.getString("direction_19");
                NavigationMapHandler.getOffAt = NavigationMapHandler.getOffAt.replace("%@", "%s");
                NavigationMapHandler.getOffAt = String.format(NavigationMapHandler.getOffAt, to);
                NavigationMapHandler.lastType = MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("type").textValue();
                arrivalTime = timeStampFormat(MapActivity.breakRouteJSON.get(NavigationMapHandler.obsInt.getPageValue()).path("journey").get(NavigationMapHandler.routePos).path("route_summary").path("arrival_time").asLong());
            } catch (Exception ex) {
                Log.d("DV", "TurnByTurn exception displayGetOffAt, ex = " + ex.getMessage());
            }

            this.textWayname.setText(NavigationMapHandler.getOffAt);
            this.textDistance.setText(arrivalTime); //set time instead of m when left radius of start public station
        } else {
            this.textWayname.setText(turn.wayName);
            this.textDistance.setText(turn.lengthInMeters + " m");
            this.imgDirectionIcon.setImageResource(turn.getBlackDirectionImageResource());
        }

    }

    public void reachedDestination() {
        Log.d("DV", "turnbyturn reacheddestination");
        this.textWayname.setText(IBikeApplication.getString("direction_15"));
        this.textDistance.setText("");
        this.imgDirectionIcon.setImageResource(R.drawable.flag);
    }

    public String timeStampFormat(long seconds) {

        String time;
        seconds = seconds * 1000;

        // 24-hour format
        if (DateFormat.is24HourFormat(this.getActivity())) {
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

    public void getType(String type, ImageView image) {

        if (type.equals("BIKE")) {
            image.setImageResource(R.drawable.route_bike);
        } else if (type.equals("M")) {
            image.setImageResource(R.drawable.route_metro_direction);
        } else if (type.equals("S")) {
            image.setImageResource(R.drawable.route_s_direction);
        } else if (type.equals("TOG")) {
            image.setImageResource(R.drawable.route_train_direction);
        } else if (type.equals("WALK")) {
            image.setImageResource(R.drawable.route_walking_direction);
        } else if (type.equals("IC")) {
            image.setImageResource(R.drawable.route_train_direction);
        } else if (type.equals("LYN")) {
            image.setImageResource(R.drawable.route_train_direction);
        } else if (type.equals("REG")) {
            image.setImageResource(R.drawable.route_train_direction);
        } else if (type.equals("BUS")) {
            image.setImageResource(R.drawable.route_bus_direction);
        } else if (type.equals("EXB")) {
            image.setImageResource(R.drawable.route_bus_direction);
        } else if (type.equals("NB")) {
            image.setImageResource(R.drawable.route_bus_direction);
        } else if (type.equals("TB")) {
            image.setImageResource(R.drawable.route_bus_direction);
        } else if (type.equals("F")) {
            image.setImageResource(R.drawable.route_ship_direction);
        }
    }

}
