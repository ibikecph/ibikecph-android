package org.osmdroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import org.osmdroid.views.util.constants.MapViewConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceProxy implements MapViewConstants {

	private static final Logger logger = LoggerFactory.getLogger(ResourceProxy.class);

	public static int center = R.drawable.center;
	public static int direction_arrow = R.drawable.direction_arrow;
	public static int marker_default = R.drawable.marker_default;
	public static int marker_default_focused_base = R.drawable.marker_default_focused_base;
	public static int navto_small = R.drawable.navto_small;
	public static int next = R.drawable.next;
	public static int previous = R.drawable.previous;
	public static int person = R.drawable.person;

	public static int ic_menu_offline = R.drawable.ic_menu_offline;
	public static int ic_menu_mylocation = R.drawable.ic_menu_mylocation;
	public static int ic_menu_compass = R.drawable.ic_menu_compass;
	public static int ic_menu_mapmode = R.drawable.ic_menu_mapmode;

	private DisplayMetrics mDisplayMetrics;

	Context appContext;

	/**
	 * Constructor.
	 * 
	 * @param pContext
	 *            Used to get the display metrics that are used for scaling the bitmaps returned by {@link getBitmap}. Can be null, in which
	 *            case the bitmaps are not scaled.
	 */
	public ResourceProxy(final Context pContext) {
		appContext = pContext.getApplicationContext();
		mDisplayMetrics = pContext.getResources().getDisplayMetrics();
		if (DEBUGMODE) {
			logger.debug("mDisplayMetrics=" + mDisplayMetrics);
		}
	}

	public Bitmap getBitmap(int resId) {
		return BitmapFactory.decodeResource(appContext.getResources(), resId);
	}

	public Drawable getDrawable(int resId) {
		return appContext.getResources().getDrawable(resId);
	}

	public float getDisplayMetricsDensity() {
		return mDisplayMetrics.density;
	}

	public String getString(int resId) {
		return appContext.getString(resId);
	}

	public String getString(int pResId, final Object... formatArgs) {
		return String.format(getString(pResId), formatArgs);
	}
}
