package org.osmdroid.views.overlay;

import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafeTranslatedCanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * An overlay class that uses the safe drawing canvas to draw itself and can be zoomed in to high levels without drawing
 * issues.
 * 
 * @see {@link ISafeCanvas}
 */
public abstract class SafeDrawOverlay extends Overlay {

	private static final SafeTranslatedCanvas sSafeCanvas = new SafeTranslatedCanvas();
	private static final Matrix sMatrix = new Matrix();
	private static final float[] sMatrixValues = new float[9];
	private boolean mUseSafeCanvas = true;

	protected abstract void drawSafe(final ISafeCanvas c, final MapView osmv, final boolean shadow);

	public SafeDrawOverlay(Context ctx) {
		super(ctx);
	}

	public SafeDrawOverlay(ResourceProxy pResourceProxy) {
		super(pResourceProxy);
	}

	public void draw(final Canvas c, final MapView osmv, final boolean shadow) {

		sSafeCanvas.setCanvas(c);

		if (this.isUsingSafeCanvas()) {

			// Find the screen offset
			Rect screenRect = osmv.getProjection().getScreenRect();
			sSafeCanvas.xOffset = -screenRect.left;
			sSafeCanvas.yOffset = -screenRect.top;

			// Save the canvas state
			c.save();

			// // Translate the coordinates
			// if (Build.VERSION.SDK_INT >= 11) { //Build.VERSION_CODES.HONEYCOMB) {
			// final float scaleX = osmv.getScaleX();
			// final float scaleY = osmv.getScaleY();
			// c.translate(screenRect.left * scaleX, screenRect.top * scaleY);
			// } else {
			c.getMatrix(sMatrix);
			sMatrix.preTranslate(screenRect.left, screenRect.top);
			c.setMatrix(sMatrix);
			// }
		} else {
			sSafeCanvas.xOffset = 0;
			sSafeCanvas.yOffset = 0;
		}

		this.drawSafe(sSafeCanvas, osmv, shadow);

		if (this.isUsingSafeCanvas()) {
			c.restore();
		}
	}

	public boolean isUsingSafeCanvas() {
		return mUseSafeCanvas;
	}

	public void setUseSafeCanvas(boolean useSafeCanvas) {
		mUseSafeCanvas = useSafeCanvas;
	}
}
