package de.egh.bikehist.de.egh.bikehist.persistance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
Created by ChristianSchulzendor on 04.02.2015.
*/ // Helper class for opening, creating, and managing database version control
class BikeHistDatabaseHelper extends SQLiteOpenHelper {

	private static final class Constants{
		/** Field name for the key field in the database tables. */
		static final String _ID = "_ID";
	}

	/**In update case the database tables will be dropped.
	 @return true, if database tables have been dropped. */
	public boolean isDropped() {
		return dropped;
	}

	private boolean dropped = false;

	private static final String TAG = BikeHistDatabaseHelper.class.getSimpleName();
	private static final String DATABASE_CREATE =
			"create table " + BikeHistProvider.BikeHistContract.Tables.Event.NAME + " ("
					+ Constants._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.NAME + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.DISTANCE + " INTEGER, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.BIKE_ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.TAG_ID + " TEXT, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_LONGITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_LATITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.GEO_ALTITUDE + " REAL, "
					+ BikeHistProvider.BikeHistContract.Tables.Event.Columns.Name.TIMESTAMP + " INTEGER);";

	public BikeHistDatabaseHelper(Context context, String name,
	                              SQLiteDatabase.CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");

		db.execSQL("DROP TABLE IF EXISTS " + BikeHistProvider.BikeHistContract.Tables.Event.NAME);
		dropped = true;
		onCreate(db);
	}
}
