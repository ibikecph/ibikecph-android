// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.util.IbikePreferences.Language;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMDictionary {
	private static final String dict_dir = "strings";

	Context ctx;
	private HashMap<String, Spanned> map;
	private Pattern pattern;

	public SMDictionary(Context ctx) {
		this.ctx = ctx;
	}

	public void init() {
		LOG.d("Dictionary init()");

		pattern = Pattern
				.compile("\\\"([a-zA-Z0-9_\\{\\}\\:]+)\\\"\\s*=\\s*\\\"(([^\\\"]|\\\\\")*)\\\";");

		changeLanguage(IbikeApplication.getSettings().getLanguage());
	}

	public static String getDictFile(Language language) {
		if (language == Language.DAN)
			return "da.strings";
		else
			return "en.strings";
	}

	private void clearMap() {
		if (map != null) {
			map.clear();
			map = null;
		}
	}

	private void loadMapFromFile(String file) {
		map = new HashMap<String, Spanned>();

		AssetManager assetManager = ctx.getAssets();
		InputStream in = null;
		try {
			in = assetManager.open(file);
			BufferedReader buffreader = new BufferedReader(
					new InputStreamReader(in));
			String line = null;
			while ((line = buffreader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					Spanned value = Html.fromHtml(matcher.group(2)
							.replace("\\\"", "\"").replace("\\\'", "\'")
							.replace("\\n", "<br>").replace("\\\\", "\\"));
					map.put(matcher.group(1), value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (in != null)
			try {
				in.close();
			} catch (IOException e) {
			}
	}

	public void changeLanguage(Language lng) {
		clearMap();
		loadMapFromFile(dict_dir + File.separator + getDictFile(lng));
	}

	public Spanned get(String key) {
		if (map == null) {
			LOG.d("dictionary not initialized");
			return null;
		}
		return map.get(key) != null ? map.get(key)
				: new SpannableStringBuilder(key);
	}

}
