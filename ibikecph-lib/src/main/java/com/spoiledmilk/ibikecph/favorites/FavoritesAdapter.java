// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;

public class FavoritesAdapter extends ArrayAdapter<FavoritesData> {

	public boolean isEditMode = false;
	private LeftMenu fragment;
	ArrayList<FavoritesData> data;

	public FavoritesAdapter(Context context, ArrayList<FavoritesData> objects, LeftMenu fragment) {
		super(context, R.layout.list_row_favorite, objects);
		this.fragment = fragment;
		data = objects;
	}

	public void setIsEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(getListRowLayout(), parent, false);
		TextView tv = (TextView) view.findViewById(R.id.textFavoriteName);
		String name = getItem(position).getName();
		if (name.length() > 19)
			name = name.substring(0, 19) + "...";
		tv.setText(name);
		tv.setTypeface(IbikeApplication.getNormalFont());
		tv.setTextColor(getTextColor());
		ImageButton btnEdit = (ImageButton) view.findViewById(R.id.btnEdit);
		final FavoritesData fd = getItem(position);
		tv.setPadding(getPadding(fd), 0, 0, 0);
		
		final ImageView imgIcon = ((ImageView) view.findViewById(R.id.icon));

		if (!isEditMode) {
			imgIcon.setImageResource(getIconResourceId(getItem(position)));
			btnEdit.setVisibility(View.GONE);
		} else {
			imgIcon.setImageResource(R.drawable.fav_reorder);
			btnEdit.setVisibility(View.VISIBLE);
		}

		return view;
	}

	public void reorder(int firstIndex, int secondIndex, boolean toNotify) {
		if (firstIndex != secondIndex) {
			LOG.d("Favorites reordering " + firstIndex + "->" + secondIndex);
			if (firstIndex < 0)
				firstIndex = 0;
			if (firstIndex > data.size() - 1)
				firstIndex = data.size() - 1;
			if (secondIndex < 0)
				secondIndex = 0;
			if (secondIndex > data.size() - 1)
				secondIndex = data.size() - 1;
			FavoritesData tmp = getItem(firstIndex);
			data.set(firstIndex, getItem(secondIndex));
			data.set(secondIndex, tmp);
		}
		if (toNotify) {
			notifyDataSetChanged();

			// reorder the favorites locally
			DB db = new DB(getContext());
			db.deleteFavorites();
			for (int i = 0; i < getCount(); i++) {
				db.saveFavorite(getItem(i), getContext(), false);
			}

			final JSONObject postObject = new JSONObject();
			try {
				postObject.put("auth_token", IbikeApplication.getAuthToken());
				JSONArray favorites = new JSONArray();
				for (int i = 0; i < data.size(); i++) {
					JSONObject item = new JSONObject();
					item.put("id", data.get(i).getApiId());
					item.put("position", i);
					favorites.put(i, item);
				}
				postObject.putOpt("pos_ary", favorites);
				new Thread(new Runnable() {
					@Override
					public void run() {
						HttpUtils.postToServer(Config.API_URL + "/favourites/reorder", postObject);
					}
				}).start();
			} catch (JSONException e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
	}

	protected int getIconResourceId(FavoritesData fd) {
		int ret = R.drawable.fav_star_grey_small;
		if (fd.getSubSource().equals(FavoritesData.favHome))
			ret = R.drawable.fav_home_grey;
		else if (fd.getSubSource().equals(FavoritesData.favWork))
			ret = R.drawable.fav_work_grey;
		else if (fd.getSubSource().equals(FavoritesData.favSchool))
			ret = R.drawable.fav_school_grey;
		return ret;
	}

	protected int getListRowLayout() {
		return R.layout.list_row_favorite;
	}

	protected int getTextColor() {
		return getContext().getResources().getColor(R.color.TextLightGrey);
	}

	protected int getPadding(FavoritesData fd) {
		return 0;
	}

}
