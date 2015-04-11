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
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.DISTANCE + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.BIKE_ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.TAG_ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_LONGITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_LATITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_ALTITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP + " INTEGER); ";

	private static final String CREATE_BIKES =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Bike.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER + " TEXT); ";

	private static final String CREATE_TAGS =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Tag.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.TAG_TYPE_ID + " TEXT); ";

	private static final String CREATE_TAG_TYPES =
			"create table " + BikeHistProvider.BikeHistContract.Tables.TagType.NAME + " ("
					+ BikeHistProvider.BikeHistContract.Tables._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME + " TEXT); ";


	public BikeHistDatabaseHelper(Context context, String name,
	                              SQLiteDatabase.CursorFactory factory, int version) {
		super(context, name, factory, version);
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
