package de.egh.bikehist.ui;

import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;


/** UI class: Item of list with all data to show. */
public class EventItem {
	public Event getEvent() {
		return event;
	}



	private Event event;

	public Tag getTag() {
		return tag;
	}

	private Tag tag;
	public EventItem(Event event, Tag tag) {

		this.event = event;
		this.tag = tag;

	}



}
