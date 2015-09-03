package de.egh.bikehist.model;

import java.util.UUID;

/**
 Has every Entity.
 */
public interface ModelType {
	UUID getId();

	String getName();

	boolean isDeleted();

	void setDeleted(boolean deleted);

	long getTouchedAt();

	void setTouchedAt(long touchedAt);

}
