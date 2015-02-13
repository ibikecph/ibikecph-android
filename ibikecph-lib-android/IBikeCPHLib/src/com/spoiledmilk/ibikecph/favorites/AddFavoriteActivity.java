package com.spoiledmilk.ibikecph.favorites;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;

public class AddFavoriteActivity extends Activity {
	private ImageButton btnBack;
	private TextView textTitle;
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
	private TexturedButton btnSave;
	private FavoritesData favoritesData = null;
	private String currentFavoriteType = "";
	private AlertDialog dialog;
	boolean isTextChanged = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_favorite);
		
		Fragment addFavoriteFragment = new AddFavoriteFragment();
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, addFavoriteFragment).commit();
		}
		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_favorite, menu);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_add_favorite,
					container, false);
			return rootView;
		}
	}
}
