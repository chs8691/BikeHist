package de.egh.bikehist;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.UUID;

import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.SaveDataService;
import de.egh.bikehist.ui.ListCallbacks;
import de.egh.bikehist.ui.drawer.DrawerController;
import de.egh.bikehist.ui.event.EventContract;
import de.egh.bikehist.ui.event.EventDetailActivity;
import de.egh.bikehist.ui.event.EventDetailFragment;
import de.egh.bikehist.ui.event.EventListFragment;
import de.egh.bikehist.ui.masterdata.AdministratorActivity;

/**
 * TODO Differenz wird am falschen Objekt platziert
 * TODO Gelöschte Einträge halten, Flag
 * TODO Synchronisation
 * TODO Refresh baut Fragment neu auf; Erestzung durch Selektion
 */

public class MainActivity extends ActionBarActivity implements ListCallbacks, EventDetailFragment.Callbacks {


	public static final String EVENT_DETAIL_FRAGMENT_TAG = "eventDetailFragment";
	public static final String EVENT_LIST_FRAGMENT_TAG = "EVENT_LIST_FRAGMENT_TAG";
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String PREF_KEY_INIT_APP = "PREF_KEY_INIT_APP";
	/**
	 * Is null, if widget.DrawerLayout is null
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	/**
	 * Handles content of drawer an select event
	 */
	private DrawerController drawerController;
	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	private boolean drawerOpen = false;
	private DrawerLayout mDrawerLayout;


	@Override
	protected void onStop() {
		super.onStop();
		drawerController.onStop();
	}

	/**
	 * Callback method from {@link de.egh.bikehist.ui.ListCallbacks}
	 * indicating that the event_item with the given EVENT_ID was selected.
	 *
	 * @param id Item id or null for new Event
	 */
	@Override
	public void onItemSelected(UUID id, String type) {

		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			EventDetailFragment fragment = new EventDetailFragment();

			Bundle arguments = new Bundle();
			if (id != null) {
				arguments.putString(EventContract.EVENT_ID, id.toString());
			} else {
				arguments.putString(EventContract.BIKE_ID, drawerController.getSelectedBike().getId().toString());
				arguments.putString(EventContract.TAG_TYPE_ID, drawerController.getSelectedTagType().getId().toString());
			}
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.mainDetailContainer, fragment, EVENT_DETAIL_FRAGMENT_TAG)
					.commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected event_item EVENT_ID.
			Intent detailIntent = new Intent(this, EventDetailActivity.class);
			if (id != null) {
				detailIntent.putExtra(EventContract.EVENT_ID, id.toString());
			} else {
				detailIntent.putExtra(EventContract.BIKE_ID, drawerController.getSelectedBike().getId().toString());
				detailIntent.putExtra(EventContract.TAG_TYPE_ID, drawerController.getSelectedTagType().getId().toString());
			}

			startActivity(detailIntent);
		}
	}

	/**
	 * Initialize app's data once.
	 */
	private void createDummyData() {

		//Do this only once
		if (getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).getBoolean(PREF_KEY_INIT_APP, false)) {
			return;
		}


		ContentResolver cr = getContentResolver();

		//----Create/add Bikes once ----//
		//Create Bike

		final String FRAME_NUMBER_BIKE_1 = "GERMANIA-448010";
		String[] args = {FRAME_NUMBER_BIKE_1};
		String where = BikeHistProvider.BikeHistContract.Tables.Bike.FrameNumber.NAME + " =?";
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, where, args, null);
		if (c.getCount() == 0) {
			Bike bike1 = new Bike(UUID.randomUUID(), getString(R.string.bikeNameBike1), FRAME_NUMBER_BIKE_1, false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_BIKES, EntityUtilsFactory.createBikeUtils(this).build(bike1));
		}
		c.close();

		//----Create/add TagTypes once ----//
		TagType tagTypeMaintenance = null;
		TagType tagTypeTimeEvent = null;

		String tagTypeWhere = BikeHistProvider.BikeHistContract.Tables.TagType.Name.NAME + " =?";
		String[] tagTypeArgsMaintenance = {getString(R.string.tagTypeNameMaintenance)};
		c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, tagTypeWhere, tagTypeArgsMaintenance, null);
		if (c.getCount() == 0) {
			//--- Create both default TagTypes
			tagTypeMaintenance = new TagType(UUID.randomUUID(), tagTypeArgsMaintenance[0], false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_TAG_TYPES, EntityUtilsFactory.createTagTypeUtils(this).build(tagTypeMaintenance));
		}
		c.close();

		String[] tagTypeArgsTimeEvents = {getString(R.string.tagTypeNameTimeEvent)};
		c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, tagTypeWhere, tagTypeArgsTimeEvents, null);
		if (c.getCount() == 0) {
			tagTypeTimeEvent = new TagType(UUID.randomUUID(), tagTypeArgsTimeEvents[0], false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_TAG_TYPES, EntityUtilsFactory.createTagTypeUtils(this).build(tagTypeTimeEvent));
		} else {
			c.moveToFirst();
		}
		c.close();

		//----Create/add Tags once ----//
		if (tagTypeMaintenance != null) {
			Tag tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameChain), tagTypeMaintenance.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, EntityUtilsFactory.createTagUtils(this).build(tag));

			tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameTyre), tagTypeMaintenance.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, EntityUtilsFactory.createTagUtils(this).build(tag));
		}

		if (tagTypeTimeEvent != null) {
			Tag tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameNewYear), tagTypeTimeEvent.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.CONTENT_URI_TAGS, EntityUtilsFactory.createTagUtils(this).build(tag));
		}

