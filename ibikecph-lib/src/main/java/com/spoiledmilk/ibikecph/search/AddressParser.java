// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;

import com.spoiledmilk.ibikecph.util.LOG;

/**
 * Parses addresses input in the search box.
 * 
 * TODO: Rewrite this to allow for a bit more fuzzyness.
 * @author jens
 *
 */
public class AddressParser {

	public static Address parseAddressRegex(String addressString) {

		// clear trailing spaces and commas
		addressString = trim(addressString);
		Address addr = new Address();

		Range range, rangeN;

		Pattern exp = Pattern.compile("[\\s,](\\d{1,3}[a-zA-Z]?)[\\s,]");

		rangeN = rangeOfFirstMatchInString(addressString, exp);

		// see if we have a house number
		if (rangeN.location != -1) {
			if (rangeN.location >= 0 && rangeN.length > 0 && rangeN.length <= addressString.length()) {
				String s = addressString.substring(rangeN.location, rangeN.location + rangeN.length);
				if (s != null && !s.equals("")) {
					addr.setHouseNumber(trim(s));
				}
			}
		} else {
			exp = Pattern.compile("[\\s,](\\d{1,3}[a-zA-Z]?)$");
			rangeN = rangeOfFirstMatchInString(addressString, exp);
			if (rangeN.location >= 0 && rangeN.length > 0 && rangeN.length <= addressString.length()) {
				String s = addressString.substring(rangeN.location, rangeN.location + rangeN.length);
				if (s != null && !s.equals("")) {
					addr.setHouseNumber(trim(s));

				}
			} else {
				exp = Pattern.compile("^(\\d{1,3}[a-zA-Z]?)[\\s,]+");
				rangeN = rangeOfFirstMatchInString(addressString, exp);
				if (rangeN.location != -1 && rangeN.length > 0 && rangeN.length <= addressString.length()) {
					String s = addressString.substring(rangeN.location, rangeN.location + rangeN.length);
					if (s != null && !s.equals("")) {
						addr.setHouseNumber(trim(s));
					}
				}
			}
		}

		// see if we have a zip
		exp = Pattern.compile("\\d{4}");
		Range rangeZ = rangeOfFirstMatchInString(addressString, exp);
		if (rangeZ.location != -1 && rangeZ.length > 0 && rangeZ.length <= addressString.length()) {
			String s = addressString.substring(rangeZ.location, rangeZ.location + rangeZ.length);
			if (s != null && !s.equals("")) {
				addr.setZip(trim(s));
			}
		}

		int len;
		if (rangeN.location < 0 && rangeZ.location < 0) {
			len = addressString.length();
		} else if (rangeN.location < 0) {
			len = Math.min(rangeZ.location, addressString.length());
		} else if (rangeZ.location < 0) {
			len = Math.min(rangeN.location, addressString.length());
		} else {
			len = Math.min(Math.min(rangeN.location, rangeZ.location), addressString.length());
		}

		// street
		if (len > 0) {
			exp = Pattern.compile("^[\\s,]*([^\\d,]+)");
			Range rangeS = rangeOfFirstMatchInString(addressString, exp, new Range(0, len));
			if (rangeS.location != -1 && rangeS.length > 0 && rangeS.length <= addressString.length()) {
				String s = addressString.substring(rangeS.location, rangeS.location + rangeS.length);
				if (s != null && !s.equals("")) {
					addr.setStreet(trim(s));
				}
			} else {
				exp = Pattern.compile("^[\\s,]*\\d{1,3}[a-zA-Z]?[\\s,]([^\\d,]+)");
				rangeS = rangeOfFirstMatchInString(addressString, exp);
				if (rangeS.location != -1 && rangeS.length > 0 && rangeS.length <= addressString.length()) {
					String s = addressString.substring(rangeS.location, rangeS.location + rangeS.length);
					if (s != null && !s.equals("")) {
						addr.setStreet(trim(s));
					}
				}
			}
		}

		// city
		exp = Pattern.compile("\\d\\w?\\s+(([\\p{L}\\.]\\s*)+)");
		String exp2 = "\\b(kbh|cph)\\.\\s*";
		String exp3 = "\\b(kbh|cph)\\b";
		Range rangeC = rangeOfFirstMatchInString(addressString, exp, new Range(0, addressString.length()));
		if (rangeC.location != -1 && rangeC.length > 0 && rangeC.length <= addressString.length()) {
			String s = addressString.substring(rangeC.location, rangeC.location + rangeC.length);
			exp = Pattern.compile("^(\\d)+(\\w)*");
			range = rangeOfFirstMatchInString(s, exp, new Range(0, s.length()));
			if (range.location != -1) {
				trim(s);
				s = s.replaceAll("^(\\d)+(\\w)*", "");
			}
			if (s != null && !s.equals("")) {
				// addr.city = trim(s);
				final String s1 = trim(s);
				s1.replaceAll(exp2, "København ");
				s1.replaceAll(exp3, "København");
				addr.setCity(s1);
			}
		} else {
			exp = Pattern.compile(",\\s*(([\\p{L}\\.]\\s*)+)");
			rangeC = rangeOfFirstMatchInString(addressString, exp);
			if (rangeC.location != -1 && rangeC.length > 0 && rangeC.length <= addressString.length()) {
				String s = addressString.substring(rangeC.location, rangeC.location + rangeC.length);
				if (s != null && !s.equals("")) {
					// addr.city = trim(s);
					final String s1 = trim(s);
					s1.replaceAll(exp2, "København ");
					s1.replaceAll(exp3, "København");
					addr.setCity(s1);
				}
			}
		}

		LOG.d("parsed address = " + addr);
		return addr;
	}

