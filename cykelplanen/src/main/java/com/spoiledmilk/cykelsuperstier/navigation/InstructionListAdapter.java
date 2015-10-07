// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.navigation;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

public class InstructionListAdapter extends com.spoiledmilk.ibikecph.navigation.InstructionListAdapter {

	public InstructionListAdapter(Activity context, int textViewResourceId, SMRoute route) {
		super(context, textViewResourceId, route);

	}

	@Override
	protected Drawable getTopImageResource(SMTurnInstruction instruction, Context context) {
		try {
			return instruction.smallDirectionIcon(context);
		} catch (Exception e) {
			return new ColorDrawable(Color.TRANSPARENT);
		}
	}
}
