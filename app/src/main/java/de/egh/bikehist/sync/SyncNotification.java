package de.egh.bikehist.sync;

/**
 * Provide Notification for Synchronizing
 */
public interface SyncNotification {

	/**To be called at start of synchronizing. Creates Notification and StatusBar icon.*/
	abstract void create(String text);

	/**While syncing call this to change the text and ProgressBar. StatusBar icon is still
	 * visible.*/
	abstract void update(String text, int max, int progress);

	/** Call this at end of synchronizing. Updates text and hides StatusBar icon.*/
	abstract void finish(String text, String subText);

	/** Call this at unexpected end (e.g. thrown exception).  StatusBar icon will change.*/
	abstract void abort(String text);

}
