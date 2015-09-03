package de.egh.bikehist.sync;

import android.content.Context;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;


/**
 * Synchronize database with a local JSON-File. Only manages the file handling, but has no
 * synchronization logic.
 */
public class SyncFileHandler implements ExternalSyncSource {

	private static final String SYNC_FILE_NAME = "bikeHist.txt";
	private static final String BACKUP_FILE_NAME = SYNC_FILE_NAME + "_bak.txt";
	private static final String TMP_FILE_NAME = SYNC_FILE_NAME + "_tmp.txt";
	private static final String TAG = SyncFileHandler.class.getSimpleName();
	private static final String SYNC_DIR_NAME = "sync";
	private final Context context;
	/**
	 * Sync-directory, may not be null
	 */
	private File syncDirFile = null;
	/**
	 * Sync source file, may not be null
	 */
	private File syncFile = null;
	/**
	 * Backup of the original sync source file, may not be null
	 */
	private File backupFile = null;
	/**
	 * Temp file, may not be null
	 */
	private File tmpFile = null;
	private File SyncDirF;

	private final JsonHelper jsonHelper;

	public SyncFileHandler(Context context) {
		this.context = context;
		jsonHelper = new JsonHelper(context);
	}

//	public JsonWriter startJsonStream(OutputStream out) throws IOException {
//		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
//		writer.setIndent("  ");
//		writer.beginObject();
//		return writer;
//	}

//	public void closeJsonStream(JsonWriter writer) throws IOException {
//		writer.endObject();
//		writer.close();
//	}

//	public void writeBikeJsonStream(JsonWriter writer, List<Bike> bikes) throws IOException {
//		writer.name(Fields.BIKES);
//		writer.beginArray();
//		for (Bike bike : bikes) {
//			if (!bike.isDeleted())
//				writeBike(writer, bike);
//		}
//		writer.endArray();
//	}

//	public void writeTagTypeJsonStream(JsonWriter writer, List<TagType> tagTypes) throws IOException {
//		writer.name(Fields.TAG_TYPES);
//		writer.beginArray();
//		for (TagType tagType : tagTypes) {
//			if (!tagType.isDeleted())
//				writeTagType(writer, tagType);
//		}
//		writer.endArray();
//	}

//	public void writeTagJsonStream(JsonWriter writer, List<Tag> tags) throws IOException {
//		writer.name(Fields.TAGS);
//		writer.beginArray();
//		for (Tag tag : tags) {
//			if (!tag.isDeleted())
//				writeTag(writer, tag);
//		}
//		writer.endArray();
//	}

//	public void writeEventJsonStream(JsonWriter writer, List<Event> events) throws IOException {
//		writer.name(Fields.EVENTS);
//		writer.beginArray();
//		for (Event event : events) {
//			if (!event.isDeleted())
//				writeEvent(writer, event);
//		}
//		writer.endArray();
//	}
	
	
//	public void writeBike(JsonWriter writer, Bike bike) throws IOException {
//		writer.beginObject();
//		writer.name(Fields.Bike.ID).value(bike.getId().toString());
//		writer.name(Fields.Bike.NAME_STRING).value(bike.getName());
//		writer.name(Fields.Bike.TOUCHED_AT).value(bike.getTouchedAt());
//		writer.name(Fields.Bike.FRAME_NUMBER).value(bike.getFrameNumber());
//		writer.endObject();
//	}

//	public void writeTagType(JsonWriter writer, TagType tagType) throws IOException {
//		writer.beginObject();
//		writer.name(Fields.TagType.ID).value(tagType.getId().toString());
//		writer.name(Fields.TagType.NAME_STRING).value(tagType.getName());
//		writer.name(Fields.TagType.TOUCHED_AT).value(tagType.getTouchedAt());
//		writer.endObject();
//	}

//	public void writeTag(JsonWriter writer, Tag tag) throws IOException {
//		writer.beginObject();
//		writer.name(Fields.Tag.ID).value(tag.getId().toString());
//		writer.name(Fields.Tag.NAME_STRING).value(tag.getName());
//		writer.name(Fields.Tag.TOUCHED_AT).value(tag.getTouchedAt());
//		writer.name(Fields.Tag.TAG_TYPE_ID).value(tag.getTagTypeId().toString());
//		writer.endObject();
//	}

//	public void writeEvent(JsonWriter writer, Event event) throws IOException {
//		writer.beginObject();
//		writer.name(Fields.Event.ID).value(event.getId().toString());
//		writer.name(Fields.Event.NAME_STRING).value(event.getName());
//		writer.name(Fields.Event.TOUCHED_AT).value(event.getTouchedAt());
//		writer.name(Fields.Event.BIKE_ID).value(event.getBikeId().toString());
//		writer.name(Fields.Event.DISTANCE).value(event.getDistance());
//		writer.name(Fields.Event.TAG_ID).value(event.getTagId().toString());
//		writer.name(Fields.Event.TIMESTAMP).value(event.getTimestamp());
//		writer.endObject();
//	}

