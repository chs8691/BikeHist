package de.egh.bikehist.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;


/**
 * Synchronize internal data with an external data source
 * TODO Sync Tags: Testen, ob abhängig gelöschte TagTypes sauber gesynct werden (je ex./intern)
 */
public class SyncController {


	private static final String TAG = SyncController.class.getSimpleName();
	/**
	 * Import has two steps, step one creates content provider operations.
	 */
	private ArrayList<ContentProviderOperation> importOps;
	private Statistic statistic;
	private final ExternalSyncSource externalSource;
	private final Context context;
	private SyncData internalData;
	private SyncData externalData;
	private MergeData mergeData;
	public SyncController(Context context, ExternalSyncSource externalSource) {
		this.context = context;
		this.externalSource = externalSource;
	}

	/**
	 * Writes statistic new.
	 */
	private static Statistic writeStatisticFromMerge(MergeData data) {

		Statistic statistic = new Statistic();

		for (MergeItem item : data.getBikeItems()) {
			statistic.bike.total++;
			if (item.getResult().isDiffer())
				statistic.bike.touched++;
		}
		for (MergeItem item : data.getTagItems()) {
			statistic.tag.total++;
			if (item.getResult().isDiffer())
				statistic.tag.touched++;
		}
		for (MergeItem item : data.getTagTypeItems()) {
			statistic.tagType.total++;
			if (item.getResult().isDiffer())
				statistic.tagType.touched++;
		}
		for (MergeItem item : data.getEventItems()) {
			statistic.event.total++;
			if (item.getResult().isDiffer())
				statistic.event.touched++;
		}

		return statistic;
	}

	private static Statistic writeStatisticFromEntityContainer(EntityContainer entityContainer) {

		Statistic statistic = new Statistic();

		statistic.bike.total = entityContainer.getBikes().size();
		statistic.tagType.total = entityContainer.getTagTypes().size();
		statistic.tag.total = entityContainer.getTags().size();
		statistic.event.total = entityContainer.getEvents().size();


		return statistic;
	}

	public StatisticReport getStatisticReport() {

		return new StatisticReport(statistic);
	}

	/**
	 * Synchronize data completely.
	 *
	 * @throws BikeHistSyncException Process failed
	 */
	public void runSync(SyncNotification syncNotification) throws BikeHistSyncException {

		Log.v(TAG, "Now = " + System.currentTimeMillis());
		int max = 6;
		int progress = 0;

		mergeData = new MergeData();

		syncNotification.update("Reading", max, ++progress);
		// Open remote source
		externalSource.prepare();

		// Get all remote data
		externalData = externalSource.getData();
		Log.v(TAG, "Imported nr. of Bikes=" + externalData.getBikeData().getSize());
		Log.v(TAG, "Imported nr. of TagTypes=" + externalData.getTagTypeData().getSize());
		Log.v(TAG, "Imported nr. of Tags=" + externalData.getTagData().getSize());
		Log.v(TAG, "Imported nr. of Events=" + externalData.getEventData().getSize());

		internalData = getInternalData();

		syncNotification.update("Merge bikes", max, ++progress);
		//Merge Bikes into internalData.Bikes (but no removing)
		mergeBikes();

		syncNotification.update("Merging tag types", max, ++progress);
		mergeTagTypes();

		syncNotification.update("Merging tags", max, ++progress);
		mergeTags();

		syncNotification.update("Merging Events", max, ++progress);
		mergeEvents();

		syncNotification.update("Saving", max, ++progress);

		// This is the new state to be written
		externalSource.putData(mergeData);

		//Save all to database
		putInternalData(new ArrayList<ContentProviderOperation>(), true);

		statistic = writeStatisticFromMerge(mergeData);

		syncNotification.finish(
				statistic.bike.total + "/" + statistic.bike.touched + " Bikes, " +
						statistic.tagType.total + "/" + statistic.tagType.touched + " TagTypes, ",
				statistic.tag.total + "/" + statistic.tag.touched + " Tags, " +
						statistic.event.total + "/" + statistic.event.touched + " Events " +
						" (total/touched).");

		//Statistic
		Log.v(TAG, "Bikes total/touched:" + statistic.bike.total + "/" + statistic.bike.touched);
		Log.v(TAG, "TagTypes total/touched:" + statistic.tagType.total + "/" + statistic.tagType.touched);
		Log.v(TAG, "Tags total/touched:" + statistic.tag.total + "/" + statistic.tag.touched);
		Log.v(TAG, "Events total/touched:" + statistic.event.total + "/" + statistic.event.touched);


	}

