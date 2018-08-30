package dk.kk.cykelsuperstier.map;

import android.graphics.Color;
import android.util.Log;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jens on 7/20/15.
 */
public class SupercykelstiPathOverlay extends PathOverlay {

    public SupercykelstiPathOverlay() {
        super(Color.argb(127, 255, 102, 0), 10);
    }

    public static ArrayList<SupercykelstiPathOverlay> getSupercykelstiPathsFromJSON() {
        ArrayList<SupercykelstiPathOverlay> ret = new ArrayList<SupercykelstiPathOverlay>();

        try {
            String routesStr = Util.stringFromJsonAssets(IBikeApplication.getContext(), "stations/farum-route.json");

            JSONArray routesArray = (JSONArray) (new JSONObject(routesStr)).getJSONArray("coordinates");
            for (int i = 0; i < routesArray.length(); i++) {
                JSONArray curRouteArray = (JSONArray) routesArray.get(i);
                SupercykelstiPathOverlay cur = new SupercykelstiPathOverlay();

                for (int j = 0; j < curRouteArray.length(); j++) {
                    cur.addPoint(
                            (Double) ((JSONArray) curRouteArray.get(j)).get(1),
                            (Double) ((JSONArray) curRouteArray.get(j)).get(0)
                    );
                }

                ret.add(cur);
            }
        } catch(JSONException e) {
            Log.e("JC", "JSONException while parsing Supercykelsti");
        }

        return ret;
    }

}
