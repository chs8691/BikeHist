package de.egh.bikehist.de.egh.bikehist.model;

import java.util.UUID;

/** Functional class: Tag of am Entry. */
public class Tag implements ModelType{
	private UUID id;
	private UUID tagTypeId;
	private String name;

	/**
	 A Tag has a TagType and should be unique for all TagTypes
	 @param id UUID of the dataset
	 @param name String with description
	 @param tagTypeId UUID of the TagType
	 */
	public Tag(UUID id, String name, UUID tagTypeId) {
		this.id = id;
		this.tagTypeId = tagTypeId;
		this.name = name;
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
}
