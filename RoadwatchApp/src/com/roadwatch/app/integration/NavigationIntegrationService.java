package com.roadwatch.app.integration;

import org.json.JSONException;
import org.json.JSONObject;

import wei.mark.standout.StandOutWindow;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.roadwatch.app.R;
import com.roadwatch.app.floating.FloatingReportButton;
import com.roadwatch.app.location.Locator;
import com.roadwatch.app.location.LocatorListener;
import com.roadwatch.app.report.UnsentReportsActivity;
import com.roadwatch.app.settings.SettingsActivity;
import com.roadwatch.app.util.GoogleAnalyticsUtils;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.Utils;

/**
 * 
 * The service detects if a navigation app is running and performs the following:<BR>
 * 1. Displays the integrated floating report window.<BR>
 * 2. Sends the user's location to the server every 30 seconds.<BR>
 * 
 * <BR>
 * PENDING:
 * 1. Handle all the queued calls from the AlarmManager to our service after Waze was running for a long period of time.
 * 
 */
public class NavigationIntegrationService extends IntentService implements OnSharedPreferenceChangeListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocatorListener
{
	private static final String TAG = NavigationIntegrationService.class.getSimpleName();
	private static final String WAZE_ACTIVITY = "com.waze.MainActivity";
	private static final String WAZE_PACKAGE = "com.waze";

