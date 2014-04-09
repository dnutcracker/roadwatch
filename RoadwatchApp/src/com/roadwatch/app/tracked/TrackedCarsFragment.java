package com.roadwatch.app.tracked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.MapBuilder;
import com.roadwatch.app.ApplicationData;
import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;

/**
 * Display all the cars that are tracked by the user and allow him to manage the list.
 *  
 * @author Nati created at : 02/12/2013 02:15:20
 */
public class TrackedCarsFragment extends ListFragment
{
	// Logging tag
	private static final String TAG = TrackedCarsFragment.class.getSimpleName();

	private static final int ADD_TRACKED_CAR_CODE = 2001;

	private ArrayList<HashMap<String, String>> trackedCarRows = new ArrayList<HashMap<String, String>>();

	private LinearLayout progressLayout;
	private TextView emptyTextView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (container == null)
		{
			// Currently in a layout without a container, so no reason to create our view.
			return null;
		}

		View rootView = inflater.inflate(R.layout.tracked_cars_fragment, container, false);

		progressLayout = (LinearLayout) rootView.findViewById(R.id.get_tracked_cars_progress_layout);
		emptyTextView = (TextView) rootView.findViewById(android.R.id.empty);

		trackedCarRows.clear();
		String loggedInLicensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		// Make sure user is logged in
		if (!loggedInLicensePlate.isEmpty())
		{
			// Load tracked cars list from cache
			List<JSONObject> jsonUsers = JSONUtils.loadJSONObjects(getActivity(), JSONUtils.LOGGED_IN_USER_DATA);
			if (jsonUsers.isEmpty())
			{
				// Cache is empty - load list from server
				new GetTrackedCarsTask().execute(loggedInLicensePlate);
			}
			else
			{
				// Show tracked cars list from cache
				trackedCarRows.addAll(getJsonUserTrackedCarsAsRows(jsonUsers.get(0)));
			}
		}
		else
		{
			// User is not logged in - clear any previously displayed tracked cars
			trackedCarRows.clear();
			progressLayout.setVisibility(View.GONE);
		}

