package dk.kk.cykelsuperstier.map;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.map.IBCMarker;
import dk.kk.ibikecphlib.map.MarkerType;
import dk.kk.ibikecphlib.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jens on 7/20/15.
 */
public class ServiceStationMarker extends IBCMarker {

    public static final Icon serviceStationIcon = new Icon(IBikeApplication.getContext().getResources().getDrawable(dk.kk.ibikecphlib.R.drawable.service_pin));

    public ServiceStationMarker(String title, LatLng latLng) {
        super(title, "", latLng, MarkerType.OVERLAY);

        this.setIcon(serviceStationIcon);
    }

    public static ArrayList<ServiceStationMarker> getServiceStationMarkersFromJSON() {

        ArrayList<ServiceStationMarker> serviceStations = new ArrayList<ServiceStationMarker>();

        try {
            String stationsStr = Util.stringFromJsonAssets(IBikeApplication.getContext(), "stations/stations.json");
            JSONArray stationsJson = (new JSONObject(stationsStr)).getJSONArray("stations");
            for (int i = 0; i < stationsJson.length(); i++) {
                JSONObject stationJson = (JSONObject) stationsJson.get(i);

                if (stationJson.has("coords")) {
                    // The coordinates are in the format: "{lon} {lat}" so first we have to split it at the space with a regex
                    String[] coords = stationJson.getString("coords").split("\\s+");
                    String type = stationJson.getString("type");
                    if (coords.length > 1) {
                        if (type.equals("service")) {
                            ServiceStationMarker m = new ServiceStationMarker(stationJson.getString("name"), new LatLng(Double.parseDouble(coords[1]),Double.parseDouble(coords[0])));

                            serviceStations.add(m);
                        }
                    }
                }
            }
        } catch(JSONException e) {
            // whatever
        }

        return serviceStations;
    }
}
