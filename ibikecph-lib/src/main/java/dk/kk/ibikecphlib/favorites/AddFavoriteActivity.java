package dk.kk.ibikecphlib.favorites;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;

public class AddFavoriteActivity extends Activity {

	AddFavoriteFragment addFavoriteFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_favorite);
		
		addFavoriteFragment = new AddFavoriteFragment();
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, addFavoriteFragment).commit();
		}

        // Set the ActionBar
        try {
            this.getActionBar().setTitle(IBikeApplication.getString("favorites"));
        } catch(NullPointerException e) {
            // There was no ActionBar. Oh well...
        }
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.add_favorite, menu);
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
	

	// We're delegating the result from the address dialog directly to the fragment 
	// in order for it to take care of things.
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("JC", "AddFavoriteActivity on address result");
		addFavoriteFragment.onActivityResult(requestCode, resultCode, data);
	}

    @Override
    public void onBackPressed() {
        // Save the favorite first.
        this.addFavoriteFragment.saveFavorite();
        super.onBackPressed();
    }
}
