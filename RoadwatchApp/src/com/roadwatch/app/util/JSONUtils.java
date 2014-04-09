package com.roadwatch.app.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.util.Log;

public class JSONUtils
{
	// Logging tag
	private static final String TAG = JSONUtils.class.getSimpleName();

	// Filenames used to store JSON files
	public static final String UNSENT_REPORTS_FILENAME = "offlineReports";
	public static final String DRIVING_PATH_FILENAME = "drivingPath";
	public static final String LOGGED_IN_USER_DATA = "userData";

	/**
	 * Creates a JSONObject with just an errorCode<BR>
	 * Since the errorMessage is not displayed to the user, it is not necessary to specify it. 
	 * @param errorCode
	 * @return A JSONObject containing the specified errorCode in its RESPONSE_KEY_ERROR_CODE key.
	 */
	public static final JSONObject createJSONObjectWithError(int errorCode)
	{
		return createJSONObjectWithError(errorCode, "");
	}

	/**
	 * Used internally for a cleaner way to return an error instead of throwing an exception
	 * 
	 * @param errorCode
	 * @param errorMessage 
	 * @return A JSONObject containing the specified errorCode in its RESPONSE_KEY_ERROR_CODE key and errorMessage 
	 * in its RESPONSE_KEY_ERROR key.
	 */
	public static final JSONObject createJSONObjectWithError(int errorCode, String errorMessage)
	{
		JSONObject jsonObjectWithError = new JSONObject();
		try
		{
			jsonObjectWithError.put(JSONKeys.RESPNOSE_KEY_ERROR_CODE, String.valueOf(errorCode));
			jsonObjectWithError.put(JSONKeys.RESPNOSE_KEY_ERROR, errorMessage);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return jsonObjectWithError;
	}

	public static final JSONObject convertReportToJSONObject(String licensePlate, double latitude, double longitude, int reportCode, long reportTime, String reportedBy)
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(JSONKeys.USER_KEY_LICENSE_PLATE, licensePlate);
			jsonObject.put(JSONKeys.REPORT_KEY_LATITUDE, latitude);
			jsonObject.put(JSONKeys.REPORT_KEY_LONGITUDE, longitude);
			jsonObject.put(JSONKeys.REPORT_KEY_CODE, reportCode);//reportCode > 0 ? String.valueOf(reportCode) : null);
			jsonObject.put(JSONKeys.REPORT_KEY_TIME, reportTime);
			List<String> reportedByList = new ArrayList<String>();
			reportedByList.add(reportedBy);
			jsonObject.put(JSONKeys.REPORT_KEY_REPORTED_BY, new JSONArray(reportedByList));

			return jsonObject;
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts a single JSON object string into a JSONObject
	 * @param jsonString
	 * @return
	 */
	public static final JSONObject convertStringToJSONObject(String jsonString)
	{
		try
		{
			String decodedJsonString = URLDecoder.decode(jsonString, "UTF-8");
			JSONTokener tokener = new JSONTokener(decodedJsonString.trim());
			return (JSONObject) tokener.nextValue();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts the received JSON string into an array of JSON objects.
	 * In case the server result is a single JSON object (not an array), A list with a single JSONObject will be returned
	 * @param jsonString A string representing a JSON array or a single JSON object
	 * @return A List of JSONObjects
	 */
	public static final List<JSONObject> convertStringToJSONArray(String jsonString)
	{
		List<JSONObject> jsonObjects = new ArrayList<JSONObject>();

		try
		{
			JSONTokener tokener = new JSONTokener(jsonString.trim());
			while (tokener.more())
			{
				Object value = tokener.nextValue();
				if (value instanceof JSONObject)
					jsonObjects.add((JSONObject) value);
				else if (value instanceof JSONArray)
				{
					JSONArray jsonArray = (JSONArray) value;
					for (int i = 0; i < jsonArray.length(); i++)
						jsonObjects.add((JSONObject) jsonArray.get(i));
				}
			}

			return jsonObjects;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Saves a single JSON object to file. By default we append to existing content.
	 * @param context
	 * @param filename
	 * @param jsonObject
	 */
	public static final void saveJSONObject(Context context, String filename, JSONObject jsonObject)
	{
		List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
		jsonObjects.add(jsonObject);
		saveJSONObjects(context, filename, jsonObjects, true);
	}

	/**
	 * Saves a single JSON object to a file.
	 * 
	 * @param context
	 * @param jsonObject The report to save
	 */
	public static final void saveJSONObject(Context context, String filename, JSONObject jsonObject, boolean append)
	{
		List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
		jsonObjects.add(jsonObject);
		saveJSONObjects(context, filename, jsonObjects, append);
	}

	/**
	 * Saves a list of JSON objects to file. By default we overwrite existing content.
	 * 
	 * @param context
	 * @param filename
	 * @param jsonObjects
	 */
	public static final void saveJSONObjects(Context context, String filename, List<JSONObject> jsonObjects)
	{
		saveJSONObjects(context, filename, jsonObjects, false);
	}

	/**
	 * Saves a list of JSON objects to file.
	 * 
	 * @param context
	 * @param filename
	 * @param jsonObjects
	 * @param append
	 */
	public static final void saveJSONObjects(Context context, String filename, List<JSONObject> jsonObjects, boolean append)
	{
		try
		{
			FileOutputStream fos = context.openFileOutput(filename, append ? Context.MODE_PRIVATE | Context.MODE_APPEND : Context.MODE_PRIVATE);
			for (JSONObject report : jsonObjects)
				fos.write(report.toString().getBytes(Utils.DEFAULT_CHARSET));
			fos.flush();
			fos.close();
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Returns all the JSON objects stored in the specified file.
	 * 
	 * @param context
	 * @param filename
	 * @return A <code>List</code> of <code>JSONObject</code>s
	 */
	public static final List<JSONObject> loadJSONObjects(Context context, String filename)
	{
		File f = new File(context.getFilesDir(), filename);

		if (!f.exists())
		{
			Log.i(TAG, f + " does not exist. returning an empty list");
			return new ArrayList<JSONObject>();
		}

		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(f.getName())));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null)
				sb.append(line + "\n");
			reader.close();

			List<JSONObject> jsonArray = convertStringToJSONArray(sb.toString());
			if (jsonArray != null)
			{
				return jsonArray;
			}
			else
			{
				Log.e(TAG, "Failed to convert " + filename + " content to JSONArray. Corrupt content: " + sb.toString());

				// File context is corrupted - remove it and return an empty list
				deleteJSONObjects(context, filename);
				return new ArrayList<JSONObject>();
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			return new ArrayList<JSONObject>();
		}
	}

	public static final void deleteJSONObjects(Context context, String filename)
	{
		File f = new File(context.getFilesDir(), filename);
		boolean deleted = f.delete();
		if (deleted)
			Log.i(TAG, filename + " deleted ");
		else
			Log.w(TAG, "Failed to delete " + filename);
	}
}