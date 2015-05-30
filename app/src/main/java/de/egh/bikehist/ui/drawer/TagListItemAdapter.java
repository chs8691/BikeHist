package de.egh.bikehist.ui.drawer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.egh.bikehist.R;
import de.egh.bikehist.ui.drawer.DrawerController.TagItem;


class TagListItemAdapter extends ArrayAdapter<TagItem> {

	private static final String TAG = TagListItemAdapter.class.getSimpleName();
	private final int resource;
	private final String inflater;

	/** One event_item can be selected */
	public TagListItemAdapter(Context _context, List<TagItem> _items) {
		super(_context, R.layout.drawer_tags_item, _items);
		resource = R.layout.drawer_tags_item;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LinearLayout itemView;
		TextView line;

		TagItem item = getItem(position);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

//		Log.d(TAG, "getView for " + event_item.getTag().getName());
		line = (TextView) itemView.findViewById(R.id.drawerTagName);
		line.setText(item.getTag().getName());
		if (item.isChecked()) {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerSelectedText));
		} else {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerText));
		}

		return itemView;
	}
}
