package de.egh.bikehist;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 Created by ChristianSchulzendor on 07.05.2015.
 */
public class SettingsActivity extends PreferenceActivity {

	private final String fragmentTag = "SETTINGS_FRAGMENT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment(), fragmentTag)
				.commit();
	}


}
