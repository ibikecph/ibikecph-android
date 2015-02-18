// Created by plusminus on 17:45:56 - 25.09.2008
package org.osmdroid.views;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import metalev.multitouch.controller.MultiTouchController;
import metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import metalev.multitouch.controller.MultiTouchController.PointInfo;
import metalev.multitouch.controller.MultiTouchController.PositionAndScale;
import microsoft.mappoint.TileSystem;
import net.wigle.wigleandroid.ZoomButtonsController;
import net.wigle.wigleandroid.ZoomButtonsController.OnZoomListener;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapView;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.IStyledTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler;
import org.osmdroid.tileprovider.util.TileDownloadListener;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.GeometryMath;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.util.constants.MapViewConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Scroller;

@SuppressLint("Recycle")
public class MapView extends ViewGroup implements IMapView, MapViewConstants, MultiTouchObjectCanvas<Object>, TileDownloadListener {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final Logger logger = LoggerFactory.getLogger(MapView.class);

    private static final double ZOOM_SENSITIVITY = 1.0;
    private static final double ZOOM_LOG_BASE_INV = 1.0 / Math.log(2.0 / ZOOM_SENSITIVITY);
    private static Method sMotionEventTransformMethod;

    // ===========================================================
    // Fields
    // ===========================================================

    /** Current zoom level for map tiles. */
    public float mZoomLevel = 0;

    private final OverlayManager mOverlayManager;

    private Projection mProjection;

    private final TilesOverlay mMapOverlay;

    private final GestureDetector mGestureDetector;

    /** Handles map scrolling */
    private final Scroller mScroller;
    private final AtomicInteger mTargetZoomLevel = new AtomicInteger();
    private final AtomicBoolean mIsAnimating = new AtomicBoolean(false);

    private final ScaleAnimation mZoomInAnimation;
    private final ScaleAnimation mZoomOutAnimation;

    protected Integer mMinimumZoomLevel;
    protected Integer mMaximumZoomLevel;

    private final MapController mController;

    // XXX we can use android.widget.ZoomButtonsController if we upgrade the
    // dependency to Android 1.6
    private final ZoomButtonsController mZoomController;
    private boolean mEnableZoomController = false;

    private final ResourceProxy mResourceProxy;

    private MultiTouchController<Object> mMultiTouchController;
    private float mMultiTouchScale = 1.0f;
    private PointF mMultiTouchScalePoint = new PointF();

    protected MapListener mListener;

    // For rotation
    private float mapOrientation = 0;
    private final Matrix mRotateMatrix = new Matrix();
    private final float[] mRotatePoints = new float[2];
    private final Rect mInvalidateRect = new Rect();

    protected BoundingBoxE6 mScrollableAreaBoundingBox;
    protected Rect mScrollableAreaLimit;

    private final MapTileProviderBase mTileProvider;

    private final Handler mTileRequestCompleteHandler;

    /* a point that will be reused to design added views */
    private final Point mPoint = new Point();

    public float scaleDiffFloat = 1f; // 0f
    public boolean isPinchZooming = false;
    public Context context;
    public float zoomLevelPinch;
    float dens;
    // ===========================================================
    // Constructors
    // ===========================================================

    float lastScale = 1f;
    // TileDownloadQueue tileDownloadQueue;
    boolean zoomSetFromPinch = false;
    public boolean tracking = false;
    public Location lastLocation;
    public boolean directionShown = false;
    boolean noRendering = false;
    public boolean isNavigation = false;
    public int locationArrowOffsset = -1;

