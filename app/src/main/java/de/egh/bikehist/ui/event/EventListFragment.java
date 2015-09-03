package de.egh.bikehist.ui.event;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;
import de.egh.bikehist.ui.ListCallbacks;


/**
 * Created by ChristianSchulzendor on 13.02.2015.
 */
public class EventListFragment extends ListFragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = EventListFragment.class.getSimpleName();
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated event_item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	/**
	 * A dummy implementation of the {@link de.egh.bikehist.ui.ListCallbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static final ListCallbacks sDummyListCallbacks = new ListCallbacks() {
		@Override
		public void onItemSelected(UUID id, String type) {
		}
	};
	private final ArrayList<EventItem> listItems = new ArrayList<>();
	private final Map<UUID, Tag> tags = new HashMap<>();
	//	private TagType actualTagType;
	private final List<Event> events = new ArrayList<>();
	/**
	 * The fragment's current callback object, which is notified of list event_item
	 * clicks.
	 */
	private ListCallbacks mListCallbacks = sDummyListCallbacks;
	private EventListItemAdapter aa;
	private ListView listView;
	private String tagTypeId;
	private String bikeId;

	private List<String> tagIds;
	/**
	 * The current activated event_item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;
	private TextView headerView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.event_list, null);
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

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

//		setEmptyText(getString(R.string.emptyList));

		//Filter
		tagTypeId = getArguments().getString(Args.TAG_TYPE_ID);
		bikeId = getArguments().getString(Args.BIKE_ID);
		tagIds = getArguments().getStringArrayList(Args.TAG_IDS);

		if (tagTypeId != null) {
			headerView.setText(
					new EntityLoader(getActivity()).tagType(tagTypeId).getName());
		}


		aa = new EventListItemAdapter(getActivity(), listItems);
		setListAdapter(aa);

		getLoaderManager().initLoader(Constants.LoaderId.TAG, null, this);

	}

	/**
	 * Call this after event data have been changed.
	 */
	public void refresh() {
		events.clear();
		listItems.clear();
		tags.clear();
		getLoaderManager().destroyLoader(Constants.LoaderId.TAG);
		getLoaderManager().destroyLoader(Constants.LoaderId.EVENT);
		getLoaderManager().initLoader(Constants.LoaderId.TAG, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
		switch (loaderId) {
			case Constants.LoaderId.TAG:
				//--- Tags: All Tags of the TagType ---//
				tags.clear();
				return new CursorLoader(
						getActivity(),
						Tables.Tag.URI,
						null,
						Tables.Tag.TagTypeId.NAME + "=?",
						new String[]{tagTypeId},
						null);

			case Constants.LoaderId.EVENT:
				events.clear();
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

					return new CursorLoader(getActivity(),
							Tables.Event.URI,
							null,
							Tables.Event.BikeId.NAME + "=? AND "
									+ Tables.BikeHistEntity.Deleted.NAME + "=? AND "
									+ Tables.Event.TagId.NAME + " IN (SELECT " + Tables.Tag.Id.NAME + " FROM "
									+ Tables.Tag.NAME + " WHERE " + Tables.Tag.TagTypeId.NAME + "=?)",

							new String[]{bikeId,
									BikeHistProvider.BikeHistContract.Boolean.False.asString,
									tagTypeId },
							// Order by
							Tables.Event.Timestamp.NAME + " DESC ");


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

					ArrayList<String> args = new ArrayList<>();
					args.add(bikeId);
					args.addAll(tagIds);

					return new CursorLoader(getActivity(),
							Tables.Event.URI,
							null,
							Tables.Event.BikeId.NAME + "=? AND "
									+ Tables.Event.TagId.NAME + " IN  (" + makePlaceholders(tagIds.size()) + ") ",
							args.toArray(new String[args.size()]),
							// Order by
							Tables.Event.Timestamp.NAME + " DESC ");
				}
		}
		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof ListCallbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mListCallbacks = (ListCallbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mListCallbacks = sDummyListCallbacks;
	}


	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an event_item has been selected.
		mListCallbacks.onItemSelected(listItems.get(position).getEvent().getId(), null);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_event_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

		switch (cursorLoader.getId()) {
			case Constants.LoaderId.TAG:

				if (cursor != null && cursor.moveToFirst()) {
					do {
						Tag tag = EntityUtilsFactory.createTagUtils(getActivity()).build(cursor);
						tags.put(tag.getId(), tag);
					} while (cursor.moveToNext());
				}
				getLoaderManager().initLoader(Constants.LoaderId.EVENT, null, this);

				break;

			case Constants.LoaderId.EVENT:
				if (cursor != null && cursor.moveToFirst()) {
					do {
						Event event = EntityUtilsFactory.createEventUtils(getActivity()).build(cursor);
						events.add(event);
					} while (cursor.moveToNext());
				}

				listItems.clear();
				EventItem item;
				for (Event event : events) {
					item = new EventItem(event, tags.get(event.getTagId()));
					listItems.add(item);
				}
				aa.notifyDataSetChanged();
				// The list should now be shown.
//				if (isResumed()) {
//					setListShown(true);
//				} else {
//					setListShownNoAnimation(true);
//				}
				break;
			default:
				Log.e(TAG, "Unknown Loader EVENT_ID " + cursorLoader.getId());
		}

	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(activateOnItemClick
				? ListView.CHOICE_MODE_SINGLE
				: ListView.CHOICE_MODE_NONE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated event_item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated event_item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}

//		TagType tagType = new EntityLoader(getActivity().getContentResolver()).tagType(tagTypeId);
		headerView = (TextView) view.findViewById(R.id.listHeader);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}

	private abstract static class Contract {
		public static final String ITEM_ID = "ENTITY_ID";
	}

	private abstract static class Constants {
		private static abstract class LoaderId {
			private static final int TAG = 0;
			private static final int EVENT = 1;
		}
	}

	public static final class Args {
		public static final String BIKE_ID = "ENTITY_ID";
		public static final String TAG_TYPE_ID = "TAG_TYPE_ID";
		public static final String TAG_IDS = "TAG_IDS";
	}

}
