package com.spoiledmilk.ibikecph.navigation;

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
import com.spoiledmilk.ibikecph.map.fragments.MapStateFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.states.NavigatingState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

import java.text.SimpleDateFormat;

/**
 * Created by jens on 7/11/15.
 */
public class TurnByTurnInstructionFragment extends MapStateFragment {

    private ImageView imgDirectionIcon, imgDirectionIconXtra;
    private TextView textDistance;
    private TextView textWayname, textLastWayNameXtra;
    private RelativeLayout XtraView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume() {
        super.onResume();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    /**
     * TODO: Have this method implement some of the behaviour from renderForBreakRoute
     */
    public void render() {
        SMRoute route = getMapState(NavigatingState.class).getRoute();
        if(route.getTurnInstructions().size() > 0) {
            if(!route.isPublic()) {
                SMTurnInstruction turn = route.getTurnInstructions().get(0);
                textWayname.setText(turn.wayName);
                textDistance.setText(turn.lengthInMeters + " m");
                imgDirectionIcon.setImageResource(turn.getBlackDirectionImageResource());
            } else {
                String from = route.startAddress.getDisplayName();
                String take = route.description;
                String to = route.endAddress.getDisplayName();

                String instruction = IBikeApplication.getString("direction_18");
                instruction = instruction.replace("%@", "%s");
                instruction = String.format(instruction, from, take, to);

                String departureTime = timeStampFormat(route.departureTime);
                textWayname.setText(instruction);
                // Use time instead of metres when next stop is public transportation
                textDistance.setText(departureTime);
                imgDirectionIcon.setImageResource(getTypeDrawableId(route.transportType));
            }
        } else {
            textWayname.setText("");
            textDistance.setText("");
            imgDirectionIcon.setImageResource(0);
        }
    }

    public void reachedDestination() {
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
            time = sdf.format(seconds).toString();
        }
        // 12-hour format
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            time = sdf.format(seconds).toString();
        }

        return time;
    }

    public int getTypeDrawableId(SMRoute.TransportationType type) {
        if (type == SMRoute.TransportationType.BIKE) {
            return R.drawable.route_bike;
        } else if (type == SMRoute.TransportationType.M) {
            return R.drawable.route_metro_direction;
        } else if (type == SMRoute.TransportationType.S) {
            return R.drawable.route_s_direction;
        } else if (type == SMRoute.TransportationType.TOG) {
            return R.drawable.route_train_direction;
        } else if (type == SMRoute.TransportationType.WALK) {
            return R.drawable.route_walking_direction;
        } else if (type == SMRoute.TransportationType.IC) {
            return R.drawable.route_train_direction;
        } else if (type == SMRoute.TransportationType.LYN) {
            return R.drawable.route_train_direction;
        } else if (type == SMRoute.TransportationType.REG) {
            return R.drawable.route_train_direction;
        } else if (type == SMRoute.TransportationType.BUS) {
            return R.drawable.route_bus_direction;
        } else if (type == SMRoute.TransportationType.EXB) {
            return R.drawable.route_bus_direction;
        } else if (type == SMRoute.TransportationType.NB) {
            return R.drawable.route_bus_direction;
        } else if (type == SMRoute.TransportationType.TB) {
            return R.drawable.route_bus_direction;
        } else if (type == SMRoute.TransportationType.F) {
            return R.drawable.route_ship_direction;
        } else {
            return 0;
        }
    }

}
