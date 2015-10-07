package org.osmdroid.tileprovider.modules;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import microsoft.mappoint.TileSystem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileCache;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.osmdroid.util.MyMath;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class TileDownloadQueue implements OpenStreetMapTileProviderConstants {

	private IFilesystemCache mFilesystemCache;
	private XYTileSource mTileSource;
	private SimpleInvalidationHandler handler;
	private final Point mUpperLeft = new Point();
	private final Point mLowerRight = new Point();
	private MapTileCache mTileCache;
	private DownloadThread thread1, thread2;
	private Rect previousViewPort;
	private float mZoomLev = 0;
	private MapTileProviderBase tileProvider;

	static final String TAG = "Tile Downloader";

	public TileDownloadQueue(IFilesystemCache mFilesystemCache, SimpleInvalidationHandler handler, MapTileCache mTileCache,
			MapTileProviderBase tileProvider) {
		this.mFilesystemCache = mFilesystemCache;
		this.handler = handler;
		this.mTileCache = mTileCache;
		this.mTileSource = (XYTileSource) TileSourceFactory.IBIKECPH;
		this.tileProvider = tileProvider;
	}

	public void downloadTiles(final float pZoomLevel, final Rect pViewPort) {

		if (previousViewPort != null && previousViewPort.contains(pViewPort) && thread1 != null && thread1.isAlive()
				&& !thread1.isInterrupted() && pZoomLevel == mZoomLev) {
			return;
		} else {
			previousViewPort = pViewPort;
		}

		mZoomLev = pZoomLevel;

		interrupt();

		thread1 = new DownloadThread(pZoomLevel, pViewPort, true);
		thread2 = new DownloadThread(pZoomLevel, pViewPort, false);

		// thread1.start();
		// thread2.start();
	}

	public void interrupt() {
		Log.d("", "tile downloader interrupted");
		if (thread1 != null && thread1.isAlive()) {
			thread1.isRunning = false;
			thread1.interrupt();
		}
		if (thread2 != null && thread2.isAlive()) {
			thread1.isRunning = false;
			thread2.interrupt();
		}
	}

	public void setTileSource(XYTileSource source) {
		this.mTileSource = source;

	}

	public float getZoomLevel() {
		return mZoomLev;
	}

	private class DownloadThread extends Thread {

		public int pZoomLevel;
		public Rect pViewPort;
		public boolean isUpDirection;
		boolean isRunning = true;

		public DownloadThread(final float pZoomLevel, final Rect pViewPort, boolean isUpDirection) {
			this.pZoomLevel = (int) pZoomLevel;
			this.pViewPort = pViewPort;
			this.isUpDirection = isUpDirection;
		}

		@Override
		public void run() {

			Log.d("", "tile downloader started");

			TileSystem.PixelXYToTileXY(pViewPort.left, pViewPort.top, mUpperLeft);
			mUpperLeft.offset(-1, -1);
			TileSystem.PixelXYToTileXY(pViewPort.right, pViewPort.bottom, mLowerRight);

			final int mapTileUpperBound = 1 << pZoomLevel;

			int y = isUpDirection ? (mUpperLeft.y + mLowerRight.y) / 2 : (mUpperLeft.y + mLowerRight.y) / 2 + 1;
			// int lastYUp = y, lastYDown = y;
			// boolean isDownDirection = true;
			// boolean isTopHit = false, isBottomHit = false;

			while (!isInterrupted() && isRunning) {
				for (int x = mUpperLeft.x; x <= mLowerRight.x && !isInterrupted() && isRunning; x++) {
					// Construct a MapTile to request from the tile provider.
					final int tileY = MyMath.mod(y, mapTileUpperBound);
					final int tileX = MyMath.mod(x, mapTileUpperBound);
					MapTile tile = new MapTile(pZoomLevel, tileX, tileY);
					final MapTile temp = tile;

					if (tile.getZoomLevel() > 17) {
						final int zoomdiff = tile.getZoomLevel() - 17;
						tile = new MapTile(17, temp.getX() >> zoomdiff, temp.getY() >> zoomdiff);
					}
					tile.originalTile = temp;

					Point screenCords = new Point();
					screenCords = TileSystem.TileXYToPixelXY(tile.getX(), tile.getY(), screenCords);
					Log.d(TAG, "tile screen coordinates = " + screenCords.x + " , " + +screenCords.y);

					boolean inCache = mTileCache.containsTile(tile);

					if (inCache) {
						Drawable d = mTileCache.getMapTile(tile);
						// Log.d(TAG, "drawable class = " + d.getClass());
						if ((d instanceof ExpirableBitmapDrawable || d instanceof ReusableBitmapDrawable)
								&& ((ExpirableBitmapDrawable) d).getIsScaled()) {
							inCache = false;
						}

					}

					Log.d(TAG, "in cache = " + inCache);

					final String state = Environment.getExternalStorageState();
					boolean sdCardAvailable = Environment.MEDIA_MOUNTED.equals(state);
					boolean inFileSystem = !sdCardAvailable ? false : mFilesystemCache.fileExists(mTileSource, tile);

					Log.d(TAG, "in fileSystem = " + inFileSystem);

					if (!(inCache || inFileSystem)) {
						Drawable d = loadTile(tile);
						if (d != null && handler != null) {
							// if (tile.originalTile.getX() >= mUpperLeft.x &&
							// tile.originalTile.getX() <= mLowerRight.x
							// && tile.originalTile.getY() >= mUpperLeft.y &&
							// tile.originalTile.getY() <= mLowerRight.y)
							// {
							// Log.d("", "invalidating the map for the tile " +
							// tile);
							mTileCache.putTile(tile, d);
							Message msg = new Message();
							msg.what = MapTile.MAPTILE_SUCCESS_ID;
							handler.sendMessage(msg);
							// }
						}
					} else if (inFileSystem && !inCache) {
						// tileProvider.getMapTile(tile);
					}

					if (x < mUpperLeft.x || x > mLowerRight.x) {
						break;
					}

					// Log.d("", "current x = " + x + " left = " + mUpperLeft.x
					// + " right = " + mLowerRight.x);
				}

				if (isUpDirection) {
					y--;
				} else {
					y++;
				}

				Log.d("", "current y = " + y + " top = " + mUpperLeft.y + " bottom = " + mLowerRight.y);
				if (y < mUpperLeft.y || y > mLowerRight.y) {
					break;
				}

				// if (isTopHit && isBottomHit) {
				// break;
				// } else if (isTopHit) {
				// y = ++lastYDown;
				// if (y >= mLowerRight.y) {
				// isBottomHit = true;
				// }
				// } else if (isBottomHit) {
				// y = --lastYUp;
				// if (y <= mUpperLeft.y) {
				// isTopHit = true;
				// }
				// } else if (isDownDirection) {
				// y = ++lastYDown;
				// if (y >= mLowerRight.y) {
				// isBottomHit = true;
				// }
				// isDownDirection = !isDownDirection;
				//
				// } else {
				// y = --lastYUp;
				// if (y <= mUpperLeft.y) {
				// isTopHit = true;
				// }
				// isDownDirection = !isDownDirection;
				// }
			}

			Log.d(TAG, "tile downloader finished");
		}

		public Drawable loadTile(final MapTile tile) {

			if (mTileSource == null) {
				return null;
			}

			InputStream in = null;
			OutputStream out = null;

			try {

				final String tileURLString = mTileSource.getTileURLString(tile);

				if (TextUtils.isEmpty(tileURLString)) {
					return null;
				}

				Log.d(TAG, "Downloading Maptile from url: " + tileURLString);

				final HttpClient client = new DefaultHttpClient();
				final HttpUriRequest head = new HttpGet(tileURLString);
				// lastClient = client;
				final HttpResponse response = client.execute(head);

				// Check to see if we got success
				final org.apache.http.StatusLine line = response.getStatusLine();
				if (line.getStatusCode() != 200) {
					Log.w("", "Problem downloading MapTile: " + tile + " HTTP response: " + line);
					return null;
				}

				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					Log.w("", "No content downloading MapTile: " + tile);
					return null;
				}
				in = entity.getContent();

				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();
				final byte[] data = dataStream.toByteArray();
				final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

				// Save the data to the filesystem cache
				if (mFilesystemCache != null) {
					// Log.d("", "putting a tile " + tile +
					// " in a file cache, zoom in downloader = " + mZoomLev +
					// " length = " + data.length);
					mFilesystemCache.saveFile(mTileSource, tile, byteStream);
					byteStream.reset();
				}
				final Drawable result = mTileSource.getDrawable(byteStream);

				return result;

			} catch (final UnknownHostException e) {
				// no network connection so empty the queue
				Log.w("", "UnknownHostException downloading MapTile: " + tile + " : " + e);
			} catch (final LowMemoryException e) {
				// low memory so empty the queue
				Log.w("", "LowMemoryException downloading MapTile: " + tile + " : " + e);
			} catch (final FileNotFoundException e) {
				Log.w("", "Tile not found: " + tile + " : " + e);
			} catch (final IOException e) {
				Log.w("", "IOException downloading MapTile: " + tile + " : " + e);
			} catch (final Throwable e) {
				Log.w("", "Error downloading MapTile: " + tile, e);
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}

			return null;
		}
	}

}