	/**
	 * Import data but don't commit changes.
	 *
	 * @throws BikeHistSyncException Process failed
	 */
	public void runImportRead() throws BikeHistSyncException {

		Log.v(TAG, "Now = " + System.currentTimeMillis());

		mergeData = new MergeData();

		// Open remote source
		externalSource.prepare();

		// Get all remote data
		externalData = externalSource.getData();
		Log.v(TAG, "Imported nr. of Bikes=" + externalData.getBikeData().getSize());
		Log.v(TAG, "Imported nr. of TagTypes=" + externalData.getTagTypeData().getSize());
		Log.v(TAG, "Imported nr. of Tags=" + externalData.getTagData().getSize());
		Log.v(TAG, "Imported nr. of Events=" + externalData.getEventData().getSize());

		createMergeDataForImport();

		//Create batch operations and save them in importOps|
		importOps = deleteDatabase();
		putInternalData(importOps, false);


		statistic = writeStatisticFromMerge(mergeData);


		//Statistic
		Log.v(TAG, "Bikes total/touched:" + statistic.bike.total + "/" + statistic.bike.touched);
		Log.v(TAG, "TagTypes total/touched:" + statistic.tagType.total + "/" + statistic.tagType.touched);
		Log.v(TAG, "Tags total/touched:" + statistic.tag.total + "/" + statistic.tag.touched);
		Log.v(TAG, "Events total/touched:" + statistic.event.total + "/" + statistic.event.touched);

	}

	/**
	 * Creates a MergeData item with operation 'insert' for every external entity item.
	 */
	private void createMergeDataForImport() {

		for (Bike bike : externalData.getBikeData().getAll()) {
			if (!bike.isDeleted())
				mergeData.addBike(new MergeItem<>(bike, externalData.getTimestamp()));
		}

		for (TagType tagType : externalData.getTagTypeData().getAll()) {
			if (!tagType.isDeleted())
				mergeData.addTagType(new MergeItem<>(tagType, externalData.getTimestamp()));
		}

		for (Tag tag : externalData.getTagData().getAll()) {
			if (!tag.isDeleted())
				mergeData.addTag(new MergeItem<>(tag, externalData.getTimestamp()));
		}

		for (Event event : externalData.getEventData().getAll()) {
			if (!event.isDeleted())
				mergeData.addEvent(new MergeItem<>(event, externalData.getTimestamp()));
		}


	}

	public void runImportWrite() throws BikeHistSyncException {
		executeBatch(importOps);

	}

	private void executeBatch(ArrayList<ContentProviderOperation> ops) throws BikeHistSyncException {
		try {
			context.getContentResolver().applyBatch(BikeHistProvider.BikeHistContract.AUTHORITY, ops);//
		} catch (OperationApplicationException | RemoteException e) {
			throw new BikeHistSyncException(e.getMessage());
		}
	}


	/**
	 * Synchronize data completely.
	 *
	 * @throws BikeHistSyncException Process failed
	 */
	public void runExport(SyncNotification syncNotification) throws BikeHistSyncException {

		Log.v(TAG, "Now = " + System.currentTimeMillis());
		int max = 2;
		int progress = 0;

		mergeData = new MergeData();

		syncNotification.update("Reading", max, ++progress);
		// Open remote source
		externalSource.prepare();

		internalData = getInternalData();

		syncNotification.update("Saving", max, ++progress);

		// This is the new state to be written
		externalSource.putData(getEntityContainer(internalData));

		statistic = writeStatisticFromEntityContainer(getEntityContainer(internalData));

		syncNotification.finish(
				"Exported " + statistic.bike.total + " Bikes, " +
						statistic.tagType.total + " TagTypes, " +
						statistic.tag.total + " Tags, " +
						statistic.event.total + " Events."
				, "");

		//Statistic
		Log.v(TAG, "Bikes total:" + statistic.bike.total);
		Log.v(TAG, "TagTypes total:" + statistic.tagType.total);
		Log.v(TAG, "Tags total:" + statistic.tag.total);
		Log.v(TAG, "Events total:" + statistic.event.total);


	}

