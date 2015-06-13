package de.egh.bikehist.sync;

import android.content.Context;

/**
 * Synchronize internal data with an external data source
 */
public class SyncController {

	private ExternalSyncSource externalSource;
	private Context context;

	public SyncController(Context context, ExternalSyncSource externalSource) {
		this.context = context;
		this.externalSource = externalSource;
	}

	/** Synchronize data completely.
	 *@throws BikeHistSyncException Process failed
	 * */
	public void run() throws BikeHistSyncException{

		//Open remote source
		externalSource.prepare();

		//Get all remote data
		SyncData externalData = externalSource.getData();

		//TODO weitermachen

	}

}
