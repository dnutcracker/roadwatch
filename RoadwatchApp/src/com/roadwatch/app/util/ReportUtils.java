package com.roadwatch.app.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.text.format.DateUtils;
import android.util.Log;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerErrorCodes;

public class ReportUtils
{
	// Logging tag
	private static final String TAG = ReportUtils.class.getSimpleName();

	private static Map<String, Integer> reportDescriptionToReportCodeMap = new HashMap<String, Integer>();
	private static Map<String, Address> addressCache = new HashMap<String, Address>();

	private static final long MINIMAL_DISTANCE_BETWEEN_REPORTS = 2 * 60 * 1000;

	public static final String PROPERTY_LAST_REPORT_TIME = "lastReportTime";

	/**
	 * This method is public to allow us to invoke it manually when language was changed without restart of the application.
	 * @param context
	 */
	public static void initResources(Context context)
	{
		reportDescriptionToReportCodeMap.clear();
		addressCache.clear();

		List<Entry<String, Integer>> reportEntries = getReportEntryList(context, R.array.danger_level_1_report_codes);
		reportEntries.addAll(getReportEntryList(context, R.array.danger_level_2_report_codes));
		reportEntries.addAll(getReportEntryList(context, R.array.danger_level_3_report_codes));

		for (Entry<String, Integer> entry : reportEntries)
			reportDescriptionToReportCodeMap.put(entry.getKey(), entry.getValue());
	}

	private static int getReportIndex(List<JSONObject> reports, long reportTime)
	{
		try
		{
			for (int i = 0; i < reports.size(); i++)
			{
				if (reports.get(i).getLong(JSONKeys.REPORT_KEY_TIME) == reportTime)
					return i;
			}
		}
		catch (JSONException e)
		{
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}

		Log.w(TAG, "Failed to find report index in loaded reports. Report time : " + new Date(reportTime));
		return -1;
	}

	public static final void deleteReport(Context context, JSONObject jsonReport)
	{
		try
		{
			long reportTime = jsonReport.getLong(JSONKeys.REPORT_KEY_TIME);
			deleteReport(context, reportTime);
		}
		catch (JSONException e)
		{
			Log.e(TAG, "Failed to get report time from JSON object", e);
		}
	}

	public static final void deleteReport(Context context, long reportTime)
	{
		// Load all reports
		List<JSONObject> loadedReports = JSONUtils.loadJSONObjects(context, JSONUtils.UNSENT_REPORTS_FILENAME);
		int reportToRemoveIndex = getReportIndex(loadedReports, reportTime);
		if (reportToRemoveIndex == -1)
		{
			Log.e(TAG, "Delete report failed - Can't find offline report on local storage. Report time : " + new Date(reportTime));
			return;
		}
		else
		{
			// Remove the sent report
			if (loadedReports.remove(reportToRemoveIndex) != null)
				Log.d(TAG, "Successfully removed report");
			else
				Log.e(TAG, "Failed to remove report. Report time : " + new Date(reportTime));
		}

		// Save the modified list overwriting the file contents
		JSONUtils.saveJSONObjects(context, JSONUtils.UNSENT_REPORTS_FILENAME, loadedReports);
		Log.d(TAG, "Successfully updated report list");
	}

