package de.egh.bikehist.ui.event;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import de.egh.bikehist.MainActivity;
import de.egh.bikehist.R;


/**
 An activity representing a single Item detail screen. This
 activity is only used on handset devices. On tablet-size devices,
 event_item details are presented side-by-side with a list of items
 in a {@link de.egh.bikehist.MainActivity}.
 <p/>
 This activity is mostly just a 'shell' activity containing nothing
 more than a {@link de.egh.bikehist.ui.masterdata.MasterDataDetailFragment}.
 */
public class EventDetailActivity extends ActionBarActivity implements EventDetailFragment.Callbacks {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_detail_container);

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

			EventDetailFragment fragment = new EventDetailFragment();
			Bundle arguments = new Bundle();
			//Edit an existing Event
			if (getIntent().hasExtra(EventContract.EVENT_ID)) {
				arguments.putString(EventContract.EVENT_ID, getIntent().getStringExtra(EventContract.EVENT_ID));
			}
			if (getIntent().hasExtra(EventContract.BIKE_ID)) {
				arguments.putString(EventContract.BIKE_ID, getIntent().getStringExtra(EventContract.BIKE_ID));
			}
			if (getIntent().hasExtra(EventContract.TAG_TYPE_ID)) {
				arguments.putString(EventContract.TAG_TYPE_ID, getIntent().getStringExtra(EventContract.TAG_TYPE_ID));
			}
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.event_detail_container, fragment)
					.commit();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This EVENT_ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			navigateUpTo(new Intent(this, MainActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onEventChanged() {
		finish();
	}
}
