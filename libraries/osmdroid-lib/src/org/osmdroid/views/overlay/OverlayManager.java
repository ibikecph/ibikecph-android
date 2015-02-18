package org.osmdroid.views.overlay;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay.Snappable;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public class OverlayManager extends AbstractList<Overlay> {

	protected TilesOverlay mTilesOverlay;
	private boolean mUseSafeCanvas = true;
	public float currentBearing = 0;
	public float previousBearing = 0;
	public int defaultZoom = 17;
	public int currentZoom = 17;
	public int previousZoom = 17;
	protected final CopyOnWriteArrayList<Overlay> mOverlayList;
	boolean isCanvasDirty = false;
	private MapView mapView;
	private ScaledTilesOverlay scaledTilesOverlay;

	public OverlayManager(final TilesOverlay tilesOverlay, MapView mapView) {
		setTilesOverlay(tilesOverlay);
		mOverlayList = new CopyOnWriteArrayList<Overlay>();
		this.mapView = mapView;
	}

	@Override
	public Overlay get(final int pIndex) {
		return mOverlayList.get(pIndex);
	}

	@Override
	public int size() {
		return mOverlayList.size();
	}

	@Override
	public void add(final int pIndex, final Overlay pElement) {
		mOverlayList.add(pIndex, pElement);
		if (pElement instanceof SafeDrawOverlay)
			((SafeDrawOverlay) pElement).setUseSafeCanvas(this.isUsingSafeCanvas());
	}

	@Override
	public Overlay remove(final int pIndex) {
		return mOverlayList.remove(pIndex);
	}

	@Override
	public void clear() {
		mOverlayList.clear();
	}

	@Override
	public Overlay set(final int pIndex, final Overlay pElement) {
		Overlay overlay = mOverlayList.set(pIndex, pElement);
		if (pElement instanceof SafeDrawOverlay)
			((SafeDrawOverlay) pElement).setUseSafeCanvas(this.isUsingSafeCanvas());
		return overlay;
	}

	public boolean isUsingSafeCanvas() {
		return mUseSafeCanvas;
	}

	public void setUseSafeCanvas(boolean useSafeCanvas) {
		mUseSafeCanvas = useSafeCanvas;
		for (Overlay overlay : mOverlayList)
			if (overlay instanceof SafeDrawOverlay)
				((SafeDrawOverlay) overlay).setUseSafeCanvas(this.isUsingSafeCanvas());
		mTilesOverlay.setUseSafeCanvas(this.isUsingSafeCanvas());
	}

	/**
	 * Gets the optional TilesOverlay class.
	 * 
	 * @return the tilesOverlay
	 */
	public TilesOverlay getTilesOverlay() {
		return mTilesOverlay;
	}

	/**
	 * Sets the optional TilesOverlay class. If set, this overlay will be drawn before all other overlays and will not
	 * be included in the editable list of overlays and can't be cleared except by a subsequent call to
	 * setTilesOverlay().
	 * 
	 * @param tilesOverlay
	 *            the tilesOverlay to set
	 */
	public void setTilesOverlay(final TilesOverlay tilesOverlay) {
		mTilesOverlay = tilesOverlay;
		mTilesOverlay.setUseSafeCanvas(this.isUsingSafeCanvas());
	}

	public Iterable<Overlay> overlaysReversed() {
		return new Iterable<Overlay>() {
			@Override
			public Iterator<Overlay> iterator() {
				final ListIterator<Overlay> i = mOverlayList.listIterator(mOverlayList.size());

				return new Iterator<Overlay>() {
					@Override
					public boolean hasNext() {
						return i.hasPrevious();
					}

					@Override
					public Overlay next() {
						return i.previous();
					}

					@Override
					public void remove() {
						i.remove();
					}
				};
			}
		};
	}

	private void sortOverlays() {
		if (mOverlayList.size() > 1) {
			int order[] = new int[mOverlayList.size()];
			for (int i = 0; i < mOverlayList.size(); i++) {
				if (mOverlayList.get(i).getClass().equals(PathOverlay.class))
					order[i] = 0;
				else if (mOverlayList.get(i).getClass().equals(ItemizedIconOverlay.class))
					order[i] = 1;
				else
					order[i] = 2;
			}
			for (int i = 0; i < mOverlayList.size() - 1; i++) {
				for (int j = i + 1; j < mOverlayList.size(); j++) {
					if (order[i] > order[j]) {
						int temp = order[i];
						order[i] = order[j];
						order[j] = temp;
						Overlay temp2 = mOverlayList.get(i);
						mOverlayList.set(i, mOverlayList.get(j));
						mOverlayList.set(j, temp2);
					}
				}
			}
		}
	}

	public void onDraw(final Canvas c, final MapView pMapView) {

		scaledTilesOverlay.setCurrentZoomLevel(mapView.getZoomLevelFloor());

		if ((mTilesOverlay != null) && mTilesOverlay.isEnabled()) {
			mTilesOverlay.draw(c, pMapView, true);
		}

		if ((mTilesOverlay != null) && mTilesOverlay.isEnabled()) {
			mTilesOverlay.draw(c, pMapView, false);
		}

		if (mapView.getZoomLevel() > 17) {
			if ((scaledTilesOverlay != null) && scaledTilesOverlay.isEnabled()) {
				scaledTilesOverlay.draw(c, pMapView, true);
			}

			if ((scaledTilesOverlay != null) && scaledTilesOverlay.isEnabled()) {
				scaledTilesOverlay.draw(c, pMapView, false);
			}
		}

		sortOverlays();

		for (final Overlay overlay : mOverlayList) {
			if (overlay.isEnabled()) {
				if (isCanvasDirty) {
					isCanvasDirty = false;
					c.restore();
				}
				if (overlay.getClass().equals(ItemizedIconOverlay.class) && mapView.isPinchZooming
						&& !((ItemizedIconOverlay<?>) overlay).isRescaleWhenZoomed()) {
					c.save();
					final Projection pj = pMapView.getProjection();
					Point mCurScreenCoords = new Point();
					pj.toMapPixels(((ItemizedIconOverlay<?>) overlay).getItem(0).mGeoPoint, mCurScreenCoords);
					float sx = mapView.scaleDiffFloat;
					float sy = mapView.scaleDiffFloat;
					if (previousBearing != currentBearing) {
						c.rotate(currentBearing, mCurScreenCoords.x, mCurScreenCoords.y);
					}
					c.scale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
					isCanvasDirty = true;
				} else if (overlay.getClass().equals(ItemizedIconOverlay.class)
						&& (previousBearing != currentBearing || previousZoom != currentZoom)) {
					c.save();
					final Projection pj = pMapView.getProjection();
					Point mCurScreenCoords = new Point();
					pj.toMapPixels(((ItemizedIconOverlay<?>) overlay).getItem(0).mGeoPoint, mCurScreenCoords);
					if (previousBearing != currentBearing)
						c.rotate(currentBearing, mCurScreenCoords.x, mCurScreenCoords.y);
					if (previousZoom != currentZoom) {
						float sx = getScale(currentZoom);
						float sy = getScale(currentZoom);
						if (((ItemizedIconOverlay<?>) overlay).isRescaleWhenZoomed())
							c.scale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
					}
					isCanvasDirty = true;
				}
				overlay.draw(c, pMapView, true);
			}
		}

		for (final Overlay overlay : mOverlayList) {
			if (overlay.isEnabled()) {

				if (isCanvasDirty) {
					isCanvasDirty = false;
					c.restore();
				}
				if (overlay.getClass().equals(ItemizedIconOverlay.class) && mapView.isPinchZooming
						&& !((ItemizedIconOverlay<?>) overlay).isRescaleWhenZoomed()) {
					c.save();
					final Projection pj = pMapView.getProjection();
					Point mCurScreenCoords = new Point();
					pj.toMapPixels(((ItemizedIconOverlay<?>) overlay).getItem(0).mGeoPoint, mCurScreenCoords);
					float sx = mapView.scaleDiffFloat;
					float sy = mapView.scaleDiffFloat;
					if (previousBearing != currentBearing) {
						c.rotate(currentBearing, mCurScreenCoords.x, mCurScreenCoords.y);
					}
					c.scale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
					isCanvasDirty = true;
				} else if (overlay.getClass().equals(ItemizedIconOverlay.class)
						&& (previousBearing != currentBearing || previousZoom != currentZoom)) {
					c.save();
					final Projection pj = pMapView.getProjection();
					Point mCurScreenCoords = new Point();
					pj.toMapPixels(((ItemizedIconOverlay<?>) overlay).getItem(0).mGeoPoint, mCurScreenCoords);
					if (previousBearing != currentBearing)
						c.rotate(currentBearing, mCurScreenCoords.x, mCurScreenCoords.y);
					if (previousZoom != currentZoom) {
						float sx = getScale(currentZoom);
						float sy = getScale(currentZoom);
						if (((ItemizedIconOverlay<?>) overlay).isRescaleWhenZoomed())
							c.scale(sx, sy, mCurScreenCoords.x, mCurScreenCoords.y);
					}
					isCanvasDirty = true;
				}
				overlay.draw(c, pMapView, false);
			}
		}

	}

	private float getScale(int zoomLevel) {
		float ret = 1.0f;
		if (!(zoomLevel >= defaultZoom)) {
			ret -= (defaultZoom - zoomLevel) / 10f;
			if (ret <= 0f)
				ret = 0.1f;
		}
		return ret;
	}

	public void onDetach(final MapView pMapView) {
		if (mTilesOverlay != null) {
			mTilesOverlay.onDetach(pMapView);
		}

		for (final Overlay overlay : this.overlaysReversed()) {
			overlay.onDetach(pMapView);
		}
	}

	public boolean onKeyDown(final int keyCode, final KeyEvent event, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onKeyDown(keyCode, event, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onKeyUp(final int keyCode, final KeyEvent event, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onKeyUp(keyCode, event, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onTouchEvent(final MotionEvent event, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onTouchEvent(event, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onTrackballEvent(final MotionEvent event, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onTrackballEvent(event, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onSnapToItem(final int x, final int y, final Point snapPoint, final IMapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay instanceof Snappable) {
				if (((Snappable) overlay).onSnapToItem(x, y, snapPoint, pMapView)) {
					return true;
				}
			}
		}

		return false;
	}

	/** GestureDetector.OnDoubleTapListener **/

	public boolean onDoubleTap(final MotionEvent e, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onDoubleTap(e, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onDoubleTapEvent(final MotionEvent e, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onDoubleTapEvent(e, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onSingleTapConfirmed(final MotionEvent e, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onSingleTapConfirmed(e, pMapView)) {
				return true;
			}
		}

		return false;
	}

	/** OnGestureListener **/

	public boolean onDown(final MotionEvent pEvent, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onDown(pEvent, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onFling(final MotionEvent pEvent1, final MotionEvent pEvent2, final float pVelocityX, final float pVelocityY,
			final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onLongPress(final MotionEvent pEvent, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onLongPress(pEvent, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public boolean onScroll(final MotionEvent pEvent1, final MotionEvent pEvent2, final float pDistanceX, final float pDistanceY,
			final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView)) {
				return true;
			}
		}

		return false;
	}

	public void onShowPress(final MotionEvent pEvent, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			overlay.onShowPress(pEvent, pMapView);
		}
	}

	public boolean onSingleTapUp(final MotionEvent pEvent, final MapView pMapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if (overlay.onSingleTapUp(pEvent, pMapView)) {
				return true;
			}
		}

		return false;
	}

	// ** Options Menu **//

	public void setOptionsMenusEnabled(final boolean pEnabled) {
		for (final Overlay overlay : mOverlayList) {
			if ((overlay instanceof IOverlayMenuProvider) && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
				((IOverlayMenuProvider) overlay).setOptionsMenuEnabled(pEnabled);
			}
		}
	}

	public boolean onCreateOptionsMenu(final Menu pMenu, final int menuIdOffset, final MapView mapView) {
		boolean result = true;
		for (final Overlay overlay : this.overlaysReversed()) {
			if ((overlay instanceof IOverlayMenuProvider) && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
				result &= ((IOverlayMenuProvider) overlay).onCreateOptionsMenu(pMenu, menuIdOffset, mapView);
			}
		}

		if ((mTilesOverlay != null) && (mTilesOverlay instanceof IOverlayMenuProvider)
				&& ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()) {
			result &= mTilesOverlay.onCreateOptionsMenu(pMenu, menuIdOffset, mapView);
		}

		return result;
	}

	public boolean onPrepareOptionsMenu(final Menu pMenu, final int menuIdOffset, final MapView mapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if ((overlay instanceof IOverlayMenuProvider) && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
				((IOverlayMenuProvider) overlay).onPrepareOptionsMenu(pMenu, menuIdOffset, mapView);
			}
		}

		if ((mTilesOverlay != null) && (mTilesOverlay instanceof IOverlayMenuProvider)
				&& ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()) {
			mTilesOverlay.onPrepareOptionsMenu(pMenu, menuIdOffset, mapView);
		}

		return true;
	}

	public boolean onOptionsItemSelected(final MenuItem item, final int menuIdOffset, final MapView mapView) {
		for (final Overlay overlay : this.overlaysReversed()) {
			if ((overlay instanceof IOverlayMenuProvider) && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()
					&& ((IOverlayMenuProvider) overlay).onOptionsItemSelected(item, menuIdOffset, mapView)) {
				return true;
			}
		}

		if ((mTilesOverlay != null) && (mTilesOverlay instanceof IOverlayMenuProvider)
				&& ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()
				&& ((IOverlayMenuProvider) mTilesOverlay).onOptionsItemSelected(item, menuIdOffset, mapView)) {
			return true;
		}

		return false;
	}

	public void setScaledTilesOverlay(ScaledTilesOverlay scaledTilesOverlay) {
		this.scaledTilesOverlay = scaledTilesOverlay;
	}
}
