package de.egh.bikehist.masterdata;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils;
import de.egh.bikehist.persistance.BikeHistProvider;

/**
 A list fragment representing a list of Items. This fragment
 also supports tablet devices by allowing list items to be given an
 'activated' state upon selection. This helps indicate which item is
 currently being viewed in a {@link MasterDataDetailFragment}.
 <p/>
 Activities containing this fragment MUST implement the {@link Callbacks}
 interface.
 The handled ID of an item in this master/detail-Fragments is the database table field _id.
 */
public class MasterDataListFragment extends ListFragment
		implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = MasterDataListFragment.class.getSimpleName();
	/**
	 The serialization (saved instance state) Bundle key representing the
	 activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	/**
	 A dummy implementation of the {@link Callbacks} interface that does
	 nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(UUID id) {
		}
	};
	/**
	 The fragment's current callback object, which is notified of list item
	 clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;
//	SimpleCursorAdapter mAdapter;
//	StructuredTagArrayAdapter structuredTagItemListAdapter;


	/**
	 The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	private ListStrategy strategy;

	/**
	 Mandatory empty constructor for the fragment manager to instantiate the
	 fragment (e.g. upon screen orientation changes).
	 */
	public MasterDataListFragment() {
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.v(TAG, "onActivityCreated()");

		setEmptyText("Empty list");

		String type;
		type = getActivity().getSharedPreferences(MasterDataListActivity.Contract.Preferences.NAME, 0)
				.getString(MasterDataListActivity.Contract.Preferences.PREF_TYPE, MasterDataContract.Type.Values.BIKE);
		if (type.equals(MasterDataContract.Type.Values.BIKE)) {
			strategy = new BikeStrategy();
		} else if (type.equals(MasterDataContract.Type.Values.TAG_TYPE)) {
			strategy = new TagTypeStrategy();
		} else if (type.equals(MasterDataContract.Type.Values.TAG)) {
			strategy = new TagStrategy();
		} else {
			Log.e(TAG, "Unknown type " + type);
			return;
		}

		setListAdapter(strategy.getAdapter());
		setListShown(false);

		// Prepare the loader.  Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(strategy.getLoaderId(), null, this);

	}

	/** Call this after data has been changed in the details edit mode. */
	public void refreshUI() {
		strategy.refreshUI();
		getLoaderManager().restartLoader(strategy.getLoaderId(), null, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(strategy.onListItemClick(position));

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 Turns on activate-on-click mode. When this mode is on, list items will be
	 given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(activateOnItemClick
				? ListView.CHOICE_MODE_SINGLE
				: ListView.CHOICE_MODE_NONE);
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
	public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

		Log.v(TAG, "onCreateLoader()");
		// This is called when a new Loader needs to be created.  This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.

		return strategy.onCreateLoader();

	}

	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> objectLoader, Cursor data) {
		Log.v(TAG, "onLoadFinished()");

		strategy.onLoadFinished(data);

	}

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> objectLoader) {
		Log.v(TAG, "onLoaderReset()");

		/*
		 * Clears out the adapter's reference to the Cursor.
     * This prevents memory leaks.
     */
		strategy.onLoaderReset();
	}

	/**
	 A callback interface that all activities containing this fragment must
	 implement. This mechanism allows activities to be notified of item
	 selections.
	 */
	public interface Callbacks {
		/**
		 Callback for when an item has been selected.
		 */
		public void onItemSelected(UUID id);
	}

	/** Private constant definitions */
	private static abstract class Constants {
		static abstract class LoaderId {
			static final int BIKES = 1;
			static final int TAG_TYPES = 2;
			static final int TAGS = 3;
		}
	}

	/** Dependent coding for master data types. Every master data type has its own strategy. */
	private static abstract class ListStrategy {
		/** Cursor loader */
		protected int loaderId;
		/** Master data type */
		private String type;

		private ListStrategy(String type, int loaderId) {
			this.type = type;
			this.loaderId = loaderId;
		}

		/** Returns the ID of the selected item. */
		abstract UUID onListItemClick(int position);

		abstract void onLoadFinished(Cursor data);

		public int getLoaderId() {
			return loaderId;
		}

		void refreshUI() {

		}

		abstract void onLoaderReset();

		abstract android.support.v4.content.Loader<Cursor> onCreateLoader();

		public abstract android.widget.ListAdapter getAdapter();
	}

	private class BikeStrategy extends ListStrategy {
		private SimpleCursorAdapter mAdapter;

		private BikeStrategy() {

			super(MasterDataContract.Type.Values.BIKE, Constants.LoaderId.BIKES);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(getActivity(),
					R.layout.master_data_list_item_bike, null,
					new String[]{
							BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.NAME,//
							BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER},
					new int[]{R.id.master_data_list_item_bike_name, R.id.master_data_list_item_bike_frame_number}, 0);
		}

		@Override
		UUID onListItemClick(int position) {
			Cursor c = mAdapter.getCursor();
			c.moveToPosition(position);
			return Utils.buildBikeFromCursor(c).getId();
		}

		@Override
		void onLoadFinished(Cursor data) {
			// Swap the new cursor in.  (The framework will take care of closing the
			// old cursor once we return.)
			mAdapter.swapCursor(data);

			// The list should now be shown.
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		@Override
		void onLoaderReset() {
			mAdapter.changeCursor(null);
		}

		@Override
		Loader<Cursor> onCreateLoader() {

			// Returns a new CursorLoader
			return new CursorLoader(
					getActivity(),   // Parent activity context
					BikeHistProvider.CONTENT_URI_BIKES,        // Table to query
					null,
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.NAME,
//								BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER},     // Projection to return / Columns to return
					null,            // No selection clause
					null,            // No selection arguments
					null             // Default sort order

			);

		}

		@Override
		public ListAdapter getAdapter() {
			return mAdapter;
		}
	}

	private class TagTypeStrategy extends ListStrategy {
		private SimpleCursorAdapter mAdapter;

		private TagTypeStrategy() {
			super(MasterDataContract.Type.Values.TAG_TYPE, Constants.LoaderId.TAG_TYPES);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(getActivity(),
					R.layout.master_data_list_item_tag_type, null,
					new String[]{
							BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME},
					new int[]{R.id.master_data_list_item_tag_type_name}, 0);
		}

		@Override
		UUID onListItemClick(int position) {
			Cursor c = mAdapter.getCursor();
			c.moveToPosition(position);
			return Utils.buildTagTypeFromCursor(c).getId();
		}

		@Override
		void onLoaderReset() {
			mAdapter.changeCursor(null);
		}

		@Override
		void onLoadFinished(Cursor data) {
			// Swap the new cursor in.  (The framework will take care of closing the
			// old cursor once we return.)
			mAdapter.swapCursor(data);

			// The list should now be shown.
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
		}

		@Override
		Loader<Cursor> onCreateLoader() {
			return new CursorLoader(
					getActivity(),   // Parent activity context
					BikeHistProvider.CONTENT_URI_TAG_TYPES,        // Table to query
					null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME},     // Projection to return / Columns to return
					null,            // No selection clause
					null,            // No selection arguments
					null             // Default sort order

			);
		}

		@Override
		public ListAdapter getAdapter() {
			return mAdapter;
		}
	}

	private class TagStrategy extends ListStrategy {
		private List<Tag> tags;
		private List<StructuredTagItem> structuredTagItemList = new ArrayList<>();
		private StructuredTagArrayAdapter structuredTagItemListAdapter;

		private TagStrategy() {
			super(MasterDataContract.Type.Values.TAG, Constants.LoaderId.TAGS);

			tags = new ArrayList<>();
			structuredTagItemList = new ArrayList<>();
			// Create an empty adapter we will use to display the loaded data.
			structuredTagItemListAdapter = new StructuredTagArrayAdapter(getActivity(),
					R.layout.master_data_list_item_tag, structuredTagItemList);
		}

		@Override
		UUID onListItemClick(int position) {
			return structuredTagItemListAdapter.getId(position);
		}

		@Override
		void onLoadFinished(Cursor data) {
			//Store tags in temp. list
			if (loaderId == Constants.LoaderId.TAGS) {
				data.moveToFirst();
				do {
					tags.add(Utils.buildTagFromCursor(data));
				} while (data.moveToNext());
				//Now load Tag Types
				loaderId = Constants.LoaderId.TAG_TYPES;
				getLoaderManager().initLoader(loaderId, null, MasterDataListFragment.this);
			}
			// Tag Types loaded
			else {
				List<TagType> tagTypes = new ArrayList<>();
				data.moveToFirst();
				do {
					tagTypes.add(Utils.buildTagTypeFromCursor(data));
				} while (data.moveToNext());

				structuredTagItemList.clear();
				//Putting both together, sorted by TagType
				for (TagType tagType : tagTypes) {
					int tagPos = 0;
					for (Tag tag : tags) {
						if (tagType.getId().equals(tag.getTagTypeId())) {
							structuredTagItemList.add(new StructuredTagItem(tag, tagType, tagPos++));
						}
					}
					structuredTagItemListAdapter.notifyDataSetChanged();
					// The list should now be shown.
					if (isResumed()) {
						setListShown(true);
					} else {
						setListShownNoAnimation(true);
					}
				}
			}
		}

		@Override
		void refreshUI() {
			//Tags have two steps (Tags and TagTypes), set to step 1
			loaderId = Constants.LoaderId.TAGS;
			tags.clear();
			structuredTagItemList.clear();
		}

		@Override
		void onLoaderReset() {

		}

		@Override
		Loader<Cursor> onCreateLoader() {
			switch (loaderId) {
				case Constants.LoaderId.TAGS:
					return new CursorLoader(
							getActivity(),   // Parent activity context
							BikeHistProvider.CONTENT_URI_TAGS,        // Table to query
							null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.NAME,
//								BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.TAG_TYPE_ID
//						},     // Projection to return / Columns to return
							null,            // No selection clause
							null,            // No selection arguments
							null             // Default sort order
					);

				case Constants.LoaderId.TAG_TYPES:
					// Returns a new CursorLoader
					return new CursorLoader(
							getActivity(),   // Parent activity context
							BikeHistProvider.CONTENT_URI_TAG_TYPES,        // Table to query
							null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME},     // Projection to return / Columns to return
							null,            // No selection clause
							null,            // No selection arguments
							null             // Default sort order

					);
			}
			return null;
		}

		@Override
		public ListAdapter getAdapter() {
			return structuredTagItemListAdapter;
		}
	}

	/** For a structured Tag list. */
	private class StructuredTagItem {
		private Tag tag;
		private TagType tagType;
		private int tagPos;

		private StructuredTagItem(Tag tag, TagType tagType, int tagPos) {
			this.tag = tag;
			this.tagType = tagType;
			this.tagPos = tagPos;

		}

		public Tag getTag() {
			return tag;
		}

		public TagType getTagType() {
			return tagType;
		}

		public int getTagPos() {
			return tagPos;
		}
	}

	/** List of Tags, structured by Tag Types */
	private class StructuredTagArrayAdapter extends ArrayAdapter<StructuredTagItem> {

		private List<StructuredTagItem> tagList;
		private StructuredTagItem item;
		private LayoutInflater vi;
		private String inflater;
		private int resource;

		public StructuredTagArrayAdapter(Context context, int resource, List<StructuredTagItem> tagList) {

			super(context, resource, tagList);
			this.tagList = tagList;
			this.resource = resource;
			inflater = Context.LAYOUT_INFLATER_SERVICE;
		}

		/** ID of the master data. */
		public UUID getId(int position) {
			return tagList.get(position).getTag().getId();
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

			// Show Tag Type' name as Header for Tags
			if (item.getTagPos() == 0) {
				itemView.findViewById(R.id.master_data_list_item_tag_tag_type_name).setVisibility(View.VISIBLE);
				((TextView) itemView.findViewById(R.id.master_data_list_item_tag_tag_type_name))
						.setText(item.getTagType().getName());
			} else {
				((TextView) itemView.findViewById(R.id.master_data_list_item_tag_tag_type_name)).setVisibility(View.GONE);
			}

			((TextView) itemView.findViewById(R.id.master_data_list_item_tag_tag_name))
					.setText(item.getTag().getName());

			return itemView;
		}

	}
}