	private LocalBroadcastManager broadcastManager;
	/**
	 * Broadcast receiver that receives activity update intents
	 */
	private final BroadcastReceiver updateDrivingStateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(ActivityRecognitionIntentService.BROADCAST_DRIVING_ACTIVITY))
			{
				isStartedDriving = true;
				listenForDriving(false);
			}
		}
	};

	/** Time to wait between checks if navigation app is running or not */
	public static final int WAKEUP_INTERVAL = 60 * 1000; // 60 seconds
	/** Time to wait between checks if floating report button should be visible (starting after navigation app is running) */
	private static final int FOREGROUND_RESPONSE_TIME_IN_MILLIS = 500; // Half a second

	public static final int UNSENT_REPORTS_NOTIFICATION_ID = 2001;

	private boolean useNavIntegration;

	private ActivityRecognitionClient activityRecognitionClient;
	private PendingIntent activityRecognitionPendingIntent;
	private boolean listenForDriving;
	private boolean isStartedDriving = false;

	private Locator locationHelper;

	public NavigationIntegrationService()
	{
		super(NavigationIntegrationService.class.getSimpleName());
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		// We listen for preferences changes since the user might change the 'navigation integration' property while this service is running
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext().getApplicationContext());
		useNavIntegration = sharedPref.getBoolean(SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY, true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY.equals(key))
			useNavIntegration = sharedPreferences.getBoolean(SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY, true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		// Make sure we are restarted if needed
		return START_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		// Start a loop listening for known navigation apps
		if (useNavIntegration)
			detectNavigationAppActivity();
	}

	private void detectNavigationAppActivity()
	{
		//Log.d(TAG, "Detecting running navigation apps...");

		// We skip the 'isDriving' check in debug mode
		boolean onlyWhenDriving = !Utils.isDebugMode(this);
		boolean isAppRunning = false;
		boolean isMapVisible = false;

		boolean isMapVisibleNow = Utils.isTopActivity(this, WAZE_ACTIVITY);
		boolean isAppRunningNow = isMapVisibleNow || Utils.isAppRunning(this, WAZE_PACKAGE, false);

		while (isAppRunningNow && useNavIntegration)
		{
			//long start = System.currentTimeMillis();
			isMapVisibleNow = Utils.isTopActivity(this, WAZE_ACTIVITY);
			isAppRunningNow = isMapVisibleNow || Utils.isAppRunning(this, WAZE_PACKAGE, false);

			if (!onlyWhenDriving || isStartedDriving)
			{
				// See if map visibility state has changed
				if (isMapVisible != isMapVisibleNow)
				{
					// Some old trickery - muuaaha ha ha..!
					if (isMapVisible = isMapVisibleNow)
					{
						Log.d(TAG, "Waze map visible");
						StandOutWindow.show(this, FloatingReportButton.class, StandOutWindow.DEFAULT_ID);
					}
					else
					{
						Log.d(TAG, "Waze map hidden");
						StandOutWindow.closeAll(this, FloatingReportButton.class);
					}
				}
			}

			// See if app running state has changed
			if (isAppRunning != isAppRunningNow)
			{
				isAppRunning = isAppRunningNow;

				// Detect if app was closed (from the foreground or background using the waze's notification 'power off' button)
				if (isAppRunning = isAppRunningNow)
				{
					Log.i(TAG, "Waze started");

					// Start detecting if the user started driving
					if (onlyWhenDriving)
						listenForDriving(true);

					// Start tracking user location
					startLocationTracking();

					// PENDING: Sleep here to prevent icon from overlapping with waze ?
				}
				else
				{
					Log.i(TAG, "Waze closed");

					// Stop tracking user location
					stopLocationTracking();

					// Reminder for the user at the end of the way
					showUnsentReportNotification();

					// Stop listening just in case we haven't detected driving yet
					if (onlyWhenDriving && !isStartedDriving)
						listenForDriving(false);
				}
			}

			try
			{
				//System.out.println("Cycle time : " + (System.currentTimeMillis() - start) + " millis");
				Thread.sleep(FOREGROUND_RESPONSE_TIME_IN_MILLIS);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void showUnsentReportNotification()
	{
		int unsentReportsNumber = JSONUtils.loadJSONObjects(this, JSONUtils.UNSENT_REPORTS_FILENAME).size();
		if (unsentReportsNumber > 0)
		{
			String title = unsentReportsNumber == 1 ? getString(R.string.exit_dialog_message_single) : getString(R.string.exit_dialog_message_multiple, Integer.valueOf(unsentReportsNumber));
			String message = getString(R.string.tap_to_send);
			Intent intent = new Intent(this, UnsentReportsActivity.class);
			Utils.showNotification(this, title, message, intent, UNSENT_REPORTS_NOTIFICATION_ID);
		}
	}

	private void listenForDriving(boolean listenForDriving)
	{
		this.listenForDriving = listenForDriving;
		activityRecognitionClient = new ActivityRecognitionClient(this, this, this);
		activityRecognitionClient.connect();
	}

	/**
	 * Upon connection register us as broadcast receivers and request activity updates
	 */
	@Override
	public void onConnected(Bundle bundle)
	{
		try
		{
			Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
			activityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			if (listenForDriving)
			{
				Log.d(TAG, "Requesting activity manager updates and registering receiver");

				broadcastManager = LocalBroadcastManager.getInstance(this);
				broadcastManager.registerReceiver(updateDrivingStateReceiver, new IntentFilter(ActivityRecognitionIntentService.BROADCAST_DRIVING_ACTIVITY));
				activityRecognitionClient.requestActivityUpdates(1000, activityRecognitionPendingIntent);

				Log.d(TAG, "Listening for driving mode...");
			}
			else
			{
				Log.d(TAG, "Removing activity manager updates and unregistring receiver");

				broadcastManager.unregisterReceiver(updateDrivingStateReceiver);
				activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent);
			}

			// Since the preceding call is synchronous, we can disconnect the client here
			activityRecognitionClient.disconnect();
		}
		catch (IllegalStateException e)
		{
			// Happens rarely (probably due to disconnection of the activityRecognitionClient immediately after calling connect()
			GoogleAnalyticsUtils.sendException(this, e, false);
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@Override
	public void onDisconnected()
	{
		Log.w(TAG, "OnDisconnected()");
		activityRecognitionClient = null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0)
	{
		Log.e(TAG, "OnConnectionFailed() !");
	}

	private void startLocationTracking()
	{
		// Must create LocationHelper from within the service's main thread
		Handler mainHandler = new Handler(getMainLooper());
		Runnable run = new Runnable()
		{
			@Override
			public void run()
			{
				Log.d(TAG, "User location tracking - started");
				locationHelper = new Locator(NavigationIntegrationService.this);
				// Update only if moved more than 15 meters
				locationHelper.setMinimalDistanceForUpdate(Locator.MINIMAL_GPS_ACCURACY_REQUIRED_IN_METERS);
				locationHelper.connect(NavigationIntegrationService.this);
			}
		};
		mainHandler.post(run);
	}

	private void stopLocationTracking()
	{
		locationHelper.disconnect();
		Log.i(TAG, "User location tracking - stopped. Got " + JSONUtils.loadJSONObjects(this, JSONUtils.DRIVING_PATH_FILENAME).size() + " locations.");
	}

	@Override
	public void onLocationChanged(Location location)
	{
		Log.d(TAG, "Saving user driving location : " + location);

		JSONObject jsonLocation = new JSONObject();
		try
		{
			jsonLocation.put(JSONKeys.USER_CURRENT_LOCATION_KEY_LATITUDE, location.getLatitude());
			jsonLocation.put(JSONKeys.USER_CURRENT_LOCATION_KEY_LONGITUDE, location.getLongitude());
			jsonLocation.put(JSONKeys.USER_CURRENT_LOCATION_KEY_TIME, location.getTime());

			JSONUtils.saveJSONObject(this, JSONUtils.DRIVING_PATH_FILENAME, jsonLocation, true);
		}
		catch (JSONException e)
		{
			// Can't really happen
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

		// Just in case we mighty got destroyed before we had the chance to call listenForDriving(false)
		if (broadcastManager != null)
			broadcastManager.unregisterReceiver(updateDrivingStateReceiver);

		//Log.d(TAG, "Destroyed");
	}
}