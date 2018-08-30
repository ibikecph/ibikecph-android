// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.favorites;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.LeftMenu;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.util.Config;
import dk.kk.ibikecphlib.util.DB;
import dk.kk.ibikecphlib.util.HttpUtils;
import dk.kk.ibikecphlib.util.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FavoritesAdapter extends ArrayAdapter<FavoriteListItem> {

    public boolean isEditMode = false;
    private LeftMenu fragment;
    ArrayList<FavoriteListItem> data;

    public FavoritesAdapter(Context context, ArrayList<FavoriteListItem> objects, LeftMenu fragment) {
        super(context, R.layout.list_row_favorite, objects);
        this.fragment = fragment;
        data = objects;
    }

    public void setIsEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(getListRowLayout(), parent, false);
        TextView tv = (TextView) view.findViewById(R.id.textFavoriteName);
        String name = getItem(position).getAddress().getName();
        /*if (name.length() > 19)
            name = name.substring(0, 19) + "...";*/
        tv.setText(name);

        ImageButton btnEdit = (ImageButton) view.findViewById(R.id.btnEdit);
        final FavoriteListItem fd = getItem(position);
        tv.setPadding(getPadding(fd), 0, 0, 0);

        final ImageView imgIcon = ((ImageView) view.findViewById(R.id.icon));

        if (!isEditMode) {
            imgIcon.setImageResource(getIconResourceId(getItem(position)));
            btnEdit.setVisibility(View.GONE);
        } else {
            imgIcon.setImageResource(R.drawable.fav_reorder);
            btnEdit.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public void reorder(int firstIndex, int secondIndex, boolean toNotify) {
        if (firstIndex != secondIndex) {
            LOG.d("Favorites reordering " + firstIndex + "->" + secondIndex);
            if (firstIndex < 0)
                firstIndex = 0;
            if (firstIndex > data.size() - 1)
                firstIndex = data.size() - 1;
            if (secondIndex < 0)
                secondIndex = 0;
            if (secondIndex > data.size() - 1)
                secondIndex = data.size() - 1;
            FavoriteListItem tmp = getItem(firstIndex);
            data.set(firstIndex, getItem(secondIndex));
            data.set(secondIndex, tmp);
        }
        if (toNotify) {
            notifyDataSetChanged();

            // reorder the favorites locally
            DB db = new DB(getContext());
            db.deleteFavorites();
            for (int i = 0; i < getCount(); i++) {
                db.saveFavorite(getItem(i), false);
            }

            final JSONObject postObject = new JSONObject();
            try {
                postObject.put("auth_token", IBikeApplication.getAuthToken());
                JSONArray favorites = new JSONArray();
                for (int i = 0; i < data.size(); i++) {
                    JSONObject item = new JSONObject();
                    item.put("id", data.get(i).getApiId());
                    item.put("position", i);
                    favorites.put(i, item);
                }
                postObject.putOpt("pos_ary", favorites);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpUtils.postToServer(Config.API_URL + "/favourites/reorder", postObject);
                    }
                }).start();
            } catch (JSONException e) {
                LOG.e(e.getLocalizedMessage());
            }
        }
    }

    protected int getIconResourceId(FavoriteListItem fd) {
        int ret = R.drawable.fav_star;
        if (fd.getSubSource().equals(FavoriteListItem.favHome))
            ret = R.drawable.fav_home;
        else if (fd.getSubSource().equals(FavoriteListItem.favWork))
            ret = R.drawable.fav_work;
        else if (fd.getSubSource().equals(FavoriteListItem.favSchool))
            ret = R.drawable.fav_school;
        return ret;
    }

    protected int getListRowLayout() {
        return R.layout.list_row_favorite;
    }

    protected int getPadding(FavoriteListItem fd) {
        return 0;
    }


}
