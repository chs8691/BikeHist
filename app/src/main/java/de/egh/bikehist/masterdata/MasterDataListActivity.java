package de.egh.bikehist.masterdata;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

import de.egh.bikehist.R;

/**
 An activity representing a list of Items. This activity
 has different presentations for handset and tablet-size devices. On
 handsets, the activity presents a list of items, which when touched,
 lead to a {@link MasterDataDetailActivity} representing
 item details. On tablets, the activity presents the list of items and
 item details side-by-side using two vertical panes.
 <p/>
 The activity makes heavy use of fragments. The list of items is a
 {@link MasterDataListFragment} and the item details
 (if present) is a {@link MasterDataDetailFragment}.
 <p/>
 This activity also implements the required
 {@link MasterDataListFragment.Callbacks} interface
 to listen for item selections.
 */
public class MasterDataListActivity extends ActionBarActivity
		implements MasterDataListFragment.Callbacks, MasterDataDetailFragment.Callbacks {
	public static final String DETAIL_FRAGMENT = "detailFragment";
	private static final String TAG = MasterDataListActivity.class.getSimpleName();
	/**
	 Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 device.
	 */
	private boolean mTwoPane;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_master_data_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case R.id.actionMasterDataAdd:
				onItemSelected(null);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.master_data_activity_item_list);

		//Save the type for fragment and returning from fragment
		if (getIntent().getStringExtra(MasterDataContract.Type.NAME) != null) {
			SharedPreferences.Editor editor = getSharedPreferences(Contract.Preferences.NAME, 0).edit();
			editor.putString(Contract.Preferences.PREF_TYPE, getIntent().getStringExtra(MasterDataContract.Type.NAME));
			editor.apply();
		}

		//Returning from detail fragment
		String type = getSharedPreferences(Contract.Preferences.NAME, 0)
				.getString(Contract.Preferences.PREF_TYPE, MasterDataContract.Type.Values.BIKE);
		if (type.equals(MasterDataContract.Type.Values.BIKE)) {
			getSupportActionBar().setTitle(R.string.actionBikes);
		} else if (type.equals(MasterDataContract.Type.Values.TAG_TYPE)) {
			getSupportActionBar().setTitle(R.string.actionTagTypes);
		} else if (type.equals(MasterDataContract.Type.Values.TAG)) {
			getSupportActionBar().setTitle(R.string.actionTags);
		} else {
			Log.e(TAG, "Unknown Master Data Type");
			finish();
		}

		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);


		if (findViewById(R.id.master_data_item_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			MasterDataListFragment itemListFragment;
			itemListFragment = ((MasterDataListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.master_data_item_list));
			itemListFragment.setActivateOnItemClick(true);
		}

		// TODO: If exposing deep links into your app, handle intents here.
	}

	/**
	 Callback method from {@link MasterDataListFragment.Callbacks}
	 indicating that the item with the given ID was selected.

	 @param id
	 Item id or null for new master data
	 */
	@Override
	public void onItemSelected(UUID id) {

		//Master Detail Type
		String type = getSharedPreferences(Contract.Preferences.NAME, 0)
				.getString(Contract.Preferences.PREF_TYPE, MasterDataContract.Type.Values.BIKE);

		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			MasterDataDetailFragment fragment = new MasterDataDetailFragment();
			if (id != null) {
				Bundle arguments = new Bundle();
				arguments.putString(MasterDataDetailFragment.Contract.Parameter.ITEM_ID, id.toString());
				arguments.putString(MasterDataContract.Type.NAME, type);
				fragment.setArguments(arguments);
			}
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.master_data_item_detail_container, fragment, DETAIL_FRAGMENT)
					.commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, MasterDataDetailActivity.class);
			detailIntent.putExtra(MasterDataContract.Type.NAME, type);
			if (id != null) {
				detailIntent.putExtra(MasterDataDetailFragment.Contract.Parameter.ITEM_ID, id.toString());
			}

			startActivity(detailIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		((MasterDataListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.master_data_item_list)).refreshUI();
	}

	@Override
	public void onChanged() {
		//Item was changed in detail edit mode
		((MasterDataListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.master_data_item_list)).refreshUI();

		if (mTwoPane) {
			getSupportFragmentManager().beginTransaction()
					.remove(getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT))
					.commit();
		}
	}

	/** Public Constants for Consumer */
	public static abstract class Contract {

		public static abstract class Preferences {

			static final String NAME = "PreferencesDefault";
			static final String PREF_TYPE = "type";
		}

	}
}
