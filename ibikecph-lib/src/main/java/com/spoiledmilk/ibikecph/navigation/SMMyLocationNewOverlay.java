// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import java.util.LinkedList;

import microsoft.mappoint.TileSystem;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.IOverlayMenuProvider;
import org.osmdroid.views.overlay.Overlay.Snappable;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.util.constants.MapViewConstants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.SMMapFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMGPSUtil;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;

public class SMMyLocationNewOverlay extends SafeDrawOverlay implements IMyLocationConsumer, IOverlayMenuProvider, Snappable {

    protected final SafePaint mPaint = new SafePaint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
    protected final SafePaint mCirclePaint = new SafePaint();

    protected Bitmap mPersonBitmap;
    protected Bitmap mDirectionArrowBitmap;

    protected final MapView mMapView;

    public IMyLocationProvider mMyLocationProvider;

    private final LinkedList<Runnable> mRunOnFirstFix = new LinkedList<Runnable>();
    private final Point mMapCoords = new Point();

    private Location mLocation;
    private boolean mIsLocationEnabled = false;
    protected boolean mIsFollowing = false; // follow location updates
    protected boolean mDrawAccuracyEnabled = true;

    /** Coordinates the feet of the person are located scaled for display density. */
    protected PointF mPersonHotspot;

    protected final float mDirectionArrowCenterX;
    protected final float mDirectionArrowCenterY;

    public static final int MENU_MY_LOCATION = getSafeMenuId();

    private boolean mOptionsMenuEnabled = true;

    // to avoid allocations during onDraw
    private final Matrix mDirectionRotater = new Matrix();
    private final float[] mMatrixValues = new float[9];
    private final Matrix mMatrix = new Matrix();
    private final Rect mMyLocationRect = new Rect();
    private final Rect mMyLocationPreviousRect = new Rect();
    
    // is icon rotating according to bearing direction
    public boolean isDirection = true;
    private SMRoute route = null;
    public float mapOrientation = 0;
    public float compassOrientation = 0;

    boolean isTooFarFromRoute = false;
    float instructionBearing;

    Location lastLocation;
    long lastDrawTimestamp = 0, lastLocTimestamp = 0;

    // private Context context;
    SMRouteNavigationMapFragment navigationFragment;
    SMMapFragment mapFragment;

    int resId;

    // ===========================================================
    // Constructors
    // ===========================================================

    public SMMyLocationNewOverlay(final Context context, IMyLocationProvider myLocationProvider, final MapView mapView) {
        this(context, myLocationProvider, mapView, new ResourceProxy(context));
    }

    public SMMyLocationNewOverlay(final Context context, IMyLocationProvider myLocationProvider, final MapView mapView,
            final ResourceProxy resourceProxy) {
        super(resourceProxy);
        // this.context = context;
        if (myLocationProvider == null)
            throw new RuntimeException("You must pass an IMyLocationProvider to enableMyLocation()");
        mMyLocationProvider = myLocationProvider;
        mMapView = mapView;
        mCirclePaint.setARGB(0, 100, 100, 255);
        mCirclePaint.setAntiAlias(true);
        mPersonBitmap = mResourceProxy.getBitmap(ResourceProxy.person);
        mDirectionArrowBitmap = mResourceProxy.getBitmap(ResourceProxy.direction_arrow);
        mDirectionArrowCenterX = mDirectionArrowBitmap.getWidth() / 2 - 0.5f;
        mDirectionArrowCenterY = mDirectionArrowBitmap.getHeight() / 2 - 0.5f;
        // Calculate position of person icon's feet, scaled to screen density
        mPersonHotspot = new PointF(24.0f * mScale + 0.5f, 39.0f * mScale + 0.5f);
    }

