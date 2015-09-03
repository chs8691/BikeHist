package de.egh.bikehist.sync;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.sync.SyncFileHandler.Fields;

/**
 * All things for reading and writing JSON for Entities.
 */
public class JsonHelper {
	private static final String TAG = JsonHelper.class.getSimpleName();
//	private static EventListener nullEventListener = new EventListener() {
//		@Override
//		public void onEntityRead(Class entityClass, int counter) {
//
//		}
//	};
	private final Context context;
//	private EventListener eventListener;


	public JsonHelper(Context context) {
		this.context = context;
	}

//	/**
//	 * EventListener can be null
//	 */
//	public JsonHelper(Context context, EventListener eventListener) {
//		this.context = context;
//		if (eventListener != null)
//			this.eventListener = eventListener;
//	}

	/**
	 * {
	 * "timestamp": 912345678901,
	 * "bikeList":[
	 * {
	 * "id": "123-FG-ASDFA345ET-123",
	 * "deleted": "0",
	 * "touchedAt": "123456787980",
	 * "name": "Brompton",
	 * "frameNumber": "BROMPTON-123345",
	 * },
	 * {
	 * "id": "///-FG-ASDFA345ET-&&&",
	 * "deleted": "0",
	 * "touchedAt": "123456765433",
	 * "name": "Giant",
	 * "frameNumber": "GIANT-123345",
	 * }
	 * ]
	 * }
	 */
	private List<Bike> readBikeArray(JsonReader reader) throws IOException {
		List<Bike> bikes = new ArrayList<>();
		EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(context);

		reader.beginArray();
		while (reader.hasNext()) {

			Bike bike = readBike(reader);
			if (bikeUtils.isValid(bike)) {
				bikes.add(bike);
//				eventListener.onEntityRead(Bike.class, bikes.size());
			} else
				Log.w(TAG, "Invalid bike will not be imported: ID=" + bike.getId() + ", Name=" + bike.getName());
		}
		reader.endArray();
		return bikes;
	}

