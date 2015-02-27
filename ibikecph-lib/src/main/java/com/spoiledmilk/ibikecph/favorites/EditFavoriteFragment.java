// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.search.AddressParser;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.util.APIListener;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A Fragment used inside the LeftMenu for editing a favorite.
 * TODO: Can this be merged with AddFavoriteFragment?
 * @author jens
 *
 */
public class EditFavoriteFragment extends Fragment implements APIListener {

	protected EditText textAddress;
	protected EditText textFavoriteName;
	private ImageButton btnFavorite;
	private ImageButton btnHome;
	private ImageButton btnWork;
	private ImageButton btnSchool;
	protected TextView textFavorite;
	private TextView textHome;
	private TextView textWork;
	private TextView textSchool;
	private TexturedButton btnSave;
	private Button btnDelete;
	private FavoritesData favoritesData = null;
	private String currentFavoriteType;
	private AlertDialog dialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View ret = inflater.inflate(R.layout.fragment_edit_favorite, container, false);

		if (getArguments() != null) {
			favoritesData = getArguments().getParcelable("favoritesData");
		}

		textAddress = (EditText) ret.findViewById(R.id.textAddress);
		textAddress.setClickable(true);
		textAddress.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(getActivity(), SearchAutocompleteActivity.class);
				i.putExtra("isA", true);
				getActivity().startActivityForResult(i, 2);

			}

		});

		textFavoriteName = (EditText) ret.findViewById(R.id.textFavoriteName);
		textFavorite = (TextView) ret.findViewById(R.id.textFavorite);
		textHome = (TextView) ret.findViewById(R.id.textHome);
		textWork = (TextView) ret.findViewById(R.id.textWork);
		textSchool = (TextView) ret.findViewById(R.id.textSchool);

		btnFavorite = (ImageButton) ret.findViewById(R.id.btnFavorite);
		btnFavorite.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				unselectGraphics();
				btnFavorite.setImageResource(R.drawable.favtypefavoritebuttonpressed);
				if (isPredefinedName(textFavoriteName.getText().toString()))
					textFavoriteName.setText(IbikeApplication.getString("Favorite"));
				currentFavoriteType = FavoritesData.favFav;
				textFavorite.setTextColor(getSelectedTextColor());
			}
		});

		btnHome = (ImageButton) ret.findViewById(R.id.btnHome);
		btnHome.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				unselectGraphics();
				btnHome.setImageResource(R.drawable.favtypehomebuttonpressed);
				if (isPredefinedName(textFavoriteName.getText().toString()))
					textFavoriteName.setText(IbikeApplication.getString("Home"));
				currentFavoriteType = FavoritesData.favHome;
				textHome.setTextColor(getSelectedTextColor());
			}
		});

		btnWork = (ImageButton) ret.findViewById(R.id.btnWork);
		btnWork.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				unselectGraphics();
				btnWork.setImageResource(R.drawable.favtypeworkbuttonpressed);
				if (isPredefinedName(textFavoriteName.getText().toString()))
					textFavoriteName.setText(IbikeApplication.getString("Work"));
				currentFavoriteType = FavoritesData.favWork;
				textWork.setTextColor(getSelectedTextColor());
			}
		});

		btnSchool = (ImageButton) ret.findViewById(R.id.btnSchool);
		btnSchool.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				unselectGraphics();
				btnSchool.setImageResource(R.drawable.favtypeschoolbuttonpressed);
				if (isPredefinedName(textFavoriteName.getText().toString()))
					textFavoriteName.setText(IbikeApplication.getString("School"));
				currentFavoriteType = FavoritesData.favSchool;
				textSchool.setTextColor(getSelectedTextColor());
			}
		});

		btnSave = (TexturedButton) ret.findViewById(R.id.btnSave);
		btnSave.setTextureResource(R.drawable.btn_pattern_repeteable);
		btnSave.setBackgroundResource(R.drawable.btn_blue_selector);
		btnSave.setTextColor(Color.WHITE);
		btnSave.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (Util.isNetworkConnected(getActivity())) {
					if (favoritesData != null && textFavoriteName.getText().toString() != null
							&& !textFavoriteName.getText().toString().trim().equals("")) {
						if (new DB(getActivity()).favoritesForName(textFavoriteName.getText().toString().trim()) < 1
								|| favoritesData.getName().trim().equalsIgnoreCase(textFavoriteName.getText().toString())) {
							String st = favoritesData.getName() + " - (" + favoritesData.getLatitude() + "," + favoritesData.getLongitude()
									+ ")";
							IbikeApplication.getTracker().sendEvent("Favorites", "Save", st, (long) 0);
							favoritesData.setName(textFavoriteName.getText().toString());
							favoritesData.setAdress(textAddress.getText().toString());
							favoritesData.setSubSource(currentFavoriteType);
							Thread updateThread = new Thread(new Runnable() {
								@Override
								public void run() {
									(new DB(getActivity())).updateFavorite(favoritesData, getActivity(), EditFavoriteFragment.this);
								}
							});
							getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
							updateThread.start();
						} else {
							Builder builder = new AlertDialog.Builder(getActivity());
							builder.setMessage(IbikeApplication.getString("name_used"));
							builder.setTitle(IbikeApplication.getString("Error"));
							builder.setCancelable(false);
							builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
							dialog = builder.create();
							dialog.show();
						}
					} else if (getActivity() != null) {
						Util.showSimpleMessageDlg(getActivity(), IbikeApplication.getString("register_error_fields"));
					}
				} else {
					Util.launchNoConnectionDialog(getActivity());
				}
			}
		});

		btnDelete = (Button) ret.findViewById(R.id.btnDelete);
		btnDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (Util.isNetworkConnected(getActivity())) {
					getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
					final FavoritesData temp = favoritesData;
					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								final JSONObject postObject = new JSONObject();
								postObject.put("auth_token", IbikeApplication.getAuthToken());
								if (temp.getApiId() < 0) {
									int apiId = new DB(getActivity()).getApiId(temp.getId());
									if (apiId != -1) {
										temp.setApiId(apiId);
									}
								}
								JsonNode ret = HttpUtils.deleteFromServer(Config.API_URL + "/favourites/" + temp.getApiId(), postObject);
								if (ret != null && ret.has("success")) {
									if (ret.path("success").asBoolean()) {
										if (getActivity() != null) {
											getActivity().runOnUiThread(new Runnable() {

												@Override
												public void run() {
													btnDelete.setTextColor(Color.WHITE);
													String st = favoritesData.getName() + " - (" + favoritesData.getLatitude() + ","
															+ favoritesData.getLongitude() + ")";
													IbikeApplication.getTracker().sendEvent("Favorites", "Delete", st, (long) 0);
													(new DB(getActivity())).deleteFavorite(favoritesData, getActivity());
													popFragment();
												}
											});

										}

									} else {
										launchErrorDialog(ret.path("info").asText());
									}
								} else {
									launchErrorDialog("Error");
								}

							} catch (Exception e) {
								LOG.e(e.getLocalizedMessage());
							}
						}

					}).start();

				} else {
					Util.launchNoConnectionDialog(getActivity());
				}
			}

		});

		return ret;
	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		textAddress.setText(favoritesData.getAdress());
		textAddress.setTypeface(IbikeApplication.getNormalFont());
		textFavoriteName.setText(favoritesData.getName());
		textFavoriteName.setTypeface(IbikeApplication.getNormalFont());
		textFavorite.setText(IbikeApplication.getString("Favorite"));
		textFavorite.setTypeface(IbikeApplication.getNormalFont());
		textHome.setText(IbikeApplication.getString("Home"));
		textHome.setTypeface(IbikeApplication.getNormalFont());
		textWork.setText(IbikeApplication.getString("Work"));
		textWork.setTypeface(IbikeApplication.getNormalFont());
		textSchool.setText(IbikeApplication.getString("School"));
		textSchool.setTypeface(IbikeApplication.getNormalFont());
		btnSave.setText(IbikeApplication.getString("save_favorite"));
		btnSave.setTypeface(IbikeApplication.getBoldFont());
		btnDelete.setText(IbikeApplication.getString("delete_favorite"));
		btnDelete.setTypeface(IbikeApplication.getNormalFont());
		currentFavoriteType = favoritesData.getSubSource();
		updateGraphics();
	}

	private void unselectGraphics() {
		btnFavorite.setImageResource(R.drawable.favtypefavoritebutton);
		textFavorite.setTextColor(getUnSelectedTextColor());
		btnHome.setImageResource(R.drawable.favtypehomebutton);
		textHome.setTextColor(getUnSelectedTextColor());
		btnWork.setImageResource(R.drawable.favtypeworkbutton);
		textWork.setTextColor(getUnSelectedTextColor());
		btnSchool.setImageResource(R.drawable.favtypeschoolbutton);
		textSchool.setTextColor(getUnSelectedTextColor());
	}

	private void updateGraphics() {
		unselectGraphics();
		if (currentFavoriteType.equals(FavoritesData.favHome)) {
			btnHome.setImageResource(R.drawable.favtypehomebuttonpressed);
			textHome.setTextColor(getSelectedTextColor());
		} else if (currentFavoriteType.equals(FavoritesData.favWork)) {
			btnWork.setImageResource(R.drawable.favtypeworkbuttonpressed);
			textWork.setTextColor(getSelectedTextColor());
		} else if (currentFavoriteType.equals(FavoritesData.favSchool)) {
			btnSchool.setImageResource(R.drawable.favtypeschoolbuttonpressed);
			textSchool.setTextColor(getSelectedTextColor());
		} else {
			btnFavorite.setImageResource(R.drawable.favtypefavoritebuttonpressed);
			textFavorite.setTextColor(getSelectedTextColor());
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			Bundle b = data.getExtras();
			if (b.containsKey("address") && b.containsKey("lat") && b.containsKey("lon")) {
				favoritesData.setAdress(AddressParser.textFromBundle(b).replaceAll("\n", ""));
				favoritesData.setLatitude(b.getDouble("lat"));
				favoritesData.setLongitude(b.getDouble("lon"));
				String txt = favoritesData.getAdress();
				textAddress.setText(txt);
				if (b.containsKey("poi")) {
					favoritesData.setName(b.getString("poi"));
				}
			}
		}

	}

	private void popFragment() {
		
		getActivity().setResult(FavoritesListActivity.RESULT_OK);
		getActivity().finish();
		
/*		((Activity) getActivity()).getFragmentManager().popBackStack();
		((Activity) getActivity()).getFragmentManager().executePendingTransactions(); */
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
		if (dialog2 != null && dialog2.isShowing()) {
			dialog2.dismiss();
		}
		hideKeyboard();
	}

	protected int getSelectedTextColor() {
		return Color.WHITE;
	}

	protected int getUnSelectedTextColor() {
		return Color.LTGRAY;
	}

	private static boolean isPredefinedName(final String name) {
		if (name.equals(IbikeApplication.getString("Favorite")) || name.equals(IbikeApplication.getString("School"))
				|| name.equals(IbikeApplication.getString("Work")) || name.equals(IbikeApplication.getString("Home")) || name.equals(""))
			return true;
		else
			return false;
	}

	public void hideKeyboard() {
		if (textFavoriteName != null) {
			InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(textFavoriteName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	private AlertDialog dialog2;

	private void launchErrorDialog(final String msg) {
		if (getActivity() != null && getView() != null) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					getView().findViewById(R.id.progress).setVisibility(View.INVISIBLE);
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle("Error");
					builder.setMessage(msg);
					builder.setPositiveButton(IbikeApplication.getString("ok"), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

						}
					});
					dialog2 = builder.show();
				}
			});
		}

	}

	@Override
	public void onRequestCompleted(final boolean success) {
		if (getActivity() != null && getView() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getView().findViewById(R.id.progress).setVisibility(View.INVISIBLE);
					if (success) {
						popFragment();
					} else {
						Util.launchNoConnectionDialog(getActivity());
					}
				}
			});
		}
	}
}
