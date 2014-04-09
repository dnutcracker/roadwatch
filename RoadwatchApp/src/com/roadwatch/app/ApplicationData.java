package com.roadwatch.app;

import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.google.analytics.tracking.android.Tracker;
import com.roadwatch.app.settings.SettingsActivity;
import com.roadwatch.app.util.ReportUtils;
import com.roadwatch.app.util.Utils;

@ReportsCrashes(formKey = "", // will not be used (but needed)
formUri = "https://dev-road-watch.appspot.com/api/logClientException", mode = ReportingInteractionMode.TOAST, reportType = org.acra.sender.HttpSender.Type.JSON, resToastText = R.string.crash_toast_text, customReportContent =
{ ReportField.USER_EMAIL, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE,
		ReportField.LOGCAT })
public class ApplicationData extends Application implements OnSharedPreferenceChangeListener
{
	private static final String TAG = ApplicationData.class.getSimpleName();

	private static GoogleAnalytics googlaAnalytics;
	private static Tracker tracker;

	// RoadWatch property ID
	private static final String GA_PROPERTY_ID = "UA-45589728-1";

	// GA Logger verbosity
	private static final LogLevel GA_LOG_VERBOSITY = LogLevel.WARNING;

	public static final String DEBUG_MODE_PROPERTY = "debug_mode_property";

	@Override
	public void onCreate()
	{
		// Init ACRA engine
		ACRA.init(this);

		// Init GA
		initializeGoogleAnalytics();

		// Init app preference (will not overwrite if already init)
		PreferenceManager.setDefaultValues(this, R.xml.preferences_display, false);
		PreferenceManager.setDefaultValues(this, R.xml.preferences_voice_recognition, false);
		PreferenceManager.setDefaultValues(this, R.xml.preferences_nav_integration, false);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		setDisplayLanguage();
	}

	/**
	 * 
	 */
	private void initializeGoogleAnalytics()
	{
		EasyTracker.getInstance(this);

		// Initialize a tracker using a Google Analytics property ID
		googlaAnalytics = GoogleAnalytics.getInstance(this);

		tracker = googlaAnalytics.getTracker(GA_PROPERTY_ID);
		googlaAnalytics.setDefaultTracker(tracker);

		// Set dryRun flag
		if (Utils.isDebugAPK(this))
			googlaAnalytics.setDryRun(true);

		// Set Logger verbosity.
		googlaAnalytics.getLogger().setLogLevel(GA_LOG_VERBOSITY);

		// Report any uncaught exceptions to Google Analytics
		//UncaughtExceptionHandler gaHandler = new ExceptionReporter(tracker, GAServiceManager.getInstance(), Thread.getDefaultUncaughtExceptionHandler(), getApplicationContext()); // Current default uncaught exception handler.

		// Make gaHandler the new default uncaught exception handler
		//Thread.setDefaultUncaughtExceptionHandler(gaHandler);

		Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (uncaughtExceptionHandler instanceof ExceptionReporter)
		{
			ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
			exceptionReporter.setExceptionParser(new StandardExceptionParser(this, null)
			{
				@Override
				public String getDescription(String threadName, Throwable e)
				{
					return super.getDescription(threadName, e) + " : " + e.getMessage();
				}
			});
		}

		// Send a single hit with session control to start the new session
		tracker.send(MapBuilder.createEvent("UX", "appstart", null, null).set(Fields.SESSION_CONTROL, "start").build());
	}

	private void setDisplayLanguage()
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext().getApplicationContext());
		String displayLanguagePref = sharedPref.getString(SettingsActivity.DISPLAY_SETTINGS_LANGUAGE_PREF_KEY, "");

		Configuration configuration = getApplicationContext().getResources().getConfiguration();
		configuration.locale = new Locale(displayLanguagePref);
		Log.d(TAG, "Set locale by displayLanguagePref '" + displayLanguagePref + "' : " + configuration.locale);
		getApplicationContext().getResources().updateConfiguration(configuration, getApplicationContext().getResources().getDisplayMetrics());

		forceDefaultLocale();
	}

	/**
	 * Some libraries(like google maps) and utilities(like DateUtils) are relying internally on Locale.getDefault() for output text.
	 * We have no control over the output locale unless we invoke Locale.setDefault(appLocale).
	 */
	private void forceDefaultLocale()
	{
		Locale appLocale = getResources().getConfiguration().locale;
		Locale defaultLocale = Locale.getDefault();

		// If device default locale langauge is different than the app locale language we need to force the default locale
		if (!appLocale.getLanguage().equals(defaultLocale.getLanguage()))
		{
			Log.i(TAG, "Forcing default local to be the same as app local (" + appLocale + ")");
			Locale.setDefault(getResources().getConfiguration().locale);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		setDisplayLanguage();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (SettingsActivity.DISPLAY_SETTINGS_LANGUAGE_PREF_KEY.equals(key))
		{
			setDisplayLanguage();

			// Update voice recognition language if necessary
			boolean useDisplayLanguageForVoiceRecognition = sharedPreferences.getBoolean(SettingsActivity.VOICE_RECOGNITION_SETTINGS_USE_DISPLAY_LANGUAGE_PREF_KEY, true);
			if (useDisplayLanguageForVoiceRecognition)
			{
				String displayLanguagePref = sharedPreferences.getString(SettingsActivity.DISPLAY_SETTINGS_LANGUAGE_PREF_KEY, "");
				Editor editor = sharedPreferences.edit();
				editor.putString(SettingsActivity.VOICE_RECOGNITION_SETTINGS_LANGUAGE_PREF_KEY, displayLanguagePref);
				editor.commit();
			}

			// Re-init the map of report descriptions with updated language
			ReportUtils.initResources(this);
		}
		else if (SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY.equals(key))
		{
			boolean useNavIntegration = sharedPreferences.getBoolean(SettingsActivity.NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY, true);
			RebootBroadcastReceiver.setNavigationIntegrationServiceEnabled(this, useNavIntegration);
		}
	}

	/*
	 * Returns the Google Analytics tracker.
	 */
	public static Tracker getTracker()
	{
		return tracker;
	}

	/*
	 * Returns the Google Analytics instance.
	 */
	public static GoogleAnalytics getGoogleAnalytics()
	{
		return googlaAnalytics;
	}
}