// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.AddressParser;
import com.spoiledmilk.ibikecph.util.*;
import org.json.JSONObject;

/**
 * A Fragment used inside the LeftMenu for editing a favorite.
 * TODO: Can this be merged with AddFavoriteFragment?
 * @author jens
 *
 */
public class EditFavoriteFragment extends AddFavoriteFragment implements APIListener {

	protected EditText textAddress;
	protected EditText textFavoriteName;

	private FavoritesData favoritesData = null;
	private AlertDialog dialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = super.onCreateView(inflater, container, savedInstanceState);

		if (getArguments() != null) {
			favoritesData = getArguments().getParcelable("favoritesData");
		}

        this.textFavoriteName = (EditText) ret.findViewById(R.id.textFavoriteName);
        this.textAddress = (EditText) ret.findViewById(R.id.textAddress);

        this.textAddress.setText(favoritesData.getAdress());
        this.textFavoriteName.setText(favoritesData.getName());

        return ret;
	}

	@Override
	public void onResume() {
        if (getArguments() != null) {
            favoritesData = getArguments().getParcelable("favoritesData");
        }

        super.onResume();
		initStrings();
	}

	private void initStrings() {
        //textFavorite.setText(IbikeApplication.getString("Favorite"));

		/*
        textAddress.setText(favoritesData.getAdress());
		textFavoriteName.setText(favoritesData.getName());

		currentFavoriteType = favoritesData.getSubSource();
		*/
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
					if (success) {
						popFragment();
					} else {
						Util.launchNoConnectionDialog(getActivity());
					}
				}
			});
		}
	}

    public void saveEditedFavorite() {
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

                    updateThread.start();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    public void deleteFavorite() {
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
}
