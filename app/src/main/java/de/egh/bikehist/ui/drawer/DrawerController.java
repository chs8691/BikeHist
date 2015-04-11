package de.egh.bikehist.ui.drawer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils;
import de.egh.bikehist.persistance.BikeHistProvider;

/** Contains all data for the TagType and corresponding Tags to show in the drawer. */
public class DrawerController {
	private static final String TAG = DrawerController.class.getSimpleName();

	private Context context;
	private ListView drawerBikeList;
	private ListView drawerTagTypeList;
	private ListView drawerTagList;
	private TagListItemAdapter aaTagList;

	//Don't kill this instance
	private Bikes bikes = new Bikes();

	//Don't kill this instance
	private TagTypes tagTypes = new TagTypes();
	private final BikeListItemAdapter aaBikeList;
	private final TagTypeListItemAdapter aaTagTypeList;

	public DrawerController(Context context, ListView drawerBikeList, ListView drawerTagTypeList, ListView drawerTagList
	) {
		this.context = context;

		this.drawerBikeList = drawerBikeList;
		this.drawerTagTypeList = drawerTagTypeList;
		this.drawerTagList = drawerTagList;

		//Drawer: List of Bikes
		aaBikeList = new BikeListItemAdapter(context, R.layout.drawer_tag_types_item, bikes.getList());
		drawerBikeList.setAdapter(aaBikeList);
		drawerBikeList.setOnItemClickListener(new DrawerBikeItemClickListener());

		//Drawer: List of TagTypes
		aaTagTypeList = new TagTypeListItemAdapter(context, R.layout.drawer_tag_types_item, tagTypes.getList());
		drawerTagTypeList.setAdapter(aaTagTypeList);
		drawerTagTypeList.setOnItemClickListener(new DrawerTagTypeItemClickListener());

		//Drawer: List of Tags
		aaTagList = new TagListItemAdapter(context, R.layout.drawer_tags_item, tagTypes.getActualTagItems());
		drawerTagList.setAdapter(aaTagList);
		drawerTagList.setOnItemClickListener(new DrawerTagItemClickListener());

		reloadDate();
	}

	/** Call this after data have been changed */
	public void reloadDate() {
		loadDataFromProvider();
		loadPrefs();
		aaTagList.notifyDataSetChanged();
		aaBikeList.notifyDataSetChanged();
		aaTagTypeList.notifyDataSetChanged();
	}

	public void onStop() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = context.getSharedPreferences(Constants.Pref.NAME, 0);
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

	/** Returns null, if no TagType is selected. */
	public TagType getSelectedTagType() {
		return tagTypes.getSelectedTagType();
	}

	/**
	 Returns all selected Tags for the actual TagType.

	 @return List with selected Tags. Can be empty.
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

	/** Returns null, if no Bike selected. */
	public Bike getSelectedBike() {
		return bikes.getSelectedBike();
	}

	private void loadPrefs() {

		SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.Pref.NAME, 0);
		//Load last status from Prefs and validate this: Bike
		//Validate actual value or, if there is no actual value, take first entry as default

		String bikeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_BIKE_ID, "");
		if (!bikeIdString.isEmpty()) {
			try {
				//Select this one, if ID is valid
				if (!bikes.setSelectedItem(UUID.fromString(bikeIdString))) {
					//Unvalid ID, select first entry
					bikes.setSelectedItem(0);
				}
			} catch (IllegalArgumentException e) {
				Log.v(TAG, "Invalid Bike id from preferences " + bikeIdString);
			}
		}

		//Same for TagType
		String tagTypeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_TAG_TYPE_ID, "");
		if (!tagTypeIdString.isEmpty()) {
			try {
				//Select this one, if ID is valid
				if (!tagTypes.setSelectedItem(UUID.fromString(tagTypeIdString))) {
					//Unvalid ID, select first entry
					tagTypes.setSelectedItem(0);
				}
			} catch (IllegalArgumentException e) {
				Log.v(TAG, "Invalid TagType id from preferences " + tagTypeIdString);
			}
		}

		Set<String> tagStringSet = sharedPrefs.getStringSet(Constants.Pref.Keys.ACTUAL_TAG_IDS, null);
		if (tagStringSet != null) {
			//String has two parts, separated by space: ID of TagType + ' ' + Tag
			for (String line : tagStringSet) {
				String[] parts = line.split(" ");
				if (parts.length == 2) {
					try {
						tagTypes.addSelelectedTag(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
					} catch (Exception e) {
						Log.v(TAG, "IDs for TagType / Tag could not be solved.");
					}
				}
			}
		}

	}

