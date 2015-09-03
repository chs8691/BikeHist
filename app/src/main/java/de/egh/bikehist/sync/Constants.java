package de.egh.bikehist.sync;

/**
 * Created by ChristianSchulzendor on 08.07.2015.
 */
 class Constants {


	 static final String SYNC_FILE_NAME = "bikeHist.txt";

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
