package de.egh.bikehist;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.UUID;

import de.egh.bikehist.importing.ImportDialog;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.SaveDataService;
import de.egh.bikehist.sync.SyncController;
import de.egh.bikehist.ui.EmptyContentFragment;
import de.egh.bikehist.ui.HelpActivity;
import de.egh.bikehist.ui.ListCallbacks;
import de.egh.bikehist.ui.drawer.DrawerController;
import de.egh.bikehist.ui.event.EventContract;
import de.egh.bikehist.ui.event.EventDetailActivity;
import de.egh.bikehist.ui.event.EventDetailFragment;
import de.egh.bikehist.ui.event.EventListFragment;
import de.egh.bikehist.ui.masterdata.AdministratorActivity;


/**
 * TODO 150917 Notifications beim export sind englisch
 * TODO 150917 Tablet, EventEdit: Schrift übereinander. Screenshots ergänzen
 * TODO 150917 Auf tablet liegt das Verzeichnis nicht auf der Karte, steht aber so in der Anleitung
 * TODO Backstack from ImportDialog must update Drawer (deleted Tags already visible)
 * and: BAck navigation in import dialog must cancel SErvice
 * TODO Synchronisation: Broadcast mit Ergebnis abfangen
 * TODO Drawer nach Sync von TagTypes nicht aktualisiert
 */

public class MainActivity extends AppCompatActivity implements ListCallbacks,
		EventDetailFragment.Callbacks, DrawerController.Callbacks {
	public static final String EVENT_DETAIL_FRAGMENT_TAG = "eventDetailFragment";
	private static final String EVENT_LIST_FRAGMENT_TAG = "EVENT_LIST_FRAGMENT_TAG";
	private static final String IMPORT_DIALOG_TAG = "dialog";
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String PREF_KEY_INIT_APP = "PREF_KEY_INIT_APP";
	private static final int READ_REQUEST_CODE = 42;
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
	/**
	 * True, if in drawer mode (mDrawerLayout!=null) and drawer visible
	 */
	private boolean drawerOpen = false;
	private DrawerLayout mDrawerLayout;
	private boolean busyService;
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(ImportDialog.BROADCAST_ACTION_DATA_CHANGED)) {
				refreshUi();
			} else {
				if (intent.hasExtra(SaveDataService.Contract.ACTION)) {

					switch (intent.getStringExtra(SaveDataService.Contract.ACTION)) {

						case SaveDataService.Contract.GetStatus.NAME:
							//Service is busy, if action name was shipped.
							onServiceStatusUpdate(
									intent.getStringExtra(SaveDataService.Contract.GetStatus.Result.ACTIVE_ACTION),
									intent.getStringExtra(SaveDataService.Contract.GetStatus.Result.LAST_ACTION),
									(SyncController.StatisticReport) intent.getSerializableExtra(SaveDataService.Contract.GetStatus.Result.STATISTIC_REPORT)
							);
							break;

						// Sync has finished
						case SaveDataService.Contract.Sync.NAME:
						case SaveDataService.Contract.Export.NAME:
							onServiceStatusUpdate(
									intent.getStringExtra(SaveDataService.Contract.GetStatus.Result.ACTIVE_ACTION),
									intent.getStringExtra(SaveDataService.Contract.GetStatus.Result.LAST_ACTION),
									(SyncController.StatisticReport)
											intent.getSerializableExtra(SaveDataService.Contract.GetStatus.Result.STATISTIC_REPORT));
							break;
					}
				}
			}
		}
	};
	private ImportDialog importDialog;

	/**
	 * To be called, when Service became busy or idle.
	 *
	 * @param actionName      String with name of the running action or null, if no action name
	 *                        was not requested and service returned a result
	 * @param lastActionName  Can be null
	 * @param importStatistic Only for import actions, otherwise null
	 */
	private void onServiceStatusUpdate(String actionName, String lastActionName,
	                                   SyncController.StatisticReport importStatistic) {

		//Service finished an action: update activity
		busyService = !(actionName == null);
		refreshUi();


	}

