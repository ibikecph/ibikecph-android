package org.osmdroid.tileprovider.modules;

import java.io.File;

import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileInfoManager;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;

/**
 * Implements a file system cache and provides cached tiles. This functions as a tile provider by serving cached tiles
 * for the supplied tile source.
 * 
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * 
 */
public class MapTileFilesystemProvider extends MapTileFileStorageProviderBase {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final Logger logger = LoggerFactory.getLogger(MapTileFilesystemProvider.class);

	// ===========================================================
	// Fields
	// ===========================================================

	private final long mMaximumCachedFileAge;

	private ITileSource mTileSource;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver) {
		this(pRegisterReceiver, TileSourceFactory.DEFAULT_TILE_SOURCE);
	}

	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver, final ITileSource aTileSource) {
		this(pRegisterReceiver, aTileSource, DEFAULT_MAXIMUM_CACHED_FILE_AGE);
	}

	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver, final ITileSource pTileSource,
			final long pMaximumCachedFileAge) {
		this(pRegisterReceiver, pTileSource, pMaximumCachedFileAge, NUMBER_OF_TILE_FILESYSTEM_THREADS, TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE);
	}

	/**
	 * Provides a file system based cache tile provider. Other providers can register and store data in the cache.
	 * 
	 * @param pRegisterReceiver
	 */
	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver, final ITileSource pTileSource,
			final long pMaximumCachedFileAge, int pThreadPoolSize, int pPendingQueueSize) {
		super(pRegisterReceiver, pThreadPoolSize, pPendingQueueSize);
		mTileSource = pTileSource;

		mMaximumCachedFileAge = pMaximumCachedFileAge;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean getUsesDataConnection() {
		return false;
	}

	@Override
	protected String getName() {
		return "File System Cache Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "filesystem";
	}

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	};

	@Override
	public int getMinimumZoomLevel() {
		return mTileSource != null ? mTileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL;
	}

	@Override
	public int getMaximumZoomLevel() {
		return mTileSource != null ? mTileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL;
	}

	@Override
	public void setTileSource(final ITileSource pTileSource) {
		mTileSource = pTileSource;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState) throws CantContinueException {

			if (mTileSource == null) {
				return null;
			}

			final MapTile tile = pState.getMapTile();

			// if there's no sdcard then don't do anything
			if (!getSdCardAvailable()) {
				if (DEBUGMODE) {
					logger.debug("No sdcard - do nothing for tile: " + tile);
				}
				return null;
			}

			// Check the tile source to see if its file is available and if so, then render the
			// drawable and return the tile
			final File file = new File(TILE_PATH_BASE, mTileSource.getTileRelativeFilenameString(tile) + TILE_PATH_EXTENSION);
			if (file.exists()) {

				try {
					final Drawable drawable = mTileSource.getDrawable(file.getPath());

					// Check to see if file has expired
					final long now = System.currentTimeMillis();
					final long lastModified = file.lastModified();
					final boolean fileExpired = (lastModified < now - mMaximumCachedFileAge) || MapTileInfoManager.getInstance().didTileExpire(tile);

					if (fileExpired) {
						if (DEBUGMODE) {
							logger.debug("Tile expired: " + tile);
						}
						drawable.setState(new int[] { ExpirableBitmapDrawable.EXPIRED });
					}

					return drawable;
				} catch (final LowMemoryException e) {
					// low memory so empty the queue
					logger.warn("LowMemoryException in MapTileFileSystem downloading MapTile: " + tile + " : " + e);
					throw new CantContinueException(e);
				}
			}

			// If we get here then there is no file in the file cache
			return null;
		}

		public Drawable getBundledTile(final MapTileRequestState pState) throws CantContinueException {
			// final MapTile tile = pState.getMapTile();
			// final int mDiff = tile.getZoomLevel() - 12;
			// final int mTileSize_2 = 256 >> mDiff;
			// final MapTile tile2 = new MapTile(12, tile.getX() >> (mDiff), tile.getY() >> (mDiff));
			// final File file2 = new File(TILE_PATH_BASE, mTileSource.getTileRelativeFilenameString(tile2) +
			// TILE_PATH_EXTENSION);
			// if (file2 == null || !file2.exists()) {
			// return null;
			// }
			// try {
			// final Drawable oldDrawable = mTileSource.getDrawable(file2.getPath());
			// final Rect mSrcRect = new Rect();
			// final Rect mDestRect = new Rect();
			// if (oldDrawable instanceof BitmapDrawable) {
			// final int xx = (tile.getX() % (1 << mDiff)) * mTileSize_2;
			// final int yy = (tile.getY() % (1 << mDiff)) * mTileSize_2;
			// mSrcRect.set(xx, yy, xx + mTileSize_2, yy + mTileSize_2);
			// mDestRect.set(0, 0, 256, 256);
			//
			// final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565);
			//
			// final Canvas canvas = new Canvas(bitmap);
			// final boolean isReusable = oldDrawable instanceof ReusableBitmapDrawable;
			// boolean success = false;
			// if (isReusable)
			// ((ReusableBitmapDrawable) oldDrawable).beginUsingDrawable();
			// try {
			// if (!isReusable || ((ReusableBitmapDrawable) oldDrawable).isBitmapValid()) {
			// final Bitmap oldBitmap = ((BitmapDrawable) oldDrawable).getBitmap();
			// canvas.drawBitmap(oldBitmap, mSrcRect, mDestRect, null);
			// success = true;
			// oldBitmap.recycle();
			// }
			// } finally {
			// if (isReusable)
			// ((ReusableBitmapDrawable) oldDrawable).finishUsingDrawable();
			// }
			// if (success) {
			// // mNewTiles.put(pTile, bitmap);
			// return new BitmapDrawable(bitmap);
			// }
			// }
			//
			// } catch (LowMemoryException e) {
			// // low memory so empty the queue
			// logger.warn("LowMemoryException in MapTileFileSystem downloading MapTile: " + tile + " : " + e);
			// throw new CantContinueException(e);
			// }
			return null;
		}
	}

}
