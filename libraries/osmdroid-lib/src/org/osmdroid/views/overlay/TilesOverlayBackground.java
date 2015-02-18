package org.osmdroid.views.overlay;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTileProviderBase;
import android.content.Context;

public class TilesOverlayBackground extends TilesOverlay {

	public TilesOverlayBackground(MapTileProviderBase aTileProvider, Context aContext) {
		super(aTileProvider, aContext);
	}

	public TilesOverlayBackground(final MapTileProviderBase aTileProvider, final ResourceProxy pResourceProxy) {
		super(aTileProvider, pResourceProxy);
	}
	//
	// @Override
	// protected void drawSafe(final ISafeCanvas c, final MapView osmv, final boolean shadow) {
	//
	// mapView = osmv;
	//
	// if (shadow) {
	// return;
	// }
	//
	// float zoomLevel = osmv.getZoomLevelFloor();
	// float scaleDiffFloat = (float) (Math.log(osmv.mMultiTouchScale) * MapView.ZOOM_LOG_BASE_INV);
	// if (scaleDiffFloat >= 0.5f || scaleDiffFloat <= -0.5f) {
	// float scaleInc = (int) (scaleDiffFloat / 0.5f) * 0.5f;
	// if (zoomLevel + scaleInc < osmv.getMaxZoomLevel() && zoomLevel - scaleInc > osmv.getMinZoomLevel()) {
	// zoomLevel = zoomLevel + scaleInc * 2f;
	// }
	// }
	//
	// // Calculate the half-world size
	// final Projection pj = osmv.getProjection();
	//
	// mWorldSize_2 = TileSystem.MapSize(osmv.getZoomLevel()) >> 1;
	//
	// // Get the area we are drawing to
	// mViewPort.set(pj.getScreenRect());
	//
	// // Translate the Canvas coordinates into Mercator coordinates
	// mViewPort.offset(mWorldSize_2, mWorldSize_2);
	//
	// if (zoomLevel > osmv.getZoomLevel()) {
	// final int worldSize_current_2 = TileSystem.MapSize(osmv.getZoomLevel()) / 2;
	// final int worldSize_new_2 = TileSystem.MapSize(zoomLevel) / 2;
	// // final IGeoPoint centerGeoPoint = TileSystem.PixelXYToLatLong(getScrollX() + worldSize_current_2,
	// // getScrollY()
	// // + worldSize_current_2, curZoomLevelFloor, null);
	// // final Point centerPoint = TileSystem.LatLongToPixelXY(centerGeoPoint.getLatitudeE6() / 1E6,
	// // centerGeoPoint.getLongitudeE6() / 1E6, newZoomLevelFloor, null);
	// // scrollTo(false, centerPoint.x - worldSize_new_2, centerPoint.y - worldSize_new_2);
	// } else if (zoomLevel < osmv.getZoomLevel()) {
	// // int diff = (int) (osmv.getZoomLevel() - zoomLevel);
	// GeoPoint gp = TileSystem.PixelXYToLatLong(mViewPort.left, mViewPort.top, osmv.getZoomLevel(), null);
	// Point p = TileSystem.LatLongToPixelXY(gp.getLatitudeE6() / 1000000d, gp.getLongitudeE6() / 1000000d, (int)
	// zoomLevel, null);
	// mViewPort.left = p.x;
	// mViewPort.top = p.y;
	// gp = TileSystem.PixelXYToLatLong(mViewPort.right, mViewPort.bottom, osmv.getZoomLevel(), null);
	// p = TileSystem.LatLongToPixelXY(gp.getLatitudeE6() / 1000000d, gp.getLongitudeE6() / 1000000d, (int) zoomLevel,
	// null);
	// mViewPort.right = p.x;
	// mViewPort.bottom = p.y;
	// }
	//
	// Log.d("", "TilesBackground zoomLevel = " + zoomLevel);
	//
	// // Draw the tiles!
	// drawTiles(c.getSafeCanvas(), (int) zoomLevel, TileSystem.getTileSize(), mViewPort);
	//
	// }

}
