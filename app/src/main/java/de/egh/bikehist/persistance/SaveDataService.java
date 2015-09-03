package de.egh.bikehist.persistance;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import de.egh.bikehist.R;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract;
import de.egh.bikehist.sync.BikeHistSyncException;
import de.egh.bikehist.sync.ExportFileHandler;
import de.egh.bikehist.sync.SyncController;
import de.egh.bikehist.sync.SyncFileHandler;
import de.egh.bikehist.sync.SyncNotification;

/**
 * Service for long running stuff, like transactional database operations or File startTaskSync/import.
 * Can only handle one task.
 */
public class SaveDataService extends Service {

	private static final String TAG = SaveDataService.class.getSimpleName();

	/**
	 * Unique ID within the app
	 */
	private static final int SYNC_NOTIFICATION_ID = 1;
	private static final int EXPORT_NOTIFICATION_ID = 2;
	private static final int IMPORT_NOTIFICATION_ID = 3;

	/**
	 * Task name while a task is running, otherwise null;
	 */
	private String activeTask = null;

	/**
	 * Task name of the finished or null;
	 */
	private String lastTask = null;

	private Handler handler;
	private SyncController importSyncController;

	public SaveDataService() {
	}

	/**
	 * Manages both fields activeTask and lastTask. Call this to change the task.
	 *
	 * @param newTask Name of task or null.
	 */
	private void changeTask(String newTask) {

		lastTask = activeTask;
		activeTask = newTask;

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//Check precondition: Action expected
		if (intent.getStringExtra(Contract.ACTION) == null) {
			Log.w(TAG, "Missing action");
			return START_NOT_STICKY;
		}

		//Special action: Send service' status
		if (intent.getStringExtra(Contract.ACTION).equals(Contract.GetStatus.NAME)) {
			broadcastStatus();
			return START_NOT_STICKY;
		}

		String action = intent.getStringExtra(Contract.ACTION);
		//Precondition for all other actions: Service must not be busy
		if (activeTask != null) {
			broadcastBusy(action);
			return START_NOT_STICKY;
		}

		switch (action) {
			case Contract.Sync.NAME:
				changeTask(action);
				importSyncController = null;
				startTaskSync();
				break;

			case Contract.Export.NAME:
				changeTask(action);
				importSyncController = null;
				startTaskExport();
				break;

//			case Contract.ImportRead.NAME_STRING:
//				changeTask(action);
//				startTaskImportRead(intent.getData());
//				break;
//
//			case Contract.ImportWrite.NAME_STRING:
//				changeTask(action);
//				startTaskImportWrite();
//				break;

			case Contract.DeleteEntityAction.NAME:
				importSyncController = null;

				//Check precondition: Item ID and MasterData type expected.
				if (intent.hasExtra(Contract.DeleteEntityAction.Parameters.ENTITY_ID)
						&& intent.hasExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE)

						&& (intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE)
						.equals(Contract.DeleteEntityAction.Parameters.TYPES.BIKE)

						|| intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE)
						.equals(Contract.DeleteEntityAction.Parameters.TYPES.TAG)

						|| intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE)
						.equals(Contract.DeleteEntityAction.Parameters.TYPES.TAG_TYPE)

						|| intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE)
						.equals(Contract.DeleteEntityAction.Parameters.TYPES.EVENT)
				)) {
					changeTask(action);
					startTaskDeleteMasterData(intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE),
							intent.getStringExtra(Contract.DeleteEntityAction.Parameters.ENTITY_ID));
				} else
					Log.w(TAG, "Ignore insufficient action " + Contract.DeleteEntityAction.NAME);
				break;

			default:
				Log.w(TAG, "Ignore unknown action " + action);
		}

		return START_NOT_STICKY;
	}

//	/**
//	 * Part one of import: Read external source and make statistic
//	 */
//	private void startTaskImportRead(final Uri uri) {
//
//		Log.d(TAG, "startTaskImportRead() for " + uri.toString());
//
//		importSyncController = null;
//		new ImportReadTask().execute(uri);
//	}

