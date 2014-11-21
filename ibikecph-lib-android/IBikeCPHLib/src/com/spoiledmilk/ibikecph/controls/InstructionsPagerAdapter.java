// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.controls;

import java.util.regex.Pattern;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.navigation.DirectionCellFragment;
import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationActivity;
import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationMapFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

public class InstructionsPagerAdapter extends FragmentStatePagerAdapter {

	SMRouteNavigationMapFragment mapFragment;
	boolean instructionsUpdated = false;
	SMRouteNavigationActivity activity;
	private static Pattern specialWaynamePattern = Pattern.compile("\\{.+\\:.+\\}");

	public InstructionsPagerAdapter(FragmentManager fm, SMRouteNavigationMapFragment mapFragment, SMRouteNavigationActivity activity) {
		super(fm);
		this.activity = activity;
		this.mapFragment = mapFragment;
	}

	@Override
	public int getCount() {
		int ret = 0;
		if (mapFragment != null && mapFragment.route != null && mapFragment.route.getTurnInstructions() != null) {
			ret = mapFragment.route.getTurnInstructions().size();
		}
		return ret;
	}

	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	public void setInstructionsUpdated(boolean b) {
		instructionsUpdated = b;
	}

	@Override
	public Fragment getItem(int position) {
		DirectionCellFragment dcf = new DirectionCellFragment();
		Bundle args = new Bundle();
		SMTurnInstruction turn = mapFragment.route.getTurnInstructions().get(position);
		String wayname = "";
		if (turn.drivingDirection == SMTurnInstruction.TurnDirection.ReachingDestination) {
			wayname = IbikeApplication.getString("direction_100");
		} else if (turn.drivingDirection == SMTurnInstruction.TurnDirection.ReachedYourDestination) {
			wayname = IbikeApplication.getString("direction_15");
		} else if (turn.drivingDirection == SMTurnInstruction.TurnDirection.Station) {
			wayname = turn.wayName;
		} else {
			if (specialWaynamePattern.matcher(turn.wayName).matches()) {
				wayname = IbikeApplication.getString(turn.wayName);
			} else {
				wayname = turn.wayName;
			}

		}
		
		wayname = turn.getPrefix() + wayname;
		
		args.putString("wayName", wayname);
		args.putFloat("lengthInMeters", turn.lengthInMeters);
		args.putInt("directionImageResource", getImageResource(turn));
		if (position == 0) {
			args.putBoolean("isFirst", true);
		}
		if (position == mapFragment.route.getTurnInstructions().size() - 1) {
			args.putBoolean("isLast", true);
		}
		dcf.setArguments(args);
		return dcf;
	}

	protected int getImageResource(SMTurnInstruction turn) {
		return turn.getDirectionImageResource();
	}
}
