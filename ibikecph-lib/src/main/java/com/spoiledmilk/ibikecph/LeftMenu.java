// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.spoiledmilk.ibikecph.favorites.AddFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.favorites.FavoritesListActivity;
import com.spoiledmilk.ibikecph.login.FacebookProfileActivity;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.map.OverlaysActivity;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingWelcomeActivity;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import io.realm.Realm;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * A menu that can be spawned in the MapActivity. It allows the user to access
 * the preferences, go to their favorites, and spawn the AboutActivity.
 *
 * @author jens
 */

public class LeftMenu extends Fragment {
    public static final int LAUNCH_LOGIN = 501;
    public static final int LAUNCH_FAVORITE = 502;
    public static final int LAUNCH_TRACKING = 503;
    public static final int LAUNCH_ABOUT = 504;

    protected static final int dividerHeight = Util.dp2px(2);

    //SortableListView favoritesList;
    protected ArrayList<FavoritesData> favorites = new ArrayList<FavoritesData>();
    protected RelativeLayout favoritesHeaderContainer, favoritesContainer;
    protected ListView menuList;

    protected ArrayList<LeftMenuItem> menuItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populateMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        LOG.d("Left menu on createView");
        final View ret = inflater.inflate(R.layout.fragment_left_menu, container, false);

        // Initialize the menu
        this.menuList = (ListView) ret.findViewById(R.id.menuListView);
        this.menuList.setAdapter(new LeftMenuItemAdapter(IbikeApplication.getContext(), menuItems));

        // This is kind of magical. Each LeftMenuItem has a handler field that describes
        // a method to be called when that element is tapped. Here we read out that field
        // and launch the right method by reflection.
        this.menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String handler = getMenuItems().get(position).getHandler();
                spawnFunction(handler);
            }
        });
        return ret;
    }

    public ArrayList<LeftMenuItem> getMenuItems() {
        return this.menuItems;
    }

    /**
     * This method adds elements to the menu. It is called onCreate, but also when the menu needs
     * to be updated, e.g. when the user has logged in and the "Log in" button needs to change to
     * "account".
     */
    public void populateMenu() {
        this.menuItems = new ArrayList<LeftMenuItem>();

        menuItems.add(new LeftMenuItem("favorites", R.drawable.fav_star, "spawnFavoritesListActivity"));
        //menuItems.add(new LeftMenuItem("voice", R.drawable.ic_menu_voice_guide, "spawnTTSSettingsActivity"));
        // Kortlag
        if (IbikeApplication.getAppName().equals("CykelPlanen")) {
            menuItems.add(new LeftMenuItem("map_overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
        }
        // Rutetype
        // Tracking
        if (getResources().getBoolean(R.bool.trackingEnabled)) {
            menuItems.add(new LeftMenuItem("tracking",
                                           R.drawable.ic_menu_tracking,
                                           "spawnTrackingActivity"));
        }
        // Påmindelser

        if (IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin()) {
            menuItems.add(new LeftMenuItem("account", R.drawable.ic_menu_profile, "spawnLoginActivity"));
        } else {
            menuItems.add(new LeftMenuItem("log_in", R.drawable.ic_menu_profile, "spawnLoginActivity"));
        }

        menuItems.add(new LeftMenuItem("about_app_ibc", R.drawable.ic_menu_info, "spawnAboutActivity"));

        // updating the view
        if (this.menuList != null && this.menuList.getAdapter() != null) {
            Log.d("JC", "updating leftmenu");
            this.menuList.setAdapter(new LeftMenuItemAdapter(IbikeApplication.getContext(), menuItems));
        }
    }

    /**
     * This method is called as a click handler on all items in the menu. It is called with the
     * name parameter corresponding to the method that the particular FavoritesListActivity denotes,
     * for example spawnAboutActivity for the About button.
     *
     * @param name Name of the method to be called.
     */
    public void spawnFunction(String name) {
        Method handlerMethod;
        try {
            handlerMethod = this.getClass().getMethod(name, (Class[]) null);
            handlerMethod.invoke(this);
        } catch (Exception e) {
            Log.e("JC", "Handler " + name + " not found");
            e.printStackTrace();
        }
    }


    @SuppressWarnings("UnusedDeclaration")
    public void spawnOverlaysActivity() {
        Intent i = new Intent(getActivity(), OverlaysActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(com.spoiledmilk.ibikecph.R.anim.slide_in_right, com.spoiledmilk.ibikecph.R.anim.slide_out_left);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnLoginActivity() {
        if (!Util.isNetworkConnected(getActivity())) {
            Util.launchNoConnectionDialog(getActivity());
        } else {
            Intent i;

            // If the user is not logged in, show her a login screen, otherwise show the relevant profile activity.
            if (!IbikeApplication.isUserLogedIn() && !IbikeApplication.isFacebookLogin()) {
                i = new Intent(getActivity(), LoginActivity.class);
            } else {
                if (IbikeApplication.isFacebookLogin())
                    i = new Intent(getActivity(), FacebookProfileActivity.class);
                else
                    i = new Intent(getActivity(), ProfileActivity.class);
            }
            getActivity().startActivityForResult(i, LAUNCH_LOGIN);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnTrackingActivity() {
        Intent i;
        IbikePreferences settings = IbikeApplication.getSettings();
        if (!settings.getTrackingEnabled() &&
                Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class).size() == 0) {

            i = new Intent(getActivity(), TrackingWelcomeActivity.class);

            // We don't want this pushed to the back stack.
            //i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        } else {
            i = new Intent(getActivity(), TrackingActivity.class);
        }
        getActivity().startActivityForResult(i, LAUNCH_TRACKING);

    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), AboutActivity.class);
        getActivity().startActivityForResult(i, LAUNCH_ABOUT);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnFavoritesListActivity() {
        Intent i = new Intent(getActivity(), FavoritesListActivity.class);
        getActivity().startActivityForResult(i, LAUNCH_FAVORITE);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnTTSSettingsActivity() {
        Intent i = new Intent(getActivity(), TTSSettingsActivity.class);
        getActivity().startActivity(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.d("Left menu onResume");

        populateMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    protected AddFavoriteFragment getAddFavoriteFragment() {
        return new AddFavoriteFragment();
    }

}
