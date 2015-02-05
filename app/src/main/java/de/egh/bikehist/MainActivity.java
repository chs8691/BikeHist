package de.egh.bikehist;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.de.egh.bikehist.model.Bike;
import de.egh.bikehist.de.egh.bikehist.model.Event;
import de.egh.bikehist.de.egh.bikehist.model.Tag;
import de.egh.bikehist.de.egh.bikehist.model.TagType;
import de.egh.bikehist.de.egh.bikehist.model.Utils;
import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables.Event.Columns;

import static de.egh.bikehist.de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables.Event.Columns.*;

public class MainActivity extends ActionBarActivity {

	public static final String TAG = MainActivity.class.getSimpleName();
	public static final String TAG_TYPE_POSITION = "Position";
	public static final String TAG_TYPE_MAINTENANCE = "Maintenance";
	public static final String BIKE_BROMPTON = "Brompton";
	public static final String TAG_CHAIN = "Chain";
	public static final String TAG_CHAIN_WHEEL = "Chain wheel";
	public static final String TAG_START = "Start";
	public static final String TAG_END = "End";
	private String[] itemArray;
	private ListView listView;
	private ArrayList<Event> listItems;
	private ListItemAdapter aa;
	private List<Tag> tags;
	private List<TagType> tagTypes;
	private List<Bike> bikes;
	private List<Event> events;

	/** Helper for Dummy data */
	private static TagType getTagTypeByName(String name, List<TagType> list) {
		for (TagType entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;

	}

	/** Helper for Dummy data */
	private static Tag getTagByName(String name, List<Tag> list) {
		for (Tag entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;

	}

	/** Helper for Dummy data */
	private static Bike getBikeByName(String name, List<Bike> list) {
		for (Bike entry : list) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	/** Only for development. */
	private void createDummyData() {

		bikes = new ArrayList<>();
		bikes.add(new Bike(UUID.randomUUID(),BIKE_BROMPTON, "448010"));
		bikes.add(new Bike(UUID.randomUUID(), "DEV", "Device 0"));

		tagTypes = new ArrayList<>();
		tagTypes.add(new TagType(UUID.randomUUID(), TAG_TYPE_MAINTENANCE));
		tagTypes.add(new TagType(UUID.randomUUID(), TAG_TYPE_POSITION));

		tags = new ArrayList<>();
		tags.add(new Tag(
						UUID.randomUUID(), TAG_CHAIN, getTagTypeByName(TAG_TYPE_MAINTENANCE, tagTypes).getId())
		);
		tags.add(new Tag(
						UUID.randomUUID(), TAG_CHAIN_WHEEL, getTagTypeByName(TAG_TYPE_MAINTENANCE, tagTypes).getId())
		);
		tags.add(new Tag(
						UUID.randomUUID(), TAG_START, getTagTypeByName(TAG_TYPE_POSITION, tagTypes).getId())
		);
		tags.add(new Tag(
						UUID.randomUUID(), TAG_END, getTagTypeByName(TAG_TYPE_POSITION, tagTypes).getId())
		);

		events = new ArrayList<>();

		ContentResolver cr = getContentResolver();
//		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null, null, null, null);

		Event event = new Event(UUID.randomUUID(), "SRAM PC1", 100000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
				getTagByName(TAG_CHAIN, tags).getId(), null, System.currentTimeMillis());

		String where = Columns.Name.ID + " =?";
		String[] args = {event.getId().toString()};

		if (cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null, where, args, null).getCount()==0) {
			ContentValues values = Utils.buildContentValues(event);
			cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, values);
		}


//		events.add(new Event(UUID.randomUUID(), "SRAM PC1", 100000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_CHAIN, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), "Truvati 52", 200000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_CHAIN_WHEEL, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), TAG_START, 1000000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_START, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), TAG_END, 1010000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_END, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), TAG_START, 1020000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_START, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), TAG_END, 1030000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_END, tags).getId(), null, System.currentTimeMillis()));
//
//
//		events.add(new Event(UUID.randomUUID(), TAG_START, 1040000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_START, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), TAG_END, 1050000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_END, tags).getId(), null, System.currentTimeMillis()));
//
//		events.add(new Event(UUID.randomUUID(), "KMC Gold", 100000, getBikeByName(BIKE_BROMPTON, bikes).getId(),
//				getTagByName(TAG_CHAIN, tags).getId(), null, System.currentTimeMillis()));


	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.list);

		listItems = new ArrayList<Event>();

		int resID = R.layout.item;
		aa = new ListItemAdapter(this, resID, listItems);
		listView.setAdapter(aa);

		createDummyData();
		loadDataFromProvider();

		refreshUI();


	}

	private void loadDataFromProvider() {
		// Clear the existing earthquake array
		events.clear();

		ContentResolver cr = getContentResolver();

		// Return all the saved earthquakes
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null, null, null, null);

		if (c.moveToFirst()) {
			do {
				Event event = Utils.buildEventFromCursor(c);
				events.add(event);
//				addQuakeToArray(q);
			} while (c.moveToNext());
		}
	}

	private void refreshUI() {

		listItems.clear();
		for (Event event : events) {
			listItems.add(event);
		}

		aa.notifyDataSetChanged();


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
