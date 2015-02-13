package com.spoiledmilk.ibikecph.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.view.View;
import android.widget.RelativeLayout;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapFragmentBase;
import com.spoiledmilk.ibikecph.map.OverlayData;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * Manages overlays, that is paths and icons.
 * @see com.spoiledmilk.ibikecph.map.OverlayData
 * @author jens
 *
 */
public class OverlaysManager {

	private MapFragmentBase mapFragment;
	protected ArrayList<OverlayData> serviceStations;
	protected ArrayList<OverlayData> metroStations;
	protected ArrayList<OverlayData> sTrainStations;
	protected ArrayList<OverlayData> localTrainStations;
	protected ArrayList<Location> bikeRoute1, bikeRoute2;
	protected PathOverlay pathOverlay1, pathOverlay2;

	public OverlaysManager(MapFragmentBase mapFragment) {
		this.mapFragment = mapFragment;
	}

	public void loadOverlaysData(Context context) {
		if (serviceStations == null) {
			serviceStations = new ArrayList<OverlayData>();
			metroStations = new ArrayList<OverlayData>();
			sTrainStations = new ArrayList<OverlayData>();
			localTrainStations = new ArrayList<OverlayData>();
			bikeRoute1 = new ArrayList<Location>();
			bikeRoute2 = new ArrayList<Location>();
			try {
				String stationsStr = Util.stringFromJsonAssets(context, "stations/stations.json");
				JSONArray stationsJson = (new JSONObject(stationsStr)).getJSONArray("stations");
				for (int i = 0; i < stationsJson.length(); i++) {
					JSONObject stationJson = (JSONObject) stationsJson.get(i);
					if (stationJson.has("coords")) {
						String[] coords = stationJson.getString("coords").split("\\s+");
						String type = stationJson.getString("type");
						if (coords.length > 1) {
							if (type.equals("service"))
								serviceStations.add(new OverlayData(stationJson.getString("name"), stationJson.getString("line"), Double
										.parseDouble(coords[1]), Double.parseDouble(coords[0]), R.drawable.service_pin));
							else if (type.equals("metro"))
								metroStations.add(new OverlayData(stationJson.getString("name"), stationJson.getString("line"), Double
										.parseDouble(coords[1]), Double.parseDouble(coords[0]), R.drawable.metro_logo_pin));
							else if (type.equals("s-train"))
								sTrainStations.add(new OverlayData(stationJson.getString("name"), stationJson.getString("line"), Double
										.parseDouble(coords[1]), Double.parseDouble(coords[0]), R.drawable.list_subway_icon));
							else if (type.equals("local-train"))
								localTrainStations.add(new OverlayData(stationJson.getString("name"), stationJson.getString("line"), Double
										.parseDouble(coords[1]), Double.parseDouble(coords[0]), R.drawable.local_train_icon_blue));
						}
					}
				}

				String routesStr = Util.stringFromJsonAssets(context, "stations/farum-route.json");
				JSONArray route1 = (JSONArray) (new JSONObject(routesStr)).getJSONArray("coordinates").get(0);
				JSONArray route2 = (JSONArray) (new JSONObject(routesStr)).getJSONArray("coordinates").get(2);
				for (int i = 0; i < route1.length(); i++) {
					bikeRoute1.add(Util.locationFromCoordinates((Double) ((JSONArray) route1.get(i)).get(1),
							(Double) ((JSONArray) route1.get(i)).get(0)));
				}
				for (int i = 0; i < route2.length(); i++) {
					bikeRoute2.add(Util.locationFromCoordinates((Double) ((JSONArray) route2.get(i)).get(1),
							(Double) ((JSONArray) route2.get(i)).get(0)));
				}

			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
	}

	public void drawBikeRoutes(Context context) {
		if (pathOverlay1 == null) {
			pathOverlay1 = new PathOverlay(Color.rgb(236, 104, 0), context);
			pathOverlay1.setAlpha(IbikePreferences.ROUTE_ALPHA);
			pathOverlay1.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH / 2));
			for (Location loc : bikeRoute1) {
				pathOverlay1.addPoint(new GeoPoint(loc));
			}
		}
		if (pathOverlay2 == null) {
			pathOverlay2 = new PathOverlay(Color.rgb(236, 104, 0), context);
			pathOverlay2.setAlpha(IbikePreferences.ROUTE_ALPHA);
			pathOverlay2.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH / 2));
			for (Location loc : bikeRoute2) {
				pathOverlay2.addPoint(new GeoPoint(loc));
			}
		}
		mapFragment.mapView.getOverlays().add(pathOverlay1);
		mapFragment.mapView.getOverlays().add(pathOverlay2);

	}

	public void removeBikeRoutes() {
		mapFragment.mapView.getOverlays().remove(pathOverlay1);
		mapFragment.mapView.getOverlays().remove(pathOverlay2);
	}

	public void drawServiceStations(Context context) {
		drawOverlays(serviceStations, context);
	}

	public void removeServiceStations() {
		if (serviceStations != null) {
			Iterator<OverlayData> it = serviceStations.iterator();
			while (it.hasNext()) {
				OverlayData od = it.next();
				if (od.markerOverlay != null)
					mapFragment.mapView.getOverlays().remove(od.markerOverlay);
			}
		}
		mapFragment.mapView.invalidate();
	}

	public void drawMetroStations(Context context) {
		drawOverlays(metroStations, context);
	}

	public void removeMetroStations() {
		if (metroStations != null) {
			Iterator<OverlayData> it = metroStations.iterator();
			while (it.hasNext()) {
				OverlayData od = it.next();
				if (od.markerOverlay != null)
					mapFragment.mapView.getOverlays().remove(od.markerOverlay);
			}
		}
		mapFragment.mapView.invalidate();
	}

	public void drawsTrainStations(Context context) {
		drawOverlays(sTrainStations, context);
	}

	public void removesTrainStations() {
		if (sTrainStations != null) {
			Iterator<OverlayData> it = sTrainStations.iterator();
			while (it.hasNext()) {
				OverlayData od = it.next();
				if (od.markerOverlay != null)
					mapFragment.mapView.getOverlays().remove(od.markerOverlay);
			}
		}
		mapFragment.mapView.invalidate();
	}

	public void drawlocalTrainStations(Context context) {
		drawOverlays(localTrainStations, context);
	}

	public void removelocalTrainStations() {
		if (metroStations != null) {
			Iterator<OverlayData> it = localTrainStations.iterator();
			while (it.hasNext()) {
				OverlayData od = it.next();
				if (od.markerOverlay != null)
					mapFragment.mapView.getOverlays().remove(od.markerOverlay);
			}
		}
		mapFragment.mapView.invalidate();
	}

	private void drawOverlays(ArrayList<OverlayData> stations, Context context) {
		Iterator<OverlayData> it = stations.iterator();
		while (it.hasNext()) {
			final OverlayData od = it.next();
			GeoPoint gp = new GeoPoint(Util.locationFromCoordinates(od.lattitude, od.longitude));
			OverlayItem oi = new OverlayItem(od.name, od.line, gp);
			oi.setMarkerHotspot(HotspotPlace.BOTTOM_CENTER);
			oi.setMarker(context.getResources().getDrawable(od.iconResource));
			List<OverlayItem> markerList = new LinkedList<OverlayItem>();
			markerList.add(oi);
			ItemizedIconOverlay<OverlayItem> markerOverlay = new ItemizedIconOverlay<OverlayItem>(context, markerList,
					new OnItemGestureListener<OverlayItem>() {
						public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
							GeoPoint gp = new GeoPoint(Util.locationFromCoordinates(od.lattitude, od.longitude));
							MapView.LayoutParams lParams = new MapView.LayoutParams(new RelativeLayout.LayoutParams(
									RelativeLayout.LayoutParams.WRAP_CONTENT, Util.dp2px(40)));
							lParams.geoPoint = gp;
							lParams.offsetY = Util.dp2px(-35);
							lParams.alignment = MapView.LayoutParams.BOTTOM_CENTER;
							mapFragment.textStationInfo.setLayoutParams(lParams);
							mapFragment.textStationInfo.setText(od.name);
							mapFragment.textStationInfo.setVisibility(View.VISIBLE);
							mapFragment.mapView.getController().animateTo(gp);
							return true;
						}

						public boolean onItemLongPress(final int index, final OverlayItem item) {
							return true;
						}
					});
			markerOverlay.setRescaleWhenZoomed(true);
			mapFragment.mapView.getOverlays().add(markerOverlay);
			od.markerOverlay = markerOverlay;
		}
		mapFragment.mapView.invalidate();

	}

}
