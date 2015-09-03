package de.egh.bikehist.model.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

/**
 * Creates EntityUtils for a particular Entity type.
 */
public class EntityUtilsFactory {
	private static final String DELI = ";";
	private static final String TAG = EntityUtilsFactory.class.getSimpleName();

	public static EntityUtils<Bike> createBikeUtils(Context context) {
		return new BikeUtils(context);
	}

	public static EntityUtils<TagType> createTagTypeUtils(Context context) {
		return new TagTypeUtils(context);
	}

	public static EntityUtils<Tag> createTagUtils(Context context) {
		return new TagUtils(context);
	}

	public static EntityUtils<Event> createEventUtils(Context context) {
		return new EventUtils(context);
	}

	/**
	 * Parses String to replace delimiter character with a '.'.
	 * Ok, that's not very professional, but simple to do.
	 */
	private static String replaceDelimiter(String text) {
		return text.replace(DELI, ".");
	}

	/**
	 * Returns null, if id is no valid UUID.
	 */
	private static UUID getUUIDFromString(String id) {

		try {
			return UUID.fromString(id);
		} catch (NullPointerException | IllegalArgumentException e) {
			if (id != null) {
				Log.w(TAG, "No valid UUID:");
			} else {
				Log.w(TAG, "id is null");
			}
			return null;
		}

	}

	/**
	 * Converts boolean to integer: 1 is true, otherwise false
	 */
	private static int asInt(boolean value) {
		return value ? 1 : 0;
	}

	private static class BikeUtils implements EntityUtils<Bike> {

		private final Context context;

		BikeUtils(Context context) {

			this.context = context;
		}

		@Override
		public String getEntityNamePlural() {
			return context.getString(R.string.entityNamePluralBikes);
		}

		@Override
		public Uri getContentUri() {
			return Tables.Bike.URI;
		}


		@Override
		public Bike build(Cursor c) {
			UUID id = getUUIDFromString(c.getString(Tables.Bike.Id.NUMBER));
			String name = c.getString(Tables.Bike.Name.NUMBER);
			boolean deleted = c.getInt(Tables.Bike.Deleted.NUMBER) == BikeHistProvider.BikeHistContract.Boolean.True.asInt;
			long touchedAt = c.getLong(Tables.Bike.TouchedAt.NUMBER);
			String frameNumber = c.getString(Tables.Bike.FrameNumber.NUMBER);

			if (id == null) {
				return null;
			}

			return new Bike(id, name, frameNumber, deleted, touchedAt);

		}

		@Override
		public ContentValues build(Bike bike) {
			ContentValues values = new ContentValues();

			values.put(Tables.Bike.Id.NAME, bike.getId().toString());
			values.put(Tables.Bike.Name.NAME, bike.getName());
			values.put(Tables.Bike.Deleted.NAME, asInt(bike.isDeleted()));
			values.put(Tables.Bike.TouchedAt.NAME, bike.getTouchedAt());
			values.put(Tables.Bike.FrameNumber.NAME, bike.getFrameNumber());

			return values;
		}

		@Override
		public boolean isValid(Bike entity) {
			return
					entity.getId() != null
							&& entity.getName() != null
							&& !entity.getName().isEmpty()
							&& entity.getFrameNumber() != null;
		}

	}

	private static class TagTypeUtils implements EntityUtils<TagType> {

		private final Context context;

		TagTypeUtils(Context context) {

			this.context = context;
		}


		@Override
		public String getEntityNamePlural() {
			return context.getString(R.string.entityNamePluralTagTypes);
		}

		@Override
		public Uri getContentUri() {
			return Tables.TagType.URI;
		}


		@Override
		public TagType build(Cursor c) {
			UUID id = getUUIDFromString(c.getString(Tables.TagType.Id.NUMBER));
			String name = c.getString(Tables.TagType.Name.NUMBER);
			boolean deleted = c.getInt(Tables.TagType.Deleted.NUMBER) == BikeHistProvider.BikeHistContract.Boolean.True.asInt;
			long touchedAt = c.getLong(Tables.TagType.TouchedAt.NUMBER);

			if (id == null) {
				return null;
			}

			return new TagType(id, name, deleted, touchedAt);

		}

		@Override
		public ContentValues build(TagType tagType) {
			ContentValues values = new ContentValues();

			values.put(Tables.TagType.Id.NAME, tagType.getId().toString());
			values.put(Tables.TagType.Name.NAME, tagType.getName());
			values.put(Tables.TagType.Deleted.NAME, asInt(tagType.isDeleted()));
			values.put(Tables.TagType.TouchedAt.NAME, tagType.getTouchedAt());

			return values;
		}

