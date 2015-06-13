package de.egh.bikehist.sync;

import java.io.UnsupportedEncodingException;

/**
 * Interface to an external synchronization source, for instance a file.
 */
public interface ExternalSyncSource {


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
	public abstract SyncData getData() throws UnsupportedEncodingException;

	/**
	 * Step 3: Merged data set. The external source data should be replaced completely by this data.
	 */
	public abstract void putData(SyncData syncData);

	/**
	 * Step 4: Commits the data to the external source, for instance by writing data to files.
	 *
	 * @throws BikeHistSyncException Step failed
	 */
	public abstract void commit() throws BikeHistSyncException;


}
