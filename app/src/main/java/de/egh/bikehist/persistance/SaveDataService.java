package de.egh.bikehist.persistance;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract;
import de.egh.bikehist.sync.BikeHistSyncException;
import de.egh.bikehist.sync.SyncController;
import de.egh.bikehist.sync.SyncFileHandler;
import de.egh.bikehist.ui.masterdata.MasterDataContract;

/**
 * Service for long running stuff, like transactional database operations or File sync/import.
 * Can only handle one task.
 */
public class SaveDataService extends Service {

	private static final String TAG = SaveDataService.class.getSimpleName();
	/**
	 * Task name while a task is running, otherwise null;
	 */
	private String activeTask = null;

	public SaveDataService() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent.getStringExtra(Contract.ACTION) != null) {
			if (activeTask != null) {
				broadCastBusy(intent.getStringExtra(Contract.ACTION));
			} else {
				//Set once while Service is running. Never set to null.
				activeTask = intent.getStringExtra(Contract.ACTION);
			}

			if (intent.getStringExtra(Contract.ACTION).equals(Contract.Export.NAME)) {
				sync();

			} else if (intent.getStringExtra(Contract.ACTION).equals(Contract.DeleteMasterDataAction.NAME)) {
				//Delete Bike
				if (intent.getStringExtra(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.BIKE)) {
					deleteBike(intent.getStringExtra(Contract.DeleteMasterDataAction.Parameters.ITEM_ID));
				}
				//Delete TagType
				else if (intent.getStringExtra(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.TAG_TYPE)) {
					deleteTagType(intent.getStringExtra(Contract.DeleteMasterDataAction.Parameters.ITEM_ID));
				}
				//Delete Tag
				else if (intent.getStringExtra(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.TAG)) {
					deleteTag(intent.getStringExtra(Contract.DeleteMasterDataAction.Parameters.ITEM_ID));
				}
			}
		}

		return START_NOT_STICKY;
	}

	/**
	 * Synchronizes all data with external JSON-file.
	 */
	private void sync() {

		SyncController syncController = new SyncController(this, new SyncFileHandler(this));

		try {
			syncController.run();

		} catch (BikeHistSyncException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			stopSelf();
		}


	}


	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Broadcast to the caller, that the service is busy and can't handle the new task.
	 */
	private void broadCastBusy(String action) {
		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, action);
		intent.putExtra(Contract.ABGELEHNT, true);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

	}

	/**
	 * Delete Tag from database. The Tag Type must be 'empty' (no existing Tags)
	 */
	private void deleteTag(String id) {

		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.DeleteMasterDataAction.NAME);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG);

		ContentResolver cr = getContentResolver();
		int resTag;
		//Always 0
		int res = 0;

		// There may not exists any Events.
		Cursor cEvents = cr.query(BikeHistProvider.CONTENT_URI_EVENTS
				, BikeHistContract.QUERY_COUNT_PROJECTION
				, BikeHistContract.Tables.Event.TagId.NAME + "=?"
				, new String[]{id},
				null);

		if (cEvents != null) {
			// Error: Existing Events
			cEvents.moveToFirst();
			if (cEvents.getInt(0) > 0) {
				Log.e(TAG, "There are existing Events, can't delete Tag.");
				intent.putExtra(Contract.DeleteMasterDataAction.Result.ERROR, true);
				cEvents.close();
				return;
			}
		}
		// Delete Tag
		//--- TagTypes: Get the one TagType ---//
//		resTag = cr.delete(BikeHistProvider.CONTENT_URI_TAGS,
//				BikeHistContract.Tables.Tag.Columns.Name.ID + "=?",
//				new String[]{id});

		resTag = setDeleteFlag(BikeHistProvider.CONTENT_URI_TAGS, BikeHistContract.Tables.Tag.Id.NAME, id);

		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_MAIN_MASTER_DATA_DELETED, resTag);
		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, res);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();


	}

	/**
	 * Updates columns DELETED and TOUCHED_AT.
	 */
	private int setDeleteFlag(Uri uri, String fieldName, String id) {

		ContentResolver cr = getContentResolver();
		ContentValues values = new ContentValues();
		values.put(BikeHistContract.Tables.BikeHistEntity.Deleted.NAME, 1);
		values.put(BikeHistContract.Tables.BikeHistEntity.TouchedAt.NAME, System.currentTimeMillis());

		//Uri uri, ContentValues values, String where, String[] selectionArgs
		return cr.update(uri, values, fieldName + "=?"
				, new String[]{id});
	}

	/**
	 * Delete Tag Type from database. The Tag Type must be 'empty' (no existing Tags)
	 */
	private void deleteTagType(String id) {

		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.DeleteMasterDataAction.NAME);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG_TYPE);

		ContentResolver cr = getContentResolver();
		int resTagTypes;
		//Always 0
		int resTags = 0;

		// There may not exists any tags.
		Cursor cTags = cr.query(BikeHistProvider.CONTENT_URI_TAGS
				, BikeHistContract.QUERY_COUNT_PROJECTION
				, BikeHistContract.Tables.Tag.TagTypeId.NAME + "=?"
				, new String[]{id},
				null);

		// Error: Existing Tags
		if (cTags != null) {
			cTags.moveToFirst();
			if (cTags.getInt(0) > 0) {
				Log.e(TAG, "There are existing Tags, can't delete Tag Type.");
				intent.putExtra(Contract.DeleteMasterDataAction.Result.ERROR, true);
				cTags.close();
				return;
			}
		}

		// Delete Tag Type
