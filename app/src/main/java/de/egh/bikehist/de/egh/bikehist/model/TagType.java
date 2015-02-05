package de.egh.bikehist.de.egh.bikehist.model;

import java.util.UUID;

/** Functional class: Type  of tags. */
public class TagType implements ModelType {
	private UUID id;


	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	private String name;

	/**
	 Avoid two different TagTypes with same name
	 @param id UUID of the dataset
	 @param name String with the name
	 */
	public TagType(UUID id, String name) {
		this.id = id;
		this.name = name;
	}


}
