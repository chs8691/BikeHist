package de.egh.bikehist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.egh.bikehist.de.egh.bikehist.model.Event;

public class ListItemAdapter extends ArrayAdapter<Event> {

	int resource;

	public ListItemAdapter(Context _context, int _resource, List<Event> _items) {
		super(_context, _resource, _items);
		resource = _resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemView;

		Event item = getItem(position);

		String eventName = item.getName();
		Date timestampDate = new Date(item.getTimestamp());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		String dateString = sdf.format(timestampDate);

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}

		TextView dateView = (TextView) itemView.findViewById(R.id.rowDate);
		TextView taskView = (TextView) itemView.findViewById(R.id.row);

		dateView.setText(dateString);
		taskView.setText(eventName);

		return itemView;
	}
}
