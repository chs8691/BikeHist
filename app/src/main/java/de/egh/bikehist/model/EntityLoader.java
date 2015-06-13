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
 Get Entities fresh from database. Use this only for particular entity access.
 Has business methods for entities.
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
	 Returns true, if a new event can be created, otherwise false. Use this for action button 'Create
	 Event'.
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

	/** Returns List with all Tags for the TagType. List can be empty. */
	public List<Tag> tags(TagType tagType) {
		List<Tag> tags = new ArrayList<>();


		//TODO: Alle Queries um Delete-Flag=TRUE erweitern
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
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
			c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, //
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

	Bike bike(UUID id) {
		return bike(id.toString());
	}

	public Bike bike(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null,
					BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME+ "=?",
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

	public TagType tagType(UUID id) {
		return tagType(id.toString());
	}

	public TagType tagType(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null,
					BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME+ "=?",
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

	public Tag tag(UUID id) {
		return tag(id.toString());
	}

	Tag tag(String id) {
		Cursor c = null;
		try {
			c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
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