//	/** Build list with all Tags for the actual TagType */
//	private void fillTags() {
//		tagTypes.fillTagItemsForSelectedTagType(tagItems);
//		aaTagList.notifyDataSetChanged();
//	}

	/** Select data from persistance. */
	private void loadDataFromProvider() {

		ContentResolver cr = context.getContentResolver();


		//--- TagTypes: Return all the saved data ---//
		tagTypes.clear();
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				TagType tagType = Utils.buildTagTypeFromCursor(c);
				tagTypes.add(tagType);
			} while (c.moveToNext());
		}


		//--- Tags: Return all the saved data ---//
		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				tagTypes.addTag(Utils.buildTagFromCursor(c));
			} while (c.moveToNext());
		}

		//--- Bikes: Return all the saved data ---//
		bikes.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				bikes.add(Utils.buildBikeFromCursor(c));
			} while (c.moveToNext());
		}

	}

	private static final class Constants {
		static final class Pref {
			static final String NAME = "default";

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
		}

	}

	private class DrawerBikeItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			bikes.setSelectedItem(position);
			drawerBikeList.setItemChecked(position, true);
		}

	}

	private class DrawerTagTypeItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			tagTypes.setSelectedItem(position);
			drawerTagTypeList.setItemChecked(position, true);
			//Corresponding TagList was updated by setSelectedItem()
			aaTagList.notifyDataSetChanged();
		}

	}

	/** Only instantiate TagTypes once, because ArrayAdapter handles the reference to the List. */
	private class TagTypes {

		private List<TagTypeItem> tagTypeItems = new ArrayList<>();
		private List<TagItem> actualTagItems = new ArrayList<>();

		/**
		 Returns reference to List of the selected TagItem for the actual TagType.
		 Don't edit the list.

		 @return List can be empty
		 */
		public List<TagItem> getActualTagItems() {
			return actualTagItems;
		}

		/** Returns reference of List. Only use this for the ArrayAdapter! */
		public List<TagTypeItem> getList() {
			return tagTypeItems;
		}

		public void clear() {
			tagTypeItems.clear();
		}

		/**
		 Set actual TagType (there can only be one selected) and loads Tags of this TagType.

		 @param pos
		 Non negative integer value.
		 @return true, if checked was set successfully.
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

		public boolean addTag(Tag tag) {
			for (TagTypeItem item : tagTypeItems) {
				if (item.getTagType().getId().equals(tag.getTagTypeId())) {
					item.addTag(tag);
					return true;
				}
			}
			return false;
		}

		/**
		 Sets the actual TagType and update the corresponding TagList

		 @param id
		 UUID of the TagType
		 @return true if TagType could be selected
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

		/** Doesn't care for duplicate entries. */
		public void add(TagType tagType) {
			tagTypeItems.add(new TagTypeItem(tagType));
		}

		/** Returns null, if no TagType is selected. */
		public TagType getSelectedTagType() {
			for (TagTypeItem item : tagTypeItems) {
				if (item.isChecked()) {
					return item.getTagType();
				}
			}
			return null;
		}

		/**
		 Only updates first found TagType/Tag combinition.

		 @param tagTypeId
		 UUID of the TagType
		 @param tagId
		 UUID of the Tag
		 @return true, if Tag could be found and checked.
		 */
		public boolean addSelelectedTag(UUID tagTypeId, UUID tagId) {
			if (tagTypeId == null || tagId == null) {
				return false;
			}

			for (TagTypeItem item : tagTypeItems) {
				if (item.getTagType().getId().equals(tagTypeId)) {
					for (TagItem tagItem : item.getTags()) {
						if (tagItem.getTag().getId().equals(tagId)) {
							tagItem.setChecked(true);
							return true;
						}
					}
				}
			}
			return false;
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
//			for (TagItem item : tagItems) {
//				if (item.getTag().getId().equals(id)) {
//					valid = true;
//				}
//			}
//			if (valid) {
//				for (TagItem item : tagItems) {
//					item.setChecked(item.getTag().getId().equals(id));
//				}
//				return true;
//			} else {
//				//Unknown ID
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
//			for (TagItem item : tagItems) {
//				if (item.isChecked()) {
//					return item.getTag();
//				}
//			}
//			return null;
//		}
//
//	}

	/** Only instantiate Bikes once, because ArrayAdapter hadles the reference to the List. */
	private class Bikes {

		private List<BikeItem> bikeItems = new ArrayList<>();

		/** Returns reference of List. Only use this for the ArrayAdapter! */
		public List<BikeItem> getList() {
			return bikeItems;
		}

		public void clear() {
			bikeItems.clear();
		}

		/**
		 Returns true, if checked was set successfully.

		 @param pos
		 Non negative integer value.
		 */
		public boolean setSelectedItem(int pos) {
			if (pos < 0 || pos >= bikeItems.size()) {
				return false;
			}

			for (int i = 0; i < bikeItems.size(); i++) {
				bikeItems.get(i).setChecked(i == pos);
			}
			return true;

		}

		/** Returns true, if checked was set successfully. */
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
				//Unknown ID
				return false;
			}

		}

		/** Doesn't care for duplicate entries. */
		public void add(Bike bike) {
			bikeItems.add(new BikeItem(bike));
		}

		/** Returns null, if no Bike is selected. */
		public Bike getSelectedBike() {
			for (BikeItem item : bikeItems) {
				if (item.isChecked()) {
					return item.getBike();
				}
			}
			return null;
		}

	}

	/** An entry in the shown list of bikes */
	class BikeItem {
		private Bike bike;
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

	/** An entry in the shown list of TagTypes */
	public class TagTypeItem {
		private List<TagItem> tags = new ArrayList<>();
		private boolean checked;
		private TagType tagType;

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
		private Tag tag;
		private boolean checked;

		private TagItem(Tag tag) {
			this.tag = tag;
		}

		public boolean isChecked() {
			return checked;
		}

		public Tag getTag() {
			return tag;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}
	}
}
