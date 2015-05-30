package de.egh.bikehist.model;

import java.util.UUID;

/**
 * Functional class; Bike.
 */
public class Bike implements ModelType {
	private final UUID id;
	private String frameNumber;
	private String name;
	private boolean deleted;
	private long touchedAt;

	/**
	 * Creates a Bike instance
	 *
	 * @param id          UUID for this dataset
	 * @param name        String with name
	 * @param frameNumber Unique EVENT_ID of the bike
	 */
	public Bike(UUID id, String name, String frameNumber, boolean deleted, long touchedAt) {
		this.name = name;
		this.frameNumber = frameNumber;
		this.id = id;
		this.deleted = deleted;
		this.touchedAt = touchedAt;
	}

	public long getTouchedAt() {
		return touchedAt;
	}

	public void setTouchedAt(long touchedAt) {
		this.touchedAt = touchedAt;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getFrameNumber() {
		return frameNumber;
	}

	public void setFrameNumber(String frameNumber) {
		this.frameNumber = frameNumber == null ? "" : frameNumber;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}
}
