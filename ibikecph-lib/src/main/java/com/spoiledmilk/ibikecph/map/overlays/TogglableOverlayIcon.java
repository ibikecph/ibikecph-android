package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kraen on 21-05-16.
 */
public class TogglableOverlayIcon extends View {

    protected TogglableOverlay overlay;
    protected Paint paint = new Paint();

    public TogglableOverlayIcon(Context context) {
        super(context);
    }

    public TogglableOverlayIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOverlay(TogglableOverlay overlay) {
        this.overlay = overlay;
        paint = overlay.getPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float margin = paint.getStrokeWidth() / 2.f;
        if (overlay != null) {
            canvas.drawLine(0.f + margin,
                            0.f + margin,
                            canvas.getWidth() - margin,
                            canvas.getHeight() - margin,
                            paint);
        }
    }
}
