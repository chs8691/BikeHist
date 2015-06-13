package de.egh.bikehist;

import android.os.Environment;

/**
 Global constants.
 */
public final class AppUtils {
	public final class Prefs {

		public static final String PREF_NAME = "default";
		/**Name of the default directory for exporting data. Without path.*/
		public static final String DEFAULT_EXPORT_DIR = "BikeHist";

		/**Name must match with key in xml/preferences.xml*/
		public static final String PREF_EXPORT_DIRECTORY_KEY = "prefExportDirectory";

	}

	/**Static Class*/
	private AppUtils(){}

	/**Returns external path with directory as leaf.*/
	public static String getDefaultPath(){

		return Environment.getExternalStorageDirectory().toString() + "/" + AppUtils.Prefs.DEFAULT_EXPORT_DIR;
	}

}
