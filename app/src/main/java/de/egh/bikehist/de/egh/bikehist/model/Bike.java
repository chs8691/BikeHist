package de.egh.bikehist.de.egh.bikehist.model;

import java.util.UUID;

/** Functional class; Bike. */
public class Bike implements ModelType{
	private UUID id;
	private String frameNumber;
	private String name;

	/**
	 Creates a Bike instance
	 @param id UUID for this dataset
	 @param name String with name
	 @param frameNumber Unique ID of the bike
	 */
	public Bike(UUID id, String name, String frameNumber) {
		this.name = name;
		this.frameNumber = frameNumber;
		this.id = id;
	}

	public String getFrameNumber() {
		return frameNumber;
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
