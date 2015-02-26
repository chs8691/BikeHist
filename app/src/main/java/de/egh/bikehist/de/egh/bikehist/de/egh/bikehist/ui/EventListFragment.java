package de.egh.bikehist.de.egh.bikehist.de.egh.bikehist.ui;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
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
import de.egh.bikehist.de.egh.bikehist.model.Bike;
import de.egh.bikehist.de.egh.bikehist.model.Event;
import de.egh.bikehist.de.egh.bikehist.model.Tag;
import de.egh.bikehist.de.egh.bikehist.model.TagType;
import de.egh.bikehist.de.egh.bikehist.model.Utils;
import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

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
	private String actualTagTypeId;
	private String actualBikeId;

	/** Select data from persistance. */
	private void loadDataFromProvider() {

		ContentResolver cr = getActivity().getContentResolver();

		//--- TagTypes: Get the one TagType ---//
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null,
				Tables.TagType.Columns.Name.ID + "=?", new String[]{actualTagTypeId}, null);

//		if (c.moveToFirst()) {
//			actualTagType = Utils.buildTagTypeFromCursor(c);
//		} else {
//			Log.w(TAG, "Unknown TagType " + actualTagTypeId);
//			actualTagType = null;
//			return;
//		}

		//--- Tags: All Tags of the TagType ---//
		tags.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
				Tables.Tag.Columns.Name.TAG_TYPE_ID + "=?", new String[]{actualTagTypeId}, null);

		if (c.moveToFirst()) {
			do {
				Tag tag = Utils.buildTagFromCursor(c);
				tags.put(tag.getId(), tag);
			} while (c.moveToNext());
		}

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

				, new String[]{actualBikeId, actualTagTypeId},
				// Order by
				Tables.Event.Columns.Name.TIMESTAMP + " DESC ");

		if (c.moveToFirst()) {
			do {
				Event event = Utils.buildEventFromCursor(c);
				events.add(event);
			} while (c.moveToNext());
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
		actualTagTypeId = getArguments().getString(Args.TAG_TYPE_ID);
		actualBikeId = getArguments().getString(Args.BIKE_ID);

		loadDataFromProvider();

		int resIdItem = R.layout.item;
		aa = new EventListItemAdapter(getActivity(), resIdItem, listItems);
		listView.setAdapter(aa);

		refreshUI();

		return view;
	}

	public static final class Args {
		public static final String TAG_TYPE_ID = "TAG_TYPE_ID";
		public static final String BIKE_ID = "BIKE_ID";
	}

}
