package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.fragments.MapStateFragment;
import com.spoiledmilk.ibikecph.map.states.NavigatingState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
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
    private SMTurnInstruction nextTurnInstruction;

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
        NavigatingState state = getMapState(NavigatingState.class);
        SMTurnInstruction instruction = state.getJourney().getUpcomingInstruction();
        if (instruction != null) {
            SMRoute route = state.getRoute();
            if (!instruction.transportType.isPublicTransportation()) {
                textWayname.setText(instruction.name);
                textDistance.setText(Math.round(instruction.distance) + " m");
                imgDirectionIcon.setImageResource(instruction.getSmallDirectionResourceId());
            } else if(instruction.drivingDirection == SMTurnInstruction.TurnDirection.GetOnPublicTransportation) {
                SMTurnInstruction nextInstruction = state.getJourney().getUpcomingInstruction(1);
                String from = instruction.name;
                String take = instruction.getDescription();
                String to = nextInstruction.name;

                String instructionString = IBikeApplication.getString("direction_18");
                instructionString = instructionString.replace("%@", "%s");
                instructionString = String.format(instructionString, from, take, to);

                String departureTime = timeStampFormat(route.departureTime);
                textWayname.setText(instructionString);
                // Use time instead of metres when next stop is public transportation
                textDistance.setText(departureTime);
                imgDirectionIcon.setImageResource(instruction.getSmallDirectionResourceId());
            } else if(instruction.drivingDirection == SMTurnInstruction.TurnDirection.GetOffPublicTransportation) {
                String instructionString = IBikeApplication.getString("direction_19");
                String arrivalTime = timeStampFormat(route.arrivalTime);
                textWayname.setText(instructionString + " " + instruction.name);
                // Use time instead of metres when next stop is public transportation
                textDistance.setText(arrivalTime);
                imgDirectionIcon.setImageResource(instruction.getSmallDirectionResourceId());
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

    /**
     * Loops through the journey's routes and returns the first upcoming instruction
     * @return
     */
    public SMTurnInstruction getNextTurnInstruction() {
        NavigatingState state = getMapState(NavigatingState.class);
        // Let's try the current route, right away
        SMRoute currentRoute = state.getRoute();
        if(currentRoute.getUpcomingTurnInstructions().size() > 0) {
            return currentRoute.getUpcomingTurnInstructions().get(0);
        } else {
            // Let's start the looping
            Journey journey = state.getJourney();
            int currentRouteIndex = journey.getRoutes().indexOf(state.getRoute());
            if(currentRouteIndex == -1) {
                return null;
            }
            for(int r = currentRouteIndex+1; r < journey.getRoutes().size(); r++) {
                SMRoute route = journey.getRoutes().get(r);
                if(route.getUpcomingTurnInstructions().size() > 0) {
                    return route.getUpcomingTurnInstructions().get(0);
                }
            }
            return null;
        }
    }
}
