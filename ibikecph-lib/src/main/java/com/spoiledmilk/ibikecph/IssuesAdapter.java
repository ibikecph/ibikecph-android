// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Acts as a frontend for issues that can be picked in a Spinner.
 * @author jens
 *
 */
public class IssuesAdapter extends ArrayAdapter<String> implements SpinnerAdapter {

	private int layoutResource = R.layout.list_row_issues;
	private int spinnerLayoutResource;

	public IssuesAdapter(Context context, ArrayList<String> objects, int layoutResource, int spinnerLayoutResource) {
		super(context, layoutResource, objects);
		this.layoutResource = layoutResource;
		this.spinnerLayoutResource = spinnerLayoutResource;
	}

	public IssuesAdapter(Context context, ArrayList<String> objects) {
		super(context, R.layout.list_row_issues, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(spinnerLayoutResource, parent, false);
		TextView textTitle = (TextView) view.findViewById(R.id.textTitle);
		textTitle.setText(getItem(position));
		textTitle.setTypeface(IBikeApplication.getBoldFont());
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(layoutResource, parent, false);
		TextView textInstruction = (TextView) view.findViewById(R.id.textInstruction);
		textInstruction.setText(getItem(position));
		return view;
	}
}
