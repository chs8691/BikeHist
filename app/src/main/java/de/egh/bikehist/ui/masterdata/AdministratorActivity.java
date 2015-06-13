/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.

TODO: Fragment um getActiveType erg�nzen und bei + abgragen

*/


package de.egh.bikehist.ui.masterdata;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.ui.ListCallbacks;
import de.egh.bikehist.ui.masterdata.MasterDataDetailFragment.Callbacks;


/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p/>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class AdministratorActivity extends ActionBarActivity implements ListCallbacks, AdministratorFragment.Callbacks, Callbacks {

	public static final String TAG = "MainActivity";
	public static final String ADMINISTRATOR_FRAGMENT = "AdministratorFragment";
	public static final int DETAILS_REQUEST_CODE = 1;
	private static final String DETAIL_FRAGMENT = "detailFragment";

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.administrator_activity);

		ActionBar bar = getSupportActionBar();
		bar.setTitle(getString(R.string.administration));

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			AdministratorFragment fragment = new AdministratorFragment();
			transaction.replace(R.id.administrator_activity_content, fragment, ADMINISTRATOR_FRAGMENT);
			transaction.commit();
		}

		if (findViewById(R.id.administratorActivityDetailContainer) != null) {
			mTwoPane = true;
		}

	}

	/**
	 * If twoPane: close detail
	 */
	@Override
	public void onBackPressed() {
		if (mTwoPane) {
			if (getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT) != null)
				getSupportFragmentManager().beginTransaction()
						.remove(getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT))
						.commit();
			else
				super.onBackPressed();
		} else
			super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.administration, menu);
		AdministratorFragment fragment =
				((AdministratorFragment) getSupportFragmentManager()
						.findFragmentByTag(ADMINISTRATOR_FRAGMENT));
		if (fragment != null) {
			menu.findItem(R.id.menuMasterDataActionCreate).setEnabled(fragment.isCreateActionPerformed());
			menu.findItem(R.id.menuMasterDataActionCreate).setVisible(fragment.isCreateActionPerformed());
		}
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu()
	 * Wird leider doch nicht aufgerufen. */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
//        AdministratorFragment fragment =
//                ((AdministratorFragment) getSupportFragmentManager()
//                        .findFragmentByTag(ADMINISTRATOR_FRAGMENT));
//        if (fragment != null) {
//            menu.findItem(R.id.menuMasterDataActionCreate).setEnabled(fragment.isCreateActionPerformed());
//            menu.findItem(R.id.menuMasterDataActionCreate).setVisible(fragment.isCreateActionPerformed());
//        }
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar event_item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case R.id.menuMasterDataActionCreate:

				onItemSelected(null, ((AdministratorFragment) getSupportFragmentManager()
						.findFragmentByTag(ADMINISTRATOR_FRAGMENT)).getActualEntityType());
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemSelected(UUID id, String type) {

		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			MasterDataDetailFragment fragment = new MasterDataDetailFragment();
			Bundle arguments = new Bundle();
			arguments.putString(MasterDataContract.Type.NAME, type);
			if (id != null) {
				arguments.putString(MasterDataDetailFragment.Contract.Parameter.ITEM_ID, id.toString());
			}
			fragment.setArguments(arguments);
			if (getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT) == null) {
				getSupportFragmentManager().beginTransaction()
						.add(R.id.administratorActivityDetailContainer, fragment, DETAIL_FRAGMENT)
						.commit();

			} else
				getSupportFragmentManager().beginTransaction()
						.remove(getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT))
						.add(R.id.administratorActivityDetailContainer, fragment, DETAIL_FRAGMENT)
						.commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected event_item EVENT_ID.
			Intent detailIntent = new Intent(this, MasterDataDetailActivity.class);
			detailIntent.putExtra(MasterDataContract.Type.NAME, type);
			if (id != null) {
				detailIntent.putExtra(MasterDataDetailFragment.Contract.Parameter.ITEM_ID, id.toString());
			}

			startActivityForResult(detailIntent, DETAILS_REQUEST_CODE);
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == DETAILS_REQUEST_CODE)
			onDetailChanged();
	}

	/**
	 * Handles closed detail pane.
	 */
	public void onDetailChanged() {
		//Item was changed in detail edit mode
		((AdministratorFragment) getSupportFragmentManager()
				.findFragmentByTag(ADMINISTRATOR_FRAGMENT)).refreshUI();

		if (mTwoPane) {
			getSupportFragmentManager().beginTransaction()
					.remove(getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT))
					.commit();
		}
	}


	@Override
	public void tabChanged() {

//        Log.v(TAG, "tabChanged()");
		invalidateOptionsMenu();

		if (mTwoPane && getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT) != null) {
			getSupportFragmentManager().beginTransaction()
					.remove(getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT))
					.commit();
		}

	}
}
