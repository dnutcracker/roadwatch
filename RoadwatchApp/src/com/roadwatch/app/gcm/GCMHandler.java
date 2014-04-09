package com.roadwatch.app.gcm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.analytics.tracking.android.MapBuilder;
import com.roadwatch.app.ApplicationData;
import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.map.ReportMapActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;
import com.roadwatch.app.util.Utils;

public class GCMHandler
{
	private static final String TAG = GCMHandler.class.getSimpleName();

	// GCM Notification IDs
	public static final int GCM_GENERAL_NOTIFICATION_ID = 1;
	public static final int GCM_SIMPLE_MESSAGE_NOTIFICATION_ID = 2;
	public static final int GCM_REPORTED_MESSAGE_NOTIFICATION_ID = 3;
	public static final int GCM_REPORT_BLOCKED_MESSAGE_NOTIFICATION_ID = 4;

	// Server side notifications keys
	private static final String REPORT_NOTIFICATION_KEY = "gcmReportNotification";
	private static final String SERVER_SIMPLE_MESSAGE_NOTIFICATION_KEY = "gcmServerMessageSimpleMessageNotification";

	private Context context;

	public GCMHandler(Context context)
	{
		this.context = context;
	}

	public void handleMessage(Bundle extras)
	{
		// Got a simple message from the server
		if (extras.containsKey(SERVER_SIMPLE_MESSAGE_NOTIFICATION_KEY))
		{
			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("GCM", "Server Message", "Server Simple Message", null).build());

			handleSimpleMessage(extras);
		}

