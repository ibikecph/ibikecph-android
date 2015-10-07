package org.osmdroid.tileprovider.modules;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileInfo;
import org.osmdroid.tileprovider.MapTileInfoManager;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

/**
 * The {@link MapTileDownloader} loads tiles from an HTTP server. It saves downloaded tiles to an IFilesystemCache if
 * available.
 * 
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 * 
 */
public class MapTileDownloader extends MapTileModuleProviderBase {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final Logger logger = LoggerFactory.getLogger(MapTileDownloader.class);

	// ===========================================================
	// Fields
	// ===========================================================

	public final IFilesystemCache mFilesystemCache;

	public OnlineTileSourceBase mTileSource;

	private final INetworkAvailablityCheck mNetworkAvailablityCheck;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MapTileDownloader(final ITileSource pTileSource) {
		this(pTileSource, null, null);
	}

	public MapTileDownloader(final ITileSource pTileSource, final IFilesystemCache pFilesystemCache) {
		this(pTileSource, pFilesystemCache, null);
	}

	public MapTileDownloader(final ITileSource pTileSource, final IFilesystemCache pFilesystemCache,
			final INetworkAvailablityCheck pNetworkAvailablityCheck) {
		this(pTileSource, pFilesystemCache, pNetworkAvailablityCheck, NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);
	}

	public MapTileDownloader(final ITileSource pTileSource, final IFilesystemCache pFilesystemCache,
			final INetworkAvailablityCheck pNetworkAvailablityCheck, int pThreadPoolSize, int pPendingQueueSize) {
		super(pThreadPoolSize, pPendingQueueSize);

		mFilesystemCache = pFilesystemCache;
		mNetworkAvailablityCheck = pNetworkAvailablityCheck;
		setTileSource(pTileSource);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ITileSource getTileSource() {
		return mTileSource;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean getUsesDataConnection() {
		return true;
	}

	@Override
	protected String getName() {
		return "Online Tile Download Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "downloader";
	}

	@Override
	public Runnable getTileLoader() {
		return new TileLoader();
	};

	@Override
	public int getMinimumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL);
	}

	@Override
	public int getMaximumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL);
	}

	@Override
	public void setTileSource(final ITileSource tileSource) {
		// We are only interested in OnlineTileSourceBase tile sources
		if (tileSource instanceof OnlineTileSourceBase) {
			mTileSource = (OnlineTileSourceBase) tileSource;
		} else {
			// Otherwise shut down the tile downloader
			mTileSource = null;
		}
	}

	// class TileDownloadConnection {
	// public MapTile tile;
	// public HttpClient client;
	//
	// public TileDownloadConnection(HttpClient client, MapTile tile) {
	// this.tile = tile;
	// this.client = client;
	// }
	// }

	// static HashMap<HttpClient, TileDownloadConnection> activeConntections = new HashMap<HttpClient,
	// TileDownloadConnection>();

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState aState) throws CantContinueException {

			if (mTileSource == null) {
				return null;
			}

			InputStream in = null;
			OutputStream out = null;
			final MapTile tile = aState.getMapTile();

			try {

				if (mNetworkAvailablityCheck != null && !mNetworkAvailablityCheck.getNetworkAvailable()) {
					if (DEBUGMODE) {
						logger.debug("Skipping " + getName() + " due to NetworkAvailabliltyCheck.");
					}
					return null;
				}

				final String tileURLString = mTileSource.getTileURLString(tile);

				if (TextUtils.isEmpty(tileURLString)) {
					return null;
				}

				final HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "I Bike CPH Android");
				final HttpUriRequest head = new HttpGet(tileURLString);

				// cancel downloading of tiles that are not visible
				// int numOfCanceled = 0;
				// synchronized (activeConntections) {
				// Iterator<Entry<HttpClient, TileDownloadConnection>> it = activeConntections.entrySet().iterator();
				// while (it.hasNext()) {
				// Entry<HttpClient, TileDownloadConnection> entry = it.next();
				// if (!entry.getValue().tile.isVisible()) {
				// entry.getKey().getConnectionManager().shutdown();
				// it.remove();
				// // numOfCanceled++;
				// }
				// }
				//
				// // Log.d("I Bike CPH", "num of canceled tile downloads = " + numOfCanceled);
				//
				// activeConntections.put(client, new TileDownloadConnection(client, aState.getMapTile()));
				// }

				// if (tile.isVisible()) {

				final HttpResponse response = client.execute(head);
				
				

				// synchronized (activeConntections) {
				// activeConntections.remove(client);
				// }

				// Check to see if we got success
				final org.apache.http.StatusLine line = response.getStatusLine();
				if (line.getStatusCode() != 200) {
					logger.warn("Problem downloading MapTile: " + tile + " HTTP response: " + line);
					return null;
				}

				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					logger.warn("No content downloading MapTile: " + tile);
					return null;
				}
				in = entity.getContent();
				
				Header lastModifiedHeader = response
						.getFirstHeader("Last-Modified");
				String lastModifiedValue = lastModifiedHeader != null ? lastModifiedHeader
						.getValue() : null;

						// used only for testing purposes
