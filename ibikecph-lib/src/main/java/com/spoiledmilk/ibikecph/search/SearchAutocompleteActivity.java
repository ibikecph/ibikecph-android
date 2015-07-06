// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.SearchListItem.nodeType;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchAutocompleteActivity extends Activity {

	public static final int RESULT_AUTOTOCMPLETE_SET = 103;
	public static final int RESULT_AUTOTOCMPLETE_NOT_SET = 104;

	private EditText textSrch;
	private ListView listSearch;
	private AutocompleteAdapter adapter;
	private SearchListItem currentSelection;
	private int lastTextSize = 0;
	private boolean addressPicked = false, isA = false, isOirestFetched = false, isFoursquareFetched = false, isClose = false,
			isAddressSearched = false;
	private Address addr;
	private ProgressBar progressBar;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_autocomplete_activiy);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setVisibility(View.INVISIBLE);
		Bundle data = getIntent().getExtras();
		if (data != null) {
			isA = data.getBoolean("isA", false);
		}
		textSrch = (EditText) findViewById(R.id.textLocation);
		textSrch.addTextChangedListener(new MyTextWatcher());
		textSrch.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
		textSrch.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					if (currentSelection == null && listSearch.getAdapter() != null && listSearch.getAdapter().getCount() > 0) {
						onItemClicked(0, false);
					} else if (currentSelection != null) {
						finishEditing();
					}
					return true;
				}
				return false;
			}
		});
		listSearch = (ListView) findViewById(R.id.listSearch);
		adapter = new AutocompleteAdapter(this, new ArrayList<SearchListItem>(), isA);
		listSearch.setAdapter(adapter);
		if (isA) {
			adapter.add(new CurrentLocation());
		}
		listSearch.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
				onItemClicked(position, true);
			}

		});
		if (data != null && data.containsKey("lastName")) {
			ArrayList<SearchListItem> listData = new ArrayList<SearchListItem>();
			DB db = new DB(this);
			String reuseName = data.getString("lastName");
			SearchListItem sli = db.getFavoriteByName(reuseName);
			if (sli != null) {
				listData.add(sli);
			} else {
				sli = db.getSearchHistoryByName(reuseName);
				if (sli != null) {
					listData.add(sli);
				} else {
					SharedPreferences prefs = getPreferences(MODE_PRIVATE);
					String nodeStr = prefs.getString("lastSearchItem", null);
					if (nodeStr != null) {
						JsonNode node = Util.stringToJsonNode(nodeStr);
						if (node != null) {
							sli = SearchListItem.instantiate(node);
							if (sli != null) {
								listData.add(sli);
							}
						}
					}
				}
			}
			db.close();
			addr = AddressParser.parseAddressRegex(reuseName);
			adapter.updateListData(listData, AddressParser.addresWithoutNumber(reuseName), addr);
			textSrch.setText(reuseName);
			textSrch.setSelection(reuseName.length());
		}
	}

	private void onItemClicked(int position, boolean isFromAdapter) {
		currentSelection = (SearchListItem) listSearch.getAdapter().getItem(position);
		boolean isFinishing = false;
		if (currentSelection.type != SearchListItem.nodeType.KORTFOR && currentSelection.type != SearchListItem.nodeType.CURRENT_POSITION) {
			isFinishing = true;
			finishAndPutData();
		} else if (currentSelection.type == SearchListItem.nodeType.CURRENT_POSITION) {
			isFinishing = true;
			(new Thread() {

				@Override
				public void run() {
					JsonNode node = HTTPAutocompleteHandler.getOiorestAddress(currentSelection.getLatitude(),
							currentSelection.getLongitude());
					if (node != null) {
						currentSelection.jsonNode = node;
					}

					runOnUiThread(new Runnable() {
						public void run() {
							finishAndPutData();
						}
					});
				}
			}).start();
		} else if (currentSelection instanceof KortforData) {
			if (((KortforData) currentSelection).isPlace()) {
				isFinishing = true;
				finishAndPutData();
			} else {
				KortforData kd = (KortforData) currentSelection;
				if (kd.getNumber() != null && !kd.getNumber().equals("") && kd.hasCoordinates()) {
					isAddressSearched = true;
					addr.houseNumber = kd.getNumber();
					isFinishing = true;
					finishAndPutData();
				}
			}
		}

		if (!isFinishing) {
			addressPicked = true;
			String number = AddressParser.numberFromAddress(textSrch.getText().toString());
			if (isFromAdapter) {
				textSrch.setText(currentSelection.getOneLineName());
			}
			int firstCommaIndex = textSrch.getText().toString().indexOf(',');
			if (number != null) {
				if (firstCommaIndex > 0) {
					String street = textSrch.getText().toString().replaceAll("\n", ",").substring(0, firstCommaIndex);
					String city = textSrch.getText().toString().replaceAll("\n", ",")
							.substring(firstCommaIndex, textSrch.getText().toString().length() - 1);
					if (isFromAdapter) {
						textSrch.setText(street + " " + number + city);
						textSrch.setSelection(street.length() + 2);
					}
				} else
					textSrch.setText(textSrch.getText().toString() + " " + number);
			} else if (firstCommaIndex > 0) {
				// add a whitespace for a house number input
				if (isFromAdapter) {
					textSrch.setText(textSrch.getText().toString().substring(0, firstCommaIndex) + " "
							+ textSrch.getText().toString().substring(firstCommaIndex, textSrch.getText().toString().length()));
					textSrch.setSelection(firstCommaIndex + 1);
				}
			}
			if (!isFromAdapter) {
				if (addr.houseNumber == null || addr.houseNumber.equals("") && currentSelection != null && currentSelection.getNumber() != null) {
					addr.houseNumber = currentSelection.getName();
				}
				finishEditing();
			}

			else if (currentSelection != null && currentSelection.getNumber() != null && !currentSelection.number.equals("")) {
				addr.houseNumber = currentSelection.getNumber();
				finishEditing();
			}
		}

	}

	private void finishEditing() {
		progressBar.setVisibility(View.VISIBLE);
		if (addr != null && (addr.houseNumber == null || addr.houseNumber.equals("")) && currentSelection != null
				&& currentSelection instanceof KortforData && !((KortforData) currentSelection).isPlace()) {
			addr.houseNumber = "1";
		}
		performGeocode();
	}

	private void performGeocode() {
		(new Thread() {
			@Override
			public void run() {
				isAddressSearched = true;
				if (addr != null && addr.houseNumber != null && !addr.houseNumber.trim().equals("") && !isClose) {
					JsonNode node;
					try {
						if (currentSelection == null) {
							currentSelection = new KortforData(AddressParser.addresWithoutNumber(textSrch.getText().toString()),
									addr.houseNumber);
						}
						LOG.d("Street searchfor the number " + addr.houseNumber);
						String urlString = "http://geo.oiorest.dk/adresser.json?q="
								+ URLEncoder.encode(currentSelection.getStreet() + " " + addr.houseNumber, "UTF-8");
						boolean coordinatesFound = false;
						if (adapter != null && adapter.getCount() > 0 && adapter.getItem(0) instanceof KortforData) {
							KortforData kd = (KortforData) adapter.getItem(0);
							LOG.d("search first item number = " + kd.getNumber() + " parsed addres number = " + addr.houseNumber
									+ " first item lattitude = " + kd.getLatitude());
							if (kd.getNumber() != null && kd.getNumber().equals(addr.houseNumber) && kd.hasCoordinates()) {
								currentSelection.setLatitude(kd.getLatitude());
								currentSelection.setLongitude(kd.getLongitude());
								coordinatesFound = true;
							}
						}
						if (!coordinatesFound) {
							node = HTTPAutocompleteHandler.getOiorestGeocode(urlString, "" + addr.houseNumber);
							if (node != null) {
								if (node.has("wgs84koordinat") && node.get("wgs84koordinat").has("bredde")) {
									currentSelection.setLatitude(Double.parseDouble(node.get("wgs84koordinat").get("bredde").asText()));
								}
								if (node.has("wgs84koordinat") && node.get("wgs84koordinat").has("længde")) {
									currentSelection.setLongitude(Double.parseDouble(node.get("wgs84koordinat").get("længde").asText()));
								}
							}
						}
						if (currentSelection != null && currentSelection.getLatitude() > -1 && currentSelection.getLongitude() > -1)
							runOnUiThread(new Runnable() {
								public void run() {
									finishAndPutData();
								}
							});
					} catch (Exception e) {
						LOG.e(e.getLocalizedMessage());
					}

				} else if (isClose)
					finishAndPutData();
			}

		}).start();
	}

	@Override
	public void onResume() {
		super.onResume();
		InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
		progressBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onPause() {
		super.onPause();
		hideKeyboard();
	}

	public void updateListData(List<SearchListItem> list, String tag, Address addr) {
		if (textSrch.getText().toString().equals(tag)) {
			adapter.updateListData(list, AddressParser.addresWithoutNumber(textSrch.getText().toString()), addr);
		}
		if (isOirestFetched && isFoursquareFetched) {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	public void hideKeyboard() {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	private class MyTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable statusText) {
			if (kmsThread != null && kmsThread.isAlive()) {
				kmsThread.interrupt();
			}
			if (foursquareThread != null && foursquareThread.isAlive()) {
				foursquareThread.interrupt();
			}
			Address temp;
			temp = AddressParser.parseAddressRegex(textSrch.getText().toString().replaceAll("\n", ","));
			LOG.d("after text changed");
			if (addr == null || !addr.equals(temp)) {
				LOG.d("clearing the adapter and spawning the search threads");
				adapter.clear();
				if (textSrch.getText().toString().length() >= 2) {
					final Location loc1;
					if (IbikeApplication.getService().hasValidLocation()) {
						loc1 = IbikeApplication.getService().getLastValidLocation();
					} else if (IbikeApplication.getService().getLastKnownLocation() != null) {
						loc1 = IbikeApplication.getService().getLastKnownLocation();
					} else {
						loc1 = Util.COPENHAGEN;
					}
					final String searchText = AddressParser.addresWithoutNumber(textSrch.getText().toString());
					isOirestFetched = false;
					isFoursquareFetched = !(textSrch.getText().toString().length() > 2);
					isAddressSearched = false;
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							spawnSearchThreads(loc1, searchText, addr, textSrch.getText().toString());
						}
					}, 500);

					adapter.updateListData(textSrch.getText().toString(), addr);

					if (textSrch.getText().toString().length() != lastTextSize && textSrch.getText().toString().length() > 1
							&& !addressPicked) {
						progressBar.setVisibility(View.VISIBLE);
					}
					addressPicked = false;
					lastTextSize = textSrch.getText().toString().length();
				}
			}
			addr = temp;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int count, int after) {

		}
	}

	private Thread kmsThread, foursquareThread;
	private Address lastAddress = null;

	private void spawnSearchThreads(final Location loc, final String searchText, final Address addr, final String tag) {

		if (lastAddress != null && lastAddress.equals(addr) && adapter != null && adapter.getCount() != 0) {
			return;
		} else {
			lastAddress = addr;
		}
		if (!Util.isNetworkConnected(this)) {
			Util.launchNoConnectionDialog(this);
			progressBar.setVisibility(View.INVISIBLE);
		} else {
			// fetch the Kortforsyningen autocomplete
			kmsThread = new Thread(new Runnable() {
				@Override
				public void run() {
					// final List<JsonNode> kortforsyningenList = new ArrayList<JsonNode>();
					final ArrayList<SearchListItem> data = new ArrayList<SearchListItem>();
					if (!(addr.street == null || addr.street.trim().equals(""))) {

						List<JsonNode> list = HTTPAutocompleteHandler.getKortforsyningenAutocomplete(loc, addr);

						int count = 0;
						if (list != null) {
							for (JsonNode node : list) {
								if (count == 10) {
									break;
								}
								KortforData kd = new KortforData(node);
								if (kd.getCity() != null && addr.city != null && kd.getCity().toLowerCase(Locale.US).contains(addr.city)) {
									LOG.d("kd = " + kd);
								}
								if (addr.zip != null && !addr.zip.equals("") && kd.getZip() != null) {
									if (!addr.zip.trim().toLowerCase(Locale.UK).equals(kd.getZip().toLowerCase(Locale.UK))) {
										continue;
									}
								}
								LOG.d("kd = " + kd);
								if (kd.getCity() != null && addr.city != null && kd.getCity().toLowerCase(Locale.US).contains(addr.city)
										&& kd.getCity().contains("Aarhus")) {
									LOG.d("kd.city = " + kd.getCity() + " addr city = " + addr.city);
								}
								if (addr.city != null && !addr.city.equals("") && !addr.city.equals(addr.street) && kd.getCity() != null) {
									if (!(addr.city.trim().toLowerCase(Locale.UK).contains(kd.getCity().toLowerCase(Locale.UK)) || kd
											.getCity().trim().toLowerCase(Locale.UK).contains(addr.city.toLowerCase(Locale.UK)))) {
										continue;
									}
								}
								LOG.d("adding a kd to the list " + kd);
								data.add(kd);
								count++;
							}

						}
					}
					if (!addr.isAddress()) {
						List<JsonNode> places = HTTPAutocompleteHandler.getKortforsyningenPlaces(loc, addr);
						if (places != null) {
							int count = 0;
							if (places != null) {
								LOG.d("places count = " + places.size() + " data = " + places.toString());
								for (JsonNode node : places) {
									if (count == 10) {
										break;
									}
									KortforData kd = new KortforData(node);
									if (addr.zip != null && !addr.zip.equals("") && kd.getZip() != null) {
										if (!addr.zip.trim().toLowerCase(Locale.UK).equals(kd.getZip().toLowerCase(Locale.UK))) {
											continue;
										}
									}
									if (addr.city != null && !addr.city.equals("") && !addr.city.equals(addr.street)
											&& kd.getCity() != null) {
										if (!(addr.city.trim().toLowerCase(Locale.UK).contains(kd.getCity().toLowerCase(Locale.UK)) || kd
												.getCity().trim().toLowerCase(Locale.UK).contains(addr.city.toLowerCase(Locale.UK)))) {
											continue;
										}
									}
									data.add(kd);
									count++;
								}

							}
						}
					}
					isOirestFetched = true;
					runOnUiThread(new Runnable() {
						public void run() {
							updateListData(data, tag, addr);

						}
					});
				}

			});
			kmsThread.start();
			if (textSrch.getText().toString().length() >= 3 && addr.isFoursquare()) {
				// fetch the Foursquare autocomplete
				foursquareThread = new Thread(new Runnable() {
					@Override
					public void run() {
						List<JsonNode> list = HTTPAutocompleteHandler.getFoursquareAutocomplete(addr, SearchAutocompleteActivity.this, loc);
						final ArrayList<SearchListItem> data = new ArrayList<SearchListItem>();
						if (list != null) {
							int count = 0;
							for (JsonNode node : list) {
								if (count == 3) {
									break;
								}
								JsonNode location = node.path("location");
								if (location.has("lat") && location.has("lng") && location.get("lat").asDouble() != 0
										&& location.get("lng").asDouble() != 0) {
									String country = location.has("country") ? location.get("country").asText() : "";
									if (country.contains("Denmark") || country.contains("Dansk") || country.contains("Danmark")) {
										FoursquareData fd = new FoursquareData(node);
										fd.setDistance(loc.distanceTo(Util.locationFromCoordinates(fd.getLatitude(), fd.getLongitude())));
										data.add(fd);
										count++;
									}
								}
							}
						}
						isFoursquareFetched = true;
						runOnUiThread(new Runnable() {
							public void run() {
								updateListData(data, tag, addr);
							}
						});
					}
				});
				foursquareThread.start();
			} else {
				isFoursquareFetched = true;
			}
		}
	}

	boolean isFinishing = false;

	public void finishAndPutData() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				progressBar.setVisibility(View.INVISIBLE);
				if (isFinishing) {
					return;
				}
				isFinishing = true;
				Intent intent = new Intent();
				if (currentSelection != null) {
					if (isAddressSearched && addr != null) {
						intent.putExtra("number", addr.houseNumber);
					}
					if (currentSelection instanceof KortforData && !((KortforData) currentSelection).isPlace()) {
						String name = currentSelection.getName();
						if (addr != null && addr.houseNumber != null && !addr.houseNumber.equals("") && !addr.houseNumber.equals("1")
								&& AddressParser.containsNumber(addr.houseNumber)) {
							name += " " + addr.houseNumber;
						}
						if (currentSelection.getZip() != null && !currentSelection.getZip().equals("")) {
							name += ", " + currentSelection.getZip();
						}
						if (currentSelection.getCity() != null && !currentSelection.getCity().equals("")) {
							name += " " + currentSelection.getCity();
						}
						intent.putExtra("name", name);
					} else {
						intent.putExtra("name", currentSelection.getName());
					}
					if (currentSelection.type == nodeType.FOURSQUARE
							|| (currentSelection instanceof KortforData && ((KortforData) currentSelection).isPlace()))
						intent.putExtra("poi", currentSelection.getName());
					if (currentSelection.getAdress() != null) {
						String address = currentSelection.getAdress();
						intent.putExtra("address", address);
					}
					intent.putExtra("source", currentSelection.getSource());
					intent.putExtra("subsource", currentSelection.getSubSource());
					intent.putExtra("lat", currentSelection.getLatitude());
					intent.putExtra("lon", currentSelection.getLongitude());
					if (currentSelection instanceof FoursquareData
							|| (currentSelection instanceof KortforData && ((KortforData) currentSelection).isPlace())) {
						intent.putExtra("isPoi", true);
					}
					if (currentSelection.getZip() != null && !currentSelection.getZip().trim().equals("")) {
						intent.putExtra("zip", currentSelection.getZip());
					}
					if (currentSelection.getCity() != null && !currentSelection.getCity().trim().equals("")) {
						intent.putExtra("city", currentSelection.getCity());
					}
					if (currentSelection.getStreet() != null && !currentSelection.getStreet().trim().equals("")) {
						intent.putExtra("street", currentSelection.getStreet());
					}
					SearchAutocompleteActivity.this.setResult(RESULT_AUTOTOCMPLETE_SET, intent);
				} else {
					SearchAutocompleteActivity.this.setResult(RESULT_AUTOTOCMPLETE_NOT_SET, intent);
				}
				SharedPreferences prefs = getPreferences(MODE_PRIVATE);
				if (currentSelection != null) {
					JsonNode node = currentSelection.getJsonNode();
					if (node != null) {
						prefs.edit().putString("lastSearchItem", node.toString()).commit();
					}
				}
				finish();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

}