	/* Checks if external storage is available for read and write */
	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}


	/**
	 * Copies content from source to destination file. Files must exists and my not be null.
	 */
	private void copyFile(File src, File dest) throws IOException {
		FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dest);
		boolean end = false;
		do {
			int b = in.read();
			if (b == -1)
				end = true;
			else
				out.write(b);
		} while (!end);
		out.close();
		in.close();


	}

	@Override
	public void prepare() throws BikeHistSyncException {
		String folder_main = context.getString(R.string.app_name);

		//Create directory structure
		File fRootDir = new File(Environment.getExternalStorageDirectory(),
				folder_main);
		if (!fRootDir.exists()) {
			fRootDir.mkdirs();
		}
		SyncDirF = new File(fRootDir, SYNC_DIR_NAME);
		if (!SyncDirF.exists()) {
			SyncDirF.mkdirs();
		}
		syncDirFile = SyncDirF;

		//Without the directory, we can't go on.
		if (!syncDirFile.exists())
			throw new BikeHistSyncException(context.getString(R.string.messageMissingSyncDirectory)
					+ syncDirFile.getName());

		//Access to the Source file
		syncFile = new File(SyncDirF.getPath(), SYNC_FILE_NAME);
		accessFile(syncFile, false);

		//Make a backup file for the original content
		backupFile = new File(SyncDirF.getPath(), BACKUP_FILE_NAME);
		accessFile(backupFile, true);

		//Access to a new tmp file
		tmpFile = new File(SyncDirF.getPath(), TMP_FILE_NAME);
		accessFile(tmpFile, true);

		//Backup content
		try {
			copyFile(syncFile, backupFile);
		} catch (IOException e) {
			throw new BikeHistSyncException(e.getMessage());
		}

	}

	/**
	 * Create if not exists and check if writeable. Set the timestamp to protect
	 * overriding by an file sync tool.
	 *
	 * @param f        File
	 * @param override true, if existing file has to be deleted
	 * @throws BikeHistSyncException
	 */
	private void accessFile(File f, boolean override) throws BikeHistSyncException {
		//Access to the Source file

		if (override) {
			if (f.exists())
				if (!f.delete())
					throw new BikeHistSyncException(
							String.format(context.getString(R.string.messageCantDeleteFile),
									f.getName()));
		}

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				throw new BikeHistSyncException(
						String.format(context.getString(R.string.messageCantCreateFile),
								f.getName()));
			}
		}

		if (!f.canWrite())
			throw new BikeHistSyncException(
					String.format(context.getString(R.string.messageCantWriteFile),
							f.getName()));

		//TODO geht wohl nicht
//		if (!f.setLastModified(System.currentTimeMillis()))
//			throw new BikeHistSyncException(String.format(context.getString(R.string.messageCantWriteFile),
//				f.getName()));

	}

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
//	public List<Bike> readBikeArray(JsonReader reader) throws IOException {
//		List<Bike> bikes = new ArrayList<>();
//		EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(context);
//
//		reader.beginArray();
//		while (reader.hasNext()) {
//
//			Bike bike = readBike(reader);
//			if (bikeUtils.isValid(bike))
//				bikes.add(bike);
//			else
//				Log.w(TAG, "Invalid bike will not be imported: ID=" + bike.getId() + ", Name=" + bike.getName());
//		}
//		reader.endArray();
//		return bikes;
//	}

