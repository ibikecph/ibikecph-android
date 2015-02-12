package com.spoiledmilk.ibikecph.favorites;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.SortableListView;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class FavoritesListActivity extends Activity {
	SortableListView favoritesList;
	private ListAdapter listAdapter;
	private FavoritesAdapter adapter;
	protected ArrayList<FavoritesData> favorites = new ArrayList<FavoritesData>();
	private tFetchFavorites fetchFavorites;

	public boolean favoritesEnabled = true;
	private AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorites_list);

		favoritesList = (SortableListView) findViewById(R.id.favoritesList);		
		listAdapter = getAdapter();

		favoritesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Log.d("JC", "Clicked on some favorite");

				if (!((FavoritesAdapter) favoritesList.getAdapter()).isEditMode) {
					if (favoritesEnabled) {
						Log.d("JC", "Calling click handler");
						favoritesEnabled = false;
						onListItemClick(position);
					}
				}
			}

		});

		if (IbikeApplication.isUserLogedIn()) {
			fetchFavorites = new tFetchFavorites();
			fetchFavorites.start();
		} 

	}

	public void onResume(View v) {
		if (IbikeApplication.isUserLogedIn()) {
			fetchFavorites = new tFetchFavorites();
			fetchFavorites.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.favorites_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected FavoritesAdapter getAdapter() {
		if (adapter == null) {
			adapter = new FavoritesAdapter(IbikeApplication.getContext(), favorites, null);
		}
		return adapter;
	}


	public void onListItemClick(int position) {
		if (!Util.isNetworkConnected(this)) {
			favoritesEnabled = true;
			Util.launchNoConnectionDialog(this);
		} else {
			FavoritesData fd = (FavoritesData) (favoritesList.getAdapter().getItem(position));

			Log.d("JC", "Clicked favorite: " + fd.getName());
			Log.d("JC", "Coords: " + fd.getLatitude() + " , "+ fd.getLongitude());

			if (SMLocationManager.getInstance().hasValidLocation()) {
				Intent returnIntent = new Intent();

				// Return some information as to where to route, so the MapActivity knows and can handle it.
				
				
				
				returnIntent.putExtra("ROUTE_TO", fd);
				setResult(RESULT_OK, returnIntent);
				finishActivity(LeftMenu.LAUNCH_FAVORITE);
				finish();
				Log.d("JC", "Should have finished activity");

			} else {
				favoritesEnabled = true;
				showRouteNotFoundDlg();
			}
		}
	}

	public void showRouteNotFoundDlg() {
		if (dialog == null) {
			String message = IbikeApplication.getString("error_route_not_found");
			if (!SMLocationManager.getInstance().hasValidLocation())
				message = IbikeApplication.getString("error_no_gps");
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(message).setPositiveButton(IbikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog = builder.create();
			dialog.setCancelable(false);
		}
		if (!dialog.isShowing()) {
			dialog.show();
		}
	}

	private class tFetchFavorites extends Thread {
		@Override
		public void run() {
			Log.d("JC", "FavFetcher started");
			while (!interrupted()) {
				LOG.d("fetching the favorites");
				final ArrayList<FavoritesData> favs = (new DB(IbikeApplication.getContext())).getFavoritesFromServer(IbikeApplication.getContext(), null);
				runOnUiThread(new Runnable() {
					public void run() {
						Log.d("JC", "Got some favorites");
						favorites.clear();
						favorites.addAll(favs);
						((FavoritesAdapter) listAdapter).notifyDataSetChanged();

						favoritesList.setAdapter(listAdapter);
					}
				});

				if (Util.isNetworkConnected(IbikeApplication.getContext())) {
					// favorites have been fetched
					break;
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
			}

		}
	}

}
