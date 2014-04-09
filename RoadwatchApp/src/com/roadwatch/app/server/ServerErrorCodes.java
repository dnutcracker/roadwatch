package com.roadwatch.app.server;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.roadwatch.app.R;
import com.roadwatch.app.util.JSONKeys;

/**
 * Used to convert the error codes recieved from the server into meaningful messages.
 * 
 * PENDING:
 * 1. Change raw error codes int values into an enum
 * 
 * @author Nati
 */
public class ServerErrorCodes
{
	// Debugging tag for the activity
	private static final String CLASS_TAG = ServerErrorCodes.class.getSimpleName();

	public static final int getErrorCode(JSONObject jsonObject)
	{
		try
		{
			return Integer.parseInt(jsonObject.getString(JSONKeys.RESPNOSE_KEY_ERROR_CODE));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			Log.e(CLASS_TAG, "Attempting to get an error code for a JSON object that contains no error!");
			e.printStackTrace();
		}

		return -1;
	}

	public static final boolean isKnownError(JSONObject jsonErrorObject)
	{
		return getErrorCode(jsonErrorObject) != 199;
	}

	/**
	 * 
	 * @param jsonErrorObject
	 * @return true if the error on the server happened due to the user's mistake.
	 */
	public static final boolean isErrorCausedByUser(JSONObject jsonErrorObject)
	{
		switch (getErrorCode(jsonErrorObject))
		{
		case 12:
			return true;
		default:
			return false;
		}
	}

	public static final boolean hasError(JSONObject jsonObject)
	{
		return jsonObject.has(JSONKeys.RESPNOSE_KEY_ERROR_CODE);
	}

	public static final String getErrorMessage(Context context, JSONObject jsonObject, String... params)
	{
		try
		{
			int errorCode = Integer.parseInt(jsonObject.getString(JSONKeys.RESPNOSE_KEY_ERROR_CODE));
			switch (errorCode)
			{
			// Login/Register errors:[0-9]
			case 0:
				// Server failed to login user
				return context.getString(R.string.db_failed_to_login_user);
			case 1:
				// No user found with these credentials
				return context.getString(R.string.bad_credentails);
			case 2:
				// Auto-login failed - No user found for this login token (user no longer exist)
				return context.getString(R.string.bad_login_token);
			case 3:
				// User with this license plate already exists
				return context.getString(R.string.license_plate_already_exist);
			case 4:
				// Failed to store user in DB
				return context.getString(R.string.db_failed_to_store_user);
				
				// Report errors:[10-19]
			case 10:
				// Failed to add report to DB
				return context.getString(R.string.db_failed_to_add_report);
			case 11:
				// Failed to get reports from DB
				return context.getString(R.string.db_failed_to_get_reports);
			case 12:
				return context.getString(R.string.report_too_frequent_error, params[0]);
			case 13:
				return context.getString(R.string.cannot_remove_report_reported_by_others);

				// General Server errors: [100-199]
			case 100:
				return context.getString(R.string.connection_timeout);
			case 101:
				return context.getString(R.string.failed_to_parse_data_from_server);
			case 150:
				return context.getString(R.string.failed_to_register_user_with_gcm);
			case 199:
				return context.getString(R.string.unexpected_server_error);

			default:
				return "Unknown remote error code : " + errorCode;
			}
		}
		catch (NumberFormatException e)
		{
			Log.e(CLASS_TAG, e.toString());
			return e.getMessage();
		}
		catch (JSONException e)
		{
			Log.e(CLASS_TAG, e.toString());
			return e.getMessage();
		}
	}
}
