package de.egh.bikehist.ui;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.Utils;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

/**
 Created by ChristianSchulzendor on 13.02.2015.
 */
public class EventListFragment extends Fragment {
	private static final String TAG = EventListFragment.class.getSimpleName();
	private EventListItemAdapter aa;
	private ListView listView;
	private ArrayList<EventItem> listItems = new ArrayList<>();
	private Map<UUID, Tag> tags = new HashMap<>();
	//	private TagType actualTagType;
	private List<Event> events = new ArrayList<>();
	private String tagTypeId;
	private String bikeId;
	private List<String> tagIds;

	/** Select data from persistance. */
	private void loadDataFromProvider() {

		ContentResolver cr = getActivity().getContentResolver();

		//--- TagTypes: Get the one TagType ---//
		Cursor c;

		//--- Tags: All Tags of the TagType ---//
		tags.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
				Tables.Tag.Columns.Name.TAG_TYPE_ID + "=?", new String[]{tagTypeId}, null);

		if (c.moveToFirst()) {
			do {
				Tag tag = Utils.buildTagFromCursor(c);
				tags.put(tag.getId(), tag);
			} while (c.moveToNext());
		}

		//Select for all Tag IDs
		if (tagIds.size() == 0) {
			//--- Events: Get all Events for the Bike / TagType ---//
		/*
		* SELECT * FROM events
       WHERE bike_id = ?
       AND tag_id IN (SELECT id
                          FROM tags
                          WHERE tag_type_id = ?)
     */
			events.clear();
			c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null,

					Tables.Event.Columns.Name.BIKE_ID + "=? AND "
							+ Tables.Event.Columns.Name.TAG_ID + " IN (SELECT " + Tables.Tag.Columns.Name.ID + " FROM "
							+ Tables.Tag.NAME + " WHERE " + Tables.Tag.Columns.Name.TAG_TYPE_ID + "=?)"

					, new String[]{bikeId, tagTypeId},
					// Order by
					Tables.Event.Columns.Name.TIMESTAMP + " DESC ");

			if (c.moveToFirst()) {
				do {
					Event event = Utils.buildEventFromCursor(c);
					events.add(event);
				} while (c.moveToNext());
			}
		}
		//Only selected Tag IDs
		else {
			//--- Events: Get all Events for the Bike / TagType ---//
		/*
		* SELECT * FROM events
       WHERE bike_id = ?
       AND tag_id IN (SELECT id
                          FROM tags
                          WHERE tag_type_id = ?)
     */
			events.clear();
		  ArrayList<String> args = new ArrayList<>();
			args.add(bikeId);
			args.addAll(tagIds);

				c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null,

					Tables.Event.Columns.Name.BIKE_ID + "=? AND "
							+ Tables.Event.Columns.Name.TAG_ID + " IN  (" + makePlaceholders(tagIds.size()) + ") "
					, args.toArray(new String[]{}),
					// Order by
					Tables.Event.Columns.Name.TIMESTAMP + " DESC ");

			if (c.moveToFirst()) {
				do {
					Event event = Utils.buildEventFromCursor(c);
					events.add(event);
				} while (c.moveToNext());
			}
		}

	}
	private String makePlaceholders(int len) {
		if (len < 1) {
			// It will lead to an invalid query anyway ..
			throw new RuntimeException("No placeholders");
		} else {
			StringBuilder sb = new StringBuilder(len * 2 - 1);
			sb.append("?");
			for (int i = 1; i < len; i++) {
				sb.append(",?");
			}
			return sb.toString();
		}
	}
	private void refreshUI() {

		listItems.clear();
		EventItem item;
		for (Event event : events) {
			item = new EventItem(event, tags.get(event.getTagId()));
			listItems.add(item);
		}

		aa.notifyDataSetChanged();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragmant_event_list, container, false);

		listView = (ListView) view.findViewById(R.id.eventList);
		tagTypeId = getArguments().getString(Args.TAG_TYPE_ID);
		bikeId = getArguments().getString(Args.BIKE_ID);
		tagIds = getArguments().getStringArrayList(Args.TAG_IDS);

		loadDataFromProvider();

		int resIdItem = R.layout.item;
		aa = new EventListItemAdapter(getActivity(), resIdItem, listItems);
		listView.setAdapter(aa);

		refreshUI();

		return view;
	}

	public static final class Args {
		public static final String BIKE_ID = "ITEM_ID";
		public static final String TAG_TYPE_ID = "TAG_TYPE_ID";
		public static final String TAG_IDS = "TAG_IDS";
	}

}
