package com.roadwatch.app.server;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;

/**
 * This class mirrors the available calls we can invoke on the app server.
 * 
 * @author Nati created at : 13/10/2013 23:52:47
 */
public class ServerAPI
{
	// Servlet names
	private static String API_NAMESPACE = "api";
	private static String SERVLET_LOGIN = API_NAMESPACE + "/login";
	private static String SERVLET_REGISTER = API_NAMESPACE + "/register";
	private static String SERVLET_ADD_REPORT = API_NAMESPACE + "/addReport";
	private static String SERVLET_REMOVE_REPORT = API_NAMESPACE + "/removeReport";
	private static String SERVLET_GET_REPORTS = API_NAMESPACE + "/getReports";
	private static String SERVLET_GET_USERS = API_NAMESPACE + "/getUsers";
	private static String SERVLET_MANAGE_TRACKED_LICENSE_PLATES = API_NAMESPACE + "/manageTrackedLicensePlates";

	private Context context;

	public ServerAPI(Context context)
	{
		this.context = context;
	}

	/**
	 * Registers this device with GCM and return the registration ID.
	 * This method uses exponential backoff.
	 * 
	 * @param context
	 * @return The GCM registration ID
	 */
	public JSONObject registerWithGCM()
	{
		return ServerConnectionManager.INSTANCE.registerWithGCM(context).get(0);
	}

	/**
	 * Register user
	 * @param licensePlate
	 * @param name
	 * @param email
	 * @param gcmRegID 
	 * */
	public JSONObject registerUser(String licensePlate, String email, String username, String password)
	{
		JSONObject jsonRequest = new JSONObject();
		try
		{
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, licensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_EMAIL, email);
			jsonRequest.put(JSONKeys.USER_KEY_USERNAME, username);
			jsonRequest.put(JSONKeys.USER_KEY_PASSWORD, password);
			// Will be mapped to an enum value in the server
			jsonRequest.put(JSONKeys.USER_KEY_TYPE, "REGULAR");
			String gcmRegID = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_GCM_REGISTRATION_ID);
			// The server expects reg ids as a list
			List<String> gcmRegistrationIDAsList = new ArrayList<String>();
			gcmRegistrationIDAsList.add(gcmRegID);
			jsonRequest.put(JSONKeys.USER_KEY_OWN_GCM_IDS, new JSONArray(gcmRegistrationIDAsList));
			jsonRequest.put(JSONKeys.USER_KEY_APP_VERSION, Utils.getAppVersionName(context));
			jsonRequest.put(JSONKeys.USER_KEY_ANDROID_VERSION, Utils.getAndroidVersionAndDevice());