//	public Bike readBike(JsonReader reader) throws IOException {
//		String id = null;
//		boolean deleted = false;
//		long touchedAt = 0;
//		String bikeName = null;
//		String frameNumber = null;
//
//		reader.beginObject();
//		while (reader.hasNext()) {
//			String name = reader.nextName();
//			if (name.equals(Fields.Bike.ID)) {
//				id = reader.nextString();
//			} else if (name.equals(Fields.Bike.DELETED)) {
//				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
//			} else if (name.equals(Fields.Bike.TOUCHED_AT)) {
//				touchedAt = reader.nextLong();
//			} else if (name.equals(Fields.Bike.NAME_STRING)) {
//				bikeName = reader.nextString();
//			} else if (name.equals(Fields.Bike.FRAME_NUMBER)) {
//				frameNumber = reader.nextString();
//			} else {
//				Log.w(TAG, "Skipped unexpected JSON value of Bike: " + name);
//				reader.skipValue();
//			}
//		}
//
//		UUID uuid = null;
//		try {
//			uuid = UUID.fromString(id);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported bike has invalid ID=" + id);
//		}
//		reader.endObject();
//		return new Bike(uuid, bikeName, frameNumber, deleted, touchedAt);
//	}

//	public List<TagType> readTagTypeArray(JsonReader reader) throws IOException {
//		List<TagType> tagTypes = new ArrayList<>();
//		EntityUtils<TagType> tagTypeUtils = EntityUtilsFactory.createTagTypeUtils(context);
//
//		reader.beginArray();
//		while (reader.hasNext()) {
//
//			TagType tagType = readTagType(reader);
//			if (tagTypeUtils.isValid(tagType))
//				tagTypes.add(tagType);
//			else
//				Log.w(TAG, "Invalid tagType will not be imported: ID=" + tagType.getId() + ", Name=" + tagType.getName());
//		}
//		reader.endArray();
//		return tagTypes;
//	}

//	public List<Tag> readTagArray(JsonReader reader) throws IOException {
//		List<Tag> tags = new ArrayList<>();
//		EntityUtils<Tag> tagUtils = EntityUtilsFactory.createTagUtils(context);
//
//		reader.beginArray();
//		while (reader.hasNext()) {
//
//			Tag tag = readTag(reader);
//			if (tagUtils.isValid(tag))
//				tags.add(tag);
//			else
//				Log.w(TAG, "Invalid tag will not be imported: ID=" + tag.getId() + ", Name=" + tag.getName());
//		}
//		reader.endArray();
//		return tags;
//	}

//	public List<Event> readEventArray(JsonReader reader) throws IOException {
//		List<Event> events = new ArrayList<>();
//		EntityUtils<Event> eventUtils = EntityUtilsFactory.createEventUtils(context);
//
//		reader.beginArray();
//		while (reader.hasNext()) {
//
//			Event event = readEvent(reader);
//			if (eventUtils.isValid(event))
//				events.add(event);
//			else
//				Log.w(TAG, "Invalid event will not be imported: ID=" + event.getId() + ", Name=" + event.getName());
//		}
//		reader.endArray();
//		return events;
//	}


//	public TagType readTagType(JsonReader reader) throws IOException {
//		String id = null;
//		boolean deleted = false;
//		long touchedAt = 0;
//		String tagTypeName = null;
//		String frameNumber = null;
//
//		reader.beginObject();
//		while (reader.hasNext()) {
//			String name = reader.nextName();
//			if (name.equals(Fields.TagType.ID)) {
//				id = reader.nextString();
//			} else if (name.equals(Fields.TagType.DELETED)) {
//				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
//			} else if (name.equals(Fields.TagType.TOUCHED_AT)) {
//				touchedAt = reader.nextLong();
//			} else if (name.equals(Fields.TagType.NAME_STRING)) {
//				tagTypeName = reader.nextString();
//			} else {
//				Log.w(TAG, "Skipped unexpected JSON value of TagType: " + name);
//				reader.skipValue();
//			}
//		}
//
//		UUID uuid = null;
//		try {
//			uuid = UUID.fromString(id);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported tagType has invalid ID=" + id);
//		}
//		reader.endObject();
//		return new TagType(uuid, tagTypeName, deleted, touchedAt);
//	}

