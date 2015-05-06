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
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.spoiledmilk.ibikecph.favorites.AddFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.favorites.FavoritesListActivity;
import com.spoiledmilk.ibikecph.login.FacebookProfileActivity;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingWelcomeActivity;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import io.realm.Realm;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * A menu that can be spawned in the MapActivity. It allows the user to access
 * the preferences, go to their favorites, and spawn the AboutActivity.
 * @author jens
 *
 */
public class LeftMenu extends Fragment implements iLanguageListener {
	public static final int LAUNCH_LOGIN = 501;
	public static final int LAUNCH_FAVORITE = 502;
	
    protected static final int menuItemHeight = Util.dp2px(40);
    protected static final int dividerHeight = Util.dp2px(2);

    protected int favoritesContainerHeight;
    TextView textFavorites,  textNewFavorite, textFavoriteHint;
    //SortableListView favoritesList;
    protected ArrayList<FavoritesData> favorites = new ArrayList<FavoritesData>();
    ImageView imgAdd;
    ImageButton btnEditFavorites;
    LinearLayout addContainer;
    protected RelativeLayout favoritesHeaderContainer, favoritesContainer, profileContainer, aboutContainer, settingsContainer;
    Button btnDone;
    View lastListDivider;
    private boolean isEditMode = false;
    public boolean favoritesEnabled = true;
    private ListAdapter listAdapter;
    protected ListView menuList;
    
    public ArrayList<LeftMenuItem> menuItems;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
    	LeftMenu self = this;
    	
        LOG.d("Left menu on createView");
        final View ret = inflater.inflate(R.layout.fragment_left_menu, container, false);
        
        // Initialize the menu
        this.menuList = (ListView) ret.findViewById(R.id.menuListView);
        populateMenu();
        
		// This is kind of magical. Each LeftMenuItem has a handler field that describes
		// a method to be called when that element is tapped. Here we read out that field
		// and launch the right method by reflection.
        this.menuList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String handler = menuItems.get(position).getHandler();
				spawnFunction(handler);
			}
		});
        
        // TODO: Get rid of all these handlers...
        /*
        btnEditFavorites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritesList.disableScroll();
                FavoritesAdapter adapter = (FavoritesAdapter) favoritesList.getAdapter();
                if (adapter != null)
                    adapter.setIsEditMode(isEditMode = true);
                btnDone.setVisibility(View.VISIBLE);
                btnEditFavorites.setVisibility(View.INVISIBLE);
                btnDone.setEnabled(true);
                btnEditFavorites.setEnabled(false);
                addContainer.setVisibility(View.GONE);
                updateControls();
            }
        });
        btnDone = (Button) ret.findViewById(R.id.btnDone);
        btnDone.setEnabled(false);
        btnDone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                favoritesList.enableScroll();
                FavoritesAdapter adapter = (FavoritesAdapter) favoritesList.getAdapter();
                if (adapter != null)
                    adapter.setIsEditMode(isEditMode = false);
                btnDone.setVisibility(View.INVISIBLE);
                btnDone.setEnabled(false);
                btnEditFavorites.setVisibility(View.VISIBLE);
                btnEditFavorites.setEnabled(true);
                addContainer.setVisibility(View.VISIBLE);
                updateControls();
            }

        });
        
        ret.findViewById(R.id.favoritesContainer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (IbikeApplication.isUserLogedIn())
                    openNewFavoriteFragment();
            }
        });
        addContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (IbikeApplication.isUserLogedIn())
                    openNewFavoriteFragment();
            }
        });
        */
        return ret;
    }
    
    /**
     * This method adds elements to the menu. It is called onCreate, but also when the menu needs
     * to be updated, e.g. when the user has logged in and the "Log in" button needs to change to
     * "account".
     */
    public void populateMenu()  {
    	this.menuItems = new ArrayList<LeftMenuItem>();
        
        menuItems.add(new LeftMenuItem("favorites", R.drawable.ic_menu_favorite, "spawnFavoritesListActivity"));
        menuItems.add(new LeftMenuItem("voice", R.drawable.ic_menu_voice_guide, "spawnTTSSettingsActivity"));
        // Kortlag
        // Rutetype
        // Fartguide
        menuItems.add(new LeftMenuItem("tracking", R.drawable.ic_menu_tracking, "spawnTrackingActivity"));
        // Påmindelser

        if (IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin()) {
            menuItems.add(new LeftMenuItem("account", R.drawable.ic_menu_profile, "spawnLoginActivity"));
        } else {
            menuItems.add(new LeftMenuItem("log_in", R.drawable.ic_menu_profile, "spawnLoginActivity"));
        }


        menuItems.add(new LeftMenuItem("about_app_ibc", R.drawable.ic_menu_info, "spawnAboutActivity"));

        this.menuList.setAdapter(new LeftMenuItemAdapter(IbikeApplication.getContext(), menuItems));
    }
    
    /**
     * This method is called as a click handler on all items in the menu. It is called with the 
     * name parameter corresponding to the method that the particular FavoritesListActivity denotes,
     * for example spawnAboutActivity for the About button. 
     * @param name Name of the method to be called. 
     */
    public void spawnFunction(String name) {
		Method handlerMethod;
		try {
			handlerMethod = this.getClass().getDeclaredMethod(name, null);
			handlerMethod.invoke(this);
		} catch (Exception e) {
			Log.e("JC", "Handler " + name + " not found");
			e.printStackTrace();
		}
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnLoginActivity() {
    	if (!Util.isNetworkConnected(getActivity())) {
            Util.launchNoConnectionDialog(getActivity());
        } else {
            Intent i;
            
            // If the user is not logged in, show her a login screen, otherwise show the relevant profile activity. 
            if ( !IbikeApplication.isUserLogedIn() && !IbikeApplication.isFacebookLogin()) {
                i = new Intent(getActivity(), LoginActivity.class);
            } else {
                if (IbikeApplication.isFacebookLogin())
                    i = new Intent(getActivity(), FacebookProfileActivity.class);
                else
                    i = new Intent(getActivity(), ProfileActivity.class);
            }
            getActivity().startActivityForResult(i, LAUNCH_LOGIN);
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnTrackingActivity() {
        Intent i;
        IbikePreferences settings = IbikeApplication.getSettings();
        if (!settings.getTrackingEnabled() &&
            Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class).size() == 0) {
            i = new Intent(getActivity(), TrackingWelcomeActivity.class);
        } else {
            i = new Intent(getActivity(), TrackingActivity.class);
        }
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

    @SuppressWarnings("UnusedDeclaration")
    public void spawnFavoritesListActivity() {
        Intent i = new Intent(getActivity(), FavoritesListActivity.class);
        getActivity().startActivityForResult(i, LAUNCH_FAVORITE);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

    @SuppressWarnings("UnusedDeclaration")
    public void spawnTTSSettingsActivity() {
        Intent i = new Intent(getActivity(), TTSSettingsActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

    @Override
    public void onResume() {
        super.onResume();
        LOG.d("Left menu onResume");
        initStrings();
        
        updateControls();
    }

    // TODO: Get rid of this
    public void reloadFavorites() {
        DB db = new DB(getActivity());
        favorites = db.getFavorites(favorites);
        LOG.d("update favorites from reloadFavorites() count = " + favorites.size());
        updateControls();
    }

    @Override
    public void reloadStrings() {
    	this.reloadFavorites();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    public void initStrings() {
    }

    protected AddFavoriteFragment getAddFavoriteFragment() {
        return new AddFavoriteFragment();
    }

    @SuppressWarnings("deprecation")
    public void updateControls() {
        this.populateMenu();
    }

}
