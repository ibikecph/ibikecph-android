package com.spoiledmilk.ibikecph.favorites;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;


public class EditFavoriteActivity extends Activity {

    public interface FavoriteCallback {
        public void onSuccess();

        public void onFailure();
    }

    EditFavoriteFragment editFavoriteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favorite);

        this.editFavoriteFragment = new EditFavoriteFragment();

        // Take the data we got from our intent (containing the favorites data) and
        // stick it in the Fragment.
        this.editFavoriteFragment.setArguments(getIntent().getExtras());

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, editFavoriteFragment).commit();
        }

        // Set the ActionBar
        try {
            this.getActionBar().setTitle(IbikeApplication.getString("favorites"));
        } catch (NullPointerException e) {
            // There was no ActionBar. Oh well...
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_favorite, menu);

        menu.getItem(0).setTitle(IbikeApplication.getString("start_route"));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_start_route) {

            // The user wants to route to the address in the current Fav. Finish the activity and set up a route in the
            // MapActivity.

            Log.d("JC", "Starting Route");
            // Putting the original intent into the result, passing on the address to the parent
            setResult(FavoritesListActivity.RESULT_ROUTE, getIntent());
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Save the favorite first.
        this.editFavoriteFragment.saveEditedFavorite();
        super.onBackPressed();
    }

    // We're delegating the result from the address dialog directly to the fragment
    // in order for it to take care of things.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("JC", "EditFavoriteActivity on address result");
        try {
            editFavoriteFragment.onActivityResult(requestCode, resultCode, data);
        } catch (Exception ex) {
        }
    }

}