    protected void drawMyLocation(final ISafeCanvas canvas, final MapView mapView, final Location lastFix) {
        final Projection pj = mapView.getProjection();
        final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - pj.getZoomLevelFloor();
        canvas.getMatrix(mMatrix);
        mMatrix.getValues(mMatrixValues);
        mDirectionRotater.reset();
        // Calculate real scale including accounting for rotation
        float scaleX = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_X] * mMatrixValues[Matrix.MSCALE_X] + mMatrixValues[Matrix.MSKEW_Y]
                * mMatrixValues[Matrix.MSKEW_Y]);
        float scaleY = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_Y] * mMatrixValues[Matrix.MSCALE_Y] + mMatrixValues[Matrix.MSKEW_X]
                * mMatrixValues[Matrix.MSKEW_X]);
        final double x = mMapCoords.x >> zoomDiff;
        final double y = mMapCoords.y >> zoomDiff;
        if (resId != com.spoiledmilk.ibikecph.R.drawable.tracking_dot && route != null) { // && mapView.directionShown
                                                                                          // && isDirection
            instructionBearing = 0;
            if (isTooFarFromRoute) {
                // instructionBearing = -mapOrientation - compassOrientation;
                instructionBearing = -mapOrientation;
                if (lastLocation != null && lastLocation.hasBearing()) {
                    instructionBearing -= lastLocation.getBearing();
                }
            } else {
                instructionBearing = (float) route.lastCorrectedHeading;
            }
            // if (mapView.isPinchZooming) {
            // Point mCurScreenCoords = new Point();
            // pj.toMapPixels(new GeoPoint(lastFix), mCurScreenCoords);
            // float sx = mapView.scaleDiffFloat, sy = mapView.scaleDiffFloat;
            // mDirectionRotater.setScale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
            // mDirectionRotater.postRotate(instructionBearing, ((mMapCoords.x >> zoomDiff)) - mDirectionArrowBitmap.getWidth() / 2,
            // ((mMapCoords.y >> zoomDiff)) - mDirectionArrowBitmap.getHeight() / 2);
            // } else {
            mDirectionRotater.setRotate(instructionBearing, ((mMapCoords.x >> zoomDiff)) - mDirectionArrowBitmap.getWidth() / 2,
                    ((mMapCoords.y >> zoomDiff)) - mDirectionArrowBitmap.getHeight() / 2);
            // }
            // try {
            // final Bitmap rotatedDirection = Bitmap.createBitmap(mDirectionArrowBitmap, 0, 0, (int) (mDirectionArrowBitmap.getWidth()),
            // (int) (mDirectionArrowBitmap.getHeight()), mDirectionRotater, true);
            // mPaint.setAntiAlias(true);
            // mPaint.setDither(true);
            // canvas.drawBitmap(rotatedDirection, (((mMapCoords.x >> zoomDiff)) - rotatedDirection.getWidth() / 2),
            // (((mMapCoords.y >> zoomDiff)) - rotatedDirection.getHeight() / 2), this.mPaint);
            // } catch (Exception e) {
            // if (e != null) {
            // LOG.e("", e);
            // }
            // canvas.drawBitmap(mDirectionArrowBitmap, mDirectionRotater, mPaint);
            // }
            canvas.save();
            // Rotate the icon
            canvas.rotate(instructionBearing, x, y);
            // Counteract any scaling that may be happening so the icon stays the same size
            canvas.scale(1 / scaleX, 1 / scaleY, x, y);
            // Draw the bitmap
            canvas.drawBitmap(mDirectionArrowBitmap, x - mDirectionArrowCenterX, y - mDirectionArrowCenterY, mPaint);
            canvas.restore();
        } else if (resId != com.spoiledmilk.ibikecph.R.drawable.tracking_dot && lastFix.hasBearing()) {
            /*
             * Rotate the direction-Arrow according to the bearing we are driving. And draw it to the canvas.
             */
            mDirectionRotater.setRotate(lastFix.getBearing(), mDirectionArrowCenterX, mDirectionArrowCenterY);
            mDirectionRotater.postTranslate(-mDirectionArrowCenterX, -mDirectionArrowCenterY);
            mDirectionRotater.postScale(1 / mMatrixValues[Matrix.MSCALE_X], 1 / mMatrixValues[Matrix.MSCALE_Y]);
            mDirectionRotater.postTranslate(mMapCoords.x >> zoomDiff, mMapCoords.y >> zoomDiff);
            canvas.drawBitmap(mDirectionArrowBitmap, mDirectionRotater, mPaint);

        } else {
            // if (mapView.isPinchZooming) {
            // Point mCurScreenCoords = new Point();
            // pj.toMapPixels(new GeoPoint(lastFix), mCurScreenCoords);
            // float sx = mapView.scaleDiffFloat, sy = mapView.scaleDiffFloat;
            // mDirectionRotater.setScale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
            // }
            // mDirectionRotater.setTranslate(-mPersonHotspot.x, -mPersonHotspot.y);
            // mDirectionRotater.postScale(1 / mMatrixValues[Matrix.MSCALE_X], 1 / mMatrixValues[Matrix.MSCALE_Y]);
            // mDirectionRotater.postTranslate(mMapCoords.x >> zoomDiff, mMapCoords.y >> zoomDiff);
            // canvas.drawBitmap(mPersonBitmap, mDirectionRotater, mPaint);
            canvas.save();
            // Unrotate the icon if the maps are rotated so the little man stays upright
            canvas.rotate(-mMapView.getMapOrientation(), x, y);
            // Counteract any scaling that may be happening so the icon stays the same size
            canvas.scale(1 / scaleX, 1 / scaleY, x, y);
            // Draw the bitmap
            canvas.drawBitmap(mPersonBitmap, x - mPersonHotspot.x, y - mPersonHotspot.y, mPaint);
            canvas.restore();
        }

    }

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     * 
     * @param drawAccuracyEnabled
     *            whether the accuracy circle will be enabled
     */
    public void setDrawAccuracyEnabled(final boolean drawAccuracyEnabled) {
        mDrawAccuracyEnabled = drawAccuracyEnabled;
    }

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isDrawAccuracyEnabled() {
        return mDrawAccuracyEnabled;
    }

    public IMyLocationProvider getMyLocationProvider() {
        return mMyLocationProvider;
    }

    protected void setMyLocationProvider(IMyLocationProvider myLocationProvider) {
        if (mMyLocationProvider != null) {
            mMyLocationProvider.stopLocationProvider();
        }
        mMyLocationProvider = myLocationProvider;
    }

    public void setPersonHotspot(float x, float y) {
        mPersonHotspot.set(x, y);
    }

    public void setCustomIcon(int resId, float x, float y) {
        this.resId = resId;
        mPersonBitmap = mResourceProxy.getBitmap(resId);
        mDirectionArrowBitmap = mResourceProxy.getBitmap(resId);
        setPersonHotspot(x, y);
    }

    public void setCustomIconCentered(int resId, boolean isDirection) {
        this.resId = resId;
        this.isDirection = isDirection;
        mPersonBitmap = mResourceProxy.getBitmap(resId);
        mDirectionArrowBitmap = mResourceProxy.getBitmap(resId);
        setPersonHotspot(mPersonBitmap.getWidth() / 2, mPersonBitmap.getHeight() / 2);
    }

    protected Rect getMyLocationDrawingBounds(int zoomLevel, Location lastFix, Rect reuse) {
        if (reuse == null) {
            reuse = new Rect();
        }
        final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - zoomLevel;
        final int posX = mMapCoords.x >> zoomDiff;
        final int posY = mMapCoords.y >> zoomDiff;
        // Start with the bitmap bounds
        if (resId != com.spoiledmilk.ibikecph.R.drawable.tracking_dot && route != null && route.getWaypoints() != null
                && route.getWaypoints().size() > 1) {
            int widestEdge = (int) Math.ceil(Math.max(mDirectionArrowBitmap.getWidth(), mDirectionArrowBitmap.getHeight()) * Math.sqrt(2));
            reuse.set(posX, posY, posX + widestEdge, posY + widestEdge);
            reuse.offset((int) -widestEdge / 2, (int) -widestEdge / 2);
        } else {
            reuse.set(posX, posY, posX + mPersonBitmap.getWidth(), posY + mPersonBitmap.getHeight());
            reuse.offset((int) -mPersonHotspot.x, (int) -mPersonHotspot.y);
        }
        reuse.offset(mMapView.getWidth() / 2, mMapView.getHeight() / 2);
        return reuse;
    }

    @Override
    protected void drawSafe(ISafeCanvas canvas, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }
        if (isMyLocationEnabled()) {
            if (mLocation == null && route != null) {
                if (SMLocationManager.getInstance().hasValidLocation()) {
                    mLocation = getSnappedLocation(SMLocationManager.getInstance().getLastValidLocation(), route);
                } else if (SMLocationManager.getInstance().getLastKnownLocation() != null) {
                    mLocation = getSnappedLocation(SMLocationManager.getInstance().getLastKnownLocation(), route);
                }
            }
            if (mLocation != null) {
                drawMyLocation(canvas, mapView, mLocation);
            }
        }
    }

    @Override
    public boolean onSnapToItem(final int x, final int y, final Point snapPoint, final IMapView mapView) {
        if (this.mLocation != null) {
            snapPoint.x = mMapCoords.x;
            snapPoint.y = mMapCoords.y;
            final double xDiff = x - mMapCoords.x;
            final double yDiff = y - mMapCoords.y;
            final boolean snap = xDiff * xDiff + yDiff * yDiff < 64;
            return snap;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            this.disableFollowLocation();
        }
        return super.onTouchEvent(event, mapView);
    }

    // ===========================================================
    // Menu handling methods
    // ===========================================================

    @Override
    public void setOptionsMenuEnabled(final boolean pOptionsMenuEnabled) {
        this.mOptionsMenuEnabled = pOptionsMenuEnabled;
    }

    @Override
    public boolean isOptionsMenuEnabled() {
        return this.mOptionsMenuEnabled;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu pMenu, final int pMenuIdOffset, final MapView pMapView) {
        pMenu.add(0, MENU_MY_LOCATION + pMenuIdOffset, Menu.NONE, mResourceProxy.getString(R.string.my_location))
                .setIcon(mResourceProxy.getDrawable(ResourceProxy.ic_menu_mylocation)).setCheckable(true);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu pMenu, final int pMenuIdOffset, final MapView pMapView) {
        pMenu.findItem(MENU_MY_LOCATION + pMenuIdOffset).setChecked(this.isMyLocationEnabled());
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem pItem, final int pMenuIdOffset, final MapView pMapView) {
        final int menuId = pItem.getItemId() - pMenuIdOffset;
        if (menuId == MENU_MY_LOCATION) {
            if (this.isMyLocationEnabled()) {
                this.disableFollowLocation();
                this.disableMyLocation();
            } else {
                this.enableFollowLocation();
                this.enableMyLocation(this.getMyLocationProvider());
            }
            return true;
        } else {
            return false;
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Return a GeoPoint of the last known location, or null if not known.
     */
    public GeoPoint getMyLocation() {
        if (mLocation == null) {
            return null;
        } else {
            return new GeoPoint(mLocation);
        }
    }

    public Location getLastFix() {
        return mLocation;
    }

    /**
     * Enables "follow" functionality. The map will center on your current location and automatically scroll as you move. Scrolling the map in the UI
     * will disable.
     */
    public void enableFollowLocation() {
        mIsFollowing = true;

        // set initial location when enabled
        if (isMyLocationEnabled()) {
            mLocation = mMyLocationProvider.getLastKnownLocation();
            if (mLocation != null) {
                TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(), MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
                final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
                mMapCoords.offset(-worldSize_2, -worldSize_2);
                // mMapController.animateTo(new GeoPoint(mLocation));
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    /**
     * Disables "follow" functionality.
     */
    public void disableFollowLocation() {
        mIsFollowing = false;
    }

    /**
     * If enabled, the map will center on your current location and automatically scroll as you move. Scrolling the map in the UI will disable.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isFollowLocationEnabled() {
        return mIsFollowing;
    }

    public void onLocationChanged(Location location, IMyLocationProvider source) {
        lastLocation = location;
        if (route != null) {
            route.isTooFarFromRoute(location, SMRoute.MAX_DISTANCE_FROM_PATH);
            location = getSnappedLocation(location, route);
        }
        // If we had a previous location, let's get those bounds
        Location oldLocation = mLocation;
        if (oldLocation != null) {
            this.getMyLocationDrawingBounds(mMapView.getZoomLevelFloor(), oldLocation, mMyLocationPreviousRect);
        }
        mLocation = location;
        mMapCoords.set(0, 0);
        if (mLocation != null) {
            TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(), MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
            final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
            mMapCoords.offset(-worldSize_2, -worldSize_2);
            if (mMapView.tracking) {
                // this is done in the navigation fragment :
                // mMapController.animateTo(mLocation.getLatitude(), mLocation.getLongitude());
                if (navigationFragment != null) {
                    // LOG.d("animating the map from the location overlay");
                    navigationFragment.rotateMap();
                    navigationFragment.animateMap(location);
                } else if (mapFragment != null) {
                    mapFragment.animateMap(location);
                }
            } else {
                // Get new drawing bounds
                this.getMyLocationDrawingBounds(mMapView.getZoomLevelFloor(), mLocation, mMyLocationRect);
                // If we had a previous location, merge in those bounds too
                if (oldLocation != null) {
                    mMyLocationRect.union(mMyLocationPreviousRect);
                }
                // Invalidate the bounds
                mMapView.post(new Runnable() {
                    @Override
                    public void run() {
                        mMapView.invalidateMapCoordinates(mMyLocationRect);
                    }
                });
                // mMapView.postInvalidate(mMyLocationRect.left, mMyLocationRect.top, mMyLocationRect.right, mMyLocationRect.bottom);
            }
        }
        for (final Runnable runnable : mRunOnFirstFix) {
            new Thread(runnable).start();
        }
        mRunOnFirstFix.clear();
    }

    private Location getSnappedLocation(Location location, SMRoute route) {
        Location ret = location;
        int start = route.lastVisitedWaypointIndex;
        if (start < 0) {
            start = 0;
        }
        if (route.waypoints != null && start < route.waypoints.size()) {
            int count = 0;
            int indexFound = -1;
            double minD = Double.MAX_VALUE;
            for (int i = start; count < 6 && i < route.waypoints.size() - 1; i++, count++) {
                double d = SMGPSUtil.distanceFromLineInMeters(location, route.waypoints.get(i), route.waypoints.get(i + 1));
                if (d < minD) {
                    minD = d;
                    indexFound = i;
                }
            }
            // if (minD < maxD && indexFound > -1) {
            if (indexFound >= 0 && indexFound + 1 < route.waypoints.size()) {
                ret = SMGPSUtil.closestCoordinate(location, route.waypoints.get(indexFound), route.waypoints.get(indexFound + 1));
            }
        }
        return ret;
    }

    /**
     * Enable receiving location updates from the provided IMyLocationProvider and show your location on the maps. You will likely want to call
     * enableMyLocation() from your Activity's Activity.onResume() method, to enable the features of this overlay. Remember to call the corresponding
     * disableMyLocation() in your Activity's Activity.onPause() method to turn off updates when in the background.
     */
    public boolean enableMyLocation(IMyLocationProvider myLocationProvider) {
        boolean result = true;
        if (myLocationProvider == null)
            throw new RuntimeException("You must pass an IMyLocationProvider to enableMyLocation()");
        this.setMyLocationProvider(myLocationProvider);
        result = mMyLocationProvider.startLocationProvider(this);
        mIsLocationEnabled = result;
        // set initial location when enabled
        if (result && isFollowLocationEnabled()) {
            mLocation = mMyLocationProvider.getLastKnownLocation();
            if (mLocation != null) {
                TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(), MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
                final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
                mMapCoords.offset(-worldSize_2, -worldSize_2);
                if (navigationFragment != null) {
                    navigationFragment.animateMap(mLocation);
                } else if (mapFragment != null) {
                    mapFragment.animateMap(mLocation);
                }
            }
        }
        // LOG.d("invalidating the map from the location change ");
        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
        return result;
    }

    /**
     * Disable location updates
     */
    public void disableMyLocation() {
        mIsLocationEnabled = false;

        if (mMyLocationProvider != null) {
            mMyLocationProvider.stopLocationProvider();
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    /**
     * If enabled, the map is receiving location updates and drawing your location on the map.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isMyLocationEnabled() {
        return mIsLocationEnabled;
    }

    public boolean runOnFirstFix(final Runnable runnable) {
        if (mMyLocationProvider != null && mLocation != null) {
            new Thread(runnable).start();
            return true;
        } else {
            mRunOnFirstFix.addLast(runnable);
            return false;
        }
    }

    public void setRoute(SMRoute route) {
        this.route = route;
    }

    public void setMapFragment(SMRouteNavigationMapFragment smRouteNavigationMapFragment) {
        this.navigationFragment = smRouteNavigationMapFragment;

    }

    public void setMapFragment(SMMapFragment mapFragment) {
        this.mapFragment = mapFragment;
    }

}
