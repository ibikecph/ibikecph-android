package dk.kk.ibikecphlib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import dk.kk.ibikecphlib.R;

import java.util.List;


public class LeftMenuItemAdapter extends ArrayAdapter<LeftMenuItem> {
    Context context; 
    int layoutResourceId = R.layout.leftmenu_listitem;
    List<LeftMenuItem> data = null;
    
	public LeftMenuItemAdapter(Context context, List<LeftMenuItem> data) {
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

		LeftMenuItem item = this.data.get(position);
		TextView tv = (TextView) row.findViewById(R.id.menuItemTextView);
		ImageView iv = (ImageView) row.findViewById(R.id.menuItemIcon);
		
		// Set the label of the button to the string denoted in the item.
		tv.setText(IBikeApplication.getString(item.getLabelID()));
		
		if (item.getIconResource() != -1) {
			iv.setImageResource(item.getIconResource());
		}
		
		return row;
	}
	
}