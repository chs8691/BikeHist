package de.egh.bikehist;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.masterdata.MasterDataListActivity;
import de.egh.bikehist.masterdata.MasterDataContract;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.ui.EmptyContentFragment;
import de.egh.bikehist.ui.EventListFragment;
import de.egh.bikehist.ui.drawer.DrawerController;


public class MainActivity extends ActionBarActivity {

	public static final String TAG = MainActivity.class.getSimpleName();
	public static final String TAG_TYPE_POSITION = "Position";
	public static final String TAG_TYPE_MAINTENANCE = "Maintenance";
	public static final String TAG_CHAIN = "Chain";
	public static final String TAG_START = "Start";
	public static final String TAG_END = "End";
	private ActionBarDrawerToggle mDrawerToggle;

	/** Handles content of drawer an select event */
	private DrawerController drawerController;

	@Override
	protected void onStop() {
		super.onStop();
		drawerController.onStop();
	}

	/** Only for development. */
	private void createDummyData() {

		// Temp. list of bikes for this builder
		List<Bike> lBikes = new ArrayList<>();
		List<Tag> lTags = new ArrayList<>();
		List<TagType> lTagTypes = new ArrayList<>();

		ContentResolver cr = getContentResolver();

		//----Create/add Bikes once ----//
		//Create Brompton
		final String FRAME_NUMBER_MY_BROMPTON = "BROMPTON-448010";
		String where = BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER + " =?";
		String[] args = {FRAME_NUMBER_MY_BROMPTON};

		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, where, args, null);
		if (c.getCount() == 0) {
			Bike brompton = new Bike(UUID.randomUUID(), "Brompton", FRAME_NUMBER_MY_BROMPTON);
			lBikes.add(brompton);
			cr.insert(BikeHistProvider.CONTENT_URI_BIKES, Utils.buildBikeContentValues(brompton));
		} else {
			c.moveToFirst();
			lBikes.add(Utils.buildBikeFromCursor(c));
		}
		c.close();

