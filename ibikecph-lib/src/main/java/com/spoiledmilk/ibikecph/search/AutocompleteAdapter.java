// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

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
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.*;

public class AutocompleteAdapter extends ArrayAdapter<SearchListItem> {

	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	static final int charCount = (int) ((Util.getScreenWidth() - Util.dp2px(28)) / Util.dp2px(11));

	String[] splitted;
	private int stringLength = 0;
	private boolean isA;
	private ViewHolder viewHolder;

	ArrayList<SearchListItem> newData = new ArrayList<SearchListItem>();

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
			viewHolder.textLocation.setTypeface(IbikeApplication.getNormalFont());
			viewHolder.textAddress = (TextView) view.findViewById(R.id.textAddress);
			viewHolder.textAddress.setTypeface(IbikeApplication.getNormalFont());
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
			String name = item.getName();
			textLocation.setText(name);
			textAddress.setVisibility(View.GONE);
		} else {
			String name = item.getName();
			String addr = item.getAdress();
			if (addr == null || addr.equals(name)) {
				addr = "";
				if (item instanceof KortforData && item.getNumber() != null && !item.getNumber().equals("")) {
					name += " " + item.getNumber();
				}
			}
			if (item.getZip() != null && !item.getZip().equals("") && !(item instanceof HistoryData)) {
				addr += (addr.equals("") ? "" : ", ") + item.getZip();
			}
			if (item.getCity() != null && !item.getCity().equals("") && !(item instanceof HistoryData)) {
				if (item.getZip() != null && !item.getZip().equals("")) {
					addr += " ";
				}
				addr += item.getCity();
			}
			if (!(item instanceof FoursquareData) && !(item instanceof KortforData) && !(item instanceof HistoryData)) {
				addr = "";
			} else if (item instanceof HistoryData) {
				if (item.getAdress() != null && !item.getAdress().equals(item.getName())) {
					addr = item.getAdress();
				} else {
					addr = "";
				}
			}
			if (item instanceof HistoryData) {
				LOG.d("history item name = " + name + " addr = " + addr);
			}
			Spannable WordtoSpan = new SpannableString(name), WordtoSpan2 = new SpannableString(addr);
			boolean found = false, found2 = false;
			for (String word : splitted) { // iterrate through the search string
				int index = name.toLowerCase(Locale.US).indexOf(word.toLowerCase(Locale.US));
				int index2 = addr.toLowerCase(Locale.US).indexOf(word.toLowerCase(Locale.US));
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
			if (addr == null || addr.equals("")) {
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
				textAddress.setText(addr);
			}
		}
		if (item.type == SearchListItem.nodeType.HISTORY) {
			textLocation.setPadding(Util.dp2px(18), 0, 0, 0);
			textAddress.setPadding(Util.dp2px(18), 0, 0, 0);
		} else if (item.type == SearchListItem.nodeType.CURRENT_POSITION) {
			textLocation.setPadding(Util.dp2px(38), 0, 0, 0);
			textAddress.setPadding(Util.dp2px(38), 0, 0, 0);
		} else if (item.type == SearchListItem.nodeType.KORTFOR) {
			if (item instanceof KortforData && ((KortforData) item).isPlace()) {
				textLocation.setPadding(Util.dp2px(28), 0, 0, 0);
				textAddress.setPadding(Util.dp2px(26), 0, 0, 0);
			} else {
				textLocation.setPadding(Util.dp2px(24), 0, 0, 0);
				textAddress.setPadding(Util.dp2px(24), 0, 0, 0);
			}
		} else {
			textLocation.setPadding(Util.dp2px(28), 0, 0, 0);
			textAddress.setPadding(Util.dp2px(28), 0, 0, 0);
		}
		if (item.getIconResourceId() > 0) {
			imgIcon.setImageResource(item.getIconResourceId());
		} else {
			imgIcon.setImageResource(R.color.Transparent);
		}
		return view;
	}

	final private Comparator<SearchListItem> comparator = new Comparator<SearchListItem>() {
		public int compare(SearchListItem e1, SearchListItem e2) {
			int ret = (int) (e1.getDistance() - e2.getDistance());
			return ret;
		}
	};

	public void updateListData(String searchStr, Address addr) {
		// fetch local favourites and search history from the database
		if (searchStr == null || searchStr.trim().length() == 0) {
			clear();
			return;
		}
		Location loc = SMLocationManager.getInstance().hasValidLocation() ? SMLocationManager.getInstance().getLastValidLocation()
				: Util.COPENHAGEN;
		splitString(searchStr);
		if (searchStr.length() != stringLength) {
			clear();
			if (isA) {
				add(new CurrentLocation());
			}
			stringLength = searchStr.length();
			ArrayList<SearchListItem> historyList = new DB(getContext()).getSearchHistoryForString(searchStr);
			Iterator<SearchListItem> it = historyList.iterator();
			while (it.hasNext()) {
				HistoryData sli = (HistoryData) it.next();
				Address a = AddressParser.parseAddressRegex(sli.getName().replaceAll(",", ""));
				sli.setName(a.street + " " + a.number);
				sli.setAddress(((a.zip != null && !a.zip.equals("")) ? a.zip + " " : "") + a.city);
				sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getLatitude(), sli.getLongitude())));
				add(sli);
			}
			ArrayList<SearchListItem> favoritesList = new DB(getContext()).getFavoritesForString(searchStr);
			Iterator<SearchListItem> it2 = favoritesList.iterator();
			while (it2.hasNext()) {
				FavoritesData sli = (FavoritesData) it2.next();
				sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getLatitude(), sli.getLongitude())));
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
		Location loc = SMLocationManager.getInstance().hasValidLocation() ? SMLocationManager.getInstance().getLastValidLocation()
				: Util.COPENHAGEN;
		boolean isForPreviousCleared = false, isKMSPreviousCleared = false;
		splitString(searchStr);
		if (searchStr.length() != stringLength) {
			clear();
			if (isA) {
				add(new CurrentLocation());
			}
			if (list != null && list.size() == 1 && (list.get(0) instanceof FavoritesData || list.get(0) instanceof HistoryData)) {
				add(list.get(0));
			} else {
				// add local favourites and search history
				stringLength = searchStr.length();
				ArrayList<SearchListItem> historyList = new DB(getContext()).getSearchHistoryForString(searchStr);
				Iterator<SearchListItem> it = historyList.iterator();
				while (it.hasNext()) {
					HistoryData sli = (HistoryData) it.next();
					Address a = AddressParser.parseAddressRegex(sli.getName().replaceAll(",", ""));
					sli.setName(a.street + " " + a.number);
					sli.setAddress(((a.zip != null && !a.zip.equals("")) ? a.zip + " " : "") + a.city);
					sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getLatitude(), sli.getLongitude())));
					add(sli);
				}
				ArrayList<SearchListItem> favoritesList = new DB(getContext()).getFavoritesForString(searchStr);
				Iterator<SearchListItem> it2 = favoritesList.iterator();
				while (it2.hasNext()) {
					FavoritesData sli = (FavoritesData) it2.next();
					sli.setDistance(loc.distanceTo(Util.locationFromCoordinates(sli.getLatitude(), sli.getLongitude())));
					add(sli);
				}
			}
		}
		if (list != null) {
			// add data from Foursquare and Kortforsyningen
			Iterator<SearchListItem> it = list.iterator();
			while (it.hasNext()) {
				SearchListItem s = it.next();
				if (s instanceof FoursquareData) { // Foursquare
					if (!isForPreviousCleared) {
						ArrayList<SearchListItem> itemsToRemove = new ArrayList<SearchListItem>();
						for (int i = 0; i < super.getCount(); i++) {
							if (getItem(i).getClass().equals(FoursquareData.class)) {
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
				} else if (s instanceof KortforData) { // Kortforsyningen
					if (!isKMSPreviousCleared) {
						ArrayList<SearchListItem> itemsToRemove = new ArrayList<SearchListItem>();
						for (int i = 0; i < super.getCount(); i++) {
							if (getItem(i).getClass().equals(KortforData.class)) {
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

	public int getStringLength() {
		return stringLength;
	}

	private class ViewHolder {
		public TextView textLocation;
		public TextView textAddress;
		public ImageView imgIcon;
	}
}
