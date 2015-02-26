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

/**
 Created by ChristianSchulzendor on 01.02.2015.
 */
public class BikeHistProvider extends ContentProvider {


	public static final Uri CONTENT_URI_EVENTS = Uri.parse("content://" + BikeHistContract.URI_PATH + "/" //
			+ BikeHistContract.Tables.Event.NAME);
	public static final Uri CONTENT_URI_BIKES = Uri.parse("content://" + BikeHistContract.URI_PATH + "/" //
			+ BikeHistContract.Tables.Bike.NAME);
	public static final Uri CONTENT_URI_TAGS = Uri.parse("content://" + BikeHistContract.URI_PATH + "/" //
			+ BikeHistContract.Tables.Tag.NAME);
	public static final Uri CONTENT_URI_TAG_TYPES = Uri.parse("content://" + BikeHistContract.URI_PATH + "/" //
			+ BikeHistContract.Tables.TagType.NAME);

	private static final String TAG = BikeHistProvider.class.getSimpleName();
	// Creates a UriMatcher object.
	private static final UriMatcher uriMatcher;

	// Allocate the UriMatcher object, where a URI ending in 'earthquakes' will
	// correspond to a request for all earthquakes, and 'earthquakes' with a
	// trailing '/[rowID]' will represent a single earthquake row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Event.NAME, Constants.Uri.Event.MULTI);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Event.NAME + "/#", Constants.Uri.Event.ID);

		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Bike.NAME, Constants.Uri.Bike.MULTI);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Bike.NAME + "/#", Constants.Uri.Bike.ID);

		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Tag.NAME, Constants.Uri.Tag.MULTI);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.Tag.NAME + "/#", Constants.Uri.Tag.ID);

		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.TagType.NAME, Constants.Uri.TagType.MULTI);
		uriMatcher.addURI(BikeHistContract.URI_PATH, BikeHistContract.Tables.TagType.NAME + "/#", Constants.Uri.TagType.ID);
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

