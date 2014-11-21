package org.osmdroid.tileprovider;

import java.util.HashMap;
import java.util.Map;

public class MapTileInfoManager {
	
	private static final String TAG = "MapTileInfoManager";

	private static MapTileInfoManager mInstance;
	
//	private static LruCache <MapTile, MapTileInfo> mapTileInfos;
	private static Map <MapTile, MapTileInfo> mapTileInfos;
	
	public static MapTileInfoManager getInstance(){
		if(mInstance == null) {
			mInstance = new MapTileInfoManager();
//			int memCacheSize = Math.round(0.15f * Runtime.getRuntime().maxMemory());
//			mapTileInfos = new LruCache<MapTile, MapTileInfo>(memCacheSize);
			mapTileInfos = new HashMap<MapTile, MapTileInfo>();
		}
		return mInstance;
	}
	
	public void setTileExpired(MapTile mapTile, boolean expired) {
//		Log.d(TAG, "setTileExpired called, expired: " + expired);
		MapTileInfo mapTileInfo = mapTileInfos.get(mapTile);
		if (mapTileInfo != null) {
			mapTileInfo.setExpired(expired);
		}
	}
	
	public boolean didTileExpire(MapTile mapTile) {
//		Log.d(TAG, "didTileExpire called");
		MapTileInfo mapTileInfo = mapTileInfos.get(mapTile);
		return mapTileInfo != null ? mapTileInfo.isExpired() : false;
	}
	
	public MapTileInfo getMapTileInfo(MapTile mapTile) {
//		Log.d(TAG, "getMapTileInfo called");
		MapTileInfo mapTileInfo = mapTileInfos.get(mapTile);
		return mapTileInfo;
	}
	
	public void putMapTileInfo(MapTile mapTile, MapTileInfo info) {
//		Log.i(TAG, "putMapTileInfo called, info: " + info);
		mapTileInfos.put(mapTile, info);
	}

}
