package de.egh.bikehist.sync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.model.Bike;

/**
 * Convenience access to all data to be synchronized, including meta data
 */
 class SyncData {

	/**Create date of the sync data. Is needed for recognizing deleted data of the foreign source.*/
    public long CreatedTimestamp;

	private Map<UUID,Bike> bikes = new HashMap<>();



}
