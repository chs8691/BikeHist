package de.egh.bikehist.de.egh.bikehist.persistance;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.UUID;

/**
 Created by ChristianSchulzendor on 01.02.2015.
 */
public class BikeHistProvider extends ContentProvider {
	public static final Uri CONTENT_URI_EVENTS = Uri.parse("content://" + BikeHistContract.URI_PATH + "/" //
			+ BikeHistContract.Tables.Event.NAME);
	private static final String TAG = BikeHistProvider.class.getSimpleName();
	// Creates a UriMatcher object.
	private static final UriMatcher uriMatcher;

	// Allocate the UriMatcher object, where a URI ending in 'earthquakes' will
	// correspond to a request for all earthquakes, and 'earthquakes' with a
	// trailing '/[rowID]' will represent a single earthquake row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Event.NAME, Constants.Uri.EVENTS);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Event.NAME + "/#", Constants.Uri.EVENT_ID);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Bike.NAME, Constants.Uri.BIKES);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Bike.NAME + "/#", Constants.Uri.BIKE_ID);
	}

	/** Only for developing. */
	private MatrixCursor eventCursor;
	/** Only for developing. */
	private MatrixCursor bikeCursor;
	//The underlying database
	private SQLiteDatabase bikeHistDB;

	@Override
	public boolean onCreate() {

		BikeHistDatabaseHelper dbHelper = new BikeHistDatabaseHelper(getContext(),
				Constants.Database.NAME,
				null,
				Constants.Database.VERSION);
		bikeHistDB = dbHelper.getWritableDatabase();

		if (dbHelper.isDropped()) {
			createDummyData();
		}

		return (bikeHistDB == null) ? false : true;
	}

	/** Only for developing. Initialize the cursor fields once. */
	private void createDummyData() {

		String[] bikeColumnNames = {BikeHistContract.Tables.Bike.Columns.Name.ID,
				BikeHistContract.Tables.Bike.Columns.Name.NAME,
				BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER,
		};
		bikeCursor = new MatrixCursor(bikeColumnNames);

		bikeCursor.newRow().add(UUID.randomUUID().toString()).add("Brompton").add("BROMPTON-448010");
		bikeCursor.newRow().add(UUID.randomUUID().toString()).add("DEV 0").add("DEV-0");

		String[] eventColumnNames = {BikeHistContract.Tables.Event.Columns.Name.BIKE_ID,
				BikeHistContract.Tables.Event.Columns.Name.NAME,
				BikeHistContract.Tables.Event.Columns.Name.DISTANCE,
				BikeHistContract.Tables.Event.Columns.Name.BIKE_ID,
				BikeHistContract.Tables.Event.Columns.Name.TAG_ID,
				BikeHistContract.Tables.Event.Columns.Name.GEO_LONGITUDE,
				BikeHistContract.Tables.Event.Columns.Name.GEO_LATITUDE,
				BikeHistContract.Tables.Event.Columns.Name.GEO_ALTITUDE,
				BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP};
		eventCursor = new MatrixCursor(eventColumnNames);

		eventCursor.newRow().add(UUID.randomUUID().toString()).add("SRAM PC 1").add(Integer.valueOf(1000))//
				.add("").add("") //
				.add(Double.valueOf(0)).add(Double.valueOf(0)).add(Double.valueOf(0))//
				.add(Long.valueOf(System.currentTimeMillis()));

		eventCursor.newRow().add(UUID.randomUUID().toString()).add("SRAM PC 1").add(Integer.valueOf(111000))//
				.add("").add("") //
				.add(Double.valueOf(0)).add(Double.valueOf(0)).add(Double.valueOf(0))//
				.add(Long.valueOf(System.currentTimeMillis()));


	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		if(uri.toString().contains(BikeHistContract.URI_PATH + "/"+BikeHistContract.Tables.Event.NAME))
		qb.setTables(BikeHistContract.Tables.Event.NAME);
		else
		  return null;

		// If this is a row query, limit the result set to the passed in row.
		switch (uriMatcher.match(uri)) {
			case Constants.Uri.EVENT_ID:
//				String[] args = { uri.getPathSegments().get(1)};
//				qb.appendWhere(BikeHistContract.Tables.Event.Columns.Name.ID + "=" + uri.getPathSegments().get(1));
				qb.appendWhere(BikeHistContract.Tables.Event.Columns.Name.ID + "=?");
				selectionArgs[selectionArgs.length] = uri.getPathSegments().get(1);
				break;
			default:
				break;
		}

		// If no sort order is specified sort by date / time
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP;
		} else {
			orderBy = sortOrder;
		}

		// Apply the query to the underlying database.
		Cursor c = qb.query(bikeHistDB,
				projection,
				selection, selectionArgs,
				null, null,
				orderBy);

		// Register the contexts ContentResolver to be notified if
		// the cursor result set changes.
		c.setNotificationUri(getContext().getContentResolver(), uri);

		// Return a cursor to the query result.
		return c;

	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case Constants.Uri.EVENTS:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.event";
			case Constants.Uri.EVENT_ID:
				return "vnd.android.cursor.item/vnd.de.egh.provider.event";
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public Uri insert(Uri _uri, ContentValues _initialValues) {
		// Insert the new row, will return the row number if
		// successful.
		long rowID = bikeHistDB.insert(BikeHistContract.Tables.Event.NAME, null, _initialValues);

		// Return a URI to the newly inserted row on success.
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(CONTENT_URI_EVENTS, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		throw new SQLException("Failed to insert row into " + _uri);
	}

	private static final class Constants {
		static final class Database {
			static final String NAME = "bikeHist.db";
			static final int VERSION = 4;

		}

		/** Accessing data for all data and for single access */
		private static final class Uri {

			static final int EVENTS = 1;

			static final int EVENT_ID = 2;

			static final int BIKES = 3;

			static final int BIKE_ID = 4;


		}
	}

	public final class BikeHistContract {
		/** URI path for the ContentProvider. */
		static final String URI_PATH = "de.egh.provider.bikehist";

		/**For all tables: ID is the UUID, not database table id (_ID) */
		public final class Tables {

			/**
			 Bike has following fields:
			 <ul>
			 <li>String id - Unique ID (UUID as String)</li>
			 <li>String name - Description</li>
			 <li>String frameNumber - Identifier of the bike, e.g. 'Brompton 448010'</li> </ul>
			 */
			public final class Bike {
				public static final String NAME = "bikes";

				public final class Columns {
					public final class Number {
						public static final int ID = 1;
						public static final int NAME = 2;
						public static final int FRAME_NUMBER = 3;
					}

					public final class Name {
						public static final String ID = "id";
						public static final String NAME = "name";
						public static final String FRAME_NUMBER = "frameNumber";
					}
				}
			}

			/**
			 Event has following fields:
			 <ul>
			 <li>String id - Unique ID (UUID as String)</li>
			 <li>String name - Events description</li>
			 <li>long distance - Millimeter value</li>
			 <li>String BikeId - Bikes unique ID (UUID as String)</li>
			 <li>String TagId - Tags unique ID (UUID as String)</li>
			 <li>Double longitude - Geoposition</li>
			 <li>Double latitude - Geoposition</li>
			 <li>Double altitude - Geoposition</li>
			 <li>long timestamp - Create time in milliseconds</li>
			 </ul>
			 */
			public final class Event {


				public static final String NAME = "events";

				public final class Columns {
					public final class Number {
						public static final int ID = 1; // 0 is the database key field _ID
						public static final int NAME = 2;
						public static final int DISTANCE = 3;
						public static final int BIKE_ID = 4;
						public static final int TAG_ID = 5;
						public static final int GEO_LONGITUDE = 6;
						public static final int GEO_LATITUDE = 7;
						public static final int GEO_ALTITUDE = 8;
						public static final int TIMESTAMP = 9;
					}

					public final class Name {
						public static final String ID = "id";
						public static final String NAME = "name";
						public static final String DISTANCE = "distance";
						public static final String BIKE_ID = "bikeId";
						public static final String TAG_ID = "tagId";
						public static final String GEO_LONGITUDE = "GeoLongitude";
						public static final String GEO_LATITUDE = "GeoLatitude";
						public static final String GEO_ALTITUDE = "GeoAltitude";
						public static final String TIMESTAMP = "timestamp";
					}
				}


			}
		}
	}

}