//		//----Add an Event ----//
//		UUID id = UUID.randomUUID();
//		Event event = new Event(id, "UUID=" + id.toString(), System.currentTimeMillis(),
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_CHAIN, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));
//
//		id = UUID.randomUUID();
//		String trip = "test trip";
//		event = new Event(id, trip, System.currentTimeMillis() - 1000000,
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_START, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));
//
//		id = UUID.randomUUID();
//		event = new Event(id, trip, System.currentTimeMillis(),
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_END, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.CONTENT_URI_EVENTS, Utils.buildEventContentValues(event));


		// Never do this again
		SharedPreferences.Editor editor = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).edit();
		editor.putBoolean(PREF_KEY_INIT_APP, true);
		editor.apply();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		//Remove for production

		createDummyData();

		setContentView(R.layout.main);

		drawerController = new DrawerController(this, (ListView) findViewById(R.id.drawerBikeList),
				(ListView) findViewById(R.id.drawerTagTypeList),
				(ListView) findViewById(R.id.drawerTagList));

		mDrawerLayout = (DrawerLayout) findViewById(R.id.mainDrawerLayoutWidget);

		//landscape has no android.support.v4.widget.DrawerLayout
		if (mDrawerLayout != null) {


			mDrawerToggle = new ActionBarDrawerToggle(
					this,                  /* host Activity */
					mDrawerLayout,         /* DrawerLayout object */
					R.string.drawerOpen,  /* "open main" description */
					R.string.drawerClose  /* "close main" description */
			) {

				/** Called when a main has settled in a completely closed state. */
				public void onDrawerClosed(View view) {
					Log.d(TAG, "onDrawerClosed");
					super.onDrawerClosed(view);
					invalidateOptionsMenu();
					showEventList();

					drawerOpen = false;

				}

				/** Called when a main has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
					Log.d(TAG, "onDrawerOpened");
					super.onDrawerOpened(drawerView);

					getSupportActionBar().setTitle(R.string.app_name);
					invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
					drawerOpen = true;
				}
			};

			// Set the main toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);
		}


		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		if (findViewById(R.id.mainDetailContainer) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;


		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		drawerOpen = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).getBoolean(Constants.Prefs.KEY_DRAWER_OPEN, false);
		drawerController.onChange();
		showEventList();
	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences.Editor editor = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).edit();
		editor.putBoolean(Constants.Prefs.KEY_DRAWER_OPEN, drawerOpen);
		editor.apply();


	}

	/**
	 * Only shows list, if Bike and Tag Type is selected in the drawer.
	 */
	private void showEventList() {

		if (drawerController.getSelectedBike() == null || drawerController.getSelectedTagType() == null) {
			return;
		}
		//Standard case: bike and Type exist and are selected
		getSupportActionBar().setTitle(drawerController.getSelectedBike().getName());

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
				.replace(R.id.mainHeaderContainer, fragment, EVENT_LIST_FRAGMENT_TAG)
				.commit();
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
//		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);
		EntityLoader el = new EntityLoader(this);

		boolean enableActionCreateEvent = !drawerOpen && hasSelection() &&
				el.tags(drawerController.getSelectedTagType()).size() > 0;

		menu.findItem(R.id.actionCreateEvent).setEnabled(enableActionCreateEvent);
		menu.findItem(R.id.actionCreateEvent).setVisible(enableActionCreateEvent);

		menu.findItem(R.id.actionConfiguration).setEnabled(!drawerOpen);
		menu.findItem(R.id.actionConfiguration).setVisible(!drawerOpen);

		menu.findItem(R.id.actionSync).setEnabled(!drawerOpen);
		menu.findItem(R.id.actionSync).setVisible(!drawerOpen);

		menu.findItem(R.id.actionRefresh).setEnabled(drawerOpen);
		menu.findItem(R.id.actionRefresh).setVisible(drawerOpen);


		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Returns true, if there is a bike and a Tag Type selected.
	 */
	private boolean hasSelection() {

		return drawerController.getSelectedBike() != null && drawerController.getSelectedTagType() != null;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerLayout != null) {
			mDrawerToggle.syncState();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (mDrawerLayout != null)
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
		// Handle action bar event_item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerLayout != null) {
			if (mDrawerToggle.onOptionsItemSelected(item)) {
				return true;
			}
		}

		switch (id)

		{
			case R.id.action_settings:
				callPreferences();
				return true;
			case R.id.actionConfiguration:
				callAdministration();
				return true;
			case R.id.actionCreateEvent:
				onItemSelected(null, null);
				return true;
			case R.id.actionRefresh:
				if (mDrawerLayout != null) {
					mDrawerLayout.closeDrawer(Gravity.START);
				}
				showEventList();
				return true;
			case R.id.actionSync:
				callSync();
				return true;
		}

		return super.

				onOptionsItemSelected(item);

	}

	/**
	 * Starts exporting data.
	 */
	private void callSync() {
		Intent intent = new Intent(this, SaveDataService.class);
		intent.putExtra(SaveDataService.Contract.ACTION, SaveDataService.Contract.Export.NAME);

		startService(intent);

	}

	private void callPreferences() {
		startActivity(new Intent(this, SettingsActivity.class));
	}

	/**
	 * Calls the master data activity for Tag Types
	 */
	private void callAdministration() {
		Intent intent = new Intent(this, AdministratorActivity.class);
		startActivity(intent);
	}

	@Override
	public void onChanged() {
		((EventListFragment) getSupportFragmentManager().findFragmentByTag(EVENT_LIST_FRAGMENT_TAG)).refresh();

		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(EVENT_DETAIL_FRAGMENT_TAG);
		fragmentManager.beginTransaction()
				.remove(fragment)
				.commit();
	}

	private static class Constants {
		static class Prefs {
			static final String KEY_DRAWER_OPEN = "KEY_DRAWER_OPEN";
		}
	}
}
