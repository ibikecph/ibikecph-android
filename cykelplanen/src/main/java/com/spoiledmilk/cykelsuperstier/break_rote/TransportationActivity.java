// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.break_rote;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.spoiledmilk.cykelsuperstier.Config;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.util.XMLParser;
import com.spoiledmilk.ibikecph.util.LOG;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

public class TransportationActivity extends Activity {

	private String fromStation = "";
	private String toStation = "";
	private String line = "";
	// private String lineB;
	private double destX, destY;
	private int minutesToAStation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transportation);
		((ImageButton) findViewById(R.id.btnBack))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
						overridePendingTransition(R.anim.slide_in_left,
								R.anim.slide_out_right);
					}
				});
		((TextView) findViewById(R.id.textTitle)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.textTitle)).setGravity(Gravity.CENTER);
		if (getIntent().hasExtra("fromStation"))
			fromStation = getIntent().getExtras().getString("fromStation");
		if (getIntent().hasExtra("toStation"))
			toStation = getIntent().getExtras().getString("toStation");
		if (getIntent().hasExtra("line"))
			line = getIntent().getExtras().getString("line");
		// if (getIntent().hasExtra("lineB"))
		// lineB = getIntent().getExtras().getString("lineB");
		if (getIntent().hasExtra("destX"))
			destX = getIntent().getExtras().getDouble("destX");
		if (getIntent().hasExtra("destY"))
			destY = getIntent().getExtras().getDouble("destY");
		if (getIntent().hasExtra("timeToAStation"))
			minutesToAStation = getIntent().getExtras()
					.getInt("timeToAStation");

		if (minutesToAStation < 0)
			minutesToAStation = 20;// safety check when time hasn't been
									// calculated
		if ((LocalTrainData.hasLine(line, this)))
			getStationsData();
		else
			getTimetableFromRejsplanen();

	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		((TextView) findViewById(R.id.textFrom))
				.setText(CykelsuperstierApplication.getString("from") + ":");
		((TextView) findViewById(R.id.textFromStation))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textFromStation)).setText(fromStation
				+ " st");
		((TextView) findViewById(R.id.textToStation))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textToStation))
				.setText(toStation + " st");
		String currentTime = "";
		Calendar calendar = Calendar.getInstance();
		int hours = calendar.get(Calendar.HOUR_OF_DAY);
		int mins = calendar.get(Calendar.MINUTE) + minutesToAStation;
		hours += mins / 60;
		mins = mins % 60;
		currentTime += calendar.get(Calendar.DAY_OF_MONTH) + ". "
				+ monthString(calendar.get(Calendar.MONTH)) + " "
				+ calendar.get(Calendar.YEAR) + ", "
				+ CykelsuperstierApplication.getString("departure") + ". "
				+ CykelsuperstierApplication.getString("at") + ". "
				+ (hours < 10 ? "0" + hours : hours + "") + ":"
				+ (mins < 10 ? "0" + mins : "" + mins);
		((TextView) findViewById(R.id.textCurrentTime))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textCurrentTime)).setText(currentTime);
		((TextView) findViewById(R.id.textTo))
				.setText(CykelsuperstierApplication.getString("to") + ":");
		((TextView) findViewById(R.id.textTime))
				.setText(CykelsuperstierApplication.getString("time") + ":");
		((TextView) findViewById(R.id.textDeparture))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textDeparture))
				.setText(CykelsuperstierApplication.getString("departure")
						+ ".");
		((TextView) findViewById(R.id.textArrival))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textArrival))
				.setText(CykelsuperstierApplication.getString("arrival") + ".");
		((TextView) findViewById(R.id.textArrivalTime))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textArrivalTime))
				.setText(CykelsuperstierApplication.getString("time"));
		((TextView) findViewById(R.id.textShift))
				.setTypeface(CykelsuperstierApplication.getBoldFont());
		((TextView) findViewById(R.id.textShift))
				.setText(CykelsuperstierApplication.getString("shift"));
		((TextView) findViewById(R.id.textTitle)).setTextSize(
				TypedValue.COMPLEX_UNIT_SP, 16);
		((TextView) findViewById(R.id.textTitle))
				.setText(CykelsuperstierApplication
						.getString("recommended_routes"));
	}

	private String monthString(final int i) {
		return CykelsuperstierApplication.getString("month_" + (i - 1));
	}

	private void getStationsData() {
		ArrayList<ITransportationInfo> list = null;
		// Log.d("line = " + line);
		// if (MetroData.hasLine(line, this))
		// list = MetroData.getNext3Arrivals(getMetroLine(line, lineB),
		// fromStation, toStation, this);
		// else if (STrainData.hasLine(line, this)) {
		// String tempLine = "";
		// String[] splittedLines1 = line.split(",");
		// String[] splittedLines2 = lineB.split(",");
		// for (int i = 0; i < splittedLines1.length; i++)
		// for (int j = 0; j < splittedLines2.length; j++)
		// if (splittedLines1[i].equals(splittedLines2[j])) {
		// if (tempLine.equals(""))
		// tempLine += splittedLines1[i];
		// else
		// tempLine += "," + splittedLines1[i];
		// }
		// list = STrainData.getNext3Arrivals(fromStation, toStation, tempLine,
		// this);
		// } else if (LocalTrainData.hasLine(line, this))
		list = LocalTrainData.getNext3Arrivals(fromStation, toStation, line,
				this);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				int hours = list.get(i).getTime() / 60;
				if (hours > 23)
					hours -= 24;
				int minutes = list.get(i).getTime() % 60;
				String time = (hours < 10 ? "0" + hours : "" + hours) + ":"
						+ (minutes < 10 ? "0" + minutes : "" + minutes);
				if (i == 0) {
					((TextView) findViewById(R.id.textDeparture1)).setText(list
							.get(i).getArrivalTime());
					((TextView) findViewById(R.id.textArrival1)).setText(list
							.get(i).getDestinationTime());
					((TextView) findViewById(R.id.textTime1)).setText(time);
					((TextView) findViewById(R.id.textShift1)).setText("0");
				} else if (i == 1) {
					((TextView) findViewById(R.id.textDeparture2)).setText(list
							.get(i).getArrivalTime());
					((TextView) findViewById(R.id.textArrival2)).setText(list
							.get(i).getDestinationTime());
					((TextView) findViewById(R.id.textTime2)).setText(time);
					((TextView) findViewById(R.id.textShift2)).setText("0");
				} else if (i == 2) {
					((TextView) findViewById(R.id.textDeparture3)).setText(list
							.get(i).getArrivalTime());
					((TextView) findViewById(R.id.textArrival3)).setText(list
							.get(i).getDestinationTime());
					((TextView) findViewById(R.id.textTime3)).setText(time);
					((TextView) findViewById(R.id.textShift3)).setText("0");
				}
			}
		}
	}

	// private String getMetroLine(String lineA, String lineB) {
	// String ret = line;
	// String[] splitted = line.split("/");
	// if (splitted != null && splitted.length > 1) {
	// if (lineB.contains(splitted[0]))
	// ret = splitted[0].trim();
	// else if (lineB.contains(splitted[1]))
	// ret = splitted[1].trim();
	// }
	// return ret;
	// }

	private void getTimetableFromRejsplanen() {
		findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream is = null;
				try {
					String urlString = Config.rejsplanenUrl
							+ "/location?input="
							+ URLEncoder.encode(fromStation, "utf-8");
					LOG.d("Rejsplanen request, url = " + urlString);
					URL url = null;
					url = new URL(urlString);
					is = url.openStream();
					String id = XMLParser.getAttributesForCount(is, "id", 1)[0];
					Calendar cal = Calendar.getInstance();
					// urlString = Config.rejsplanenUrl + "/departureBoard?id="
					// + id + "&date=" +
					// cal.get(Calendar.DAY_OF_MONTH) + "."
					// + (cal.get(Calendar.MONTH) + 1) + "." +
					// cal.get(Calendar.YEAR) + "&time=" +
					// cal.get(Calendar.HOUR_OF_DAY) + ":"
					// + cal.get(Calendar.MINUTE) + "&useBus=0";
					// LOG.d("Rejsplanen request, url = " + urlString);
					// url = new URL(urlString);
					// is = url.openStream();
					// String[] times = XMLParser.getAttributesForCount(is,
					// "time", 3);
					// for (int i = 0; i < times.length; i++)
					// LOG.d("Rejsplanen time = " + times[i]);

					String formattedX = "" + destX;
					formattedX = formattedX.replace(".", "");
					if (formattedX.length() > 8)
						formattedX = formattedX.substring(0, 7);
					if (formattedX.length() < 8) {
						String append = "";
						for (int i = formattedX.length(); i < 8; i++)
							append = append + "0";
						formattedX += append;
					}
					String formattedY = "" + destY;
					formattedY = formattedY.replace(".", "");
					if (formattedY.length() > 8)
						formattedY = formattedY.substring(0, 7);
					if (formattedY.length() < 8) {
						String append = "";
						for (int i = formattedY.length(); i < 8; i++)
							append = append + "0";
						formattedY += append;
					}
					int hours = cal.get(Calendar.HOUR_OF_DAY);
					int minutes = cal.get(Calendar.MINUTE) + minutesToAStation
							+ 10; // +10 mins offset is needed
									// because Rejsplanen sometimes
									// returns the time a few mins
									// before the current
					hours += minutes / 60;
					minutes = minutes % 60;
					urlString = Config.rejsplanenUrl + "/trip?originId=" + id
							+ "&destCoordX=" + formattedX + "&destCoordY="
							+ formattedY + "&destCoordName="
							+ URLEncoder.encode(toStation, "utf-8") + "&date="
							+ cal.get(Calendar.DAY_OF_MONTH) + "."
							+ (cal.get(Calendar.MONTH) + 1) + "."
							+ cal.get(Calendar.YEAR) + "&time=" + hours + ":"
							+ minutes + "&useBus=0";
					LOG.d("Rejsplanen request, url = " + urlString);
					url = new URL(urlString);
					is = url.openStream();
					final ArrayList<TimetableData> timetableData = XMLParser
							.getTimetableData(is, fromStation, toStation);
					if (timetableData != null && timetableData.size() > 0)
						TransportationActivity.this
								.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										updateTableData(timetableData);
									}
								});

				} catch (Exception e) {
					if (e != null && e.getLocalizedMessage() != null)
						LOG.e(e.getLocalizedMessage());
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (Exception e) {
							if (e != null && e.getLocalizedMessage() != null)
								LOG.e(e.getLocalizedMessage());
						}
					}
					TransportationActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							findViewById(R.id.progressBar).setVisibility(
									View.GONE);
						}
					});
				}

			}
		}).start();
	}

	private void updateTableData(ArrayList<TimetableData> timetableData) {
		for (int i = 0; i < timetableData.size(); i++) {
			if (i == 0) {
				((TextView) findViewById(R.id.textDeparture1))
						.setText(timetableData.get(i).getDepartureTime());
				((TextView) findViewById(R.id.textArrival1))
						.setText(timetableData.get(i).getArrivalTime());
				((TextView) findViewById(R.id.textTime1)).setText(timetableData
						.get(i).getTime());
				((TextView) findViewById(R.id.textShift1)).setText("0");
			} else if (i == 1) {
				((TextView) findViewById(R.id.textDeparture2))
						.setText(timetableData.get(i).getDepartureTime());
				((TextView) findViewById(R.id.textArrival2))
						.setText(timetableData.get(i).getArrivalTime());
				((TextView) findViewById(R.id.textTime2)).setText(timetableData
						.get(i).getTime());
				((TextView) findViewById(R.id.textShift2)).setText("0");
			} else if (i == 2) {
				((TextView) findViewById(R.id.textDeparture3))
						.setText(timetableData.get(i).getDepartureTime());
				((TextView) findViewById(R.id.textArrival3))
						.setText(timetableData.get(i).getArrivalTime());
				((TextView) findViewById(R.id.textTime3)).setText(timetableData
						.get(i).getTime());
				((TextView) findViewById(R.id.textShift3)).setText("0");
			}
		}
	}
}
