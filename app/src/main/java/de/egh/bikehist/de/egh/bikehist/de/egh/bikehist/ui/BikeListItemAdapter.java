package de.egh.bikehist.de.egh.bikehist.de.egh.bikehist.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.egh.bikehist.R;
import de.egh.bikehist.de.egh.bikehist.de.egh.bikehist.ui.DrawerController.BikeItem;

/** For Drawer. */
public class BikeListItemAdapter extends ArrayAdapter<BikeItem> {

	private static final String TAG = BikeListItemAdapter.class.getSimpleName();
	int resource;
	private BikeItem item;
	private String inflater;
	private LayoutInflater vi;
	private TextView line;


	public BikeListItemAdapter(Context _context, int _resource, List<BikeItem> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LinearLayout itemView;

		item = getItem(position);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

		Log.d(TAG, "getView for " + item.getBike().getName());
		line = (TextView) itemView.findViewById(R.id.drawerTagTypeName);
		line.setText(item.getBike().getName());

		if (item.isChecked()) {
			line.setTextColor(Color.GREEN);
		} else {
			line.setTextColor(Color.WHITE);
		}

		return itemView;
	}
}
