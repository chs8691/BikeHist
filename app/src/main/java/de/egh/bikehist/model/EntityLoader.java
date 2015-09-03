package de.egh.bikehist.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;

/**
 * Get Entities fresh from database. Use this only for particular entity access and background access.
 * Has business methods for entities.
 */
public class EntityLoader {
	private final static String TAG = EntityLoader.class.getSimpleName();
	private final ContentResolver cr;
	private final Context context;


	public EntityLoader(Context context) {
		this.context = context;
		this.cr = context.getContentResolver();
	}

	/**
	 * Return List with all Bike dataset inclusive deleted ones (deleted==true)
	 */
	public List<Bike> getAllBikes() {
		List<Bike> bikes = new ArrayList<>();
		EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(context);

		//--- Bikes: Return all the saved data ---//
		Cursor c;
		c = context.getContentResolver().query(BikeHistProvider.BikeHistContract.Tables.Bike.URI,
				null,
				null,
				null,
//				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME_STRING + "=?",
//				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				bikes.add(bikeUtils.build(c));
			} while (c.moveToNext());
		}

		return bikes;
	}

	/**
	 * Return List with all TagType dataset inclusive deleted ones (deleted==true)
	 */
	public List<TagType> getAllTagTypes() {
		List<TagType> tagTypes = new ArrayList<>();
		EntityUtils<TagType> tagTypeUtils = EntityUtilsFactory.createTagTypeUtils(context);

		//--- TagTypes: Return all the saved data ---//
		Cursor c;
		c = context.getContentResolver().query(BikeHistProvider.BikeHistContract.Tables.TagType.URI,
				null,
				null,
				null,
//				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME_STRING + "=?",
//				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				tagTypes.add(tagTypeUtils.build(c));
			} while (c.moveToNext());
		}

		return tagTypes;
	}

	/**
	 * Return List with all Tag dataset inclusive deleted ones (deleted==true)
	 */
	public List<Tag> getAllTags() {
		List<Tag> tags = new ArrayList<>();
		EntityUtils<Tag> tagUtils = EntityUtilsFactory.createTagUtils(context);

		//--- Tags: Return all the saved data ---//
		Cursor c;
		c = context.getContentResolver().query(BikeHistProvider.BikeHistContract.Tables.Tag.URI,
				null,
				null,
				null,
//				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME_STRING + "=?",
//				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				tags.add(tagUtils.build(c));
			} while (c.moveToNext());
		}

		return tags;
	}

	/**
	 * Return List with all Event dataset inclusive deleted ones (deleted==true)
	 */
	public List<Event> getAllEvents() {
		List<Event> events = new ArrayList<>();
		EntityUtils<Event> eventUtils = EntityUtilsFactory.createEventUtils(context);

		//--- Events: Return all the saved data ---//
		Cursor c;
		c = context.getContentResolver().query(BikeHistProvider.BikeHistContract.Tables.Event.URI,
				null,
				null,
				null,
//				BikeHistProvider.BikeHistContract.Tables.BikeHistEntity.Deleted.NAME_STRING + "=?",
//				new String[]{BikeHistProvider.BikeHistContract.Boolean.False.asString},
				null);

		if (c.moveToFirst()) {
			do {
				events.add(eventUtils.build(c));
			} while (c.moveToNext());
		}

		return events;
	}
	
	
	/**
	 * Returns true, if a new event can be created, otherwise false. Use this for action button 'Create
	 * Event'.
	 */
	public boolean checkEventCreate(Bike bike, TagType tagType) {

		//Check parameters
		if (bike == null || tagType == null) {
			return false;
		}

		//Bike and TagType must exists
		if (bike(bike.getId()) == null || tagType(tagType.getId()) == null) {
			return false;
		}

		//There must be a least on Tag
		return tags(tagType).size() >= 1;

	}

	/**
	 * Returns List with all Tags for the TagType. List can be empty.
	 */
	public List<Tag> tags(TagType tagType) {
		List<Tag> tags = new ArrayList<>();


		//TODO: Alle Queries um Delete-Flag=TRUE erweitern
		Cursor c = cr.query(BikeHistProvider.BikeHistContract.Tables.Tag.URI, null,
				BikeHistProvider.BikeHistContract.Tables.Tag.TagTypeId.NAME + "=?",
				new String[]{tagType.getId().toString()}, null);

		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				tags.add(EntityUtilsFactory.createTagUtils(context).build(c));
			} while (c.moveToNext());
		}

		c.close();

		return tags;
	}

	public Event event(UUID id) {
		return event(id.toString());
	}

	public Event event(String id) {
		Cursor c = null;
		try {

			/* uri            The URI, using the content:// scheme, for the content to retrieve.
			   projection     A list of which columns to return. Passing null will return all columns, which is inefficient.
			   selection      A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
			   selectionArgs  You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
			   sortOrder      How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.*/
			c = cr.query(BikeHistProvider.BikeHistContract.Tables.Event.URI, //
					null, //
					BikeHistProvider.BikeHistContract.Tables.Event.Id.NAME + "=?", //
					new String[]{id}, //
					null);

			//Edit existing Event
			if (c.getCount() == 1) {
				c.moveToFirst();
				return EntityUtilsFactory.createEventUtils(context).build(c);
			} else {
				Log.e(TAG, "Event not found:" + id + " Number of Events for this ID=" + c.getCount());
				return null;
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;

	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	private Bike bike(UUID id) {
		return bike(id.toString());
	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	public Bike bike(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.BikeHistContract.Tables.Bike.URI, null,
					BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createBikeUtils(context).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	public TagType tagType(UUID id) {
		return tagType(id.toString());
	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	public TagType tagType(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.BikeHistContract.Tables.TagType.URI, null,
					BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createTagTypeUtils(context).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}

	/**
	 * Checks, if TagType has no deleted Tags.
	 */
	public boolean hasTags(String tagTypeId) {
		TagType tagType = tagType(tagTypeId);
		if (tagType != null)
			return hasTags(tagType);
		else
			return true;

	}

	/**
	 * Checks, if TagType has no deleted Tags.
	 */
	public boolean hasTags(TagType tagType) {
		Cursor c = cr.query(BikeHistProvider.BikeHistContract.Tables.Tag.URI

				, BikeHistProvider.BikeHistContract.QUERY_COUNT_PROJECTION

				, BikeHistProvider.BikeHistContract.Tables.Tag.TagTypeId.NAME + "=? AND "
				+ BikeHistProvider.BikeHistContract.Tables.Tag.Deleted.NAME + "=?"

				, new String[]{tagType.getId().toString(),
				BikeHistProvider.BikeHistContract.Boolean.False.asString}

				, null);
		if (c == null) {
			return false;
		} else {
			c.moveToFirst();
			int i = c.getInt(0);
			c.close();
			return i > 0;
		}

	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	public Tag tag(UUID id) {
		return tag(id.toString());
	}

	/**
	 * Returns Entity by ID (even, it is deleted)
	 */
	private Tag tag(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.BikeHistContract.Tables.Tag.URI, null,
					BikeHistProvider.BikeHistContract.Tables.Tag.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createTagUtils(context).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}
}
