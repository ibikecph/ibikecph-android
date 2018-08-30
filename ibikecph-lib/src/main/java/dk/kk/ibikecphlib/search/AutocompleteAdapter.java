// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.util.DB;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import java.util.*;

import dk.kk.ibikecphlib.util.Util;

public class AutocompleteAdapter extends ArrayAdapter<SearchListItem> {

	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	String[] splitted;
	private int stringLength = 0;
	private boolean isA;
	private ViewHolder viewHolder;

	public AutocompleteAdapter(Context context, ArrayList<SearchListItem> objects, boolean isA) {
		super(context, R.layout.list_row_search, objects);
		this.isA = isA;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.list_row_search, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.textLocation = (TextView) view.findViewById(R.id.textLocation);
			viewHolder.textAddress = (TextView) view.findViewById(R.id.textAddress);
			// TODO: Move this to a layout instead of changing text size in code
			if (Util.getDensity() > 1.5f) {
				viewHolder.textLocation.setTextSize(22);
				viewHolder.textAddress.setTextSize(20);
			} else if (Util.getDensity() > 1f) {
				viewHolder.textLocation.setTextSize(18);
				viewHolder.textAddress.setTextSize(16);
			}
			viewHolder.imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		final SearchListItem item = getItem(position);
		final ImageView imgIcon = viewHolder.imgIcon;
		final TextView textLocation = viewHolder.textLocation;
		final TextView textAddress = viewHolder.textAddress;

		if (item.type == SearchListItem.nodeType.CURRENT_POSITION) {
			String name = item.getAddress().getName();
			textLocation.setText(name);
			textAddress.setVisibility(View.GONE);
		} else {
			String name = item.getPrimaryDisplayString();
			String secondary = item.getSecondaryDisplayString();

			Spannable WordtoSpan = new SpannableString(name), WordtoSpan2 = new SpannableString(secondary);
			boolean found = false, found2 = false;
			for (String word : splitted) { // iterrate through the search string
				int index = name.toLowerCase(Locale.US).indexOf(word.toLowerCase(Locale.US));
				int index2 = secondary.toLowerCase(Locale.US).indexOf(word.toLowerCase(Locale.US));
				if (index >= 0) {
					try {
						WordtoSpan.setSpan(new ForegroundColorSpan(Color.BLACK), index, index + word.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						found = true;
					} catch (Exception e) {
						LOG.e(e.getLocalizedMessage());
					}
				}
				if (index2 >= 0) {
					try {
						WordtoSpan2.setSpan(new ForegroundColorSpan(Color.BLACK), index2, index2 + word.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						found2 = true;
					} catch (Exception e) {
						LOG.e(e.getLocalizedMessage());
					}
				}

			}
			if (secondary == null || secondary.equals("")) {
				textAddress.setVisibility(View.GONE);
			} else {
				textAddress.setVisibility(View.VISIBLE);
			}
			if (found) {
				textLocation.setText(WordtoSpan);
			} else {
				textLocation.setText(name);
			}
			if (found2) {
				textAddress.setText(WordtoSpan2);
			} else {
				textAddress.setText(secondary);
			}
		}
		imgIcon.setImageResource(item.getIconResourceId());
		return view;
	}

	final private Comparator<SearchListItem> comparator = new Comparator<SearchListItem>() {
		public int compare(SearchListItem e1, SearchListItem e2) {
			int distance = (int) Math.round(e1.getDistance() - e2.getDistance());
			if(distance == 0) {
				return e1.getOrder() - e2.getOrder();
			} else {
				return distance;
			}
		}
	};

	public void updateListData(String searchStr, Address addr) {
		// fetch local favourites and search history from the database
		if (searchStr == null || searchStr.trim().length() == 0) {
			clear();
			return;
		}
		Location loc = IBikeApplication.getService().hasValidLocation() ? IBikeApplication.getService().getLastValidLocation()
				: Util.COPENHAGEN;
		splitString(searchStr);
		if (searchStr.length() != stringLength) {
			clear();
			if (isA) {
				add(new CurrentLocationListItem());
			}
			stringLength = searchStr.length();
			ArrayList<SearchListItem> historyList = new DB(getContext()).getSearchHistoryForString(searchStr);
			Iterator<SearchListItem> it = historyList.iterator();
			while (it.hasNext()) {
				HistoryListItem sli = (HistoryListItem) it.next();
				sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getAddress().getLocation().getLatitude(), sli.getAddress().getLocation().getLongitude())));
				add(sli);
			}
			ArrayList<SearchListItem> favoritesList = new DB(getContext()).getFavoritesForString(searchStr);
			Iterator<SearchListItem> it2 = favoritesList.iterator();
			while (it2.hasNext()) {
				FavoriteListItem sli = (FavoriteListItem) it2.next();
				sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getAddress().getLocation().getLatitude(), sli.getAddress().getLocation().getLongitude())));
				add(sli);
			}
		}
		super.sort(comparator);
		notifyDataSetChanged();
	}

	public void updateListData(List<SearchListItem> list, String searchStr, Address addr) {
		// search items fetched from server
		if (searchStr == null || searchStr.trim().length() == 0) {
			clear();
			return;
		}
		Location loc = IBikeApplication.getService().hasValidLocation() ? IBikeApplication.getService().getLastValidLocation()
				: Util.COPENHAGEN;
		boolean isForPreviousCleared = false, isKMSPreviousCleared = false;
		splitString(searchStr);
		if (searchStr.length() != stringLength) {
			clear();
			if (isA) {
				add(new CurrentLocationListItem());
			}
			if (list != null && list.size() == 1 && (list.get(0) instanceof FavoriteListItem || list.get(0) instanceof HistoryListItem)) {
				add(list.get(0));
			} else {
				// add local favourites and search history
				stringLength = searchStr.length();
				ArrayList<SearchListItem> historyList = new DB(getContext()).getSearchHistoryForString(searchStr);
				Iterator<SearchListItem> it = historyList.iterator();
				while (it.hasNext()) {
					HistoryListItem sli = (HistoryListItem) it.next();
					sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getAddress().getLocation().getLatitude(), sli.getAddress().getLocation().getLongitude())));
					add(sli);
				}
				ArrayList<SearchListItem> favoritesList = new DB(getContext()).getFavoritesForString(searchStr);
				Iterator<SearchListItem> it2 = favoritesList.iterator();
				while (it2.hasNext()) {
					dk.kk.ibikecphlib.favorites.FavoriteListItem sli = (FavoriteListItem) it2.next();
					sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getAddress().getLocation().getLatitude(), sli.getAddress().getLocation().getLongitude())));
					add(sli);
				}
			}
		}
		if (list != null) {
			// add data from Foursquare and Kortforsyningen
			Iterator<SearchListItem> it = list.iterator();
			while (it.hasNext()) {
				SearchListItem s = it.next();
				if (s instanceof FoursquareListItem) { // Foursquare
					if (!isForPreviousCleared) {
						ArrayList<SearchListItem> itemsToRemove = new ArrayList<SearchListItem>();
						for (int i = 0; i < super.getCount(); i++) {
							if (getItem(i).getClass().equals(FoursquareListItem.class)) {
								itemsToRemove.add(getItem(i));
							}
						}
						Iterator<SearchListItem> it2 = itemsToRemove.iterator();
						while (it2.hasNext()) {
							SearchListItem sli = it2.next();
							super.remove(sli);
							it2.remove();
						}
						isForPreviousCleared = true;
					}
					add(s);
				} else if (s instanceof KortforsyningenListItem) { // Kortforsyningen
					if (!isKMSPreviousCleared) {
						ArrayList<SearchListItem> itemsToRemove = new ArrayList<SearchListItem>();
						for (int i = 0; i < super.getCount(); i++) {
							if (getItem(i).getClass().equals(KortforsyningenListItem.class)) {
								itemsToRemove.add(getItem(i));
							}
						}
						Iterator<SearchListItem> it2 = itemsToRemove.iterator();
						while (it2.hasNext()) {
							SearchListItem sli = it2.next();
							super.remove(sli);
							it2.remove();
						}
						isKMSPreviousCleared = true;
					}
					add(s);
				}

			}
		}
		super.sort(comparator);
		notifyDataSetChanged();
	}

	private void splitString(String searchStr) {
		splitted = null;
		splitted = searchStr.split("\\s");
		if (splitted == null) {
			splitted = new String[1];
			splitted[0] = searchStr;
		}
		int j = 0;
		for (String word : splitted) {
			word = word.trim();
			if (word.length() > 0) {
				if (word.charAt(word.length() - 1) == ',') {
					word = word.substring(0, word.length() - 1);
				}
				if (word.length() > 0 && word.charAt(0) == ',') {
					word = word.substring(0, word.length());
				}
			}
			splitted[j] = word;
			j++;
		}
	}

	private class ViewHolder {
		public TextView textLocation;
		public TextView textAddress;
		public ImageView imgIcon;
	}
}
