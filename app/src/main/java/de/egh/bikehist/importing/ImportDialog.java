package de.egh.bikehist.importing;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.egh.bikehist.R;
import de.egh.bikehist.importing.ImportService.Status;

/**
 * User control for importing entity data from an external source
 * For SDK < 19 there must be an file app like OI File to pick up the file.
 */
public class ImportDialog extends DialogFragment {
	private static final int READ_REQUEST_CODE = 42;
	private static final String TAG = ImportDialog.class.getSimpleName();
	public static final String BROADCAST_ACTION_DATA_CHANGED = "de.egh.bikehist.ImportDialog.DATA_CHANGED";
	private Button importButton;
	private Button closeButton;
	private EditText fileNameView;
	private ProgressBar progressBar;
	TextView bikesValue;
	TextView tagTypesValue;
	TextView tagsValue;
	private TextView messageView;
	private TextView statusView;
	TextView eventsValue;
	private TextView nrBikesView;
	private TextView nrTagTypesView;
	private TextView nrTagsView;
	private TextView nrEventsView;
	private ImportService.ImportManager importManager;
	/**
	 * Marker with Uri, if binding was slower than onActivityResult. But normal it's null
	 */
	private Uri startReadingUri = null;
	private final ImportService.Callbacks importCallbacks = new ImportService.Callbacks() {
		@Override
		public void onStatusChanged(Status status) {
			onStatusUpdate(importManager.getStatus());
		}
	};

	/**
	 * Defines callbacks for importManager binding, passed to bindService()
	 */
	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
		                               IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			ImportService.LocalBinder binder = (ImportService.LocalBinder) service;
			importManager = binder.getService();

			onStatusUpdate(importManager.getStatus());

			importManager.setCallbacks(importCallbacks);

