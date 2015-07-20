package com.spoiledmilk.cykelsuperstier.map;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jens on 7/20/15.
 */
public class CPOverlay  {


    public void loadStations() {

        ArrayList<Marker> serviceStations = new ArrayList<Marker>();

        try {
            String stationsStr = Util.stringFromJsonAssets(IbikeApplication.getContext(), "stations/stations.json");
            JSONArray stationsJson = (new JSONObject(stationsStr)).getJSONArray("stations");
            for (int i = 0; i < stationsJson.length(); i++) {
                JSONObject stationJson = (JSONObject) stationsJson.get(i);

                if (stationJson.has("coords")) {
                    // The coordinates are in the format: "{lon} {lat}" so first we have to split it at the space with a regex
                    String[] coords = stationJson.getString("coords").split("\\s+");
                    String type = stationJson.getString("type");
                    if (coords.length > 1) {
                        if (type.equals("service")) {
                            Marker m = new Marker(stationJson.getString("name"),stationJson.getString("line"),new LatLng(Double.parseDouble(coords[1]),Double.parseDouble(coords[0])));

                            m.setIcon(new Icon(IbikeApplication.getContext().getResources().getDrawable(com.spoiledmilk.ibikecph.R.drawable.service_pin)));

                            serviceStations.add(m);
                        }
                    }
                }
            }
        } catch(JSONException e) {
            // whatever
        }
    }
}