//	private void onImportWriteResult(boolean success) {
//		importDialog.onImportWriteResult(success);
//	}

//	/**
//	 * Handles result of import file reading. *
//	 */
//	private void onImportReadResult(boolean success, SyncController.StatisticReport report) {
//		importDialog.onImportReadResult(success, report);
//	}

	private void refreshUi() {
		drawerController.onChange();
		showEventList();
		invalidateOptionsMenu();

	}


	/**
	 * Callback method from {@link de.egh.bikehist.ui.ListCallbacks}
	 * indicating that the event_item with the given EVENT_ID was selected.
	 * Does nothing, if Service is busy.
	 *
	 * @param id Item id or null for new Event
	 */
	@Override
	public void onItemSelected(UUID id, String type) {

		// Do nothing if syncing
		if (busyService)
			return;

//		if (mTwoPane) {
//			// In two-pane mode, show the detail view in this activity by
//			// adding or replacing the detail fragment using a
//			// fragment transaction.
//			EventDetailFragment fragment = new EventDetailFragment();
//
//			Bundle arguments = new Bundle();
//			if (id != null) {
//				arguments.putString(EventContract.EVENT_ID, id.toString());
//			} else {
//				arguments.putString(EventContract.BIKE_ID, drawerController.getSelectedBike().getId().toString());
//				arguments.putString(EventContract.TAG_TYPE_ID, drawerController.getSelectedTagType().getId().toString());
//			}
//			fragment.setArguments(arguments);
//			getSupportFragmentManager().beginTransaction()
//					.replace(R.id.mainDetailContainer, fragment, EVENT_DETAIL_FRAGMENT_TAG)
//					.commit();
//
//		} else {
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
//		}
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
		Cursor c = cr.query(BikeHistProvider.BikeHistContract.Tables.Bike.URI, null, where, args, null);
		if (c.getCount() == 0) {
			Bike bike1 = new Bike(UUID.randomUUID(), getString(R.string.bikeNameBike1),
					FRAME_NUMBER_BIKE_1, false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.Bike.URI,
					EntityUtilsFactory.createBikeUtils(this).build(bike1));

		}
		c.close();

		//----Create/add TagTypes once ----//
		TagType tagTypeMaintenance = null;
		TagType tagTypeTimeEvent = null;

		String tagTypeWhere = BikeHistProvider.BikeHistContract.Tables.TagType.Name.NAME + " =?";
		String[] tagTypeArgsMaintenance = {getString(R.string.tagTypeNameMaintenance)};
		c = cr.query(BikeHistProvider.BikeHistContract.Tables.TagType.URI, null, tagTypeWhere, tagTypeArgsMaintenance, null);
		if (c.getCount() == 0) {
			//--- Create both default TagTypes
			tagTypeMaintenance = new TagType(UUID.randomUUID(), tagTypeArgsMaintenance[0], false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.TagType.URI, EntityUtilsFactory.createTagTypeUtils(this).build(tagTypeMaintenance));
		}
		c.close();

		String[] tagTypeArgsTimeEvents = {getString(R.string.tagTypeNameTimeEvent)};
		c = cr.query(BikeHistProvider.BikeHistContract.Tables.TagType.URI, null, tagTypeWhere, tagTypeArgsTimeEvents, null);
		if (c.getCount() == 0) {
			tagTypeTimeEvent = new TagType(UUID.randomUUID(), tagTypeArgsTimeEvents[0], false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.TagType.URI, EntityUtilsFactory.createTagTypeUtils(this).build(tagTypeTimeEvent));
		} else {
			c.moveToFirst();
		}
		c.close();

		//----Create/add Tags once ----//
		if (tagTypeMaintenance != null) {
			Tag tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameChain), tagTypeMaintenance.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.Tag.URI, EntityUtilsFactory.createTagUtils(this).build(tag));

			tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameTyre), tagTypeMaintenance.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.Tag.URI, EntityUtilsFactory.createTagUtils(this).build(tag));
		}

		if (tagTypeTimeEvent != null) {
			Tag tag = new Tag(UUID.randomUUID(), getString(R.string.tagNameNewYear), tagTypeTimeEvent.getId(), false, System.currentTimeMillis());
			cr.insert(BikeHistProvider.BikeHistContract.Tables.Tag.URI, EntityUtilsFactory.createTagUtils(this).build(tag));
		}

