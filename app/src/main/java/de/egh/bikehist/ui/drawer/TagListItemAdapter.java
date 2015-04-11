package de.egh.bikehist.ui.drawer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.egh.bikehist.R;
import de.egh.bikehist.ui.drawer.DrawerController.TagItem;


public class TagListItemAdapter extends ArrayAdapter<TagItem> {

	private static final String TAG = TagListItemAdapter.class.getSimpleName();
	int resource;
	private TagItem item;
	private String inflater;
	private LayoutInflater vi;

	/** One item can be selected */
	public TagListItemAdapter(Context _context, int _resource, List<TagItem> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
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

//		Log.d(TAG, "getView for " + item.getTag().getName());
		line = (TextView) itemView.findViewById(R.id.drawerTagName);
		line.setText(item.getTag().getName());
		if (item.isChecked()) {
			line.setTextColor(Color.GREEN);
		} else {
			line.setTextColor(Color.WHITE);
		}

		return itemView;
	}
}
