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
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;

import java.text.SimpleDateFormat;

/**
 * Created by jens on 7/11/15.
 */
public class TurnByTurnInstructionFragment extends MapStateFragment {

    private ImageView imgDirectionIcon, imgDirectionIconXtra;
    private TextView textDistance;
    private TextView textWayname, textLastWayNameXtra;
    private RelativeLayout XtraView;
    private TurnInstruction nextTurnInstruction;

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
     * Updates the TextViews and Images, to show the next upcoming step.
     */
    public void render() {
        NavigationState state = getMapState(NavigatingState.class).getNavigationState();
        TurnInstruction instruction = state.getNextStep();
        if (instruction != null) {
            if (!instruction.transportType.isPublicTransportation()) {
                textWayname.setText(instruction.name);
                float distance = state.getDistanceToStep(instruction);
                String distanceString = Math.round(distance) + " m";
                textDistance.setText(distanceString);
                imgDirectionIcon.setImageResource(instruction.getSmallDirectionResourceId());
            } else {
                // This is public transportation
                Leg leg = state.getCurrentLeg();
                String instructionString;
                String timeString;

                if (instruction.getType().equals(TurnInstruction.Type.DEPART)) {
                    String from = instruction.name;
                    String take = instruction.getDescription();
                    String to = leg.getEndAddress().getDisplayName();

                    instructionString = IBikeApplication.getString("direction_18");
                    instructionString = instructionString.replace("%@", "%s");
                    instructionString = String.format(instructionString, from, take, to);

                    timeString = timeStampFormat(leg.getDepartureTime());
                } else if (instruction.getType().equals(TurnInstruction.Type.ARRIVE)) {
                    instructionString = IBikeApplication.getString("direction_19");
                    instructionString = instructionString + " " + instruction.name;

                    timeString = timeStampFormat(leg.getArrivalTime());
                } else {
                    throw new RuntimeException("Encountered an unexpected turn instruction");
                }

                textWayname.setText(instructionString);
                textDistance.setText(timeString);
                int transportationTypeResId = leg.getTransportType().getDrawableId();
                imgDirectionIcon.setImageResource(transportationTypeResId);
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
        return IBikeApplication.getTimeFormat().format(seconds * 1000);
    }
}
