// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.search.HistoryData;
import com.spoiledmilk.ibikecph.search.SearchListItem;

public class DB extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "iBikeCPHDB";
	private static final String TABLE_SEARCH_HISTORY = "SearchHistory";
	private static final String TABLE_FAVORITES = "Favorites";
	private static final String KEY_ID = "_id";
	private static final String KEY_NAME = "Name";
	private static final String KEY_ADDRESS = "address";
	private static final String KEY_START_DATE = "startDate";
	private static final String KEY_END_DATE = "endDate";
	private static final String KEY_SOURCE = "source";
	private static final String KEY_SUBSOURCE = "subsource";
	private static final String KEY_LAT = "lat";
	private static final String KEY_LONG = "long";
	private static final String KEY_API_ID = "apiId";

	public DB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public SQLiteDatabase getReadableDatabase() {
		SQLiteDatabase db = null;
		try {
			db = super.getReadableDatabase();
		} catch (SQLiteException e) {
			LOG.e(e.getLocalizedMessage());
		}
		return db;
	}

	public SQLiteDatabase getWritableDatabase() {
		SQLiteDatabase db = null;
		try {
			db = super.getWritableDatabase();
		} catch (SQLiteException e) {
			LOG.e(e.getLocalizedMessage());
		}
		return db;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SEARCH_HOSTORY_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SEARCH_HISTORY + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
				+ KEY_NAME + " TEXT," + KEY_ADDRESS + " TEXT," + KEY_START_DATE + " TEXT," + KEY_END_DATE + " TEXT," + KEY_SOURCE
				+ " TEXT," + KEY_SUBSOURCE + " TEXT," + KEY_LAT + " REAL," + KEY_LONG + " REAL)";
		db.execSQL(CREATE_SEARCH_HOSTORY_TABLE);
		String CREATE_FAVORITES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME
				+ " TEXT," + KEY_ADDRESS + " TEXT," + KEY_SOURCE + " TEXT," + KEY_SUBSOURCE + " TEXT," + KEY_LAT + " REAL," + KEY_LONG
				+ " REAL, " + KEY_API_ID + " INTEGER DEFAULT -1)";
		db.execSQL(CREATE_FAVORITES_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public ArrayList<SearchListItem> getSearchHistory() {

		ArrayList<SearchListItem> ret = new ArrayList<SearchListItem>();

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_START_DATE, KEY_END_DATE, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG };

		Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, null, null, null, null, KEY_START_DATE + " DESC", null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colStartDate = cursor.getColumnIndex(KEY_START_DATE);
				int colEndDate = cursor.getColumnIndex(KEY_END_DATE);
				int colSource = cursor.getColumnIndex(KEY_SOURCE);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);

				HistoryData hd = new HistoryData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
				if (hd.getName() != null && !hd.getName().trim().equals("")) {
					ret.add(hd);
				}
				if (ret.size() > 10) {
					break;
				}
				cursor.moveToNext();
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public ArrayList<SearchListItem> getSearchHistoryForString(String srchString) {

		ArrayList<SearchListItem> ret = new ArrayList<SearchListItem>();

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_START_DATE, KEY_END_DATE, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG };

		Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, KEY_NAME + " LIKE ? OR " + KEY_ADDRESS + " LIKE ?", new String[] {
				"%" + srchString + "%", "%" + srchString + "%" }, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colStartDate = cursor.getColumnIndex(KEY_START_DATE);
				int colEndDate = cursor.getColumnIndex(KEY_END_DATE);
				int colSource = cursor.getColumnIndex(KEY_SOURCE);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);

				HistoryData hd = new HistoryData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
				if (hd.getName() != null && !hd.getName().trim().equals(""))
					ret.add(hd);
				cursor.moveToNext();
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public SearchListItem getSearchHistoryByName(String name) {

		SearchListItem ret = null;

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_START_DATE, KEY_END_DATE, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG };

		Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, KEY_NAME + " = ? ", new String[] { name.trim() }, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colStartDate = cursor.getColumnIndex(KEY_START_DATE);
				int colEndDate = cursor.getColumnIndex(KEY_END_DATE);
				int colSource = cursor.getColumnIndex(KEY_SOURCE);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);

				HistoryData hd = new HistoryData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
				if (hd.getName() != null && !hd.getName().trim().equals("")) {
					ret = hd;
				}
				break;
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public long saveSearchHistory(HistoryData hd, HistoryData from, Context context) {
		SQLiteDatabase db = this.getWritableDatabase();
		if (db == null)
			return -1;

		String[] columns = { KEY_ID, KEY_NAME };
		long id;
		Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, KEY_NAME + " = ?", new String[] { hd.getName() }, null, null, null, null);
		if (cursor == null || cursor.isAfterLast()) {
			ContentValues values = new ContentValues();
			values.put(KEY_NAME, hd.getName());
			values.put(KEY_ADDRESS, hd.getAdress());
			values.put(KEY_START_DATE, hd.getStartDate());
			values.put(KEY_END_DATE, hd.getEndDate());
			values.put(KEY_SOURCE, hd.getSource());
			values.put(KEY_SUBSOURCE, hd.getSubSource());
			values.put(KEY_LAT, Double.valueOf(hd.getLatitude()));
			values.put(KEY_LONG, Double.valueOf(hd.getLongitude()));
			id = db.insert(TABLE_SEARCH_HISTORY, null, values);
		} else {
			cursor.moveToFirst();
			id = cursor.getInt(cursor.getColumnIndex(KEY_ID));

		}
		if (cursor != null)
			cursor.close();

		db.close();

		if (context != null && from != null)
			postHistoryItemToServer(hd, from, context);

		return id;
	}

	private void postHistoryItemToServer(HistoryData hd, HistoryData from, Context context) {
		if (PreferenceManager.getDefaultSharedPreferences(context).contains("auth_token")) {
			String authToken = PreferenceManager.getDefaultSharedPreferences(context).getString("auth_token", "");
			final JSONObject postObject = new JSONObject();
			try {
				postObject.put("auth_token", authToken);
				JSONObject routeObject = new JSONObject();
				if (from.getName() == null || from.getName().trim().equals("")) {
					from.setName(IbikeApplication.getString("current_position"));
				}
				routeObject.put("from_name", from.getName());
				routeObject.put("from_lattitude", from.getLatitude());
				routeObject.put("from_longitude", from.getLongitude());
				routeObject.put("to_name", hd.getName());
				routeObject.put("to_lattitude", hd.getLatitude());
				routeObject.put("to_longitude", hd.getLongitude());
				routeObject.put("start_date", DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));
				postObject.put("route", routeObject);
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						LOG.d("Server request: " + Config.API_URL + "/routes");
						HttpUtils.postToServer(Config.API_URL + "/routes", postObject);
					}
				});
				thread.start();
			} catch (JSONException e) {
				LOG.e(e.getLocalizedMessage());
			}
		}

	}

	public ArrayList<SearchListItem> getSearchHistoryFromServer(Context context) {
		ArrayList<SearchListItem> ret = new ArrayList<SearchListItem>();
		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			try {
				JsonNode getObject = HttpUtils.getFromServer(Config.API_URL + "/routes?auth_token=" + authToken);
				if (getObject != null) {
					IbikeApplication.setHistoryFetched(true);
					boolean success = getObject.get("success").asBoolean();
					if (success) {
						JsonNode historyList = getObject.get("data");
						for (int i = 0; i < historyList.size(); i++) {
							JsonNode data = historyList.get(i);
							HistoryData hd = new HistoryData(data.get("toName").asText(), data.get("toLattitude").asDouble(), data.get(
									"toLongitude").asDouble());
							if (hd.getName() != null && !hd.getName().trim().equals("")) {
								saveSearchHistory(hd, null, null);
								ret.add(hd);
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
		return ret;
	}

	public long saveFavorite(FavoritesData fd, Context context, boolean spawnThread) {
		SQLiteDatabase db = this.getWritableDatabase();
		if (db == null) {
			LOG.e("db is null in saveFavorite");
			return -1;
		}
		long id;
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, fd.getName());
		values.put(KEY_ADDRESS, fd.getAdress());
		values.put(KEY_SOURCE, fd.getSource());
		values.put(KEY_SUBSOURCE, fd.getSubSource());
		values.put(KEY_LAT, Double.valueOf(fd.getLatitude()));
		values.put(KEY_LONG, Double.valueOf(fd.getLongitude()));
		values.put(KEY_API_ID, fd.getApiId());
		id = db.insert(TABLE_FAVORITES, null, values);
		fd.setId(id);
		db.close();
		if (context != null) {
			postFavoriteToServer(fd, context, spawnThread);
		}
		return id;
	}

	private void postFavoriteToServer(final FavoritesData fd, Context context, boolean spawnThread) {
		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			final JSONObject postObject = new JSONObject();
			try {
				JSONObject favouriteObject = new JSONObject();
				favouriteObject.put("name", fd.getName());
				favouriteObject.put("address", fd.getAdress());
				favouriteObject.put("lattitude", fd.getLatitude());
				favouriteObject.put("longitude", fd.getLongitude());
				favouriteObject.put("source", fd.getSource());
				favouriteObject.put("sub_source", fd.getSubSource());
				postObject.put("favourite", favouriteObject);
				postObject.put("auth_token", authToken);
				if (spawnThread) {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							LOG.d("Server request: " + Config.API_URL + "/favourites");
							JsonNode responseNode = HttpUtils.postToServer(Config.API_URL + "/favourites", postObject);
							if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
								int id = responseNode.get("data").get("id").asInt();
								SQLiteDatabase db = getWritableDatabase();
								if (db == null)
									return;
								String strFilter = "_id=" + fd.getId();
								ContentValues args = new ContentValues();
								args.put(KEY_API_ID, id);
								db.update(TABLE_FAVORITES, args, strFilter, null);
								db.close();
							}
						}
					});
					thread.start();
				} else {
					LOG.d("Server request: " + Config.API_URL + "/favourites");
					JsonNode responseNode = HttpUtils.postToServer(Config.API_URL + "/favourites", postObject);
					if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
						int id = responseNode.get("data").get("id").asInt();
						SQLiteDatabase db = getWritableDatabase();
						if (db == null)
							return;
						String strFilter = "_id=" + fd.getId();
						ContentValues args = new ContentValues();
						args.put(KEY_API_ID, id);
						db.update(TABLE_FAVORITES, args, strFilter, null);
						db.close();
					}
				}
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}

	}

	public ArrayList<FavoritesData> getFavorites(ArrayList<FavoritesData> ret) {
		if (ret == null) {
			ret = new ArrayList<FavoritesData>();
		} else {
			ret.clear();
		}
		SQLiteDatabase db = getReadableDatabase();
		if (db == null) {
			return null;
		}
		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG, KEY_API_ID };
		Cursor cursor = db.query(TABLE_FAVORITES, columns, null, null, null, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);
				int colApiId = cursor.getColumnIndex(KEY_API_ID);
				FavoritesData fd = new FavoritesData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));
				ret.add(fd);
				cursor.moveToNext();
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		db.close();
		LOG.d("favourites count from DB = " + ret.size());
		return ret;
	}

	public ArrayList<FavoritesData> getFavoritesFromServer(Context context, ArrayList<FavoritesData> ret) {
		if (ret == null) {
			ret = new ArrayList<FavoritesData>();
		} else {
			ret.clear();
		}
		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			try {
				JsonNode getObject = HttpUtils.getFromServer(Config.API_URL + "/favourites?auth_token=" + authToken);
				if (getObject != null && getObject.has("data")) {
					SQLiteDatabase db = this.getWritableDatabase();
					if (db != null) {
						db.delete(TABLE_FAVORITES, null, null);
					}
					IbikeApplication.setFavoritesFetched(true);
					JsonNode favoritesList = getObject.get("data");
					for (int i = 0; i < favoritesList.size(); i++) {
						JsonNode data = favoritesList.get(i);
						FavoritesData fd = new FavoritesData(data.get("name").asText(), data.get("address").asText(), data
								.get("sub_source").asText(), data.get("lattitude").asDouble(), data.get("longitude").asDouble(), data.get(
								"id").asInt());
						saveFavorite(fd, null, false);
						// ret.add(fd);
					}
					LOG.d("favorites fetched = " + ret);
				}
			} catch (Exception e) {
				if (e != null && e.getLocalizedMessage() != null) {
					LOG.e(e.getLocalizedMessage());
				}
			}
		}
		getFavorites(ret);
		return ret;
	}

	public void deleteFavorites() {
		SQLiteDatabase db = this.getWritableDatabase();
		if (db == null)
			return;
		db.delete(TABLE_FAVORITES, null, null);
		db.close();
		IbikeApplication.setFavoritesFetched(false);
	}

	public ArrayList<SearchListItem> getFavorites2() {

		ArrayList<SearchListItem> ret = new ArrayList<SearchListItem>();

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG, KEY_API_ID };

		Cursor cursor = db.query(TABLE_FAVORITES, columns, null, null, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);
				int colApiId = cursor.getColumnIndex(KEY_API_ID);

				FavoritesData fd = new FavoritesData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));

				ret.add((SearchListItem) fd);
				cursor.moveToNext();
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public ArrayList<SearchListItem> getFavoritesForString(String srchString) {
		ArrayList<SearchListItem> ret = new ArrayList<SearchListItem>();

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG, KEY_API_ID };

		Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " LIKE ? ", new String[] { "%" + srchString + "%" }, null, null,
				null, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);
				int colApiId = cursor.getColumnIndex(KEY_API_ID);

				FavoritesData fd = new FavoritesData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));

				ret.add((SearchListItem) fd);
				cursor.moveToNext();
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public SearchListItem getFavoriteByName(String name) {
		SearchListItem ret = null;

		SQLiteDatabase db = getReadableDatabase();
		if (db == null)
			return null;

		String[] columns = { KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG, KEY_API_ID };

		Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " = ? ", new String[] { name.trim() }, null, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (cursor != null && !cursor.isAfterLast()) {
				int colId = cursor.getColumnIndex(KEY_ID);
				int colName = cursor.getColumnIndex(KEY_NAME);
				int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
				int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
				int colLat = cursor.getColumnIndex(KEY_LAT);
				int colLong = cursor.getColumnIndex(KEY_LONG);
				int colApiId = cursor.getColumnIndex(KEY_API_ID);

				ret = new FavoritesData(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
						cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));
				break;
			}
		}

		if (cursor != null)
			cursor.close();

		db.close();

		return ret;
	}

	public void updateFavorite(FavoritesData fd, Context context, APIListener listener) {
		SQLiteDatabase db = this.getWritableDatabase();
		if (db == null)
			return;

		ContentValues values = new ContentValues();
		values.put(KEY_NAME, fd.getName());
		values.put(KEY_ADDRESS, fd.getAdress());
		values.put(KEY_SOURCE, fd.getSource());
		values.put(KEY_SUBSOURCE, fd.getSubSource());
		values.put(KEY_LAT, Double.valueOf(fd.getLatitude()));
		values.put(KEY_LONG, Double.valueOf(fd.getLongitude()));
		db.update(TABLE_FAVORITES, values, KEY_ID + " = ?", new String[] { "" + fd.getId() });

		db.close();

		if (context != null)
			updateFavoriteToServer(fd, context, listener);

	}

	public void updateFavorite(FavoritesData fd, Context context) {
		updateFavorite(fd, context, null);
	}

	public void deleteFavorite(FavoritesData fd, Context context) {
		SQLiteDatabase db = this.getWritableDatabase();
		if (db == null)
			return;

		db.delete(TABLE_FAVORITES, KEY_ID + " = ?", new String[] { "" + fd.getId() });

		db.close();

		if (context != null)
			deleteFavoriteFromServer(fd, context);

	}

	// public int getApiId(FavoritesData fd) {
	// int ret = -1;
	// SQLiteDatabase db = getReadableDatabase();
	// if (db != null) {
	// String[] columns = { KEY_ID, KEY_API_ID };
	// Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_ID + " = ? ", new String[] { "" + fd.getId() }, null,
	// null, null, null);
	// if (cursor != null && cursor.moveToFirst()) {
	// int colApiId = cursor.getColumnIndex(KEY_API_ID);
	// ret = cursor.getInt(colApiId);
	// }
	// if (cursor != null)
	// cursor.close();
	// db.close();
	// }
	// return ret;
	// }

	private void updateFavoriteToServer(final FavoritesData fd, Context context, final APIListener listener) {

		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			final JSONObject postObject = new JSONObject();
			try {
				JSONObject favouriteObject = new JSONObject();
				favouriteObject.put("name", fd.getName());
				favouriteObject.put("address", fd.getAdress());
				favouriteObject.put("lattitude", fd.getLatitude());
				favouriteObject.put("longitude", fd.getLongitude());
				favouriteObject.put("source", fd.getSource());
				favouriteObject.put("sub_source", fd.getSubSource());
				postObject.put("favourite", favouriteObject);
				postObject.put("auth_token", authToken);
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						JsonNode node = HttpUtils.putToServer(Config.API_URL + "/favourites/" + fd.getApiId(), postObject);
						if (listener != null) {
							boolean success = false;
							if (node != null && node.has("success") && node.get("success").asBoolean()) {
								success = true;
							}
							listener.onRequestCompleted(success);
						}
					}
				});
				thread.start();
			} catch (JSONException e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
	}

	public void deleteFavoriteFromServer(final FavoritesData fd, Context context) {
		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			final JSONObject postObject = new JSONObject();
			try {
				postObject.put("auth_token", authToken);
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						HttpUtils.deleteFromServer(Config.API_URL + "/favourites/" + fd.getApiId(), postObject);
					}
				});
				thread.start();
			} catch (JSONException e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
	}

	public void saveFinishedRoute(Location startLocation, Location endLocation, String startName, String endName, String startDate,
			String endDate, String visitedLocations) {
		if (IbikeApplication.isUserLogedIn()) {
			String authToken = IbikeApplication.getAuthToken();
			final JSONObject postObject = new JSONObject();
			try {
				postObject.put("auth_token", authToken);
				JSONObject routeObject = new JSONObject();
				routeObject.put("from_name", startName);
				routeObject.put("from_lattitude", startLocation.getLatitude());
				routeObject.put("from_longitude", startLocation.getLongitude());
				routeObject.put("to_name", endName);
				routeObject.put("to_lattitude", endLocation.getLatitude());
				routeObject.put("to_longitude", endLocation.getLongitude());
				routeObject.put("start_date", startDate);
				routeObject.put("end_date", endDate);
				routeObject.put("route_visited_locations", visitedLocations);
				routeObject.put("is_finished", true);
				postObject.put("route", routeObject);

				new Thread(new Runnable() {
					@Override
					public void run() {
						HttpUtils.postToServer(Config.API_URL + "/routes", postObject);
					}
				}).start();

			} catch (JSONException e) {
				LOG.e(e.getLocalizedMessage());
			}
		}

	}

	// public void updateApiIds(ArrayList<FavoritesData> favorites) {
	// Iterator<FavoritesData> it = favorites.iterator();
	// while (it.hasNext()) {
	// FavoritesData fd = it.next();
	// fd.setApiId(getApiId(fd));
	// }
	//
	// }

	public int favoritesForName(String name) {
		int ret = 0;
		SQLiteDatabase db = getReadableDatabase();
		if (db != null) {
			String[] columns = { KEY_NAME };
			Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " = ? ", new String[] { "" + name }, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret = cursor.getCount();
			}
			if (cursor != null)
				cursor.close();
			db.close();
		}
		return ret;
	}

	public int getApiId(int id) {
		int ret = -1;
		SQLiteDatabase db = getWritableDatabase();
		if (db == null)
			return -1;
		String strFilter = "_id= ?";
		Cursor cur = db.query(TABLE_FAVORITES, new String[] { KEY_API_ID }, strFilter, new String[] { id + "" }, null, null, null);
		if (cur != null && cur.moveToFirst()) {
			if (cur != null && !cur.isAfterLast()) {
				ret = cur.getInt(cur.getColumnIndex(KEY_API_ID));
			}
		}
		db.close();
		return ret;
	}
}
