package de.egh.bikehist.ui.drawer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.egh.bikehist.AppUtils;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;

/**
 * Contains all data for the TagType and corresponding Tags to show in the drawer.
 */
public class DrawerController {
	private static final String TAG = DrawerController.class.getSimpleName();
	private final BikeListItemAdapter aaBikeList;
	private final TagTypeListItemAdapter aaTagTypeList;
	private final Context context;
	private final ListView drawerBikeList;
	private final ListView drawerTagTypeList;
	private final ListView drawerTagList;
	private final TagListItemAdapter aaTagList;
	//Don't kill this instance
	private final Bikes bikes = new Bikes();
	//Don't kill this instance
	private final TagTypes tagTypes = new TagTypes();
	private Callbacks callbacks;

	public DrawerController(Context context, ListView drawerBikeList, ListView drawerTagTypeList,
	                        ListView drawerTagList, Callbacks callbacks) {

		this.context = context;

		this.drawerBikeList = drawerBikeList;
		this.drawerTagTypeList = drawerTagTypeList;
		this.drawerTagList = drawerTagList;
		this.callbacks = callbacks;

		//Drawer: List of Bikes
		aaBikeList = new BikeListItemAdapter(context, bikes.getList());
		drawerBikeList.setAdapter(aaBikeList);
		drawerBikeList.setOnItemClickListener(new DrawerBikeItemClickListener());

		//Drawer: List of TagTypes
		aaTagTypeList = new TagTypeListItemAdapter(context, tagTypes.getList());
		drawerTagTypeList.setAdapter(aaTagTypeList);
		drawerTagTypeList.setOnItemClickListener(new DrawerTagTypeItemClickListener());

		//Drawer: List of Tags
		aaTagList = new TagListItemAdapter(context, tagTypes.getActualTagItems());
		drawerTagList.setAdapter(aaTagList);
		drawerTagList.setOnItemClickListener(new DrawerTagItemClickListener());


		onChange();
	}

	public void setListViewHeightBasedOnChildren(ListView listView) {
		ArrayAdapter listAdapter = (ArrayAdapter) listView.getAdapter();
		if (listAdapter == null) {
			// pre-condition
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	/**
	 * Call this after data have been changed
	 */
	public void onChange() {
		loadDataFromProvider();
		loadPrefs();
		aaTagList.notifyDataSetChanged();
		aaBikeList.notifyDataSetChanged();
		aaTagTypeList.notifyDataSetChanged();

		//Calculate height for scrolling
		setListViewsHeight();

	}

	/**
	 * ListView in ScrollView-Issue: ListView have not to be scrollable.
	 * So we have to maximaize their heights.
	 */
	private void setListViewsHeight() {
		//Show all items of the lists
		setListViewHeightBasedOnChildren(drawerBikeList);
		setListViewHeightBasedOnChildren(drawerTagTypeList);
		setListViewHeightBasedOnChildren(drawerTagList);

	}

	public void onStop() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = context.getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Constants.Pref.Keys.ACTUAL_BIKE_ID, bikes.getSelectedBike() == null
				? "" : bikes.getSelectedBike().getId().toString());
		editor.putString(Constants.Pref.Keys.ACTUAL_TAG_TYPE_ID, tagTypes.getSelectedTagType() == null
				? "" : tagTypes.getSelectedTagType().getId().toString());

		Set<String> selectedString = new HashSet<>();
		for (TagTypeItem tagTypeItem : tagTypes.getList()) {
			for (TagItem tagItem : tagTypeItem.getTags()) {
				if (tagItem.isChecked()) {
					selectedString.add(tagTypeItem.getTagType().getId().toString() + " "
							+ tagItem.getTag().getId().toString());
				}
			}
		}
		editor.putStringSet(Constants.Pref.Keys.ACTUAL_TAG_IDS, selectedString);

		// Commit the edits!
		editor.apply();
	}

	/**
	 * Returns null, if no TagType is selected.
	 */
	public TagType getSelectedTagType() {
		return tagTypes.getSelectedTagType();
	}

	/**
	 * Returns all selected Tags for the actual TagType.
	 *
	 * @return List with selected Tags. Can be empty.
	 */
	public List<Tag> getSelectedTags() {
		List<Tag> tags = new ArrayList<>();
		for (TagItem item : tagTypes.getActualTagItems()) {
			if (item.isChecked()) {
				tags.add(item.getTag());
			}
		}

		return tags;
	}

