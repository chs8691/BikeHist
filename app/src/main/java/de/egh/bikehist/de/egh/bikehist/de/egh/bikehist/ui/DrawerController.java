package de.egh.bikehist.de.egh.bikehist.de.egh.bikehist.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.de.egh.bikehist.model.Bike;
import de.egh.bikehist.de.egh.bikehist.model.Tag;
import de.egh.bikehist.de.egh.bikehist.model.TagType;
import de.egh.bikehist.de.egh.bikehist.model.Utils;
import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider;

/** Contains all data for the TagType and corresponding Tags to show in the drawer. */
public class DrawerController {
	private static final String TAG = DrawerController.class.getSimpleName();
	//	List<BikeItem> bikeItems = new ArrayList<>();
//	List<TagTypeItem> tagTypes = new ArrayList<>();
	private Context context;
	private ListView drawerBikeList;
	private ListView drawerTagTypeList;
	private ListView drawerTagList;
	private BikeListItemAdapter aaBikeList;
	private TagTypeListItemAdapter aaTagTypeList;
	private TagListItemAdapter aaTagList;
	private List<TagType> tagTypes = new ArrayList<>();
	private Map<UUID, Tag> allTagMap = new HashMap<>();
	private List<Tag> tags = new ArrayList<>();
	/** Can be null */
	private UUID actualTagTypeId;

  //Don't kill this instance
	private Bikes bikes = new Bikes();

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
		aaTagTypeList = new TagTypeListItemAdapter(context, R.layout.drawer_tag_types_item, tagTypes);
		drawerTagTypeList.setAdapter(aaTagTypeList);
		drawerTagTypeList.setOnItemClickListener(new DrawerTagTypeItemClickListener());

		//Drawer: List of Tags
		aaTagList = new TagListItemAdapter(context, R.layout.drawer_tags_item, tags);
		drawerTagList.setAdapter(aaTagList);
		drawerTagList.setOnItemClickListener(new DrawerTagItemClickListener());

		loadDataFromProvider();
		loadPrefs();
		fillTags();


	}

	/** Helper for Dummy data */
	public static TagType getTagTypeById(UUID id, List<TagType> list) {
		for (TagType entry : list) {
			if (entry.getId().equals(id)) {
				return entry;
			}
		}
		return null;

	}

	public void onStop() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = context.getSharedPreferences(Constants.Pref.NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Constants.Pref.Keys.ACTUAL_BIKE_ID, bikes.getSelectedBike()==null? "" : bikes.getSelectedBike().getId().toString());
		editor.putString(actualTagTypeId == null ? "" : Constants.Pref.Keys.ACTUAL_TAG_TYPE_ID, actualTagTypeId.toString());

		// Commit the edits!
		editor.apply();
	}

	/** Returns null, if no TagType is selected. */
	public TagType getActualTagType() {
		for (TagType tagType : tagTypes) {
			if (tagType.getId().equals(actualTagTypeId)) {
				return tagType;
			}
		}
		return null;
	}

	/** Returns null, if no Bike selected. */
	public Bike getActualBike() {
		return bikes.getSelectedBike();
	}

	private void loadPrefs() {

		UUID bikeId;
		SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.Pref.NAME, 0);
		//Load last status from Prefs and validate this: Bike
		//Validate actual value or, if there is no actual value, take first entry as default

		String bikeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_BIKE_ID, "");
		if (!bikeIdString.isEmpty()) {
		}
		try {

			//Select this one, if ID is valid
			if (!bikes.setSelectedItem(UUID.fromString(bikeIdString))) {
				//Unvalid ID, select first entry
				bikes.setSelectedItem(0);
			}

		} catch (IllegalArgumentException e) {
			Log.v(TAG, "Invalid Bike id from preferences " + bikeIdString);
		}

		//Same for TagType
		if (tagTypes.size() == 0) {
			// No valid bike
			actualTagTypeId = null;
			aaTagTypeList.setSelectedItem(-1);
		} else {
			String tagTypeIdString = sharedPrefs.getString(Constants.Pref.Keys.ACTUAL_TAG_TYPE_ID, "");
			if (tagTypeIdString.isEmpty()) {
				actualTagTypeId = null;
			} else {
				try {
					actualTagTypeId = UUID.fromString(tagTypeIdString);
				} catch (IllegalArgumentException e) {
					actualTagTypeId = null;
				}
			}

			if (actualTagTypeId == null || Utils.getTagTypeById(actualTagTypeId, tagTypes) == null) {
				//Nothing/invalid stored, take random bike
				actualTagTypeId = tagTypes.get(0).getId();
			}
			//Mark entry for DrawerList
			aaTagTypeList.setSelectedItem(tagTypes.indexOf(Utils.getTagTypeById(actualTagTypeId, tagTypes)));
		}
	}

	/** Build list with all Tags for the actual TagType */
	private void fillTags() {
		tags.clear();

		for (UUID id : allTagMap.keySet()) {
			if (allTagMap.get(id).getTagTypeId().equals(actualTagTypeId)) {
				tags.add(allTagMap.get(id));
			}
		}
		aaTagList.notifyDataSetChanged();
	}

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
		allTagMap.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				Tag tag = Utils.buildTagFromCursor(c);
				allTagMap.put(tag.getId(), tag);
			} while (c.moveToNext());
		}

		//--- Bikes: Return all the saved data ---//
		bikes.clear();
		c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				bikes.addBike(Utils.buildBikeFromCursor(c));
			} while (c.moveToNext());
		}

	}

	private static final class Constants {
		static final class Pref {
			static final String NAME = "default";

			static final class Keys {
				static final String ACTUAL_BIKE_ID = "ACTUAL_BIKE_ID";
				static final String ACTUAL_TAG_TYPE_ID = "ACTUAL_TAG_TYPE_ID";
			}
		}
	}

	private class DrawerTagItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			boolean check = !aaTagList.isItemSelected(position);
			drawerTagList.setItemChecked(position, check);
			aaTagList.setSelectedItem(position, check);
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
			actualTagTypeId = aaTagTypeList.getItem(position).getId();
			drawerTagTypeList.setItemChecked(position, true);
			aaTagTypeList.setSelectedItem(position);
			fillTags();
		}

	}

	/** Only instantiate Bikes once, because ArrayAdapter hadles the reference to the List. */
	private class Bikes {

		private List<BikeItem> bikeItems = new ArrayList<>();

		/** Returns reference of List. Only use this for the ArrayAdapter! */
		public List<BikeItem> getList() {
			return bikeItems;
		}

		public void clear(){
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

		public void addBike(Bike bike) {
			if (!bikeItems.contains(bike)) {
				bikeItems.add(new BikeItem(bike));
			}
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

	private class TagTypeItem {
		private List<TagItem> tags = new ArrayList<>();
		private boolean checked;
	}

	private class TagItem {
		private Tag tag;
		private boolean checked;
	}
}
