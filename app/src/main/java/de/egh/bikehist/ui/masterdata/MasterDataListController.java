package de.egh.bikehist.ui.masterdata;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.ui.ListCallbacks;

import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

/**
 * Controller for master data database access
 */
public class MasterDataListController {
	private static final String TAG = MasterDataListController.class.getSimpleName();
	/**
	 * A dummy implementation of the {@link de.egh.bikehist.ui.ListCallbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static final ListCallbacks sDummyListCallbacks = new ListCallbacks() {
		@Override
		public void onItemSelected(UUID id, String type) {
		}

	};
	private final Context context;
	private final Class entity;
	private final ListView listView;
	private ListStrategy strategy;
	/**
	 * The fragment's current callback object, which is notified of list event_item
	 * clicks.
	 */
	private ListCallbacks mListCallbacks = sDummyListCallbacks;

	public MasterDataListController(Context context, ViewGroup container, LayoutInflater inflater, LoaderManager loaderManager, Class entity) {
		this.context = context;
		this.entity = entity;
		listView = (ListView) inflater.inflate(R.layout.master_data_list2, container, false);

		if (entity.getName().equals(Bike.class.getName()))
			strategy = new BikeStrategy(loaderManager);
		else if (entity.getName().equals(TagType.class.getName()))
			strategy = new TagTypeStrategy(loaderManager);
		else if (entity.getName().equals(Tag.class.getName()))
			strategy = new TagStrategy(loaderManager);
		else {
			throw new IllegalArgumentException("Unknown entity " + entity.getName());
		}

		listView.setAdapter(strategy.getAdapter());

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Notify the active callbacks interface (the activity, if the
				// fragment is attached to one) that an event_item has been selected.
				mListCallbacks.onItemSelected(strategy.onListItemClick(position), strategy.getType());
			}
		});

