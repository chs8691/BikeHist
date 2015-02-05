package de.egh.bikehist.de.egh.bikehist.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.UUID;

import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables.Event.Columns;

/**
 Helper class.
 */
public class Utils {

	private static final String TAG = Utils.class.getSimpleName();

	/**Event builder for SQL inserts. */
	public static ContentValues buildContentValues(Event event){
		ContentValues values = new ContentValues();

		values.put(Columns.Name.ID, event.getId().toString());
		values.put(Columns.Name.NAME, event.getName());
		values.put(Columns.Name.DISTANCE, event.getDistance());
		values.put(Columns.Name.BIKE_ID, event.getBikeId().toString());
		values.put(Columns.Name.TAG_ID, event.getTagId().toString());
		double longitude = event.getGeoLocation().getLongitude();
		double latitude = event.getGeoLocation().getLatitude();
		double altitude = event.getGeoLocation().getAltitude();
		values.put(Columns.Name.GEO_LONGITUDE, longitude);
		values.put(Columns.Name.GEO_LATITUDE, latitude);
		values.put(Columns.Name.GEO_ALTITUDE, altitude);
		values.put(Columns.Name.TIMESTAMP, event.getTimestamp());

		return values;
	}
	/**
	 Create new event for the actual data set in the cursor. Cursor must point to events.

	 @return Event is null, if dataset is invalid
	 */
	public static Event buildEventFromCursor(Cursor c) {
		UUID id = getUUIDFromString(c.getString(Columns.Number.ID));
		String name = c.getString(Columns.Number.NAME);
		long distance = c.getLong(Columns.Number.DISTANCE);
		UUID bikeId = getUUIDFromString(c.getString(Columns.Number.BIKE_ID));
		UUID tagId = getUUIDFromString(c.getString(Columns.Number.TAG_ID));
		GeoLocation location = new GeoLocation(c.getDouble(Columns.Number.GEO_LONGITUDE),
				c.getDouble(Columns.Number.GEO_LATITUDE),
				c.getDouble(Columns.Number.GEO_ALTITUDE)
		);
		long timestamp = c.getLong(Columns.Number.TIMESTAMP);

		if (id == null) {
			return null;
		}

		return new Event(
				id, name, distance, bikeId, tagId, location, timestamp);

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

}
