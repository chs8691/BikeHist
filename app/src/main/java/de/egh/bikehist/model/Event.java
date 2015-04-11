package de.egh.bikehist.model;

import java.util.UUID;

/** Functional class: Event for an particular bike. Distance in mm. */
public class Event implements ModelType {
	private UUID id;
	private long timestamp;
	private UUID bikeId;
	private UUID tagId;
	private GeoLocation geoLocation;
	private String name;
	private long distance;

	/** Distance to previous Event of same TagType. */
	private long diffDistance;
	/** Timestamp to previous Event of same TagType. */
	private long diffTimestamp;

	/**
	 @param id
	 UUID for the event
	 @param name
	 String
	 @param distance
	 Distance in Meter
	 @param bikeId
	 UUID for the foreign key bikeID
	 @param tagId
	 UUID for the foreign key tagID
	 @param geoLocation
	 Optional
	 @param timestamp
	 Long with the timestamp of the event
	 */
	public Event(UUID id,
	             String name,
	             long distance,
	             UUID bikeId,
	             UUID tagId,
	             GeoLocation geoLocation,
	             long timestamp,
	             long diffDistance,
	             long diffTimestamp
	) {

		this.id = id;
		this.distance = distance;
		this.bikeId = bikeId;
		this.tagId = tagId;
		this.name = name;
		this.timestamp = timestamp;
		this.diffDistance = diffDistance;
		this.diffTimestamp = diffTimestamp;
		this.geoLocation = geoLocation != null ? geoLocation : new GeoLocation(0, 0, 0);
	}

	public long getDiffTimestamp() {
		return diffTimestamp;
	}

	public long getDiffDistance() {
		return diffDistance;
	}

	public long getDistance() {
		return distance;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public UUID getBikeId() {
		return bikeId;
	}

	public UUID getTagId() {
		return tagId;
	}

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	@Override
	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
