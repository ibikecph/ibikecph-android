// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.R;

import java.util.ArrayList;

public class HistoryAdapter extends ArrayAdapter<SearchListItem> {

    private LayoutInflater inflater;

    public HistoryAdapter(Context context, ArrayList<SearchListItem> objects) {
        super(context, R.layout.list_row_history, objects);
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.list_row_history, parent, false);
            holder = new ViewHolder();
            view.setTag(holder);
            holder.imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            holder.textLocation = (TextView) view.findViewById(R.id.textHistory);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        final SearchListItem item = getItem(position);

        String text = item.getName();
        if (item.getName().length() > 40) {
            text = item.getName().substring(0, 37) + "...";
        }
        holder.textLocation.setText(text);

        // Don't show an icon if the resource ID is set to -1
        if (item.getIconResourceId() != -1) {
            holder.imgIcon.setImageResource(R.drawable.fav_star);
        } else {
            holder.imgIcon.setVisibility(View.GONE);
        }
        return view;
    }

    class ViewHolder {
        public ImageView imgIcon;
        public TextView textLocation;
    }
}