		setupAdapterData();

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.track_actionbar_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// handle item selection
		switch (item.getItemId())
		{
		case R.id.action_add_tracked_car:
			// Check if user is logged in.
			boolean loggedIn = !PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE).isEmpty();
			if (loggedIn)
			{
				Intent addTrackedCarIntent = new Intent(getActivity(), AddTrackedCarActivity.class);
				startActivityForResult(addTrackedCarIntent, ADD_TRACKED_CAR_CODE);
			}
			else
			{
				Toast.makeText(getActivity(), "Only registered users can add tracked cars", Toast.LENGTH_LONG).show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupAdapterData()
	{
		setListAdapter(new TrackedCarsAdapter(getActivity(), trackedCarRows));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == ADD_TRACKED_CAR_CODE)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "Add Tracked Car", null).build());

				String newTrackedName = data.getStringExtra(AddTrackedCarActivity.EXTRA_TRACKED_NAME);
				String newTrackedLicensePlate = data.getStringExtra(AddTrackedCarActivity.EXTRA_TRACKED_LICENSE_PLATE);
				String errorMessage = validateTrackedInfo(newTrackedName, null, newTrackedLicensePlate);

				// PENDING:
				// Move validation code and toast message into the AddTrackedCarActivity
				if (errorMessage.isEmpty())
				{
					new ManageTrackedCarsTask(getActivity()).execute(newTrackedLicensePlate, newTrackedName);
				}
				else
					Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
			}
		}
	}

	private boolean isTrackedLicensePlateAlreadyExists(String licensePlate)
	{
		ArrayList<HashMap<String, String>> reportRows = getRows();
		for (HashMap<String, String> row : reportRows)
		{
			if (row.get(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE).equals(licensePlate))
				return true;
		}

		return false;
	}

	/**
	 * Open the edit/remove tracked license plate dialog.
	 * PENDING: Add the '...' icon on the right of each license plate to show a popup with options instead ?
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		ArrayList<HashMap<String, String>> reportList = getRows();
		String name = reportList.get(position).get(JSONKeys.USER_KEY_TRACKED_NAME);
		final String licensePlate = reportList.get(position).get(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE);

		final EditTrackedCarDialog dialog = new EditTrackedCarDialog(getActivity(), name, licensePlate);

		dialog.getUpdateButton().setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "Update Tracked Car", null).build());

				String newTrackedName = dialog.getTrackedName();
				String newTrackedLicensePlate = dialog.getTrackedLicensePlate();
				String errorMessage = validateTrackedInfo(newTrackedName, licensePlate, newTrackedLicensePlate);

				if (errorMessage.isEmpty())
				{
					// Update tracked cars license plate and name
					new ManageTrackedCarsTask(getActivity()).execute(newTrackedLicensePlate, newTrackedName, licensePlate);
					dialog.dismiss();
				}
				else
					Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
			}
		});

		dialog.getRemoveButton().setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "Remove Tracked Car", null).build());

				// Remove tracked car
				new ManageTrackedCarsTask(getActivity()).execute(licensePlate);
				dialog.dismiss();
			}
		});
		dialog.show();
	}

	/**
	 * Validates the new or edited tracked name and license plate values.
	 * 
	 * @param newTrackedName
	 * @param existingTrackedLicensePlate
	 * @param newTrackedLicensePlate
	 * @return The error message or an empty string if info is valid
	 */
	private String validateTrackedInfo(String newTrackedName, String existingTrackedLicensePlate, String newTrackedLicensePlate)
	{
		// Validate the license plate format
		if (!newTrackedName.isEmpty() && LicensePlateUtils.isValidLicensePlate(newTrackedLicensePlate))
		{
			// Make sure we're not already tracking this car (unless we're editing an existing one)
			if (!isTrackedLicensePlateAlreadyExists(newTrackedLicensePlate) || newTrackedLicensePlate.equals(existingTrackedLicensePlate))
			{
				String userLicensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);
				if (newTrackedLicensePlate.equals(userLicensePlate))
				{
					return getString(R.string.tracking_own_license_plate);
				}
				else
					return "";
			}
			else
				return getString(R.string.tracked_license_plate_already_exists);
		}
		else
			return getString(R.string.tracked_info_invalid);
	}

	@SuppressWarnings("unchecked")
	private ArrayList<HashMap<String, String>> getRows()
	{
		int rowCount = getListAdapter().getCount();
		ArrayList<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>(rowCount);
		for (int i = 0; i < rowCount; i++)
		{
			HashMap<String, String> row = (HashMap<String, String>) getListAdapter().getItem(i);
			results.add(row);
		}

		return results;
	}

	private ArrayList<HashMap<String, String>> getJsonUserTrackedCarsAsRows(JSONObject jsonUser)
	{
		ArrayList<HashMap<String, String>> resultsRows = new ArrayList<HashMap<String, String>>();
		try
		{
			JSONArray jsonTrackedLicensePlates = jsonUser.getJSONArray(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATES);
			JSONArray jsonTrackedLicensePlateNames = jsonUser.getJSONArray(JSONKeys.USER_KEY_TRACKED_NAMES);
			for (int i = 0; i < jsonTrackedLicensePlates.length(); i++)
			{
				String trackedLicensePlate = jsonTrackedLicensePlates.getString(i);
				String trackedLicensePlateOwnerName = jsonTrackedLicensePlateNames.getString(i);

				HashMap<String, String> row = new HashMap<String, String>();
				row.put(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE, trackedLicensePlate);
				row.put(JSONKeys.USER_KEY_TRACKED_NAME, trackedLicensePlateOwnerName);

				resultsRows.add(row);
			}
		}
		catch (JSONException e)
		{
			String errorMessage = ServerErrorCodes.getErrorMessage(getActivity(), jsonUser);
			Log.e(TAG, errorMessage);
		}

		return resultsRows;

	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class GetTrackedCarsTask extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>>
	{
		@Override
		protected void onPreExecute()
		{
			// Remove empty results message and display progress bar
			progressLayout.setVisibility(View.VISIBLE);

			// Hide the empty list text until after loading is finished
			emptyTextView.setVisibility(View.INVISIBLE);
		}

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(String... params)
		{
			ServerAPI serverAPI = new ServerAPI(getActivity());
			String licensePlate = params[0];

			ArrayList<HashMap<String, String>> resultAsRows = new ArrayList<HashMap<String, String>>();

			// Get user by license plate
			List<JSONObject> jsonUsers = serverAPI.getUserByLicensePlate(licensePlate);

			// Check if it is logged in
			if (!jsonUsers.isEmpty())
			{
				JSONObject jsonUser = jsonUsers.get(0);
				resultAsRows = getJsonUserTrackedCarsAsRows(jsonUser);
				Log.i(TAG, "Got " + resultAsRows.size() + " tracked cars for license plate [" + licensePlate + "]");

				// Update cached user data
				JSONUtils.saveJSONObjects(getActivity(), JSONUtils.LOGGED_IN_USER_DATA, jsonUsers, false);
			}
			else
				Log.w(TAG, "User is not logged in");

			return resultAsRows;
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> updatedRowList)
		{
			// Show empty list (will be shown only if actually empty)
			emptyTextView.setVisibility(View.VISIBLE);

			// Hide progress bar
			progressLayout.setVisibility(View.GONE);

			// Replace with new content
			trackedCarRows.clear();
			trackedCarRows.addAll(updatedRowList);
			((TrackedCarsAdapter) TrackedCarsFragment.this.getListAdapter()).notifyDataSetChanged();
		}
	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class ManageTrackedCarsTask extends AsyncTask<String, Void, JSONObject>
	{
		private Context context;
		private String trackingLicensePlate;

		public ManageTrackedCarsTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			// Display progress bar
			progressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected JSONObject doInBackground(String... params)
		{
			ServerAPI serverAPI = new ServerAPI(context);

			String newTrackedLicensePlate = params[0];
			String newTrackedName = params.length > 1 ? params[1] : "";
			String existingTrackedLicensePlate = params.length > 2 ? params[2] : "";
			trackingLicensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

			JSONObject jsonUser = null;
			if (!existingTrackedLicensePlate.isEmpty())
			{
				// Update an existing tracked car
				jsonUser = serverAPI.updateTrackedLicensePlate(trackingLicensePlate, newTrackedLicensePlate, newTrackedName, existingTrackedLicensePlate);
			}
			else if (!newTrackedName.isEmpty())
			{
				// Add a new tracked car
				jsonUser = serverAPI.addTrackedLicensePlate(trackingLicensePlate, newTrackedLicensePlate, newTrackedName);
			}
			else
			{
				// Remove a tracked car
				jsonUser = serverAPI.removeTrackedLicensePlate(trackingLicensePlate, newTrackedLicensePlate);
			}

			return jsonUser;
		}

		@Override
		protected void onPostExecute(JSONObject jsonUser)
		{
			// Refresh list with updated data from the server
			new GetTrackedCarsTask().execute(trackingLicensePlate);
		}
	}
}