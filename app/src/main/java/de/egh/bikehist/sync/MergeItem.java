package de.egh.bikehist.sync;

import java.util.UUID;

import de.egh.bikehist.model.ModelType;

/**
 * A dataset for one Entity. Holds input data (both Entities) and a result (Merged Entity,
 * database operation and compare result flag.
 * Include functional logic that is equal for all entity types.
 */
public class MergeItem<T extends ModelType> {

	private final Data externalData;
	private final Data internalData;
	private T mergedEntity;
	private Result result;


	/**
	 * Timestamp will only be used, if Entity is null. Entities may be changed.
	 *
	 * @param externalEntity Reference to destination entity.
	 * @param internalEntity Reference to destination entity. Attributes will be changed.
	 */
	public MergeItem(T externalEntity, long externalSyncTimestamp,
	                 T internalEntity, long internalSyncTimestamp) {

		this.externalData = new Data(externalEntity, externalSyncTimestamp);
		this.internalData = new Data(internalEntity, internalSyncTimestamp);

		//--- Create default result  ---//

		// New from external
		if (!internalData.exist()) {
			result = new Result(true, InternalOperation.CREATE);
			mergedEntity = externalData.getEntity();
			return;
		}

		// External deleted
		if (!externalData.exist()) {
			mergedEntity = internalData.getEntity();
			if (internalData.getEntity().isDeleted()) {
				result = new Result(true, InternalOperation.DELETE);
				mergedEntity.setDeleted(true);
				mergedEntity.setTouchedAt(System.currentTimeMillis());
			} else {
				if (externalData.getSyncTimestamp() < internalData.getEntity().getTouchedAt()) {
					result = new Result(true, InternalOperation.NULL);
				} else {
					result = new Result(true, InternalOperation.DELETE);
					mergedEntity.setDeleted(true);
					mergedEntity.setTouchedAt(System.currentTimeMillis());
				}
			}
			return;
		}

		// Update from external
		if (externalData.getEntity().getTouchedAt() > internalData.getEntity().getTouchedAt()) {
			result = new Result(true, InternalOperation.UPDATE);
			mergedEntity = externalData.getEntity();
			return;
		}

		// Internal newer, but external changed, too
		if (internalData.getEntity().getTouchedAt() > externalData.getEntity().getTouchedAt()) {
			if (internalData.getEntity().isDeleted()) {
				result = new Result(true, InternalOperation.DELETE);
			} else {
				result = new Result(true, InternalOperation.NULL);
			}
			mergedEntity = internalData.getEntity();
			return;
		}

		// Default: Both are equal
		result = new Result(false, InternalOperation.NULL);
		mergedEntity = internalData.getEntity();

	}


	/**
	 * Timestamp will only be used, if Entity is null. Entities may be changed.
	 *
	 * @param externalEntity Reference to destination entity.
	 */
	public MergeItem(T externalEntity, long externalSyncTimestamp) {

		this.externalData = new Data(externalEntity, externalSyncTimestamp);
		this.internalData = null;

		// New from external
		result = new Result(true, InternalOperation.CREATE);
		mergedEntity = externalData.getEntity();
	}

	public Result getResult() {
		return result;
	}

	/**
	 * Returns modified reference to the original entity instance.
	 */
	public T getMergedEntity() {
		return mergedEntity;
	}

	/**
	 * Returns the ID of the entity.
	 */
	public UUID getId() {
		return mergedEntity.getId();
	}

	/**
	 * Modifies the entity by setting deleted flag. Does nothing, if flag is already set.
	 * If internal data doesn't exists, a NULL-operation will be created, otherwise a DELETE one.
	 *
	 * @return true, if item touched, otherwise false (for instance, TagType is alreaded deleted==true)
	 */
	public boolean setDeleted() {

		if (mergedEntity.isDeleted())
			return false;

		if (internalData.getEntity() == null) {
			result = new Result(false, InternalOperation.NULL);
		} else {
			result = new Result(false, InternalOperation.DELETE);
			mergedEntity = internalData.getEntity();
		}

		mergedEntity.setDeleted(true);
		mergedEntity.setTouchedAt(System.currentTimeMillis());
		return true;
	}

	/**
	 * Database operation
	 */
	public enum InternalOperation {
		NULL, CREATE, UPDATE, DELETE;
	}

	/**
	 * Input data
	 */
	public class Data {
		/**
		 * Can be null, if Input
		 */
		private final T entity;

		/**
		 * Touched at syncTimestamp of the Entity
		 */
		private final long syncTimestamp;

		public Data(T entity, long timestamp) {
			this.entity = entity;
			this.syncTimestamp = timestamp;
		}

		public T getEntity() {
			return entity;
		}

		/**
		 * Touched syncTimestamp. Use for comparing.
		 */
		public long getSyncTimestamp() {
			return syncTimestamp;
		}

		/**
		 * True, if entity exists.
		 */
		public boolean exist() {
			return entity != null;
		}
	}

	/**
	 * Result set of the merge
	 */
	public class Result {
		/**
		 * True, if input data differ (in a functional way) from each other and have to be merged.
		 * For instance external data is null and internal data is deleted.
		 */
		private final boolean differ;

		/**
		 * Database operation
		 */
		private InternalOperation internalOperation;

		public Result(boolean differ, InternalOperation internalOperation) {
			this.differ = differ;
			this.internalOperation = internalOperation;
		}

		/**
		 * True, if entities differ in a functional way.
		 */
		public boolean isDiffer() {
			return differ;
		}

		public InternalOperation getInternalOperation() {
			return internalOperation;
		}

		/**
		 * Only use this to override calculated op.
		 */
		public void setInternalOperation(InternalOperation internalOperation) {
			this.internalOperation = internalOperation;
		}
	}


}
