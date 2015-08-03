// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.AddressParser;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A Fragment used inside the LeftMenu for adding a favorite address.
 * @author jens
 *
 */
public class AddFavoriteFragment extends Fragment {

	protected EditText textAddress;
	protected EditText textFavoriteName;
	private ImageButton btnFavorite;
	private ImageButton btnHome;
	private ImageButton btnWork;
	private ImageButton btnSchool;
	private TextView textFavorite;
	private TextView textHome;
	private TextView textWork;
	private TextView textSchool;
	private Button btnSave;
	private FavoritesData favoritesData = null;
	private String currentFavoriteType = "";
	private AlertDialog dialog;
	boolean isTextChanged = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View ret = inflater.inflate(R.layout.fragment_add_favorite, container, false);

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
		textFavoriteName.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !isTextChanged) {

					isTextChanged = true;
					textFavoriteName.setText("");
				}
			}
		});

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

		btnSave = (Button) ret.findViewById(R.id.btnSave);
		btnSave.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (Util.isNetworkConnected(getActivity())) {
					if (favoritesData != null && textFavoriteName.getText().toString() != null
							&& !textFavoriteName.getText().toString().trim().equals("")) {
						if (new DB(getActivity()).favoritesForName(textFavoriteName.getText().toString().trim()) == 0) {
							favoritesData.setName(textFavoriteName.getText().toString());
							favoritesData.setSubSource(currentFavoriteType);
							String st = favoritesData.getName() + " - (" + favoritesData.getLatitude() + "," + favoritesData.getLongitude()
									+ ")";
							IbikeApplication.getTracker().sendEvent("Favorites", "Save", st, (long) 0);

							Thread saveThread = new Thread(new Runnable() {
								@Override
								public void run() {
									(new DB(getActivity())).saveFavorite(favoritesData, getActivity(), false);
								}
							});
							saveThread.start();
							try {
								saveThread.join();
							} catch (Exception e) {
								e.getLocalizedMessage();
							}
							
							getActivity().setResult(FavoritesListActivity.RESULT_OK);
							getActivity().finish();
							
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

		textFavoriteName.setText(IbikeApplication.getString("Favorite"));

		return ret;
	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		textAddress.setHint(IbikeApplication.getString("add_favorite_address_placeholder"));
		textFavorite.setText(IbikeApplication.getString("Favorite"));
		textHome.setText(IbikeApplication.getString("Home"));
		textWork.setText(IbikeApplication.getString("Work"));
		textSchool.setText(IbikeApplication.getString("School"));
		btnSave.setText(IbikeApplication.getString("save_favorite"));
		if (currentFavoriteType.equals(""))
			currentFavoriteType = IbikeApplication.getString("Favorite");
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.i("JC", "AddFavoriteFragment on address result");
		
		if (data != null) {
			Bundle b = data.getExtras();
			if (b.containsKey("address") && b.containsKey("lat") && b.containsKey("lon")) {
				String address = AddressParser.textFromBundle(b).replace("\n", "");
				favoritesData = new FavoritesData(textFavoriteName.getText().toString(), address, currentFavoriteType, b.getDouble("lat"),
						b.getDouble("lon"), -1);
				textAddress.setText(address);
				if (b.containsKey("poi")) {
					textFavoriteName.setText(b.getString("poi"));
				}
			}

		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		hideKeyboard();
	}

	protected int getSelectedTextColor() {
		return Color.BLACK;
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
}
