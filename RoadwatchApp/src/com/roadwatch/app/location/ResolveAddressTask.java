package com.roadwatch.app.location;

import java.util.HashMap;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.roadwatch.app.R;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.ReportUtils;

/**
 * An AsyncTask that calls getFromLocation() in the background. The class uses the following generic types: Location - A {@link android.location.Location} object containing the current location,
 * passed as the input parameter to doInBackground() Void - indicates that progress units are not used by this subclass String - An address passed to onPostExecute()
 */
public class ResolveAddressTask extends AsyncTask<Location, Void, Address>
{
	private static final String TAG = ResolveAddressTask.class.getSimpleName();

	private Activity activity;
	private TextView textView;
	private ListView listView;

	// Constructor called by the system to instantiate the task
	public ResolveAddressTask(Activity activity, TextView textView)
	{
		this.activity = activity;
		this.textView = textView;
	}

	public ResolveAddressTask(Activity activity, ListView listView)
	{
		this.activity = activity;
		this.listView = listView;
	}

	@Override
	protected void onPreExecute()
	{
		// In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
		{
			if (!Geocoder.isPresent())
			{
				cancel(false);

				Log.e(TAG, "Geocode.isPresent() returned false !");

				// No geocoder is present. Issue an error message (Happens on Genymotion emulator)
				Toast.makeText(activity, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Get a geocoding service instance, pass latitude and longitude to it, format the returned address, and return the address to the UI thread.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Address doInBackground(Location... params)
	{
		if (isCancelled())
			return null;

		if (listView == null)
		{
			// Get the current location from the input parameter list
			Location location = params[0];

			return ReportUtils.resolveLocationToAddress(activity, location.getLatitude(), location.getLongitude());
		}
		else
		{
			for (int i = 0; i < listView.getAdapter().getCount(); i++)
			{
				if (isCancelled())
					return null;

				HashMap<String, String> row = (HashMap<String, String>) listView.getAdapter().getItem(i);

				// Check if this row might already have an address (may happen when, for example, user changed device orientation while this task is running)
				if (row.get(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY) == null)
				{
					double latitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LATITUDE));
					double longtitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LONGITUDE));
					Address address = ReportUtils.resolveLocationToAddress(activity, latitude, longtitude);

					// Check if the reverse geocode returned an address
					String addressStr = ReportUtils.getSingleLineAddress(activity, address);
					row.put(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, addressStr);

					int top = listView.getFirstVisiblePosition();
					int bottom = listView.getLastVisiblePosition();

					// Update visible elements as fast as they come
					if (i >= top && i <= bottom)
						updateUI();
					else // Update out of screen elements in groups of 7s
					if (i % 7 == 0)
						updateUI();
				}
			}

			updateUI();

			return null;
		}
	}

	private void updateUI()
	{
		Handler handler = listView.getHandler();
		if (handler != null)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (!isCancelled())
						((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	protected void onPostExecute(Address address)
	{
		if (textView != null)
		{
			// Update the resolved address in the UI
			if (address != null)
				textView.setText(ReportUtils.getSingleLineAddress(activity, address));
		}
	}
}