//	public Tag readTag(JsonReader reader) throws IOException {
//		String id = null;
//		boolean deleted = false;
//		long touchedAt = 0;
//		String tagName = null;
//		String tagTypeId = null;
//
//		reader.beginObject();
//		while (reader.hasNext()) {
//			String name = reader.nextName();
//			if (name.equals(Fields.Tag.ID)) {
//				id = reader.nextString();
//			} else if (name.equals(Fields.Tag.DELETED)) {
//				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
//			} else if (name.equals(Fields.Tag.TOUCHED_AT)) {
//				touchedAt = reader.nextLong();
//			} else if (name.equals(Fields.Tag.NAME_STRING)) {
//				tagName = reader.nextString();
//			} else if (name.equals(Fields.Tag.TAG_TYPE_ID)) {
//				tagTypeId = reader.nextString();
//			} else {
//				Log.w(TAG, "Skipped unexpected JSON value of Tag: " + name);
//				reader.skipValue();
//			}
//		}
//
//		UUID uuid = null;
//		try {
//			uuid = UUID.fromString(id);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported tag has invalid ID=" + id);
//		}
//		UUID tagTypeUuid = null;
//		try {
//			tagTypeUuid = UUID.fromString(tagTypeId);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported tag has invalid tag type ID=" + tagTypeUuid);
//		}
//		reader.endObject();
//		return new Tag(uuid, tagName, tagTypeUuid, deleted, touchedAt);
//	}

