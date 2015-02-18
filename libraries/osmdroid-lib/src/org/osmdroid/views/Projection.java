package org.osmdroid.views;

import microsoft.mappoint.TileSystem;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IProjection;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.constants.GeoConstants;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * A Projection serves to translate between the coordinate system of x/y on-screen pixel coordinates and that of
 * latitude/longitude points on the surface of the earth. You obtain a Projection from MapView.getProjection(). You
 * should not hold on to this object for more than one draw, since the projection of the map could change. <br />
 * <br />
 * <I>Screen coordinates</I> are in the coordinate system of the screen's Canvas. The origin is in the center of the
 * plane. <I>Screen coordinates</I> are appropriate for using to draw to the screen.<br />
 * <br />
 * <I>Map coordinates</I> are in the coordinate system of the standard Mercator projection. The origin is in the
 * upper-left corner of the plane. <I>Map coordinates</I> are appropriate for use in the TileSystem class.<br />
 * <br />
 * <I>Intermediate coordinates</I> are used to cache the computationally heavy part of the projection. They aren't
 * suitable for use until translated into <I>screen coordinates</I> or <I>map coordinates</I>.
 * 
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 */
public class Projection implements IProjection, GeoConstants {

	private int viewWidth_2;
	private int viewHeight_2;
	private int worldSize_2;
	private int offsetX;
	private int offsetY;

	private final BoundingBoxE6 mBoundingBoxProjection;
	private final float mZoomLevelProjection;
	private final Rect mScreenRectProjection;
	private final Rect mIntrinsicScreenRectProjection;
	private final float mMapOrientation;

	private MapView mapView;

	public Projection(MapView mapView) {
		this.mapView = mapView;
		viewWidth_2 = mapView.getWidth() / 2;
		viewHeight_2 = mapView.getHeight() / 2;
		worldSize_2 = TileSystem.MapSize((int) mapView.mZoomLevel) / 2;
		offsetX = -worldSize_2;
		offsetY = -worldSize_2;
		mZoomLevelProjection = mapView.mZoomLevel;
		mBoundingBoxProjection = mapView.getBoundingBox();
		mScreenRectProjection = mapView.getScreenRect(null);
		mIntrinsicScreenRectProjection = mapView.getIntrinsicScreenRect(null);
		mMapOrientation = mapView.getMapOrientation();
	}

	public float getZoomLevel() {
		return mZoomLevelProjection;
	}

	public int getZoomLevelFloor() {
		return (int) mZoomLevelProjection;
	}

	public BoundingBoxE6 getBoundingBox() {
		return mBoundingBoxProjection;
	}

	public Rect getScreenRect() {
		return mScreenRectProjection;
	}

	public Rect getIntrinsicScreenRect() {
		return mIntrinsicScreenRectProjection;
	}

	public float getMapOrientation() {
		return mMapOrientation;
	}

	/**
	 * @deprecated Use TileSystem.getTileSize() instead.
	 */
	@Deprecated
	public int getTileSizePixels() {
		return TileSystem.getTileSize();
	}

	/**
	 * @deprecated Use
	 *             <code>Point out = TileSystem.PixelXYToTileXY(screenRect.centerX(), screenRect.centerY(), null);</code>
	 *             instead.
	 */
	@Deprecated
	public Point getCenterMapTileCoords() {
		final Rect rect = getScreenRect();
		return TileSystem.PixelXYToTileXY(rect.centerX(), rect.centerY(), null);
	}

	/**
	 * @deprecated Use
	 *             <code>final Point out = TileSystem.TileXYToPixelXY(centerMapTileCoords.x, centerMapTileCoords.y, null);</code>
	 *             instead.
	 */
	@Deprecated
	public Point getUpperLeftCornerOfCenterMapTile() {
		final Point centerMapTileCoords = getCenterMapTileCoords();
		return TileSystem.TileXYToPixelXY(centerMapTileCoords.x, centerMapTileCoords.y, null);
	}

	/**
	 * Converts <I>screen coordinates</I> to the underlying GeoPoint.
	 * 
	 * @param x
	 * @param y
	 * @return GeoPoint under x/y.
	 */
	public IGeoPoint fromPixels(final float x, final float y) {
		final Rect screenRect = getIntrinsicScreenRect();
		return TileSystem.PixelXYToLatLong(screenRect.left + (int) x + worldSize_2, screenRect.top + (int) y + worldSize_2, getZoomLevel(),
				null);
	}

	public Point fromMapPixels(final int x, final int y, final Point reuse) {
		final Point out = reuse != null ? reuse : new Point();
		out.set(x - viewWidth_2, y - viewHeight_2);
		out.offset(mapView.getScrollX(), mapView.getScrollY());
		return out;
	}

