// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.controls;

import android.support.v4.app.FragmentManager;

import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationActivity;
import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationMapFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

public class InstructionsPagerAdapter extends
		com.spoiledmilk.ibikecph.controls.InstructionsPagerAdapter {
	public InstructionsPagerAdapter(FragmentManager fm,
			SMRouteNavigationMapFragment mapFragment,
			SMRouteNavigationActivity activity) {
		super(fm, mapFragment, activity);
	}

	@Override
	protected int getImageResource(SMTurnInstruction turn) {
		return turn.getBlackDirectionImageResource();
	}
}