	public static Range rangeOfFirstMatchInString(String str, Pattern exp) {
		Range ret = new Range();
		Matcher mtchr = exp.matcher(str);
		if (mtchr.find()) {
			String match = mtchr.group();
			ret.location = str.indexOf(match);
			ret.length = match.length();
		}
		return ret;
	}

	public static Range rangeOfFirstMatchInString(String str, Pattern exp, Range range) {
		Range ret = new Range();
		Matcher mtchr = exp.matcher(str);
		if (mtchr.find()) {
			String match = mtchr.group();
			if (!(str.indexOf(match) > range.location + range.length - 1)) {
				ret.location = str.indexOf(match);
				ret.length = match.length();
			}
		}
		return ret;
	}

	public static String trim(String s) {
		String ret = s;
		if (ret.length() > 0) {
			if (ret.charAt(ret.length() - 1) == ',') {
				ret = ret.substring(0, ret.length() - 1);
			}
			if (ret.charAt(0) == ',') {
				ret = ret.substring(1, ret.length());
			}
			ret = ret.trim();
		}
		return ret;
	}

	public static boolean containsNumber(String str) {
		if (str.matches(".*\\d.*")) {
			return true;
		} else {
			return false;
		}
	}

	public static String addresWithoutNumber(String address) {
		String[] splitted = address.split("\\s*(,|\\s)\\s*");
		String number = splitted[splitted.length - 1];
		String tmp = address;
		if (containsNumber(number) && address.length() > 1 && address.indexOf(number) > 0 && address.indexOf(number) < address.length() - 1) {
			tmp = address.substring(0, address.indexOf(number) - 1);
		}
		return tmp.trim();
	}

	public static String numberFromAddress(String address) {
		String ret = null;
		if (address.contains(",")) {
			address = address.substring(0, address.indexOf(','));
		}
		String[] splitted = address.split("\\s*(,|\\s)\\s*");
		try {

			String number = null;
			if (address.length() > 1) {
				// for (int i = 0; i < splitted.length; i++) {
				number = splitted[1];

				if (containsNumber(number) && number.length() < 4) {
					ret = number;
				}
				if (ret == null) {

					number = splitted[splitted.length - 1];

					if (containsNumber(number) && number.length() < 4) {
						ret = number;
					}
				}
				// }
			}
		} catch (Exception e) {

		}

		return ret;
	}

	public static boolean isZipFirst(String address) {
		boolean ret = false;
		String[] splitted = address.trim().split("\\s*(,|\\s)\\s*");
		try {

			if (containsNumber(splitted[0])) {
				ret = true;
			}

		} catch (Exception e) {

		}

		return ret;
	}

	public static String textFromBundle(Bundle bundle) {
		String ret = bundle.getString("name");
		if (bundle.containsKey("isPoi") && bundle.getBoolean("isPoi")) {
			if (bundle.containsKey("address") && !bundle.getString("address").trim().equals(ret.trim())) {
				ret += ", " + bundle.getString("address");
			}
			if (bundle.containsKey("zip")) {
				ret += ", " + bundle.getString("zip");
			}
			if (bundle.containsKey("city")) {
				ret += " " + bundle.getString("city");
			}
		}
		return ret;
	}

	private static class Range {
		public int location;
		public int length;

		public Range(int location, int length) {
			this.location = location;
			this.length = length;
		}

		public Range() {
			location = -1;
			length = -1;
		}
	}
}
