package de.egh.bikehist.ui.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.EntityLoader;
import de.egh.bikehist.model.Event;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.model.Utils.EntityUtilsFactory;
import de.egh.bikehist.persistance.BikeHistProvider;
import de.egh.bikehist.persistance.SaveDataService;
import de.egh.bikehist.ui.masterdata.MasterDataContract;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link de.egh.bikehist.ui.masterdata.AdministratorActivity}
 * in two-pane mode (on tablets) or a {@link de.egh.bikehist.ui.masterdata.MasterDataDetailActivity}
 * on handsets.
 */
public class EventDetailFragment extends Fragment {

	private static final String TAG = EventDetailFragment.class.getSimpleName();
	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static final Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onEventChanged() {
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
					mCallbacks.onEventChanged();
				}
			}
		}
	};
	private Event event;
	private Bike bike;
	private TagType tagType;
	@Deprecated
	private Tag tag;
	private TextView name;
	private TextView frameNumber;
	private InputMethodManager imm;
	private View rootView;
	private List<Tag> tagSpinnerList;
	private TextView dateView;
	private TextView distanceView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EventDetailFragment() {
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
		inflater.inflate(R.menu.menu_event_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * Returns true, if event is new, otherwise false
	 */
	private boolean isNew() {
		return !getArguments().containsKey(EventContract.EVENT_ID);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.menuEventDetailActionDelete:
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
			c.close();
			return c.getInt(0) > 0;
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


	private Tag loadTag(String id) {
		Cursor c = null;
		try {
			ContentResolver cr = getActivity().getContentResolver();
			c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
					BikeHistProvider.BikeHistContract.Tables.Tag.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createTagUtils(getActivity()).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}

	private TagType loadTagType(String id) {
		Cursor c = null;
		try {
			ContentResolver cr = getActivity().getContentResolver();
			c = cr.query(BikeHistProvider.CONTENT_URI_TAG_TYPES, null,
					BikeHistProvider.BikeHistContract.Tables.TagType.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createTagTypeUtils(getActivity()).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}

	private Bike loadBike(String id) {
		Cursor c = null;
		try {
			ContentResolver cr = getActivity().getContentResolver();
			c = cr.query(BikeHistProvider.CONTENT_URI_BIKES, null,
					BikeHistProvider.BikeHistContract.Tables.Bike.Id.NAME + "=?",
					new String[]{id}, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				return EntityUtilsFactory.createBikeUtils(getActivity()).build(c);
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}

	private Event loadEvent(String id) {
		Cursor c = null;
		try {
			// Load the content specified by the fragment
			ContentResolver cr = getActivity().getContentResolver();

			/* uri            The URI, using the content:// scheme, for the content to retrieve.
			   projection     A list of which columns to return. Passing null will return all columns, which is inefficient.
			   selection      A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI.
			   selectionArgs  You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings.
			   sortOrder      How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.*/
			c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, //
					null, //
					BikeHistProvider.BikeHistContract.Tables.Event.Id.NAME + "=?", //
					new String[]{id}, //
					null);

			//Edit existing Bike
			if (c.getCount() == 1) {
				c.moveToFirst();
				return EntityUtilsFactory.createEventUtils(getActivity()).build(c);
			} else {
				throw new IllegalArgumentException("Unknown Event EVENT_ID " + getArguments().getString(EventContract.EVENT_ID));
			}
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;

	}

	/**
	 * Dialog for Confirmation
	 */
	private void deleteItemQuestion() {

		de.egh.bikehist.ui.DeleteDialogFragment newFragment = new de.egh.bikehist.ui.DeleteDialogFragment();
		Bundle args = new Bundle();
		String text;
		text = getString(R.string.dialogEventDeleteQuestion);

		args.putString(de.egh.bikehist.ui.DeleteDialogFragment.ARG_MESSAGE, text);
		newFragment.setArguments(args);
		newFragment.setCallbacks(new de.egh.bikehist.ui.DeleteDialogFragment.Callbacks() {
			@Override
			public void onClickOk() {
				deleteItem();
			}
		});
		newFragment.show(getActivity().getSupportFragmentManager(), "delete");

	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		boolean showDeleteButton = getArguments().containsKey(EventContract.EVENT_ID);

		menu.findItem(R.id.menuEventDetailActionDelete).setEnabled(showDeleteButton);
		menu.findItem(R.id.menuEventDetailActionDelete).setVisible(showDeleteButton);
		super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Deletes event_item asynchronous.
	 */
	private void deleteItem() {
		ContentResolver cr = getActivity().getContentResolver();
		int res = cr.delete(BikeHistProvider.CONTENT_URI_EVENTS,
				BikeHistProvider.BikeHistContract.Tables.Event.Id.NAME + "=?",
				new String[]{event.getId().toString()}
		);

		if (res > 0)
			Toast.makeText(getActivity(), String.format(getString(R.string.messageDeleteEventSuccess), res), Toast.LENGTH_LONG).show();
		else {
			Toast.makeText(getActivity(), getString(R.string.messageDeleteError), Toast.LENGTH_LONG).show();
		}

		mCallbacks.onEventChanged();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		EntityLoader entityLoader = new EntityLoader(getActivity());

		//Needed for switching keyboard
		imm = (InputMethodManager) (getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE));


		//Edit event_item
		if (getArguments().containsKey(EventContract.EVENT_ID)) {
			event = entityLoader.event(getArguments().getString(EventContract.EVENT_ID));
			bike = entityLoader.bike(event.getBikeId().toString());
			tagType = entityLoader.tagType(entityLoader.tag(event.getTagId()).getTagTypeId());
		}

		// Create event_item
		else {
			//Mandatory extras for new event: Bike- and TagType-IDs of the Event
			bike = entityLoader.bike(getArguments().getString(EventContract.BIKE_ID));
			tagType = entityLoader.tagType(getArguments().getString(EventContract.TAG_TYPE_ID));
			/*	public Event(UUID id,
			         String name,
	             boolean deleted
	             long touchedAt
	             long distance,
	             UUID bikeId,
	             UUID tagId,
	             GeoLocation geoLocation,
	             long timestamp,
	             long diffDistance,
	             long diffTimestamp
	             */
			event = new Event(UUID.randomUUID(), "", false, System.currentTimeMillis(), 0L,
					bike.getId(), null, null, System.currentTimeMillis(), 0L, 0L);
		}

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(SaveDataService.Contract.INTENT_NAME));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {


		rootView = inflater.inflate(R.layout.event_detail, container, false);

		((TextView) rootView.findViewById(R.id.event_detail_bike_name)).setText(bike.getName() + "/" + tagType.getName());

		name = (TextView) rootView.findViewById(R.id.event_detail_name);
		dateView = (TextView) rootView.findViewById(R.id.event_detail_date);
		distanceView = (TextView) rootView.findViewById(R.id.event_detail_distance);

		dateView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerFragment newFragment = new DatePickerFragment();
				Bundle bundle = new Bundle();
				bundle.putLong(DatePickerFragment.ARGUMENT_TIMESTAMP, event.getTimestamp());
				newFragment.setArguments(bundle);
				newFragment.setCallbacks(new DatePickerFragment.Callbacks() {
					@Override
					public void onDateSet(long timestamp) {
						setDate(timestamp);
					}
				});
				newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
			}
		});

		name.setText(event.getName());
		setDate(event.getTimestamp());
		//Distance are stored in Meters but shown in Kilometers
		distanceView.setText(String.valueOf(event.getDistance() / 1000));

		Spinner tagSpinner = (Spinner) rootView.findViewById(R.id.event_detail_tag);

		tagSpinnerList = new ArrayList<>();
		List<String> tags = new ArrayList<>();
		ArrayAdapter<String> tagSpinnerAdapter = new ArrayAdapter<>(getActivity(),
				android.R.layout.simple_spinner_item, tags);
		tagSpinner.setAdapter(tagSpinnerAdapter);
		tagSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		ContentResolver cr = getActivity().getContentResolver();
		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_TAGS, null,
				BikeHistProvider.BikeHistContract.Tables.Tag.TagTypeId.NAME + "=?",
				new String[]{tagType.getId().toString()}, null);

		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				Tag tag = EntityUtilsFactory.createTagUtils(getActivity()).build(c);
				tagSpinnerList.add(tag);
				tags.add(tag.getName());
			} while (c.moveToNext());
		} else {
			Toast.makeText(getActivity(), String.format(getString(R.string.messageEmptyTagType), tagType.getName()),
					Toast.LENGTH_LONG).show();
			mCallbacks.onEventChanged();
		}

		tagSpinnerAdapter.notifyDataSetChanged();

		//Select actual value
		if (event.getTagId() != null) {
			for (int i = 0; i < tagSpinnerList.size(); i++) {
				if (event.getTagId().equals(tagSpinnerList.get(i).getId())) {
					tagSpinner.setSelection(i);
				}
			}
		}

		tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				event.setTagId(tagSpinnerList.get(position).getId());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				event.setTagId(tagSpinnerList.get(0).getId());
			}
		});

		name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
		});

		if (getArguments().containsKey(EventContract.EVENT_ID)) {
			getActivity().setTitle(R.string.titleEditEvent);
		} else {
			getActivity().setTitle(R.string.titleCreateEvent);
		}


		Button saveButton = ((Button) rootView.findViewById(R.id.event_detail_save));
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
			}

		});


		return rootView;
	}

	/**
	 * Check and save event.
	 */
	private void save() {
		//Check preconditions
//		if (name.getText().toString().isEmpty()) {
//			Toast.makeText(getActivity(), getString(R.string.messageNameMissing), Toast.LENGTH_SHORT).show();
//			name.requestFocus();
//			return;
//		}

		event.setName(name.getText().toString());
		//Date was set by setDate(long)
		event.setDistance(Long.valueOf(distanceView.getText().toString()) * 1000);

		saveEvent();

	}

	/**
	 * Set the value in the date field and in the event.
	 */
	private void setDate(long timestamp) {
		dateView.setText(DateFormat.getDateInstance().format(timestamp));
		event.setTimestamp(timestamp);
	}

	private void saveEvent() {
		//update entity
		ContentResolver cr = getActivity().getContentResolver();
		String where = BikeHistProvider.BikeHistContract.Tables.Event.Id.NAME + "=?";
		String[] args = {event.getId().toString()};

		Cursor c = cr.query(BikeHistProvider.CONTENT_URI_EVENTS, null, where, args, null);
		if (c.getCount() == 0) {
			ContentValues v = EntityUtilsFactory.createEventUtils(getActivity()).build(event);
			if (cr.insert(BikeHistProvider.CONTENT_URI_EVENTS,
					EntityUtilsFactory.createEventUtils(getActivity()).build(event)) != null) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onEventChanged();
			} else {
				Log.e(TAG, "Insert failed !");
			}
		} else {
			ContentValues cv = EntityUtilsFactory.createEventUtils(getActivity()).build(event);
			if (cr.update(BikeHistProvider.CONTENT_URI_EVENTS, cv,
					where, args) == 1) {
				Toast.makeText(getActivity(), getString(R.string.messageSaved), Toast.LENGTH_SHORT).show();
				mCallbacks.onEventChanged();
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
		public void onEventChanged();
	}

	public static class DatePickerFragment extends DialogFragment
			implements DatePickerDialog.OnDateSetListener {

		public static final String ARGUMENT_TIMESTAMP = "DATE";
		private final Calendar c = Calendar.getInstance();
		private Callbacks callbacks = new Callbacks() {
			@Override
			public void onDateSet(long timestamp) {
			}
		};

		public void setCallbacks(Callbacks callbacks) {
			this.callbacks = callbacks;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			if (getArguments().containsKey(ARGUMENT_TIMESTAMP)) {
				c.setTimeInMillis(getArguments().getLong(ARGUMENT_TIMESTAMP));
			}

			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, month);
			c.set(Calendar.DAY_OF_MONTH, day);
			callbacks.onDateSet(c.getTimeInMillis());
		}


		public interface Callbacks {
			abstract void onDateSet(long timestamp);
		}
	}


	/**
	 * Confirm dialog. Needs argument with Question. Consumer must call setCallbacks().
	 */
	public static class DeleteDialogFragment extends DialogFragment {
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

		/**
		 * For result events.
		 */
		public interface Callbacks {
			public void onClickOk();
		}
	}

}