//		//----Add an Event ----//
//		UUID id = UUID.randomUUID();
//		Event event = new Event(id, "UUID=" + id.toString(), System.currentTimeMillis(),
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_CHAIN, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.URI, Utils.buildEventContentValues(event));
//
//		id = UUID.randomUUID();
//		String trip = "test trip";
//		event = new Event(id, trip, System.currentTimeMillis() - 1000000,
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_START, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.URI, Utils.buildEventContentValues(event));
//
//		id = UUID.randomUUID();
//		event = new Event(id, trip, System.currentTimeMillis(),
//				Utils.getBikeByFrameNumber(FRAME_NUMBER_BIKE_1, lBikes).getId(),
//				Utils.getTagByName(TAG_END, lTags).getId(), null, System.currentTimeMillis(),
//				0, 0 //Transient fields
//		);
//
//		cr.insert(BikeHistProvider.URI, Utils.buildEventContentValues(event));


		// Never do this again
		SharedPreferences.Editor editor = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).edit();
		editor.putBoolean(PREF_KEY_INIT_APP, true);
		editor.apply();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		//Remove for production

		createDummyData();

		setContentView(R.layout.main);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.mainDrawerLayoutWidget);

		drawerController = new DrawerController(this, (ListView) findViewById(R.id.drawerBikeList),
				(ListView) findViewById(R.id.drawerTagTypeList),
				(ListView) findViewById(R.id.drawerTagList), this);


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
//					Log.d(TAG, "onDrawerClosed");
					super.onDrawerClosed(view);
					invalidateOptionsMenu();