	/**
	 * Take entity lists from SyncData to EntityContainer. Uses References.
	 */
	private EntityContainer getEntityContainer(final SyncData data) {

		return new EntityContainer() {
			@Override
			public List<Bike> getBikes() {
				return data.getBikeData().getAll();
			}

			@Override
			public List<TagType> getTagTypes() {
				return data.getTagTypeData().getAll();
			}

			@Override
			public List<Tag> getTags() {
				return data.getTagData().getAll();
			}

			@Override
			public List<Event> getEvents() {
				return data.getEventData().getAll();
			}
		};
	}


	/**
	 * Create Operations for all Entities. Write merged Data to database as batch, if executedBatch == true.
	 */
	private void putInternalData(ArrayList<ContentProviderOperation> ops, boolean executeBatch) throws BikeHistSyncException {

		ops.addAll(putInternalBikes());
		ops.addAll(putInternalTagTypes());
		ops.addAll(putInternalTags());
		ops.addAll(putInternalEvents());

		if (executeBatch)
			executeBatch(ops);
	}

	/**
	 * Creates batch operations to remove all entity data sets from the database. The batch has to
	 * be executed afterwards.
	 */
	private ArrayList<ContentProviderOperation> deleteDatabase() {

		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		ops.add(ContentProviderOperation.newDelete(Tables.Event.URI)
				.build());

		ops.add(ContentProviderOperation.newDelete(Tables.Bike.URI)
				.build());

		ops.add(ContentProviderOperation.newDelete(Tables.Tag.URI)
				.build());

		ops.add(ContentProviderOperation.newDelete(Tables.TagType.URI)
				.build());

		return ops;

	}

