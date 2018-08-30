package dk.kk.ibikecphlib.favorites;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.LeftMenu;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.controls.SortableListView;
import dk.kk.ibikecphlib.navigation.routing_engine.SMLocationManager;
import dk.kk.ibikecphlib.util.DB;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import java.util.ArrayList;

import dk.kk.ibikecphlib.controls.SortableListView;
import dk.kk.ibikecphlib.util.Util;

public class FavoritesListActivity extends Activity {
    public static final int ADD_FAVORITE = 510;
    public static final int RESULT_ROUTE = 620;

    static SortableListView favoritesList;
    private static ListAdapter listAdapter;
    private FavoritesAdapter adapter;
    protected static ArrayList<FavoriteListItem> favorites = new ArrayList<FavoriteListItem>();
    private tFetchFavorites fetchFavorites;
    public ProgressBar progressBar;
    TextView textLogin;

    public boolean favoritesEnabled = true;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites_list);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textLogin = (TextView) findViewById(R.id.textLogin);
        textLogin.setText(IBikeApplication.getString("favorites_login"));

        try {
            this.getActionBar().setTitle(IBikeApplication.getString("favorites"));
        } catch (NullPointerException e) {
            // If we have no action bar, then whatever.
        }

        favoritesList = (SortableListView) findViewById(R.id.favoritesList);
        listAdapter = getAdapter();

        favoritesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                onListItemClick(position);
            }
        });

        // Bring up a menu on long press, to let the user delete.
        favoritesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FavoritesListActivity.this);
                String[] options = {IBikeApplication.getString("Delete")};
                builder.setTitle(IBikeApplication.getString("Delete"))
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!Util.isNetworkConnected(FavoritesListActivity.this)) {
                                    favoritesEnabled = true;
                                    Util.launchNoConnectionDialog(FavoritesListActivity.this);
                                } else {
                                    progressBar.setVisibility(View.VISIBLE);
                                    final FavoriteListItem fd = (FavoriteListItem) (favoritesList.getAdapter().getItem(i));
                                    new AsyncTask<String, Integer, String>() {
                                        @Override
                                        protected String doInBackground(String... strings) {
                                            try {
                                                new DB(FavoritesListActivity.this).deleteFavorite(fd);
                                            } catch (Exception ex) {
                                            }
                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(String result) {
                                            super.onPostExecute(result);
                                            reloadFavorites();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }.execute();
                                }

                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("DV", "Onresume Favorites!");

        if (IBikeApplication.isUserLogedIn() || IBikeApplication.isFacebookLogin()) {
            reloadFavorites();

            favoritesList.setVisibility(View.VISIBLE);
        } else {
            textLogin.setVisibility(View.VISIBLE);
        }
    }

    public void reloadFavorites() {
        if (IBikeApplication.isUserLogedIn()) {
            fetchFavorites = new tFetchFavorites();
            fetchFavorites.start();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show the Add button if the user is logged in
        if (IBikeApplication.isFacebookLogin() || IBikeApplication.isUserLogedIn()) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.favorites_list, menu);
            menu.getItem(0).setTitle(IBikeApplication.getString("new_favorite"));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.addButton) {
            Intent i = new Intent(this, AddFavoriteActivity.class);
            this.startActivityForResult(i, ADD_FAVORITE);
            this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected FavoritesAdapter getAdapter() {
        if (adapter == null) {
            adapter = new FavoritesAdapter(IBikeApplication.getContext(), favorites, null);
        }
        return adapter;
    }


    public void onListItemClick(int position) {
        if (!Util.isNetworkConnected(this)) {
            favoritesEnabled = true;
            Util.launchNoConnectionDialog(this);
        } else {
            FavoriteListItem fd = (FavoriteListItem) (favoritesList.getAdapter().getItem(position));

            // Start editing the fav
            Intent i = new Intent(this, EditFavoriteActivity.class);

            // Add the favorite to the intent so it can be passed on to the EditFavoriteFragment later on.
            i.putExtra("favoriteListItem", fd);

            this.startActivityForResult(i, ADD_FAVORITE);
        }
    }

    public void showRouteNotFoundDlg() {
        if (dialog == null) {
            String message = IBikeApplication.getString("error_route_not_found");
            if (!SMLocationManager.getInstance().hasValidLocation())
                message = IBikeApplication.getString("error_no_gps");
            Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message).setPositiveButton(IBikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
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

    public void onPause() {
        super.onPause();
        if (fetchFavorites != null && fetchFavorites.isAlive()) {
            fetchFavorites.interrupt();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_ROUTE) {
            Log.d("JC", "FavoritesList, start route");

            Intent returnIntent = new Intent();

            // Return some information as to where to route, so the MapActivity knows and can handle it.
            returnIntent.putExtra("ROUTE_TO", data.getParcelableExtra("favoriteListItem"));
            setResult(RESULT_OK, returnIntent);
            finishActivity(LeftMenu.LAUNCH_FAVORITE);
            finish();

        } else {
            if (requestCode == ADD_FAVORITE) {
                //reloadFavorites();
            }
        }
    }

    public static class fetchFavoritesAfterEdit {
        static ArrayList<FavoriteListItem> favs = null;

        public static void updateFavorites() {
            LOG.d("fetching the favorites from AfterEdit");

            new AsyncTask<String, Integer, String>() {

                @Override
                protected String doInBackground(String... strings) {
                    favs = (new DB(IBikeApplication.getContext())).getFavoritesFromServer(null);
                    return null;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    Log.d("DV", "Got some favorites from AfterEdit");
                    favorites.clear();
                    favorites.addAll(favs);
                    ((FavoritesAdapter) listAdapter).notifyDataSetChanged();

                    favoritesList.setAdapter(listAdapter);
                    Log.d("DV", "FavoriteAdapter updated from AfterEdit!");
                }
            }.execute();

        }

    }

    private class tFetchFavorites extends Thread {

        @Override
        public void run() {
            Looper.myLooper();
            Looper.prepare();
            showProgressDialog();
            Log.d("JC", "FavFetcher started");
            while (!interrupted()) {
                LOG.d("fetching the favorites");
                final ArrayList<FavoriteListItem> favs = (new DB(IBikeApplication.getContext())).getFavoritesFromServer(null);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d("JC", "Got some favorites");
                        favorites.clear();
                        favorites.addAll(favs);
                        ((FavoritesAdapter) listAdapter).notifyDataSetChanged();

                        favoritesList.setAdapter(listAdapter);
                        Log.d("DV", "FavoriteAdapter opdateret!");
                        dismissProgressDialog();
                    }
                });

                if (Util.isNetworkConnected(IBikeApplication.getContext())) {
                    // favorites have been fetched
                    break;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            dismissProgressDialog();
        }
    }

    public void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }


}
