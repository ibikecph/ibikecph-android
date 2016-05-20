// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The activity for reporting issues with a route.
 * @author SpoiledMilk
 *
 * TODO: Have this Activity take a SMRoute instead of all this string passing.
 */
public class IssuesActivity extends Activity {

	private Spinner spinner;
	private TextView textOption1, textOption2, textOption3, textOption4, textOption5, textOption6;
	private EditText textComment1, textComment2, textComment3, textComment4, textComment5, textComment6;
	private ImageView imgRadio1, imgRadio2, imgRadio3, imgRadio4, imgRadio5, imgRadio6;
	private TexturedButton btnSend;
	private TextView currentOption = null;
	private EditText currentComment = null;
	private ArrayList<String> turns;
	private String startLoc, endLoc, startName, endName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_issues);
		spinner = (Spinner) findViewById(R.id.spinner);
		Bundle data = getIntent().getExtras();
		turns = data.getStringArrayList("turns");
		startLoc = data.getString("startLoc");
		endLoc = data.getString("endLoc");
		startName = data.getString("startName");
		endName = data.getString("endName");
		IssuesAdapter dataAdapter = new IssuesAdapter(this, turns, R.layout.list_row_issues, R.layout.spinner_layout);
		spinner.setAdapter(dataAdapter);
		textOption1 = (TextView) findViewById(R.id.textOption1);
		textOption2 = (TextView) findViewById(R.id.textOption2);
		textOption3 = (TextView) findViewById(R.id.textOption3);
		textOption4 = (TextView) findViewById(R.id.textOption4);
		textOption5 = (TextView) findViewById(R.id.textOption5);
		textOption6 = (TextView) findViewById(R.id.textOption6);
		textComment1 = (EditText) findViewById(R.id.textComment1);
		textComment2 = (EditText) findViewById(R.id.textComment2);
		textComment3 = (EditText) findViewById(R.id.textComment3);
		textComment4 = (EditText) findViewById(R.id.textComment4);
		textComment5 = (EditText) findViewById(R.id.textComment5);
		textComment6 = (EditText) findViewById(R.id.textComment6);
		imgRadio1 = (ImageView) findViewById(R.id.imgRadio1);
		imgRadio2 = (ImageView) findViewById(R.id.imgRadio2);
		imgRadio3 = (ImageView) findViewById(R.id.imgRadio3);
		imgRadio4 = (ImageView) findViewById(R.id.imgRadio4);
		imgRadio5 = (ImageView) findViewById(R.id.imgRadio5);
		imgRadio6 = (ImageView) findViewById(R.id.imgRadio6);
		btnSend = (TexturedButton) findViewById(R.id.btnSend);
		btnSend.setTextureResource(R.drawable.btn_pattern_repeteable);
		btnSend.setBackgroundResource(R.drawable.btn_blue_selector);
		btnSend.setTextColor(Color.WHITE);

		deselectAll();
	}

	@Override
	public void onResume() {
		super.onResume();
		spinner.setPrompt(IBikeApplication.getString("choose_a_route_step"));

        this.getActionBar().setTitle(IBikeApplication.getString("describe_problem"));
		textOption1.setText(IBikeApplication.getString("report_wrong_address"));
		textOption2.setText(IBikeApplication.getString("report_road_closed"));
		textOption3.setText(IBikeApplication.getString("report_one_way"));
		textOption4.setText(IBikeApplication.getString("report_illegal_turn"));
		textOption5.setText(IBikeApplication.getString("report_wrong_instruction"));
		textOption6.setText(IBikeApplication.getString("report_other"));
		btnSend.setText(IBikeApplication.getString("report_send"));

        // Tell Google Analytics that the user has resumed on this screen.
        IBikeApplication.sendGoogleAnalyticsActivityEvent(this);
        // TODO: Consider if this double event tracking is needed.
        IBikeApplication.sendGoogleAnalyticsEvent(this, "Report", "Start");
	}
	
	// TODO: Don't repeat yourself /jc 
	public void onRadio1Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio1.setImageResource(R.drawable.radio_checked);
		textComment1.setVisibility(View.VISIBLE);
		currentOption = textOption1;
		if (currentComment != null && currentComment.getText() != null)
			textComment1.setText(currentComment.getText().toString());
		currentComment = textComment1;
	}

	public void onRadio2Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio2.setImageResource(R.drawable.radio_checked);
		textComment2.setVisibility(View.VISIBLE);
		currentOption = textOption2;
		if (currentComment != null && currentComment.getText() != null)
			textComment2.setText(currentComment.getText().toString());
		currentComment = textComment2;
	}

	public void onRadio3Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio3.setImageResource(R.drawable.radio_checked);
		textComment3.setVisibility(View.VISIBLE);
		currentOption = textOption3;
		if (currentComment != null && currentComment.getText() != null)
			textComment3.setText(currentComment.getText().toString());
		currentComment = textComment3;
	}

	public void onRadio4Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio4.setImageResource(R.drawable.radio_checked);
		textComment4.setVisibility(View.VISIBLE);
		currentOption = textOption4;
		if (currentComment != null && currentComment.getText() != null)
			textComment4.setText(currentComment.getText().toString());
		currentComment = textComment4;
	}

	public void onRadio5Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio5.setImageResource(R.drawable.radio_checked);
		textComment5.setVisibility(View.VISIBLE);
		currentOption = textOption5;
		if (currentComment != null && currentComment.getText() != null)
			textComment5.setText(currentComment.getText().toString());
		currentComment = textComment5;
	}

	public void onRadio6Click(View v) {
		deselectAll();
		btnSend.setEnabled(true);
		btnSend.setDimmed(false);
		imgRadio6.setImageResource(R.drawable.radio_checked);
		textComment6.setVisibility(View.VISIBLE);
		currentOption = textOption6;
		if (currentComment != null && currentComment.getText() != null) {
			textComment6.setText(currentComment.getText().toString());
		}
		currentComment = textComment6;
	}

	private void deselectAll() {
		imgRadio1.setImageResource(R.drawable.radio_unchecked);
		imgRadio2.setImageResource(R.drawable.radio_unchecked);
		imgRadio3.setImageResource(R.drawable.radio_unchecked);
		imgRadio4.setImageResource(R.drawable.radio_unchecked);
		imgRadio5.setImageResource(R.drawable.radio_unchecked);
		imgRadio6.setImageResource(R.drawable.radio_unchecked);
		textComment1.setVisibility(View.GONE);
		textComment2.setVisibility(View.GONE);
		textComment3.setVisibility(View.GONE);
		textComment4.setVisibility(View.GONE);
		textComment5.setVisibility(View.GONE);
		textComment6.setVisibility(View.GONE);
		btnSend.setEnabled(false);
		btnSend.setDimmed(true);
	}

	public void onButtonSendClick(View v) {
		if (currentOption != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JsonNode response = null;
					JSONObject jsonPost = new JSONObject();
					String auth_token = IBikeApplication.getAuthToken();
					try {
						jsonPost.put("auth_token", auth_token);
						JSONObject jsonIssue = new JSONObject();
						jsonIssue.put("route_segment", spinner.getSelectedItem().toString());
						jsonIssue.put("error_type", currentOption.getText().toString());
						String comment = "";
						comment += IBikeApplication.getString("report_from") + "\n";
						comment += startName + "\n" + startLoc + "\n\n";
						comment += IBikeApplication.getString("report_to") + "\n";
						comment += endName + "\n" + endLoc + "\n\n";
						comment += IBikeApplication.getString("report_reason") + "\n";
						comment += currentOption.getText().toString() + "\n\n";
						comment += (currentComment == null ? "" : currentComment.getText().toString()) + "\n\n";
						comment += spinner.getSelectedItem().toString() + "\n\n";
						comment += IBikeApplication.getString("report_tbt_instructions") + "\n";
						for (String turn : turns) {
							comment += turn + "\n";
						}
						jsonIssue.put("comment", comment);
						jsonPost.put("issue", jsonIssue);
						response = HttpUtils.postToServer(Config.API_URL + "/issues", jsonPost);

                        IBikeApplication.sendGoogleAnalyticsEvent(IssuesActivity.this, "Report", "Completed");
					} catch (JSONException e) {
						LOG.e(e.getLocalizedMessage());
					} finally {
						final JsonNode responseTemp = response;
						IssuesActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String message = IBikeApplication.getString("Error");
								if (responseTemp != null && responseTemp.has("info")) {
									message = responseTemp.get("info").asText();
									LOG.d("issues response message = " + message);
								}
								AlertDialog.Builder builder = new AlertDialog.Builder(IssuesActivity.this);
								builder.setMessage("Besked modtaget");
								builder.setPositiveButton("OK", new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										finish();
										overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
									}
								});
								builder.show();
							}
						});

					}
				}
			}).start();

		}
	}
}
