package org.osmdroid.tileprovider.tilesource;

import microsoft.mappoint.TileSystem;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.util.GeoPoint;

import android.graphics.Point;

public class XYTileSource extends OnlineTileSourceBase {

	public XYTileSource(final String aName, int aResourceId, final int aZoomMinLevel, final int aZoomMaxLevel, final int aTileSizePixels,
			final String aImageFilenameEnding, final String... aBaseUrl) {
		super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
	}

	@Override
	public String getTileURLString(final MapTile aTile) {
		if (aTile.getZoomLevel() > 17) {
			final String zoomLevel = "" + 17;
			int x = aTile.getX(), y = aTile.getY();
			final Point p2 = TileSystem.TileXYToPixelXY(x, y, null);
			final GeoPoint gp = TileSystem.PixelXYToLatLong(p2.x, p2.y, aTile.getZoomLevel(), null);
			double lat = gp.getLatitudeE6() / 1000000d, lon = gp.getLongitudeE6() / 1000000d;
			final Point p = TileSystem.LatLongToPixelXY(lat, lon, 17, null);
			final Point p3 = TileSystem.PixelXYToTileXY(p.x, p.y, null);
			return getBaseUrl() + zoomLevel + "/" + p3.x + "/" + p3.y + mImageFilenameEnding;
		}
		return getBaseUrl() + aTile.getZoomLevel() + "/" + aTile.getX() + "/" + aTile.getY() + mImageFilenameEnding;
	}
}
