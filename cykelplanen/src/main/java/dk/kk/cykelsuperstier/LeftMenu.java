// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package dk.kk.cykelsuperstier;

import android.content.Intent;
import android.util.Log;

import dk.kk.cykelsuperstier.R;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.LeftMenuItem;
import dk.kk.ibikecphlib.persist.Track;
import dk.kk.ibikecphlib.tracking.TrackingActivity;
import dk.kk.ibikecphlib.tracking.TrackingWelcomeActivity;
import dk.kk.ibikecphlib.util.IBikePreferences;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class LeftMenu extends dk.kk.ibikecphlib.LeftMenu {

    @Override
    public void populateMenu() {
        super.populateMenu();
        // Find the ID of the "voice" menu item. Append the overlays after that.
        for (LeftMenuItem item: menuItems) {
            if (item.getLabelID().equals("favorites")) {
                Log.d("JC", "Adding overlays menu");
                //this.cpMenuItems.add(i+1, new LeftMenuItem("map_overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
            }
            if (item.getLabelID().equals("about_app_ibc")) {
                item.setLabelID("about_app_cp");
            }
        }
    }

    /*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View ret = super.onCreateView(inflater, container, savedInstanceState);

        Log.d("JC", "CP LeftMenu onCreateView");
        this.menuList.setAdapter(new LeftMenuItemAdapter(IBikeApplication.getContext(), cpMenuItems));
        super.menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String handler = cpMenuItems.get(position).getHandler();
                spawnFunction(handler);
            }
        });

		return ret;
	}
	*/

	@Override
	public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

    public void spawnTrackingActivity() {
        Intent i;
        IBikePreferences settings = IBikeApplication.getSettings();
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Track> query = realm.where(Track.class);
        RealmResults<Track> results = query.findAll();
        if (!settings.getTrackingEnabled() &&
                results.size() == 0) {
            i = new Intent(getActivity(), TrackingWelcomeActivity.class);
        } else {
            i = new Intent(getActivity(), TrackingActivity.class);
        }
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

}
