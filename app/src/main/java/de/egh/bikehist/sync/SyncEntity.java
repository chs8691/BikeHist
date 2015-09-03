package de.egh.bikehist.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.model.ModelType;

/**
 * Convenience access to all data to be synchronized, including meta data
 */
public class SyncEntity<T extends ModelType> {

	/**
	 * Entities as List
	 */
	private final List<T> entityList = new ArrayList<>();
	/**
	 * Entities as Map
	 */
	private final Map<UUID, T> entityMap = new HashMap<>();
	/**
	 * Lists entities, that have to be modified.
	 */
	private final Map<UUID, OperationType> operationMap = new HashMap<>();

	/**
	 * Returns a copy of the operationMap.
	 */
	public Map<UUID, OperationType> getOperationMap() {
		Map<UUID, OperationType> map = new HashMap<>();
		map.putAll(operationMap);

		return map;
	}

	/**
	 * Adds Entities, if not already there. Key is ID.
	 *
	 * @return Number of added entries (>= 0)
	 */
	public int add(List<T> entities) {

		int res = 0;

		for (T entity : entities) {
			if (!entityMap.containsKey(entity.getId())) {
				entityMap.put(entity.getId(), entity);
				entityList.add(entity);
				res++;
			}
		}
		return res;
	}

	/**
	 * Adds one Entity to the set of Entites and marks it with OperationType.CREATE. if not already there.
	 *
	 * @return false, if entity already exists and nothing was done. Otherwise true.
	 */
	public boolean opNew(T entity) {
		List<T> newEntities = new ArrayList<>();
		newEntities.add(entity);
		if (add(newEntities) == 1) {
			operationMap.put(entity.getId(), OperationType.NEW);
			return true;
		}
		return false;
	}

	/**
	 * Adds an existing, not deleted Entity to the set of Entites and marks it with OperationType.UPDATE.
	 * Does nothing, if entity doesn't exist.
	 *
	 * @param entity Entity to be updated, must have deleted!=true
	 * @return false, if entity doesn't exists or has deleted=true and nothing was done. Otherwise true.
	 */
	public boolean opUpdate(T entity) {

		if (entity.isDeleted())
			return false;

		if (entityMap.containsKey(entity.getId())) {
			entityMap.put(entity.getId(), entity);
			entityList.remove(entity);
			entityList.add(entity);
			operationMap.put(entity.getId(), OperationType.UPDATE);
			return true;
		}
		return false;

	}

	/**
	 * Creates operation for just counting. Has no effect for database operations, but just
	 * increase the touched counter. Nothing happens, if there is already an event for this Entity.
	 *
	 * @param entity Entity
	 * @return false, if entity already touched or if entity unknown, otherwise true.
	 */
	public boolean opCount(T entity) {

		if (!entityMap.containsKey(entity.getId()) ||operationMap.containsKey(entity.getId())) {
			return false;
		}

		operationMap.put(entity.getId(), OperationType.COUNT);
		return true;
	}


	/**
	 * Creates operation for deleting entity. Take care, that the Entity has deleted==true.
	 * Does nothing, if Entity not found.
	 *
	 * @param entity Entity, must have deleted==true
	 * @return false, if entity doesn't exists or deleted!=true and nothing was done. Otherwise true.
	 */
	public boolean opDelete(T entity) {

		if (!entity.isDeleted())
			return false;

		if (entityMap.containsKey(entity.getId())) {
			entityMap.put(entity.getId(), entity);
			entityList.remove(entity);
			entityList.add(entity);
			operationMap.put(entity.getId(), OperationType.UPDATE);
			return true;
		}
		return false;
	}

	/**
	 * Returns null, if not found.
	 */
	public T getById(UUID id) {
		return entityMap.get(id);
	}

	/**
	 * Counts the Entites.
	 */
	public int getSize() {
		return entityList.size();
	}

	/**
	 * Returns a copy of the List, but it's not a deep copy; Entities are referenced.
	 */
	public List<T> getAll() {
		List<T> entities = new ArrayList<>();

		entities.addAll(entityList);

		return entities;
	}

	/**
	 * All Entities with deleted flag.
	 * Returns a copy of the List, but it's not a deep copy; Entities are referenced.
	 */
	public List<T> getDeleted() {
		List<T> entities = new ArrayList<>();

		for (T entity : entityList) {
			if (entity.isDeleted())
				entities.add(entity);
		}

		return entities;
	}

	/**
	 * Defines operation types for an entity item. Will be used to update database and writing
	 * statistic.
	 */
	public enum OperationType {
		/**
		 * Existing Entity with deleted==true. Has to be removed from database.
		 */
		DELETED,

		/**
		 * Existing Entity with deleted!=true. Has to be updated.
		 */
		UPDATE,

		/**
		 * Non existing Entity. Has to be created in the database.
		 */
		NEW,

		/**
		 * Just count. Nothing to do on database, but external is touched
		 * (external.timestamp < internal.timestamp
		 */
		COUNT;
	}

}
