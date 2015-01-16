// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.cykelsuperstier.favorites.AddFavoriteFragment;
import com.spoiledmilk.cykelsuperstier.favorites.EditFavoriteFragment;
import com.spoiledmilk.cykelsuperstier.favorites.FavoritesAdapter;
import com.spoiledmilk.cykelsuperstier.reminders.AlarmUtils;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class LeftMenu extends com.spoiledmilk.ibikecph.LeftMenu {

	boolean remindersExpanded = false;
	boolean wasBtnDoneVisible = false;
	LinearLayout remindersContainer;
	LinearLayout remindersSettingsContainer;
	TextView textPath;
	TextView textService;
	TextView textStrain;
	TextView textMetro;
	TextView textLocalTrain;
	
	int repetition;
	int settingsHeight = 0;
	boolean isAnimationStarted = false;
	boolean checked1 = false, checked2 = false, checked3 = false, checked4 = false, checked5 = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View ret = super.onCreateView(inflater, container, savedInstanceState);
		ret.findViewById(R.id.remindersBackground).setOnTouchListener(
				new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							ret.findViewById(R.id.remindersBackground).setBackgroundColor(getActivity().getResources().getColor(R.color.Orange));
							final Handler handler = new Handler();
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									ret.findViewById(R.id.remindersBackground).setBackgroundColor(getActivity().getResources().getColor(R.color.MenuItemBackground));
								}
							}, 250);
						}
						return false;
					}
				});

		
		
		this.textPath = (TextView) ret.findViewById(R.id.textPath);		
		this.textService = (TextView) ret.findViewById(R.id.textService);
		this.textStrain = (TextView) ret.findViewById(R.id.textStrain);
		this.textMetro = (TextView) ret.findViewById(R.id.textMetro);
		this.textLocalTrain = (TextView) ret.findViewById(R.id.textLocalTrain);
		
		return ret;
	}

	protected int getMenuItemSelectedColor() {
		return getActivity().getResources().getColor(R.color.Orange);
	}

	protected int getMenuItemBackgroundColor() {
		return getActivity().getResources()
				.getColor(R.color.MenuItemBackground);
	}

	@Override
	public void onResume() {
		super.onResume();
		remindersContainer = (LinearLayout) getView().findViewById(R.id.remindersContainer);
		remindersSettingsContainer = (LinearLayout) getView().findViewById(R.id.remindersSettingsContainer);
		getView().findViewById(R.id.aboutContainer).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						Intent i = new Intent(getActivity(), AboutActivity.class);
						getActivity().startActivity(i);
						getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
					}

				});
		remindersContainer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!isAnimationStarted) {
					if (remindersExpanded)
						colapseReminders();
					else
						expandReminders();
					
					remindersExpanded = !remindersExpanded;
				}
			}

		});

		repetition = PreferenceManager.getDefaultSharedPreferences(
				getActivity()).getInt("alarm_repetition", 0);
		final ImageView imgSwitch1 = (ImageView) getView().findViewById(R.id.switch1);
		final ImageView imgSwitch2 = (ImageView) getView().findViewById(R.id.switch2);
		final ImageView imgSwitch3 = (ImageView) getView().findViewById(R.id.switch3);
		final ImageView imgSwitch4 = (ImageView) getView().findViewById(R.id.switch4);
		final ImageView imgSwitch5 = (ImageView) getView().findViewById(R.id.switch5);
		if ((repetition & 64) > 0) {
			checked1 = true;
			imgSwitch1.setImageResource(R.drawable.switch_on);
		}
		if ((repetition & 1) > 0) {
			checked2 = true;
			imgSwitch2.setImageResource(R.drawable.switch_on);
		}
		if ((repetition & 2) > 0) {
			checked3 = true;
			imgSwitch3.setImageResource(R.drawable.switch_on);
		}
		if ((repetition & 4) > 0) {
			checked4 = true;
			imgSwitch4.setImageResource(R.drawable.switch_on);
		}
		if ((repetition & 8) > 0) {
			checked5 = true;
			imgSwitch5.setImageResource(R.drawable.switch_on);
		}

		imgSwitch1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checked1 = !checked1;
				if (checked1)
					repetition = repetition | 64;
				else
					repetition = repetition & 63;
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("alarm_repetition", repetition).commit();
				AlarmUtils.setAlarm(getActivity(), repetition);
				imgSwitch1.setImageResource(checked1 ? R.drawable.switch_on : R.drawable.switch_off);
				LOG.d("repetition = " + repetition);
			}
		});

		imgSwitch2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checked2 = !checked2;
				if (checked2)
					repetition = repetition | 1;
				else
					repetition = repetition & 126;
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("alarm_repetition", repetition).commit();
				AlarmUtils.setAlarm(getActivity(), repetition);
				imgSwitch2.setImageResource(checked2 ? R.drawable.switch_on : R.drawable.switch_off);
				LOG.d("repetition = " + repetition);
			}
		});
		imgSwitch3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checked3 = !checked3;
				if (checked3)
					repetition = repetition | 2;
				else
					repetition = repetition & 125;
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("alarm_repetition", repetition).commit();
				AlarmUtils.setAlarm(getActivity(), repetition);
				imgSwitch3.setImageResource(checked3 ? R.drawable.switch_on : R.drawable.switch_off);
				LOG.d("repetition = " + repetition);
			}
		});
		imgSwitch4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checked4 = !checked4;
				if (checked4)
					repetition = repetition | 4;
				else
					repetition = repetition & 123;
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("alarm_repetition", repetition).commit();
				AlarmUtils.setAlarm(getActivity(), repetition);
				imgSwitch4.setImageResource(checked4 ? R.drawable.switch_on : R.drawable.switch_off);
				LOG.d("repetition = " + repetition);
			}
		});
		imgSwitch5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checked5 = !checked5;
				if (checked5)
					repetition = repetition | 8;
				else
					repetition = repetition & 119;
				PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt("alarm_repetition", repetition).commit();
				AlarmUtils.setAlarm(getActivity(), repetition);
				imgSwitch5.setImageResource(checked5 ? R.drawable.switch_on : R.drawable.switch_off);
				LOG.d("repetition = " + repetition);
			}
		});
		LOG.d("repetition = " + repetition);
		settingsHeight = Util.dp2px(220);		
	}

	@Override
	public void initStrings() {
		super.initStrings();
		try {
			((TextView) getActivity().findViewById(R.id.textAbout))
			.setText(CykelsuperstierApplication
					.getString("about_cykelsuperstier"));
			((TextView) getActivity().findViewById(R.id.textReminders))
			.setTypeface(CykelsuperstierApplication.getBoldFont());
			((TextView) getActivity().findViewById(R.id.textReminders))
			.setText(CykelsuperstierApplication
					.getString("reminder_title"));
			((Button) getActivity().findViewById(R.id.btnStart)).setText("");
			final TextView textMonday = (TextView) getView().findViewById(R.id.textMonday);
			final TextView textTuesday = (TextView) getView().findViewById(R.id.textTuesday);
			final TextView textWednesday = (TextView) getView().findViewById(R.id.textWednesday);
			final TextView textThursday = (TextView) getView().findViewById(R.id.textThursday);
			final TextView textFriday = (TextView) getView().findViewById(R.id.textFriday);
			
			textMonday.setTypeface(CykelsuperstierApplication.getNormalFont());
			textMonday.setText(CykelsuperstierApplication.getString("monday"));
			textTuesday.setTypeface(CykelsuperstierApplication.getNormalFont());
			textTuesday.setText(CykelsuperstierApplication.getString("tuesday"));
			textWednesday.setTypeface(CykelsuperstierApplication.getNormalFont());
			textWednesday.setText(CykelsuperstierApplication.getString("wednesday"));
			textThursday.setTypeface(CykelsuperstierApplication.getNormalFont());
			textThursday.setText(CykelsuperstierApplication.getString("thursday"));
			textFriday.setTypeface(CykelsuperstierApplication.getNormalFont());
			textFriday.setText(CykelsuperstierApplication.getString("friday"));
			
			textPath.setTypeface(CykelsuperstierApplication.getNormalFont());
			textPath.setText(CykelsuperstierApplication.getString("marker_type_1"));
			textService.setTypeface(CykelsuperstierApplication.getNormalFont());
			textService.setText(CykelsuperstierApplication.getString("marker_type_2"));
			textStrain.setTypeface(CykelsuperstierApplication.getNormalFont());
			textStrain.setText(CykelsuperstierApplication.getString("marker_type_3"));
			textMetro.setTypeface(CykelsuperstierApplication.getNormalFont());
			textMetro.setText(CykelsuperstierApplication.getString("marker_type_4"));
			textLocalTrain.setTypeface(CykelsuperstierApplication.getNormalFont());
			textLocalTrain.setText(CykelsuperstierApplication.getString("marker_type_5"));
			
		} catch (Exception e) {

		}
	}

	@Override
	protected AddFavoriteFragment getAddFavoriteFragment() {
		return new AddFavoriteFragment();
	}

	@Override
	protected EditFavoriteFragment getEditFavoriteFragment() {
		return new EditFavoriteFragment();
	}

	private void expandReminders() {
		remindersSettingsContainer.setVisibility(View.VISIBLE);
		remindersSettingsContainer.measure(0, 0);
		TranslateAnimation animationRemindersHeader = new TranslateAnimation(0,
				0, 0, -favoritesContainerHeight);
		animationRemindersHeader.setFillAfter(true);
		animationRemindersHeader.setFillBefore(true);
		animationRemindersHeader.setDuration(500);
		TranslateAnimation animation2 = new TranslateAnimation(0, 0, 0,
				-(favoritesContainerHeight - settingsHeight));
		animation2.setDuration(500);
		TranslateAnimation animation3 = new TranslateAnimation(0, 0, 0,
				-favoritesContainerHeight);
		animation3.setDuration(500);
		animationRemindersHeader.setFillAfter(true);
		animationRemindersHeader.setFillBefore(true);
		animationRemindersHeader.setDuration(500);
		TranslateAnimation animation4 = new TranslateAnimation(0, 0,
				Util.dp2px(40), Util.dp2px(50));
		animation4.setDuration(500);
		animationRemindersHeader.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				favoritesContainer.setVisibility(View.INVISIBLE);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.BELOW,
						favoritesHeaderContainer.getId());
				params.topMargin = Util.dp2px(1);
				remindersContainer.setLayoutParams(params);
				params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						menuItemHeight);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.BELOW,
						remindersSettingsContainer.getId());
				params.topMargin = Util.dp2px(1);
				profileContainer.setLayoutParams(params);
				params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.BELOW, remindersContainer.getId());
				remindersSettingsContainer.clearAnimation();
				remindersSettingsContainer.setLayoutParams(params);
				remindersContainer.clearAnimation();
				getView().findViewById(R.id.horizontalDivider4)
				.clearAnimation();
				((ImageView) getView().findViewById(R.id.imgExpand))
				.setImageResource(R.drawable.notification_arrow_up);
				isAnimationStarted = false;
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
			}

			@Override
			public void onAnimationStart(Animation arg0) {
				isAnimationStarted = true;
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				if (favoritesContainerHeight <= Util.dp2px(140)) {
					params.addRule(RelativeLayout.ALIGN_TOP, getActivity()
							.findViewById(R.id.profileContainer).getId());
					params.topMargin = Util.dp2px(-65);
				} else
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				remindersSettingsContainer.setLayoutParams(params);
			}
		});

		AnimationSet as = new AnimationSet(true);
		as.addAnimation(animationRemindersHeader);
		as.addAnimation(animation2);
		as.addAnimation(animation3);
		remindersContainer.setAnimation(animationRemindersHeader);
		remindersSettingsContainer.setAnimation(animation3);
		profileContainer.setAnimation(animation2);
		aboutContainer.setAnimation(animation2);
		settingsContainer.startAnimation(animation2);
		getView().findViewById(R.id.horizontalDivider2)
		.setAnimation(animation2);
		getView().findViewById(R.id.horizontalDivider3)
		.setAnimation(animation2);
		getView().findViewById(R.id.horizontalDivider5)
		.setAnimation(animation2);
		if (favoritesContainerHeight > Util.dp2px(140)) {
			getView().findViewById(R.id.overlayView).setAnimation(animation2);
			getView().findViewById(R.id.overlayView)
			.setVisibility(View.VISIBLE);
		} else {
			getView().findViewById(R.id.overlayView).setVisibility(View.GONE);
			getView().findViewById(R.id.overlayView2).setAnimation(animation4);
			as.addAnimation(animation4);
		}
		as.start();

		if (getView().findViewById(R.id.btnDone).getVisibility() == View.VISIBLE) {
			getView().findViewById(R.id.btnDone).setVisibility(View.GONE);
			wasBtnDoneVisible = true;
		} else {
			getView().findViewById(R.id.btnEditFavourites).setVisibility(
					View.GONE);
			wasBtnDoneVisible = false;
		}
	}

	private void colapseReminders() {
		int offset = 0;
		if (Util.getDensity() >= 2.5f)
			offset = -Util.dp2px(20);
		TranslateAnimation animationRemindersHeader = new TranslateAnimation(0,
				0, 0, favoritesContainerHeight + offset);
		animationRemindersHeader.setFillAfter(true);
		animationRemindersHeader.setFillBefore(true);
		animationRemindersHeader.setDuration(500);
		TranslateAnimation animation2 = new TranslateAnimation(0, 0, 0,
				(favoritesContainerHeight - settingsHeight) + Util.dp2px(20));
		animation2.setDuration(500);
		TranslateAnimation animation4 = new TranslateAnimation(0, 0,
				-Util.dp2px(100), -Util.dp2px(40));
		animation4.setDuration(500);
		animationRemindersHeader.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.BELOW, favoritesContainer.getId());
				params.topMargin = Util.dp2px(1);
				remindersContainer.setLayoutParams(params);
				params = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT, Util
						.dp2px(40));
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.BELOW, remindersContainer.getId());
				params.topMargin = 0;// Util.dp2px(1);
				profileContainer.setLayoutParams(params);
				remindersSettingsContainer.setVisibility(View.GONE);
				((ImageView) getView().findViewById(R.id.imgExpand))
				.setImageResource(R.drawable.notification_arrow_down);
				isAnimationStarted = false;
				remindersContainer.clearAnimation();
				remindersSettingsContainer.clearAnimation();
				getView().findViewById(R.id.horizontalDivider4)
				.clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
			}

			@Override
			public void onAnimationStart(Animation arg0) {
				isAnimationStarted = true;
				favoritesContainer.setVisibility(View.VISIBLE);
			}
		});
		remindersContainer.startAnimation(animationRemindersHeader);
		remindersSettingsContainer.startAnimation(animationRemindersHeader);
		// getView().findViewById(R.id.horizontalDivider4).startAnimation(animationRemindersHeader);
		profileContainer.startAnimation(animation2);
		aboutContainer.startAnimation(animation2);
		settingsContainer.startAnimation(animation2);
		getView().findViewById(R.id.horizontalDivider2).startAnimation(
				animation2);
		getView().findViewById(R.id.horizontalDivider3).startAnimation(
				animation2);
		getView().findViewById(R.id.horizontalDivider5).startAnimation(
				animation2);
		if (favoritesContainerHeight > Util.dp2px(140)) {
			getView().findViewById(R.id.overlayView).startAnimation(animation2);
			getView().findViewById(R.id.overlayView2)
			.startAnimation(animation2);
			getView().findViewById(R.id.overlayView)
			.setVisibility(View.VISIBLE);
			// getView().findViewById(R.id.overlayView2).setVisibility(View.VISIBLE);
		} else {
			getView().findViewById(R.id.overlayView).setVisibility(View.GONE);
			getView().findViewById(R.id.overlayView2)
			.startAnimation(animation4);
			// getView().findViewById(R.id.overlayView2).setVisibility(View.GONE);
		}

		if (wasBtnDoneVisible) {
			getView().findViewById(R.id.btnDone).setVisibility(View.VISIBLE);
			getView().findViewById(R.id.btnEditFavourites).setVisibility(
					View.INVISIBLE);
		} else {
			getView().findViewById(R.id.btnDone).setVisibility(View.INVISIBLE);
			getView().findViewById(R.id.btnEditFavourites).setVisibility(
					View.VISIBLE);
		}

	}

	@Override
	protected FavoritesAdapter getAdapter() {
		return new FavoritesAdapter(getActivity(), favorites, this);
	}

	@Override
	protected int getAddFavoriteTextColor() {
		return Color.rgb(243, 109, 0);
	}

	@Override
	public int getFavoritesVisibleItemCount() {
		return Util.getDensity() >= 2 ? 7 : 6;
	}

	@Override
	protected int getHintEnabledTextColor() {
		return getActivity().getResources().getColor(R.color.TextDarkGrey);
	}

	@Override
	protected int getHintDisabledTextColor() {
		return getActivity().getResources().getColor(R.color.TextDarkGrey);
	}

	@Override
	protected int getMenuItemsCount() {
		return 6;
	}

}
