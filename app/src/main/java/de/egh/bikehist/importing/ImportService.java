package de.egh.bikehist.importing;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.sync.BikeHistSyncException;
import de.egh.bikehist.sync.JsonHelper;
import de.egh.bikehist.sync.SyncData;

/**
 * Service for a 2-step import process: reading file and writing to database
 */
public class ImportService extends Service {

	private static final String TAG = ImportService.class.getSimpleName();

	private static final Callbacks callbacksDummy = new Callbacks() {

		@Override
		public void onStatusChanged(Status status) {
			//Dummy
		}
	};
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	private Status status = Status.INITIAL;
	private Callbacks callbacks = callbacksDummy;
	private Uri uri;
	private Statistic statistic = new Statistic();
	private ReadData data = null;
	private final ImportManager importManager = new ImportManager() {
		@Override
		public Status getStatus() {
			return status;
		}

		@Override
		public Uri getUri() {
			return uri;
		}

		@Override
		public StatisticValues getStatisticValues() {
			return statistic;
		}

		@Override
		public void setCallbacks(Callbacks callbacks) {
			if (callbacks == null)
				ImportService.this.callbacks = callbacksDummy;
			else
				ImportService.this.callbacks = callbacks;
		}

		@Override
		public void startReading(Uri uri) {
			ImportService.this.startReading(uri);
		}

		@Override
		public void reset() {
			ImportService.this.reset();
		}

		@Override
		public void startWriting() {
			ImportService.this.startWriting();
		}

		@Override
		public void stopService() {
			ImportService.this.stopSelf();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Set the status and post it via callback
	 */
	private void setStatus(Status newStatus) {
		status = newStatus;
		callbacks.onStatusChanged(status);
	}

	private void reset() {
		uri = null;
		statistic = new Statistic();
		setStatus(Status.INITIAL);

	}

	/**
	 * To be called after writing has finished.
	 */
	private void onWriteDone(WriteController controller) {
		if (controller.hasFinishedOk()) {
			setStatus(Status.WRITE_OK);
		} else {
			setStatus(Status.WRITE_NOK);
		}

	}


	/**
	 * To be called after reading has finished. Controller can be null, if failed?=???
	 */
	private void onReadDone(ReadController controller) {
		data = controller.getReadData();

		statistic.setNrBikes(data.getBikes().size());
		statistic.setNrTagTypes(data.getTagTypes().size());
		statistic.setNrTags(data.getTags().size());
		statistic.setNrEvents(data.getEvents().size());

		if (controller.hasFinishedOk()) {
			setStatus(Status.READ_OK);
		} else {
			setStatus(Status.READ_FAILED);
		}

	}

	private void startReading(Uri uri) {

		//Save for Dialog's request with ImportManager.getUri()
		this.uri = uri;
		new ReadTask().execute(uri);

	}

	private void startWriting() {

		new WriteTask().execute(data);

	}

	/**
	 * Status of Service.
	 */
	public enum Status {
		INITIAL, READING, READ_OK, READ_FAILED, WRITING, WRITE_OK, WRITE_NOK;
	}

	/**
	 * Bounded Consumer's interface
	 */
	public interface ImportManager {
		/**
		 * Returns the actual status of the import process.
		 */
		abstract Status getStatus();

		/**
		 * Return Uri shipped with startReading, or null.
		 */
		abstract Uri getUri();

		/**
		 * Returns statistic, never null.
		 */
		abstract StatisticValues getStatisticValues();

		/**
		 * Consumer's listener.
		 */
		abstract void setCallbacks(Callbacks callbacks);

		/**
		 * Import step one: Read file
		 */
		abstract void startReading(Uri uri);

		/**
		 * Call this before restarting the import
		 */
		abstract void reset();

		/**
		 * Import step 2: write to database
		 */
		abstract void startWriting();

		/**
		 * Service doesn't stop by itself, so consumer has to stop it explicitly.
		 */
		abstract void stopService();

	}

	/**
	 * Comsumers callback methods
	 */
	public interface Callbacks {
		abstract void onStatusChanged(Status status);

	}

	public interface StatisticValues {
		abstract int getNrBikes();

		abstract int getNrTagTypes();

		abstract int getNrTags();

		abstract int getNrEvents();
	}

	private class ReadData {
		private List<Bike> bikes = new ArrayList<>();
		private List<TagType> tagTypes = new ArrayList<>();
		private List<Tag> tags = new ArrayList<>();
		private List<Event> events = new ArrayList<>();

		public List<Bike> getBikes() {
			return bikes;
		}

		public void setBikes(List<Bike> bikes) {
			this.bikes = bikes;
		}

		public List<TagType> getTagTypes() {
			return tagTypes;
		}

		public void setTagTypes(List<TagType> tagTypes) {
			this.tagTypes = tagTypes;
		}

		public List<Tag> getTags() {
			return tags;
		}

		public void setTags(List<Tag> tags) {
			this.tags = tags;
		}

		public List<Event> getEvents() {
			return events;
		}

		public void setEvents(List<Event> events) {
			this.events = events;
		}
	}


	private class ReadController {

		private final ReadData data = new ReadData();
		private boolean finishedOk = false;

		public ReadData getReadData() {
			return data;
		}

		public void read(Uri uri) throws BikeHistSyncException {
			JsonHelper jsonHelper = new JsonHelper(ImportService.this);
			SyncData syncData;
			try {
				syncData = jsonHelper.read(ImportService.this.getContentResolver().openInputStream(uri));
				data.setBikes(syncData.getBikeData().getAll());
				data.setTagTypes(syncData.getTagTypeData().getAll());
				data.setTags(syncData.getTagData().getAll());
				data.setEvents(syncData.getEventData().getAll());
				finishedOk = true;

			} catch (IOException e) {
				finishedOk = false;
				throw new BikeHistSyncException(e.getMessage());
			}


		}

		public boolean hasFinishedOk() {
			return finishedOk;
		}

	}

	private class WriteController {

		private ReadData data;
		private boolean finishedOk = false;

		public void write(ReadData data) {
			this.data = data;

			ArrayList<ContentProviderOperation> ops = deleteDatabase();
			try {
				putInternalData(ops);
			} catch (BikeHistSyncException e) {
				Log.e(TAG, e.getMessage());
				finishedOk = false;
			}
			finishedOk = true;

		}

		public boolean hasFinishedOk() {
			return finishedOk;
		}

		/**
		 * Creates batch operations to remove all entity data sets from the database. The batch has to
		 * be executed afterwards.
		 */
		private ArrayList<ContentProviderOperation> deleteDatabase() {

			ArrayList<ContentProviderOperation> ops = new ArrayList<>();

			ops.add(ContentProviderOperation.newDelete(BikeHistProvider.BikeHistContract.Tables.Event.URI)
					.build());

			ops.add(ContentProviderOperation.newDelete(BikeHistProvider.BikeHistContract.Tables.Bike.URI)
					.build());

			ops.add(ContentProviderOperation.newDelete(BikeHistProvider.BikeHistContract.Tables.Tag.URI)
					.build());

			ops.add(ContentProviderOperation.newDelete(BikeHistProvider.BikeHistContract.Tables.TagType.URI)
					.build());

			return ops;

		}

		/**
		 * Create Operations for all Entities. Write merged Data to database as batch, if executedBatch == true.
		 */
		private void putInternalData(ArrayList<ContentProviderOperation> ops) throws BikeHistSyncException {

			ops.addAll(putInternalBikes());
			ops.addAll(putInternalTagTypes());
			ops.addAll(putInternalTags());
			ops.addAll(putInternalEvents());

				executeBatch(ops);
		}

		private ArrayList<ContentProviderOperation> putInternalBikes() {

			EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(ImportService.this);

			ArrayList<ContentProviderOperation> ops = new ArrayList<>();

			for (Bike item : data.getBikes()) {
				ops.add(ContentProviderOperation.newInsert(BikeHistProvider.BikeHistContract.Tables.Bike.URI)
						.withValues(bikeUtils.build(item))
						.build());
			}

			return ops;
		}

		private ArrayList<ContentProviderOperation> putInternalTagTypes() throws BikeHistSyncException {

			EntityUtils<TagType> tagTypeUtils = EntityUtilsFactory.createTagTypeUtils(ImportService.this);

			ArrayList<ContentProviderOperation> ops = new ArrayList<>();

			for (TagType item : data.getTagTypes()) {
				ops.add(ContentProviderOperation.newInsert(BikeHistProvider.BikeHistContract.Tables.TagType.URI)
						.withValues(tagTypeUtils.build(item))
						.build());
			}

			return ops;
		}

		private ArrayList<ContentProviderOperation> putInternalTags() throws BikeHistSyncException {

			EntityUtils<Tag> tagUtils = EntityUtilsFactory.createTagUtils(ImportService.this);

			ArrayList<ContentProviderOperation> ops = new ArrayList<>();

			for (Tag item : data.getTags()) {
				ops.add(ContentProviderOperation.newInsert(BikeHistProvider.BikeHistContract.Tables.Tag.URI)
						.withValues(tagUtils.build(item))
						.build());
			}

			return ops;
		}

		private ArrayList<ContentProviderOperation> putInternalEvents() throws BikeHistSyncException {

			EntityUtils<Event> eventUtils = EntityUtilsFactory.createEventUtils(ImportService.this);

			ArrayList<ContentProviderOperation> ops = new ArrayList<>();

			for (Event item : data.getEvents()) {
				ops.add(ContentProviderOperation.newInsert(BikeHistProvider.BikeHistContract.Tables.Event.URI)
						.withValues(eventUtils.build(item))
						.build());
			}

			return ops;
		}

		private void executeBatch(ArrayList<ContentProviderOperation> ops) throws BikeHistSyncException {
			try {
				getContentResolver().applyBatch(BikeHistProvider.BikeHistContract.AUTHORITY, ops);//
			} catch (OperationApplicationException | RemoteException e) {
				throw new BikeHistSyncException(e.getMessage());
			}
		}
	}


	private class ReadTask extends AsyncTask<Uri, Void, ReadController> {

		@Override
		protected void onPostExecute(ReadController controller) {
			onReadDone(controller);
		}

		@Override
		protected ReadController doInBackground(Uri... uris) {
			ReadController controller = new ReadController();
			try {
				controller.read(uris[0]);
			} catch (BikeHistSyncException e) {
				Log.e(ImportService.TAG, e.getMessage());
				return null;
			}
			return controller;
		}

		@Override
		protected void onPreExecute() {
			setStatus(ImportService.Status.READING);
		}
	}


	private class WriteTask extends AsyncTask<ReadData, Void, WriteController> {

		@Override
		protected void onPostExecute(WriteController controller) {
			onWriteDone(controller);
		}

		@Override
		protected WriteController doInBackground(ReadData... data) {
			WriteController controller = new WriteController();
			controller.write(data[0]);
			return controller;
		}

		@Override
		protected void onPreExecute() {
			setStatus(ImportService.Status.WRITING);
		}
	}

	private class Statistic implements StatisticValues {
		private int nrBikes = 0;
		private int nrTagTypes = 0;
		private int nrTags = 0;
		private int nrEvents = 0;

		@Override
		public int getNrBikes() {
			return nrBikes;
		}

		public void setNrBikes(int nrBikes) {
			this.nrBikes = nrBikes;
		}

		@Override
		public int getNrTagTypes() {
			return nrTagTypes;
		}

		public void setNrTagTypes(int nrTagTypes) {
			this.nrTagTypes = nrTagTypes;
		}

		@Override
		public int getNrTags() {
			return nrTags;
		}

		public void setNrTags(int nrTags) {
			this.nrTags = nrTags;
		}

		@Override
		public int getNrEvents() {
			return nrEvents;
		}

		public void setNrEvents(int nrEvents) {
			this.nrEvents = nrEvents;
		}
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public ImportManager getService() {
			// Return this instance of LocalService so clients can call public methods
			return importManager;
		}
	}
}
