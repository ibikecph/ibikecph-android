package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.content.Intent;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.spoiledmilk.ibikecph.util.Config;
import org.json.JSONException;

/**
 * Created by jens on 7/22/15.
 */
public class TermsManager {

    /**
     * Checks the version of the user's accepted Terms of Service.
     * @param hostActivity
     */
    public static void checkTerms(final Activity hostActivity) {

        // If folks are not logged in, then we don't care.
        if (!(IBikeApplication.isUserLogedIn() || IBikeApplication.isFacebookLogin())) {
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Accept", "application/vnd.ibikecph.v1");
        client.get(Config.TRACKING_TERMS_JSON_URL, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, org.apache.http.Header[] headers, org.json.JSONObject response) {

                try {
                    int version = response.getInt("version");
                    String importantNews = response.getString("important_parts_description_"+ IBikeApplication.getLanguageString());

                    // If the most recent terms we read turns out to be older than the current one, then spawn a dialog.
                    if (IBikeApplication.getSettings().getNewestTermsAccepted() < version) {
                        Intent i = new Intent(hostActivity, IBikeApplication.getTermsAcceptanceClass());

                        i.putExtra("important_news", importantNews);
                        i.putExtra("version", version);

                        hostActivity.startActivity(i);
                    }


                } catch(JSONException e) {

                }
            }
        });

    }
}