	/**
	 * Returns null, if no Bike selected.
	 */
	public Bike getSelectedBike() {
		return bikes.getSelectedBike();
	}

	/**
	 * Set the selections for the lists. There must always be a bike and a type selected.
	 */
	private void loadPrefs() {

		SharedPreferences sharedPrefs = context.getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0);
		//Load last status from Prefs and validate this: Bike
		//Validate actual value or, if there is no actual value, take first entry as default

		String bikeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_BIKE_ID, "");
		if (!bikeIdString.isEmpty()) {
			try {
				//Select this one, if EVENT_ID is valid
				if (!bikes.setSelectedItem(UUID.fromString(bikeIdString))) {
					//Invalid EVENT_ID, select first entry
					bikes.setSelectedItem(0);
				}
			} catch (IllegalArgumentException e) {
				Log.v(TAG, "Invalid Bike id from preferences " + bikeIdString);
			}
		}
		//First call: select first bike
		else if (bikes.getList().size() > 0) {
			bikes.setSelectedItem(0);
		}

		//Same for TagType
		String tagTypeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_TAG_TYPE_ID, "");
		if (!tagTypeIdString.isEmpty()) {
			try {
				//Select this one, if EVENT_ID is valid
				if (!tagTypes.setSelectedItem(UUID.fromString(tagTypeIdString))) {
					//Invalid EVENT_ID, select first entry
					tagTypes.setSelectedItem(0);
				}
			} catch (IllegalArgumentException e) {
				Log.v(TAG, "Invalid TagType id from preferences " + tagTypeIdString);
			}
		}
		//First call: select first Tag Type
		else if (tagTypes.getList().size() > 0) {
			tagTypes.setSelectedItem(0);
		}

		Set<String> tagStringSet = sharedPrefs.getStringSet(Constants.Pref.Keys.ACTUAL_TAG_IDS, null);
		if (tagStringSet != null) {
			//String has two parts, separated by space: EVENT_ID of TagType + ' ' + Tag
			for (String line : tagStringSet) {
				String[] parts = line.split(" ");
				if (parts.length == 2) {
					try {
						tagTypes.addSelectedTag(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
					} catch (Exception e) {
						Log.v(TAG, "IDs for TagType / Tag could not be solved.");
					}
				}
			}
		}

	}

	/**
	 * Select data from storage.
	 */
	private void loadDataFromProvider() {

		ContentResolver cr = context.getContentResolver();


		//--- TagTypes: Return all the saved data ---//
		tagTypes.clear();
		Cursor c = cr.query(
				BikeHistProvider.CONTENT_URI_TAG_TYPES,
				null,
				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME + "=?",
				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				TagType tagType = EntityUtilsFactory.createTagTypeUtils(context).build(c);
				tagTypes.add(tagType);
			} while (c.moveToNext());
		}


		//--- Tags: Return all the saved data ---//
		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS,
				null,
				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME + "=?",
				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				tagTypes.addTag(EntityUtilsFactory.createTagUtils(context).build(c));
			} while (c.moveToNext());
		}

		//--- Bikes: Return all the saved data ---//
		bikes.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_BIKES,
				null,
				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME + "=?",
				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				bikes.add(EntityUtilsFactory.createBikeUtils(context).build(c));
			} while (c.moveToNext());
		}

	}

