// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.login.HTTPDeleteWithBody;
import com.spoiledmilk.ibikecph.login.UserData;

public class HttpUtils {

	private static final String ACCEPT = "application/vnd.ibikecph.v1";
	private static final int CONNECTON_TIMEOUT = 30000;

	public static JsonResult readLink(String url_string, String method) {
		JsonResult result = new JsonResult();
		if (url_string == null) {
			return result;
		}
		URL url = null;
		HttpURLConnection httpget = null;
		try {
			LOG.d("HttpUtils readlink() " + url_string);
			url = new URL(url_string);
			httpget = (HttpURLConnection) url.openConnection();
			httpget.setDoInput(true);
			httpget.setRequestMethod(method);
			httpget.setRequestProperty("Accept", "application/json");
			httpget.setRequestProperty("Content-type", "application/json");
			httpget.setConnectTimeout(CONNECTON_TIMEOUT);
			httpget.setReadTimeout(CONNECTON_TIMEOUT);
			JsonNode root = null;
			root = Util.getJsonObjectMapper().readValue(httpget.getInputStream(), JsonNode.class);
			if (root != null) {
				result.setNode(root);
			}
		} catch (JsonParseException e) {
			LOG.w("HttpUtils readLink() JsonParseException ", e);
			result.error = JsonResult.ErrorCode.APIError;
		} catch (MalformedURLException e) {
			LOG.w("HttpUtils readLink() MalformedURLException", e);
			result.error = JsonResult.ErrorCode.APIError;
		} catch (FileNotFoundException e) {
			LOG.w("HttpUtils readLink() FileNotFoundException", e);
			result.error = JsonResult.ErrorCode.NotFound;
		} catch (IOException e) {
			LOG.w("HttpUtils readLink() IOException", e);
			result.error = JsonResult.ErrorCode.ConnectionError;
		} finally {
			if (httpget != null) {
				httpget.disconnect();
			}
		}
		LOG.d("HttpUtils readLink() " + (result != null && result.error == JsonResult.ErrorCode.Success ? "succeeded" : "failed"));
		return result;
	}

	public static JsonNode get(String url_string) {
		JsonResult result = readLink(url_string, "GET");
		Log.d("debug", "get:" + result.toString());
		if (result.error == JsonResult.ErrorCode.Success) {
			return result.getNode();
		}
		return null;
	}

	public static class JsonResult {
		private JsonNode node;

		enum ErrorCode {
			Success, ConnectionError, NotFound, APIError, InternalError
		};

		ErrorCode error = ErrorCode.InternalError;

		public JsonNode getNode() {
			return node;
		}

		public void setNode(JsonNode node) {
			this.node = node;
			error = ErrorCode.Success;
		}
	}