//	/**
//	 * Part two of import: Write imported data to local database
//	 */
//	private void startTaskImportWrite() {
//
//		Log.d(TAG, "startTaskImportWrite()");
//		boolean res = false;
//
//		//Check Precondition
//		if (importSyncController == null) {
//			sendImportWriteResult(false);
//			return;
//		}
//
//		try {
//			importSyncController.runImportWrite();
//			res = true;
//		} catch (BikeHistSyncException e) {
//			res = false;
//			Log.e(TAG, e.getMessage());
//			return;
//		}
//		finally {
//			sendImportWriteResult(res);
//			changeTask(null);
//		}
//
//	}

//	/**
//	 * Call this after reading import file. Precondition: importSyncController is null, if
//	 * import failed.
//	 */
//	private void sendImportWriteResult(boolean success) {
//		Intent intent = new Intent(Contract.INTENT_NAME);
//		intent.putExtra(Contract.ACTION, Contract.ImportWrite.NAME_STRING);
//		intent.putExtra(Contract.ImportWrite.Result.ERROR, !success);
//
//		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//	}

//	/**
//	 * Call this after reading import file. Precondition: importSyncController is null, if
//	 * import failed.
//	 */
//	private void sendImportReadResult() {
//		Intent intent = new Intent(Contract.INTENT_NAME);
//		intent.putExtra(Contract.ACTION, Contract.ImportRead.NAME_STRING);
//		intent.putExtra(Contract.ImportRead.Result.ERROR_BOOLEAN, importSyncController == null);
//
//		if (importSyncController != null) {
//			intent.putExtra(Contract.ImportRead.Result.NR_BIKES_INT,
//					importSyncController.getStatisticReport().getBikesTotal());
//			intent.putExtra(Contract.ImportRead.Result.NR_TAG_TYPES_INT,
//					importSyncController.getStatisticReport().getTagTypesTotal());
//			intent.putExtra(Contract.ImportRead.Result.NR_TAGS_INT,
//					importSyncController.getStatisticReport().getTagsTotal());
//			intent.putExtra(Contract.ImportRead.Result.NR_EVENTS_INT,
//					importSyncController.getStatisticReport().getEventsTotal());
//			intent.putExtra(Contract.ImportRead.Result.STATISTIC_REPORT,
//					importSyncController.getStatisticReport());
//		}
//
//		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//	}

	private void startTaskDeleteMasterData(String Type, String id) {
		switch (Type) {
			case Contract.DeleteEntityAction.Parameters.TYPES.BIKE:
				deleteBike(id);
				break;
			case Contract.DeleteEntityAction.Parameters.TYPES.TAG_TYPE:
				deleteTagType(id);
				break;
			case Contract.DeleteEntityAction.Parameters.TYPES.TAG:
				deleteTag(id);
				break;
			case Contract.DeleteEntityAction.Parameters.TYPES.EVENT:
				deleteEvent(id);
				break;
		}
	}

	/**
	 * Synchronizes all data with external JSON-file.
	 */
	private void startTaskSync() {

		new WorkerTask().execute(new TaskStrategy() {

			final SyncController syncController = new SyncController(SaveDataService.this,
					new SyncFileHandler(SaveDataService.this));


			@Override
			public String getTaskName() {
				return getString(R.string.titleSyncFunctionName);
			}

			@Override
			public int getNotificationId() {
				return SYNC_NOTIFICATION_ID;
			}

			@Override
			public void run(SyncNotification syncNotification) throws BikeHistSyncException {
				syncController.runSync(syncNotification);
			}

			@Override
			public int getIconId() {
				return R.drawable.ic_sync_bar;
			}

			@Override
			public String getContractFunctionName() {
				return Contract.Sync.NAME;
			}
		});

//		new SyncTask().execute();

	}

	/**
	 * Synchronizes all data with external JSON-file.
	 */
	private void startTaskExport() {

		new WorkerTask().execute(new TaskStrategy() {

			final SyncController syncController = new SyncController(SaveDataService.this,
					new ExportFileHandler(SaveDataService.this));


			@Override
			public String getTaskName() {
				return getString(R.string.titleExportFunctionName);
			}

			@Override
			public int getNotificationId() {
				return EXPORT_NOTIFICATION_ID;
			}

			@Override
			public void run(SyncNotification syncNotification) throws BikeHistSyncException {
				syncController.runExport(syncNotification);
			}

			@Override
			public int getIconId() {
				return R.drawable.actionbar_icon_export;
			}

			@Override
			public String getContractFunctionName() {
				return Contract.Export.NAME;
			}
		});

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
	 * Broadcast Result for Request Contract.GetRunningTaskAction.
	 */
	private void broadcastStatus() {
		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.GetStatus.NAME);
		if (activeTask != null) {
			intent.putExtra(Contract.GetStatus.Result.ACTIVE_ACTION, activeTask);
		}
		if (lastTask != null) {
			intent.putExtra(Contract.GetStatus.Result.LAST_ACTION, lastTask);
		}

		if(importSyncController != null){
			intent.putExtra(Contract.GetStatus.Result.STATISTIC_REPORT,
					importSyncController.getStatisticReport());
		}

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

	}

	/**
	 * Broadcast to the caller, that the service is busy and can't handle the new task.
	 */
	private void broadcastBusy(String action) {
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
		intent.putExtra(Contract.ACTION, Contract.DeleteEntityAction.NAME);
		intent.putExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE, Contract.DeleteEntityAction.Parameters.TYPES.TAG);

		ContentResolver cr = getContentResolver();
		int resTag;
		//Always 0
		int res = 0;

		// There may not exists any Events.
		Cursor cEvents = cr.query(BikeHistContract.Tables.Event.URI
				, BikeHistContract.QUERY_COUNT_PROJECTION
				, BikeHistContract.Tables.Event.TagId.NAME + "=?"
				, new String[]{id},
				null);

		if (cEvents != null) {
			// Error: Existing Events
			cEvents.moveToFirst();
			if (cEvents.getInt(0) > 0) {
				Log.e(TAG, "There are existing Events, can't delete Tag.");
				intent.putExtra(Contract.DeleteEntityAction.Result.ERROR, true);
				cEvents.close();
				return;
			}
		}
		// Delete Tag
		//--- TagTypes: Get the one TagType ---//