//	/** Build list with all Tags for the actual TagType */
//	private void fillTags() {
//		tagTypes.fillTagItemsForSelectedTagType(tagItems);
//		aaTagList.notifyDataSetChanged();
//	}

	/**
	 * Events for the consumer
	 */
	public interface Callbacks {

		/**
		 * Fired, when a item was selected.
		 */
		void onDrawerControllerSelectionChanged();
	}

	private static final class Constants {
		static final class Pref {
			static final class Keys {
				static final String ACTUAL_BIKE_ID = "ACTUAL_BIKE_ID";
				static final String ACTUAL_TAG_TYPE_ID = "ACTUAL_TAG_TYPE_ID";
				static final String ACTUAL_TAG_IDS = "ACTUAL_TAG_IDS";
			}
		}
	}

	private class DrawerTagItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			boolean check = !tagTypes.getActualTagItems().get(position).isChecked();
			drawerTagList.setItemChecked(position, check);
			tagTypes.getActualTagItems().get(position).setChecked(check);
			callbacks.onDrawerControllerSelectionChanged();
		}

	}

	private class DrawerBikeItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			bikes.setSelectedItem(position);
			drawerBikeList.setItemChecked(position, true);
			callbacks.onDrawerControllerSelectionChanged();

		}

	}

	private class DrawerTagTypeItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			tagTypes.setSelectedItem(position);
			drawerTagTypeList.setItemChecked(position, true);
			//Corresponding TagList was updated by setSelectedItem()
			aaTagList.notifyDataSetChanged();

			setListViewsHeight();
			callbacks.onDrawerControllerSelectionChanged();

		}

	}

	/**
	 * Only instantiate TagTypes once, because ArrayAdapter handles the reference to the List.
	 */
	private class TagTypes {

		private final List<TagTypeItem> tagTypeItems = new ArrayList<>();
		private final List<TagItem> actualTagItems = new ArrayList<>();

		/**
		 * Returns reference to List of the selected TagItem for the actual TagType.
		 * Don't edit the list.
		 *
		 * @return List can be empty
		 */
		public List<TagItem> getActualTagItems() {
			return actualTagItems;
		}

		/**
		 * Returns reference of List. Only use this for the ArrayAdapter!
		 */
		public List<TagTypeItem> getList() {
			return tagTypeItems;
		}

		public void clear() {
			tagTypeItems.clear();
		}

		/**
		 * Set actual TagType (there can only be one selected) and loads Tags of this TagType.
		 *
		 * @param pos Non negative integer value.
		 * @return true, if checked was set successfully.
		 */
		public boolean setSelectedItem(int pos) {
			if (pos < 0 || pos >= tagTypeItems.size()) {
				return false;
			}

			for (int i = 0; i < tagTypeItems.size(); i++) {
				if (i == pos) {
					tagTypeItems.get(i).setChecked(true);

					//Fill TagList, a copy of selected TagType's Tags.
					//ArrayAdapter will need this
					actualTagItems.clear();
					for (TagItem tagItem : tagTypeItems.get(i).getTags()) {
						actualTagItems.add(tagItem);
					}
				} else {
					tagTypeItems.get(i).setChecked(false);
				}
			}
			return true;

		}

		public void addTag(Tag tag) {
			for (TagTypeItem item : tagTypeItems) {
				if (item.getTagType().getId().equals(tag.getTagTypeId())) {
					item.addTag(tag);
					return;
				}
			}
		}

		/**
		 * Sets the actual TagType and update the corresponding TagList
		 *
		 * @param id UUID of the TagType
		 * @return true if TagType could be selected
		 */
		public boolean setSelectedItem(UUID id) {
			if (id == null) {
				return false;
			}

			//Delegate to position based select method.
			for (int i = 0; i < tagTypeItems.size(); i++) {
				if (tagTypeItems.get(i).getTagType().getId().equals(id)) {
					return setSelectedItem(i);
				}
			}

			return false;
		}

		/**
		 * Doesn't care for duplicate entries.
		 */
		public void add(TagType tagType) {
			tagTypeItems.add(new TagTypeItem(tagType));
		}

		/**
		 * Returns null, if no TagType is selected.
		 */
		public TagType getSelectedTagType() {
			for (TagTypeItem item : tagTypeItems) {
				if (item.isChecked()) {
					return item.getTagType();
				}
			}
			return null;
		}

		/**
		 * Only updates first found TagType/Tag combination.
		 *
		 * @param tagTypeId UUID of the TagType
		 * @param tagId     UUID of the Tag
		 * @return true, if Tag could be found and checked.
		 */
		public void addSelectedTag(UUID tagTypeId, UUID tagId) {
			if (tagTypeId == null || tagId == null) {
				return;
			}

			for (TagTypeItem item : tagTypeItems) {
				if (item.getTagType().getId().equals(tagTypeId)) {
					for (TagItem tagItem : item.getTags()) {
						if (tagItem.getTag().getId().equals(tagId)) {
							tagItem.setChecked(true);
							return;
						}
					}
				}
			}
		}

	}

