package com.spoiledmilk.ibikecph.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MapGridView extends View {

    static final int mLoadingBackgroundColor = Color.rgb(216, 208, 208);
    static final int mLoadingLineColor = Color.rgb(200, 192, 192);
    static final int tileSize = 256;

    public MapGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MapGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MapGridView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final Paint paint = new Paint();
        canvas.drawColor(mLoadingBackgroundColor);
        paint.setColor(mLoadingLineColor);
        paint.setStrokeWidth(0);
        final int lineSize = tileSize / 16;
        for (int a = 0; a < getWidth(); a += lineSize) {
            canvas.drawLine(a, 0, a, getHeight(), paint);
        }
        for (int a = 0; a < getHeight(); a += lineSize) {
            canvas.drawLine(0, a, getWidth(), a, paint);
        }
    }
}
