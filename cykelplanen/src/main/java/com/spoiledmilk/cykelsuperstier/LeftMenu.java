// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.spoiledmilk.cykelsuperstier.favorites.AddFavoriteFragment;
import com.spoiledmilk.cykelsuperstier.favorites.EditFavoriteFragment;
import com.spoiledmilk.cykelsuperstier.favorites.FavoritesAdapter;
import com.spoiledmilk.cykelsuperstier.map.OverlaysActivity;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenuItem;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingWelcomeActivity;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import io.realm.Realm;

public class LeftMenu extends com.spoiledmilk.ibikecph.LeftMenu {

	boolean remindersExpanded = false;
	boolean wasBtnDoneVisible = false;
	LinearLayout remindersContainer;
	LinearLayout remindersSettingsContainer;

	int repetition;
	int settingsHeight = 0;
	boolean isAnimationStarted = false;
	boolean checked1 = false, checked2 = false, checked3 = false, checked4 = false, checked5 = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View ret = super.onCreateView(inflater, container, savedInstanceState);

        this.menuItems.add(new LeftMenuItem("overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
		/*
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
        */

		return ret;
	}

	@Override
	public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), com.spoiledmilk.cykelsuperstier.AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	public void spawnTTSSettingsActivity() {
		Log.i("JC", "CP: Spawning TTS settings");
		super.spawnTTSSettingsActivity();
	}


	public void spawnOverlaysActivity() {
		Log.d("JC", "CP: Spawning overlays");
        Intent i = new Intent(getActivity(), OverlaysActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	public void spawnFavoritesListActivity() {
		Log.i("JC", "CP: Spawning Favorites settings");
		super.spawnFavoritesListActivity();
	}

	public void spawnLoginActivity() {
		super.spawnLoginActivity();
	}

    public void spawnTrackingActivity() {
        Intent i;
        IbikePreferences settings = IbikeApplication.getSettings();
        if (!settings.getTrackingEnabled() &&
                Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class).size() == 0) {
            i = new Intent(getActivity(), TrackingWelcomeActivity.class);
        } else {
            i = new Intent(getActivity(), TrackingActivity.class);
        }
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

		/*
		remindersContainer = (LinearLayout) getView().findViewById(R.id.remindersContainer);
		remindersSettingsContainer = (LinearLayout) getView().findViewById(R.id.remindersSettingsContainer);

        getView().findViewById(R.id.overlaysContainer).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent i = new Intent(getActivity(), OverlaysActivity.class);
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

		*/
		LOG.d("repetition = " + repetition);
		settingsHeight = Util.dp2px(220);
	}

	@Override
	public void initStrings() {
		super.initStrings();
		try {
			/*
            ((TextView) getActivity().findViewById(R.id.textOverlays))
                    .setText("Dummy"); // TODO: get real title
			((TextView) getActivity().findViewById(R.id.textReminders))
			.setTypeface(CykelsuperstierApplication.getBoldFont());
			((TextView) getActivity().findViewById(R.id.textReminders))
			.setText(CykelsuperstierApplication
					.getString("reminder_title"));
			((Button) getActivity().findViewById(R.id.btnStart)).setText("");
			*/

			/*
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
			*/
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