//	public Event readEvent(JsonReader reader) throws IOException {
//		String id = null;
//		boolean deleted = false;
//		long touchedAt = 0;
//		String eventName = null;
//		String tagId = null;
//		String bikeId = null;
//		long distance = 0;
//		long timestamp = 0;
//
//		reader.beginObject();
//		while (reader.hasNext()) {
//			String name = reader.nextName();
//			if (name.equals(Fields.Event.ID)) {
//				id = reader.nextString();
//			} else if (name.equals(Fields.Event.DELETED)) {
//				deleted = reader.nextString().equals(BikeHistProvider.BikeHistContract.Boolean.True.asString);
//			} else if (name.equals(Fields.Event.TOUCHED_AT)) {
//				touchedAt = reader.nextLong();
//			} else if (name.equals(Fields.Event.NAME_STRING)) {
//				eventName = reader.nextString();
//			} else if (name.equals(Fields.Event.BIKE_ID)) {
//				bikeId = reader.nextString();
//			} else if (name.equals(Fields.Event.DISTANCE)) {
//				distance = reader.nextLong();
//			} else if (name.equals(Fields.Event.TAG_ID)) {
//				tagId = reader.nextString();
//			} else if (name.equals(Fields.Event.TIMESTAMP)) {
//				timestamp = reader.nextLong();
//			} else {
//				Log.w(TAG, "Skipped unexpected JSON value of Event: " + name);
//				reader.skipValue();
//			}
//		}
//
//		UUID uuid = null;
//		try {
//			uuid = UUID.fromString(id);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported event has invalid ID=" + id);
//		}
//
//		UUID bikeUuid = null;
//		try {
//			bikeUuid = UUID.fromString(bikeId);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported event has invalid  bike ID=" + id);
//		}
//
//		UUID tagUuid = null;
//		try {
//			tagUuid = UUID.fromString(tagId);
//		} catch (IllegalArgumentException | NullPointerException e) {
//			Log.w(TAG, "Imported event has invalid  tag ID=" + id);
//		}
//
//		reader.endObject();
//		return new Event(uuid, eventName, deleted, touchedAt, distance, bikeUuid, tagUuid, timestamp, 0, 0);
//	}


	@Override
	public SyncData getData() {


		SyncData syncData = new SyncData();

		try {
			FileInputStream fis = new FileInputStream(syncFile);
			syncData = jsonHelper.read(fis);

//			JsonReader reader = new JsonReader(new InputStreamReader(fis, "UTF-8"));
//
//			//Level 1: Sync-timestamp, bikeList, etc.
//			reader.beginObject();
//
//			while (reader.hasNext()) {
//				String name = reader.nextName();
//				if (name.equals(Fields.TIMESTAMP)) {
//					syncData.setTimestamp(reader.nextLong());
//				} else if (name.equals(Fields.BIKES)) {
//					syncData.getBikeData().add(readBikeArray(reader));
//				} else if (name.equals(Fields.TAG_TYPES)) {
//					syncData.getTagTypeData().add(readTagTypeArray(reader));
//				} else if (name.equals(Fields.TAGS)) {
//					syncData.getTagData().add(readTagArray(reader));
//				} else if (name.equals(Fields.EVENTS)) {
//					syncData.getEventData().add(readEventArray(reader));
//				} else {
//					Log.w(TAG, "Skipped unexpected JSON value on Level 1: " + name);
//					reader.skipValue();
//				}
//			}
//			reader.endObject();

		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		return syncData;
	}

	/**
	 * Create temp file with this data.
	 */
	@Override
	public void putData(EntityContainer data) throws BikeHistSyncException {

		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new FileWriter(tmpFile));

			jsonHelper.write(br, data);

			//**old**//
//			JsonWriter jw = new JsonWriter(br);
//			jw.setIndent(" ");
//
//			//Level 1
//			jw.beginObject();
//			jw.name(Fields.TIMESTAMP).value(System.currentTimeMillis());
//
//			//Bike array
//			writeBikeJsonStream(jw, mergeData.getBikes());
//
//			//TagType array
//			writeTagTypeJsonStream(jw, mergeData.getTagTypes());
//
//			//Tag array
//			writeTagJsonStream(jw, mergeData.getTags());
//
//			//Tag array
//			writeEventJsonStream(jw, mergeData.getEvents());
//
//			jw.endObject();
//
//
//			br.close();

			commit();

		} catch (IOException e) {
			throw new BikeHistSyncException(e.getMessage());

		} finally {
			if (br != null)
				try {
					br.close();
				} catch (Exception e) {
					throw new BikeHistSyncException(e.getMessage());
				}
		}

	}


	private void commit() throws IOException {

		copyFile(tmpFile, syncFile);
		tmpFile.delete();


		//TODO Remove JSON Logging
		FileInputStream fis = new FileInputStream(syncFile);
		Reader reader = new InputStreamReader(fis);
		int r;
		while ((r = reader.read()) != -1) {
			char ch = (char) r;
			System.out.print(ch);
		}


// initiate media scan and put the new things into the path array to
// make the scanner aware of the location and the files you want to see
//		MediaScannerConnection.scanFile(context,
//				new String[]{tmpFile.getPath(),syncFile.getPath(),backupFile.getPath() }, null, null);
//


	}

	/**
	 * Fields, every Entity has.
	 */
	 static abstract class EntityFields {
		static final String ID = "id";
		static final String DELETED = "deleted";
		static final String TOUCHED_AT = "touchedAt";
		static final String NAME = "name";
	}

	/**
	 * Defines the JSON field names
	 */
	static abstract class Fields {

		static final String TIMESTAMP = "timestamp";
		static final String BIKES = "bikes";
		static final String TAG_TYPES = "tagTypes";
		static final String TAGS = "tags";
		static final String EVENTS = "events";

		static abstract class Bike extends EntityFields {
			static final String FRAME_NUMBER = "frameNumber";
		}

		static abstract class TagType extends EntityFields {
		}

		static abstract class Tag extends EntityFields {
			static final String TAG_TYPE_ID = "tagTypeId";
		}

		static abstract class Event extends EntityFields {
			static final String BIKE_ID = "bikeId";
			static final String DISTANCE = "distance";
			static final String TAG_ID = "tagId";
			static final String TIMESTAMP = "timestamp";
		}
	}
}