		@Override
		public boolean isValid(TagType entity) {
			return
					entity.getId() != null
							&& entity.getName() != null
							&& !entity.getName().isEmpty();
		}

	}

	private static class TagUtils implements EntityUtils<Tag> {

		private final Context context;

		TagUtils(Context context) {

			this.context = context;
		}


		@Override
		public String getEntityNamePlural() {
			return context.getString(R.string.entityNamePluralTags);
		}

		@Override
		public Uri getContentUri() {
			return Tables.Tag.URI;
		}


		@Override
		public Tag build(Cursor c) {
			UUID id = getUUIDFromString(c.getString(Tables.Tag.Id.NUMBER));
			String name = c.getString(Tables.Tag.Name.NUMBER);
			boolean deleted = c.getInt(Tables.Tag.Deleted.NUMBER) == BikeHistProvider.BikeHistContract.Boolean.True.asInt;
			long touchedAt = c.getLong(Tables.Tag.TouchedAt.NUMBER);
			UUID tagTypeId = getUUIDFromString(c.getString(Tables.Tag.TagTypeId.NUMBER));

			if (id == null) {
				return null;
			}

			return new Tag(id, name, tagTypeId, deleted, touchedAt);

		}

		@Override
		public ContentValues build(Tag tag) {
			ContentValues values = new ContentValues();

			values.put(Tables.Tag.Id.NAME, tag.getId().toString());
			values.put(Tables.Tag.Name.NAME, tag.getName());
			values.put(Tables.Tag.Deleted.NAME, asInt(tag.isDeleted()));
			values.put(Tables.Tag.TouchedAt.NAME, tag.getTouchedAt());
			values.put(Tables.Tag.TagTypeId.NAME, tag.getTagTypeId().toString());
			return values;
		}

		@Override
		public boolean isValid(Tag entity) {
			return entity.getId() != null
					&& entity.getName() != null
					&& !entity.getName().isEmpty()
					&& entity.getTagTypeId() != null;
		}

	}

	private static class EventUtils implements EntityUtils<Event> {

		private final Context context;

		EventUtils(Context context) {

			this.context = context;
		}


		@Override
		public String getEntityNamePlural() {
			return context.getString(R.string.entityNamePluralEvents);
		}

		@Override
		public Uri getContentUri() {
			return Tables.Event.URI;
		}

		@Override
		public Event build(Cursor c) {
			UUID id = getUUIDFromString(c.getString(Tables.Event.Id.NUMBER));

			if (id == null) {
				return null;
			}

/**UUID id, String name, long distance, UUID bikeId, UUID tagId,
 long timestamp, long diffDistance, long diffTimestamp, boolean deleted, long touchedAt*/
			return new Event(
					id,
					c.getString(Tables.Event.Name.NUMBER),
					c.getInt(Tables.Event.Deleted.NUMBER) == BikeHistProvider.BikeHistContract.Boolean.True.asInt,
					c.getLong(Tables.Event.TouchedAt.NUMBER),
					c.getLong(Tables.Event.Distance.NUMBER),
					getUUIDFromString(c.getString(Tables.Event.BikeId.NUMBER)),
					getUUIDFromString(c.getString(Tables.Event.TagId.NUMBER)),
					c.getLong(Tables.Event.Timestamp.NUMBER),
					c.getLong(Tables.Event.DiffDistance.NUMBER),
					c.getLong(Tables.Event.DiffTimestamp.NUMBER)
			);
		}

		@Override
		public ContentValues build(Event event) {
			ContentValues values = new ContentValues();

			values.put(Tables.Event.Id.NAME, event.getId().toString());
			values.put(Tables.Event.Name.NAME, event.getName());
			values.put(Tables.Event.Deleted.NAME, asInt(event.isDeleted()));
			values.put(Tables.Event.TouchedAt.NAME, event.getTouchedAt());
			values.put(Tables.Event.Distance.NAME, event.getDistance());
			values.put(Tables.Event.BikeId.NAME, event.getBikeId().toString());
			values.put(Tables.Event.TagId.NAME, event.getTagId().toString());
			values.put(Tables.Event.Timestamp.NAME, event.getTimestamp());

			return values;
		}

		@Override
		public boolean isValid(Event entity) {
			return entity.getId() != null
					&& entity.getBikeId() != null
					&& entity.getTimestamp() > 0
					&& entity.getTagId() != null;
		}

	}
}
