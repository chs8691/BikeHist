package de.egh.bikehist.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;

import de.egh.bikehist.R;


/**
 * Synchronize database with a local JSON-File. Only manages the file handling, but has no
 * synchronization logic.
 */
public class ExportFileHandler implements ExternalSyncSource {

	private final String exportFileName;
	private final String tmpFileName;
	public static final String TAG = ExportFileHandler.class.getSimpleName();
	private static final String EXPORT_DIR_NAME = "export";
	private final Context context;
	/**
	 * Sync-directory, may not be null
	 */
	private File syncDirFile = null;
	/**
	 * Sync source file, may not be null
	 */
	private File exportFile = null;
	/**
	 * Backup of the original sync source file, may not be null
	 */
	private File backupFile = null;
	/**
	 * Temp file, may not be null
	 */
	private File tmpFile = null;
	private File SyncDirF;

	private final JsonHelper jsonHelper;

	@SuppressLint("SimpleDateFormat")
	public ExportFileHandler(Context context) {
		this.context = context;
		jsonHelper = new JsonHelper(context);

		this.exportFileName = "bikeHist_" +
				new SimpleDateFormat("yyMMdd-HHmmss").format(System.currentTimeMillis()) + ".txt";
		this.tmpFileName = "bikeHist_tmp.txt";
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
	 * Copies content from source to destination file. Files must exists and my not be null.
	 */
	private void copyFile(File src, File dest) throws IOException {
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
		SyncDirF = new File(fRootDir, EXPORT_DIR_NAME);
		if (!SyncDirF.exists()) {
			SyncDirF.mkdirs();
		}
		syncDirFile = SyncDirF;

		//Without the directory, we can't go on.
		if (!syncDirFile.exists())
			throw new BikeHistSyncException(context.getString(R.string.messageMissingSyncDirectory)
					+ syncDirFile.getName());

		//Access to the Source file
		exportFile = new File(SyncDirF.getPath(), exportFileName);
		accessFile(exportFile, false);

		//Access to a new tmp file
		tmpFile = new File(SyncDirF.getPath(), tmpFileName);
		accessFile(tmpFile, true);

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
				if (!f.delete())
					throw new BikeHistSyncException(
							String.format(context.getString(R.string.messageCantDeleteFile),
									f.getName()));
		}

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				throw new BikeHistSyncException(
						String.format(context.getString(R.string.messageCantCreateFile),
								f.getName()));
			}
		}

		if (!f.canWrite())
			throw new BikeHistSyncException(
					String.format(context.getString(R.string.messageCantWriteFile),
							f.getName()));

		//TODO geht wohl nicht
//		if (!f.setLastModified(System.currentTimeMillis()))
//			throw new BikeHistSyncException(String.format(context.getString(R.string.messageCantWriteFile),
//				f.getName()));

	}


	/* Not used for export. */
	@Override
	public SyncData getData() {

		throw new IllegalStateException("Method not supported.");
	}

	/**
	 * Create temp file with this data.
	 */
	@Override
	public void putData(EntityContainer data) throws BikeHistSyncException {

		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new FileWriter(tmpFile));

			jsonHelper.write(br, data);

			commit();

		} catch (IOException e) {
			throw new BikeHistSyncException(e.getMessage());

		} finally {
			if (br != null)
				try {
					br.close();
				} catch (Exception e) {
					throw new BikeHistSyncException(e.getMessage());
				}
		}

	}


	private void commit() throws IOException {

		copyFile(tmpFile, exportFile);
		tmpFile.delete();

		//TODO Remove JSON Logging
		FileInputStream fis = new FileInputStream(exportFile);
		Reader reader = new InputStreamReader(fis);
		int r;
		while ((r = reader.read()) != -1) {
			char ch = (char) r;
			System.out.print(ch);
		}


// initiate media scan and put the new things into the path array to
// make the scanner aware of the location and the files you want to see
//		MediaScannerConnection.scanFile(context,
//				new String[]{tmpFile.getPath(),exportFile.getPath(),backupFile.getPath() }, null, null);
//


	}

}
