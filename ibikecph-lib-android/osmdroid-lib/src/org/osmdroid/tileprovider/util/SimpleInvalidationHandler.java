package org.osmdroid.tileprovider.util;

import org.osmdroid.tileprovider.MapTile;

import android.os.Handler;
import android.os.Message;

public class SimpleInvalidationHandler extends Handler {
	private final TileDownloadListener listener;

	public SimpleInvalidationHandler(TileDownloadListener listener) {
		super();
		this.listener = listener;
	}

	@Override
	public void handleMessage(final Message msg) {
		switch (msg.what) {
		case MapTile.MAPTILE_SUCCESS_ID:
			listener.onTileDownloaded();
			break;
		}
	}
}
