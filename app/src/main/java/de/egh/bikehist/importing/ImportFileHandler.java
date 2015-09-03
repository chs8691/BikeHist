package de.egh.bikehist.importing;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import de.egh.bikehist.sync.BikeHistSyncException;
import de.egh.bikehist.sync.JsonHelper;
import de.egh.bikehist.sync.SyncData;


/**
 * Import data and replace database from the local JSON-File. Only manages the file handling, but has no
 * synchronization logic.
 */
public class ImportFileHandler{

	public static final String TAG = ImportFileHandler.class.getSimpleName();
	private final Context context;
	private final Uri uri;
	private SyncData syncData;

	private final JsonHelper jsonHelper;

	private ImportFileHandler(Context context, Uri uri) {
		this.context = context;
		this.uri = uri;
		jsonHelper = new JsonHelper(context);
	}

	public void prepare() throws BikeHistSyncException {

		try {
			syncData = jsonHelper.read(context.getContentResolver().openInputStream(uri));
		} catch (IOException e) {
			throw new BikeHistSyncException(e.getMessage());
		}
	}

	public SyncData getData() {
		return syncData;
	}

}
