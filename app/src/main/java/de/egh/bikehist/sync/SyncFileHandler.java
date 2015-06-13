package de.egh.bikehist.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Utils.EntityUtils;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.BikeHistProvider.BikeHistContract.Tables;

/**
 * Synchronize database with a local JSON-File. Only manages the file handling, but has no
 * synchronization logic.
 */
public class SyncFileHandler implements ExternalSyncSource {

	public static final String SYNC_FILE_NAME = "bikeHist.json";
	public static final String BACKUP_FILE_NAME = SYNC_FILE_NAME + ".bak";
	public static final String TMP_FILE_NAME = SYNC_FILE_NAME + ".tmp";
	public static final String TAG = SyncFileHandler.class.getSimpleName();
	private static final String FILE_SUFFIX = ".txt";
	private static final String FILE_TMP_SUFFIX = ".tmp";
	private static final String SYNC_DIR_NAME = "sync";
	private Context context;

	/**
	 * Sync-directory, may not be null
	 */
	private File syncDirFile = null;

	/**
	 * Sync source file, may not be null
	 */
	private File syncFile = null;

	/**
	 * Backup of the original sync source file, may not be null
	 */
	private File backupFile = null;

	/**
	 * Temp file, may not be null
	 */
	private File tmpFile = null;

	public SyncFileHandler(Context context) {
		this.context = context;
	}

	/**
	 * Starts synchronization.
	 *
	 * @throws Exception Synchronization failed.
	 */
	public void start() throws Exception {

		//First we need the sync directory
		if (!isExternalStorageWritable()) {
			throw new Exception(context.getString(R.string.messageExternalStorageNotWritable));
		}

		// Prepare directory structure: There must be a directory 'sync'
		prepareFiles();
		if (!syncDirFile.exists()) {
			throw new Exception(String.format("Could not create Sync dir %1$s", syncDirFile.getAbsoluteFile()));
		}

		// If there is no sync file, we just have to create files and export our entities
		if (!syncFile.exists()) {
			exportEntitiesToTmp(); //Without deleted items
		} else {
//			importSource();
//			mergeEntities();
//			exportToTmp(); //Without deleted items
		}
		//Rename sync file to bak, rename tmp to sync, delete bak
//		tmpToSyn();

		//Remove item in database with deleted eq true
//		removeDeletedItems();

	}

	/**
	 * Read all entities items from database and export them to the tmp file as json.
	 * Precondition: empty tmpFile exists
	 */
	private void exportEntitiesToTmp() {

		//Non-deleted Bikes
		List<Bike> bikes = new ArrayList<>();
		bikes = getAllUndeletedBikes();


		Log.v(TAG, "");

		;
	}

	public JsonWriter startJsonStream(OutputStream out) throws IOException {
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("  ");
		writer.beginObject();
		return writer;
	}

	public void closeJsonStream(JsonWriter writer) throws IOException {
		writer.endObject();
		writer.close();
	}

	public void writeBikeJsonStream(JsonWriter writer, List<Bike> bikes) throws IOException {
		writer.name(Tables.Bike.NAME);
		writer.beginArray();
		for (Bike bike : bikes) {
			writeBike(writer, bike);
		}
		writer.endArray();
	}

	public void writeBike(JsonWriter writer, Bike bike) throws IOException {
		writer.beginObject();
		writer.name(Tables.Bike.Id.NAME).value(bike.getId().toString());
		writer.name(Tables.Bike.Name.NAME).value(bike.getName());
		writer.name(Tables.Bike.TouchedAt.NAME).value(bike.getTouchedAt());
		writer.name(Tables.Bike.FrameNumber.NAME).value(bike.getFrameNumber());
		writer.endObject();
	}


	private List<Bike> getAllUndeletedBikes() {
		List<Bike> bikes = new ArrayList<>();
		EntityUtils<Bike> bikeUtils = EntityUtilsFactory.createBikeUtils(context);
		Cursor c = context.getContentResolver().query(BikeHistProvider.CONTENT_URI_BIKES,
				null, BikeHistProvider.BikeHistContract.Tables.Bike.Deleted.NAME + "=?",
				new String[]{"0"}, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			do {
				bikes.add(bikeUtils.build(c));
			} while (c.moveToNext());
		}
		return bikes;
	}

	/* Checks if external storage is available for read and write */
	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Create export directory, if not exists. Create files
	 */
	private void prepareFiles() {
		String folder_main = context.getString(R.string.app_name);

		//Create directory structure
		File fRootDir = new File(Environment.getExternalStorageDirectory(),
				folder_main);
		if (!fRootDir.exists()) {
			fRootDir.mkdirs();
		}
		File fSyncDir = new File(fRootDir, SYNC_DIR_NAME);
		if (!fSyncDir.exists()) {
			fSyncDir.mkdirs();
		}
		syncDirFile = fSyncDir;

		//Without the directory, we can't go on.
		if (!syncDirFile.exists()) return;

		//Access to the Source file
		syncFile = new File(fSyncDir.getPath(), SYNC_FILE_NAME);

		//Access to a new tmp file
		tmpFile = new File(fSyncDir.getPath(), TMP_FILE_NAME);
		if (tmpFile.exists()) {
			tmpFile.delete();
			tmpFile = new File(fSyncDir.getPath(), TMP_FILE_NAME);
		}


	}

