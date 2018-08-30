// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.favorites;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;

import com.mapbox.mapboxsdk.geometry.LatLng;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.search.AddressParser;
import dk.kk.ibikecphlib.util.*;

import dk.kk.ibikecphlib.search.AddressParser;
import dk.kk.ibikecphlib.util.APIListener;
import dk.kk.ibikecphlib.util.Util;

/**
 * A Fragment used inside the LeftMenu for editing a favorite.
 *
 * @author jens
 */
public class EditFavoriteFragment extends AddFavoriteFragment implements APIListener {

    protected EditText textAddress;
    protected EditText textFavoriteName;

    private FavoriteListItem favoriteListItem = null;
    private AlertDialog dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = super.onCreateView(inflater, container, savedInstanceState);

        if (getArguments() != null) {
            favoriteListItem = getArguments().getParcelable("favoriteListItem");
        } else {
            throw new RuntimeException("Expected a parcelable argument named favoriteListItem");
        }

        this.textFavoriteName = (EditText) ret.findViewById(R.id.textFavoriteName);
        this.textAddress = (EditText) ret.findViewById(R.id.textAddress);

        this.textAddress.setText(favoriteListItem.getAddress().getFullAddress());
        this.textFavoriteName.setText(favoriteListItem.getAddress().getName());

        String type = favoriteListItem.getSubSource();
        this.currentFavoriteType = type;

        if (type.equals(favoriteListItem.favFav)) {
            ((RadioButton) ret.findViewById(R.id.radioButtonFavorite)).setChecked(true);
        } else if (type.equals(favoriteListItem.favHome)) {
            ((RadioButton) ret.findViewById(R.id.radioButtonHome)).setChecked(true);
        } else if (type.equals(favoriteListItem.favSchool)) {
            ((RadioButton) ret.findViewById(R.id.radioButtonSchool)).setChecked(true);
        } else if (type.equals(favoriteListItem.favWork)) {
            ((RadioButton) ret.findViewById(R.id.radioButtonWork)).setChecked(true);
        }

        return ret;
    }

    @Override
    public void onResume() {
        if (getArguments() != null) {
            favoriteListItem = getArguments().getParcelable("favoriteListItem");
        }

        super.onResume();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Bundle b = data.getExtras();

            if (b.containsKey("address") && b.containsKey("lat") && b.containsKey("lon")) {
                favoriteListItem.setFullAddress(AddressParser.textFromBundle(b).replaceAll("\n", ""));
                double latitude = b.getDouble("lat");
                double longitude = b.getDouble("lon");
                favoriteListItem.getAddress().setLocation(new LatLng(latitude, longitude));
                String txt = favoriteListItem.getAddress().getFullAddress();
                textAddress.setText(txt);

                if (b.containsKey("poi")) {
                    favoriteListItem.getAddress().setName(b.getString("poi"));
                }

                saveEditedFavorite();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        hideKeyboard();
    }

    public void hideKeyboard() {
        if (textFavoriteName != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(textFavoriteName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onRequestCompleted(final boolean success) {
        if (getActivity() != null && getView() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (success) {
                        //popFragment();
                    } else {
                        Util.launchNoConnectionDialog(getActivity());
                    }
                }
            });
        }
    }

    public void saveEditedFavorite() {
        if (Util.isNetworkConnected(getActivity())) {
            String nameString = textFavoriteName.getText().toString().trim();
            String addressString = textAddress.getText().toString().trim();

            if (favoriteListItem != null && !nameString.isEmpty()) {
                int existingFavoritesWithName = new DB(getActivity()).favoritesForName(nameString);
                boolean nameUnchanged = favoriteListItem.getAddress().getName().trim().equalsIgnoreCase(textFavoriteName.getText().toString());
                if (existingFavoritesWithName < 1 || nameUnchanged) {
                    favoriteListItem.setFullAddress(addressString);
                    favoriteListItem.getAddress().setName(nameString);
                    favoriteListItem.setSubSource(currentFavoriteType);
                    Thread updateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            (new DB(getActivity())).updateFavorite(favoriteListItem, EditFavoriteFragment.this);
                        }
                    });
                    updateThread.start();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(IBikeApplication.getString("name_used"));
                    builder.setTitle(IBikeApplication.getString("Error"));
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
                Util.showSimpleMessageDlg(getActivity(), IBikeApplication.getString("register_error_fields"));
            }
        } else {
            Util.launchNoConnectionDialog(getActivity());
        }
    }

}
