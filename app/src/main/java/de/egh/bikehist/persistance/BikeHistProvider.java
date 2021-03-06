package de.egh.bikehist.persistance;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;

import de.egh.bikehist.BuildConfig;

/**
 * Content Provider.
 */
public class BikeHistProvider extends ContentProvider {

	private static final String TAG = BikeHistProvider.class.getSimpleName();
	// Creates a UriMatcher object.
	private static final UriMatcher uriMatcher;

	// Allocate the UriMatcher object, where a URI ending in 'earthquakes' will
	// correspond to a request for all earthquakes, and 'earthquakes' with a
	// trailing '/[rowID]' will represent a single earthquake row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Event.NAME, Constants.Uri.Event.MULTI);
		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Event.NAME + "/#", Constants.Uri.Event.ID);

		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Bike.NAME, Constants.Uri.Bike.MULTI);
		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Bike.NAME + "/#", Constants.Uri.Bike.ID);

		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Tag.NAME, Constants.Uri.Tag.MULTI);
		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.Tag.NAME + "/#", Constants.Uri.Tag.ID);

		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.TagType.NAME, Constants.Uri.TagType.MULTI);
		uriMatcher.addURI(BikeHistContract.AUTHORITY, BikeHistContract.Tables.TagType.NAME + "/#", Constants.Uri.TagType.ID);
	}

	//The underlying database
	private SQLiteDatabase bikeHistDB;

	/**
	 * Transactional batch processing. Idionm seen here: http://www.databaseskill.com/3214964/
	 */
	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {

		bikeHistDB.beginTransaction();
		try {
			ContentProviderResult[] results = super.applyBatch(operations);
			bikeHistDB.setTransactionSuccessful();//successful
			return results;
		} finally {
			bikeHistDB.endTransaction();
		}
	}

	@Override
	public boolean onCreate() {

		BikeHistDatabaseHelper dbHelper = new BikeHistDatabaseHelper(getContext()
		);
		bikeHistDB = dbHelper.getWritableDatabase();

//		if (dbHelper.isDropped()) {
//			createDummyData();
//		}

		return (bikeHistDB != null);
	}


	/**
	 * @throws java.lang.IllegalArgumentException Wrong URI
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
				return "vnd.android.cursor.event_item/vnd.de.egh.provider.event";

			case Constants.Uri.Bike.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.bike";
			case Constants.Uri.Bike.ID:
				return "vnd.android.cursor.event_item/vnd.de.egh.provider.bike";

			case Constants.Uri.Tag.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.tag";
			case Constants.Uri.Tag.ID:
				return "vnd.android.cursor.event_item/vnd.de.egh.provider.tag";

			case Constants.Uri.TagType.MULTI:
				return "vnd.android.cursor.dir/vnd.de.egh.provider.tagType";
			case Constants.Uri.TagType.ID:
				return "vnd.android.cursor.event_item/vnd.de.egh.provider.tagType";

			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return TableStrategy.create(bikeHistDB, uri).delete(selection, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return TableStrategy.create(bikeHistDB, uri).update(values, selection, selectionArgs);
	}

	/**
	 * @throws java.lang.IllegalArgumentException Wrong URI
	 * @throws android.database.SQLException      Insert failed
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


	/**
	 * Access to a specific table.
	 */
	private abstract static class TableStrategy {

		final String tableName;
		final SQLiteDatabase db;
		Uri uri;

		/**
		 * Internal use only. Use factory method instead.
		 */
		private TableStrategy(SQLiteDatabase db, Uri uri, String tableName) {
			this.uri = uri;
			this.tableName = tableName;
			this.db = db;
		}

		/**
		 * Database access for a particular table.
		 *
		 * @return TableStrategy
		 * @throws IllegalArgumentException Uri could not resolved for a table
		 */
		static TableStrategy create(SQLiteDatabase db, Uri uri) {
			if (uri.toString().contains(BikeHistContract.AUTHORITY + "/" + BikeHistContract.Tables.Event.NAME)) {
				return new EventStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.AUTHORITY + "/" + BikeHistContract.Tables.Bike.NAME)) {
				return new BikeStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.AUTHORITY + "/" + BikeHistContract.Tables.TagType.NAME)) {
				return new TagTypeStrategy(db, uri);
			}

			if (uri.toString().contains(BikeHistContract.AUTHORITY + "/" + BikeHistContract.Tables.Tag.NAME)) {
				return new TagStrategy(db, uri);
			}

			throw new IllegalArgumentException("Unknown table addressed: " + uri.toString());

		}

		int delete(String selection, String[] selectionArgs) {
			return db.delete(tableName, selection, selectionArgs);
		}


		int update(ContentValues values, String selection, String[] selectionArgs) {
			return db.update(tableName, values, selection, selectionArgs);
		}

		/**
		 * @throws android.database.SQLException Insert failed
		 */
		Uri insert(ContentValues _initialValues) {
			// Insert the new row, will return the row number if
			// successful.
			long rowID = db.insert(tableName, null, _initialValues);

			// Return a URI to the newly inserted row on success.
			if (rowID > 0) {
				return uri = ContentUris.withAppendedId(BikeHistContract.Tables.Event.URI, rowID);
			}
			throw new SQLException("Insert failed into " + uri + " for " + _initialValues.toString());
		}

		/**
		 * Has to support count(*) AS count.
		 */
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
				orderBy = BikeHistContract.Tables.BikeHistEntity.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Bike.ID:
					qb.appendWhere(BikeHistContract.Tables.BikeHistEntity.Id.NAME + "=?");
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
				orderBy = BikeHistContract.Tables.Tag.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Tag.ID:
					qb.appendWhere(BikeHistContract.Tables.Tag.Id.NAME + "=?");
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
				orderBy = BikeHistContract.Tables.BikeHistEntity.Name.NAME;
			} else {
				orderBy = sortOrder;
			}


			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Tag.ID:
					qb.appendWhere(BikeHistContract.Tables.BikeHistEntity.Id.NAME + "=?");
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

			qb.setTables(tableName);

			// If no sort order is specified sort by date / time
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = BikeHistContract.Tables.Event.Timestamp.NAME;
			} else {
				orderBy = sortOrder;
			}

			// If this is a row query, limit the result set to the passed in row.
			switch (uriMatcher.match(uri)) {
				case Constants.Uri.Event.ID:
					qb.appendWhere(BikeHistContract.Tables.BikeHistEntity.Id.NAME + "=?");
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

			//Just a count(*)
			if (projection != null && projection.length == 1 && projection[0].equals("count(*) AS count")) {
				return c;
			}

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
					BikeHistProvider.BikeHistContract.Tables._ID,
					BikeHistContract.Tables.Event.Id.NAME,
					BikeHistContract.Tables.Event.Name.NAME,
					BikeHistContract.Tables.Event.Deleted.NAME,
					BikeHistContract.Tables.Event.TouchedAt.NAME,
					BikeHistContract.Tables.Event.Distance.NAME,
					BikeHistContract.Tables.Event.BikeId.NAME,
					BikeHistContract.Tables.Event.TagId.NAME,
					BikeHistContract.Tables.Event.Timestamp.NAME,
					BikeHistContract.Tables.Event.DiffDistance.NAME,
					BikeHistContract.Tables.Event.DiffTimestamp.NAME});

			// For all entries: Map to new cursor and add transient fields
			long distance;
			long timestamp;
			long diffDistance;
			long diffTimestamp;

			if (c.getCount() > 0) {
				do {
					distance = c.getLong(BikeHistContract.Tables.Event.Distance.NUMBER);
					timestamp = c.getLong(BikeHistContract.Tables.Event.Timestamp.NUMBER);

					// Get corresponding Event: next Event with same Tag
					Cursor c2 = db.rawQuery(
							"SELECT * FROM ( SELECT " +
									BikeHistContract.Tables.Event.Distance.NAME + ", " +
									BikeHistContract.Tables.Event.Timestamp.NAME +
									" FROM " + BikeHistContract.Tables.Event.NAME +
									" WHERE " + BikeHistContract.Tables.Event.BikeId.NAME + "=?" +
									" AND " + BikeHistContract.Tables.Event.TagId.NAME + "=?" +
									" AND " + BikeHistContract.Tables.Event.Timestamp.NAME + ">?" +
									" ORDER BY " + BikeHistContract.Tables.Event.Timestamp.NAME + " ASC)" +
									" LIMIT 1"
							,
							new String[]{c.getString(BikeHistContract.Tables.Event.BikeId.NUMBER),
									c.getString(BikeHistContract.Tables.Event.TagId.NUMBER),
									c.getString(BikeHistContract.Tables.Event.Timestamp.NUMBER)}
					);

					if (c2.getCount() == 1) {
						c2.moveToFirst();
						diffDistance = c2.getLong(0) - distance;
						diffTimestamp = c2.getLong(1) - timestamp;
					} else {
						diffDistance = 0;
						diffTimestamp = 0;
					}

					//Create new entries
					Object[] values = {
							c.getInt(0), //The key for the database entry
							c.getString(BikeHistContract.Tables.Event.Id.NUMBER),
							c.getString(BikeHistContract.Tables.Event.Name.NUMBER),
							c.getString(BikeHistContract.Tables.Event.Deleted.NUMBER),
							c.getString(BikeHistContract.Tables.Event.TouchedAt.NUMBER),
							distance,
							c.getString(BikeHistContract.Tables.Event.BikeId.NUMBER),
							c.getString(BikeHistContract.Tables.Event.TagId.NUMBER),
							timestamp,
							diffDistance,
							diffTimestamp
					};
					mc.addRow(values);
					c2.close();

				} while (c.moveToNext());
			}
			c.close();
			return mc;
		}
	}

	static final class Constants {
		static final class Database {
			static final String NAME = "bikeHist.db";
			static final int VERSION = 20;

		}

		/**
		 * Accessing data for all data and for single access
		 */
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
	 * Contract for the consumer of the ContentProvider. The constants defined, only shows the
	 * outer view to the persistance layer. It does not define the table definition of the
	 * underlaying database. So, this Contract may not be used for defining the database.
	 */
	public static final class BikeHistContract {
		/**
		 * Use this as Projection in the query() for a count(*).
		 */
		public static final String[] QUERY_COUNT_PROJECTION = {"count(*) AS count"};
		/**
		 * URI path for the ContentProvider. Also known as authority. Must match
		 * with the manifest entry.
		 */
			public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
				".provider.bikehist";


		/**
		 * ContentProvider doesn't hide that SQLite database has no Boolean type. Use Int instead.
		 */
		public static final class Boolean {
			public static final class False {
				public static final int asInt = 0;
				public static final String asString = "0";
			}

			public final class True {
				public static final int asInt = 1;
				public static final String asString = "1";
			}
		}

		/**
		 * For all tables: EVENT_ID is the UUID, not database table id (_ID)
		 */
		public static final class Tables {

			/**
			 * Field name for the key field in the database tables. Never use this for
			 * domain classes.
			 */
			public static final String _ID = "_id";

			/**
			 * Base fields for all Entities.
			 */
			public static abstract class BikeHistEntity {

				/**
				 * UUID
				 */
				public static final class Id {
					public static final int NUMBER = 1;
					public static final String NAME = "id";
				}

				/**
				 * Readable name
				 */
				public static final class Name {
					public static final int NUMBER = 2;
					public static final String NAME = "name";
				}

				/**
				 * Int as Boolean: 1 (true), if dataset is deleted, otherwise 0
				 */
				public final static class Deleted {
					public static final int NUMBER = 3;
					public static final String NAME = "deleted";
				}


				/**
				 * system time of last touch (modified data)
				 */
				public final static class TouchedAt {
					public static final int NUMBER = 4;
					public static final String NAME = "touchedAt";
				}
			}

			/**
			 * Additional constants for table of Tags, base definition, see BikeHistEntity
			 */
			public static final class Tag extends BikeHistEntity {
				/**
				 * Name of the table
				 */
				public static final String NAME = "tags";
				public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" //
						+ NAME);

				/**
				 * Foreign key: ID of TagType
				 */
				public final class TagTypeId {
					public static final int NUMBER = 5;
					public static final String NAME = "tagTypeId";
				}
			}

			/**
			 * Additional constants for table of TagTypes, base definition, see BikeHistEntity
			 */
			public static final class TagType extends BikeHistEntity {
				/**
				 * Name of the table
				 */
				public static final String NAME = "tagTypes";
				public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" //
						+ NAME);
			}

			/**
			 * Additional constants for table of Bikes, base definition, see BikeHistEntity
			 */
			public static final class Bike extends BikeHistEntity {


				/**
				 * Name of the table
				 */
				public static final String NAME = "bikes";
				public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" //
						+ NAME);

				/**
				 * String with the frame number
				 */
				public final class FrameNumber {
					public static final int NUMBER = 5;
					public static final String NAME = "frameNumber";
				}
			}

			/**
			 * Additional constants for table of Events, base definition, see BikeHistEntity
			 */
			public static final class Event extends BikeHistEntity {
				/**
				 * Name of the table
				 */
				public static final String NAME = "events";
				public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" //
						+ NAME);

				/**
				 * Long with the distance in meters
				 */
				public static final class Distance {
					public static final int NUMBER = 5;
					public static final String NAME = "distance";
				}

				/**
				 * Foreign key: ID of Bike
				 */
				public static final class BikeId {
					public static final int NUMBER = 6;
					public static final String NAME = "bikeId";
				}

				/**
				 * Foreign key: ID of Tag
				 */
				public static final class TagId {
					public static final int NUMBER = 7;
					public static final String NAME = "tagId";
				}

				/**
				 * Long with system time of the event
				 */
				public static final class Timestamp {
					public static final int NUMBER = 8;
					public static final String NAME = "timestamp";
				}

				/**
				 * Transient field: Long the differnce of the distance
				 */
				public static final class DiffDistance {
					public static final int NUMBER = 9;
					public static final String NAME = "diffDistance";
				}

				/**
				 * Transient field: Long the differnce of the timestamp
				 */
				public static final class DiffTimestamp {
					public static final int NUMBER = 10;
					public static final String NAME = "diffTimestamp";
				}
			}

		}
	}

}
