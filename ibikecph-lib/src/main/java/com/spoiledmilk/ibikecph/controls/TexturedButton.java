// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.controls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.util.Util;

/**
 * A button control that includes an image.
 * @author jens
 *
 */
public class TexturedButton extends RelativeLayout {

	private Button button;
	private ImageView texture;
	private TextView text;

	public TexturedButton(Context context) {
		super(context);
		init();
	}

	public TexturedButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TexturedButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setBackgroundColor(Color.TRANSPARENT);
		button = new Button(getContext());
		button.setBackgroundColor(Color.TRANSPARENT);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		button.setLayoutParams(params);
		addView(button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				TexturedButton.this.performClick();
			}

		});
		texture = new ImageView(getContext());
		params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.setMargins(Util.dp2px(2), Util.dp2px(2), Util.dp2px(2),
				Util.dp2px(1));
		texture.setLayoutParams(params);
		texture.setScaleType(ScaleType.FIT_XY);
		// texture.setAlpha(140);
		addView(texture);
		text = new TextView(getContext());
		params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		text.setLayoutParams(params);
		text.setGravity(Gravity.CENTER);
		addView(text);
	}

	public void setBackgroundResource(int resId) {
		button.setBackgroundResource(resId);
	}

	public void setTextureResource(int resId) {
		texture.setBackgroundResource(resId);
		texture.getBackground().setColorFilter(0xFFFFFF,
				PorterDuff.Mode.SRC_ATOP);
	}

	public void setText(String text) {
		this.text.setText(text);
	}

	public void showTexture() {
		texture.setVisibility(View.VISIBLE);
	}

	public void hideTexture() {
		texture.setVisibility(View.GONE);
	}

	public void setTypeface(Typeface tf) {
		text.setTypeface(tf);
	}

	public void setTextColor(int color) {
		text.setTextColor(color);
	}

	@Override
	public void setEnabled(boolean b) {
		button.setEnabled(b);
	}

	public void setTextSize(float size) {
		text.setTextSize(size);
	}

	public void setDimmed(boolean dimmed) {
		if (dimmed)
			button.getBackground().setAlpha(180);
		else
			button.getBackground().setAlpha(255);

	}

}