//		if (dbHelper.isDropped()) {
//			createDummyData();
//		}

		return (bikeHistDB == null) ? false : true;
	}


	/**
	 @throws java.lang.IllegalArgumentException
	 Wrong URI
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		TableStrategy tableStrategy = TableStrategy.create(bikeHistDB, uri);
		if (tableStrategy == null) {
			throw new IllegalArgumentException("Couldn't query for this table: " + uri);
		}

		Cursor c = tableStrategy.query(projection, selection, selectionArgs, sortOrder);

		// Register the contexts ContentResolver to be notified if
		// the cursor result set changes.
		c.setNotificationUri(getContext().getContentResolver(), uri);

		// Return a cursor to the query result.
		return c;

	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case Constants.Uri.Event.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.event";
			case Constants.Uri.Event.ID:
				return "vnd.android.cursor.item/vnd.de.egh.provider.event";

			case Constants.Uri.Bike.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.bike";
			case Constants.Uri.Bike.ID:
				return "vnd.android.cursor.item/vnd.de.egh.provider.bike";

			case Constants.Uri.Tag.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.tag";
			case Constants.Uri.Tag.ID:
				return "vnd.android.cursor.item/vnd.de.egh.provider.tag";

			case Constants.Uri.TagType.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.tagType";
			case Constants.Uri.TagType.ID:
				return "vnd.android.cursor.item/vnd.de.egh.provider.tagType";

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

	/**
	 @throws java.lang.IllegalArgumentException
	 Wrong URI
	 @throws android.database.SQLException
	 Insert failed
	 */
	@Override
	public Uri insert(Uri _uri, ContentValues _initialValues) {

		//Delegate to table
		Uri uri = TableStrategy.create(bikeHistDB, _uri).insert(_initialValues);
		if (uri != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return uri;
	}


	/** Access to a specific table. */
	private abstract static class TableStrategy {

		Uri uri;
		String tableName;
		SQLiteDatabase db;

		/**
		 Internal use only. Use factory method instead.
		 */
		private TableStrategy(SQLiteDatabase db, Uri uri, String tableName) {
			this.uri = uri;
			this.tableName = tableName;
			this.db = db;
		}

		/**
		 Database access for a particular table.

		 @return TableStrategy
		 @throws IllegalArgumentException
		 Uri could not resolved for a table
		 */
		static TableStrategy create(SQLiteDatabase db, Uri uri) {
			if (uri.toString().contains(BikeHistContract.URI_PATH + "/" + BikeHistContract.Tables.Event.NAME)) {
				return new EventStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.URI_PATH + "/" + BikeHistContract.Tables.Bike.NAME)) {
				return new BikeStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.URI_PATH + "/" + BikeHistContract.Tables.TagType.NAME)) {
				return new TagTypeStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.URI_PATH + "/" + BikeHistContract.Tables.Tag.NAME)) {
				return new TagStrategy(db, uri);
			}

			throw new IllegalArgumentException("Unknown table addressed: " + uri.toString());

		}

		/**
		 @throws android.database.SQLException
		 Insert failed
		 */
		Uri insert(ContentValues _initialValues) {
			// Insert the new row, will return the row number if
			// successful.
			long rowID = db.insert(tableName, null, _initialValues);

			// Return a URI to the newly inserted row on success.
			if (rowID > 0) {
				Uri uri = ContentUris.withAppendedId(CONTENT_URI_EVENTS, rowID);
				return uri;
			}
			throw new SQLException("Insert failed into " + uri + " for " + _initialValues.toString());
		}

		abstract Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder);
	}

	private static class BikeStrategy extends TableStrategy {


		public BikeStrategy(SQLiteDatabase db, Uri uri) {
			super(db, uri, BikeHistContract.Tables.Bike.NAME);
		}


		@Override
		public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			String orderBy;

			qb.setTables(tableName);

			// If no sort order is specified sort by date / time
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = BikeHistContract.Tables.Bike.Columns.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Bike.ID:
					qb.appendWhere(BikeHistContract.Tables.Bike.Columns.Name.ID + "=?");
					selectionArgs[selectionArgs.length] = uri.getPathSegments().get(1);
					break;
				default:
					break;
			}


			// Apply the query to the underlying database.
			return qb.query(db,
					projection,
					selection, selectionArgs,
					null, null,
					orderBy);

		}
	}

	private static class TagStrategy extends TableStrategy {


		public TagStrategy(SQLiteDatabase db, Uri uri) {
			super(db, uri, BikeHistContract.Tables.Tag.NAME);
		}


		@Override
		public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			String orderBy;

			qb.setTables(tableName);

			// If no sort order is specified sort by date / time
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = BikeHistContract.Tables.Tag.Columns.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Tag.ID:
					qb.appendWhere(BikeHistContract.Tables.Tag.Columns.Name.ID + "=?");
					selectionArgs[selectionArgs.length] = uri.getPathSegments().get(1);
					break;
				default:
					break;
			}


			// Apply the query to the underlying database.
			return qb.query(db,
					projection,
					selection, selectionArgs,
					null, null,
					orderBy);

		}
	}

	private static class TagTypeStrategy extends TableStrategy {


		public TagTypeStrategy(SQLiteDatabase db, Uri uri) {
			super(db, uri, BikeHistContract.Tables.TagType.NAME);
		}


		@Override
		public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			String orderBy;

			qb.setTables(tableName);

			// If no sort order is specified sort by date / time
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = BikeHistContract.Tables.TagType.Columns.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Tag.ID:
					qb.appendWhere(BikeHistContract.Tables.TagType.Columns.Name.ID + "=?");
					selectionArgs[selectionArgs.length] = uri.getPathSegments().get(1);
					break;
				default:
					break;
			}


			// Apply the query to the underlying database.
			return qb.query(db,
					projection,
					selection, selectionArgs,
					null, null,
					orderBy);

		}
	}

	private static class EventStrategy extends TableStrategy {


		public EventStrategy(SQLiteDatabase db, Uri uri) {
			super(db, uri, BikeHistContract.Tables.Event.NAME);
		}


		@Override
		public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			String orderBy;

			//TODO check for wrong selections, e.g: selection transient fields

			qb.setTables(tableName);

			// If no sort order is specified sort by date / time
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP;
			} else {
				orderBy = sortOrder;
			}

			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Event.ID:
					qb.appendWhere(BikeHistContract.Tables.Event.Columns.Name.ID + "=?");
					selectionArgs[selectionArgs.length] = uri.getPathSegments().get(1);
					break;
				default:
					break;
			}

			// Apply the query to the underlying database.
			Cursor c = qb.query(db,
					projection,
					selection, selectionArgs,
					null, null,
					orderBy);

			// Get the corresponding Event and calculate transient fields
			/* SELECT * FROM
					(SELECT id, distance, timestamp
				     FROM events
				     WHERE bikeId = bikeId
				     AND   tagId  = tagId
				     AND   timestamp < timestamp
				     ORDER BY timestamp DESC)
				 LIMIT 1;
			*/
			c.moveToFirst();
			MatrixCursor mc = new MatrixCursor(new String[]{
					BikeHistDatabaseHelper.Constants._ID,
					BikeHistContract.Tables.Event.Columns.Name.ID,
					BikeHistContract.Tables.Event.Columns.Name.NAME,
					BikeHistContract.Tables.Event.Columns.Name.DISTANCE,
					BikeHistContract.Tables.Event.Columns.Name.BIKE_ID,
					BikeHistContract.Tables.Event.Columns.Name.TAG_ID,
					BikeHistContract.Tables.Event.Columns.Name.GEO_LONGITUDE,
					BikeHistContract.Tables.Event.Columns.Name.GEO_LATITUDE,
					BikeHistContract.Tables.Event.Columns.Name.GEO_ALTITUDE,
					BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP,
					BikeHistContract.Tables.Event.Columns.Name.DIFF_DISTANCE,
					BikeHistContract.Tables.Event.Columns.Name.DIFF_TIMESTAMP});

			// For all entries: Map to new cursor and add transient fields
			long distance;
			long timestamp;
			long diffDistance;
			long diffTimestamp;

			if (c.getCount() > 0) {
				do {
					distance = c.getLong(BikeHistContract.Tables.Event.Columns.Number.DISTANCE);
					timestamp = c.getLong(BikeHistContract.Tables.Event.Columns.Number.TIMESTAMP);

					// Get corresponding Event: previous Event with same Tag
					Cursor c2 = db.rawQuery(
							"SELECT * FROM ( SELECT " +
									BikeHistContract.Tables.Event.Columns.Name.DISTANCE + ", " +
									BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP +
									" FROM " + BikeHistContract.Tables.Event.NAME +
									" WHERE " + BikeHistContract.Tables.Event.Columns.Name.BIKE_ID + "=?" +
									" AND " + BikeHistContract.Tables.Event.Columns.Name.TAG_ID + "=?" +
									" AND " + BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP + "<?" +
									" ORDER BY " + BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP + " DESC)" +
									" LIMIT 1"
							,
							new String[]{c.getString(BikeHistContract.Tables.Event.Columns.Number.BIKE_ID),
									c.getString(BikeHistContract.Tables.Event.Columns.Number.TAG_ID),
									c.getString(BikeHistContract.Tables.Event.Columns.Number.TIMESTAMP)}
					);

					if (c2.getCount() == 1) {
						c2.moveToFirst();
						diffDistance = distance - c2.getLong(0);
						diffTimestamp = timestamp - c2.getLong(1);
					} else {
						diffDistance = 0;
						diffTimestamp = 0;
					}

					//Create new entries
					Object[] values = {
							c.getInt(0), //The key for the database entry
							c.getString(BikeHistContract.Tables.Event.Columns.Number.ID),
							c.getString(BikeHistContract.Tables.Event.Columns.Number.NAME),
							distance,
							c.getString(BikeHistContract.Tables.Event.Columns.Number.BIKE_ID),
							c.getString(BikeHistContract.Tables.Event.Columns.Number.TAG_ID),
							c.getString(BikeHistContract.Tables.Event.Columns.Number.GEO_LONGITUDE),
							c.getString(BikeHistContract.Tables.Event.Columns.Number.GEO_LATITUDE),
							c.getString(BikeHistContract.Tables.Event.Columns.Number.GEO_ALTITUDE),
							timestamp,
							diffDistance,
							diffTimestamp
					};
					mc.addRow(values);

				} while (c.moveToNext());
			}

			return mc;
		}
	}

	private static final class Constants {
		static final class Database {
			static final String NAME = "bikeHist.db";
			static final int VERSION = 11;

		}

		/** Accessing data for all data and for single access */
		private static final class Uri {

			static final class Event {
				static final int MULTI = 1;
				static final int ID = 2;
			}

			static final class Bike {
				static final int MULTI = 3;
				static final int ID = 4;
			}

			static final class Tag {
				static final int MULTI = 5;
				static final int ID = 6;
			}

			static final class TagType {
				static final int MULTI = 7;
				static final int ID = 8;
			}


		}
	}

	/**
	 Contract for the consumer of the ContentProvider. The constants defined, only shows the
	 outer view to the persistance layer. It does not define the table definition of the
	 underlaying database. So, this Contract may not be used for defining the database.
	 */
	public final class BikeHistContract {
		/** URI path for the ContentProvider. */
		static final String URI_PATH = "de.egh.provider.bikehist";

		/** For all tables: ID is the UUID, not database table id (_ID) */
		public final class Tables {

			/**
			 Tag has following fields:
			 <ul>
			 <li>String id - Unique ID (UUID as String)</li>
			 <li>String name - Description</li>
			 <li>String tagTypeId - Identifier (UUID) of the TagType </ul>
			 */
			public final class Tag {
				public static final String NAME = "tags";

				public final class Columns {
					public final class Number {
						public static final int ID = 1;
						public static final int NAME = 2;
						public static final int TAG_TYPE_ID = 3;
					}

					public final class Name {
						public static final String ID = "id";
						public static final String NAME = "name";
						public static final String TAG_TYPE_ID = "tagTypeId";
					}
				}
			}

			/**
			 TagType has following fields:
			 <ul>
			 <li>String id - Unique ID (UUID as String)</li>
			 <li>String name - Description</li>
			 */
			public final class TagType {
				public static final String NAME = "tagTypes";

				public final class Columns {
					public final class Number {
						public static final int ID = 1;
						public static final int NAME = 2;
					}

					public final class Name {
						public static final String ID = "id";
						public static final String NAME = "name";
					}
				}
			}


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
						/** Transient field */
						public static final int DIFF_DISTANCE = 10;
						/** Transient field */
						public static final int DIFF_TIMESTAMP = 11;
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
						/** Transient field */
						public static final String DIFF_DISTANCE = "diffDistance";
						/** Transient field */
						public static final String DIFF_TIMESTAMP = "diffTimestamp";
					}
				}


			}
		}
	}

}