		//Create
		final String FRAME_NUMBER_DEV = "DEV-1";
		String devBikeWhere = BikeHistProvider.BikeHistContract.Tables.Bike.Columns.Name.FRAME_NUMBER + " =?";
		String[] devBikeArgs = {FRAME_NUMBER_DEV};
		c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, devBikeWhere, devBikeArgs, null);
		if (c.getCount() == 0) {
			Bike devBike = new Bike(UUID.randomUUID(), "DEV Device", FRAME_NUMBER_DEV);
			lBikes.add(devBike);
			cr.insert(BikeHistProvider.CONTENT_URI_BIKES, Utils.buildBikeContentValues(devBike));
		} else {
			c.moveToFirst();
			lBikes.add(Utils.buildBikeFromCursor(c));
		}
		c.close();

		//----Create/add TagTypes once ----//
		String tagTypeWhere = BikeHistProvider.BikeHistContract.Tables.TagType.Columns.Name.NAME + " =?";
		String[] tagTypeArgs = {TAG_TYPE_MAINTENANCE};

		c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, tagTypeWhere, tagTypeArgs, null);
		if (c.getCount() == 0) {
			TagType tagType = new TagType(UUID.randomUUID(), TAG_TYPE_MAINTENANCE);
			lTagTypes.add(tagType);
			cr.insert(BikeHistProvider.CONTENT_URI_TAG_TYPES, Utils.buildTagTypeContentValues(tagType));
		} else {
			c.moveToFirst();
			lTagTypes.add(Utils.buildTagTypeFromCursor(c));
		}
		c.close();

		tagTypeArgs[0] = TAG_TYPE_POSITION;
		c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, tagTypeWhere, tagTypeArgs, null);
		if (c.getCount() == 0) {
			TagType tagType = new TagType(UUID.randomUUID(), TAG_TYPE_POSITION);
			lTagTypes.add(tagType);
			cr.insert(BikeHistProvider.CONTENT_URI_TAG_TYPES, Utils.buildTagTypeContentValues(tagType));
		} else {
			c.moveToFirst();
			lTagTypes.add(Utils.buildTagTypeFromCursor(c));
		}
		c.close();

		//----Create/add Tags once ----//
		String tagWhere = BikeHistProvider.BikeHistContract.Tables.Tag.Columns.Name.NAME + " =?";
		String[] tagArgs = {"Chain"};

		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, tagWhere, tagArgs, null);
		if (c.getCount() == 0) {
			Tag tag = new Tag(UUID.randomUUID(), tagArgs[0], Utils.getTagTypeByName(TAG_TYPE_MAINTENANCE, lTagTypes).getId());
			lTags.add(tag);
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, Utils.buildTagContentValues(tag));
		} else {
			c.moveToFirst();
			lTags.add(Utils.buildTagFromCursor(c));
		}
		c.close();

		tagArgs[0] = "Tyre";

		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, tagWhere, tagArgs, null);
		if (c.getCount() == 0) {
			Tag tag = new Tag(UUID.randomUUID(), tagArgs[0], Utils.getTagTypeByName(TAG_TYPE_MAINTENANCE, lTagTypes).getId());
			lTags.add(tag);
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, Utils.buildTagContentValues(tag));
		} else {
			c.moveToFirst();
			lTags.add(Utils.buildTagFromCursor(c));
		}
		c.close();

		tagArgs[0] = TAG_START;

		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, tagWhere, tagArgs, null);
		if (c.getCount() == 0) {
			Tag tag = new Tag(UUID.randomUUID(), tagArgs[0], Utils.getTagTypeByName(TAG_TYPE_POSITION, lTagTypes).getId());
			lTags.add(tag);
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, Utils.buildTagContentValues(tag));
		} else {
			c.moveToFirst();
			lTags.add(Utils.buildTagFromCursor(c));
		}
		c.close();

		tagArgs[0] = TAG_END;

		c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, tagWhere, tagArgs, null);
		if (c.getCount() == 0) {
			Tag tag = new Tag(UUID.randomUUID(), tagArgs[0], Utils.getTagTypeByName(TAG_TYPE_POSITION, lTagTypes).getId());
			lTags.add(tag);
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, Utils.buildTagContentValues(tag));
		} else {
			c.moveToFirst();
			lTags.add(Utils.buildTagFromCursor(c));
		}
		c.close();

		//----Add an Event ----//
		UUID id = UUID.randomUUID();
		Event event = new Event(id, "UUID=" + id.toString(), System.currentTimeMillis(),
				Utils.getBikeByFrameNumber(FRAME_NUMBER_MY_BROMPTON, lBikes).getId(),
				Utils.getTagByName(TAG_CHAIN, lTags).getId(), null, System.currentTimeMillis(),
				0, 0 //Transient fields
		);

		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));

		id = UUID.randomUUID();
		String trip = "test trip";
		event = new Event(id, trip, System.currentTimeMillis() - 1000000,
				Utils.getBikeByFrameNumber(FRAME_NUMBER_MY_BROMPTON, lBikes).getId(),
				Utils.getTagByName(TAG_START, lTags).getId(), null, System.currentTimeMillis(),
				0, 0 //Transient fields
		);

		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));

		event = new Event(id, trip, System.currentTimeMillis(),
				Utils.getBikeByFrameNumber(FRAME_NUMBER_MY_BROMPTON, lBikes).getId(),
				Utils.getTagByName(TAG_END, lTags).getId(), null, System.currentTimeMillis(),
				0, 0 //Transient fields
		);

		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		//Remove for production
		createDummyData();

		setContentView(R.layout.drawer_layout);

		DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		drawerController = new DrawerController(this, (ListView) findViewById(R.id.drawerBikeList),
				(ListView) findViewById(R.id.drawerTagTypeList),
				(ListView) findViewById(R.id.drawerTagList));

		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.string.drawerOpen,  /* "open drawer_layout" description */
				R.string.drawerClose  /* "close drawer_layout" description */
		) {

			/** Called when a drawer_layout has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				Log.d(TAG, "onDrawerClosed");
				super.onDrawerClosed(view);
				invalidateOptionsMenu();
				showEventList();
			}

			/** Called when a drawer_layout has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				Log.d(TAG, "onDrawerOpened");
				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle(R.string.app_name);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		// Set the drawer_layout toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

	}

	@Override
	protected void onResume() {
		super.onResume();
		drawerController.reloadDate();
		showEventList();


	}

	private void showEventList() {

		//Both, bike and Type must be selected
		if (drawerController.getSelectedBike() == null || drawerController.getSelectedTagType() == null) {
			getSupportActionBar().setTitle(R.string.app_name);
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new EmptyContentFragment()).commit();
			return;
		}

		//Standard case: bike and Type exist and are selected
		getSupportActionBar().setTitle(drawerController.getSelectedBike().getName() + "/" + drawerController.getSelectedTagType().getName());

		//Strings with Tag-IDs
		ArrayList<String> tagIds = new ArrayList<>();
		for (Tag tag : drawerController.getSelectedTags()) {
			tagIds.add(tag.getId().toString());
		}

		// Create a new fragment
		Fragment fragment = new EventListFragment();
		Bundle args = new Bundle();
		args.putString(EventListFragment.Args.BIKE_ID, drawerController.getSelectedBike().getId().toString());
		args.putString(EventListFragment.Args.TAG_TYPE_ID, drawerController.getSelectedTagType().getId().toString());
		args.putStringArrayList(EventListFragment.Args.TAG_IDS, tagIds);
		fragment.setArguments(args);
//
//		// Insert the fragment by replacing any existing fragment
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.content_frame, fragment)
				.commit();
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
//		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);
//		menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
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

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (id) {
			case R.id.action_settings:
				return true;
			case R.id.actionBikes:
				callBikes();
				return true;
			case R.id.actionTags:
				callTags();
				return true;
			case R.id.actionTagTypes:
				callTagTypes();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/** Calls the master data activity for Bikes */
	private void callBikes() {
		Intent intent = new Intent(this, MasterDataListActivity.class);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.BIKE);
		startActivity(intent);
	}

	/** Calls the master data activity for Tag Types */
	private void callTagTypes() {
		Intent intent = new Intent(this, MasterDataListActivity.class);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG_TYPE);
		startActivity(intent);
	}

	/** Calls the master data activity for Tags */
	private void callTags() {
		Intent intent = new Intent(this, MasterDataListActivity.class);
		intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG);
		startActivity(intent);
	}


}
