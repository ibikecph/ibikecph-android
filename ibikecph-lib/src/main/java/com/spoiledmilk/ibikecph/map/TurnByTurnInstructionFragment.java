package com.spoiledmilk.ibikecph.map;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

/**
 * Created by jens on 7/11/15.
 */
public class TurnByTurnInstructionFragment extends Fragment {
    private NavigationMapHandler parent;
    private ImageView imgDirectionIcon;
    private TextView textDistance;
    private TextView textWayname;

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
        this.parent.setTurnByTurnFragment(this);
    }

    public void onResume() {
        super.onResume();
        this.parent.setTurnByTurnFragment(this);
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.parent.setTurnByTurnFragment(this);
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.instruction_top_view, container, false);
        this.imgDirectionIcon = (ImageView) v.findViewById(R.id.imgDirectionIcon);
        this.textDistance = (TextView) v.findViewById(R.id.textDistance);
        this.textWayname = (TextView) v.findViewById(R.id.textWayname);

        render();

        return v;
    }

    public void updateTurn(boolean firstElementRemoved) {
        this.render();
    }

    public void render() {
        // If the size=0, we've actually already arrived, but render() is called before NavigationMapHandler gets its
        // reachedDestination() callback from the SMRoute. Blame somebody else...
        if (this.parent.getRoute().getTurnInstructions().size() == 0)  return;

        SMTurnInstruction turn = this.parent.getRoute().getTurnInstructions().get(0);
        this.textWayname.setText(turn.wayName);
        this.textDistance.setText(turn.lengthInMeters + " m");
        this.imgDirectionIcon.setImageResource(turn.getBlackDirectionImageResource());

    }

    public void reachedDestination() {
        this.textWayname.setText(IbikeApplication.getString("direction_15"));
        this.textDistance.setText("");
        this.imgDirectionIcon.setImageResource(R.drawable.flag);
    }
}
