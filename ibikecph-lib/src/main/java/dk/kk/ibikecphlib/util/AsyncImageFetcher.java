// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package dk.kk.ibikecphlib.util;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class AsyncImageFetcher extends AsyncTask<Intent, Void, ImageData> {

	ImagerPrefetcherListener listener;
	Context context;
	ImageView imageViewDummy;

	public AsyncImageFetcher(ImagerPrefetcherListener listener, Context context) {
		this.listener = listener;
		this.context = context;
		this.imageViewDummy = new ImageView(context);
		this.imageViewDummy.setScaleType(ScaleType.CENTER_CROP);
		this.imageViewDummy.setLayoutParams(new LinearLayout.LayoutParams(Util.dp2px(70), Util.dp2px(70)));
	}

	@Override
	protected ImageData doInBackground(Intent... params) {
		Intent data = params[0];
		ImageData imageData = new ImageData();
		Bitmap bmp = null;
		try {
			if (data.getExtras() != null && data.getExtras().containsKey("data")) {
				// Camera
				bmp = (Bitmap) data.getExtras().get("data");

				Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI,
						new String[] { Media.DATA, Media.DATE_ADDED, MediaStore.Images.ImageColumns.ORIENTATION }, Media.DATE_ADDED, null,
						"date_added ASC");
				if (cursor != null && cursor.moveToLast()) {
					Uri fileURI = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
					String fileSrc = fileURI.toString();
					cursor.close();
					File file = new File(fileSrc);
					if (file != null && file.exists()) {
						file.delete();
						// force refreshing of the gallery, so the image can't
						// be seen
						try {
							context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
									+ Environment.getExternalStorageDirectory())));
						} catch (Exception e) {
							// do nothing, this is not important if it fails
						}
					}
				}
			} else {
				// Gallery
				Uri photoUri = data.getData();
				if (photoUri != null) {
					String[] filePathColumn = { MediaStore.Images.Media.DATA };
					Cursor cursor = context.getContentResolver().query(photoUri, filePathColumn, null, null, null);
					if (cursor != null) {
						cursor.moveToFirst();
						int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
						String filePath = cursor.getString(columnIndex);
						cursor.close();
						bmp = Util.bmpDecodeFile(new File(filePath), Util.dp2px(560), Util.dp2px(560), -1, false);
					} else {
						String filePath = photoUri.toString();
						if (filePath != null && filePath.contains("/")) {
							filePath = filePath.substring(filePath.indexOf('/') + 2, filePath.length());
						}
						bmp = Util.bmpDecodeFile(new File(filePath), Util.dp2px(560), Util.dp2px(560), -1, false);
					}
				}
			}

			if (bmp != null) {
				final int width = bmp.getWidth();
				final int height = bmp.getHeight();
				boolean isLandscape = false;
				if (width > height) {
					isLandscape = true;
				}
				int newWidth;
				float ratio;
				int newHeight;
				if (isLandscape) {
					newWidth = Util.dp2px(560);
					ratio = (float) height / (float) width;
					newHeight = (int) (Util.dp2px(560f) * ratio);
				} else {
					newHeight = Util.dp2px(560);
					ratio = (float) width / (float) height;
					newWidth = (int) (Util.dp2px(560f) * ratio);
				}
				Matrix matrix = new Matrix();
				// RESIZE THE BIT MAP
				matrix.postScale(newWidth, newHeight);
				// "RECREATE" THE NEW BITMAP
				Bitmap resizedBitmap = null;
				resizedBitmap = bmp;

				if (resizedBitmap != null) {
					imageData.base64 = getBase64(resizedBitmap);
				}

				bmp = resizedBitmap;
				this.imageViewDummy.setImageDrawable(new BitmapDrawable(context.getResources(), bmp));
				imageData.bmp = this.imageViewDummy.getDrawable();
				return imageData;
			}
		} catch (Exception e) {

		}
		return imageData;
	}

	@Override
	protected void onPostExecute(ImageData result) {
		super.onPostExecute(result);
		if (listener != null) {
			listener.onImagePrefetched(result);
		}
	}

	public static String getBase64(Bitmap bmp) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] b = baos.toByteArray();
		String base64Image = Base64.encodeToString(b, Base64.DEFAULT);
		return base64Image;
	}

}
