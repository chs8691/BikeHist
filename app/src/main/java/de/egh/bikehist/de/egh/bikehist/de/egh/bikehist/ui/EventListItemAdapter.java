package de.egh.bikehist.de.egh.bikehist.de.egh.bikehist.ui;

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

public class EventListItemAdapter extends ArrayAdapter<EventItem> {

	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;
	int resource;
	Calendar cal = Calendar.getInstance();
	private Date timestampDate = new Date();
	private EventItem item;
	private String inflater;
	private LayoutInflater vi;

	public EventListItemAdapter(Context _context, int _resource, List<EventItem> _items) {
		super(_context, _resource, _items);
		resource = _resource;
		inflater = Context.LAYOUT_INFLATER_SERVICE;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemView;

		item = getItem(position);

		timestampDate.setTime(item.getEvent().getTimestamp());

		if (convertView == null) {
			itemView = new LinearLayout(getContext());
			vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, itemView, true);
		} else {
			itemView = (LinearLayout) convertView;
		}


		((TextView) itemView.findViewById(R.id.itemDate)).setText(DateFormat.getDateInstance().format(timestampDate));
		((TextView) itemView.findViewById(R.id.itemTime)).setText(DateFormat.getTimeInstance().format(timestampDate));

		((TextView) itemView.findViewById(R.id.itemDistance)).setText(String.format("%,1.1f", ((double) item.getEvent().getDistance()) / 1000.0));
		((TextView) itemView.findViewById(R.id.itemEventName)).setText(item.getEvent().getName());
		((TextView) itemView.findViewById(R.id.itemTagName)).setText(item.getTag().getName());

		((TextView) itemView.findViewById(R.id.itemDiffDistance)).setText(String.format("%,1.1f", ((double) item.getEvent().getDiffDistance()) / 1000.0));

		DateTime jStart = new DateTime(item.getEvent().getTimestamp() - item.getEvent().getDiffTimestamp());
		DateTime jEnd = new DateTime(item.getEvent().getTimestamp());

		Years years = Years.yearsBetween(jStart, jEnd);
		jEnd.plusYears(years.getYears());
		Months months = Months.monthsBetween(jStart, jEnd);
		jEnd.plusMonths(months.getMonths());
		Days days = Days.daysBetween(jStart, jEnd);
		Hours hours = Hours.hoursBetween(jStart, jEnd);
		Minutes minutes = Minutes.minutesBetween(jStart, jEnd);
		Seconds seconds = Seconds.secondsBetween(jStart, jEnd);
		Interval interval = new Interval(jStart,jEnd);

		PeriodFormatter formatter;
		// Longer than a day: just print period without time
    if(days.getDays()>0) {

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
		else{
	    formatter = new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2)
			    .appendHours().appendSuffix(":")
			    .appendMinutes().appendSuffix(":")
			    .appendSeconds().toFormatter();
    }
		Period period = new Period(jStart, jEnd);

		((TextView) itemView.findViewById(R.id.itemDiffDuration)).setText(formatter.print(period));


		return itemView;
	}


}