//					showEventList();

					drawerOpen = false;

				}

				/** Called when a main has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
//					Log.d(TAG, "onDrawerOpened");
					super.onDrawerOpened(drawerView);

					getSupportActionBar().setTitle(R.string.app_name);
					invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
					drawerOpen = true;
				}
			};

			// Set the main toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);
		}

		if (mDrawerLayout != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(true);
		}

		IntentFilter filter = new IntentFilter(SaveDataService.Contract.INTENT_NAME);
		filter.addAction(ImportDialog.BROADCAST_ACTION_DATA_CHANGED);


		LocalBroadcastManager.getInstance(this).registerReceiver(
				broadcastReceiver, filter);

		//Trigger Service to Broadcast its status
		callServiceStatusRequest();


	}

	@Override
	protected void onResume() {
		super.onResume();

		showEventList();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void onStart() {
		super.onStart();
		drawerOpen = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).getBoolean(Constants.Prefs.KEY_DRAWER_OPEN, false);
		drawerController.onChange();

		if (drawerOpen && mDrawerLayout != null)
			mDrawerLayout.openDrawer(GravityCompat.START);


	}

	@Override
	protected void onStop() {
		super.onStop();
		drawerController.onStop();
		SharedPreferences.Editor editor = getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0).edit();
		editor.putBoolean(Constants.Prefs.KEY_DRAWER_OPEN, drawerOpen);
		editor.apply();
	}

	/**
	 * Only shows list, if Bike and Tag Type is selected in the drawer.
	 */
	private void showEventList() {
		FragmentManager fragmentManager = getSupportFragmentManager();

		//Show initial list, if missing selection
		if (drawerController.getSelectedBike() == null || drawerController.getSelectedTagType() == null) {
			//Remove existing Fragment with old list
			if (fragmentManager.findFragmentByTag(EVENT_LIST_FRAGMENT_TAG) != null)
				fragmentManager.beginTransaction()
						.replace(R.id.mainHeaderContainer, new EmptyContentFragment())
						.commit();
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

		boolean enable = !busyService && (!drawerOpen || mDrawerLayout == null);
		boolean enableActionCreateEvent = enable && hasSelection() &&
				el.tags(drawerController.getSelectedTagType()).size() > 0;

		menu.findItem(R.id.actionCreateEvent).setEnabled(enableActionCreateEvent);
		menu.findItem(R.id.actionCreateEvent).setVisible(enableActionCreateEvent);

		menu.findItem(R.id.actionConfiguration).setEnabled(enable);
		menu.findItem(R.id.actionConfiguration).setVisible(enable);

		// BuildConfig was set in app's build gradle
		if(BuildConfig.HAS_SYNC) {
			menu.findItem(R.id.actionSync).setEnabled(enable);
			menu.findItem(R.id.actionSync).setVisible(enable);
		}

		menu.findItem(R.id.actionExport).setEnabled(enable);
		menu.findItem(R.id.actionExport).setVisible(enable);

		menu.findItem(R.id.actionImport).setEnabled(enable);
		menu.findItem(R.id.actionImport).setVisible(enable);


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

			case R.id.actionSync:
				callSync();
				return true;

			case R.id.actionExport:
				callExport();
				return true;

			case R.id.actionImport:
				callImportDialog();
				return true;

			case R.id.actionHelp:
				callHelp();
				return true;

		}

		return super.

				onOptionsItemSelected(item);

	}

	private void callHelp() {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
	}

	/**
	 * Popup the import dialog.
	 */
	private void callImportDialog() {

		// DialogFragment.show() will take care of adding the fragment
		// in a transaction.  We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(IMPORT_DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);


		// Create and show the dialog.
		importDialog = new ImportDialog();
		importDialog.show(ft, IMPORT_DIALOG_TAG);


	}

	/**
	 * Starts synchronizing data.
	 */
	private void callSync() {

		//Lock edit functions
		busyService = true;
		invalidateOptionsMenu();

		Intent intent = new Intent(this, SaveDataService.class);
		intent.putExtra(SaveDataService.Contract.ACTION, SaveDataService.Contract.Sync.NAME);

		startService(intent);

	}

	/**
	 * Starts exporting data.
	 */
	private void callExport() {

		//Lock edit functions
		busyService = true;
		invalidateOptionsMenu();

		Intent intent = new Intent(this, SaveDataService.class);
		intent.putExtra(SaveDataService.Contract.ACTION, SaveDataService.Contract.Export.NAME);

		startService(intent);

	}

	/**
	 * TRiggers Service to broadcast its status (busy or idle)
	 */
	private void callServiceStatusRequest() {
		Intent intent = new Intent(this, SaveDataService.class);
		intent.putExtra(SaveDataService.Contract.ACTION, SaveDataService.Contract.GetStatus.NAME);

		startService(intent);

	}

	private void callPreferences() {
//		startActivity(new Intent(this, SettingsActivity.class));
	}

	/**
	 * Calls the master data activity for Tag Types
	 */
	private void callAdministration() {
		Intent intent = new Intent(this, AdministratorActivity.class);
		startActivity(intent);
	}

	@Override
	public void onEventChanged() {
		((EventListFragment) getSupportFragmentManager().findFragmentByTag(EVENT_LIST_FRAGMENT_TAG)).refresh();

//		FragmentManager fragmentManager = getSupportFragmentManager();
//		Fragment fragment = fragmentManager.findFragmentByTag(EVENT_DETAIL_FRAGMENT_TAG);
//		fragmentManager.beginTransaction()
//				.remove(fragment)
//				.commit();
	}

	@Override
	public void onDrawerControllerSelectionChanged() {
		showEventList();
	}

	private static class Constants {
		static class Prefs {
			static final String KEY_DRAWER_OPEN = "KEY_DRAWER_OPEN";
		}
	}
}
