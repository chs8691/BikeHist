package de.egh.bikehist.sync;

import java.util.List;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;

/**
 * Structure to hold set of Entities
 */
interface EntityContainer {

	abstract List<Bike> getBikes();

	abstract List<TagType> getTagTypes();

	abstract List<Tag> getTags();

	abstract List<Event> getEvents();
}
