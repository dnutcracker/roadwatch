package com.roadwatch.app.tracked;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.util.JSONKeys;

public class TrackedCarsAdapter extends ArrayAdapter<HashMap<String, String>>
{
	private final Activity context;

	static class ViewHolder
	{
		public TextView carName;
		public TextView licensePlateView;
	}

	public TrackedCarsAdapter(Activity context, List<HashMap<String, String>> resultRows)
	{
		super(context, R.layout.tracked_car_row, resultRows);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View rowView = convertView;
		if (rowView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.tracked_car_row, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.carName = (TextView) rowView.findViewById(R.id.car_name_text);
			viewHolder.licensePlateView = (TextView) rowView.findViewById(R.id.license_plate);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		HashMap<String, String> reportRow = getItem(position);
		String trackedCarName = reportRow.get(JSONKeys.USER_KEY_TRACKED_NAME);
		String trackedCarLicensePlate = reportRow.get(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE);

		// On Gingerbread the RTL alignment is bad so we just show the name of the car
		// PENDING: Until we'll let the user choose vehicle type(car, motorcycle...etc) - don't show this prefix
		//String trackedCarTitle = Utils.isGingerbread() ? trackedCarName + ":" : context.getString(R.string.tracked_car_name, trackedCarName);
		holder.carName.setText(trackedCarName+":");
		holder.licensePlateView.setText(trackedCarLicensePlate);

		return rowView;
	}
}