	/**
	 * Use reflection to obtain report codes and descriptions for the specified danger level report codes
	 * @return 
	 */
	public static List<Entry<String, Integer>> getReportEntryList(Context context, int dangerLevelReportCodesResId)
	{
		String[] reportCodes = context.getResources().getStringArray(dangerLevelReportCodesResId);
		List<Entry<String, Integer>> reportEntryList = new ArrayList<Entry<String, Integer>>(reportCodes.length);

		try
		{
			for (String code : reportCodes)
			{
				Integer reportCode = Integer.valueOf(code);
				Field f = R.string.class.getField("_" + code);
				String reportDescription = context.getString(((Integer) f.get(null)).intValue());

				Entry<String, Integer> reportEntry = new SimpleEntry<String, Integer>(reportDescription, reportCode);
				reportEntryList.add(reportEntry);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to construct report entry list : " + e.getMessage());
		}

		return reportEntryList;
	}

	public static int getReportDangerLevel(Context context, int reportCode)
	{
		String[] reportCodes = context.getResources().getStringArray(R.array.danger_level_1_report_codes);
		for (int i = 0; i < reportCodes.length; i++)
		{
			if (reportCodes[i].equals("" + reportCode))
				return 1;
		}

		reportCodes = context.getResources().getStringArray(R.array.danger_level_2_report_codes);
		for (int i = 0; i < reportCodes.length; i++)
		{
			if (reportCodes[i].equals("" + reportCode))
				return 2;
		}

		reportCodes = context.getResources().getStringArray(R.array.danger_level_3_report_codes);
		for (int i = 0; i < reportCodes.length; i++)
		{
			if (reportCodes[i].equals("" + reportCode))
				return 3;
		}

		return -1;
	}

	public static int getReportCode(Context context, CharSequence reportDescription)
	{
		if (reportDescriptionToReportCodeMap.isEmpty())
			initResources(context);

		// PENDING: Got a NPE here ! check why !
		return reportDescription.length() > 0 ? reportDescriptionToReportCodeMap.get(reportDescription).intValue() : -1;
	}

	public static String getReportDescription(Context context, int reportCode)
	{
		if (reportCode == -1)
			return "";

		if (reportDescriptionToReportCodeMap.isEmpty())
			initResources(context);

		Set<Entry<String, Integer>> reportsEntrySet = reportDescriptionToReportCodeMap.entrySet();
		for (Entry<String, Integer> reportEntry : reportsEntrySet)
		{
			if (reportEntry.getValue().intValue() == reportCode)
				return reportEntry.getKey();
		}

		Log.wtf(TAG, "Could not find a report description for reportCode=" + reportCode);
		return null;
	}

	public static Address resolveLocationToAddress(Context context, double latitude, double longitude)
	{
		Address cachedAddress = addressCache.get("" + latitude + longitude);
		if (cachedAddress != null)
			return cachedAddress;

		// PENDING: We might want to use a 2 separate address resolution locales - the one defined by the user for display and
		// a fixed locale (English?) to store on the server. 
		Locale preferredLocale = context.getResources().getConfiguration().locale;

		// Get a new geocoding service instance, set for localized addresses. 
		Geocoder geocoder = new Geocoder(context, preferredLocale);

		// Create a list to contain the result address
		List<Address> addresses = null;

		try
		{
			// Call the synchronous getFromLocation() method with the latitude and longitude of the current location			
			addresses = geocoder.getFromLocation(latitude, longitude, 1);
			if (addresses != null && addresses.size() > 0)
			{
				Address address = addresses.get(0);
				addressCache.put("" + latitude + longitude, address);
				return address;
			}
			else
				return null;
		}
		catch (IOException e) // Catch network or other I/O problems.
		{
			// We might get 'Service not available' on some devices due to Google Services update - reboot solves this issue
			e.printStackTrace();

			// Log an error and return an error message
			Log.w(TAG, "Failed to resolve address for location (" + latitude + "," + longitude + ")");
			Log.w(TAG, e.toString());

			return null;
		}
		catch (IllegalArgumentException e) // Catch incorrect latitude or longitude values
		{
			e.printStackTrace();

			// Log the error and print the stack trace
			Log.e(TAG, e.toString());

			return null;
		}
	}

	/**
	 * Extracts the street, city and country from the Address object.
	 * @param address
	 * @return A list consisting of 3 elements by the order of street, city and country.
	 */
	public static List<String> getStreetCityCountry(Address address)
	{
		List<String> addressList = new ArrayList<String>();

		String street = "";
		street += address.getAddressLine(0);
		// Get city by the following precedence : locality, admin area, sub-admin area
		String city = address.getAddressLine(1) !=null ? address.getAddressLine(1) : "";//address.getLocality() != null ? address.getLocality() : address.getSubAdminArea()!=null ? address.getSubAdminArea() : address.getAdminArea()!=null ? address.getAdminArea() : "";		
		String country = address.getCountryName()!=null ? address.getCountryName() : "";

		// When city and street are the same value, use it only as city
		addressList.add(street.equals(city) ? "" : street);
		addressList.add(city);
		addressList.add(country);

		return addressList;
	}

	public static String getSingleLineAddress(String street, String city, String country)
	{
		String address = "";
		if (!street.isEmpty() && !city.isEmpty())
			address = street + " " + city;
		else if (street.isEmpty())
			address = city;

		return address;
	}

	public static String getSingleLineAddress(Context context, Address address)
	{
		if (address != null)
		{
			List<String> streetCityCountry = getStreetCityCountry(address);
			return getSingleLineAddress(streetCityCountry.get(0), streetCityCountry.get(1), streetCityCountry.get(2));
		}
		else
			return context.getString(R.string.unresolved_address);
	}

	/**
	 * Prepares the data from the json report objects for the list adapter
	 * 
	 * @param jsonReportList
	 */
	public static ArrayList<HashMap<String, String>> getJsonReportsAsListRows(Context context, List<JSONObject> jsonReportList)
	{
		ArrayList<HashMap<String, String>> resultsRows = new ArrayList<HashMap<String, String>>();
		try
		{
			for (JSONObject report : jsonReportList)
			{
				// Add reported license plate
				HashMap<String, String> row = new HashMap<String, String>();
				if (report.has(JSONKeys.DATASTORE_KEY))
					row.put(JSONKeys.DATASTORE_KEY, report.getJSONObject(JSONKeys.DATASTORE_KEY).toString());
				String licensePlate = report.getString(JSONKeys.REPORT_KEY_LICENSE_PLATE);
				row.put(JSONKeys.REPORT_KEY_LICENSE_PLATE, licensePlate);

				if (report.has(JSONKeys.REPORT_KEY_CODE))
				{
					int reportCode = report.getInt(JSONKeys.REPORT_KEY_CODE);
					row.put(JSONKeys.REPORT_KEY_CODE, String.valueOf(reportCode));
					row.put(JSONKeys.REPORT_KEY_DESCRIPTION, ReportUtils.getReportDescription(context, reportCode));
				}

				// Add report geo location (address resolving is done in async task)
				double latitude = report.getDouble(JSONKeys.REPORT_KEY_LATITUDE);
				row.put(JSONKeys.REPORT_KEY_LATITUDE, String.valueOf(latitude));
				double longitude = report.getDouble(JSONKeys.REPORT_KEY_LONGITUDE);
				row.put(JSONKeys.REPORT_KEY_LONGITUDE, String.valueOf(longitude));

				// Add report time
				long reportTime = Long.valueOf(report.getString(JSONKeys.REPORT_KEY_TIME)).longValue();
				row.put(JSONKeys.REPORT_KEY_TIME, String.valueOf(reportTime));
				String reportTimeForDisplay = DateUtils.getRelativeTimeSpanString(reportTime, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
				row.put(JSONKeys.REPORT_KEY_TIME_FOR_DISPLAY, reportTimeForDisplay);
				JSONArray jsonReportedByArray = report.getJSONArray(JSONKeys.REPORT_KEY_REPORTED_BY);
				row.put(JSONKeys.REPORT_KEY_REPORTED_BY, jsonReportedByArray.toString());

				resultsRows.add(row);
			}
		}
		catch (JSONException e)
		{
			String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonReportList.get(0));
			Log.e(TAG, errorMessage);
		}

		return resultsRows;
	}

	public static boolean isReportingSelf(Context context, String reportedPlate)
	{
		// In debug mode you can go crazy !! 
		if (Utils.isDebugMode(context))
			return false;

		String reportedBy = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		return reportedPlate.equals(reportedBy);
	}

	public static boolean isExcessiveReporting(Context context, long reportTime)
	{
		// In debug mode you can go crazy !! 
		if (Utils.isDebugMode(context))
			return false;

		long lastReportTime = PreferencesUtils.getLong(context, PROPERTY_LAST_REPORT_TIME, 0);
		return reportTime - lastReportTime < MINIMAL_DISTANCE_BETWEEN_REPORTS;
	}

	public static int getNumberOfWitnesses(Map<String, String> reportRow)
	{
		return reportRow.get(JSONKeys.REPORT_KEY_REPORTED_BY) != null ? reportRow.get(JSONKeys.REPORT_KEY_REPORTED_BY).split(",").length : 0;
	}

	public static Bitmap getReportMapPinBitmap(Context context, HashMap<String, String> reportRow)
	{
		String reportKeyCodeStr = reportRow.get(JSONKeys.REPORT_KEY_CODE);
		// Report code may be null when showing map for unsent reports
		int dangerLevel = reportKeyCodeStr != null ? ReportUtils.getReportDangerLevel(context, Integer.parseInt(reportKeyCodeStr)) : -1;
		int resId = -1;
		switch (dangerLevel)
		{
		case 1:
			resId = R.drawable.danger1_pin;
			break;
		case 2:
			resId = R.drawable.danger2_pin;
			break;
		case 3:
			resId = R.drawable.danger3_pin;
			break;
		}

		if (resId != -1)
		{
			Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);

			int witnessesNumber = getNumberOfWitnesses(reportRow);// + ((int)(Math.random() * 10));
			if (witnessesNumber > 1)
			{
				// Overlay with the number of witnesses			
				bitmap = Utils.addTextOverlay(context, bitmap, "+" + witnessesNumber, 28, 9);
			}

			return bitmap;
		}

		// Happens when report code is unknown (like for unsent reports)
		return null;
	}
}