//		resTag = cr.delete(BikeHistProvider.URI,
//				BikeHistContract.Tables.Tag.Columns.Name.ID + "=?",
//				new String[]{id});

		resTag = setDeleteFlag(BikeHistContract.Tables.Tag.URI, BikeHistContract.Tables.Tag.Id.NAME, id);

		intent.putExtra(Contract.DeleteEntityAction.Result.NO_MAIN_ENTITY_DELETED, resTag);
		intent.putExtra(Contract.DeleteEntityAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, res);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();


	}

	/**
	 * Updates columns DELETED and TOUCHED_AT.
	 */
	private int setDeleteFlag(Uri uri, String fieldName, String id) {

		ContentResolver cr = getContentResolver();
		ContentValues values = new ContentValues();
		values.put(BikeHistContract.Tables.BikeHistEntity.Deleted.NAME, BikeHistContract.Boolean.True.asInt);
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
		intent.putExtra(Contract.ACTION, Contract.DeleteEntityAction.NAME);
		intent.putExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE, Contract.DeleteEntityAction.Parameters.TYPES.TAG_TYPE);

		ContentResolver cr = getContentResolver();
		int resTagTypes;
		//Always 0
		int resTags = 0;

		if (!new EntityLoader(this).hasTags(id)) {
			resTagTypes = setDeleteFlag(BikeHistContract.Tables.TagType.URI, BikeHistContract.Tables.TagType.Id.NAME, id);

			intent.putExtra(Contract.DeleteEntityAction.Result.NO_MAIN_ENTITY_DELETED, resTagTypes);
			intent.putExtra(Contract.DeleteEntityAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, resTags);

		} else {
			Log.e(TAG, "There are existing Tags, can't delete Tag Type.");
		}

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();

	}

	/**
	 * Delete Event from database.
	 */
	private void deleteEvent(String id) {

		int resEvent = setDeleteFlag(BikeHistContract.Tables.Event.URI, BikeHistContract.Tables.Event.Id.NAME, id);

		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.DeleteEntityAction.NAME);
		intent.putExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE, Contract.DeleteEntityAction.Parameters.TYPES.EVENT);
		intent.putExtra(Contract.DeleteEntityAction.Result.NO_MAIN_ENTITY_DELETED, resEvent);


		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();

	}

	/**
	 * Delete Bike from database and all its events.
	 */
	private void deleteBike(String id) {

		int resEvent = setDeleteFlag(BikeHistContract.Tables.Event.URI, BikeHistContract.Tables.Event.BikeId.NAME, id);

		int resBike = setDeleteFlag(BikeHistContract.Tables.Bike.URI, BikeHistContract.Tables.Bike.Id.NAME, id);

		Intent intent = new Intent(Contract.INTENT_NAME);
		intent.putExtra(Contract.ACTION, Contract.DeleteEntityAction.NAME);
		intent.putExtra(Contract.DeleteEntityAction.Parameters.ENTITY_TYPE, Contract.DeleteEntityAction.Parameters.TYPES.BIKE);
		intent.putExtra(Contract.DeleteEntityAction.Result.NO_MAIN_ENTITY_DELETED, resBike);
		intent.putExtra(Contract.DeleteEntityAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, resEvent);


		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		stopSelf();

	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private enum StepStatus {
		NEW, RUNNING, ERROR, SUCCESS;
	}

	private interface TaskStrategy {

		abstract String getTaskName();

		abstract int getNotificationId();

		abstract void run(SyncNotification syncNotification) throws BikeHistSyncException;

		abstract int getIconId();

		abstract String getContractFunctionName();

	}

	/**
	 * Controls the 2 steps of synchronizing.
	 */
	private class ImportController {
		private SyncController syncController;
		private final Uri uri;
		private int nrBikes = 0;
		private int nrTagTypes = 0;
		private int nrTags = 0;
		private int nrEvents = 0;
		private int stepNr;
		private StepStatus stepStatus;

		public ImportController(Uri uri) {


			this.uri = uri;
			this.stepNr = 0;
			this.stepStatus = StepStatus.NEW;

		}

		public boolean isImportFinished() {
			return (stepNr == 1 && (stepStatus.equals(StepStatus.SUCCESS) || stepStatus.equals(StepStatus.ERROR)));
		}

		public boolean isImportSuccessful() {
			return (stepNr == 1 && stepStatus.equals(StepStatus.SUCCESS));
		}

		public void incNrBikes() {
			this.nrBikes++;
		}

		public void incNrTagTypes() {
			this.nrTagTypes++;
		}

		public void incNrTags() {
			this.nrTags++;
		}

		public void incNrEvents() {
			this.nrEvents++;
		}

		public int getNrBikes() {
			return nrBikes;
		}

		public int getNrTagTypes() {
			return nrTagTypes;
		}

		public int getNrTags() {
			return nrTags;
		}

		public int getNrEvents() {
			return nrEvents;
		}

		public SyncController getSyncController() {
			return syncController;
		}

		public void setSyncController(SyncController syncController) {
			this.syncController = syncController;
		}

		public Uri getUri() {
			return uri;
		}

		/**
		 * Returns true, if import successful, otherwise false.
		 */
		public boolean doImport() {
			if (stepNr > 0) {
				throw new IllegalStateException("Wrong stepNr, expected 0 but was " + stepNr);
			}
			stepNr = 1;
			stepStatus = StepStatus.RUNNING;

//			Intent intent = new Intent(Contract.INTENT_NAME);
//			intent.putExtra(Contract.ACTION, Contract.Sync.NAME_STRING);

			try {
				syncController.runImportRead();
				stepStatus = StepStatus.SUCCESS;
				return true;

//				intent.putExtra(Contract.Import.Result.ERROR_BOOLEAN, false);

			} catch (BikeHistSyncException e) {
				stepStatus = StepStatus.ERROR;
				return false;

//				intent.putExtra(Contract.Import.Result.ERROR_BOOLEAN, true);

//			} finally {
//				LocalBroadcastManager.getInstance(SaveDataService.this).sendBroadcast(intent);
			}

		}

		public void doCommit() {
			stepNr = 2;
		}


	}


//	/**
//	 * The defines type Integer is not in use.
//	 */
//	private class ImportReadTask extends AsyncTask<Uri, Void, SyncController> {
//
//		@Override
//		protected void onPostExecute(SyncController syncController) {
//			//Store controller for Writing
//			importSyncController = syncController;
//			//Don't close the service here, but finalize acutal activceTask
//			changeTask(null);
//			sendImportReadResult();
//		}
//
//		@Override
//		protected SyncController doInBackground(Uri... uris) {
//			SyncController sc = new SyncController(SaveDataService.this,
//					new ImportFileHandler(SaveDataService.this, uris[0]));
//			try {
//				sc.runImportRead();
//
//			} catch (BikeHistSyncException e) {
//				e.printStackTrace();
//				return null;
//			}
//
//			return sc;
//		}
//	}

	/**
	 * Generic Task is the worker task for all long running functions
	 * (synchronizing, exporting, importing). The first define type holds
	 * the function specific stuff.
	 * The other defines types will not be used in this class.
	 */
	private class WorkerTask extends AsyncTask<TaskStrategy, Integer, Long> {

		TaskStrategy strategy;

		@Override
		protected Long doInBackground(TaskStrategy... params) {

			strategy = params[0];
			SyncNotification syncNotification = buildSyncNotification(strategy.getNotificationId());
			Intent intent = new Intent(Contract.INTENT_NAME);
			intent.putExtra(Contract.ACTION, strategy.getContractFunctionName());

			syncNotification.create("Start exporting");

			try {
				strategy.run(syncNotification);

				intent.putExtra(Contract.Sync.Result.ERROR, false);

			} catch (BikeHistSyncException e) {
				syncNotification.abort(strategy.getTaskName() + " failed: " + e.getMessage());
				intent.putExtra(Contract.Sync.Result.ERROR, true);

			} finally {
				LocalBroadcastManager.getInstance(SaveDataService.this).sendBroadcast(intent);
				stopSelf();
			}


			return 0L;
		}


		/**
		 * Returns a brand new SynNotification for the given ID.
		 */
		private SyncNotification buildSyncNotification(final int notificationId) {
			return new SyncNotification() {
				final NotificationManager mNotificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				private NotificationCompat.Builder mBuilder;
				private final int id = notificationId;

				@Override
				public void create(String text) {
					mBuilder = new NotificationCompat.Builder(SaveDataService.this)
							.setSmallIcon(strategy.getIconId())
							.setContentTitle(strategy.getTaskName())
							.setContentText(text);

					mBuilder.setOngoing(true);

					// mId allows you to update the notification later on.
					mNotificationManager.notify(id, mBuilder.build());
				}

				@Override
				public void update(String text, int max, int progress) {
					mBuilder = new NotificationCompat.Builder(SaveDataService.this)
							.setSmallIcon(strategy.getIconId())
							.setContentTitle(getString(R.string.titleSyncNotification))
							.setContentText(text);
					NotificationManager mNotificationManager =
							(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mBuilder.setProgress(max, progress, false);
					// mId allows you to update the notification later on.
					mNotificationManager.notify(id, mBuilder.build());
				}

				@Override
				public void finish(String text, String subText) {
					mBuilder = new NotificationCompat.Builder(SaveDataService.this)
							.setSmallIcon(strategy.getIconId())
							.setPriority(NotificationCompat.PRIORITY_MIN)
							.setContentTitle(strategy.getTaskName() + " " + "finished")
							.setContentText(text)
							.setSubText(subText);
					// mId allows you to update the notification later on.
					mNotificationManager.notify(id, mBuilder.build());
				}

				@Override
				public void abort(String text) {
					mBuilder = new NotificationCompat.Builder(SaveDataService.this)
							.setSmallIcon(R.drawable.ic_sync_error_bar)
							.setPriority(NotificationCompat.PRIORITY_DEFAULT)
							.setContentTitle(strategy.getTaskName() + " " + "aborted")
							.setContentText(text);
					// mId allows you to update the notification later on.
					mNotificationManager.notify(id, mBuilder.build());
				}
			};
		}

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

		public abstract class Sync {

			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME = "Sync";

			public abstract class Result {
				/**
				 * Boolean with TRUE, if action failed. Otherwise not set.
				 */
				public static final String ERROR = "ERROR_BOOLEAN";

			}
		}


		public abstract class Export {

			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME = "Export";

			public abstract class Result {
				/**
				 * Boolean with TRUE, if action failed. Otherwise not set.
				 */
				public static final String ERROR = "ERROR_BOOLEAN";

			}
		}


		/**
		 * Uri will be shipped by setData(Uri)
		 */
		public abstract class ImportRead {

			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME_STRING = "ImportRead";

			public abstract class Result {

				/**
				 * Boolean with TRUE, if action failed. Otherwise FALSE
				 */
				public static final String ERROR_BOOLEAN = "ERROR_BOOLEAN";

				public static final String NR_BIKES_INT = "NR_BIKES_INT";
				public static final String NR_TAG_TYPES_INT = "NR_TAG_TYPES_INT";
				public static final String NR_TAGS_INT = "NR_TAGS_INT";
				public static final String NR_EVENTS_INT = "NR_EVENTS_INT";
				public static final String STATISTIC_REPORT = "STATISTIC_REPORT";

			}
		}

		/**
		 * Second part of import: Write to db
		 */
		public abstract class ImportWrite {

			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME_STRING = "ImportWrite";

			public abstract class Result {

				/**
				 * Boolean with TRUE, if action failed. Otherwise not set.
				 */
				public static final String ERROR = "ERROR_BOOLEAN";

			}
		}

		/**
		 * Request for Service' status. Broadcast the actual running action or an empty Broadcast,
		 * if Service is idle.
		 */
		public abstract class GetStatus {

			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME = "AreYouBusyAction";

			public abstract class Result {
				/**
				 * String with NAME_STRING of the active Action.
				 */
				public static final String ACTIVE_ACTION = "ACTIVE_ACTION";
				public static final String LAST_ACTION = "LAST_ACTION";

                /** Only used for Import steps as lastAction. */
				public static final String STATISTIC_REPORT = "STATISTIC_REPORT";


			}

		}

		/**
		 * Defines all particular constants for action 'Delete Entity'.
		 * Only set the delete-flag, no dataset will be removed.
		 */
		public abstract class DeleteEntityAction {


			/**
			 * NAME_STRING of the action. Value for ACTION
			 */
			public static final String NAME = "DeleteEntityAction";

			/**
			 * Incoming Parameters to give to the service within the Intent.
			 * In addition to that, there is a parameter {@link de.egh.bikehist.ui.masterdata.MasterDataContract.Type}
			 * for the Type fo of the Master Data od 'event'
			 */
			public abstract class Parameters {
				/**
				 * UUID of the Master Data as String
				 */
				public static final String ENTITY_ID = "entityId";

				public static final String ENTITY_TYPE = "entityType";

				public abstract class TYPES {
					public static final String BIKE = "bike";
					public static final String TAG_TYPE = "tagType";
					public static final String TAG = "tag";
					public static final String EVENT = "event";
				}
			}

			/**
			 * Result send by a LocalBroadcast. In addition to this, the {@link
			 * de.egh.bikehist.ui.masterdata.MasterDataContract.Type} or 'event'
			 * will be returned.
			 */
			public abstract class Result {

				/**
				 * Boolean with TRUE, if action failed. Otherwise not set.
				 */
				public static final String ERROR = "ERROR_BOOLEAN";

				/**
				 * Number of deleted Master Data as int
				 */
				public static final String NO_MAIN_ENTITY_DELETED = "NO_MAIN_ENTITY_DELETED";
				/**
				 * Number of changed or deleted dependent items
				 */
				public static final String NO_DEPENDENT_ITEMS_TOUCHED = "NO_DEPENDENT_ITEMS_TOUCHED";
			}
		}
	}

}
