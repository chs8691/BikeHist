package de.egh.bikehist.masterdata;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import de.egh.bikehist.R;

/**
 An activity representing a single Item detail screen. This
 activity is only used on handset devices. On tablet-size devices,
 item details are presented side-by-side with a list of items
 in a {@link MasterDataListActivity}.
 <p/>
 This activity is mostly just a 'shell' activity containing nothing
 more than a {@link MasterDataDetailFragment}.
 */
public class MasterDataDetailActivity extends ActionBarActivity implements MasterDataDetailFragment.Callbacks {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_detail);

		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			MasterDataDetailFragment fragment = new MasterDataDetailFragment();
			Bundle arguments = new Bundle();
			// Type must be set
			arguments.putString(MasterDataContract.Type.NAME, getIntent().getStringExtra(MasterDataContract.Type.NAME));
			// Edit: ItemID must be set, otherwise Create new
			if (getIntent().hasExtra(MasterDataDetailFragment.Contract.Parameter.ITEM_ID)) {
				arguments.putString(MasterDataDetailFragment.Contract.Parameter.ITEM_ID,
						getIntent().getStringExtra(MasterDataDetailFragment.Contract.Parameter.ITEM_ID));
			}
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.master_data_item_detail_container, fragment)
					.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			navigateUpTo(new Intent(this, MasterDataListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onChanged() {
		finish();
	}
}
