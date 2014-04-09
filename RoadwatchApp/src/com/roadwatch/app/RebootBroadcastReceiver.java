package com.roadwatch.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.roadwatch.app.integration.NavigationIntegrationService;
import com.roadwatch.app.login.VerifyLoginService;
import com.roadwatch.app.settings.SettingsActivity;

/**
 * Schedules RoadWatch background services using the <code>AlarmManager</code> service.
 * 
 * @author Nati created at 23/09/13 02:28
 */
public class RebootBroadcastReceiver extends BroadcastReceiver
{
	private static final String TAG = RebootBroadcastReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i(TAG, "reboot detected");

		scheduleVerifyLoginService(context);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean useNavIntegration = sharedPref.getBoolean(SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY, true);
		if (useNavIntegration)
			setNavigationIntegrationServiceEnabled(context, true);
	}

	public static void scheduleVerifyLoginService(Context context)
	{
		// Get the alarm manager service
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// Create the intent and pending intent for our service
		Intent startServiceIntent = new Intent(context, VerifyLoginService.class);
		PendingIntent alarmIntent = PendingIntent.getService(context, 0, startServiceIntent, 0);

		// Start the alarm manager
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, AlarmManager.INTERVAL_DAY, alarmIntent);
		Log.i(TAG, "Scheduling verify login service with AlarmManager");
	}

	public static void setNavigationIntegrationServiceEnabled(Context context, boolean enable)
	{
		// Get the alarm manager service
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// Create the intent and pending intent for our service
		Intent startServiceIntent = new Intent(context, NavigationIntegrationService.class);
		PendingIntent alarmIntent = PendingIntent.getService(context, 0, startServiceIntent, 0);

		// Start the alarm manager (We don't call setInexcatRepeatings() since it only benefits if it is invoked with the AlarmManager's intervals constants)
		if (enable)
		{
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 0, NavigationIntegrationService.WAKEUP_INTERVAL, alarmIntent);
			Log.i(TAG, "Scheduling navigation integration service with AlarmManager");
		}
		else
		{
			alarmManager.cancel(alarmIntent);
			Log.i(TAG, "Cancelling navigation integration service schedule with AlarmManager");
		}
	}
}