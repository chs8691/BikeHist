package de.egh.bikehist.model;

import java.util.UUID;

/**
 * Functional class: Event for an particular bike. Distance in mm.
 */
public class Event implements ModelType {
	private final UUID id;
	private final UUID bikeId;
	private long timestamp;
	private UUID tagId;
	private GeoLocation geoLocation;
	private String name;
	private long distance;
	/**
	 * Distance to previous Event of same TagType.
	 */
	private long diffDistance;
	/**
	 * Timestamp to previous Event of same TagType.
	 */
	private long diffTimestamp;
	private boolean deleted;
	private long touchedAt;

	/**
	 * @param id            UUID for the event
	 * @param name          String
	 * @param distance      Distance in Meter
	 * @param bikeId        UUID for the foreign key bikeID
	 * @param tagId         UUID for the foreign key tagID
	 * @param geoLocation   Optional
	 * @param timestamp     Long with the timestamp of the event
	 * @param diffDistance  Long
	 * @param diffTimestamp Long
	 * @param deleted       boolean
	 * @param touchedAt     long
	 */
	public Event(UUID id, String name, boolean deleted, long touchedAt, long distance, UUID bikeId,
	             UUID tagId, GeoLocation geoLocation, long timestamp,
	             long diffDistance, long diffTimestamp) {

		this.id = id;
		this.name = name;
		this.deleted = deleted;
		this.touchedAt = touchedAt;
		this.distance = distance;
		this.bikeId = bikeId;
		this.tagId = tagId;
		this.timestamp = timestamp;
		this.diffDistance = diffDistance;
		this.diffTimestamp = diffTimestamp;
		this.geoLocation = geoLocation != null ? geoLocation : new GeoLocation(0, 0, 0);
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public long getTouchedAt() {
		return touchedAt;
	}

	public void setTouchedAt(long touchedAt) {
		this.touchedAt = touchedAt;
	}

	public long getDiffTimestamp() {
		return diffTimestamp;
	}

	public void setDiffTimestamp(long diffTimestamp) {
		this.diffTimestamp = diffTimestamp;
	}

	public long getDiffDistance() {
		return diffDistance;
	}

	public void setDiffDistance(long diffDistance) {
		this.diffDistance = diffDistance;
	}

	public long getDistance() {
		return distance;
	}

	public void setDistance(long distance) {
		this.distance = distance;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public UUID getBikeId() {
		return bikeId;
	}

	public UUID getTagId() {
		return tagId;
	}

	public void setTagId(UUID tagId) {
		this.tagId = tagId;
	}

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	@Override
	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
