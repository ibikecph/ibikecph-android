// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.favorites.FavoritesListActivity;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.search.HistoryListItem;
import dk.kk.ibikecphlib.search.SearchListItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.favorites.FavoritesListActivity;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.search.HistoryListItem;
import dk.kk.ibikecphlib.search.SearchListItem;

public class DB extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "iBikeCPHDB";
    private static final String TABLE_SEARCH_HISTORY = "SearchHistory";
    private static final String TABLE_FAVORITES = "Favorites";
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "Name";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_START_DATE = "startDate";
    // TODO: Consider removing the endDate as this makes no sense.
    private static final String KEY_END_DATE = "endDate";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_SUBSOURCE = "subsource";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LONG = "long";
    private static final String KEY_API_ID = "apiId";

    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        return getSearchHistory("5");
    }

    public ArrayList<SearchListItem> getSearchHistory(String limit) {

        ArrayList<SearchListItem> ret = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
                KEY_ID,
                KEY_NAME,
                KEY_ADDRESS,
                KEY_START_DATE,
                KEY_END_DATE,
                KEY_SOURCE,
                KEY_SUBSOURCE,
                KEY_LAT,
                KEY_LONG
        };

        Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, null, null, null, null, KEY_START_DATE + " DESC", limit);

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

                HistoryListItem hd = new HistoryListItem(
                        cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
                ret.add(hd);
                cursor.moveToNext();
            }
        }

        if (cursor != null)
            cursor.close();

        db.close();

        return ret;
    }

    public ArrayList<SearchListItem> getSearchHistoryForString(String srchString) {
        ArrayList<SearchListItem> ret = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        if (db == null)
            return null;

        String[] columns = {KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_START_DATE, KEY_END_DATE, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG};

        Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, KEY_NAME + " LIKE ? OR " + KEY_ADDRESS + " LIKE ?", new String[]{
                "%" + srchString + "%", "%" + srchString + "%"}, null, null, null, null);

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

                HistoryListItem hd = new HistoryListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
                if (hd.getAddress().getName() != null && !hd.getAddress().getName().trim().equals(""))
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

        String[] columns = {KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_START_DATE, KEY_END_DATE, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG};

        Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, KEY_NAME + " = ? ", new String[]{name.trim()}, null, null, null, null);

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

                HistoryListItem hd = new HistoryListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colStartDate), cursor.getString(colEndDate), cursor.getString(colSource),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong));
                if (hd.getAddress().getName() != null && !hd.getAddress().getName().trim().equals("")) {
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

    public long saveSearchHistory(HistoryListItem historyListItem) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(historyListItem.getAddress().getLocation() == null) {
            throw new RuntimeException("Cannot update a history with a missing location.");
        }
        if(historyListItem.getAddress() == null) {
            throw new RuntimeException("Cannot update a history with a missing address.");
        }
        String name = historyListItem.getAddress().getName() != null ?
                      historyListItem.getAddress().getName() :
                      "";
        String fullAddress = historyListItem.getAddress().getFullAddress() != null ?
                             historyListItem.getAddress().getFullAddress() :
                             "";

        String[] columns = {KEY_ID, KEY_NAME};
        long id = -1;

        String selection = KEY_NAME + " = ? AND " + KEY_ADDRESS + " = ?";
        Cursor cursor = db.query(TABLE_SEARCH_HISTORY, columns, selection, new String[]{ name, fullAddress }, null, null, null, "1");

        if (cursor != null) {
            if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(KEY_NAME, name);
                values.put(KEY_ADDRESS, fullAddress);
                values.put(KEY_START_DATE, historyListItem.getStartDate());
                values.put(KEY_END_DATE, historyListItem.getEndDate());
                values.put(KEY_SOURCE, historyListItem.getSource());
                values.put(KEY_SUBSOURCE, historyListItem.getSubSource());
                values.put(KEY_LAT, historyListItem.getAddress().getLocation().getLatitude());
                values.put(KEY_LONG, historyListItem.getAddress().getLocation().getLongitude());
                id = db.insert(TABLE_SEARCH_HISTORY, null, values);
            } else {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
            }
            cursor.close();
        }

        db.close();

        return id;
    }

    public long saveFavorite(FavoriteListItem favoriteListItem, boolean spawnThread) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(favoriteListItem.getAddress().getLocation() == null) {
            throw new RuntimeException("Cannot update a favorite with a missing location.");
        }
        if(favoriteListItem.getAddress() == null) {
            throw new RuntimeException("Cannot update a favorite with a missing address.");
        }

        long id;
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, favoriteListItem.getAddress().getName());
        values.put(KEY_ADDRESS, favoriteListItem.getAddress().getFullAddress());
        values.put(KEY_SOURCE, favoriteListItem.getSource());
        values.put(KEY_SUBSOURCE, favoriteListItem.getSubSource());
        values.put(KEY_LAT, favoriteListItem.getAddress().getLocation().getLatitude());
        values.put(KEY_LONG, favoriteListItem.getAddress().getLocation().getLongitude());
        values.put(KEY_API_ID, favoriteListItem.getApiId());
        Log.d("DB", "Inserting " + values);
        id = db.insert(TABLE_FAVORITES, null, values);
        favoriteListItem.setId(id);
        db.close();

        postFavoriteToServer(favoriteListItem, spawnThread);
        return id;
    }

    private void postFavoriteToServer(final FavoriteListItem favoriteListItem, boolean spawnThread) {
        if (IBikeApplication.isUserLogedIn()) {
            String authToken = IBikeApplication.getAuthToken();
            final JSONObject postObject = new JSONObject();
            try {
                JSONObject favouriteObject = new JSONObject();
                String name = favoriteListItem.getAddress().getName();
                // The API requires that the name of the favorite item is set
                favouriteObject.put("name", name != null ? name : favoriteListItem.getAddress().getDisplayName());
                favouriteObject.put("address", favoriteListItem.getAddress().getFullAddress());
                favouriteObject.put("latitude", favoriteListItem.getAddress().getLocation().getLatitude());
                favouriteObject.put("longitude", favoriteListItem.getAddress().getLocation().getLongitude());
                favouriteObject.put("source", favoriteListItem.getSource());
                favouriteObject.put("sub_source", favoriteListItem.getSubSource());
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
                                String strFilter = "_id=" + favoriteListItem.getId();
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
                        String strFilter = "_id=" + favoriteListItem.getId();
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

    public ArrayList<FavoriteListItem> getFavorites(ArrayList<FavoriteListItem> ret) {
        if (ret == null) {
            ret = new ArrayList<>();
        } else {
            ret.clear();
        }
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
            KEY_ID,
            KEY_NAME,
            KEY_ADDRESS,
            KEY_SOURCE,
            KEY_SUBSOURCE,
            KEY_LAT,
            KEY_LONG,
            KEY_API_ID
        };

        Cursor cursor = db.query(TABLE_FAVORITES, columns, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int colId = cursor.getColumnIndex(KEY_ID);
                int colName = cursor.getColumnIndex(KEY_NAME);
                int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
                int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
                int colLat = cursor.getColumnIndex(KEY_LAT);
                int colLong = cursor.getColumnIndex(KEY_LONG);
                int colApiId = cursor.getColumnIndex(KEY_API_ID);
                FavoriteListItem fd = new FavoriteListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));
                ret.add(fd);
                cursor.moveToNext();
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return ret;
    }

    public ArrayList<FavoriteListItem> getFavoritesFromServer(ArrayList<FavoriteListItem> ret) {
        if (ret == null) {
            ret = new ArrayList<>();
        } else {
            ret.clear();
        }
        if (IBikeApplication.isUserLogedIn()) {
            String authToken = IBikeApplication.getAuthToken();
            JsonNode getObject = HttpUtils.getFromServer(Config.API_URL + "/favourites?auth_token=" + authToken);
            if(getObject != null && getObject.has("invalid_token")) {
                if (getObject.get("invalid_token").asBoolean()) {
                    IBikeApplication.logoutWrongToken();
                }
            }
            if (getObject != null && getObject.has("data")) {
                SQLiteDatabase db = this.getWritableDatabase();
                if (db != null) {
                    db.beginTransaction();
                    try {
                        // Delete all existing favorites
                        db.delete(TABLE_FAVORITES, null, null);
                        // Add the ones that was returned from the API
                        JsonNode favoritesList = getObject.get("data");
                        for (JsonNode favoriteNode: favoritesList) {
                            String name = favoriteNode.get("name").asText();
                            String address = favoriteNode.get("address").asText();
                            String source = favoriteNode.get("source").asText();
                            String subSource = favoriteNode.get("sub_source").asText();
                            double latitude = favoriteNode.get("lattitude").asDouble();
                            double longitude = favoriteNode.get("longitude").asDouble();
                            int id = favoriteNode.get("id").asInt();

                            ContentValues values = new ContentValues();
                            values.put(KEY_NAME, name);
                            values.put(KEY_ADDRESS, address);
                            values.put(KEY_SOURCE, source);
                            values.put(KEY_SUBSOURCE, subSource);
                            values.put(KEY_LAT, latitude);
                            values.put(KEY_LONG, longitude);
                            values.put(KEY_API_ID, id);

                            db.insert(TABLE_FAVORITES, null, values);
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }
        }
        getFavorites(ret);
        return ret;
    }

    public void deleteFavorites() {
        LOG.d("Deleting favorites from sqllite database");
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, null, null);
        db.close();
    }

    public ArrayList<SearchListItem> getFavorites2() {

        ArrayList<SearchListItem> ret = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {KEY_ID, KEY_NAME, KEY_ADDRESS, KEY_SOURCE, KEY_SUBSOURCE, KEY_LAT, KEY_LONG, KEY_API_ID};

        Cursor cursor = db.query(TABLE_FAVORITES, columns, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int colId = cursor.getColumnIndex(KEY_ID);
                int colName = cursor.getColumnIndex(KEY_NAME);
                int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
                int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
                int colLat = cursor.getColumnIndex(KEY_LAT);
                int colLong = cursor.getColumnIndex(KEY_LONG);
                int colApiId = cursor.getColumnIndex(KEY_API_ID);

                FavoriteListItem fd = new FavoriteListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));

                ret.add(fd);
                cursor.moveToNext();
            }
        }

        if (cursor != null)
            cursor.close();

        db.close();

        return ret;
    }

    public ArrayList<SearchListItem> getFavoritesForString(String srchString) {
        ArrayList<SearchListItem> ret = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
            KEY_ID,
            KEY_NAME,
            KEY_ADDRESS,
            KEY_SOURCE,
            KEY_SUBSOURCE,
            KEY_LAT,
            KEY_LONG,
            KEY_API_ID
        };

        Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " LIKE ? ", new String[]{"%" + srchString + "%"}, null, null,
                null, null);

        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int colId = cursor.getColumnIndex(KEY_ID);
                int colName = cursor.getColumnIndex(KEY_NAME);
                int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
                int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
                int colLat = cursor.getColumnIndex(KEY_LAT);
                int colLong = cursor.getColumnIndex(KEY_LONG);
                int colApiId = cursor.getColumnIndex(KEY_API_ID);

                FavoriteListItem fd = new FavoriteListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));

                ret.add(fd);
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

        String[] columns = {
            KEY_ID,
            KEY_NAME,
            KEY_ADDRESS,
            KEY_SOURCE,
            KEY_SUBSOURCE,
            KEY_LAT,
            KEY_LONG,
            KEY_API_ID
        };

        Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " = ?", new String[]{name.trim()}, null, null, null, "1");

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                int colId = cursor.getColumnIndex(KEY_ID);
                int colName = cursor.getColumnIndex(KEY_NAME);
                int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
                int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
                int colLat = cursor.getColumnIndex(KEY_LAT);
                int colLong = cursor.getColumnIndex(KEY_LONG);
                int colApiId = cursor.getColumnIndex(KEY_API_ID);

                ret = new FavoriteListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                       cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));
            }
            cursor.close();
        }
        db.close();

        return ret;
    }

    public FavoriteListItem getFavoriteByAddress(Address address) {
        FavoriteListItem ret = null;

        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
            KEY_ID,
            KEY_NAME,
            KEY_ADDRESS,
            KEY_SOURCE,
            KEY_SUBSOURCE,
            KEY_LAT,
            KEY_LONG,
            KEY_API_ID
        };

        Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_ADDRESS + " = ?", new String[]{ address.getFullAddress().trim() }, null, null, null, null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                int colId = cursor.getColumnIndex(KEY_ID);
                int colName = cursor.getColumnIndex(KEY_NAME);
                int colAddress = cursor.getColumnIndex(KEY_ADDRESS);
                int colSubSource = cursor.getColumnIndex(KEY_SUBSOURCE);
                int colLat = cursor.getColumnIndex(KEY_LAT);
                int colLong = cursor.getColumnIndex(KEY_LONG);
                int colApiId = cursor.getColumnIndex(KEY_API_ID);

                ret = new FavoriteListItem(cursor.getInt(colId), cursor.getString(colName), cursor.getString(colAddress),
                        cursor.getString(colSubSource), cursor.getDouble(colLat), cursor.getDouble(colLong), cursor.getInt(colApiId));
            }
            cursor.close();
        }

        db.close();

        return ret;
    }

    public void updateFavorite(FavoriteListItem favoriteListItem, APIListener listener) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(favoriteListItem.getAddress().getLocation() == null) {
            throw new RuntimeException("Cannot update a favorite with a missing location.");
        }
        if(favoriteListItem.getAddress() == null) {
            throw new RuntimeException("Cannot update a favorite with a missing address.");
        }

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, favoriteListItem.getAddress().getName());
        values.put(KEY_ADDRESS, favoriteListItem.getAddress().getFullAddress());
        values.put(KEY_SOURCE, favoriteListItem.getSource());
        values.put(KEY_SUBSOURCE, favoriteListItem.getSubSource());
        values.put(KEY_LAT, favoriteListItem.getAddress().getLocation().getLatitude());
        values.put(KEY_LONG, favoriteListItem.getAddress().getLocation().getLongitude());
        db.update(TABLE_FAVORITES, values, KEY_ID + " = ?", new String[]{"" + favoriteListItem.getId()});

        db.close();

        updateFavoriteToServer(favoriteListItem, listener);
    }

    public void deleteFavorite(FavoriteListItem fd) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (db == null)
            return;

        db.delete(TABLE_FAVORITES, KEY_ID + " = ?", new String[]{"" + fd.getId()});

        db.close();

        deleteFavoriteFromServer(fd);
    }

    private void updateFavoriteToServer(final FavoriteListItem fd, final APIListener listener) {
        if (IBikeApplication.isUserLogedIn()) {
            String authToken = IBikeApplication.getAuthToken();
            final JSONObject postObject = new JSONObject();
            try {
                JSONObject favouriteObject = new JSONObject();
                favouriteObject.put("name", fd.getAddress().getName());
                favouriteObject.put("address", fd.getAddress().getFullAddress());
                favouriteObject.put("lattitude", fd.getAddress().getLocation().getLatitude());
                favouriteObject.put("longitude", fd.getAddress().getLocation().getLongitude());
                favouriteObject.put("source", fd.getSource());
                favouriteObject.put("sub_source", fd.getSubSource());
                postObject.put("favourite", favouriteObject);
                postObject.put("auth_token", authToken);

                JsonNode node = HttpUtils.putToServer(Config.API_URL + "/favourites/" + fd.getApiId(), postObject);
                if (listener != null) {
                    boolean success = false;
                    if (node != null && node.has("success") && node.get("success").asBoolean()) {
                        success = true;
                        FavoritesListActivity.fetchFavoritesAfterEdit.updateFavorites();
                    }
                    listener.onRequestCompleted(success);
                }
            } catch (JSONException e) {
                LOG.e(e.getLocalizedMessage());
            }
        }
    }

    public void deleteFavoriteFromServer(final FavoriteListItem fd) {
        if (IBikeApplication.isUserLogedIn()) {
            String authToken = IBikeApplication.getAuthToken();
            final JSONObject postObject = new JSONObject();
            try {
                postObject.put("auth_token", authToken);

                HttpUtils.deleteFromServer(Config.API_URL + "/favourites/" + fd.getApiId(), postObject);

            } catch (JSONException e) {
                LOG.e(e.getLocalizedMessage());
            }
        }
    }

    public int favoritesForName(String name) {
        int ret = 0;
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
            KEY_NAME
        };

        Cursor cursor = db.query(TABLE_FAVORITES, columns, KEY_NAME + " = ?", new String[]{ name }, null, null, null, null);

        if (cursor != null) {
            ret = cursor.getCount();
            cursor.close();
        }

        db.close();

        return ret;
    }
}