			if (startReadingUri != null) {
				//Copy Uri before deleting it here
				importManager.startReading(startReadingUri.buildUpon().build());
				startReadingUri = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			importManager.setCallbacks(null);
			importManager = null;
		}
	};



	private void onStatusUpdate(Status status) {
		statusView.setText(status.toString());
		updateStatistic(importManager.getStatisticValues());
		switch (status) {
			case READING:
				importButton.setVisibility(View.VISIBLE);
				importButton.setEnabled(false);
				closeButton.setVisibility(View.GONE);
				updateFileNameText();
				progressBar.setIndeterminate(true);
				messageView.setText(getString(R.string.importDialogMessageReading));
				break;

			case READ_FAILED:
				importButton.setVisibility(View.VISIBLE);
				importButton.setEnabled(false);
				closeButton.setVisibility(View.GONE);
				updateFileNameText();
				progressBar.setIndeterminate(false);
				messageView.setText(getString(R.string.importDialogMessageReadingFailed));
				break;

			case READ_OK:
				importButton.setVisibility(View.VISIBLE);
				importButton.setEnabled(true);
				closeButton.setVisibility(View.GONE);
				updateFileNameText();
				progressBar.setIndeterminate(false);
				messageView.setText(getString(R.string.importDialogMessageReadingOk));
				updateStatistic(importManager.getStatisticValues());
				break;

			case WRITING:
				importButton.setVisibility(View.VISIBLE);
				importButton.setEnabled(false);
				closeButton.setVisibility(View.GONE);
				updateFileNameText();
				progressBar.setIndeterminate(true);
				messageView.setText(getString(R.string.importDialogMessageWrting));
				break;

			case WRITE_NOK:
				importButton.setVisibility(View.GONE);
				closeButton.setVisibility(View.VISIBLE);
				closeButton.setEnabled(true);
				updateFileNameText();
				progressBar.setIndeterminate(false);
				messageView.setText(getString(R.string.importDialogMessageWritingFailed));
				break;

			case WRITE_OK:
				importButton.setVisibility(View.GONE);
				closeButton.setVisibility(View.VISIBLE);
				closeButton.setEnabled(true);
				updateFileNameText();
				progressBar.setIndeterminate(false);
				messageView.setText(getString(R.string.importDialogMessageWriteOk));
				break;

			default:
				importButton.setVisibility(View.VISIBLE);
				importButton.setEnabled(false);
				closeButton.setVisibility(View.GONE);
				updateFileNameText();
				progressBar.setIndeterminate(false);
				messageView.setText(getString(R.string.importDialogMessageValueDefault));
				break;
		}
	}

	private void updateStatistic(ImportService.StatisticValues statisticValues) {
		nrBikesView.setText("" + statisticValues.getNrBikes());
		nrTagTypesView.setText("" + statisticValues.getNrTagTypes());
		nrTagsView.setText("" + statisticValues.getNrTags());
		nrEventsView.setText("" + statisticValues.getNrEvents());
	}

	@Override
	public void onPause() {

		if (importManager != null) {
			getActivity().unbindService(mConnection);
		}

		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		Intent intent = new Intent(getActivity(), ImportService.class);
		getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);


	}

	/**
	 * Fires an intent to spin up the "file chooser" UI and select an image.
	 */
	private void performFileSearch() {

		//Only newer SDK has the cool Open Document
		if(Build.VERSION.SDK_INT >= 19){
		// ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
		// browser.
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

		// Filter to only show results that can be "opened", such as a
		// file (as opposed to a list of contacts or timezones)
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		// Filter to show only images, using the image MIME data type.
		// If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
		// To search for all documents available via installed storage providers,
		// it would be "*/*".
		intent.setType("text/plain");
		startActivityForResult(intent, READ_REQUEST_CODE);}

		else
		//Oder SDK
		{
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("text/plain");
			startActivityForResult(intent, READ_REQUEST_CODE);

		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		// The ACTION_OPEN_DOCUMENT intent was sent with the request code
		// READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
		// response to some other intent, and the code below shouldn't run at all.

		if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			// The document selected by the user won't be returned in the intent.
			// Instead, a URI to that document will be contained in the return intent
			// provided to this method as a parameter.
			// Pull that URI using resultData.getData().

			if (resultData != null) {
				Log.i(TAG, "Uri: " + resultData.getData().toString());

				//Maybe after configuration change, binding has not been done up to here
				if (importManager == null) {
					startReadingUri = resultData.getData();
				} else {
					startReadingUri = null;
					importManager.startReading(resultData.getData());
				}
			}
		}
	}

	private void updateFileNameText() {
		if (importManager.getUri() != null) {
			fileNameView.setText(importManager.getUri().getLastPathSegment());
		} else {
			fileNameView.setText(R.string.importDialogFileNameDefaultValue);
		}
	}

	/**
	 * Call this before the import dialog will be closed by the user.
	 */
	private void closeDialog() {

		broadcastDataChanged();
		if (importManager != null) {
			importManager.reset();
			importManager.stopService();
		}

		getDialog().dismiss();
	}

	/**
	 * Broadcast Result for Request contract.GetRunningTaskAction.
	 */
	private void broadcastDataChanged() {
		Intent intent = new Intent(BROADCAST_ACTION_DATA_CHANGED);
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.import_dialog, container, false);
		fileNameView = (EditText) v.findViewById(R.id.importDialogFileNameValue);
		importButton = (Button) v.findViewById(R.id.importDialogOkButton);
		closeButton = (Button) v.findViewById(R.id.importDialogCloseButton);
		progressBar = (ProgressBar) v.findViewById(R.id.importDialogProgressBar);
		messageView = (TextView) v.findViewById(R.id.importDialogMessageValue);
		statusView = (TextView) v.findViewById(R.id.importDialogStatusValue);
		nrBikesView = (TextView) v.findViewById(R.id.importDialogBikesValue);
		nrTagTypesView = (TextView) v.findViewById(R.id.importDialogTagTypesValue);
		nrTagsView = (TextView) v.findViewById(R.id.importDialogTagsValue);
		nrEventsView = (TextView) v.findViewById(R.id.importDialogEventsValue);


		fileNameView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				performFileSearch();
			}
		});

		getDialog().setTitle(getString(R.string.titleImportDialog));

		importButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				callImportWrite();
				importManager.startWriting();

			}
		});
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeDialog();
			}
		});
		messageView.setText(getString(R.string.importDialogMessageValueDefault));

		progressBar.setVisibility(View.INVISIBLE);

		importButton.setVisibility(View.VISIBLE);
		closeButton.setVisibility(View.GONE);
		importButton.setEnabled(false);


//		updateFileNameText();
		Intent intent = new Intent(getActivity(), ImportService.class);
		getActivity().startService(intent);

		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					closeDialog();
					return true;
				}
				return false;
			}
		});

		return v;
	}


}