		// Got a 'you've been reported' notification
		if (extras.containsKey(REPORT_NOTIFICATION_KEY))
		{
			handleReportMessage(extras);
		}
	}

	private void handleSimpleMessage(Bundle extras)
	{
		Log.d(TAG, "Got simple GCM message : " + extras);
		int messageCode = Integer.parseInt(extras.getString(SERVER_SIMPLE_MESSAGE_NOTIFICATION_KEY));
		switch (messageCode)
		{
		case 1:
			String shortMessage = context.getString(R.string.notification_report_witness_short);
			String longMessage = context.getString(R.string.notification_report_witness_long);
			Utils.showNotification(context, context.getString(R.string.notification_report_good_title), shortMessage, longMessage, GCM_SIMPLE_MESSAGE_NOTIFICATION_ID);
			break;
		}

		Utils.beep(context);
	}

	private void handleReportMessage(Bundle extras)
	{
		JSONObject jsonReport = JSONUtils.convertStringToJSONObject(extras.getString(REPORT_NOTIFICATION_KEY));
		Intent showReportMapIntent = getReportIntent(jsonReport);

		if (didWeGotReported(jsonReport))
		{
			if (hasAllibiOutsideReportScene(jsonReport))
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("GCM", "Report Message", "Protected by RoadWatch", null).build());

				// Remove report from server
				removeReportFromServer(jsonReport);

				// Notify user that Roadwatch protected him
				String shortMessage = context.getString(R.string.notification_report_blocked_short);
				String longMessage = context.getString(R.string.notification_report_blocked_long);
				Utils.showNotification(context, context.getString(R.string.notification_report_blocked_title), shortMessage, longMessage, GCM_REPORT_BLOCKED_MESSAGE_NOTIFICATION_ID);
			}
			else
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("GCM", "Report Message", "User was reported", null).build());

				// Notify the user that he was 'Roadwatched!'
				String shortMessage = getReportedMessage(jsonReport, false);
				String longMessage = getReportedMessage(jsonReport, true);
				long reportTime = jsonReport.optLong(JSONKeys.REPORT_KEY_TIME);

				Utils.showNotification(context, context.getString(R.string.notification_reported_title), shortMessage, longMessage, reportTime, R.drawable.floating_report_button, showReportMapIntent,
						GCM_REPORTED_MESSAGE_NOTIFICATION_ID);
			}
		}
		else
		// We got a report on one of our tracked cars
		{
			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("GCM", "Report Message", "Tracked Car Reported", null).build());

			// Notify the user that one of his tracked cars was 'Roadwatched!'
			String shortMessage = getReportedMessage(jsonReport, false);
			String longMessage = getReportedMessage(jsonReport, true);
			long reportTime = jsonReport.optLong(JSONKeys.REPORT_KEY_TIME);
			String reportedPlate = jsonReport.optString(JSONKeys.REPORT_KEY_LICENSE_PLATE);
			String reportedName = getTrackedCarName(reportedPlate);

			Utils.showNotification(context, context.getString(R.string.notification_tracked_car_reported_title, reportedName), shortMessage, longMessage, reportTime,
					R.drawable.floating_report_button, showReportMapIntent, GCM_REPORTED_MESSAGE_NOTIFICATION_ID);
		}

		Utils.beep(context);
	}

	private String getTrackedCarName(String trackedLicensePlate)
	{
		List<JSONObject> jsonUsers = JSONUtils.loadJSONObjects(context, JSONUtils.LOGGED_IN_USER_DATA);
		if (!jsonUsers.isEmpty())
		{
			JSONArray jsonTrackedLicensePlates = jsonUsers.get(0).optJSONArray(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATES);
			JSONArray jsonTrackedLicensePlateNames = jsonUsers.get(0).optJSONArray(JSONKeys.USER_KEY_TRACKED_NAMES);
			for (int i = 0; i < jsonTrackedLicensePlates.length(); i++)
			{
				String licensePlate = jsonTrackedLicensePlates.optString(i);
				String ownerName = jsonTrackedLicensePlateNames.optString(i);
				if (trackedLicensePlate.equals(licensePlate))
					return ownerName;
			}
		}

		return trackedLicensePlate;
	}

	private boolean didWeGotReported(JSONObject jsonReport)
	{
		String loggedInLicensePlate = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);
		String reportedLicensePlate = jsonReport.optString(JSONKeys.REPORT_KEY_LICENSE_PLATE);
		return loggedInLicensePlate.equals(reportedLicensePlate);
	}

	private void removeReportFromServer(JSONObject jsonReport)
	{
		ServerAPI serverAPI = new ServerAPI(context);
		JSONArray reportedByJsonArray = jsonReport.optJSONArray(JSONKeys.REPORT_KEY_REPORTED_BY);
		long reportTime = jsonReport.optLong(JSONKeys.REPORT_KEY_TIME);
		serverAPI.removeReport(reportedByJsonArray, reportTime);
	}

	/**
	 * Checks if user has location history proving he wasn't present at the scene of the report
	 * @param jsonReport The report on the user
	 * @return true if location history proves he wasn't present at the scene of the report
	 */
	private boolean hasAllibiOutsideReportScene(JSONObject jsonReport)
	{
		final int MAX_ALLOWED_TIME_GAP_IN_SECONDS = 60;
		final int MAX_AVERAGE_SPEED_IN_KPH = 90;
		long reportTime = jsonReport.optLong(JSONKeys.REPORT_KEY_TIME);
		JSONObject closestLocation = getClosestLocationByTime(reportTime);

		// No history
		if (closestLocation == null)
			return false;

		long timeGapFromReport = Math.abs(reportTime - closestLocation.optLong(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME));

		if (timeGapFromReport > MAX_ALLOWED_TIME_GAP_IN_SECONDS * 1000)
			return false;

		// Assuming max average speed of 90Kmh, this is the allowed distance for a car to be present
		// while the report was created in order to be eligible for reporting.
		// Anything above this distance and the report will be rejected as false report.
		double allowedDistanceInMeters = timeGapFromReport / 1000.0 * (MAX_AVERAGE_SPEED_IN_KPH / 3.6);

		double reportLat = jsonReport.optDouble(JSONKeys.REPORT_KEY_LATITUDE);
		double reportLong = jsonReport.optDouble(JSONKeys.REPORT_KEY_LONGITUDE);
		double locationLat = closestLocation.optDouble(JSONKeys.USER_CURRENT_LOCATION_KEY_LATITUDE);
		double locationLong = closestLocation.optDouble(JSONKeys.USER_CURRENT_LOCATION_KEY_LONGITUDE);

		float[] distanceInMeters = new float[1];
		Location.distanceBetween(reportLat, reportLong, locationLat, locationLong, distanceInMeters);
		return distanceInMeters[0] > allowedDistanceInMeters;
	}

	private JSONObject getClosestLocationByTime(long reportTime)
	{
		// Load driving history (last 24-hours)
		List<JSONObject> drivingPath = JSONUtils.loadJSONObjects(context, JSONUtils.DRIVING_PATH_FILENAME);

		if (drivingPath.isEmpty())
			return null;

		JSONObject reportLocation = new JSONObject();
		try
		{
			reportLocation.put(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME, reportTime);
		}
		catch (JSONException ignore)
		{
			// no-op
		}

		int binarySearch = Collections.binarySearch(drivingPath, reportLocation, new Comparator<JSONObject>()
		{
			@Override
			public int compare(JSONObject lhs, JSONObject rhs)
			{
				try
				{
					long lhsTime = lhs.getLong(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME);
					long rhsTime = rhs.getLong(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME);
					return (int) (lhsTime - rhsTime);
				}
				catch (JSONException ignore)
				{
					return 0;
				}
			}
		});

		if (binarySearch < 0)
		{
			int index = Math.abs(binarySearch) - 1;
			// If report is newer than the lastest location just return the latest location
			if (index == drivingPath.size())
				return drivingPath.get(index - 1);

			long timeBeforeReport = drivingPath.get(index - 1).optLong(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME);
			long timeAfterReport = drivingPath.get(index).optLong(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME);

			// Get the location with the closest time to the report
			JSONObject closestLocation = reportTime - timeBeforeReport < timeAfterReport - reportTime ? drivingPath.get(index - 1) : drivingPath.get(index);

			return closestLocation;
		}
		else
		{
			// The unlikely case where we have a location with the exact time as the report
			return drivingPath.get(binarySearch);
		}
	}

	private String getReportedMessage(JSONObject jsonReport, boolean longMessage)
	{
		int reportCode = jsonReport.optInt(JSONKeys.REPORT_KEY_CODE);
		String reportDescription = ReportUtils.getReportDescription(context, reportCode);
		String message = reportDescription;

		if (!longMessage)
			return message;

		// Resolve report address for geo-points
		double latitude = jsonReport.optDouble(JSONKeys.REPORT_KEY_LATITUDE);
		double longitude = jsonReport.optDouble(JSONKeys.REPORT_KEY_LONGITUDE);
		Address address = ReportUtils.resolveLocationToAddress(context, latitude, longitude);
		String addressStr = ReportUtils.getSingleLineAddress(context, address);
		if (!addressStr.equals(context.getString(R.string.unresolved_address)))
			message += " " + context.getString(R.string.notification_reported_at) + addressStr;

		return message;
	}

	private Intent getReportIntent(JSONObject jsonReport)
	{
		Intent showMapIntent = new Intent(context, ReportMapActivity.class);
		ArrayList<JSONObject> jsonReportList = new ArrayList<JSONObject>();
		jsonReportList.add(jsonReport);
		ArrayList<HashMap<String, String>> reportList = ReportUtils.getJsonReportsAsListRows(context, jsonReportList);

		showMapIntent.putExtra(ReportMapActivity.EXTRA_REPORT_LIST, reportList);
		showMapIntent.putExtra(ReportMapActivity.EXTRA_SELECTED_REPORT_INDEX, 0);

		return showMapIntent;
	}
}