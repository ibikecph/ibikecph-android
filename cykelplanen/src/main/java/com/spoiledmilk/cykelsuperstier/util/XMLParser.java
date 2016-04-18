// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.util;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

import com.spoiledmilk.cykelsuperstier.break_rote.TimetableData;

public class XMLParser {

	public static String[] getAttributesForCount(InputStream is,
			String attributeName, int count) {
		XmlPullParser parser = Xml.newPullParser();
		String[] ret = new String[count];
		try {
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(is, null);
			parser.nextTag();
			int currentCount = 0;
			while (parser.next() != XmlPullParser.END_DOCUMENT
					&& currentCount != count) {
				for (int i = 0; i < parser.getAttributeCount(); i++)
					if (parser.getAttributeName(i).equals(attributeName)) {
						ret[currentCount++] = parser.getAttributeValue(i);
						if (currentCount == count)
							break;
					}
			}
		} catch (Exception e) {
			Log.e("XMLParser", e.getLocalizedMessage());
		}
		return ret;
	}

	public static ArrayList<TimetableData> getTimetableData(InputStream is,
			String startStation, String endStation) {
		XmlPullParser parser = Xml.newPullParser();
		ArrayList<TimetableData> ret = new ArrayList<TimetableData>();
		try {
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(is, null);
			parser.nextTag();
			TimetableData td = null;
			while (parser.next() != XmlPullParser.END_DOCUMENT
					&& ret.size() < 3) {
				if (parser.getName() != null
						&& parser.getName().contains("Origin")) {
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeName(i).contains("name")
								&& !parser.getAttributeValue(i).contains(
										startStation))
							break;
						if (parser.getAttributeName(i).contains("type")
								&& !parser.getAttributeValue(i).contains("ST"))
							break;
						if (parser.getAttributeName(i).contains("time")) {
							td = new TimetableData(parser.getAttributeValue(i),
									"");
							break;
						}
					}

				} else if (parser.getName() != null
						&& parser.getName().contains("Destination")) {
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeName(i).contains("name")
								&& !parser.getAttributeValue(i).contains(
										endStation))
							break;
						if (parser.getAttributeName(i).contains("type")
								&& !parser.getAttributeValue(i).contains("ST"))
							break;
						if (parser.getAttributeName(i).contains("time")) {
							td.setArrivalTime(parser.getAttributeValue(i));
							if (ret.size() == 0
									|| (ret.size() > 0 && !ret
											.get(ret.size() - 1)
											.getDepartureTime()
											.equals(td.getDepartureTime())))
								ret.add(td);
							break;
						}
					}

				}
			}
		} catch (Exception e) {
			Log.e("XMLParser", e.getLocalizedMessage());
		}
		return ret;
	}
}
