package de.egh.bikehist.ui.masterdata;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.SaveDataService;
import de.egh.bikehist.ui.DeleteDialogFragment;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link AdministratorActivity}
 * in two-pane mode (on tablets) or a {@link MasterDataDetailActivity}
 * on handsets.
 */
public class MasterDataDetailFragment extends Fragment {

	private static final String TAG = MasterDataDetailFragment.class.getSimpleName();
	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static final Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onDetailChanged() {
		}
	};
	/**
	 * The fragment's current callback object, which is notified of list event_item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.hasExtra(SaveDataService.Contract.ACTION)) {
				if (intent.getStringExtra(SaveDataService.Contract.ACTION).equals(SaveDataService.Contract.DeleteMasterDataAction.NAME)) {

					//Error handling
					if (intent.getBooleanExtra(SaveDataService.Contract.DeleteMasterDataAction.Result.ERROR, false)) {
						Toast.makeText(getActivity(), getString(R.string.messageDeleteError), Toast.LENGTH_LONG).show();
						return;
					}

					//Success reporting
					int nrItems = intent.getIntExtra(SaveDataService.Contract.DeleteMasterDataAction.Result.NO_MAIN_MASTER_DATA_DELETED, 0);
					int nrDependent = intent.getIntExtra(SaveDataService.Contract.DeleteMasterDataAction.Result.NO_DEPENDENT_ITEMS_TOUCHED, 0);
					String type = intent.getStringExtra(MasterDataContract.Type.NAME);
					if (type.equals(MasterDataContract.Type.Values.BIKE)) {
						//  Deleted %1$d bike with %2$d event(s).
						Toast.makeText(getActivity(), String.format(getString(R.string.messageDeleteBikeSuccess), nrItems, nrDependent), Toast.LENGTH_LONG).show();
					} else if (type.equals(MasterDataContract.Type.Values.TAG_TYPE)) {
						// Deleted %1$d tag type.
						Toast.makeText(getActivity(), String.format(getString(R.string.messageDeleteTagTypesSuccess), nrItems), Toast.LENGTH_LONG).show();
					} else if (type.equals(MasterDataContract.Type.Values.TAG)) {
						//  Deleted %1$d bike with %2$d event(s).
						Toast.makeText(getActivity(), String.format(getString(R.string.messageDeleteTagsSuccess), nrItems), Toast.LENGTH_LONG).show();
					}
					mCallbacks.onDetailChanged();
				}
			}
		}
	};
	private Bike bike;
	private TagType tagType;
	private Tag tag;
	private TextView name;
	private TextView frameNumber;
	private InputMethodManager imm;
	/**
	 * TRUE, if this entry doesn't exist on database.
	 */
	private boolean newEntry;
	private View rootView;
	private List<TagType> tagTypeSpinnerList;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public MasterDataDetailFragment() {
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}
		mCallbacks = (Callbacks) activity;

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_master_data_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		boolean showDeleteButton = !newEntry;

		if (tagType != null) {
			showDeleteButton = showDeleteButton && !hasTags(tagType);
		}

		if (tag != null) {
			showDeleteButton = showDeleteButton && !hasEvents(tag);
		}


		menu.findItem(R.id.actionMasterDataDelete).setEnabled(showDeleteButton);
		menu.findItem(R.id.actionMasterDataDelete).setVisible(showDeleteButton);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.actionMasterDataDelete:
				deleteItemQuestion();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Returns true, if there Tags  exists for the given TagType. Otherwise false.
	 */
	private boolean hasTags(TagType tagType) {

		if (tagType == null) {
			return false;
		}

		ContentResolver cr = getActivity().getContentResolver();
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAGS
				, BikeHistProvider.BikeHistContract.QUERY_COUNT_PROJECTION
				, BikeHistProvider.BikeHistContract.Tables.Tag.TagTypeId.NAME + "=?"
				, new String[]{tagType.getId().toString()}
				, null);
		if (c == null) {
			return false;
		} else {
			c.moveToFirst();
			int i = c.getInt(0);
			c.close();
			return i > 0;
		}
	}

	/**
	 * Returns true, if  Events exists for the given Tag. Otherwise false.
	 */
	private boolean hasEvents(Tag tag) {

		if (tag == null) {
			return false;
		}

		// Count entries
		ContentResolver cr = getActivity().getContentResolver();
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS
				, BikeHistProvider.BikeHistContract.QUERY_COUNT_PROJECTION
				, BikeHistProvider.BikeHistContract.Tables.Event.TagId.NAME + "=?"
				, new String[]{tag.getId().toString()}
				, null);
		c.moveToFirst();
		int cnt = c.getInt(0);
		c.close();
		return (cnt > 0);
	}

	/**
	 * Dialog for Confirmation
	 */
	private void deleteItemQuestion() {

		DeleteDialogFragment newFragment = new DeleteDialogFragment();
		Bundle args = new Bundle();
		String text;
		if (tagType != null) {
			text = getString(R.string.dialogTagTypeDeleteQuestion);
		} else if (tag != null)
			text = getString(R.string.dialogTagDeleteQuestion);
		else {
			text = getString(R.string.dialogBikeDeleteQuestion);
		}

		args.putString(DeleteDialogFragment.ARG_MESSAGE, text);
		newFragment.setArguments(args);
		newFragment.setCallbacks(new DeleteDialogFragment.Callbacks() {
			@Override
			public void onClickOk() {
				deleteItem();
			}
		});
		newFragment.show(getActivity().getSupportFragmentManager(), "delete");

	}

	/**
	 * Deletes event_item asynchronous.
	 */
	private void deleteItem() {
		Intent intent = new Intent(getActivity(), SaveDataService.class);
		intent.putExtra(SaveDataService.Contract.ACTION, SaveDataService.Contract.DeleteMasterDataAction.NAME);
		if (bike != null) {
			intent.putExtra(SaveDataService.Contract.DeleteMasterDataAction.Parameters.ITEM_ID, bike.getId().toString());
			intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.BIKE);
		} else if (tagType != null) {
			intent.putExtra(SaveDataService.Contract.DeleteMasterDataAction.Parameters.ITEM_ID, tagType.getId().toString());
			intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG_TYPE);
		} else if (tag != null) {
			intent.putExtra(SaveDataService.Contract.DeleteMasterDataAction.Parameters.ITEM_ID, tag.getId().toString());
			intent.putExtra(MasterDataContract.Type.NAME, MasterDataContract.Type.Values.TAG);
		}
		getActivity().startService(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		newEntry = true;

		//Needed for switching keyboard
		imm = (InputMethodManager) (getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE));

		if (getArguments().getString(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.BIKE)) {

			//Edit event_item
			if (getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				// Load the content specified by the fragment
				ContentResolver cr = getActivity().getContentResolver();

			/* uri            The URI, using the content:// scheme, for the content to retrieve.
	           projection     A list of which columns to return. Passing null will return all columns, which is inefficient.
			   selection      A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
			   selectionArgs  You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
			   sortOrder      How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.*/
				Cursor c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, //
						null, //
						BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME + "=?", //
						new String[]{getArguments().getString(Contract.Parameter.ITEM_ID)}, //
						null);

				//Edit existing Bike
				if (c.getCount() == 1) {
					newEntry = false;
					c.moveToFirst();
					bike = EntityUtilsFactory.createBikeUtils(getActivity()).build(c);
//	      getActivity().getActionBar().setTitle("Edit Bike");
				}
				// Create new Bike
				else {
					bike = new Bike(UUID.randomUUID(), "", "", false, System.currentTimeMillis());
				}
			}


			// Create event_item
			else {
				bike = new Bike(UUID.randomUUID(), "", "", false, System.currentTimeMillis());
			}
		}

		// Tag Types
		else if (getArguments().getString(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.TAG_TYPE)) {

			//Edit event_item
			if (getArguments() != null && getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				// Load the content specified by the fragment
				ContentResolver cr = getActivity().getContentResolver();

			/* uri            The URI, using the content:// scheme, for the content to retrieve.
               projection     A list of which columns to return. Passing null will return all columns, which is inefficient.
			   selection      A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
			   selectionArgs  You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
			   sortOrder      How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.*/
				Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, //
						null, //
						BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME + "=?", //
						new String[]{getArguments().getString(Contract.Parameter.ITEM_ID)}, //
						null);

				//Edit existing Bike
				if (c.getCount() == 1) {
					newEntry = false;
					c.moveToFirst();
					tagType = EntityUtilsFactory.createTagTypeUtils(getActivity()).build(c);
				}
				// Create new
				else {
					tagType = new TagType(UUID.randomUUID(), "", false, System.currentTimeMillis());
				}
			}


			// Create event_item
			else {
				tagType = new TagType(UUID.randomUUID(), "", false, System.currentTimeMillis());
			}
		}
		// Tag
		else if (getArguments().getString(MasterDataContract.Type.NAME).equals(MasterDataContract.Type.Values.TAG)) {

			//Edit event_item
			if (getArguments() != null && getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				// Load the content specified by the fragment
				ContentResolver cr = getActivity().getContentResolver();

			/* uri            The URI, using the content:// scheme, for the content to retrieve.
			   projection     A list of which columns to return. Passing null will return all columns, which is inefficient.
			   selection      A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
			   selectionArgs  You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
			   sortOrder      How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.*/
				Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, //
						null, //
						BikeHistProvider.BikeHistContract.Tables.Tag.Id.NAME + "=?", //
						new String[]{getArguments().getString(Contract.Parameter.ITEM_ID)}, //
						null);

				//Edit existing Bike
				if (c.getCount() == 1) {
					newEntry = false;
					c.moveToFirst();
					tag = EntityUtilsFactory.createTagUtils(getActivity()).build(c);
				}
				// Create new
				else {
					tag = new Tag(UUID.randomUUID(), "", null, false, System.currentTimeMillis());
				}
			}


			// Create event_item
			else {
				tag = new Tag(UUID.randomUUID(), "", null, false, System.currentTimeMillis());
			}
		}

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(SaveDataService.Contract.INTENT_NAME));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		//--- Bike ---
		Button saveButton;
		if (bike != null) {
			rootView = inflater.inflate(R.layout.master_data_detail_bike, container, false);

			name = (TextView) rootView.findViewById(R.id.master_data_fragment_item_detail_bike_name);
			frameNumber = (TextView) rootView.findViewById(R.id.master_data_fragment_item_detail_bike_frame_number);

			name.setText(bike.getName());
			frameNumber.setText(bike.getFrameNumber());

			name.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				}
			});
			frameNumber.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				}
			});

			if (getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				getActivity().setTitle(R.string.titleEditBike);
			} else {
				getActivity().setTitle(R.string.titleCreateBike);
			}


			saveButton = ((Button) rootView.findViewById(R.id.master_data_fragment_item_detail_bike_save));
			saveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					//Check preconditions
					if (name.getText().toString().isEmpty()) {
						Toast.makeText(getActivity(), getString(R.string.messageNameMissing), Toast.LENGTH_SHORT).show();
						name.requestFocus();
						return;
					}
					if (frameNumber.getText().toString().isEmpty()) {
						Toast.makeText(getActivity(), getString(R.string.messageFrameNumberMissing), Toast.LENGTH_SHORT).show();
						frameNumber.requestFocus();
						return;
					}

					bike.setName(name.getText().toString());
					bike.setFrameNumber(frameNumber.getText().toString());
					saveBike();

				}

			});
		}

		//--- Tag Type ---
		else if (tagType != null) {
			rootView = inflater.inflate(R.layout.master_data_detail_tag_type, container, false);

			name = (TextView) rootView.findViewById(R.id.master_data_fragment_item_detail_tag_type_name);

			name.setText(tagType.getName());

			name.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				}
			});

			if (getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				getActivity().setTitle(R.string.titleEditTagType);
			} else {
				getActivity().setTitle(R.string.titleCreateTagType);
			}

			saveButton = ((Button) rootView.findViewById(R.id.master_data_fragment_item_detail_tag_type_save));
			saveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					//Check preconditions
					if (name.getText().toString().isEmpty()) {
						Toast.makeText(getActivity(), getString(R.string.messageNameMissing), Toast.LENGTH_SHORT).show();
						name.requestFocus();
						return;
					}

					tagType.setName(name.getText().toString());
					saveTagType();
				}

			});
		}

		//--- Tag ---
		else if (tag != null) {
			rootView = inflater.inflate(R.layout.master_data_detail_tag, container, false);

			name = (TextView) rootView.findViewById(R.id.master_data_fragment_item_detail_tag_tag_name);
			Spinner tagTypeSpinner = (Spinner) rootView.findViewById(R.id.master_data_fragment_item_detail_tag_tag_type_spinner);

			tagTypeSpinnerList = new ArrayList<>();
			List<String> tagTypes = new ArrayList<>();
			ArrayAdapter<String> tagTypeSpinnerAdapter = new ArrayAdapter<>(getActivity(),
					android.R.layout.simple_spinner_item, tagTypes);
			tagTypeSpinner.setAdapter(tagTypeSpinnerAdapter);
			tagTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, null, null, null);

			if (c.getCount() > 0) {
				c.moveToFirst();
				do {
					TagType tagType = EntityUtilsFactory.createTagTypeUtils(getActivity()).build(c);
					tagTypeSpinnerList.add(tagType);
					tagTypes.add(tagType.getName());
				} while (c.moveToNext());
			}
			tagTypeSpinnerAdapter.notifyDataSetChanged();

			//Get TagType of Tag
			if (tag.getTagTypeId() != null) {
				for (int i = 0; i < tagTypeSpinnerList.size(); i++) {
					if (tag.getTagTypeId().equals(tagTypeSpinnerList.get(i).getId())) {
						tagTypeSpinner.setSelection(i);
					}
				}
			}

			tagTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					tag.setTagTypeId(tagTypeSpinnerList.get(position).getId());
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					tag.setTagTypeId(tagTypeSpinnerList.get(0).getId());
				}
			});


			name.setText(tag.getName());

			name.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				}
			});

			if (getArguments().containsKey(Contract.Parameter.ITEM_ID)) {
				getActivity().setTitle(R.string.titleEditTag);
			} else {
				getActivity().setTitle(R.string.titleCreateTag);
			}

			saveButton = ((Button) rootView.findViewById(R.id.master_data_fragment_item_detail_tag_save));
			saveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					//Check preconditions
					if (name.getText().toString().isEmpty()) {
						Toast.makeText(getActivity(), getString(R.string.messageNameMissing), Toast.LENGTH_SHORT).show();
						name.requestFocus();
						return;
					}

					tag.setName(name.getText().toString());
					saveTag();
				}

			});
		}


		return rootView;
	}

	private void saveTag() {
		//update entity
		ContentResolver cr = getActivity().getContentResolver();
		String where = BikeHistProvider.BikeHistContract.Tables.Tag.Id.NAME + "=?";
		String[] args = {tag.getId().toString()};

		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null, where, args, null);
		if (c.getCount() == 0) {
			if (cr.insert(BikeHistProvider.CONTENT_URI_TAGS,
					EntityUtilsFactory.createTagUtils(getActivity()).build(tag)) != null) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Insert failed !");
			}
		} else {
			ContentValues cv = EntityUtilsFactory.createTagUtils(getActivity()).build(tag);
			if (cr.update(BikeHistProvider.CONTENT_URI_TAGS, cv,
					where, args) == 1) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Update failed !");
			}
		}

		c.close();
	}

	private void saveTagType() {
		//update entity
		ContentResolver cr = getActivity().getContentResolver();
		String where = BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME + "=?";
		String[] args = {tagType.getId().toString()};

		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null, where, args, null);
		if (c.getCount() == 0) {
			if (cr.insert(BikeHistProvider.CONTENT_URI_TAG_TYPES,
					EntityUtilsFactory.createTagTypeUtils(getActivity()).build(tagType)) != null) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Insert failed !");
			}
		} else {
			ContentValues cv = EntityUtilsFactory.createTagTypeUtils(getActivity()).build(tagType);
			if (cr.update(BikeHistProvider.CONTENT_URI_TAG_TYPES, cv,
					where, args) == 1) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Update failed !");
			}
		}

		c.close();
	}


	private void saveBike() {
		//update entity
		ContentResolver cr = getActivity().getContentResolver();
		String where = BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME + "=?";
		String[] args = {bike.getId().toString()};

		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null, where, args, null);
		if (c.getCount() == 0) {
			if (cr.insert(BikeHistProvider.CONTENT_URI_BIKES,
					EntityUtilsFactory.createBikeUtils(getActivity()).build(bike)) != null) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Insert failed !");
			}
		} else {
			ContentValues cv = EntityUtilsFactory.createBikeUtils(getActivity()).build(bike);
			if (cr.update(BikeHistProvider.CONTENT_URI_BIKES, cv,
					where, args) == 1) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onDetailChanged();
			} else {
				Log.e(TAG, "Update failed !");
			}
		}

		c.close();
	}

	@Override
	public void onPause() {
		super.onPause();
		imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
	}

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of event_item changed
	 * tu update their views.
	 */
	public interface Callbacks {
		/**
		 * Callback for when the event_item was changed.
		 */
		void onDetailChanged();
	}

	/**
	 * Parameters for consuming the Fragment.
	 */
	public static abstract class Contract {

		/**
		 * Particular arguments for Fragment consumer. In addition, ....
		 */
		public static abstract class Parameter {

			/**
			 * The fragment argument representing the event_item EVENT_ID that this fragment
			 * represents (UUID).
			 */
			public static final String ITEM_ID = "item_id";
		}

	}

//	/** Confirm dialog. Needs argument with Question. Consumer must call setCallbacks(). */
//	public static class DeleteDialogFragment extends DialogFragment {
//		public static final String ARG_MESSAGE = "message";
//		private Callbacks callbacks;
//
//		public void setCallbacks(Callbacks callbacks) {
//			this.callbacks = callbacks;
//		}
//
//		@Override
//		public Dialog onCreateDialog(Bundle savedInstanceState) {
//			// Use the Builder class for convenient dialog construction
//			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//			builder.setMessage(getArguments().getString(ARG_MESSAGE))
//					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int id) {
//							callbacks.onClickOk();
//						}
//					})
//					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int id) {
//							// User cancelled the dialog
//						}
//					});
//			// Create the AlertDialog object and return it
//			return builder.create();
//		}
//
//		/** For result events. */
//		public interface Callbacks {
//			public void onClickOk();
//		}
//	}

}
