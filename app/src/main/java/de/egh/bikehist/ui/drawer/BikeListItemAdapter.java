package de.egh.bikehist.ui.drawer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.egh.bikehist.R;

/** For Drawer. */
class BikeListItemAdapter extends ArrayAdapter<DrawerController.BikeItem> {

	private static final String TAG = BikeListItemAdapter.class.getSimpleName();
	private final int resource;
	private final String inflater;


	public BikeListItemAdapter(Context _context, List<DrawerController.BikeItem> _items) {
		super(_context, R.layout.drawer_tag_types_item, _items);
		resource = R.layout.drawer_tag_types_item;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LinearLayout itemView;

		DrawerController.BikeItem item = getItem(position);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

//		Log.d(TAG, "getView for " + item.getBike().getName());
		TextView line = (TextView) itemView.findViewById(R.id.drawerTagTypeName);
		line.setText(item.getBike().getName());

		if (item.isChecked()) {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerSelectedText));
		} else {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerText));
		}

		return itemView;
	}
}
