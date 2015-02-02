package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class LeftMenuItemAdapter extends ArrayAdapter<LeftMenuItem> {
    Context context; 
    int layoutResourceId = R.layout.leftmenu_listitem;
    LeftMenuItem data[] = null;
    
	public LeftMenuItemAdapter(Context context, LeftMenuItem[] data) {
        super(context, R.layout.leftmenu_listitem, data);
        this.context = context;
        this.data = data;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		
		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			row = inflater.inflate(layoutResourceId, parent, false);
		}

		LeftMenuItem item = this.data[position];
		TextView tv = (TextView) row.findViewById(R.id.menuItemTextView);
		
		// Set the label of the button to the string denoted in the item.
		tv.setText(IbikeApplication.getString(item.getLabelID()));
		
		return row;
	}
	
}