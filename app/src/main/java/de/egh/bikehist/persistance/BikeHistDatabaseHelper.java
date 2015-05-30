package de.egh.bikehist.persistance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 Created by ChristianSchulzendor on 04.02.2015.
 */ // Helper class for opening, creating, and managing database version control
class BikeHistDatabaseHelper extends SQLiteOpenHelper {

	/**
	 In update case the database tables will be dropped.

	 @return true, if database tables have been dropped.
	 */
	public boolean isDropped() {
		return dropped;
	}

	private boolean dropped = false;

	private static final String TAG = BikeHistDatabaseHelper.class.getSimpleName();
	private static final String CREATE_EVENTS =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Event.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Id.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Deleted.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.TouchedAt.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Distance.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.BikeId.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.TagId.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.GeoLongitude.NAME + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.GeoLatitude.NAME + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.GeoAltitude.NAME + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Timestamp.NAME + " INTEGER); ";

	private static final String CREATE_BIKES =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Bike.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Deleted.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.TouchedAt.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.FrameNumber.NAME + " TEXT); ";

	private static final String CREATE_TAGS =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Tag.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.Id.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Deleted.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.TouchedAt.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.TagTypeId.NAME + " TEXT); ";

	private static final String CREATE_TAG_TYPES =
			"create table " + BikeHistProvider.BikeHistContract.Tables.TagType.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.TagType.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Deleted.NAME + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.TouchedAt.NAME + " INTEGER);  ";


	public BikeHistDatabaseHelper(Context context) {
		super(context, BikeHistProvider.Constants.Database.NAME, null, BikeHistProvider.Constants.Database.VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_EVENTS);
		db.execSQL(CREATE_BIKES);
		db.execSQL(CREATE_TAG_TYPES);
		db.execSQL(CREATE_TAGS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");

		db.execSQL("DROP TABLE IF EXISTS " + BikeHistProvider.BikeHistContract.Tables.Event.NAME + "; ");
		db.execSQL("DROP TABLE IF EXISTS " + BikeHistProvider.BikeHistContract.Tables.Bike.NAME + "; ");
		db.execSQL("DROP TABLE IF EXISTS " + BikeHistProvider.BikeHistContract.Tables.Tag.NAME + "; ");
		db.execSQL("DROP TABLE IF EXISTS " + BikeHistProvider.BikeHistContract.Tables.TagType.NAME + "; ");
		dropped = true;
		onCreate(db);
	}
}