			JSONObject jsonResponse = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_REGISTER, jsonRequest).get(0);
			return jsonResponse;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Login
	 * 
	 * @param licensePlate
	 * @param password
	 * */
	public JSONObject loginUser(String licensePlate, String password)
	{
		JSONObject jsonRequest = new JSONObject();
		try
		{
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, licensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_PASSWORD, password);
			jsonRequest.put(JSONKeys.USER_KEY_APP_VERSION, Utils.getAppVersionName(context));
			jsonRequest.put(JSONKeys.USER_KEY_ANDROID_VERSION, Utils.getAndroidVersionAndDevice());

			JSONObject jsonResponse = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_LOGIN, jsonRequest).get(0);
			return jsonResponse;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	public JSONObject autoLoginUser()
	{
		String loginToken = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
		return autoLoginUser(loginToken, "", "", "", null);
	}

	public JSONObject autoLoginUser(List<String> gcmRegIDs)
	{
		String loginToken = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
		return autoLoginUser(loginToken, "", "", "", gcmRegIDs);
	}

	public JSONObject autoLoginUser(String username, String email, String password)
	{
		String loginToken = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
		return autoLoginUser(loginToken, username, email, password, null);
	}

	public JSONObject autoLoginUser(String loginToken)
	{
		return autoLoginUser(loginToken, "", "", "", null);
	}

	/**
	 * Auto-Login using the login token
	 * 
	 * @return
	 */
	public JSONObject autoLoginUser(String loginToken, String username, String email, String password, List<String> gcmRegIDs)
	{
		JSONObject jsonRequest = new JSONObject();
		try
		{
			jsonRequest.put(JSONKeys.USER_KEY_UUID, loginToken);
			String gcmRegID = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_GCM_REGISTRATION_ID);
			jsonRequest.put(JSONKeys.USER_KEY_OWN_GCM_ID, gcmRegID);
			jsonRequest.put(JSONKeys.USER_KEY_APP_VERSION, Utils.getAppVersionName(context));
			jsonRequest.put(JSONKeys.USER_KEY_ANDROID_VERSION, Utils.getAndroidVersionAndDevice());
			if (gcmRegIDs != null && !gcmRegIDs.isEmpty())
				jsonRequest.put(JSONKeys.USER_KEY_OWN_GCM_IDS, new JSONArray(gcmRegIDs));

			// Theses 3 properties are used when user is changing its account information
			if (!username.isEmpty())
				jsonRequest.put(JSONKeys.USER_KEY_USERNAME, username);
			if (!email.isEmpty())
				jsonRequest.put(JSONKeys.USER_KEY_EMAIL, email);
			if (!password.isEmpty())
				jsonRequest.put(JSONKeys.USER_KEY_PASSWORD, password);

			JSONObject jsonResponse = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_LOGIN, jsonRequest).get(0);
			return jsonResponse;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sends the report to the remote database.
	 * 
	 * 07/12/2013 - Check why we got java.io.EOFException: (at libcore.net.http.HttpEngine.readResponseHeaders(HttpEngine.java:573)
	 * @param reportTime 
	 */
	public JSONObject addReport(String licensePlate, double latitude, double longitude, int actionCode, long reportTime, String reportedBy)
	{
		JSONObject jsonRequest = JSONUtils.convertReportToJSONObject(licensePlate, latitude, longitude, actionCode, reportTime, reportedBy);
		JSONObject jsonResponse = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_ADD_REPORT, jsonRequest).get(0);
		return jsonResponse;
	}

	public JSONObject removeReport(JSONArray reportedBy, long reportTime)
	{
		JSONObject jsonRequest = new JSONObject();
		try
		{
			jsonRequest.put(JSONKeys.REPORT_KEY_REPORTED_BY, reportedBy);
			jsonRequest.put(JSONKeys.REPORT_KEY_TIME, reportTime);

			JSONObject jsonResponse = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_REMOVE_REPORT, jsonRequest).get(0);
			return jsonResponse;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns all the reports on the specified license plate
	 * 
	 * @param licensePlate
	 * @param jsonCursor null if querying from the beginning
	 * @return
	 */
	public List<JSONObject> getReportsOn(String licensePlate, JSONObject jsonCursor)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.REPORT_KEY_LICENSE_PLATE, licensePlate);
			jsonRequest.put(JSONKeys.QUERY_KEY_PAGE_SIZE, jsonCursor != null ? 100 : 30);
			if (jsonCursor != null)
				jsonRequest.put(JSONKeys.QUERY_KEY_CURSOR, jsonCursor.get(JSONKeys.QUERY_KEY_ENCODED_CURSOR));

			List<JSONObject> jsonReports = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_GET_REPORTS, jsonRequest);
			return jsonReports;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns all the reports reported by the specified license plate
	 * 
	 * @param licensePlate
	 * @param jsonCursor null if querying from the beginning
	 * @return
	 */
	public List<JSONObject> getReportsBy(String licensePlate, long fromTime, long toTime, JSONObject jsonCursor)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.REPORT_KEY_REPORTED_BY, licensePlate);
			// Get 30 at the first request and 100 at the rest 
			jsonRequest.put(JSONKeys.QUERY_KEY_PAGE_SIZE, jsonCursor != null ? 100 : 30);
			jsonRequest.put(JSONKeys.QUERY_KEY_FROM_TIME, fromTime);
			jsonRequest.put(JSONKeys.QUERY_KEY_TO_TIME, toTime);
			if (jsonCursor != null)
				jsonRequest.put(JSONKeys.QUERY_KEY_CURSOR, jsonCursor.get(JSONKeys.QUERY_KEY_ENCODED_CURSOR));

			List<JSONObject> jsonReports = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_GET_REPORTS, jsonRequest);
			return jsonReports;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns all the reports reported by the specified license plate
	 * 
	 * @param licensePlate
	 * @param jsonCursor null if querying from the beginning
	 * @return
	 */
	public List<JSONObject> getUserByLicensePlate(String licensePlate)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, licensePlate);

			List<JSONObject> jsonUsers = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_GET_USERS, jsonRequest);
			return jsonUsers;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Adds a new tracked car to the tracking user
	 * @return
	 */
	public JSONObject addTrackedLicensePlate(String trackingLicensePlate, String newTrackedLicensePlate, String newTrackedName)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, trackingLicensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE, newTrackedLicensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_TRACKED_NAME, newTrackedName);

			JSONObject jsonUser = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_MANAGE_TRACKED_LICENSE_PLATES, jsonRequest).get(0);
			return jsonUser;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Removes an existing tracked car from the tracking user
	 * @return
	 */
	public JSONObject removeTrackedLicensePlate(String trackingLicensePlate, String newTrackedLicensePlate)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, trackingLicensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE, newTrackedLicensePlate);

			JSONObject jsonUser = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_MANAGE_TRACKED_LICENSE_PLATES, jsonRequest).get(0);
			return jsonUser;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Updates an existing tracked car
	 * @return
	 */
	public JSONObject updateTrackedLicensePlate(String trackingLicensePlate, String newTrackedLicensePlate, String newTrackedName, String existingTrackedLicensePlate)
	{
		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(JSONKeys.USER_KEY_LICENSE_PLATE, trackingLicensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_NEW_TRACKED_LICENSE_PLATE, newTrackedLicensePlate);
			jsonRequest.put(JSONKeys.USER_KEY_TRACKED_NAME, newTrackedName);
			jsonRequest.put(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATE, existingTrackedLicensePlate);

			JSONObject jsonUser = ServerConnectionManager.INSTANCE.sendServerAndGetResponse(context, SERVLET_MANAGE_TRACKED_LICENSE_PLATES, jsonRequest).get(0);
			return jsonUser;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}
}