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
import de.egh.bikehist.de.egh.bikehist.model.TagType;


public class TagTypeListItemAdapter extends ArrayAdapter<TagType> {

	private static final String TAG = TagTypeListItemAdapter.class.getSimpleName();
	int resource;
	private TagType item;
	private String inflater;
	private LayoutInflater vi;

	/** One item can be selected */
	public TagTypeListItemAdapter(Context _context, int _resource, List<TagType> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}

	/** Can be -1, if nothing is selected */
	private int selectedPosition = -1;

	public void setSelectedItem(int pos) {
		selectedPosition = pos;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LinearLayout itemView;
		TextView line;

		item = getItem(position);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

		Log.d(TAG, "getView for " + item.getName());
		line = (TextView) itemView.findViewById(R.id.drawerTagTypeName);
		line.setText(item.getName());
		if (position == selectedPosition) {
			line.setTextColor(Color.GREEN);
		} else {
			line.setTextColor(Color.WHITE);
		}

		return itemView;
	}
}