	/**
	 * Writes all item of an particular Entiry type to an export file. Line by line.
	 */
	private void exportEntity(File folder, String prefix, EntityUtils utils) throws IOException {

		File endFile = new File(folder, prefix + utils.getEntityNamePlural() + FILE_SUFFIX);
		File tmpFile = new File(folder, endFile.getName() + FILE_TMP_SUFFIX);

		BufferedWriter br = new BufferedWriter(new FileWriter(tmpFile));

		//Get all Bikes
		Cursor c = context.getApplicationContext().getContentResolver().query(utils.getContentUri(), null, null, null, null);
		c.moveToFirst();
		do {
			br.write(utils.toExport(utils.build(c)));
			br.newLine();
		} while (c.moveToNext());
		br.close();

		tmpFile.renameTo(endFile);

	}

	/**
	 * Copies content from source to destination file. File must exists and my not be null.
	 */
	private void copyFile(File src, File dest) {
		try {
			FileInputStream in = new FileInputStream(src);
			FileOutputStream out = new FileOutputStream(dest);
			boolean end = false;
			do {
				int b = in.read();
				if (b == -1)
					end = true;
				else
					out.write(b);
			} while (!end);
			out.close();
			in.close();


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void prepare() throws BikeHistSyncException {
		String folder_main = context.getString(R.string.app_name);

		//Create directory structure
		File fRootDir = new File(Environment.getExternalStorageDirectory(),
				folder_main);
		if (!fRootDir.exists()) {
			fRootDir.mkdirs();
		}
		File fSyncDir = new File(fRootDir, SYNC_DIR_NAME);
		if (!fSyncDir.exists()) {
			fSyncDir.mkdirs();
		}
		syncDirFile = fSyncDir;

		//Without the directory, we can't go on.
		if (!syncDirFile.exists())
			throw new BikeHistSyncException(context.getString(R.string.messageMissingSyncDirectory)
					+ syncDirFile.getName());

		//Access to the Source file
		syncFile = new File(fSyncDir.getPath(), SYNC_FILE_NAME);
		accessFile(syncFile, false);

		//Make a backup file for the original content
		backupFile = new File(fSyncDir.getPath(), BACKUP_FILE_NAME);
		accessFile(backupFile, true);

		//Access to a new tmp file
		tmpFile = new File(fSyncDir.getPath(), TMP_FILE_NAME);
		accessFile(tmpFile, true);

		//Backup content
		copyFile(syncFile, backupFile);

	}

	/**
	 * Create if not exists and check if writeable. Set the timestamp to protect
	 * overriding by an file sync tool.
	 *
	 * @param f        File
	 * @param override true, if existing file has to be deleted
	 * @throws BikeHistSyncException
	 */
	private void accessFile(File f, boolean override) throws BikeHistSyncException {
		//Access to the Source file

		if (override) {
			if (f.exists())
				if (f.delete())
					throw new BikeHistSyncException(context.getString(R.string.messageCantDeleteFile)
							+ f.getName());
		}

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				throw new BikeHistSyncException(context.getString(R.string.messageCantCreateFile)
						+ f.getName());
			}
		}

		if (!f.canWrite())
			throw new BikeHistSyncException(String.format(context.getString(R.string.messageCantWriteFile),
					f.getName()));

		//TODO geht wohl nicht
//		if (!f.setLastModified(System.currentTimeMillis()))
//			throw new BikeHistSyncException(String.format(context.getString(R.string.messageCantWriteFile),
//				f.getName()));

	}
/**
 {
    "timestamp": 912345678901,
    "bikes":[
        {
			"id": "123-FG-ASDFA345ET-123",
            "deleted": "0",
            "touchedAt": "123456787980",
            "name": "Brompton",
            "frameNumber": "BROMPTON-123345",
        },
		 {
		 "id": "///-FG-ASDFA345ET-&&&",
		 "deleted": "0",
		 "touchedAt": "123456765433",
		 "name": "Giant",
		 "frameNumber": "GIANT-123345",
		 }
    ]
 }
 * */


	@Override
	public SyncData getData() {

		try {
			FileInputStream fis = new FileInputStream(SYNC_FILE_NAME);
			JsonReader reader = new JsonReader(new InputStreamReader(fis, "UTF-8"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}


		return null;
	}

	@Override
	public void putData(SyncData syncData) {

	}

	@Override
	public void commit() {

	}
}