	/**
	 * Converts a GeoPoint to its <I>screen coordinates</I>.
	 * 
	 * @param in
	 *            the GeoPoint you want the <I>screen coordinates</I> of
	 * @param reuse
	 *            just pass null if you do not have a Point to be 'recycled'.
	 * @return the Point containing the <I>screen coordinates</I> of the GeoPoint passed.
	 */
	public Point toMapPixels(final IGeoPoint in, final Point reuse) {
		final Point out = reuse != null ? reuse : new Point();
		TileSystem.LatLongToPixelXY(in.getLatitudeE6() * 1E-6, in.getLongitudeE6() * 1E-6, getZoomLevelFloor(), out);
		out.offset(offsetX, offsetY);
		if (Math.abs(out.x - mapView.getScrollX()) > Math.abs(out.x - TileSystem.MapSize(getZoomLevelFloor()) - mapView.getScrollX())) {
			out.x -= TileSystem.MapSize(getZoomLevelFloor());
		}
		if (Math.abs(out.x - mapView.getScrollX()) > Math.abs(out.x + TileSystem.MapSize(getZoomLevelFloor()) - mapView.getScrollX())) {
			out.x += TileSystem.MapSize(getZoomLevelFloor());
		}
		if (Math.abs(out.y - mapView.getScrollY()) > Math.abs(out.y - TileSystem.MapSize(getZoomLevelFloor()) - mapView.getScrollY())) {
			out.y -= TileSystem.MapSize(getZoomLevelFloor());
		}
		if (Math.abs(out.y - mapView.getScrollY()) > Math.abs(out.y + TileSystem.MapSize(getZoomLevelFloor()) - mapView.getScrollY())) {
			out.y += TileSystem.MapSize(getZoomLevelFloor());
		}

		return out;
	}

	/**
	 * Performs only the first computationally heavy part of the projection. Call toMapPixelsTranslated to get the final
	 * position.
	 * 
	 * @param latituteE6
	 *            the latitute of the point
	 * @param longitudeE6
	 *            the longitude of the point
	 * @param reuse
	 *            just pass null if you do not have a Point to be 'recycled'.
	 * @return intermediate value to be stored and passed to toMapPixelsTranslated.
	 */
	public Point toMapPixelsProjected(final int latituteE6, final int longitudeE6, final Point reuse) {
		final Point out = reuse != null ? reuse : new Point();

		TileSystem.LatLongToPixelXY(latituteE6 * 1E-6, longitudeE6 * 1E-6, MapView.MAXIMUM_ZOOMLEVEL, out);
		return out;
	}

	/**
	 * Performs the second computationally light part of the projection. Returns results in <I>screen coordinates</I>.
	 * 
	 * @param in
	 *            the Point calculated by the toMapPixelsProjected
	 * @param reuse
	 *            just pass null if you do not have a Point to be 'recycled'.
	 * @return the Point containing the <I>Screen coordinates</I> of the initial GeoPoint passed to the
	 *         toMapPixelsProjected.
	 */
	public Point toMapPixelsTranslated(final Point in, final Point reuse) {
		final Point out = reuse != null ? reuse : new Point();

		final int zoomDifference = MapView.MAXIMUM_ZOOMLEVEL - getZoomLevelFloor();
		out.set((in.x >> zoomDifference) + offsetX, (in.y >> zoomDifference) + offsetY);
		return out;
	}

	/**
	 * Translates a rectangle from <I>screen coordinates</I> to <I>intermediate coordinates</I>.
	 * 
	 * @param in
	 *            the rectangle in <I>screen coordinates</I>
	 * @return a rectangle in </I>intermediate coordindates</I>.
	 */
	public Rect fromPixelsToProjected(final Rect in) {
		final Rect result = new Rect();

		final int zoomDifference = MapView.MAXIMUM_ZOOMLEVEL - getZoomLevelFloor();

		final int x0 = in.left - offsetX << zoomDifference;
		final int x1 = in.right - offsetX << zoomDifference;
		final int y0 = in.bottom - offsetY << zoomDifference;
		final int y1 = in.top - offsetY << zoomDifference;

		result.set(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));
		return result;
	}

	/**
	 * @deprecated Use TileSystem.TileXYToPixelXY
	 */
	@Deprecated
	public Point toPixels(final Point tileCoords, final Point reuse) {
		return toPixels(tileCoords.x, tileCoords.y, reuse);
	}

	/**
	 * @deprecated Use TileSystem.TileXYToPixelXY
	 */
	@Deprecated
	public Point toPixels(final int tileX, final int tileY, final Point reuse) {
		return TileSystem.TileXYToPixelXY(tileX, tileY, reuse);
	}

	// not presently used
	public Rect toPixels(final BoundingBoxE6 pBoundingBoxE6) {
		final Rect rect = new Rect();

		final Point reuse = new Point();

		toMapPixels(new GeoPoint(pBoundingBoxE6.getLatNorthE6(), pBoundingBoxE6.getLonWestE6()), reuse);
		rect.left = reuse.x;
		rect.top = reuse.y;

		toMapPixels(new GeoPoint(pBoundingBoxE6.getLatSouthE6(), pBoundingBoxE6.getLonEastE6()), reuse);
		rect.right = reuse.x;
		rect.bottom = reuse.y;

		return rect;
	}

	@Override
	public float metersToEquatorPixels(final float meters) {
		return meters / (float) TileSystem.GroundResolution(0, getZoomLevelFloor());
	}

	@Override
	public Point toPixels(final IGeoPoint in, final Point out) {
		return toMapPixels(in, out);
	}

	@Override
	public IGeoPoint fromPixels(final int x, final int y) {
		return fromPixels((float) x, (float) y);
	}
}
