/**
 * ScaledTilesOverlay.java
 * osmdroid-lib
 *
 * Author: Nikola Gencic nikola.gencic@spoiledmilk.com
 * Date: � Sep 13, 2013
 */
package org.osmdroid.views.overlay;

import java.util.Map.Entry;

import microsoft.mappoint.TileSystem;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

/**
 * These objects are the principle consumer of map tiles.
 * 
 * see {@link MapTile} for an overview of how tiles are acquired by this overlay.
 * 
 */

public class ScaledTilesOverlay extends SafeDrawOverlay implements IOverlayMenuProvider {

	private static final Logger logger = LoggerFactory.getLogger(ScaledTilesOverlay.class);

	public static final int MENU_MAP_MODE = getSafeMenuId();
	public static final int MENU_TILE_SOURCE_STARTING_ID = getSafeMenuIdSequence(TileSourceFactory.getTileSources().size());
	public static final int MENU_OFFLINE = getSafeMenuId();

	/** Current tile source */
	protected final MapTileProviderBase mTileProvider;

	/* to avoid allocations during draw */
	protected final Paint mDebugPaint = new Paint();

	private boolean mOptionsMenuEnabled = true;

	private int currentZoom = 17;

	public ScaledTilesOverlay(final MapTileProviderBase aTileProvider, final Context aContext) {
		this(aTileProvider, new ResourceProxy(aContext));
	}

	public ScaledTilesOverlay(final MapTileProviderBase aTileProvider, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		if (aTileProvider == null) {
			throw new IllegalArgumentException("You must pass a valid tile provider to the tiles overlay.");
		}
		this.mTileProvider = aTileProvider;
	}

	@Override
	public void onDetach(final MapView pMapView) {
		this.mTileProvider.detach();
	}

	public int getMinimumZoomLevel() {
		return mTileProvider.getMinimumZoomLevel();
	}

	public int getMaximumZoomLevel() {
		return mTileProvider.getMaximumZoomLevel();
	}

	/**
	 * Whether to use the network connection if it's available.
	 */
	public boolean useDataConnection() {
		return mTileProvider.useDataConnection();
	}

	/**
	 * Set whether to use the network connection if it's available.
	 * 
	 * @param aMode
	 *            if true use the network connection if it's available. if false don't use the network connection even
	 *            if it's available.
	 */
	public void setUseDataConnection(final boolean aMode) {
		mTileProvider.setUseDataConnection(aMode);
	}

	@Override
	protected void drawSafe(final ISafeCanvas c, final MapView osmv, final boolean shadow) {

		if (DEBUGMODE) {
			logger.trace("onDraw(" + shadow + ")");
		}

		if (shadow) {
			return;
		}

		final Projection pj = osmv.getProjection();
		final int zoomLevel = pj.getZoomLevelFloor();

		SafePaint sp = new SafePaint();
		sp.setColor(Color.argb(0, 255, 0, 0)); // 25
		c.drawRect(pj.getScreenRect(), sp);

		// Draw all tiles that aren't in the current zoom level
		for (Entry<MapTile, Drawable> entry : mTileProvider.getTileCache().getAllTiles(null)) {
			MapTile mapTile = entry.getKey();
			// if (mapTile.getZoomLevel() != zoomLevel) {
			// final boolean zoomingIn = mapTile.getZoomLevel() < zoomLevel;
			// if (zoomingIn) {
			// if (mapTile.getX() % 2 == 0 && mapTile.getY() % 2 == 0) {
			final int zoomdiff = currentZoom - 17;// Math.abs(mapTile.getZoomLevel() - zoomLevel);
			final int worldsize_2 = TileSystem.MapSize(mapTile.getZoomLevel()) >> 1;
			Point p = TileSystem.TileXYToPixelXY(mapTile.getX(), mapTile.getY(), null);
			p.offset(-worldsize_2, -worldsize_2);
			Drawable drawable = entry.getValue();
			drawable.setBounds((p.x) << zoomdiff, (p.y) << zoomdiff, (p.x + 256) << zoomdiff, (p.y + 256) << zoomdiff);
			drawable.draw(c.getSafeCanvas());
			// }
			// } else {
			// final int zoomdiff = Math.abs(mapTile.getZoomLevel() - zoomLevel);
			// final int worldsize_2 = TileSystem.MapSize(mapTile.getZoomLevel()) >> 1;
			// Point p = TileSystem.TileXYToPixelXY(mapTile.getX(), mapTile.getY(), null);
			// p.offset(-worldsize_2, -worldsize_2);
			// Drawable drawable = entry.getValue();
			// drawable.setBounds((p.x) >> zoomdiff, (p.y) >> zoomdiff, (p.x + 256) >> zoomdiff, (p.y + 256) >>
			// zoomdiff);
			// drawable.draw(c.getSafeCanvas());
			// }

		}
	}

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

		// final SubMenu mapMenu = pMenu.addSubMenu(0, MENU_MAP_MODE + pMenuIdOffset, Menu.NONE,
		// mResourceProxy.getString(ResourceProxy.string.map_mode)).setIcon(
		// mResourceProxy.getDrawable(ResourceProxy.bitmap.ic_menu_mapmode));

		// for (int a = 0; a < TileSourceFactory.getTileSources().size(); a++) {
		// final ITileSource tileSource = TileSourceFactory.getTileSources().get(a);
		// mapMenu.add(MENU_MAP_MODE + pMenuIdOffset, MENU_TILE_SOURCE_STARTING_ID + a + pMenuIdOffset, Menu.NONE,
		// tileSource.localizedName(mResourceProxy));
		// }
		// mapMenu.setGroupCheckable(MENU_MAP_MODE + pMenuIdOffset, true, true);

		// TODO
		final String title = "";// pMapView.getResourceProxy().getString(
		// pMapView.useDataConnection() ? ResourceProxy.string.offline_mode : ResourceProxy.string.online_mode);
		final Drawable icon = pMapView.getResourceProxy().getDrawable(ResourceProxy.ic_menu_offline);
		pMenu.add(0, MENU_OFFLINE + pMenuIdOffset, Menu.NONE, title).setIcon(icon);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu pMenu, final int pMenuIdOffset, final MapView pMapView) {
		final int index = TileSourceFactory.getTileSources().indexOf(pMapView.getTileProvider().getTileSource());
		if (index >= 0) {
			pMenu.findItem(MENU_TILE_SOURCE_STARTING_ID + index + pMenuIdOffset).setChecked(true);
		}

		// pMenu.findItem(MENU_OFFLINE + pMenuIdOffset).setTitle(
		// pMapView.getResourceProxy().getString(
		// pMapView.useDataConnection() ? ResourceProxy.string.offline_mode : ResourceProxy.string.online_mode));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem pItem, final int pMenuIdOffset, final MapView pMapView) {

		final int menuId = pItem.getItemId() - pMenuIdOffset;
		if ((menuId >= MENU_TILE_SOURCE_STARTING_ID) && (menuId < MENU_TILE_SOURCE_STARTING_ID + TileSourceFactory.getTileSources().size())) {
			pMapView.setTileSource(TileSourceFactory.getTileSources().get(menuId - MENU_TILE_SOURCE_STARTING_ID));
			return true;
		} else if (menuId == MENU_OFFLINE) {
			final boolean useDataConnection = !pMapView.useDataConnection();
			pMapView.setUseDataConnection(useDataConnection);
			return true;
		} else {
			return false;
		}
	}

	public void setCurrentZoomLevel(int zoomLevel) {
		currentZoom = zoomLevel;
	}
}
