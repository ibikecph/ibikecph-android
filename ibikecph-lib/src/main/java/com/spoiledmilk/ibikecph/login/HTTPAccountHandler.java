// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;

public class HTTPAccountHandler {

	public static final int GET_USER = 0;
	public static final int PUT_USER = 1;
	public static final int ERROR = 2;
	public static final int DELETE_USER = 3;
	public static final int REGISTER_USER = 4;

	public static Message performLogin(final UserData userData) {
		Message message = new Message();
		JsonNode result = null;
		JSONObject jsonPOST = new JSONObject();
		JSONObject jsonUser = new JSONObject();
		
		try {
			jsonUser.put("password", userData.getPassword());
			jsonUser.put("email", userData.getEmail());
			jsonPOST.put("user", jsonUser);
			result = HttpUtils.postToServer(Config.API_SERVER_LOGIN, jsonPOST);
			message = HttpUtils.JSONtoMessage(result);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
		}
		return message;
	}

	public static Message performFacebookLogin(String fbToken) {

		Message message = new Message();
		try {

			JsonNode result = null;
			JSONObject jsonPOST = new JSONObject();
			JSONObject jsonUser = new JSONObject();

			jsonUser.put("fb_token", fbToken);
			jsonPOST.put("user", jsonUser);
			
			result = HttpUtils.postToServer(Config.API_SERVER_LOGIN, jsonPOST);
			message = HttpUtils.JSONtoMessage(result);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
		}
		return message;
	}

	public static Message performRegister(final UserData userData, Context context) {
		Message message = new Message();
		JsonNode result = null;
		JSONObject jsonPOST = new JSONObject();
		JSONObject jsonUser = new JSONObject();
		JSONObject jsonImagePath = new JSONObject();
		try {
			jsonImagePath.put("filename", userData.getImageName());
			jsonImagePath.put("original_filename", userData.getImageName());
			jsonImagePath.put("file", userData.getBase64Image());
			jsonUser.put("name", userData.getName());
			jsonUser.put("email", userData.getEmail());
			jsonUser.put("email_confirmation", userData.getEmail());
			jsonUser.put("password", userData.getPassword());
			jsonUser.put("password_confirmation", userData.getPassword());
			if (userData.getBase64Image() != null && !userData.getBase64Image().trim().equals(""))
				jsonUser.put("image_path", jsonImagePath);
			jsonUser.put("account_source", context.getResources().getString(R.string.account_source));
			jsonPOST.put("user", jsonUser);
	
			result = HttpUtils.postToServer(Config.API_SERVER_REGISTER, jsonPOST);
			message = HttpUtils.JSONtoMessage(result);
			message.getData().putInt("type", REGISTER_USER);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
			message.getData().putInt("type", ERROR);
		}
		return message;
	}

	public static Message performGetUser(final UserData userData) {
		Message message = new Message();
		JsonNode result = null;
		JSONObject jsonPOST = new JSONObject();
		try {
			LOG.d("facebook api token get = " + userData.getAuth_token());
			jsonPOST.put("auth_token", userData.getAuth_token());
			result = HttpUtils.getFromServer(Config.API_SERVER_REGISTER + "/" + userData.getId() + "?auth_token="
					+ userData.getAuth_token());
			message = HttpUtils.JSONtoUserDataMessage(result, userData);
			message.getData().putInt("type", GET_USER);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
			message.getData().putInt("type", ERROR);
		}
		return message;
	}
	
	// Updates the user account details
	public static Message performPutUser(UserData userData) {
		Message message = new Message();
		JsonNode result = null;
		JSONObject jsonPOST = new JSONObject();
		JSONObject jsonUser = new JSONObject();
		JSONObject jsonImagePath = new JSONObject();
		try {

			jsonUser.put("name", userData.getName());
			jsonUser.put("email", userData.getEmail());
			jsonUser.put("password", userData.getPassword());
			jsonUser.put("password_confirmation", userData.getPassword());
			if (userData.getBase64Image() != null && !userData.getBase64Image().trim().equals("")) {
				jsonImagePath.put("filename", userData.getImageName());
				jsonImagePath.put("original_filename", userData.getImageName());
				jsonImagePath.put("file", userData.getBase64Image());
				jsonUser.put("image_path", jsonImagePath);
			}
			jsonPOST.put("user", jsonUser);
			jsonPOST.put("id", userData.getId());
			jsonPOST.put("auth_token", userData.getAuth_token());

			result = HttpUtils.putToServer(Config.API_SERVER_REGISTER, jsonPOST);
			message = HttpUtils.JSONtoMessage(result);
			message.getData().putInt("type", PUT_USER);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
			message.getData().putInt("type", ERROR);
		}
		return message;
	}

	public static Message performDeleteUser(UserData userData) {
		Message message = new Message();
		JsonNode result = null;
		JSONObject jsonPOST = new JSONObject();
		try {

			jsonPOST.put("auth_token", userData.getAuth_token());
			LOG.d("fb auth token = " + userData.getAuth_token());
			result = HttpUtils.deleteFromServer(Config.API_SERVER_REGISTER + "/" + userData.getId(), jsonPOST);
			message = HttpUtils.JSONtoMessage(result);
			message.getData().putInt("type", DELETE_USER);
		} catch (JSONException e) {
			LOG.e(e.getLocalizedMessage());
			message.getData().putInt("type", ERROR);
		}
		return message;
	}

	public static void checkIsFbTokenValid(final String token, final FBLoginListener listener) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean ret = false;
				try {
					JsonNode response = HttpUtils.get("https://graph.facebook.com/me?access_token=" + token);
					if (response != null) {
						LOG.d("fb graph response = " + response);
						if (response.has("error")) {
							ret = false;
						} else if (response.has("id")) {
							ret = true;
						}
					}
				} catch (Exception e) {

				} finally {
					if (ret) {
						listener.onFBLoginSuccess(token);
					} else {
						listener.onFBLoginError();
					}
				}
			}
		}).start();

	}

}
