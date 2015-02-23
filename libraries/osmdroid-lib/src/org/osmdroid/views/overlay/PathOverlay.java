package org.osmdroid.views.overlay;

import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * 
 * @author Viesturs Zarins
 * @author Martin Pearman
 * 
 *         This class draws a path line in given color.
 */
public class PathOverlay extends Overlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * Stores points, converted to the map projection.
	 */
	private ArrayList<Point> mPoints;

	/**
	 * Number of points that have precomputed values.
	 */
	private int mPointsPrecomputed;

	/**
	 * Paint settings.
	 */
	protected Paint mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG); // Paint.DITHER_FLAG |

	private final Path mPath = new Path();

	private final Point mTempPoint1 = new Point();
	private final Point mTempPoint2 = new Point();

	// bounding rectangle for the current line segment.
	private final Rect mLineBounds = new Rect();

	// ===========================================================
	// Constructors
	// ===========================================================

	public PathOverlay(final int color, final Context ctx) {
		this(color, new ResourceProxy(ctx));
	}

	public PathOverlay(final int color, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		this.mPaint.setColor(color);
		this.mPaint.setStrokeWidth(2.0f / mResourceProxy.getDisplayMetricsDensity());
		this.mPaint.setStyle(Paint.Style.STROKE); // set to STOKE
		this.mPaint.setStrokeJoin(Paint.Join.ROUND); // set the join to round you want
		this.mPaint.setStrokeCap(Paint.Cap.ROUND); // set the paint cap to round too
		// this.mPaint.setPathEffect(new CornerPathEffect(1)); // set the path effect when they join. 10

		this.clearPath();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setColor(final int color) {
		this.mPaint.setColor(color);
	}

	public void setAlpha(final int a) {
		this.mPaint.setAlpha(a);
	}

	/**
	 * Draw a great circle. Calculate a point for every 100km along the path.
	 * 
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 */
	public void addGreatCircle(final GeoPoint startPoint, final GeoPoint endPoint) {
		// // get the great circle path length in meters
		// final int greatCircleLength=startPoint.distanceTo(endPoint);
		//
		// // add one point for every 100kms of the great circle path
		// final int numberOfPoints=greatCircleLength/100000;
		//
		// addGreatCircle(startPoint, endPoint, numberOfPoints);
		addGreatCircle(startPoint, endPoint, 2);
	}

	/**
	 * Draw a great circle.
	 * 
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 * @param numberOfPoints
	 *            number of points to calculate along the path
	 */
	public void addGreatCircle(final GeoPoint startPoint, final GeoPoint endPoint, final int numberOfPoints) {
		// adapted from page http://compastic.blogspot.co.uk/2011/07/how-to-draw-great-circle-on-map-in.html
		// which was adapted from page http://maps.forum.nu/gm_flight_path.html

		// convert to radians
		final double lat1 = startPoint.getLatitudeE6() * 1E-6 * Math.PI / 180;
		final double lon1 = startPoint.getLongitudeE6() * 1E-6 * Math.PI / 180;
		final double lat2 = endPoint.getLatitudeE6() * 1E-6 * Math.PI / 180;
		final double lon2 = endPoint.getLongitudeE6() * 1E-6 * Math.PI / 180;

		final double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
				* Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
		double bearing = Math.atan2(Math.sin(lon1 - lon2) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2))
				/ -(Math.PI / 180);
		bearing = bearing < 0 ? 360 + bearing : bearing;

		for (int i = 0; i < numberOfPoints + 1; i++) {
			final double f = 1.0 / numberOfPoints * i;
			final double A = Math.sin((1 - f) * d) / Math.sin(d);
			final double B = Math.sin(f * d) / Math.sin(d);
			final double x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2) * Math.cos(lon2);
			final double y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2) * Math.sin(lon2);
			final double z = A * Math.sin(lat1) + B * Math.sin(lat2);

			final double latN = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
			final double lonN = Math.atan2(y, x);
			addPoint((int) (latN / (Math.PI / 180) * 1E6), (int) (lonN / (Math.PI / 180) * 1E6));
		}
	}

	public Paint getPaint() {
		return mPaint;
	}

	public void setPaint(final Paint pPaint) {
		if (pPaint == null) {
			throw new IllegalArgumentException("pPaint argument cannot be null");
		}
		mPaint = pPaint;
	}

	public void clearPath() {
		this.mPoints = new ArrayList<Point>();
		this.mPointsPrecomputed = 0;
	}

	public void addPoint(final GeoPoint pt) {
		if (this.mPoints.size() > 0) {
			// sampling
			Point prev = this.mPoints.get(this.mPoints.size() - 1);
			if (pt.bearingTo(new GeoPoint(prev.x, prev.y)) <= 120 && pt.bearingTo(new GeoPoint(prev.x, prev.y)) >= 30) {
				// divide(prev.x, prev.y, pt.getLatitudeE6(), pt.getLongitudeE6(), 0);
			}
		}
		this.addPoint(pt.getLatitudeE6(), pt.getLongitudeE6());
	}

	// final static int SAMPLE_DEPTH = 1;

	// private void divide(int x1, int y1, int x2, int y2, int depth) {
	// if (depth != SAMPLE_DEPTH && (x1 != x2 && y1 != y2)) {
	// divide(x1, y1, (x1 + x2) / 2, (y1 + y2) / 2, depth + 1);
	// divide((x1 + x2) / 2, (y1 + y2) / 2, x2, y2, depth + 1);
	// }
	// this.mPoints.add(new Point((x1 + x2) / 2, (y1 + y2) / 2));
	// }

	public void addPoint(final int latitudeE6, final int longitudeE6) {
		this.mPoints.add(new Point(latitudeE6, longitudeE6));
	}

	public int getNumberOfPoints() {
		return this.mPoints.size();
	}

	float zoomDiff, newWidth;

	/**
	 * This method draws the line. Note - highly optimized to handle long paths, proceed with care. Should be fine up to
	 * 10K points.
	 */
	@Override
	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {

		if (shadow) {
			return;
		}

		if (this.mPoints.size() < 2) {
			// nothing to paint
			return;
		}

		final float DEFAULT_PATH_WIDTH = 10 * mResourceProxy.getDisplayMetricsDensity();

		if (mapView.isPinchZooming) {
			zoomDiff = mapView.getZoomLevel() - mapView.zoomLevelPinch;
			newWidth = DEFAULT_PATH_WIDTH + zoomDiff * DEFAULT_PATH_WIDTH / 4;
			this.mPaint.setStrokeWidth(DEFAULT_PATH_WIDTH * mapView.scaleDiffFloat);
		} else {
			this.mPaint.setStrokeWidth(DEFAULT_PATH_WIDTH);
		}

		final Projection pj = mapView.getProjection();

		// precompute new points to the intermediate projection.
		final int size = this.mPoints.size();

		while (this.mPointsPrecomputed < size) {
			final Point pt = this.mPoints.get(this.mPointsPrecomputed);
			pj.toMapPixelsProjected(pt.x, pt.y, pt);

			this.mPointsPrecomputed++;
		}

		Point screenPoint0 = null; // points on screen
		Point screenPoint1 = null;
		Point projectedPoint0; // points from the points list
		Point projectedPoint1;

		// clipping rectangle in the intermediate projection, to avoid performing projection.
		// final Rect clipBounds = pj.fromPixelsToProjected(pj.getScreenRect());

		mPath.rewind();
		projectedPoint0 = this.mPoints.get(size - 1);
		mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);

		for (int i = size - 2; i >= 0; i--) {
			// compute next points
			projectedPoint1 = this.mPoints.get(i);
			mLineBounds.union(projectedPoint1.x, projectedPoint1.y);

			// if (!Rect.intersects(clipBounds, mLineBounds)) {
			// // skip this line, move to next point
			// projectedPoint0 = projectedPoint1;
			// screenPoint0 = null;
			// continue;
			// }

			// the starting point may be not calculated, because previous segment was out of clip
			// bounds
			if (screenPoint0 == null) {
				screenPoint0 = pj.toMapPixelsTranslated(projectedPoint0, this.mTempPoint1);
				mPath.moveTo(screenPoint0.x, screenPoint0.y);
			}

			screenPoint1 = pj.toMapPixelsTranslated(projectedPoint1, this.mTempPoint2);

			// skip this point, too close to previous point
			if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
				continue;
			}

			mPath.lineTo(screenPoint1.x, screenPoint1.y);

			// update starting point to next position
			projectedPoint0 = projectedPoint1;
			screenPoint0.x = screenPoint1.x;
			screenPoint0.y = screenPoint1.y;
			mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);
		}

		canvas.drawPath(mPath, this.mPaint);
	}

}
