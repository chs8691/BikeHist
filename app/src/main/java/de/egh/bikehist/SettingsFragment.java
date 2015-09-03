package de.egh.bikehist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

/**
 Created by ChristianSchulzendor on 07.05.2015.
 */
public class SettingsFragment extends PreferenceFragment {
	static final int PICKFILE_RESULT_CODE = 1;
	private SharedPreferences prefs;
	private Preference connectionPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = getActivity().getSharedPreferences(AppUtils.Prefs.PREF_NAME, 0);
		// Load the preferences from an XML resource
//		addPreferencesFromResource(R.xml.preferences);

//		((Preference) findPreference(AppUtils.Prefs.PREF_EXPORT_DIRECTORY_KEY))
//				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//
//					@Override
//					public boolean onPreferenceClick(Preference preference) {
//
////						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////						intent.setType("file/*");
////						startActivityForResult(intent, PICKFILE_RESULT_CODE);
//
//
//						Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
//						Uri uri = Uri.withAppendedPath(Uri.parse("file:"), prefs.getString(AppUtils.Prefs.PREF_EXPORT_DIRECTORY_KEY, AppUtils.Prefs.DEFAULT_EXPORT_DIR));
//						intent.setData(uri);
//						intent.putExtra("org.openintents.extra.TITLE", getActivity().getString(R.string.directoryPickerTitle));
//						intent.putExtra("org.openintents.extra.BUTTON_TEXT", getActivity().getString(R.string.directoryPickerButton));
//						startActivityForResult(intent, 1);
//
//						return false;
//					}
//				});

	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
//			case SettingsFragment.PICKFILE_RESULT_CODE:
//				if (data != null)
//					setExportDirectory(Uri.parse(data.getDataString()).getPath());
//				break;
			default:
				break;
		}

		super.onActivityResult(requestCode, resultCode, data);

	}

//	public void setExportDirectory(String path) {
//
//		// Set summary to be the user-description for the selected value
//		connectionPref.setSummary(path);
//		SharedPreferences.Editor editor = prefs.edit();
//		editor.putString(AppUtils.Prefs.PREF_EXPORT_DIRECTORY_KEY, path);
//		editor.apply();
//	}
}
