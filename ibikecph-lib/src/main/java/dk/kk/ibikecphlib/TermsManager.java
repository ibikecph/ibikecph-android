package dk.kk.ibikecphlib;

import android.app.Activity;
import android.content.Intent;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import dk.kk.ibikecphlib.util.Config;
import org.json.JSONException;
import dk.kk.ibikecphlib.util.LOG;

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

        LOG.d("checking for new terms");
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Accept", "application/vnd.ibikecph.v1");
        client.get(Config.TRACKING_TERMS_JSON_URL, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, org.json.JSONObject response) {

                try {
                    int version = response.getInt("version");
                    String importantNews = response.getString("important_parts_description_" + IBikeApplication.getLanguageString());

                    // If the most recent terms we read turns out to be older than the current one, then spawn a dialog.
                    if (IBikeApplication.getSettings().getNewestTermsAccepted() < version) {
                        LOG.d("god newer terms, ask user to accept them");
                        Intent i = new Intent(hostActivity, IBikeApplication.getTermsAcceptanceClass());

                        i.putExtra("important_news", importantNews);
                        i.putExtra("version", version);

                        hostActivity.startActivity(i);
                    } else {
                        LOG.d("terms are up-to-date");
                    }


                } catch(JSONException e) {

                }
            }
        });

    }
}
