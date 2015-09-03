package de.egh.bikehist.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;

/**
 * Convenience access to all data to be synchronized, including meta data
 */
public class SyncData {

	/**
	 * Create date of the sync data. Is needed for recognizing deleted data of the foreign source.
	 */
	private long timestamp;

	private final SyncEntity<Bike> bikeData = new SyncEntity<>();
	private final SyncEntity<Tag> tagData = new SyncEntity<>();
	private final SyncEntity<TagType> tagTypeData = new SyncEntity<>();
	private final SyncEntity<Event> eventData = new SyncEntity<>();

	public long getTimestamp() {
		return timestamp;
	}

	/** Access to Bike's synchronization object.*/
	public SyncEntity<Bike> getBikeData(){
		return bikeData;
	}

	/** Access to Tag's synchronization object.*/
	public SyncEntity<Tag> getTagData(){
		return tagData;
	}

	/** Access to TagType's synchronization object.*/
	public SyncEntity<TagType> getTagTypeData(){
		return tagTypeData;
	}

	/** Access to Event's synchronization object.*/
	public SyncEntity<Event> getEventData(){
		return eventData;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}


}