//		// Prepare the loader.  Either re-connect with an existing one,
//		// or start a new one.
//		loaderManager.initLoader(strategy.getLoaderId(), null, this);

	}


	public boolean isCreateActionPerformed() {
		return strategy.isCreateActionPerformed();
	}

	/**
	 * Call this after data has been changed in the details edit mode.
	 */
	public void restart() {
		strategy.restart();
	}

	/**
	 * EOL: close stuff, if needed. You must destroy the controller after close has finished
	 * (but you don't know, when the strategy has finished :-((
	 */
	public void close() {

		strategy.close();

	}

	public View getView() {

		return listView;

	}


	public void setListCallbacks(ListCallbacks callbacks) {
		if (callbacks == null)
			mListCallbacks = sDummyListCallbacks;
		else
			mListCallbacks = callbacks;
	}

	/**
	 * Private constant definitions
	 */
	private static abstract class Constants {
		static abstract class LoaderId {
			static final int BIKES = 1;
			static final int TAG_TYPES = 2;
			static final int TAG_TYPES_FOR_TAGS = 3;
			static final int TAGS = 4;
		}
	}

	/**
	 * Dependent coding for master data types. Every master data type has its own strategy.
	 */
	private static abstract class ListStrategy implements LoaderManager.LoaderCallbacks<Cursor> {
		/**
		 * Master data type
		 */
		private final String type;
		final LoaderManager loaderManager;
		/**
		 * Cursor loader
		 */
		int loaderId;


		ListStrategy(String type, int loaderId, LoaderManager loaderManager) {
			this.type = type;
			this.loaderId = loaderId;
			this.loaderManager = loaderManager;

			// Prepare the loader.  Either re-connect with an existing one,
			// or start a new one.
			loaderManager.initLoader(loaderId, null, this);


		}

		public abstract void restart();

		/**
		 * Returns the type.
		 */
		public String getType() {
			return type;
		}

		/**
		 * EOL: Call this before tear down.
		 */
		public abstract void close();

		/**
		 * Override this method, if create action's enabling depends on something.
		 */
		public boolean isCreateActionPerformed() {
			return true;
		}

		/**
		 * Returns the EVENT_ID of the selected event_item.
		 */
		abstract UUID onListItemClick(int position);


		public int getLoaderId() {
			return loaderId;
		}


		public abstract android.widget.ListAdapter getAdapter();
	}

	private class BikeStrategy extends ListStrategy {
		private final SimpleCursorAdapter mAdapter;

		public BikeStrategy(LoaderManager loaderManager) {

			super(MasterDataContract.Type.Values.BIKE, Constants.LoaderId.BIKES, loaderManager);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(context,
					R.layout.master_data_list_item_bike, null,
					new String[]{
							BikeHistProvider.BikeHistContract.Tables.Bike.Name.NAME,//
							BikeHistProvider.BikeHistContract.Tables.Bike.FrameNumber.NAME},
					new int[]{R.id.master_data_list_item_bike_name, R.id.master_data_list_item_bike_frame_number}, 0);

			loaderManager.initLoader(getLoaderId(), null, this);
		}

		@Override
		public void restart() {
			loaderManager.restartLoader(getLoaderId(), null, this);
		}

		@Override
		public void close() {
			loaderManager.destroyLoader(getLoaderId());
		}

		@Override
		UUID onListItemClick(int position) {
			Cursor c = mAdapter.getCursor();
			c.moveToPosition(position);
			return EntityUtilsFactory.createBikeUtils(context).build(c).getId();
		}

		@Override
		public ListAdapter getAdapter() {
			return mAdapter;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			// Returns a new CursorLoader
			return new CursorLoader(
					context,   // Parent activity context
					Tables.Bike.URI,        // Table to query
					null,
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.NAME_STRING,
//								BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER},     // Projection to return / Columns to return
					Tables.BikeHistEntity.Deleted.NAME + "=?",
					new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
					null             // Default sort order

			);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			// Swap the new cursor in.  (The framework will take care of closing the
			// old cursor once we return.)
			mAdapter.swapCursor(data);

			listView.refreshDrawableState();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			//Remove any references it has to the Loader's data.
			mAdapter.changeCursor(null);
		}
	}

	private class TagTypeStrategy extends ListStrategy {
		private final SimpleCursorAdapter mAdapter;

		private TagTypeStrategy(LoaderManager loaderManager) {
			super(MasterDataContract.Type.Values.TAG_TYPE, Constants.LoaderId.TAG_TYPES, loaderManager);

			// Create an empty adapter we will use to display the loaded data.
			mAdapter = new SimpleCursorAdapter(context,
					R.layout.master_data_list_item_tag_type, null,
					new String[]{
							BikeHistProvider.BikeHistContract.Tables.TagType.Name.NAME},
					new int[]{R.id.master_data_list_item_tag_type_name}, 0);
			loaderManager.initLoader(getLoaderId(), null, this);

		}

		@Override
		public void close() {
			loaderManager.destroyLoader(getLoaderId());
		}

		@Override
		public void restart() {
			loaderManager.restartLoader(getLoaderId(), null, this);
		}

		@Override
		UUID onListItemClick(int position) {
			Cursor c = mAdapter.getCursor();
			c.moveToPosition(position);
			return EntityUtilsFactory.createTagTypeUtils(context).build(c).getId();
		}

		@Override
		public ListAdapter getAdapter() {
			return mAdapter;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(
					context,   // Parent activity context
					Tables.TagType.URI,        // Table to query
					null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME_STRING},     // Projection to return / Columns to return
					Tables.BikeHistEntity.Deleted.NAME + "=?",
					new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
					null             // Default sort order

			);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			// Swap the new cursor in.  (The framework will take care of closing the
			// old cursor once we return.)
			mAdapter.swapCursor(data);
			listView.refreshDrawableState();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			//Remove any references it has to the Loader's data.
			mAdapter.changeCursor(null);
		}
	}

	private class TagStrategy extends ListStrategy {
		private final List<Tag> tags;
		private final StructuredTagArrayAdapter structuredTagItemListAdapter;
		private final List<TagType> tagTypes = new ArrayList<>();
		private List<StructuredTagItem> structuredTagItemList = new ArrayList<>();

		private TagStrategy(LoaderManager loaderManager) {
			super(MasterDataContract.Type.Values.TAG, Constants.LoaderId.TAGS, loaderManager);

			tags = new ArrayList<>();
			structuredTagItemList = new ArrayList<>();
			// Create an empty adapter we will use to display the loaded data.
			structuredTagItemListAdapter = new StructuredTagArrayAdapter(context,
					structuredTagItemList);
			loaderManager.initLoader(getLoaderId(), null, this);

		}

		@Override
		public void restart() {

			loaderManager.destroyLoader(Constants.LoaderId.TAGS);
			loaderManager.destroyLoader(Constants.LoaderId.TAG_TYPES_FOR_TAGS);

			loaderId = Constants.LoaderId.TAGS;

			tags.clear();
			tagTypes.clear();
			structuredTagItemList.clear();

			loaderManager.initLoader(getLoaderId(), null, this);
		}

		@Override
		public void close() {
			loaderManager.destroyLoader(Constants.LoaderId.TAG_TYPES_FOR_TAGS);
			loaderManager.destroyLoader(Constants.LoaderId.TAGS);
		}

		@Override
		public boolean isCreateActionPerformed() {
			//Only if there is at least one Tag Type, a Tag can be created.
			return tagTypes.size() > 0;
		}

		@Override
		UUID onListItemClick(int position) {
			return structuredTagItemListAdapter.getId(position);
		}


		@Override
		public ListAdapter getAdapter() {
			return structuredTagItemListAdapter;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (loaderId) {
				case Constants.LoaderId.TAGS:
					return new CursorLoader(
							context,   // Parent activity context
							Tables.Tag.URI,        // Table to query
							null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.NAME_STRING,
//								BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.TAG_TYPE_ID
//						},     // Projection to return / Columns to return
							Tables.BikeHistEntity.Deleted.NAME + "=?",
							new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
							null             // Default sort order
					);

				case Constants.LoaderId.TAG_TYPES_FOR_TAGS:
					// Returns a new CursorLoader
					return new CursorLoader(
							context,   // Parent activity context
							Tables.TagType.URI,        // Table to query
							null, //get all attributes for Utils.Build...
//						new String[]{
//								BikeHistProvider.BikeHistContract.Tables._ID,
//								BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME_STRING},     // Projection to return / Columns to return
							Tables.BikeHistEntity.Deleted.NAME + "=?",
							new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
							null             // Default sort order

					);
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			//Store tags in temp. list
			if (loaderId == Constants.LoaderId.TAGS) {
				if (data.getCount() > 0) {
					data.moveToFirst();
					do {
						tags.add(EntityUtilsFactory.createTagUtils(context).build(data));
					} while (data.moveToNext());
				}
				//Now load Tag Types
				loaderId = Constants.LoaderId.TAG_TYPES_FOR_TAGS;

				loaderManager.initLoader(loaderId, null, this);
			}
			// Tag Types loaded
			else {
				tagTypes.clear();
				if (data.getCount() > 0) {
					data.moveToFirst();
					do {
						tagTypes.add(EntityUtilsFactory.createTagTypeUtils(context).build(data));
					} while (data.moveToNext());
				}
//				data.close();

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
				}

			}

		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
		}
	}

	/**
	 * For a structured Tag list.
	 */
	private class StructuredTagItem {
		private final Tag tag;
		private final TagType tagType;
		private final int tagPos;

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

	/**
	 * List of Tags, structured by Tag Types
	 */
	private class StructuredTagArrayAdapter extends ArrayAdapter<StructuredTagItem> {

		private final List<StructuredTagItem> tagList;
		private final String inflater;
		private final int resource;
		private StructuredTagItem item;
		private LayoutInflater vi;

		public StructuredTagArrayAdapter(Context context, List<StructuredTagItem> tagList) {

			super(context, R.layout.master_data_list_item_tag, tagList);
			this.tagList = tagList;
			this.resource = R.layout.master_data_list_item_tag;
			inflater = Context.LAYOUT_INFLATER_SERVICE;
		}

		/**
		 * EVENT_ID of the master data.
		 */
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
				itemView.findViewById(R.id.listHeader).setVisibility(View.VISIBLE);
				((TextView) itemView.findViewById(R.id.listHeader))
						.setText(item.getTagType().getName());
			} else {
				itemView.findViewById(R.id.listHeader).setVisibility(View.GONE);
			}

			((TextView) itemView.findViewById(R.id.master_data_list_item_tag_tag_name))
					.setText(item.getTag().getName());

			return itemView;
		}

	}

}
