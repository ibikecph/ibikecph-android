// Created by plusminus on 17:58:57 - 25.09.2008
package org.osmdroid.tileprovider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;

import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class MapTileCache implements OpenStreetMapTileProviderConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Object mCachedTilesLockObject = new Object();
	protected LRUMapTileCache mCachedTiles;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MapTileCache() {
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	/**
	 * @param aMaximumCacheSize
	 *            Maximum amount of MapTiles to be hold within.
	 */
	public MapTileCache(final int aMaximumCacheSize) {
		this.mCachedTiles = new LRUMapTileCache(aMaximumCacheSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void ensureCapacity(final int aCapacity) {
		synchronized (mCachedTilesLockObject) {
			mCachedTiles.ensureCapacity(aCapacity);
		}
	}

	public Drawable getMapTile(final MapTile aTile) {
		synchronized (mCachedTilesLockObject) {
			return this.mCachedTiles.get(aTile);
		}
	}

	public void putTile(final MapTile aTile, final Drawable aDrawable) {
		if (aDrawable != null) {
			synchronized (mCachedTilesLockObject) {
				this.mCachedTiles.put(aTile, aDrawable);
			}
		}
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public boolean containsTile(final MapTile aTile) {
		synchronized (mCachedTilesLockObject) {
			return this.mCachedTiles.containsKey(aTile);
		}
	}

	public void clear() {
		synchronized (mCachedTilesLockObject) {
			this.mCachedTiles.clear();
		}
	}
	
	public void removeTile(final MapTile aTile) {
		synchronized (mCachedTilesLockObject) {
			this.mCachedTiles.remove(aTile);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public Set<Entry<MapTile, Drawable>> getAllTiles(Set<Entry<MapTile, Drawable>> reuse) {
		if (reuse == null)
			reuse = new HashSet<Entry<MapTile, Drawable>>();
		else
			reuse.clear();

		synchronized (mCachedTilesLockObject) {
			reuse.addAll(mCachedTiles.entrySet());
		}

		return reuse;
	}

	public void dump() {
		Iterator<Entry<MapTile, Drawable>> it = getAllTiles(null).iterator();
		Log.d("", "cache:");
		while (it.hasNext()) {
			Log.d("", "" + it.next().getKey());
		}

	}
}
