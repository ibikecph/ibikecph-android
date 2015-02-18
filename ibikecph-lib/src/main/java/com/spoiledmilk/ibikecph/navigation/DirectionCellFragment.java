// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import java.util.regex.Pattern;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;


public class DirectionCellFragment extends Fragment {

	TextView textWayname;
	TextView textDistance;
	ImageView imgDirectionIcon;
	ImageView imgLeftArrow;
	ImageView imgRightArrow;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.direction_top_cell, container, false);

		// view.setBackgroundResource(R.drawable.normal_directions_bg);

		textWayname = (TextView) view.findViewById(R.id.textWayname);
		textDistance = (TextView) view.findViewById(R.id.textDistance);
		imgDirectionIcon = ((ImageView) view.findViewById(R.id.imgDirectionIcon));
		imgLeftArrow = ((ImageView) view.findViewById(R.id.imgLeftArrow));
		imgRightArrow = ((ImageView) view.findViewById(R.id.imgRightArrow));

		init();

		return view;
	}

	private static Pattern specialWaynamePattern = Pattern.compile("\\{.+\\:.+\\}");

	private void init() {
		Bundle args = getArguments();
		if (args != null) {
			String wayName = args.getString("wayName");
			float lengthInMeters = args.getFloat("lengthInMeters", 0);
			LOG.d("wayname inside fragment = " + wayName);
			if (wayName != null && specialWaynamePattern.matcher(wayName).matches())
				textWayname.setText(IbikeApplication.getString(wayName));
			else
				textWayname.setText(wayName);
			textWayname.setTypeface(IbikeApplication.getBoldFont());
			textDistance.setText(Util.formatDistance(lengthInMeters));
			textDistance.setTypeface(IbikeApplication.getBoldFont());
			int imageResource = args.getInt("directionImageResource", -1);
			imgDirectionIcon.setImageResource(imageResource);
			imgDirectionIcon.invalidate();

			if (args.getBoolean("isFirst", false))
				imgLeftArrow.setVisibility(View.GONE);
			else
				imgLeftArrow.setVisibility(View.VISIBLE); // arrows are invinsible in maximized view by default
			if (args.getBoolean("isLast", false))
				imgRightArrow.setVisibility(View.GONE);
			else
				imgRightArrow.setVisibility(View.VISIBLE);
		}
	}
}
