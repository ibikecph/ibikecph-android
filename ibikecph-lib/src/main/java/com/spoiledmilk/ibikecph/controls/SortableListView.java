// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.controls;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;

import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.favorites.FavoritesAdapter;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A list view that allows for sorting of the list items. Used in the list of favorites.
 * @author jens
 *
 */
public class SortableListView extends ListView {
	private int firstVisibleItem = 0;
	private int viewIndex = 0;
	private int viewHeight;
	private FavoritesAdapter adapter;
	private View view = null;
	LeftMenu parentContainer;
	private int indexToAnimate = -1;
	private int indexToReorder = 0;
	boolean isDragStarted = false;
	ArrayList<ViewData> views;
	float lastY;
	boolean scrolling = false;
	int fixedPositionOffset = 0;
	private GestureDetectorCompat mDetector;
	public int srollPos = 0;

	private void init() {
		mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
	}

	public SortableListView(Context context) {
		super(context);
		init();
	}

	public SortableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void setParentContainer(LeftMenu parentContainer) {
		this.parentContainer = parentContainer;
	}

	public void enableScroll() {
		getParent().requestDisallowInterceptTouchEvent(false);
	}

	public void disableScroll() {
		getParent().requestDisallowInterceptTouchEvent(true);
		smoothScrollToPosition(0);
		firstVisibleItem = 0;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		mDetector.onTouchEvent(event);

		if ((getAdapter() != null && !(((FavoritesAdapter) getAdapter()).isEditMode)) || getAdapter() == null) {
			LOG.d("sortable list view super.onTouch");
			return super.onTouchEvent(event);
		}
		this.adapter = (FavoritesAdapter) getAdapter();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			lastDistanceY = 0;
			views = new ArrayList<ViewData>();
			for (int i = 0; i < getAdapter().getCount(); i++) {
				views.add(new ViewData(getChildAt(i), new TranslateAnimation(0, 0, 0, 0)));
			}
			posY = 0;
			movY = 0;
			touchY = event.getY();
			View temp = getChildAt(0);
			temp.measure(0, 0);
			viewHeight = temp.getMeasuredHeight();
			viewIndex = (int) (event.getY() / temp.getMeasuredHeight()) + fixedPositionOffset;
			indexToReorder = viewIndex;
			if (viewIndex < 0 || viewIndex > views.size() - 1) {
				mDetector.onTouchEvent(event);
				return true;
			}
			view = getChildAt(viewIndex); // - firstVisibleItem
			if (event.getX() < Util.dp2px(5) || event.getX() > Util.dp2px(85)) {
				// draging is enabled only when touching the left part of the view
				isDragStarted = false;
			} else {
				isDragStarted = true;
			}
		}

		if (view == null) {
			view = getChildAt(0);
			viewIndex = 0;
		}
		if (isDragStarted) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				adapter.notifyDataSetChanged();
				view.clearAnimation();
				translate(view, 0, true);
				isDragStarted = false;
				if (adapter.getCount() > 1)
					adapter.reorder(0, 0, true);
				Iterator<ViewData> it = views.iterator();
				while (it.hasNext()) {
					it.next().animation.cancel();
				}
				if (animation != null)
					animation.cancel();
				break;
			case MotionEvent.ACTION_DOWN:
				moveCount = 0;
				touchY = event.getY();
				break;