	private Bike readBike(JsonReader reader) throws IOException {
		String id = null;
		boolean deleted = false;
		long touchedAt = 0;
		String bikeName = null;
		String frameNumber = null;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(Fields.Bike.ID)) {
				id = reader.nextString();
			} else if (name.equals(Fields.Bike.DELETED)) {
				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
			} else if (name.equals(Fields.Bike.TOUCHED_AT)) {
				touchedAt = reader.nextLong();
			} else if (name.equals(Fields.Bike.NAME)) {
				bikeName = reader.nextString();
			} else if (name.equals(Fields.Bike.FRAME_NUMBER)) {
				frameNumber = reader.nextString();
			} else {
				Log.w(TAG, "Skipped unexpected JSON value of Bike: " + name);
				reader.skipValue();
			}
		}

		UUID uuid = null;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported bike has invalid ID=" + id);
		}
		reader.endObject();
		return new Bike(uuid, bikeName, frameNumber, deleted, touchedAt);
	}

	private Event readEvent(JsonReader reader) throws IOException {
		String id = null;
		boolean deleted = false;
		long touchedAt = 0;
		String eventName = null;
		String tagId = null;
		String bikeId = null;
		long distance = 0;
		long timestamp = 0;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(Fields.Event.ID)) {
				id = reader.nextString();
			} else if (name.equals(Fields.Event.DELETED)) {
				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
			} else if (name.equals(Fields.Event.TOUCHED_AT)) {
				touchedAt = reader.nextLong();
			} else if (name.equals(Fields.Event.NAME)) {
				eventName = reader.nextString();
			} else if (name.equals(Fields.Event.BIKE_ID)) {
				bikeId = reader.nextString();
			} else if (name.equals(Fields.Event.DISTANCE)) {
				distance = reader.nextLong();
			} else if (name.equals(Fields.Event.TAG_ID)) {
				tagId = reader.nextString();
			} else if (name.equals(Fields.Event.TIMESTAMP)) {
				timestamp = reader.nextLong();
			} else {
				Log.w(TAG, "Skipped unexpected JSON value of Event: " + name);
				reader.skipValue();
			}
		}

		UUID uuid = null;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported event has invalid ID=" + id);
		}

		UUID bikeUuid = null;
		try {
			bikeUuid = UUID.fromString(bikeId);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported event has invalid  bike ID=" + id);
		}

		UUID tagUuid = null;
		try {
			tagUuid = UUID.fromString(tagId);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported event has invalid  tag ID=" + id);
		}

		reader.endObject();
		return new Event(uuid, eventName, deleted, touchedAt, distance, bikeUuid, tagUuid, timestamp, 0, 0);
	}

	private List<Event> readEventArray(JsonReader reader) throws IOException {
		List<Event> events = new ArrayList<>();
		EntityUtils<Event> eventUtils = EntityUtilsFactory.createEventUtils(context);

		reader.beginArray();
		while (reader.hasNext()) {

			Event event = readEvent(reader);
			if (eventUtils.isValid(event)) {
				events.add(event);
//				eventListener.onEntityRead(Event.class, events.size());
			} else
				Log.w(TAG, "Invalid event will not be imported: ID=" + event.getId() + ", Name=" + event.getName());
		}
		reader.endArray();
		return events;
	}

	private TagType readTagType(JsonReader reader) throws IOException {
		String id = null;
		boolean deleted = false;
		long touchedAt = 0;
		String tagTypeName = null;
		String frameNumber = null;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(Fields.TagType.ID)) {
				id = reader.nextString();
			} else if (name.equals(Fields.TagType.DELETED)) {
				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
			} else if (name.equals(Fields.TagType.TOUCHED_AT)) {
				touchedAt = reader.nextLong();
			} else if (name.equals(Fields.TagType.NAME)) {
				tagTypeName = reader.nextString();
			} else {
				Log.w(TAG, "Skipped unexpected JSON value of TagType: " + name);
				reader.skipValue();
			}
		}

		UUID uuid = null;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported tagType has invalid ID=" + id);
		}
		reader.endObject();
		return new TagType(uuid, tagTypeName, deleted, touchedAt);
	}

	private Tag readTag(JsonReader reader) throws IOException {
		String id = null;
		boolean deleted = false;
		long touchedAt = 0;
		String tagName = null;
		String tagTypeId = null;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(Fields.Tag.ID)) {
				id = reader.nextString();
			} else if (name.equals(Fields.Tag.DELETED)) {
				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
			} else if (name.equals(Fields.Tag.TOUCHED_AT)) {
				touchedAt = reader.nextLong();
			} else if (name.equals(Fields.Tag.NAME)) {
				tagName = reader.nextString();
			} else if (name.equals(Fields.Tag.TAG_TYPE_ID)) {
				tagTypeId = reader.nextString();
			} else {
				Log.w(TAG, "Skipped unexpected JSON value of Tag: " + name);
				reader.skipValue();
			}
		}

		UUID uuid = null;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported tag has invalid ID=" + id);
		}
		UUID tagTypeUuid = null;
		try {
			tagTypeUuid = UUID.fromString(tagTypeId);
		} catch (IllegalArgumentException | NullPointerException e) {
			Log.w(TAG, "Imported tag has invalid tag type ID=" + tagTypeUuid);
		}
		reader.endObject();
		return new Tag(uuid, tagName, tagTypeUuid, deleted, touchedAt);
	}

	private List<TagType> readTagTypeArray(JsonReader reader) throws IOException {
		List<TagType> tagTypes = new ArrayList<>();
		EntityUtils<TagType> tagTypeUtils = EntityUtilsFactory.createTagTypeUtils(context);

		reader.beginArray();
		while (reader.hasNext()) {

			TagType tagType = readTagType(reader);
			if (tagTypeUtils.isValid(tagType)) {
				tagTypes.add(tagType);
//				eventListener.onEntityRead(TagType.class, tagTypes.size());
			} else
				Log.w(TAG, "Invalid tagType will not be imported: ID=" + tagType.getId() + ", Name=" + tagType.getName());
		}
		reader.endArray();
		return tagTypes;
	}

	private List<Tag> readTagArray(JsonReader reader) throws IOException {
		List<Tag> tags = new ArrayList<>();
		EntityUtils<Tag> tagUtils = EntityUtilsFactory.createTagUtils(context);

		reader.beginArray();
		while (reader.hasNext()) {

			Tag tag = readTag(reader);
			if (tagUtils.isValid(tag)) {
				tags.add(tag);
//				eventListener.onEntityRead(Tag.class, tags.size());
			} else
				Log.w(TAG, "Invalid tag will not be imported: ID=" + tag.getId() + ", Name=" + tag.getName());
		}
		reader.endArray();
		return tags;
	}

	private void writeBikeJsonStream(JsonWriter writer, List<Bike> bikes) throws IOException {
		writer.name(Fields.BIKES);
		writer.beginArray();
		for (Bike bike : bikes) {
			if (!bike.isDeleted())
				writeBike(writer, bike);
		}
		writer.endArray();
	}

	private void writeTagTypeJsonStream(JsonWriter writer, List<TagType> tagTypes) throws IOException {
		writer.name(Fields.TAG_TYPES);
		writer.beginArray();
		for (TagType tagType : tagTypes) {
			if (!tagType.isDeleted())
				writeTagType(writer, tagType);
		}
		writer.endArray();
	}

	private void writeTagJsonStream(JsonWriter writer, List<Tag> tags) throws IOException {
		writer.name(Fields.TAGS);
		writer.beginArray();
		for (Tag tag : tags) {
			if (!tag.isDeleted())
				writeTag(writer, tag);
		}
		writer.endArray();
	}

	private void writeEventJsonStream(JsonWriter writer, List<Event> events) throws IOException {
		writer.name(Fields.EVENTS);
		writer.beginArray();
		for (Event event : events) {
			if (!event.isDeleted())
				writeEvent(writer, event);
		}
		writer.endArray();
	}

	public void write(BufferedWriter br, EntityContainer data) throws IOException {

		JsonWriter jw = new JsonWriter(br);
		jw.setIndent(" ");

		//Level 1
		jw.beginObject();
		jw.name(Fields.TIMESTAMP).value(System.currentTimeMillis());

		//Bike array
		writeBikeJsonStream(jw, data.getBikes());

		//TagType array
		writeTagTypeJsonStream(jw, data.getTagTypes());

		//Tag array
		writeTagJsonStream(jw, data.getTags());

		//Tag array
		writeEventJsonStream(jw, data.getEvents());

		jw.endObject();


		br.close();


	}

	public SyncData read(InputStream is) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
		SyncData syncData = new SyncData();

		//Level 1: Sync-timestamp, bikeList, etc.
		reader.beginObject();

		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(Fields.TIMESTAMP)) {
				syncData.setTimestamp(reader.nextLong());
			} else if (name.equals(Fields.BIKES)) {
				syncData.getBikeData().add(readBikeArray(reader));
			} else if (name.equals(Fields.TAG_TYPES)) {
				syncData.getTagTypeData().add(readTagTypeArray(reader));
			} else if (name.equals(Fields.TAGS)) {
				syncData.getTagData().add(readTagArray(reader));
			} else if (name.equals(Fields.EVENTS)) {
				syncData.getEventData().add(readEventArray(reader));
			} else {
				Log.w(TAG, "Skipped unexpected JSON value on Level 1: " + name);
				reader.skipValue();
			}
		}
		reader.endObject();

		return syncData;
	}

	private void writeBike(JsonWriter writer, Bike bike) throws IOException {
		writer.beginObject();
		writer.name(Fields.Bike.ID).value(bike.getId().toString());
		writer.name(Fields.Bike.NAME).value(bike.getName());
		writer.name(Fields.Bike.TOUCHED_AT).value(bike.getTouchedAt());
		writer.name(Fields.Bike.FRAME_NUMBER).value(bike.getFrameNumber());
		writer.endObject();
	}

	private void writeTagType(JsonWriter writer, TagType tagType) throws IOException {
		writer.beginObject();
		writer.name(Fields.TagType.ID).value(tagType.getId().toString());
		writer.name(Fields.TagType.NAME).value(tagType.getName());
		writer.name(Fields.TagType.TOUCHED_AT).value(tagType.getTouchedAt());
		writer.endObject();
	}

	private void writeTag(JsonWriter writer, Tag tag) throws IOException {
		writer.beginObject();
		writer.name(Fields.Tag.ID).value(tag.getId().toString());
		writer.name(Fields.Tag.NAME).value(tag.getName());
		writer.name(Fields.Tag.TOUCHED_AT).value(tag.getTouchedAt());
		writer.name(Fields.Tag.TAG_TYPE_ID).value(tag.getTagTypeId().toString());
		writer.endObject();
	}

	private void writeEvent(JsonWriter writer, Event event) throws IOException {
		writer.beginObject();
		writer.name(Fields.Event.ID).value(event.getId().toString());
		writer.name(Fields.Event.NAME).value(event.getName());
		writer.name(Fields.Event.TOUCHED_AT).value(event.getTouchedAt());
		writer.name(Fields.Event.BIKE_ID).value(event.getBikeId().toString());
		writer.name(Fields.Event.DISTANCE).value(event.getDistance());
		writer.name(Fields.Event.TAG_ID).value(event.getTagId().toString());
		writer.name(Fields.Event.TIMESTAMP).value(event.getTimestamp());
		writer.endObject();
	}

//	/**
//	 * For Consumers to listen to read/write events
//	 */
//	public interface EventListener {
//		/**
//		 * Reading: New number of imported Entity
//		 */
//		abstract void onEntityRead(Class entityClass, int counter);
//	}

}
