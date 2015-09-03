package de.egh.bikehist.model;

import java.util.UUID;

/**
 * Functional class: Type  of tags.
 */
public class TagType implements ModelType {
	private final UUID id;
	private String name;
	private boolean deleted;
	private long touchedAt;

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public long getTouchedAt() {
		return touchedAt;
	}

	@Override
	public void setTouchedAt(long touchedAt) {
		this.touchedAt = touchedAt;
	}

	/**
	 * Avoid two different TagTypes with same name
	 *
	 * @param id   UUID of the dataset
	 * @param name String with the name
	 */
	public TagType(UUID id, String name, boolean deleted, long touchedAt) {
		this.id = id;
		this.name = name;
		this.deleted = deleted;
		this.touchedAt = touchedAt;
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
		this.name = name;
	}


}
