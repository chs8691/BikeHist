package de.egh.bikehist.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;

/**
 * Holds MergeItems and provides convenience access
 */
public class MergeData implements EntityContainer {

	private final List<MergeItem<Bike>> bikes = new ArrayList<>();
	private final List<MergeItem<TagType>> tagTypes = new ArrayList<>();
	private final List<MergeItem<Tag>> tags = new ArrayList<>();
	private final List<MergeItem<Event>> events = new ArrayList<>();


	public void addBike(MergeItem<Bike> item) {
		bikes.add(item);
	}

	public void addEvent(MergeItem<Event> item) {
		events.add(item);
	}

	public List<MergeItem<Bike>> getBikeItems() {
		return bikes;
	}

	public List<MergeItem<TagType>> getTagTypeItems() {
		return tagTypes;
	}

	public List<MergeItem<Tag>> getTagItems() {
		return tags;
	}

	/**
	 * Returns reference to list of events. Use this only, if list must be modified.
	 */
	public List<MergeItem<Event>> getEventItems() {
		return events;
	}


	/**
	 * List with reference to all merged bikes.
	 */
	@Override
	public List<Bike> getBikes() {
		List<Bike> entityList = new ArrayList<>();

		for (MergeItem<Bike> item : bikes) {
			entityList.add(item.getMergedEntity());
		}

		return entityList;
	}

	/**
	 * List with reference to all merged tagTypes.
	 */
	@Override
	public List<TagType> getTagTypes() {
		List<TagType> entityList = new ArrayList<>();

		for (MergeItem<TagType> item : tagTypes) {
			entityList.add(item.getMergedEntity());
		}

		return entityList;
	}

	/**
	 * List with reference to all merged tags.
	 */
	@Override
	public List<Tag> getTags() {
		List<Tag> entityList = new ArrayList<>();

		for (MergeItem<Tag> item : tags) {
			entityList.add(item.getMergedEntity());
		}

		return entityList;
	}

	/**
	 * List with references to all merged events.
	 */
	@Override
	public List<Event> getEvents() {
		List<Event> entityList = new ArrayList<>();

		for (MergeItem<Event> item : events) {
			entityList.add(item.getMergedEntity());
		}

		return entityList;
	}

	public void addTagType(MergeItem<TagType> item) {
		tagTypes.add(item);
	}

	/**
	 * Returns Item of merged TagType, or null, if not exists.
	 */
	public MergeItem<TagType> getTagTypeItem(UUID id) {
		for (MergeItem<TagType> item : tagTypes) {
			if (item.getMergedEntity().getId().equals(id))
				return item;
		}
		return null;
	}

	/**
	 * Returns Item of merged Tag, or null, if not exists.
	 */
	public MergeItem<Tag> getTagItem(UUID id) {
		for (MergeItem<Tag> item : tags) {
			if (item.getMergedEntity().getId().equals(id))
				return item;
		}
		return null;
	}

	/**
	 * Returns Item of merged Bike, or null, if not exists.
	 */
	public MergeItem<Bike> getBikeItem(UUID id) {
		for (MergeItem<Bike> item : bikes) {
			if (item.getMergedEntity().getId().equals(id))
				return item;
		}
		return null;
	}


	public void addTag(MergeItem<Tag> item) {
		tags.add(item);
	}

	public List<MergeItem<Tag>> getTagList() {
		List<MergeItem<Tag>> list = new ArrayList<>();
		list.addAll(tags);
		return list;
	}

	/**
	 * Returns new List with event references, no deep copy.
	 */
	public List<MergeItem<Event>> getEventList() {
		List<MergeItem<Event>> list = new ArrayList<>();
		list.addAll(events);
		return list;
	}

}
