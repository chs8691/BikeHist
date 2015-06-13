package de.egh.bikehist.ui.event;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.Seconds;
import org.joda.time.Years;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.egh.bikehist.R;

class EventListItemAdapter extends ArrayAdapter<EventItem> {

	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;
	private final int resource;
	Calendar cal = Calendar.getInstance();
	private final Date timestampDate = new Date();
	private final String inflater;

	public EventListItemAdapter(Context _context, List<EventItem> _items) {
		super(_context, R.layout.event_item, _items);
		resource = R.layout.event_item;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemView;

		EventItem item = getItem(position);

		timestampDate.setTime(item.getEvent().getTimestamp());

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}


		((TextView) itemView.findViewById(R.id.itemDate)).setText(DateFormat.getDateInstance().format(timestampDate));
		((TextView) itemView.findViewById(R.id.itemTime)).setText(DateFormat.getTimeInstance().format(timestampDate));

		((TextView) itemView.findViewById(R.id.itemEventName)).setText(item.getEvent().getName());

		((TextView) itemView.findViewById(R.id.itemTagName)).setText(item.getTag().getName());

		((TextView) itemView.findViewById(R.id.itemDistance)).setText(String.format("%,d", item.getEvent().getDistance() / 1000));

		DateTime jStart = new DateTime(item.getEvent().getTimestamp() - item.getEvent().getDiffTimestamp());
		DateTime jEnd = new DateTime(item.getEvent().getTimestamp());

		Years years = Years.yearsBetween(jStart, jEnd);
		jEnd.plusYears(years.getYears());
		Months months = Months.monthsBetween(jStart, jEnd);
		jEnd.plusMonths(months.getMonths());
		Days days = Days.daysBetween(jStart, jEnd);

		PeriodFormatter formatter;
		// Longer than a day: just print period without time
		if (days.getDays() > 0) {

			formatter = new PeriodFormatterBuilder()
					.appendYears()
					.appendSuffix(" y")
					.appendMonths()
					.appendSuffix(" m")
					.appendDays()
					.appendSuffix(" d")
					.toFormatter();

		}
		// lighter than a day: just print time
		else {
			formatter = new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2)
					.appendHours().appendSuffix(":")
					.appendMinutes().appendSuffix(":")
					.appendSeconds().toFormatter();
		}
		Period period = new Period(jStart, jEnd);

		// If first Event for this Tag, don't show difference values
		// Ok, it's tricky how to find this first entry... Maybe Event should have a flag instead
		TextView diffDistanceView = (TextView) itemView.findViewById(R.id.itemDiffDistance);
		TextView diffDurationView = (TextView) itemView.findViewById(R.id.itemDiffDuration);
    if(item.getEvent().getDiffDistance()>0 && item.getEvent().getDiffTimestamp() > 0) {
	    diffDistanceView.setText(String.format("%,d", item.getEvent().getDiffDistance() / 1000));
	    diffDurationView.setText(formatter.print(period));
    }else{
	    diffDistanceView.setText("");
	    diffDurationView.setText("");
	    diffDistanceView.setVisibility(View.INVISIBLE);
	    itemView.findViewById(R.id.itemDiffDistanceUnit).setVisibility(View.INVISIBLE);
	    diffDurationView.setVisibility(View.INVISIBLE);
    }

		return itemView;
	}


}