    protected MapView(final Context context, final int tileSizePixels, final ResourceProxy resourceProxy, MapTileProviderBase tileProvider,
            final Handler tileRequestCompleteHandler, final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dens = metrics.density;

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            float spanStart;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isPinchZooming = true;
                spanStart = detector.getCurrentSpan();
                return super.onScaleBegin(detector);
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoomSetFromPinch = false;
                float scaleDiff = Math.abs(detector.getCurrentSpan() - spanStart);
                float minScaleOffsset = dens * 15f;

                if (scaleDiff >= minScaleOffsset && scaling) {
                    if (getZoomLevel() < (float) getMaxZoomLevel() && detector.getCurrentSpan() - spanStart > 0) {
                        zoomSetFromPinch = true;
                        center = getProjection().fromPixels(detector.getFocusX(), detector.getFocusY());
                        setZoomLevel(getZoomLevel() + 0.5f);
                        // if (getZoomLevel() - getZoomLevelFloor() ==
                        // 0.5f) {
                        // }
                        spanStart = detector.getCurrentSpan();
                        if (getMapOrientation() == 0f && zoomSetFromPinch) {
                            // setMapCenter(center);
                            // getController().animateTo(center);
                        }
                    } else if (getZoomLevel() > (float) getMinZoomLevel()) {
                        zoomSetFromPinch = true;
                        center = getProjection().fromPixels(detector.getFocusX(), detector.getFocusY());
                        setZoomLevel(getZoomLevel() - 0.5f);
                        // if (getZoomLevel() - getZoomLevelFloor() ==
                        // 0.5f) {
                        // }
                        spanStart = detector.getCurrentSpan();
                    } else {
                        spanStart = detector.getCurrentSpan();
                    }

                }

                return zoomSetFromPinch;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isPinchZooming = false;
                super.onScaleEnd(detector);
            }

        });

        mResourceProxy = resourceProxy;
        this.mController = new MapController(this);
        this.mScroller = new Scroller(context);
        TileSystem.setTileSize(tileSizePixels);

        if (tileProvider == null) {
            final ITileSource tileSource = getTileSourceFromAttributes(attrs);
            tileProvider = new MapTileProviderBasic(context, tileSource);
        }

        mTileRequestCompleteHandler = tileRequestCompleteHandler == null ? new SimpleInvalidationHandler(this) : tileRequestCompleteHandler;
        mTileProvider = tileProvider;
        mTileProvider.setTileRequestCompleteHandler(mTileRequestCompleteHandler);

        this.mMapOverlay = new TilesOverlay(mTileProvider, mResourceProxy);
        mOverlayManager = new OverlayManager(mMapOverlay, this);

        this.mZoomController = new ZoomButtonsController(this);
        this.mZoomController.setOnZoomListener(new MapViewZoomListener());

        mZoomInAnimation = new ScaleAnimation(1, 2, 1, 2, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mZoomOutAnimation = new ScaleAnimation(1, 0.5f, 1, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mZoomInAnimation.setDuration(ANIMATION_DURATION_SHORT);
        mZoomOutAnimation.setDuration(ANIMATION_DURATION_SHORT);

        mGestureDetector = new GestureDetector(context, new MapViewGestureDetectorListener());
        mGestureDetector.setOnDoubleTapListener(new MapViewDoubleClickListener());

        // tileDownloadQueue = new TileDownloadQueue(((MapTileProviderBasic)
        // mTileProvider).downloaderProvider.mFilesystemCache,
        // (SimpleInvalidationHandler) mTileRequestCompleteHandler, MapTileProviderBase.mTileCache, mTileProvider);

    }

    /**
     * Constructor used by XML layout resource (uses default tile source).
     */
    public MapView(final Context context, final AttributeSet attrs) {
        this(context, 256, new ResourceProxy(context), null, null, attrs);
    }

    /**
     * Standard Constructor.
     */
    public MapView(final Context context, final int tileSizePixels) {
        this(context, tileSizePixels, new ResourceProxy(context));
    }

    public MapView(final Context context, final int tileSizePixels, final ResourceProxy resourceProxy) {
        this(context, tileSizePixels, resourceProxy, null);
    }

    public MapView(final Context context, final int tileSizePixels, final ResourceProxy resourceProxy, final MapTileProviderBase aTileProvider) {
        this(context, tileSizePixels, resourceProxy, aTileProvider, null);
    }

    public MapView(final Context context, final int tileSizePixels, final ResourceProxy resourceProxy, final MapTileProviderBase aTileProvider,
            final Handler tileRequestCompleteHandler) {
        this(context, tileSizePixels, resourceProxy, aTileProvider, tileRequestCompleteHandler, null);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    @Override
    public MapController getController() {
        return this.mController;
    }

    /**
     * You can add/remove/reorder your Overlays using the List of {@link Overlay}. The first (index 0) Overlay gets drawn first, the one with the
     * highest as the last one.
     */
    public List<Overlay> getOverlays() {
        return this.getOverlayManager();
    }

    public OverlayManager getOverlayManager() {
        return mOverlayManager;
    }

    public MapTileProviderBase getTileProvider() {
        return mTileProvider;
    }

    public Scroller getScroller() {
        return mScroller;
    }

    public Handler getTileRequestCompleteHandler() {
        return mTileRequestCompleteHandler;
    }

    @Override
    public int getLatitudeSpan() {
        return this.getBoundingBox().getLatitudeSpanE6();
    }

    @Override
    public int getLongitudeSpan() {
        return this.getBoundingBox().getLongitudeSpanE6();
    }

    public BoundingBoxE6 getBoundingBox() {
        return getBoundingBox(getWidth(), getHeight());
    }

    public BoundingBoxE6 getBoundingBox(final int pViewWidth, final int pViewHeight) {

        // final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
        final int world_2 = TileSystem.MapSize(getZoomLevelFloor()) / 2;
        final Rect screenRect = getScreenRect(null);
        screenRect.offset(world_2, world_2);

        final IGeoPoint neGeoPoint = TileSystem.PixelXYToLatLong(screenRect.right, screenRect.top, getZoomLevelFloor(), null);
        final IGeoPoint swGeoPoint = TileSystem.PixelXYToLatLong(screenRect.left, screenRect.bottom, getZoomLevelFloor(), null);

        return new BoundingBoxE6(neGeoPoint.getLatitudeE6(), neGeoPoint.getLongitudeE6(), swGeoPoint.getLatitudeE6(), swGeoPoint.getLongitudeE6());
    }

    public BoundingBoxE6 getBoundingBox(int zoomLevel) {
        return getBoundingBox(getWidth(), getHeight(), zoomLevel);
    }

    public BoundingBoxE6 getBoundingBox(final int pViewWidth, final int pViewHeight, final int zoomLevel) {

        final int world_2 = TileSystem.MapSize(zoomLevel) / 2;
        final Rect screenRect = getScreenRect(null);
        screenRect.offset(world_2, world_2);

        final IGeoPoint neGeoPoint = TileSystem.PixelXYToLatLong(screenRect.right, screenRect.top, zoomLevel, null);
        final IGeoPoint swGeoPoint = TileSystem.PixelXYToLatLong(screenRect.left, screenRect.bottom, zoomLevel, null);

        return new BoundingBoxE6(neGeoPoint.getLatitudeE6(), neGeoPoint.getLongitudeE6(), swGeoPoint.getLatitudeE6(), swGeoPoint.getLongitudeE6());
    }

    /**
     * Gets the current bounds of the screen in <I>screen coordinates</I>.
     */
    public Rect getScreenRect(final Rect reuse) {
        final Rect out = getIntrinsicScreenRect(reuse);
        if (this.getMapOrientation() != 0 && this.getMapOrientation() != 180) {
            // Since the canvas is shifted by getWidth/2, we can just return our
            // natural scrollX/Y
            // value since that is the same as the shifted center.
            int centerX = this.getScrollX();
            int centerY = this.getScrollY();
            GeometryMath.getBoundingBoxForRotatatedRectangle(out, centerX, centerY, this.getMapOrientation(), out);
        }
        return out;
    }

    public Rect getIntrinsicScreenRect(final Rect reuse) {
        final Rect out = reuse == null ? new Rect() : reuse;
        out.set(getScrollX() - getWidth() / 2, getScrollY() - getHeight() / 2, getScrollX() + getWidth() / 2, getScrollY() + getHeight() / 2);
        return out;
    }

    /**
     * Get a projection for converting between screen-pixel coordinates and latitude/longitude coordinates. You should not hold on to this object for
     * more than one draw, since the projection of the map could change.
     * 
     * @return The Projection of the map in its current state. You should not hold on to this object for more than one draw, since the projection of
     *         the map could change.
     */
    @Override
    public Projection getProjection() {
        if (mProjection == null) {
            mProjection = new Projection(this);
        }
        return mProjection;
    }

    void setMapCenter(final IGeoPoint aCenter) {
        this.setMapCenter(aCenter.getLatitudeE6(), aCenter.getLongitudeE6());
    }

    void setMapCenter(final int aLatitudeE6, final int aLongitudeE6) {
        final Point coords = TileSystem.LatLongToPixelXY(aLatitudeE6 / 1E6, aLongitudeE6 / 1E6, getZoomLevelFloor(), null);
        final int worldSize_2 = TileSystem.MapSize(getZoomLevelFloor()) / 2;
        if (getAnimation() == null || getAnimation().hasEnded()) {
            mScroller.startScroll(getScrollX(), getScrollY(), coords.x - worldSize_2 - getScrollX(), coords.y - worldSize_2 - getScrollY(), 500);
            postInvalidate();
        }
    }

    public void setTileSource(final ITileSource aTileSource) {
        mTileProvider.setTileSource(aTileSource);
        // tileDownloadQueue.setTileSource((XYTileSource) mTileProvider.getTileSource());// (((MapTileProviderBasic)
        // mTileProvider).downloaderProvider.mTileSource);
        TileSystem.setTileSize(aTileSource.getTileSizePixels());
        this.checkZoomButtons();
        this.setZoomLevel(mZoomLevel); // revalidate zoom level
        postInvalidate();
    }

    /**
     * @param aZoomLevel
     *            the zoom level bound by the tile source
     */
    float setZoomLevel(final float aZoomLevel) {
        return setZoomLevel(aZoomLevel, null);
    }

    float setZoomLevel(final float aZoomLevel, IGeoPoint gp) {
        // tileDownloadQueue.interrupt();
        final int minZoomLevel = getMinZoomLevel();
        final int maxZoomLevel = (int) getMaxZoomLevel();

        final float newZoomLevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, aZoomLevel));
        final int newZoomLevelFloor = (int) newZoomLevel;
        final int curZoomLevelFloor = (int) mZoomLevel;

        if (newZoomLevel != mZoomLevel) {
            mScroller.forceFinished(true);
        }

        this.mZoomLevel = newZoomLevel;
        this.zoomLevelPinch = (int) this.mZoomLevel;
        this.checkZoomButtons();

        // logger.debug("new zoom level = " + this.mZoomLevel);

        // MapTile.mapZoom = (int) newZoomLevel;

        if (newZoomLevelFloor > curZoomLevelFloor) {
            // We are going from a lower-resolution plane to a higher-resolution
            // plane, so we have
            // to do it the hard way.
            final int worldSize_current_2 = TileSystem.MapSize(curZoomLevelFloor) / 2;
            final int worldSize_new_2 = TileSystem.MapSize(newZoomLevelFloor) / 2;
            final IGeoPoint centerGeoPoint = gp != null ? gp : TileSystem.PixelXYToLatLong(getScrollX() + worldSize_current_2, getScrollY()
                    + worldSize_current_2, curZoomLevelFloor, null);
            // final IGeoPoint centerGeoPoint =
            // TileSystem.PixelXYToLatLong(getScrollX() + worldSize_current_2,
            // getScrollY()
            // + worldSize_current_2, curZoomLevelFloor, null);
            final Point centerPoint = TileSystem.LatLongToPixelXY(centerGeoPoint.getLatitudeE6() * 1E-6, centerGeoPoint.getLongitudeE6() * 1E-6,
                    newZoomLevelFloor, null);
            scrollTo(false, centerPoint.x - worldSize_new_2, centerPoint.y - worldSize_new_2);
        } else if (newZoomLevelFloor < curZoomLevelFloor) {
            // We are going from a higher-resolution plane to a lower-resolution
            // plane, so we can do
            // it the easy way.
            scrollTo(false, getScrollX() >> curZoomLevelFloor - newZoomLevelFloor, getScrollY() >> curZoomLevelFloor - newZoomLevelFloor);
        }

        zoomLevelPinch = mZoomLevel;

        // snap for all snappables
        final Point snapPoint = new Point();
        mProjection = new Projection(this);
        if (this.getOverlayManager().onSnapToItem(getScrollX(), getScrollY(), snapPoint, this)) {
            scrollTo(snapPoint.x, snapPoint.y);
        }

        if (newZoomLevelFloor > curZoomLevelFloor)
            mTileProvider.rescaleCache(newZoomLevelFloor, curZoomLevelFloor, getScreenRect(null), false);

        // do callback on listener
        if (newZoomLevel != curZoomLevelFloor && mListener != null) {
            final ZoomEvent event = new ZoomEvent(this, newZoomLevel);
            mListener.onZoom(event);
        }
        // Allows any views fixed to a Location in the MapView to adjust
        this.requestLayout();

        // handler.postDelayed(new Runnable() {
        //
        // @Override
        // public void run() {
        // logger.debug("download tiles from setZoom aZoom = " + aZoomLevel + " getZoom = " + getZoomLevel());
        // if (aZoomLevel == getZoomLevel()) {
        // downloadTiles();
        // }
        //
        // }
        // }, 500);

        // mMultiTouchScale = 1 + Math.abs(newZoomLevelFloor - newZoomLevel);
        return getZoomLevel();
    }

    /**
     * Zoom the map to enclose the specified bounding box, as closely as possible. Must be called after display layout is complete, or screen
     * dimensions are not known, and will always zoom to center of zoom level 0. Suggestion: Check getScreenRect(null).getHeight() > 0
     */
    public void zoomToBoundingBox(final BoundingBoxE6 boundingBox) {
        final BoundingBoxE6 currentBox = getBoundingBox();
        int currentBoxLatSpan = currentBox.getLatitudeSpanE6();
        // Calculated required zoom based on latitude span
        double maxZoomLatitudeSpan;
        if (mZoomLevel == getMaxZoomLevel()) {
            maxZoomLatitudeSpan = currentBox.getLatitudeSpanE6();
        } else {
            int zoomDiff = (int) (getMaxZoomLevel() - mZoomLevel);
            int factor = (int) Math.pow(2, zoomDiff);
            maxZoomLatitudeSpan = currentBoxLatSpan / factor;
        }

        double latitudeRatio = boundingBox.getLatitudeSpanE6() / maxZoomLatitudeSpan;
        double latitudeRatioLog = Math.log(latitudeRatio) / Math.log(2);
        double latitudeCeil = Math.ceil(latitudeRatioLog);
        double requiredLatitudeZoom = getMaxZoomLevel() - latitudeCeil;
        if (latitudeCeil >= 5d) {
            requiredLatitudeZoom--;
        }

        // Calculated required zoom based on longitude span
        final double maxZoomLongitudeSpan = mZoomLevel == getMaxZoomLevel() ? currentBox.getLongitudeSpanE6() : currentBox.getLongitudeSpanE6()
                / Math.pow(2, getMaxZoomLevel() - mZoomLevel);

        final double requiredLongitudeZoom = getMaxZoomLevel()
                - Math.ceil(Math.log(boundingBox.getLongitudeSpanE6() / maxZoomLongitudeSpan) / Math.log(2));

        // Zoom to boundingBox center, at calculated maximum allowed zoom level
        getController().setZoom((int) (requiredLatitudeZoom < requiredLongitudeZoom ? requiredLatitudeZoom : requiredLongitudeZoom));

        getController().setCenter(
                new GeoPoint(boundingBox.getCenter().getLatitudeE6() / 1000000.0, boundingBox.getCenter().getLongitudeE6() / 1000000.0));
    }

    /**
     * Get the current ZoomLevel for the map tiles.
     * 
     * @return the current ZoomLevel between 0 (equator) and 18/19(closest), depending on the tile source chosen.
     */
    @Override
    public float getZoomLevel() {
        return mZoomLevel;
    }

    public int getZoomLevelFloor() {
        return (int) mZoomLevel;
    }

    /**
     * Get the minimum allowed zoom level for the maps.
     */
    public int getMinZoomLevel() {
        return mMinimumZoomLevel == null ? mMapOverlay.getMinimumZoomLevel() : mMinimumZoomLevel;
    }

    /**
     * Get the maximum allowed zoom level for the maps.
     */
    @Override
    public float getMaxZoomLevel() {
        return mMaximumZoomLevel == null ? mMapOverlay.getMaximumZoomLevel() : mMaximumZoomLevel;
    }

    /**
     * Set the minimum allowed zoom level, or pass null to use the minimum zoom level from the tile provider.
     */
    public void setMinZoomLevel(Integer zoomLevel) {
        mMinimumZoomLevel = zoomLevel;
    }

    /**
     * Set the maximum allowed zoom level, or pass null to use the maximum zoom level from the tile provider.
     */
    public void setMaxZoomLevel(Integer zoomLevel) {
        mMaximumZoomLevel = zoomLevel;
    }

    public boolean canZoomIn() {
        final int maxZoomLevel = (int) getMaxZoomLevel();
        if (mZoomLevel >= maxZoomLevel) {
            return false;
        }
        if (mIsAnimating.get() & mTargetZoomLevel.get() >= maxZoomLevel) {
            return false;
        }
        return true;
    }

    public boolean canZoomOut() {
        final int minZoomLevel = getMinZoomLevel();
        if (mZoomLevel <= minZoomLevel) {
            return false;
        }
        if (mIsAnimating.get() && mTargetZoomLevel.get() <= minZoomLevel) {
            return false;
        }
        return true;
    }

    /**
     * Zoom in by one zoom level.
     */
    boolean zoomIn() {

        if (canZoomIn()) {
            if (mIsAnimating.get()) {
                // TODO extend zoom (and return true)
                return false;
            } else {
                // TODO - maybe go an extra level if already close to target
                // zoom level?
                mTargetZoomLevel.set(getZoomLevelFloor() + 1);
                mIsAnimating.set(true);
                startAnimation(mZoomInAnimation);
                return true;
            }
        } else {
            return false;
        }
    }

    boolean zoomInFixing(final IGeoPoint point) {
        setMapCenter(point); // TODO should fix on point, not center on it
        return zoomIn();
    }

    boolean zoomInFixing(final int xPixel, final int yPixel) {
        setMapCenter(xPixel, yPixel); // TODO should fix on point, not center on
                                      // it
        return zoomIn();
    }

    /**
     * Zoom out by one zoom level.
     */
    boolean zoomOut() {

        if (canZoomOut()) {
            if (mIsAnimating.get()) {
                // TODO extend zoom (and return true)
                return false;
            } else {
                // TODO - maybe go an extra level if already close to target
                // zoom level?
                mTargetZoomLevel.set(getZoomLevelFloor() - 1);
                mIsAnimating.set(true);
                startAnimation(mZoomOutAnimation);
                return true;
            }
        } else {
            return false;
        }

    }

    final Rect mViewPort = new Rect();
    final Handler handler = new Handler();

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // logger.debug("onScrollChanged " + (l - oldl) + "," + (t - oldt));
        // tileDownloadQueue.interrupt();
        lastScrollTimestamp = Calendar.getInstance().getTimeInMillis();

        // handler.postDelayed(new Runnable() {
        //
        // @Override
        // public void run() {
        // if (Calendar.getInstance().getTimeInMillis() - lastScrollTimestamp >= 400
        // || tileDownloadQueue.getZoomLevel() != getZoomLevel()) {
        // // logger.debug("scroll finished");
        // // if (mTileProvider instanceof MapTileProviderBasic) {
        //
        // downloadTiles();
        // // }
        // }
        //
        // }
        // }, 500);
    }

    Rect viewPort = new Rect();

    // private void downloadTiles() {
    // // Calculate the half-world size
    // final Projection pj = getProjection();
    // int mWorldSize_2 = TileSystem.MapSize(getZoomLevelFloor()) >> 1;
    // // Get the area we are drawing to
    // viewPort.set(pj.getScreenRect());
    // // Translate the Canvas coordinates into Mercator coordinates
    // viewPort.offset(mWorldSize_2, mWorldSize_2);
    // if (!(viewPort.left == viewPort.right || viewPort.top == viewPort.bottom)) {
    // // tileDownloadQueue.downloadTiles(getZoomLevel(), viewPort);
    // }
    // }

    boolean zoomOutFixing(final IGeoPoint point) {
        setMapCenter(point); // TODO should fix on point, not center on it
        return zoomOut();
    }

    boolean zoomOutFixing(final int xPixel, final int yPixel) {
        setMapCenter(xPixel, yPixel); // TODO should fix on point, not center on
                                      // it
        return zoomOut();
    }

    /**
     * Returns the current center-point position of the map, as a GeoPoint (latitude and longitude).
     * 
     * @return A GeoPoint of the map's center-point.
     */
    @Override
    public IGeoPoint getMapCenter() {
        final int world_2 = TileSystem.MapSize(getZoomLevelFloor()) / 2;
        final Rect screenRect = getScreenRect(null);
        screenRect.offset(world_2, world_2);
        return TileSystem.PixelXYToLatLong(screenRect.centerX(), screenRect.centerY(), getZoomLevelFloor(), null);
    }

    public ResourceProxy getResourceProxy() {
        return mResourceProxy;
    }

    public void setMapOrientation(float degrees) {
        this.mapOrientation = degrees % 360.0f;
        this.invalidate();
    }

    public float getMapOrientation() {
        return mapOrientation;
    }

    /**
     * Whether to use the network connection if it's available.
     */
    public boolean useDataConnection() {
        return mMapOverlay.useDataConnection();
    }

    /**
     * Set whether to use the network connection if it's available.
     * 
     * @param aMode
     *            if true use the network connection if it's available. if false don't use the network connection even if it's available.
     */
    public void setUseDataConnection(final boolean aMode) {
        mMapOverlay.setUseDataConnection(aMode);
    }

    /**
     * Set the map to limit it's scrollable view to the specified BoundingBoxE6. Note this does not limit zooming so it will be possible for the user
     * to zoom to an area that is larger than the limited area.
     * 
     * @param boundingBox
     *            A lat/long bounding box to limit scrolling to, or null to remove any scrolling limitations
     */
    public void setScrollableAreaLimit(BoundingBoxE6 boundingBox) {
        final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;

        mScrollableAreaBoundingBox = boundingBox;

        // Clear scrollable area limit if null passed.
        if (boundingBox == null) {
            mScrollableAreaLimit = null;
            return;
        }

        // Get NW/upper-left
        final Point upperLeft = TileSystem.LatLongToPixelXY(boundingBox.getLatNorthE6() / 1E6, boundingBox.getLonWestE6() / 1E6,
                MapViewConstants.MAXIMUM_ZOOMLEVEL, null);
        upperLeft.offset(-worldSize_2, -worldSize_2);

        // Get SE/lower-right
        final Point lowerRight = TileSystem.LatLongToPixelXY(boundingBox.getLatSouthE6() / 1E6, boundingBox.getLonEastE6() / 1E6,
                MapViewConstants.MAXIMUM_ZOOMLEVEL, null);
        lowerRight.offset(-worldSize_2, -worldSize_2);
        mScrollableAreaLimit = new Rect(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
    }

    public BoundingBoxE6 getScrollableAreaLimit() {
        return mScrollableAreaBoundingBox;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    public void invalidateMapCoordinates(Rect dirty) {
        mInvalidateRect.set(dirty);
        final int width_2 = this.getWidth() / 2;
        final int height_2 = this.getHeight() / 2;

        // Since the canvas is shifted by getWidth/2, we can just return our
        // natural scrollX/Y value
        // since that is the same as the shifted center.
        int centerX = this.getScrollX();
        int centerY = this.getScrollY();

        if (this.getMapOrientation() != 0)
            GeometryMath.getBoundingBoxForRotatatedRectangle(mInvalidateRect, centerX, centerY, this.getMapOrientation() + 180, mInvalidateRect);
        mInvalidateRect.offset(width_2, height_2);

        super.invalidate(mInvalidateRect);
    }

    /**
     * Returns a set of layout parameters with a width of {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}, a height of
     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} at the {@link GeoPoint} (0, 0) align with {@link MapView.LayoutParams#BOTTOM_CENTER}.
     */
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null,
                MapView.LayoutParams.BOTTOM_CENTER, 0, 0);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new MapView.LayoutParams(getContext(), attrs);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof MapView.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
        return new MapView.LayoutParams(p);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Find rightmost and bottom-most child
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
                final int childHeight = child.getMeasuredHeight();
                final int childWidth = child.getMeasuredWidth();
                getProjection().toMapPixels(lp.geoPoint, mPoint);
                final int x = mPoint.x + getWidth() / 2;
                final int y = mPoint.y + getHeight() / 2;
                int childRight = x;
                int childBottom = y;
                switch (lp.alignment) {
                    case MapView.LayoutParams.TOP_LEFT:
                        childRight = x + childWidth;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.TOP_CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.TOP_RIGHT:
                        childRight = x;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.CENTER_LEFT:
                        childRight = x + childWidth;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER_RIGHT:
                        childRight = x;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.BOTTOM_LEFT:
                        childRight = x + childWidth;
                        childBottom = y + childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y + childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_RIGHT:
                        childRight = x;
                        childBottom = y + childHeight;
                        break;
                }
                childRight += lp.offsetX;
                childBottom += lp.offsetY;

                maxWidth = Math.max(maxWidth, childRight);
                maxHeight = Math.max(maxHeight, childBottom);
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
                final int childHeight = child.getMeasuredHeight();
                final int childWidth = child.getMeasuredWidth();
                getProjection().toMapPixels(lp.geoPoint, mPoint);
                final int x = mPoint.x + getWidth() / 2;
                final int y = mPoint.y + getHeight() / 2;
                int childLeft = x;
                int childTop = y;
                switch (lp.alignment) {
                    case MapView.LayoutParams.TOP_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.TOP_CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.TOP_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.CENTER_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.BOTTOM_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                }
                childLeft += lp.offsetX;
                childTop += lp.offsetY;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }
        }
    }

    public void onDetach() {
        this.getOverlayManager().onDetach(this);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final boolean result = this.getOverlayManager().onKeyDown(keyCode, event, this);

        return result || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        final boolean result = this.getOverlayManager().onKeyUp(keyCode, event, this);

        return result || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(final MotionEvent event) {

        logger.debug("MapView onTrackballEvent");

        if (this.getOverlayManager().onTrackballEvent(event, this)) {
            return true;
        }

        scrollBy((int) (event.getX() * 25), (int) (event.getY() * 25));

        return super.onTrackballEvent(event);
    }

    ScaleGestureDetector scaleDetector;
    public MotionEvent lastEv, lastDoubleFingerEvent;
    float dPrev = 0f;
    boolean scaling = true;

    float twoPointerDownDistance = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1 && (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)) {
            lastDoubleFingerEvent = MotionEvent.obtain(event);
        }
        return super.onTouchEvent(event);
    };

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {

        if (mZoomController.isVisible() && mZoomController.onTouch(this, event)) {
            return true;
        }

        // Get rotated event for some touch listeners.
        MotionEvent rotatedEvent = rotateTouchEvent(event);

        try {

            if (((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) && rotatedEvent.getPointerCount() > 1) {
                float x = rotatedEvent.getX(rotatedEvent.getPointerId(0)) - rotatedEvent.getX(rotatedEvent.getPointerId(1));
                float y = rotatedEvent.getY(rotatedEvent.getPointerId(0)) - rotatedEvent.getY(rotatedEvent.getPointerId(1));
                twoPointerDownDistance = (float) Math.sqrt(x * x + y * y);

            }
        } catch (Exception e) {

        }
        try {

            scaling = true;

            if (rotatedEvent.getPointerCount() > 1 && lastEv != null && lastEv.getPointerCount() > 1
                    && rotatedEvent.getAction() != MotionEvent.ACTION_CANCEL && rotatedEvent.getAction() != MotionEvent.ACTION_UP) {
                float x = rotatedEvent.getX(0) - rotatedEvent.getX(1);
                float y = rotatedEvent.getY(0) - rotatedEvent.getY(1);
                float d = (float) Math.sqrt(x * x + y * y);
                if (Math.abs(d - twoPointerDownDistance) < 30) {
                    scaling = false;
                } else if (Math.abs(d - dPrev) > 30) {
                    scaling = true;
                    dPrev = d;
                } else if (Math.abs(d - dPrev) > 200) {
                    // put one finger on the screen, and after that another one
                    scaling = false;
                    dPrev = d;
                } else {
                    scaling = false;
                }
            }
        } catch (Exception e) {

        }

        lastEv = rotatedEvent;

        // scaleDetector.onTouchEvent(rotatedEvent);

        if (super.dispatchTouchEvent(event)) {
            return true;
        }

        if (!scaling && this.getOverlayManager().onTouchEvent(rotatedEvent, this)) {
            return true;
        }

        // scaling &&
        if (mMultiTouchController != null && mMultiTouchController.onTouchEvent(event)) {
            if (mMultiTouchScale > 1.5f && getMapOrientation() != 0) {
                return true;
            }
        }

        if (!(tracking && isPinchZooming)) {
            if (mGestureDetector.onTouchEvent(rotatedEvent)) {
                return true;
            }
        }
        return false;
    }

    private MotionEvent rotateTouchEvent(MotionEvent ev) {
        if (this.getMapOrientation() == 0)
            return ev;

        mRotateMatrix.setRotate(-getMapOrientation(), this.getWidth() / 2, this.getHeight() / 2);

        MotionEvent rotatedEvent = MotionEvent.obtain(ev);
        if (Build.VERSION.SDK_INT < 11) { // Build.VERSION_CODES.HONEYCOMB) {
            mRotatePoints[0] = ev.getX();
            mRotatePoints[1] = ev.getY();
            mRotateMatrix.mapPoints(mRotatePoints);
            rotatedEvent.setLocation(mRotatePoints[0], mRotatePoints[1]);
        } else {
            // This method is preferred since it will rotate historical touch
            // events too
            try {
                if (sMotionEventTransformMethod == null) {
                    sMotionEventTransformMethod = MotionEvent.class.getDeclaredMethod("transform", new Class[] { Matrix.class });
                }
                sMotionEventTransformMethod.invoke(rotatedEvent, mRotateMatrix);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return rotatedEvent;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScroller.isFinished()) {
                // One last scrollTo to get to the final destination
                scrollTo(false, mScroller.getCurrX(), mScroller.getCurrY());
                // This will facilitate snapping-to any Snappable points.
                setZoomLevel(mZoomLevel);
            } else {
                scrollTo(false, mScroller.getCurrX(), mScroller.getCurrY());
            }

            // logger.debug("computeScroll");
            postInvalidate(); // Keep on drawing until the animation has
            // finished.
        }
    }

    long lastScrollTimestamp = 0;

    @Override
    public void scrollTo(int x, int y) {
        scrollTo(true, x, y);
    }

    int lastScrollX = Integer.MAX_VALUE, lastScrollY = Integer.MAX_VALUE;

    public void scrollTo(boolean userAction, int x, int y) {

        if (lastScrollX != Integer.MAX_VALUE && x == lastScrollX && y == lastScrollY) {
            return;
        }

        int worldSize_2 = TileSystem.MapSize(getZoomLevel()) / 2;

        while (x < -worldSize_2) {
            x += worldSize_2 * 2;
        }
        while (x > worldSize_2) {
            x -= worldSize_2 * 2;
        }
        while (y < -worldSize_2) {
            y += worldSize_2 * 2;
        }
        while (y > worldSize_2) {
            y -= worldSize_2 * 2;
        }

        if (mScrollableAreaLimit != null) {
            final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - getZoomLevelFloor();
            int minX = (mScrollableAreaLimit.left >> zoomDiff);
            int minY = (mScrollableAreaLimit.top >> zoomDiff);
            int maxX = (mScrollableAreaLimit.right >> zoomDiff);
            int maxY = (mScrollableAreaLimit.bottom >> zoomDiff);

            final int scrollableWidth = maxX - minX;
            final int scrollableHeight = maxY - minY;
            final int width = this.getWidth();
            final int height = this.getHeight();
            // Adjust if we are outside the scrollable area
            if (scrollableWidth <= width)
                x = minX + (scrollableWidth / 2);
            else if (x - (width / 2) < minX)
                x = minX + (width / 2);
            else if (x + (width / 2) > maxX)
                x = maxX - (width / 2);

            if (scrollableHeight <= height)
                y = minY + (scrollableHeight / 2);
            else if (y - (height / 2) < minY)
                y = minY + (height / 2);
            else if (y + (height / 2) > maxY)
                y = maxY - (height / 2);
        }

        lastScrollX = x;
        lastScrollY = y;

        super.scrollTo(x, y);

        // do callback on listener
        if (mListener != null) {
            final ScrollEvent event = new ScrollEvent(this, x, y);
            event.setUserAction(userAction);
            mListener.onScroll(event);
        }

    }

    @Override
    public void setBackgroundColor(final int pColor) {
        mMapOverlay.setLoadingBackgroundColor(pColor);
        invalidate();
    }

    @SuppressLint("WrongCall")
    @Override
    protected void dispatchDraw(final Canvas c) {

        final long startMs = System.currentTimeMillis();

        mProjection = new Projection(this);

        // Save the current canvas matrix
        c.save();

        c.translate(getWidth() / 2, getHeight() / 2);
        c.scale(mMultiTouchScale, mMultiTouchScale, mMultiTouchScalePoint.x, mMultiTouchScalePoint.y);

        /* rotate Canvas */
        c.rotate(mapOrientation, mProjection.getScreenRect().centerX(), mProjection.getScreenRect().centerY());

        /* Draw background */
        // c.drawColor(mBackgroundColor);

        /* Draw all Overlays. */
        this.getOverlayManager().onDraw(c, this);

        // Restore the canvas matrix
        c.restore();

        super.dispatchDraw(c);

        if (DEBUGMODE) {
            final long endMs = System.currentTimeMillis();
            logger.debug("Rendering overall: " + (endMs - startMs) + "ms");
        }

    }

    /**
     * Returns true if the safe drawing canvas is being used.
     * 
     * @see {@link ISafeCanvas}
     */
    public boolean isUsingSafeCanvas() {
        return this.getOverlayManager().isUsingSafeCanvas();
    }

    /**
     * Sets whether the safe drawing canvas is being used.
     * 
     * @see {@link ISafeCanvas}
     */
    public void setUseSafeCanvas(boolean useSafeCanvas) {
        this.getOverlayManager().setUseSafeCanvas(useSafeCanvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mZoomController.setVisible(false);
        this.onDetach();
        super.onDetachedFromWindow();
    }

    // ===========================================================
    // Animation
    // ===========================================================

    @Override
    protected void onAnimationStart() {
        mIsAnimating.set(true);
        super.onAnimationStart();
    }

    @Override
    protected void onAnimationEnd() {
        mIsAnimating.set(false);
        if ((getAnimation() != null && getAnimation().getClass() != RotateAnimation.class)) {
            clearAnimation();
            setZoomLevel(mTargetZoomLevel.get());
        }
        super.onAnimationEnd();
    }

    /**
     * Check mAnimationListener.isAnimating() to determine if view is animating. Useful for overlays to avoid recalculating during an animation
     * sequence.
     * 
     * @return boolean indicating whether view is animating.
     */
    public boolean isAnimating() {
        return mIsAnimating.get();
    }

    // ===========================================================
    // Implementation of MultiTouchObjectCanvas
    // ===========================================================

    final Matrix m = new Matrix();
    final float[] points = new float[2];

    @Override
    public Object getDraggableObjectAtPoint(final PointInfo pt) {

        points[0] = pt.getX();
        points[1] = pt.getY();

        if (tracking) {
            mMultiTouchScalePoint.x = getWidth() / 2;
            if (isNavigation) {
                mMultiTouchScalePoint.y = getHeight() / 2 + locationArrowOffsset;// 2 * getHeight() / 3;
            } else {
                mMultiTouchScalePoint.y = getHeight() / 2 - locationArrowOffsset;
            }
        } else {
            mMultiTouchScalePoint.x = points[0];
            mMultiTouchScalePoint.y = points[1];
        }
        mMultiTouchScalePoint.x += getScrollX() - (this.getWidth() / 2);
        mMultiTouchScalePoint.y += getScrollY() - (this.getHeight() / 2);

        return this;
    }

    @Override
    public void getPositionAndScale(final Object obj, final PositionAndScale objPosAndScaleOut) {
        objPosAndScaleOut.set(0, 0, true, mMultiTouchScale, false, 0, 0, false, 0);
    }

    IGeoPoint center;
    public long lastPinchTimestamp = System.currentTimeMillis();

    @Override
    public void selectObject(final Object obj, final PointInfo pt) {

        if (obj == null && mMultiTouchScale != 1.0f) {
            final float scaleDiffFloat = (float) (Math.log(mMultiTouchScale) * ZOOM_LOG_BASE_INV);
            final int scaleDiffInt = Math.round(scaleDiffFloat);
            // If we are changing zoom levels,
            // adjust the center point in respect to the scaling point
            if (scaleDiffInt != 0) { // && !directionShown
                Matrix m = new Matrix();
                m.setScale(1 / mMultiTouchScale, 1 / mMultiTouchScale, mMultiTouchScalePoint.x, mMultiTouchScalePoint.y);
                m.postRotate(-mapOrientation, mProjection.getScreenRect().centerX(), mProjection.getScreenRect().centerY());
                float[] pts = new float[2];
                pts[0] = getScrollX();
                pts[1] = getScrollY();
                m.mapPoints(pts);
                scrollTo(false, (int) pts[0], (int) pts[1]);
            }

            setZoomLevel(mZoomLevel + scaleDiffInt);

        }

        // reset scale
        mMultiTouchScale = 1.0f;
        isPinchZooming = false;
        lastPinchTimestamp = System.currentTimeMillis();
    }

    PointInfo lastTouch;

    @Override
    public boolean setPositionAndScale(final Object obj, final PositionAndScale aNewObjPosAndScale, final PointInfo aTouchPoint) {

        if (mListener != null) {
            mListener.onPinchZoom();
        }

        lastTouch = aTouchPoint;

        isPinchZooming = true;

        float multiTouchScale = aNewObjPosAndScale.getScale();
        // If we are at the first or last zoom level, prevent pinching/expanding
        if (multiTouchScale > 1 && !canZoomIn()) {
            multiTouchScale = 1;
        }
        if (multiTouchScale < 1 && !canZoomOut()) {
            multiTouchScale = 1;
        }

        if (aTouchPoint.getNumTouchPoints() < 2) {
            // mMultiTouchScale = 1f;
        } else {
            mMultiTouchScale = multiTouchScale;
        }
        scaleDiffFloat = 1 / mMultiTouchScale;

        invalidate(); // redraw

        return true;
    }

    /*
     * Set the MapListener for this view
     */
    public void setMapListener(final MapListener ml) {
        mListener = ml;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private void checkZoomButtons() {
        this.mZoomController.setZoomInEnabled(canZoomIn());
        this.mZoomController.setZoomOutEnabled(canZoomOut());
    }

    public void setBuiltInZoomControls(final boolean on) {
        this.mEnableZoomController = on;
        this.checkZoomButtons();
    }

    public void setMultiTouchControls(final boolean on) {
        mMultiTouchController = on ? new MultiTouchController<Object>(this, false) : null;
    }

    private ITileSource getTileSourceFromAttributes(final AttributeSet aAttributeSet) {

        ITileSource tileSource = TileSourceFactory.DEFAULT_TILE_SOURCE;

        if (aAttributeSet != null) {
            final String tileSourceAttr = aAttributeSet.getAttributeValue(null, "tilesource");
            if (tileSourceAttr != null) {
                try {
                    final ITileSource r = TileSourceFactory.getTileSource(tileSourceAttr);
                    logger.info("Using tile source specified in layout attributes: " + r);
                    tileSource = r;
                } catch (final IllegalArgumentException e) {
                    logger.warn("Invalid tile souce specified in layout attributes: " + tileSource);
                }
            }
        }

        if (aAttributeSet != null && tileSource instanceof IStyledTileSource) {
            final String style = aAttributeSet.getAttributeValue(null, "style");
            if (style == null) {
                logger.info("Using default style: 1");
            } else {
                logger.info("Using style specified in layout attributes: " + style);
                ((IStyledTileSource<?>) tileSource).setStyle(style);
            }
        }

        logger.info("Using tile source: " + tileSource);
        return tileSource;
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class MapViewGestureDetectorListener implements OnGestureListener {

        @Override
        public boolean onDown(final MotionEvent e) {
            if (MapView.this.getOverlayManager().onDown(e, MapView.this)) {
                return true;
            }

            mZoomController.setVisible(mEnableZoomController);
            return true;
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            if (MapView.this.getOverlayManager().onFling(e1, e2, velocityX, velocityY, MapView.this)) {
                return true;
            }

            final int worldSize = TileSystem.MapSize(getZoomLevelFloor());
            mScroller.fling(getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, -worldSize, worldSize, -worldSize, worldSize);
            return true;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
            if (((mMultiTouchController != null && mMultiTouchController.isPinching()) || (isPinchZooming))
                    || (lastEv != null && lastEv.getPointerCount() > 1)) {
                return;
            }
            MapView.this.getOverlayManager().onLongPress(e, MapView.this);
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            if (MapView.this.getOverlayManager().onScroll(e1, e2, distanceX, distanceY, MapView.this)) {
                return true;
            }

            scrollBy((int) distanceX, (int) distanceY);
            return true;
        }

        @Override
        public void onShowPress(final MotionEvent e) {
            MapView.this.getOverlayManager().onShowPress(e, MapView.this);
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            if (MapView.this.getOverlayManager().onSingleTapUp(e, MapView.this)) {
                return true;
            }

            return false;
        }

    }

    private class MapViewDoubleClickListener implements GestureDetector.OnDoubleTapListener {
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (MapView.this.getOverlayManager().onDoubleTap(e, MapView.this)) {
                return true;
            }

            if (mZoomLevel < getMaxZoomLevel()) {
                if (!tracking || !directionShown) {
                    Matrix m = new Matrix();
                    float x = e.getX() + getScrollX() - (getWidth() / 2);
                    float y = e.getY() + getScrollY() - (getHeight() / 2);
                    m.setScale(0.5f, 0.5f, x, y);
                    m.postRotate(-mapOrientation, mProjection.getScreenRect().centerX(), mProjection.getScreenRect().centerY());
                    float[] pts = new float[2];
                    pts[0] = getScrollX();
                    pts[1] = getScrollY();
                    m.mapPoints(pts);
                    scrollTo((int) pts[0], (int) pts[1]);
                    setZoomLevel(getZoomLevel() + 1);
                } else if (lastLocation != null) {
                    setZoomLevel(getZoomLevel() + 1, new GeoPoint(lastLocation));
                }
                if (mListener != null) {
                    mListener.onDoubleTapZoom();
                }
            }

            return true;// zoomInFixing(center);
        }

        @Override
        public boolean onDoubleTapEvent(final MotionEvent e) {
            if (MapView.this.getOverlayManager().onDoubleTapEvent(e, MapView.this)) {
                return true;
            }

            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            if (MapView.this.getOverlayManager().onSingleTapConfirmed(e, MapView.this)) {
                return true;
            }

            return false;
        }
    }

    private class MapViewZoomListener implements OnZoomListener {
        @Override
        public void onZoom(final boolean zoomIn) {
            if (zoomIn) {
                getController().zoomIn();
            } else {
                getController().zoomOut();
            }
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
        }
    }

    // ===========================================================
    // Public Classes
    // ===========================================================

    /**
     * Per-child layout information associated with OpenStreetMapView.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * Special value for the alignment requested by a View. TOP_LEFT means that the location will at the top left the View.
         */
        public static final int TOP_LEFT = 1;
        /**
         * Special value for the alignment requested by a View. TOP_RIGHT means that the location will be centered at the top of the View.
         */
        public static final int TOP_CENTER = 2;
        /**
         * Special value for the alignment requested by a View. TOP_RIGHT means that the location will at the top right the View.
         */
        public static final int TOP_RIGHT = 3;
        /**
         * Special value for the alignment requested by a View. CENTER_LEFT means that the location will at the center left the View.
         */
        public static final int CENTER_LEFT = 4;
        /**
         * Special value for the alignment requested by a View. CENTER means that the location will be centered at the center of the View.
         */
        public static final int CENTER = 5;
        /**
         * Special value for the alignment requested by a View. CENTER_RIGHT means that the location will at the center right the View.
         */
        public static final int CENTER_RIGHT = 6;
        /**
         * Special value for the alignment requested by a View. BOTTOM_LEFT means that the location will be at the bottom left of the View.
         */
        public static final int BOTTOM_LEFT = 7;
        /**
         * Special value for the alignment requested by a View. BOTTOM_CENTER means that the location will be centered at the bottom of the view.
         */
        public static final int BOTTOM_CENTER = 8;
        /**
         * Special value for the alignment requested by a View. BOTTOM_RIGHT means that the location will be at the bottom right of the View.
         */
        public static final int BOTTOM_RIGHT = 9;
        /**
         * The location of the child within the map view.
         */
        public IGeoPoint geoPoint;

        /**
         * The alignment the alignment of the view compared to the location.
         */
        public int alignment;

        public int offsetX;
        public int offsetY;

        /**
         * Creates a new set of layout parameters with the specified width, height and location.
         * 
         * @param width
         *            the width, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height
         *            the height, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param geoPoint
         *            the location of the child within the map view
         * @param alignment
         *            the alignment of the view compared to the location {@link #BOTTOM_CENTER}, {@link #BOTTOM_LEFT}, {@link #BOTTOM_RIGHT}
         *            {@link #TOP_CENTER}, {@link #TOP_LEFT}, {@link #TOP_RIGHT}
         * @param offsetX
         *            the additional X offset from the alignment location to draw the child within the map view
         * @param offsetY
         *            the additional Y offset from the alignment location to draw the child within the map view
         */
        public LayoutParams(final int width, final int height, final IGeoPoint geoPoint, final int alignment, final int offsetX, final int offsetY) {
            super(width, height);
            if (geoPoint != null) {
                this.geoPoint = geoPoint;
            } else {
                this.geoPoint = new GeoPoint(0, 0);
            }
            this.alignment = alignment;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        /**
         * Since we cannot use XML files in this project this constructor is useless. Creates a new set of layout parameters. The values are extracted
         * from the supplied attributes set and context.
         * 
         * @param c
         *            the application environment
         * @param attrs
         *            the set of attributes fom which to extract the layout parameters values
         */
        public LayoutParams(final Context c, final AttributeSet attrs) {
            super(c, attrs);
            this.geoPoint = new GeoPoint(0, 0);
            this.alignment = BOTTOM_CENTER;
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(final ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    @Override
    public void onTileDownloaded() {
        invalidate();
    }

    public void setNoRendering(boolean b) {
        noRendering = b;
    }

    @Override
    public void invalidate() {
        if (!noRendering) {
            super.invalidate();
        }
    }

    public void forceInvalidate() {
        super.invalidate();
    }

}
