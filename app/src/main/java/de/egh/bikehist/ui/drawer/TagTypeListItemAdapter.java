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
import de.egh.bikehist.ui.drawer.DrawerController.TagTypeItem;


class TagTypeListItemAdapter extends ArrayAdapter<TagTypeItem> {

	private static final String TAG = TagTypeListItemAdapter.class.getSimpleName();
	private final int resource;
	private final String inflater;

	/** One event_item can be selected */
	public TagTypeListItemAdapter(Context _context, List<TagTypeItem> _items) {
		super(_context, R.layout.drawer_tag_types_item, _items);
		resource = R.layout.drawer_tag_types_item;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LinearLayout itemView;
		TextView line;

		TagTypeItem item = getItem(position);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

//		Log.d(TAG, "getView for " + event_item.getTagType().getName());
		line = (TextView) itemView.findViewById(R.id.drawerTagTypeName);
		line.setText(item.getTagType().getName());
		if (item.isChecked()) {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerSelectedText));
		} else {
			line.setTextColor(getContext().getResources().getColor(R.color.eghDrawerText));
		}

		return itemView;
	}
}