//			    if (lastModifiedValue == null) {
//			    	Header dateHeader = response.getFirstHeader("Date");
//					lastModifiedValue = dateHeader != null ? dateHeader.getValue() : null;
//			    }
						
				Header eTagHeader = response.getFirstHeader("Etag");
				String eTagValue = eTagHeader != null ? eTagHeader.getValue()
						: null;
				if (eTagValue != null && lastModifiedValue != null) {
					if (DEBUGMODE) {
						logger.debug("Tile can be used with Conditional GET");
					}
					MapTileInfo info = new MapTileInfo();
					info.seteTag(eTagValue);
					info.setLastModified(lastModifiedValue);
					info.setUrl(tileURLString);
					info.setExpired(false);
					MapTileInfoManager.getInstance().putMapTileInfo(tile, info);
				}

				// if (tile.isVisible()) {
				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();
				final byte[] data = dataStream.toByteArray();
				final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

				// if (tile.isVisible()) {

				// Save the data to the filesystem cache
				if (mFilesystemCache != null) {
					mFilesystemCache.saveFile(mTileSource, tile, byteStream);
					byteStream.reset();
				}
				final Drawable result = mTileSource.getDrawable(byteStream);

				return result;
				// } else {
				// return null;
				// }
				//
				// } else {
				// return null;
				// }
				//
				// } else {
				// return null;
				// }
			} catch (final UnknownHostException e) {
				// no network connection so empty the queue
				logger.warn("UnknownHostException downloading MapTile: " + tile + " : " + e);
				throw new CantContinueException(e);
			} catch (final LowMemoryException e) {
				// low memory so empty the queue
				logger.warn("LowMemoryException downloading MapTile: " + tile + " : " + e);
				throw new CantContinueException(e);
			} catch (final FileNotFoundException e) {
				logger.warn("Tile not found: " + tile + " : " + e);
			} catch (final IOException e) {
				logger.warn("IOException downloading MapTile: " + tile + " : " + e);
			} catch (final Throwable e) {
				logger.warn("Error downloading MapTile: " + tile, e);
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}

			return null;
		}

		@Override
		protected void tileLoaded(final MapTileRequestState pState, final Drawable pDrawable) {
			removeTileFromQueues(pState.getMapTile());
			// don't return the tile because we'll wait for the fs provider to ask for it
			// this prevent flickering when a load of delayed downloads complete for tiles
			// that we might not even be interested in any more
			pState.getCallback().mapTileRequestCompleted(pState, null);
			// We want to return the Bitmap to the BitmapPool if applicable
			if (pDrawable instanceof ReusableBitmapDrawable) {
				BitmapPool.getInstance().returnDrawableToPool((ReusableBitmapDrawable) pDrawable);
			}
		}

	}
}