//	/** Only instantiate Tags once, because ArrayAdapter handles the reference to the List. */
//	private class Tags {
//
//		private List<TagItem> tagItems = new ArrayList<>();
//
//		/** Returns reference of List. Only use this for the ArrayAdapter! */
//		public List<TagItem> getList() {
//			return tagItems;
//		}
//
//		public void clear() {
//			tagItems.clear();
//		}
//
//		/**
//		 Returns true, if checked was set successfully.
//
//		 @param pos
//		 Non negative integer value.
//		 */
//		public boolean setSelectedItem(int pos) {
//			if (pos < 0 || pos >= tagItems.size()) {
//				return false;
//			}
//
//			for (int i = 0; i < tagItems.size(); i++) {
//				tagItems.get(i).setChecked(i == pos);
//			}
//			return true;
//
//		}
//
//		/** Returns true, if checked was set successfully. */
//		public boolean setSelectedItem(UUID id) {
//			if (id == null) {
//				return false;
//			}
//
//			// Check id
//			boolean valid = false;
//			for (TagItem event_item : tagItems) {
//				if (event_item.getTag().getId().equals(id)) {
//					valid = true;
//				}
//			}
//			if (valid) {
//				for (TagItem event_item : tagItems) {
//					event_item.setChecked(event_item.getTag().getId().equals(id));
//				}
//				return true;
//			} else {
//				//Unknown EVENT_ID
//				return false;
//			}
//
//		}
//
//		/** Doesn't care for duplicate entries. */
//		public void add(Tag tag) {
//			tagItems.add(new TagItem(tag));
//		}
//
//		/** Returns null, if no Tag is selected. */
//		public Tag getSelectedTag() {
//			for (TagItem event_item : tagItems) {
//				if (event_item.isChecked()) {
//					return event_item.getTag();
//				}
//			}
//			return null;
//		}
//
//	}

	/**
	 * Only instantiate Bikes once, because ArrayAdapter handles the reference to the List.
	 */
	private class Bikes {

		private final List<BikeItem> bikeItems = new ArrayList<>();

		/**
		 * Returns reference of List. Only use this for the ArrayAdapter!
		 */
		public List<BikeItem> getList() {
			return bikeItems;
		}

		public void clear() {
			bikeItems.clear();
		}

		/**
		 * Returns true, if checked was set successfully.
		 *
		 * @param pos Non negative integer value.
		 */
		public void setSelectedItem(int pos) {
			if (pos < 0 || pos >= bikeItems.size()) {
				return;
			}

			for (int i = 0; i < bikeItems.size(); i++) {
				bikeItems.get(i).setChecked(i == pos);
			}

		}

		/**
		 * Returns true, if checked was set successfully.
		 */
		public boolean setSelectedItem(UUID id) {
			if (id == null) {
				return false;
			}

			// Check id
			boolean valid = false;
			for (BikeItem item : bikeItems) {
				if (item.getBike().getId().equals(id)) {
					valid = true;
				}
			}
			if (valid) {
				for (BikeItem item : bikeItems) {
					item.setChecked(item.getBike().getId().equals(id));
				}
				return true;
			} else {
				//Unknown EVENT_ID
				return false;
			}

		}

		/**
		 * Doesn't care for duplicate entries.
		 */
		public void add(Bike bike) {
			bikeItems.add(new BikeItem(bike));
		}

		/**
		 * Returns null, if no Bike is selected.
		 */
		public Bike getSelectedBike() {
			for (BikeItem item : bikeItems) {
				if (item.isChecked()) {
					return item.getBike();
				}
			}
			return null;
		}

	}

	/**
	 * An entry in the shown list of bikes
	 */
	class BikeItem {
		private final Bike bike;
		private boolean checked;

		private BikeItem(Bike bike) {
			this.bike = bike;
		}

		public Bike getBike() {
			return bike;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}
	}

	/**
	 * An entry in the shown list of TagTypes
	 */
	public class TagTypeItem {
		private final List<TagItem> tags = new ArrayList<>();
		private final TagType tagType;
		private boolean checked;

		private TagTypeItem(TagType tagType) {
			this.tagType = tagType;
		}

		public List<TagItem> getTags() {
			return tags;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}

		public TagType getTagType() {

			return tagType;
		}

		public void addTag(Tag tag) {
			tags.add(new TagItem(tag));
		}
	}

	public class TagItem {
		private final Tag tag;
		private boolean checked;

		private TagItem(Tag tag) {
			this.tag = tag;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}

		public Tag getTag() {
			return tag;
		}
	}
}
