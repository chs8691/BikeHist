package de.egh.bikehist.sync;

import java.io.UnsupportedEncodingException;

/**
 * Interface to an external synchronization source, for instance a file.
 */
interface ExternalSyncSource {


	/**
	 * Step 1: Prepare source for data access, e.g. protect external data for being modified.
	 *
	 * @throws BikeHistSyncException Step failed
	 */
	public abstract void prepare() throws BikeHistSyncException;

	/**
	 * Step 2: Get all data from external source. Precondition: Step 1 was successful executed.
	 *
	 * @return SyncData External data of null, if access failed
	 */
	public abstract SyncData getData();

	/**
	 * Step 3: Replaces existing data with new data set in the persistance unit (e.g. sync file).
	 * The external source data should be replaced completely by this data.
	 * @throws BikeHistSyncException Step failed
	 */
	public abstract void putData(EntityContainer data)throws BikeHistSyncException;



}