	public static JsonNode postToServer(String urlString, JSONObject objectToPost) {
		JsonNode ret = null;
		LOG.d("POST api request, url = " + urlString + " object = " + objectToPost.toString());
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, CONNECTON_TIMEOUT);
		HttpConnectionParams.setSoTimeout(myParams, CONNECTON_TIMEOUT);
		HttpClient httpclient = new DefaultHttpClient(myParams);
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Config.USER_AGENT);
		HttpPost httppost = null;
		URL url = null;
		try {
			url = new URL(urlString);
			httppost = new HttpPost(url.toString());
			httppost.setHeader("Content-type", "application/json");
			httppost.setHeader("Accept", ACCEPT);
			httppost.setHeader("LANGUAGE_CODE", IbikeApplication.getLanguageString());
			StringEntity se = new StringEntity(objectToPost.toString(), HTTP.UTF_8);// , HTTP.UTF_8
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httppost.setEntity(se);
			HttpResponse response = httpclient.execute(httppost);
			String serverResponse = EntityUtils.toString(response.getEntity());
			LOG.d("API response = " + serverResponse);
			ret = Util.stringToJsonNode(serverResponse);
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null) {
				LOG.e(e.getLocalizedMessage());
			}
		}
		return ret;
	}

	public static JsonNode getFromServer(String urlString) {
		JsonNode ret = null;
		LOG.d("GET api request, url = " + urlString);
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, CONNECTON_TIMEOUT);
		HttpConnectionParams.setSoTimeout(myParams, CONNECTON_TIMEOUT);
		HttpClient httpclient = new DefaultHttpClient(myParams);
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Config.USER_AGENT);
		HttpGet httpget = null;
		URL url = null;
		try {

			url = new URL(urlString);
			httpget = new HttpGet(url.toString());
			httpget.setHeader("Content-type", "application/json");
			httpget.setHeader("Accept", ACCEPT);
			httpget.setHeader("LANGUAGE_CODE", IbikeApplication.getLanguageString());
			HttpResponse response = httpclient.execute(httpget);
			String serverResponse = EntityUtils.toString(response.getEntity());
			LOG.d("API response = " + serverResponse);
			ret = Util.stringToJsonNode(serverResponse);
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null) {
				LOG.e(e.getLocalizedMessage());
			}
		}
		return ret;
	}

	public static JsonNode putToServer(String urlString, JSONObject objectToPost) {
		JsonNode ret = null;
		LOG.d("PUT api request, url = " + urlString);
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, CONNECTON_TIMEOUT);
		HttpConnectionParams.setSoTimeout(myParams, CONNECTON_TIMEOUT);
		HttpClient httpclient = new DefaultHttpClient(myParams);
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Config.USER_AGENT);
		HttpPut httput = null;
		URL url = null;
		try {
			url = new URL(urlString);
			httput = new HttpPut(url.toString());
			httput.setHeader("Content-type", "application/json");
			httput.setHeader("Accept", ACCEPT);
			httput.setHeader("LANGUAGE_CODE", IbikeApplication.getLanguageString());
			StringEntity se = new StringEntity(objectToPost.toString(), HTTP.UTF_8);
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httput.setEntity(se);
			HttpResponse response = httpclient.execute(httput);
			String serverResponse = EntityUtils.toString(response.getEntity());
			LOG.d("API response = " + serverResponse);
			ret = Util.stringToJsonNode(serverResponse);
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null) {
				LOG.e(e.getLocalizedMessage());
			}
		}
		return ret;
	}

	public static JsonNode deleteFromServer(String urlString, JSONObject objectToPost) {
		JsonNode ret = null;
		LOG.d("DELETE api request, url = " + urlString);
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, CONNECTON_TIMEOUT);
		HttpConnectionParams.setSoTimeout(myParams, CONNECTON_TIMEOUT);
		HttpClient httpclient = new DefaultHttpClient(myParams);
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Config.USER_AGENT);
		HTTPDeleteWithBody httpdelete = null;
		URL url = null;
		try {
			url = new URL(urlString);
			httpdelete = new HTTPDeleteWithBody(url.toString());
			httpdelete.setHeader("Content-type", "application/json");
			httpdelete.setHeader("Accept", ACCEPT);
			httpdelete.setHeader("LANGUAGE_CODE", IbikeApplication.getLanguageString());
			StringEntity se = new StringEntity(objectToPost.toString(), HTTP.UTF_8);
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			httpdelete.setEntity(se);
			HttpResponse response = httpclient.execute(httpdelete);
			String serverResponse = EntityUtils.toString(response.getEntity());
			LOG.d("API response = " + serverResponse);
			ret = Util.stringToJsonNode(serverResponse);
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null)
				LOG.e(e.getLocalizedMessage());
		}
		return ret;
	}

	public static Message JSONtoMessage(JsonNode result) {
		Message ret = new Message();
		Bundle data = new Bundle();
		if (result != null) {
			data.putBoolean("success", result.get("success").asBoolean());
			data.putString("info", result.get("info").asText());
			if (result.has("errors"))
				data.putString("errors", result.get("errors").asText());
			JsonNode dataNode = result.get("data");
			if (dataNode != null) {
				if (dataNode.has("id"))
					data.putInt("id", dataNode.get("id").asInt());
				if (dataNode.has("auth_token"))
					data.putString("auth_token", dataNode.get("auth_token").asText());
			}
		}
		ret.setData(data);
		return ret;
	}

	public static Message JSONtoUserDataMessage(JsonNode result, UserData userData) {
		Message ret = new Message();
		Bundle data = new Bundle();
		if (result != null) {
			data.putBoolean("success", result.get("success").asBoolean());
			data.putString("info", result.get("info").asText());
			JsonNode dataNode = result.get("data");
			if (dataNode != null) {
				data.putInt("id", dataNode.get("id").asInt());
				data.putString("name", dataNode.get("name").asText());
				data.putString("email", dataNode.get("email").asText());
				if (dataNode.has("image_url"))
					data.putString("image_url", dataNode.get("image_url").asText());
			}
			ret.setData(data);
		}
		return ret;
	}

    public static void openLinkInBrowser(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }
}
