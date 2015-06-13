package de.egh.bikehist.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import de.egh.bikehist.R;

/**
 Created by ChristianSchulzendor on 30.04.2015.
 */
public class DeleteDialogFragment extends DialogFragment {
	public static final String ARG_MESSAGE = "message";
	private Callbacks callbacks;

	public void setCallbacks(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setMessage(getArguments().getString(ARG_MESSAGE))
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						callbacks.onClickOk();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User cancelled the dialog
					}
				});
		// Create the AlertDialog object and return it
		return builder.create();
	}

	/** For result events. */
	public interface Callbacks {
		public void onClickOk();
	}
}