	private ArrayList<ContentProviderOperation> putInternalTagTypes() throws BikeHistSyncException {

		EntityUtils<TagType> tagTypeUtils = EntityUtilsFactory.createTagTypeUtils(context);

		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		for (MergeItem<TagType> item : mergeData.getTagTypeItems()) {
			switch (item.getResult().getInternalOperation()) {

				case CREATE:
					ops.add(ContentProviderOperation.newInsert(Tables.TagType.URI)
							.withValues(tagTypeUtils.build(item.getMergedEntity()))
							.build());
					break;

				case UPDATE:
					ops.add(ContentProviderOperation.newUpdate(Tables.TagType.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.withValues(tagTypeUtils.build(item.getMergedEntity()))
							.build());
					break;

				case DELETE:
					ops.add(ContentProviderOperation.newDelete(Tables.TagType.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.build());
					break;

				case NULL:
					//Nothing to do
					break;

				default:
					throw new BikeHistSyncException("Unknown OperationType " + item.getResult().getInternalOperation());
			}

		}

		return ops;

	}

	private ArrayList<ContentProviderOperation> putInternalTags() throws BikeHistSyncException {

		EntityUtils<Tag> tagUtils = EntityUtilsFactory.createTagUtils(context);

		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		for (MergeItem<Tag> item : mergeData.getTagItems()) {
			switch (item.getResult().getInternalOperation()) {
				case CREATE:
					ops.add(ContentProviderOperation.newInsert(Tables.Tag.URI)
							.withValues(tagUtils.build(item.getMergedEntity()))
							.build());
					break;

				case UPDATE:
					ops.add(ContentProviderOperation.newUpdate(Tables.Tag.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.withValues(tagUtils.build(item.getMergedEntity()))
							.build());
					break;

				case DELETE:
					ops.add(ContentProviderOperation.newDelete(Tables.Tag.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.build());
					break;
				case NULL:
					//Nothing to do
					break;

				default:
					throw new BikeHistSyncException("Unknown OperationType " + item.getResult().getInternalOperation());
			}
		}

		return ops;
	}

	private ArrayList<ContentProviderOperation> putInternalEvents() throws BikeHistSyncException {

		EntityUtils<Event> eventUtils = EntityUtilsFactory.createEventUtils(context);

		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		for (MergeItem<Event> item : mergeData.getEventItems()) {
			switch (item.getResult().getInternalOperation()) {
				case CREATE:
					ops.add(ContentProviderOperation.newInsert(Tables.Event.URI)
							.withValues(eventUtils.build(item.getMergedEntity()))
							.build());
					break;

				case UPDATE:
					ops.add(ContentProviderOperation.newUpdate(Tables.Event.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.withValues(eventUtils.build(item.getMergedEntity()))
							.build());
					break;

				case DELETE:
					ops.add(ContentProviderOperation.newDelete(Tables.Event.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.build());
					break;
				case NULL:
					//Nothing to do
					break;

				default:
					throw new BikeHistSyncException("Unknown OperationType " + item.getResult().getInternalOperation());
			}
		}

		return ops;
	}

	private ArrayList<ContentProviderOperation> putInternalBikes() throws BikeHistSyncException {

		EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(context);

		ArrayList<ContentProviderOperation> ops = new ArrayList<>();

		for (MergeItem<Bike> item : mergeData.getBikeItems()) {
			switch (item.getResult().getInternalOperation()) {
				case CREATE:
					ops.add(ContentProviderOperation.newInsert(Tables.Bike.URI)
							.withValues(bikeUtils.build(item.getMergedEntity()))
							.build());
					break;

				case UPDATE:
					ops.add(ContentProviderOperation.newUpdate(Tables.Bike.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.withValues(bikeUtils.build(item.getMergedEntity()))
							.build());
					break;

				case DELETE:
					ops.add(ContentProviderOperation.newDelete(Tables.Bike.URI)
							.withSelection(Tables.BikeHistEntity.Id.NAME + "=?", new String[]{item.getId().toString()})
							.build());
					break;
				case NULL:
					//Nothing to do
					break;
				default:
					throw new BikeHistSyncException("Unknown OperationType " + item.getResult().getInternalOperation());
			}
		}

		return ops;
	}

	/**
	 * Merges TagTypes: external into internal data set. Removed TagTypes will not handled here,
	 * This method has to be called as first merge.. method.
	 * Writes statistic for tagTypes.
	 */
	private void mergeTagTypes() {

		//--- Step 1: Create item for every pair of entities---//
		//Create item for every entity pair
		for (TagType inTagType : internalData.getTagTypeData().getAll()) {

			mergeData.addTagType(new MergeItem<>(
					externalData.getTagTypeData().getById(inTagType.getId()),
					externalData.getTimestamp(),
					inTagType, internalData.getTimestamp()));
		}

		//Add entities, that only exist external
		for (TagType exTagType : externalData.getTagTypeData().getAll()) {
			if (internalData.getTagTypeData().getById(exTagType.getId()) == null) {

				mergeData.addTagType(new MergeItem<>(
						exTagType,
						externalData.getTimestamp(),
						internalData.getTagTypeData().getById(exTagType.getId()),
						internalData.getTimestamp()));
			}
		}

	}


	/**
	 * Merges Tags: external into internal data set. Removed Tags will not handled here,
	 * TagTypes have to merged before, so Tags with dead TagType links can be ignored.
	 * For every touched Entity, an Op-Item will be created.
	 * Writes statistic for tags.
	 */
	private void mergeTags() {

		//--- Step 1: Create item for every pair of entities---//
		//Create item for every entity pair
		for (Tag inTag : internalData.getTagData().getAll()) {

			mergeData.addTag(new MergeItem<>(
					externalData.getTagData().getById(inTag.getId()),
					externalData.getTimestamp(),
					inTag, internalData.getTimestamp()));
		}

		//Add entities, that only exist external
		for (Tag exTag : externalData.getTagData().getAll()) {
			if (internalData.getTagData().getById(exTag.getId()) == null) {

				mergeData.addTag(new MergeItem<>(
						exTag,
						externalData.getTimestamp(),
						internalData.getTagData().getById(exTag.getId()),
						internalData.getTimestamp()));
			}
		}

		//Check dependencies: TagType and find invalid ones (deleted TagTypes)
		for (MergeItem<Tag> item : mergeData.getTagList()) {
			MergeItem<TagType> tagTypeItem = mergeData.getTagTypeItem(item.getMergedEntity().getTagTypeId());
			if (tagTypeItem == null || tagTypeItem.getMergedEntity().isDeleted()) {
				if (item.setDeleted())
					Log.v(TAG, "Delete Tag with invalid TagType-ID: " + item.getMergedEntity().getName());
			}
		}
	}

	/**
	 * Merges Events: external into internal data set. Removed Events will not handled here,
	 * Tags  and Bikes have to be merged before, so Events with dead foreign IDs links can be ignored.
	 * For every touched Entity, an Op-Item will be created.
	 * Writes statistic for events.
	 */
	private void mergeEvents() {

		//--- Step 1: Create item for every pair of entities---//
		//Create item for every entity pair
		for (Event inEvent : internalData.getEventData().getAll()) {

			mergeData.addEvent(new MergeItem<>(
					externalData.getEventData().getById(inEvent.getId()),
					externalData.getTimestamp(),
					inEvent, internalData.getTimestamp()));
		}

		//Add entities, that only exist external
		for (Event exEvent : externalData.getEventData().getAll()) {
			if (internalData.getEventData().getById(exEvent.getId()) == null) {

				mergeData.addEvent(new MergeItem<>(
						exEvent,
						externalData.getTimestamp(),
						internalData.getEventData().getById(exEvent.getId()),
						internalData.getTimestamp()));
			}
		}

		//Check dependencies: Bike, Tag and find invalid ones (deleted Events)
		for (MergeItem<Event> item : mergeData.getEventList()) {
			MergeItem<Tag> tagItem = mergeData.getTagItem(item.getMergedEntity().getTagId());
			if (tagItem == null || tagItem.getMergedEntity().isDeleted()) {
				if (item.setDeleted())
					Log.v(TAG, "Delete Event with invalid TagType-ID: " + item.getMergedEntity().getName());
			} else {
				MergeItem<Bike> bikeItem = mergeData.getBikeItem(item.getMergedEntity().getBikeId());
				if (bikeItem == null || bikeItem.getMergedEntity().isDeleted()) {
					if (item.setDeleted())
						Log.v(TAG, "Delete Event with invalid BikeType-ID: " + item.getMergedEntity().getName());
				}
			}
		}
	}

	/**
	 * Checks a Tag, if it is valid for merging process. A Tag can only be invalid, if it
	 * exists and is not deleted.
	 *
	 * @param tagType Linked TagType, can be null
	 * @param tag     Tag to be examined
	 * @return true, if Tag can be used for merge (includes tag is null); false if Tag has to be ignored.
	 */
	private boolean checkTagForProcessing(Tag tag, TagType tagType) {

		if (tag == null)
			return true;

		if (tag.isDeleted())
			return true;

		if (tagType == null)
			return false;

		return !tagType.isDeleted();

	}

	/**
	 * Merges Bikes: external into internal data set. Removed Bikes will not handled here,
	 * Events have to be merged before.
	 * This method has to be called as first merge.. method.
	 * Writes statistic for bikes.
	 */
	private void mergeBikes() {

		//--- Step 1: Create item for every pair of entities---//
		//Create item for every entity pair
		for (Bike inBike : internalData.getBikeData().getAll()) {

			mergeData.addBike(new MergeItem<>(
					externalData.getBikeData().getById(inBike.getId()),
					externalData.getTimestamp(),
					inBike, internalData.getTimestamp()));
		}

		//Add entities, that only exist external
		for (Bike exBike : externalData.getBikeData().getAll()) {
			if (internalData.getBikeData().getById(exBike.getId()) == null) {

				mergeData.addBike(new MergeItem<>(
						exBike,
						externalData.getTimestamp(),
						internalData.getBikeData().getById(exBike.getId()),
						internalData.getTimestamp()));
			}
		}

	}


	/**
	 * Read data from database
	 */
	private SyncData getInternalData() {
		SyncData syncData = new SyncData();
		EntityLoader el = new EntityLoader(context);

		syncData.setTimestamp(System.currentTimeMillis());
		syncData.getBikeData().add(el.getAllBikes());
		syncData.getTagTypeData().add(el.getAllTagTypes());
		syncData.getTagData().add(el.getAllTags());
		syncData.getEventData().add(el.getAllEvents());

		return syncData;
	}

	/**
	 * Structure for a particular Entity
	 */
	private static class EntityStatistic {
		/**
		 * Total number of Entities
		 */
		int total = 0;

		/**
		 * Number of touched entities
		 */
		int touched = 0;
	}

	/**
	 * Statistic values for synchronizing.
	 */
	private static class Statistic {
		final EntityStatistic bike = new EntityStatistic();
		final EntityStatistic tagType = new EntityStatistic();
		final EntityStatistic tag = new EntityStatistic();
		final EntityStatistic event = new EntityStatistic();
	}

	/**
	 * For external usage. A wrapper of the Statistic, because Statistic has no proper interface.
	 */
	public class StatisticReport implements Serializable {

		private final Statistic statistic;

		StatisticReport(Statistic statistic) {
			this.statistic = statistic;
		}

		public int getBikesTotal() {
			return statistic.bike.total;
		}

		public int getTagTypesTotal() {
			return statistic.tagType.total;
		}

		public int getTagsTotal() {
			return statistic.tag.total;
		}

		public int getEventsTotal() {
			return statistic.event.total;
		}
	}


}