			case MotionEvent.ACTION_MOVE:
				float newTouchY = event.getY();
				float delta = newTouchY - touchY;
				indexToAnimate = (int) (event.getY() / viewHeight) + fixedPositionOffset;
				try {
					if (indexToReorder != indexToAnimate) {
						if (indexToAnimate >= 0 && indexToAnimate < adapter.getCount()) {
							int newY = delta > 0 ? views.get(indexToAnimate).lastY - viewHeight : views.get(indexToAnimate).lastY
									+ viewHeight;
							ViewData animateView = views.get(indexToAnimate);
							if (animateView.animation != null && animateView.animation.isInitialized())
								animateView.animation.cancel();
							animateView.animation = new TranslateAnimation(0, 0, animateView.lastY, newY);
							animateView.lastY = newY;
							animateView.animation.setFillAfter(true);
							animateView.animation.setFillBefore(true);
							animateView.animation.setDuration(500);
							adapter.reorder(indexToAnimate + firstVisibleItem, indexToReorder + firstVisibleItem, false);
							animateView.view.clearAnimation();
							animateView.view.startAnimation(animateView.animation);
							ViewData temp = animateView;
							views.set(indexToAnimate, views.get(indexToReorder));
							views.set(indexToReorder, temp);
							indexToReorder = indexToAnimate;
						}
					}
				} catch (Exception e) {
					e.getLocalizedMessage();
				}
				translate(view, delta, false);
				touchY = newTouchY;
				break;
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			final float newY = event.getY();
			if ((movY == 0 && Math.abs(newY - touchY) > viewHeight) || Math.abs(newY - movY) > viewHeight) {
				movY = newY;
			}
		}

		return true;
	}

	TranslateAnimation animation;
	float posY = 0, movY = 0;
	float touchY = 0;
	int maxSlide = 200;
	boolean slidden = false;
	int moveCount = 0;

	void translate(View v, float deltaY, final boolean finalAnim) {
		float newY = posY + deltaY;
		if (animation != null && animation.isInitialized())
			animation.cancel();
		animation = new TranslateAnimation(0, 0, posY, newY);
		animation.setDuration(finalAnim ? 0 : 100);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!finalAnim) {
					animation.setFillEnabled(true);
					animation.setFillAfter(true);
				} else {
					view.clearAnimation();
					posY = 0;
				}
			}
		});

		posY = newY;

		v.startAnimation(animation);
	}

	public void clearAnimations() {
		if (view != null)
			view.clearAnimation();
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).clearAnimation();
		}
		invalidate();
	}

	private static class ViewData {
		public View view;
		public TranslateAnimation animation;
		public int lastY = 0;

		public ViewData(View view, TranslateAnimation animation) {
			this.view = view;
			this.animation = animation;
		}
	}

	float lastDistanceY = 0;

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (adapter != null && adapter.getCount() > 1) {
				if (Math.signum(distanceY) != Math.signum(lastDistanceY)) {
					lastDistanceY = 0;
				}
				lastDistanceY += distanceY;
				LOG.d("scroll diff = " + distanceY);
				if (Math.abs(lastDistanceY) > viewHeight) {
					int itemsToScroll = (int) (viewHeight / Math.abs(lastDistanceY));
					if (itemsToScroll == 0) {
						itemsToScroll = 1;
					}
					if (firstVisibleItem != 0 && distanceY < 0) {
						smoothScrollToPosition(firstVisibleItem - itemsToScroll);
						firstVisibleItem -= itemsToScroll;
					} else if (firstVisibleItem != adapter.getCount() - 1 && distanceY > 0) {
						smoothScrollToPosition(firstVisibleItem + getChildCount() + itemsToScroll - 1);
						firstVisibleItem += itemsToScroll;
					}

					if (firstVisibleItem < 0) {
						firstVisibleItem = 0;
					}
					if (firstVisibleItem > adapter.getCount() - getChildCount()) {
						firstVisibleItem = adapter.getCount() - getChildCount();
					}
					lastDistanceY = 0;
				}
			}
			return true;
		};

		@Override
		public boolean onDown(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
			LOG.d("fling");
			if (adapter != null && adapter.getCount() > 1) {
				int itemsToScroll = (int) (viewHeight / Math.abs(velocityY));
				if (firstVisibleItem != 0 && velocityY > 0) {
					smoothScrollToPosition(firstVisibleItem - itemsToScroll);
					firstVisibleItem -= itemsToScroll;
				} else if (firstVisibleItem != adapter.getCount() - 1 && velocityY < 0) {
					smoothScrollToPosition(firstVisibleItem + getChildCount() + itemsToScroll - 1);
					firstVisibleItem += itemsToScroll;
				}
				if (firstVisibleItem < 0) {
					firstVisibleItem = 0;
				}
				if (firstVisibleItem > adapter.getCount() - getChildCount()) {
					firstVisibleItem = adapter.getCount() - getChildCount();
				}
			}
			return true;
		}
	}

}
