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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.egh.bikehist.R;
import de.egh.bikehist.de.egh.bikehist.model.Tag;


public class TagListItemAdapter extends ArrayAdapter<Tag> {

	private static final String TAG = TagListItemAdapter.class.getSimpleName();
	int resource;
	private Tag item;
	private String inflater;
	private LayoutInflater vi;

	/** One item can be selected */
	public TagListItemAdapter(Context _context, int _resource, List<Tag> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}

	/** Can be -1, if nothing is selected */
	private Set<Integer> selectedList = new HashSet<>();

	public boolean isItemSelected(int pos) {
		return selectedList.contains(pos);
	}

	public void setSelectedItem(int pos, boolean selected) {
		if (selected) {
			selectedList.add(pos);
		} else {
			selectedList.remove(pos);
		}
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
		line = (TextView) itemView.findViewById(R.id.drawerTagName);
		line.setText(item.getName());
		if (selectedList.contains(position)) {
			line.setTextColor(Color.GREEN);
		} else {
			line.setTextColor(Color.WHITE);
		}

		return itemView;
	}
}
