package de.egh.bikehist.ui.masterdata;

/**
 General Constants for Fragments and Activity. Use this as Intent Extras and Fragment Arguments.
 */
public class MasterDataContract {

	/** Type of master data */
	public static abstract class Type {
		/** NAME_STRING of Extra */
		public static final String NAME = "PARAMETERS_TYPE";

		/** Values for Extra TYPE */
		public static abstract class Values {
			/** Master data Bikes */
			public static final String BIKE = "PARAMETERS_TYPE_VALUES_BIKE";
			/** Master data Tags */
			public static final String TAG = "PARAMETERS_TYPE_VALUES_TAG";
			/** Master data Tag Types */
			public static final String TAG_TYPE = "PARAMETERS_TYPE_VALUES_TAG_TYPE";
		}
	}
}

