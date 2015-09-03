package de.egh.bikehist.model;

import java.util.UUID;

/** Functional class: Tag of am Entry. */
public class Tag implements ModelType{
	private final UUID id;
	private UUID tagTypeId;
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
	 A Tag has a TagType and should be unique for all TagTypes
	 @param id UUID of the dataset
	 @param name String with description
	 @param tagTypeId UUID of the TagType
	 */
	public Tag(UUID id, String name, UUID tagTypeId, boolean deleted, long touchedAt) {
		this.id = id;
		this.tagTypeId = tagTypeId;
		this.name = name;
		this.deleted = deleted;
		this.touchedAt = touchedAt;
	}

	public UUID getTagTypeId() {
		return tagTypeId;
	}


	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setTagTypeId(UUID tagTypeId) {
		this.tagTypeId = tagTypeId;
	}

	public void setName(String name) {
		this.name = name;
	}
}
