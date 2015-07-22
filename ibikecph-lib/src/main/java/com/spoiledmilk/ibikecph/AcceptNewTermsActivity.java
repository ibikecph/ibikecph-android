package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.spoiledmilk.ibikecph.util.IbikePreferences;


public class AcceptNewTermsActivity extends Activity {

    private int version;
    private String importantNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_new_terms);

        this.getActionBar().setTitle(IbikeApplication.getString("user_terms_title"));

        version = this.getIntent().getIntExtra("version", 0);
        importantNews = this.getIntent().getStringExtra("important_news");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_accept_new_terms, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(IbikeApplication.getString("user_terms_title"));
        builder.setMessage(IbikeApplication.getString("accept_user_terms_or_log_out"));

        builder.setPositiveButton(IbikeApplication.getString("logout"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                IbikeApplication.logout();
                finish();
            }
        });

        builder.setNegativeButton(IbikeApplication.getString("back"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismiss the dialog.
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
     }

    public void onLogoutButtonClick(View v) {
        IbikeApplication.logout();

        finish();
    }

    public  void onAcceptButtonClick(View v) {
        IbikePreferences prefs = IbikeApplication.getSettings();

        prefs.setNewestTermsAccepted(this.version);

        finish();
    }
}
