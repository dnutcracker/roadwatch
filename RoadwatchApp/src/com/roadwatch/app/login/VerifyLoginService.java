package com.roadwatch.app.login;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.wizards.LoginWizardActivity;

/**
 * The service verify the user's credentials against our app server.
 * This service is scheduled to run once a day and when the user explicitly starts the application
 *  
 * @author Nati created at : 13/10/13 23:21:26
 */
public class VerifyLoginService extends IntentService
{
	private static final String TAG = VerifyLoginService.class.getSimpleName();

	public static final int USER_CREDENTIALS_VERIFICATION_FAILED_NOTIFICATION_ID = 2000;

	// Used when the service is invoked outside the regular bg scheduler (like when starting the application)
	public static final String EXTRA_EXPLICIT_LOGIN = "explicityLogin";

	public VerifyLoginService()
	{
		super(VerifyLoginService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		boolean explicit = intent.getBooleanExtra(EXTRA_EXPLICIT_LOGIN, false);

		if (Utils.isDeviceOnline(this))
		{
			generateGCMRegID();

			verifyUserLoginCredentials();
		}
		else
			Log.w(TAG, "Device is offline, cannot verify user credentails. Will try again on next wakeup");

		if (!explicit)
			purgeOldLocationsAndReports();
	}

	private void purgeOldLocationsAndReports()
	{
		final long YESTERDAY = System.currentTimeMillis() - 24 * 60 * 60 * 1000;

		// Remove old locations from history
		int locationsRemoved = purgeOld(JSONUtils.DRIVING_PATH_FILENAME, JSONKeys.USER_CURRENT_LOCATION_KEY_TIME, YESTERDAY);
		if (locationsRemoved > 0)
			Log.i(TAG, "Removed " + locationsRemoved + " stale locations from history");

		// Remove old unsent reports
		int reportsRemoved = purgeOld(JSONUtils.UNSENT_REPORTS_FILENAME, JSONKeys.REPORT_KEY_TIME, YESTERDAY);
		if (reportsRemoved > 0)
			Log.i(TAG, "Removed " + reportsRemoved + " old unsent reports");
	}

	/**
	 * Performs a binary search of the time limit on the elements inside the specified filename and removed the
	 * items which are older.
	 * 
	 * @param jsonFilename The filename containing the elements to purge
	 * @param jsonKey The JSON key by which to retrieve the time
	 * @param timeLimit The oldest time allowed
	 * @return The number of elements removed
	 */
	private int purgeOld(String jsonFilename, final String jsonKey, long timeLimit)
	{
		List<JSONObject> jsonList = JSONUtils.loadJSONObjects(this, jsonFilename);

		try
		{
			JSONObject timeLimitJSONObject = new JSONObject();
			timeLimitJSONObject.put(jsonKey, timeLimit);

			int binarySearch = Collections.binarySearch(jsonList, timeLimitJSONObject, new Comparator<JSONObject>()
			{
				@Override
				public int compare(JSONObject lhs, JSONObject rhs)
				{
					try
					{
						long lhsTime = lhs.getLong(jsonKey);
						long rhsTime = rhs.getLong(jsonKey);
						return (int) (lhsTime - rhsTime);
					}
					catch (JSONException ignore)
					{
						return 0;
					}
				}
			});

			if (binarySearch != -1)
			{
				// Elements before this index are too old
				int endIndex = Math.abs(binarySearch) - 1;
				List<JSONObject> nonStaleSubList = jsonList.subList(endIndex, jsonList.size());
				JSONUtils.saveJSONObjects(this, jsonFilename, nonStaleSubList);
				return endIndex;
			}
			else
				return 0;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Perform registration of the device with google GCM servers in this background service.
	 */
	private void generateGCMRegID()
	{
		String deviceRegID = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_GCM_REGISTRATION_ID);
		// If we don't have one yet or application was upgraded generate a new reg id 
		if (deviceRegID.isEmpty() || isAppVersionChanged())
		{
			ServerAPI serverAPI = new ServerAPI(this);
			JSONObject jsonRegID = serverAPI.registerWithGCM();
			try
			{
				if (ServerErrorCodes.hasError(jsonRegID))
				{
					Log.e(TAG, "Failed to register device with google servers : " + jsonRegID.getString(JSONKeys.RESPNOSE_KEY_ERROR));
					Log.e(TAG, "Will try again on next schedule");
				}
				else
				{
					Log.i(TAG, "Registered with GCM successfully");
					PreferencesUtils.putString(this, RoadwatchMainActivity.PROPERTY_GCM_REGISTRATION_ID, jsonRegID.getString(JSONKeys.USER_KEY_OWN_GCM_ID));
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean verifyUserLoginCredentials()
	{
		Log.i(TAG, "Starting user login credentials verification...");
		String loginToken = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);

		// Check if the user already logged in
		if (!loginToken.isEmpty())
		{
			long startTime = System.currentTimeMillis();
			JSONObject jsonUser = getUserFromServer(loginToken);

			if (!ServerErrorCodes.hasError(jsonUser))
			{
				Log.i(TAG, "Verified by server. took " + (System.currentTimeMillis() - startTime) + " ms");
				return true;
			}
			else
			{
				String errorMessage = ServerErrorCodes.getErrorMessage(this, jsonUser);

				// If bad login token, logout user by removing local preferences
				if (ServerErrorCodes.getErrorCode(jsonUser) == 2)
				{
					Log.w(TAG, "User was removed from server. Logging out user");
					PreferencesUtils.remove(this, RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
					PreferencesUtils.remove(this, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

					// Add open login activity intent to the notification
					Intent loginIntent = new Intent(this, LoginWizardActivity.class);
					// Show a notification about this! (allowing the user to login manually)
					Utils.showNotification(this, getString(R.string.notification_error_title), errorMessage, loginIntent, USER_CREDENTIALS_VERIFICATION_FAILED_NOTIFICATION_ID);
				}
				else
				{
					// On other errors we only log (user cannot do anything about it anyway)
					Log.e(TAG, "User credentials verification failed");
				}

				return false;
			}
		}
		else
		{
			Log.i(TAG, "User is not logged in - forcing server to start");
			long start = System.currentTimeMillis();
			// Invoking login on server for two reasons: (This login is never intended to succeed since we don't have a login token yet)
			// 1. Speed up access when user is registering (forcing server instance to start)
			// 2. Keep track at the server log for unregistered users
			getUserFromServer(Utils.getUserEmailAddress(this));
			Log.i(TAG, "Server response took : " + (System.currentTimeMillis() - start) + "ms");
		}

		return false;
	}

	private JSONObject getUserFromServer(String email)
	{
		ServerAPI serverAPI = new ServerAPI(this);
		return serverAPI.autoLoginUser(email);
	}

	private boolean isAppVersionChanged()
	{
		String registeredVersion = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_APP_VERSION);
		String currentVersion = Utils.getAppVersionNumber(this);

		if (!(registeredVersion.equals(currentVersion)))
		{
			Log.i(TAG, "App version change detected !");

			// Update local version
			PreferencesUtils.putString(this, RoadwatchMainActivity.PROPERTY_APP_VERSION, currentVersion);

			return true;
		}

		return false;
	}
}