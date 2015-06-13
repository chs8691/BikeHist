package de.egh.bikehist.ui;

import java.util.UUID;

/**
 A callback interface that all activities containing this fragment must
 implement. This mechanism allows activities to be notified of event_item
 selections.
 */
public interface ListCallbacks {
	/**
	 Callback for when an event_item has been selected.
	 @param id
	 @param type String with value from MasterDataContract.Type.Values
	 */
	public void onItemSelected(UUID id, String type);
}