//		//--- TagTypes: Get the one TagType ---//
//		resTagTypes = cr.delete(BikeHistProvider.CONTENT_URI_TAG_TYPES,
//				BikeHistContract.Tables.TagType.Id.NAME + "=?",
//				new String[]{id}
//		);
		resTagTypes = setDeleteFlag(BikeHistProvider.CONTENT_URI_TAG_TYPES, BikeHistContract.Tables.TagType.Id.NAME, id);

		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_MAIN_MASTER_DATA_DELETED, resTagTypes);
		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, resTags);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();

	}

	/**
	 * Delete Bike from database and all its events.
	 */
	private void deleteBike(String id) {

		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.DeleteMasterDataAction.NAME);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.BIKE);

		ContentResolver cr = getContentResolver();

		//--- TagTypes: Get the one TagType ---//
//		int resEvent = cr.delete(BikeHistProvider.CONTENT_URI_EVENTS,
//				BikeHistContract.Tables.Event.BikeId.NAME + "=?",
//				new String[]{id}
//		);
		int resEvent = setDeleteFlag(BikeHistProvider.CONTENT_URI_EVENTS, BikeHistContract.Tables.Event.BikeId.NAME, id);

//		int resBike = cr.delete(BikeHistProvider.CONTENT_URI_BIKES,
//				BikeHistContract.Tables.Bike.Id.NAME + "=?",
//				new String[]{id}
//		);
		int resBike = setDeleteFlag(BikeHistProvider.CONTENT_URI_BIKES, BikeHistContract.Tables.Bike.Id.NAME, id);

		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_MAIN_MASTER_DATA_DELETED, resBike);
		intent.putExtra(Contract.DeleteMasterDataAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, resEvent);


		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();

	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Public Contract for consumer.
	 */
	public abstract class Contract {

		public static final String INTENT_NAME = "MY_INTENT";
		/**
		 * Action as String to do. Used as Parameter for the Service Intent
		 * and as result in the Broadcast Intent.
		 */
		public static final String ACTION = "ACTION";

		/**
		 * Boolean with TRUE, if Action was not handled. Otherwise not used.
		 */
		public static final String ABGELEHNT = "ABGELEHNT";

		/**
		 * Defines all particular constants for action 'Export'
		 */
		public abstract class Export {

			/**
			 * NAME of the action. Value for ACTION
			 */
			public static final String NAME = "Export";

		}

		/**
		 * Defines all particular constants for action 'Delete Master Data'.
		 * Only set the delete-flag, no dataset will be removed.
		 */
		public abstract class DeleteMasterDataAction {

			/**
			 * NAME of the action. Value for ACTION
			 */
			public static final String NAME = "DeleteMasterDataAction";

			/**
			 * Incoming Parameters to give to the service within the Intent.
			 * In addition to that, there is a parameter {@link de.egh.bikehist.ui.masterdata.MasterDataContract.Type}
			 * for the Type fo of the Master Data.
			 */
			public abstract class Parameters {
				/**
				 * UUID of the Master Data as String
				 */
				public static final String ITEM_ID = "DeleteMasterDataItemId";
			}

			/**
			 * Result send by a LocalBroadcast. In addition to this, the {@link
			 * de.egh.bikehist.ui.masterdata.MasterDataContract.Type}
			 * will be returned.
			 */
			public abstract class Result {

				/**
				 * Boolean with TRUE, if action failed. Otherwise not set.
				 */
				public static final String ERROR = "ERROR";

				/**
				 * Number of deleted Master Data as int
				 */
				public static final String NO_MAIN_MASTER_DATA_DELETED = "NO_MAIN_MASTER_DATA_DELETED";
				/**
				 * Number of changed or deleted dependent items
				 */
				public static final String NO_DEPENDENT_ITEMS_TOUCHED = "NO_DEPENDENT_ITEMS_TOUCHED";
			}
		}
	}
}
