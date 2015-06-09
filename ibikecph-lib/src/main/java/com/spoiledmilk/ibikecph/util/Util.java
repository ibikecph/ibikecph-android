// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.osmdroid.api.IGeoPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.iLanguageListener;
import com.spoiledmilk.ibikecph.util.IbikePreferences.Language;

public class Util {
    public static final String TIME_FORMAT = "HH.mm";
    public static final String TIME_DAYS_SHORT = "d";
    public static final String TIME_HOURS_SHORT = "h";
    public static final String TIME_MINUTES_SHORT = "min";
    public static final String TIME_SECONDS_SHORT = "s";
    public static final String DISTANCE_KM_SHORT = "km";
    public static final String DISTANCE_M_SHORT = "m";

    private static DisplayMetrics metrics = null;
    private static float screenWidht = 0;
    private static float screenHeight = 0;

    public static final int MENU_LANGUAGE = Menu.FIRST;

    private static ObjectMapper jsonMapper = new ObjectMapper();

    public static void init(WindowManager manager) {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
        }

        if (metrics == null) {
            metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);
            if (metrics != null) {
                screenWidht = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
                LOG.d("screenWidth = " + screenWidht);
                LOG.d("screenHeight = " + screenHeight);
                LOG.d("density = " + metrics.density);
            }
        }
    }

    public static ObjectMapper getJsonObjectMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
        }
        return jsonMapper;
    }

    public static float getScreenWidth() {
        return screenWidht;
    }

    public static float getScreenHeight() {
        return screenHeight;
    }

    public static float getScreenRatio() {
        return screenHeight == 0 ? -1 : screenWidht / screenHeight;
    }

    public static float getDensity() {
        if (metrics != null) {
            return metrics.density;
        } else {
            return 1.0f;
        }
    }

    public static int px2dp(int px) {
        return (int) ((float) px / metrics.density);
    }

    public static float dp2px(float number) {
        return getDensity() * number;
    }

    public static int dp2px(int number) {
        return Math.round(getDensity() * number);
    }

    // Format distance string (choose between meters and kilometers)
    public static String formatDistance(float meters) {
        String ret;
        if (meters < 5) {
            ret = "";
        } else if (meters < 100f) {
            float rounded = (float) Math.round(meters / 10.0f) * 10f;
            ret = String.format(Locale.US, "%.0f %s", rounded, DISTANCE_M_SHORT);
        } else if (meters < 1000f) {
            float rounded = (float) (Math.round(meters / 100.0f) * 100);
            ret = String.format(Locale.US, "%.0f %s", rounded, DISTANCE_M_SHORT);
        } else {
            ret = String.format(Locale.US, "%.1f %s", meters / 1000.0f, DISTANCE_KM_SHORT);
        }
        if (ret.contains("1.0")) {
            ret = "1 " + DISTANCE_KM_SHORT;
        }
        return ret;
        // return meters > 1000.0f ? String.format(Locale.US, "%.1f %@", meters / 1000.0f, DISTANCE_KM_SHORT) :
        // String.format(Locale.US,
        // "%.0f %@", meters, DISTANCE_M_SHORT);
    }

    public static JsonNode stringToJsonNode(String jsonStr) {
        JsonNode result = null;
        try {
            result = getJsonObjectMapper().readValue(jsonStr, JsonNode.class);
        } catch (JsonParseException e) {
            LOG.w("HttpUtils readLink() JsonParseException ", e);
        } catch (JsonMappingException e) {
            LOG.w("HttpUtils readLink() JsonMappingException ", e);
        } catch (IOException e) {
            LOG.w("HttpUtils readLink() IOException ", e);
        }
        return result;
    }

    public static List<JsonNode> JsonNodeToList(JsonNode arg) {
        List<JsonNode> result = null;
        try {
            if (arg != null)
                result = getJsonObjectMapper().readValue(arg.toString(), new TypeReference<List<JsonNode>>() {
                });
        } catch (JsonParseException e) {
            LOG.w("JsonParseException ", e);
        } catch (JsonMappingException e) {
            LOG.w("JsonMappingException ", e);
        } catch (IOException e) {
            LOG.w("IOException ", e);
        }
        return result;
    }

    public static void showSimpleMessageDlg(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message).setPositiveButton(IbikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    public static Location locationFromGeoPoint(IGeoPoint geoPoint) {
        return locationFromCoordinates(geoPoint.getLatitudeE6() * (float) 1E-6, geoPoint.getLongitudeE6() * (float) 1E-6);
    }

    public static Location locationFromCoordinates(double lat, double lng) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        return loc;
    }

    public static int numberOfOccurenciesOfString(String string, String srchString) {
        int ret = 0;
        int start = 0;
        while (start < string.length() && start > -1) {
            start = string.indexOf(srchString, 0);
            if (start > -1)
                ret++;
        }
        return ret;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

    // used to determine alpha of a view depending of the y coordinate from
    // swiping
    public static int yToAlpha(int y) {
        int minAlpha = 0;
        int maxAlpha = 240;
        double percentage = y / Util.getScreenHeight();
        percentage = 1.0d - percentage;
        int ret = (int) (minAlpha + percentage * (maxAlpha - minAlpha));
        if (y > 3 * Util.getScreenHeight() / 4) {
            ret = minAlpha;
        }
        return ret;
    }

    public static void launchNoConnectionDialog(Context ctx) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_no_connection);
        TextView text = (TextView) dialog.findViewById(R.id.textNetworkError);
        text.setTypeface(IbikeApplication.getNormalFont());
        text.setText(IbikeApplication.getString("network_error_text"));
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    // dialog not attached to window
                }
            }
        }, 3000);
    }

    public static void showLanguageDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(IbikeApplication.getString("choose_language"));
        builder.setItems(IbikePreferences.getLanguageNames(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ((IbikeApplication) activity.getApplication()).changeLanguage(Language.values()[item + 1]);
                ((iLanguageListener) activity).reloadStrings();
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(IbikeApplication.getString("Cancel"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static String stringFromJsonAssets(Context context, String path) {
        String bufferString = "";
        try {
            InputStream is = context.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            bufferString = new String(buffer);
        } catch (Exception e) {
            LOG.e(e.getLocalizedMessage());
        }
        return bufferString;
    }

    public static double limitDecimalPlaces(double d, int count) {
        double ret = d;
        try {
            int temp = (int) (d * Math.pow(10, count));
            ret = temp / (double) (Math.pow(10, count));
        } catch (Exception e) {

        }
        return ret;
    }

    public static Bitmap bmpDecodeFile(File f, int width_limit, int height_limit, long max_size, boolean max_dimensions) {
        if (f == null) {
            return null;
        }

        LOG.d("bmpDecodeFile(" + f.getAbsolutePath() + "," + width_limit + "," + height_limit + "," + max_size + "," + max_dimensions + ")");

        Bitmap bmp = null;
        boolean shouldReturn = false;

        FileInputStream fin = null;
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            fin = new FileInputStream(f);
            BitmapFactory.decodeStream(fin, null, o);
            try {
                fin.close();
                fin = null;
            } catch (IOException e) {
            }

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            if (width_limit != -1 && height_limit != -1) {
                if (max_dimensions) {
                    while (o.outWidth / scale > width_limit || o.outHeight / scale > height_limit)
                        scale *= 2;
                } else {
                    while (o.outWidth / scale / 2 >= width_limit && o.outHeight / scale / 2 >= height_limit)
                        scale *= 2;
                }
            } else if (max_size != -1)
                while ((o.outWidth * o.outHeight) / (scale * scale) > max_size)
                    scale *= 2;

            // Decode with inSampleSize
            o = null;
            if (scale > 1) {
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
            }
            fin = new FileInputStream(f);
            try {
                bmp = BitmapFactory.decodeStream(fin, null, o);
            } catch (OutOfMemoryError e) {
                // Try to recover from out of memory error - but keep in mind
                // that behavior after this error is
                // undefined,
                // for example more out of memory errors might appear in catch
                // block.
                if (bmp != null)
                    bmp.recycle();
                bmp = null;
                System.gc();
                LOG.e("Util.bmpDecodeFile() OutOfMemoryError in decodeStream()! Trying to recover...");
            }

            if (bmp != null) {
                LOG.d("resulting bitmap width : " + bmp.getWidth() + " height : " + bmp.getHeight() + " size : "
                        + (bmp.getRowBytes() * bmp.getHeight()));

                ExifInterface exif = new ExifInterface(f.getPath());
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (FileNotFoundException e) {
            shouldReturn = true;
        } catch (IOException e) {
            shouldReturn = true; // bitmap is still valid here, just can't be
            // rotated
        } finally {
            if (fin != null)
                try {
                    fin.close();
                } catch (IOException e) {
                }
        }

        if (shouldReturn || bmp == null)
            return bmp;

        float rotate = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
        }
        if (rotate > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            Bitmap bmpRot = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            matrix = null;
            bmp.recycle();
            bmp = null;
            // System.gc();
            return bmpRot;
        }

        return bmp;
    }

    public static String locationString(Location loc) {
        return loc.getLatitude() + " , " + loc.getLongitude();
    }

    /**
     * Get a diff between two dates
     * author: Sebastien Lorber (http://stackoverflow.com/a/10650881/1851450)
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

}
