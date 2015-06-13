package de.egh.bikehist.ui.event;

import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;


/** UI class: Item of list with all data to show. */
class EventItem {
	public Event getEvent() {
		return event;
	}



	private final Event event;

	public Tag getTag() {
		return tag;
	}

	private final Tag tag;
	public EventItem(Event event, Tag tag) {

		this.event = event;
		this.tag = tag;

	}



}
