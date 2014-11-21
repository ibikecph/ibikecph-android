// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class InstructionListAdapter extends ArrayAdapter<SMTurnInstruction> {

	private LayoutInflater vi;
	private int type;
	ViewHolder holder;
	SMRoute route;

	public InstructionListAdapter(Activity context, int textViewResourceId, SMRoute route) {
		super(context, textViewResourceId, route.getTurnInstructions());
		vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.route = route;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return (position == 0) ? 0 : 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = convertView;

		type = getItemViewType(position);

		if (view == null) {

			if (type == 0) {
				view = vi.inflate(R.layout.instruction_top_view, null);
			} else {
				view = vi.inflate(R.layout.direction_cell, null);
			}
			holder = new ViewHolder();
			holder.textWayname = (TextView) view.findViewById(R.id.textWayname);
			holder.textDistance = (TextView) view.findViewById(R.id.textDistance);
			holder.directionIcon = (ImageView) view.findViewById(R.id.imgDirectionIcon);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		SMTurnInstruction instruction = null;
		if (position >= 0 && position < route.getTurnInstructions().size())
			instruction = route.getTurnInstructions().get(position);
		else {
			instruction = new SMTurnInstruction();
		}

		setInstructionCell(getContext(), holder, instruction, position == 0);

		return view;
	}

	private static Pattern specialWaynamePattern = Pattern.compile("\\{.+\\:.+\\}");

	public void setInstructionCell(Context context, ViewHolder holder, SMTurnInstruction instruction, boolean large) {
		holder.textWayname.setTypeface(IbikeApplication.getBoldFont());
		String wayname = "";
		if (instruction.drivingDirection == SMTurnInstruction.TurnDirection.ReachingDestination)
			wayname = IbikeApplication.getString("direction_100");
		else if (instruction.drivingDirection == SMTurnInstruction.TurnDirection.ReachedYourDestination)
			wayname = IbikeApplication.getString("direction_15");
		else if (specialWaynamePattern.matcher(instruction.wayName).matches()) {
			wayname = IbikeApplication.getString(instruction.wayName);
		} else {
			wayname = instruction.wayName;
		}

		wayname = instruction.getPrefix() + wayname;

		if (wayname.length() > 35) {
			wayname = wayname.substring(0, 32) + "...";
		}
		holder.textWayname.setText(wayname);
		holder.textDistance.setText(Util.formatDistance(instruction.lengthInMeters));
		holder.textDistance.setTypeface(IbikeApplication.getBoldFont());
		try {
			holder.directionIcon.setImageDrawable(large ? getTopImageResource(instruction, context) : instruction
					.smallDirectionIcon(context));
		} catch (Exception e) {
			LOG.e(e.getLocalizedMessage());
		}
	}

	protected Drawable getTopImageResource(SMTurnInstruction instruction, Context context) {
		return instruction.largeDirectionIcon(context);
	}

	static class ViewHolder {
		TextView textWayname;
		TextView textDistance;
		ImageView directionIcon;
	}

	@Override
	public int getCount() {
		return route.getTurnInstructions().size();
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		LOG.d("turn instructions in adapter = " + route.getTurnInstructions().toString());
	}

	public void setRoute(SMRoute route) {
		this.route = route;
	}

}
