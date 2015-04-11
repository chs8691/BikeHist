package de.egh.bikehist.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

/**
 Helper class.
 */
public class Utils {

	private static final String TAG = Utils.class.getSimpleName();


	/** TagType builder for SQL inserts. */
	public static ContentValues buildTagTypeContentValues(TagType tagType) {
		ContentValues values = new ContentValues();

		values.put(Tables.TagType.Columns.Name.ID, tagType.getId().toString());
		values.put(Tables.TagType.Columns.Name.NAME, tagType.getName());

		return values;
	}

	/** Tag builder for SQL inserts. */
	public static ContentValues buildTagContentValues(Tag tag) {
		ContentValues values = new ContentValues();

		values.put(Tables.Tag.Columns.Name.ID, tag.getId().toString());
		values.put(Tables.Tag.Columns.Name.NAME, tag.getName());
		values.put(Tables.Tag.Columns.Name.TAG_TYPE_ID, tag.getTagTypeId().toString());

		return values;
	}

	/** Bike builder for SQL inserts. */
	public static ContentValues buildBikeContentValues(Bike bike) {
		ContentValues values = new ContentValues();

		values.put(Tables.Bike.Columns.Name.ID, bike.getId().toString());
		values.put(Tables.Bike.Columns.Name.NAME, bike.getName());
		values.put(Tables.Bike.Columns.Name.FRAME_NUMBER, bike.getFrameNumber());

		return values;
	}

	/** Event builder for SQL inserts. */
	public static ContentValues buildEventContentValues(Event event) {
		ContentValues values = new ContentValues();

		values.put(Tables.Event.Columns.Name.ID, event.getId().toString());
		values.put(Tables.Event.Columns.Name.NAME, event.getName());
		values.put(Tables.Event.Columns.Name.DISTANCE, event.getDistance());
		values.put(Tables.Event.Columns.Name.BIKE_ID, event.getBikeId().toString());
		values.put(Tables.Event.Columns.Name.TAG_ID, event.getTagId().toString());
		double longitude = event.getGeoLocation().getLongitude();
		double latitude = event.getGeoLocation().getLatitude();
		double altitude = event.getGeoLocation().getAltitude();
		values.put(Tables.Event.Columns.Name.GEO_LONGITUDE, longitude);
		values.put(Tables.Event.Columns.Name.GEO_LATITUDE, latitude);
		values.put(Tables.Event.Columns.Name.GEO_ALTITUDE, altitude);
		values.put(Tables.Event.Columns.Name.TIMESTAMP, event.getTimestamp());

		return values;
	}



	/**
	 Create new event for the actual data set in the cursor. Cursor must point to events.

	 @return Event is null, if dataset is invalid
	 */
	public static Event buildEventFromCursor(Cursor c) {
		UUID id = getUUIDFromString(c.getString(Tables.Event.Columns.Number.ID));

		if (id == null) {
			return null;
		}

		return new Event(
				id,
				c.getString(Tables.Event.Columns.Number.NAME),
				c.getLong(Tables.Event.Columns.Number.DISTANCE),
				getUUIDFromString(c.getString(Tables.Event.Columns.Number.BIKE_ID)),
				getUUIDFromString(c.getString(Tables.Event.Columns.Number.TAG_ID)),
				new GeoLocation(c.getDouble(Tables.Event.Columns.Number.GEO_LONGITUDE),
						c.getDouble(Tables.Event.Columns.Number.GEO_LATITUDE),
						c.getDouble(Tables.Event.Columns.Number.GEO_ALTITUDE)
				),
				c.getLong(Tables.Event.Columns.Number.TIMESTAMP),
				c.getLong(Tables.Event.Columns.Number.DIFF_DISTANCE),
				c.getLong(Tables.Event.Columns.Number.DIFF_TIMESTAMP)
		);

	}

	/**
	 Create new TagType for the actual data set in the cursor. Cursor must point to events.

	 @param c
	 @return TagType is null, if dataset is invalid
	 */
	public static TagType buildTagTypeFromCursor(Cursor c) {
		UUID id = getUUIDFromString(c.getString(Tables.TagType.Columns.Number.ID));
		String name = c.getString(Tables.TagType.Columns.Number.NAME);

		if (id == null) {
			return null;
		}

		return new TagType(id, name);

	}

	/**
	 Create new Tag for the actual data set in the cursor. Cursor must point to events.

	 @param c
	 @return Tag is null, if dataset is invalid
	 */
	public static Tag buildTagFromCursor(Cursor c) {
		UUID id = getUUIDFromString(c.getString(Tables.Tag.Columns.Number.ID));
		String name = c.getString(Tables.Tag.Columns.Number.NAME);
		UUID tagTypeId = getUUIDFromString(c.getString(Tables.Tag.Columns.Number.TAG_TYPE_ID));

		if (id == null) {
			return null;
		}

		return new Tag(id, name, tagTypeId);

	}

	/**
	 Create new event for the actual data set in the cursor. Cursor must point to events.

	 @return Bike is null, if dataset is invalid
	 */
	public static Bike buildBikeFromCursor(Cursor c) {
		UUID id = getUUIDFromString(c.getString(Tables.Bike.Columns.Number.ID));
		String name = c.getString(Tables.Bike.Columns.Number.NAME);
		String frameNumber = c.getString(Tables.Bike.Columns.Number.FRAME_NUMBER);

		if (id == null) {
			return null;
		}

		return new Bike(id, name, frameNumber);

	}

	/** Returns null, if id is no valid UUID. */
	private static UUID getUUIDFromString(String id) {

		try {
			return UUID.fromString(id);
		} catch (NullPointerException | IllegalArgumentException e) {
			if (id != null) {
				Log.w(TAG, "No valid UUID:");
			} else {
				Log.w(TAG, "id is null");
			}
			return null;
		}

	}

	/**
	 Helper for Dummy data

	 @param id
	 UUID of the bike, can be null.
	 */
	public static Bike getBikeById(UUID id, List<Bike> list) {
		for (Bike entry : list) {
			if (entry.getId().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	/** Helper for Dummy data */
	public static Bike getBikeByName(String name, List<Bike> list) {
		for (Bike entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	/** Helper for Dummy data */
	public static Bike getBikeByFrameNumber(String frameNumber, List<Bike> list) {
		for (Bike entry : list) {
			if (entry.getFrameNumber().equals(frameNumber)) {
				return entry;
			}
		}
		return null;
	}

	/** Helper for Dummy data */
	public static Tag getTagByName(String name, List<Tag> list) {
		for (Tag entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;

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

	/** Helper for Dummy data */
	public static TagType getTagTypeByName(String name, List<TagType> list) {
		for (TagType entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;

	}
}
