package com.roadwatch.app;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.Fields;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.roadwatch.app.location.Locator;
import com.roadwatch.app.settings.SettingsActivity;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;

/**
 *  
 * @author Nati created at : 21 בספט 2013 03:25:01
 */
public class SplashScreenActivity extends Activity
{
	// Logging tag
	private static final String TAG = SplashScreenActivity.class.getSimpleName();

	private static final String FIRST_RUN_PROPERTY = "firstRun";
	private static final int SPLASH_MINIMAL_DISPLAY_TIME = 2000;
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private static final Locale HEBREW_LOCALE = new Locale("iw", "IL");

	public static final String PREF_APP_LANGUAGE = "app_language_preference";

	private ProgressBar initProgressBar;
	private TextView initTextView;

	private Locator locationHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ApplicationData.getTracker().set(Fields.SCREEN_NAME, TAG);

		setContentView(R.layout.splash_screen);

		initProgressBar = (ProgressBar) findViewById(R.id.init_progress);
		initTextView = (TextView) findViewById(R.id.init_text);

		if (!checkPlayServices())
		{
			String errorMessage = "No valid Google Play Services APK found, Exiting RoadWatchApp";
			Log.e(TAG, errorMessage);
			Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
		}
		else
		{
			new Initialize(this).execute();
		}
	}

	//	/**
	//	 * Since the gradient background when defined in XML does auto scale to density - we do it programatically. 
	//	 */
	//	private void addScaledGradientBackground()
	//	{
	//		RelativeLayout layout = (RelativeLayout) findViewById(R.id.splash_screen_layout);
	//
	//		GradientDrawable g = new GradientDrawable(Orientation.TL_BR, new int[]
	//		{ getResources().getColor(R.color.splash_bg_blue), getResources().getColor(R.color.splash_bg_blue2) });
	//		g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
	//		g.setGradientRadius(270 * getResources().getDisplayMetrics().density);
	//		g.setGradientCenter(0.5f, 0.5f);
	//		layout.setBackgroundDrawable(g);
	//	}

	@SuppressWarnings("unused")
	private Locale getLocaleAccordingToLocation(final Context context)
	{
		locationHelper = new Locator(this, false);
		Location location = locationHelper.getLastKnownLocation();
		Locale localLocale = Locale.getDefault();
		if (location != null)
		{
			Address address = ReportUtils.resolveLocationToAddress(context, location.getLatitude(), location.getLongitude());
			if (address != null)
			{
				String countryCode = address.getCountryCode();
				if (countryCode != null)
				{
					localLocale = getLocaleByCountryCode(countryCode);
					Log.i(TAG, "Initial locale for country(" + countryCode + ") : " + localLocale);
				}
				else
					Log.w(TAG, "Failed to get user country on first run. Using default locale");
			}
			else
				Log.w(TAG, "Failed to resolve user location on first run. Using default locale");
		}
		else
			Log.w(TAG, "Failed to get user location on first run. Using default locale");

		Log.i(TAG, "Saving locale : " + localLocale);

		return localLocale;
	}

	private Locale getLocaleByCountryCode(String countryCode)
	{
		// return some predefined locales per country code
		if (countryCode.equals(HEBREW_LOCALE.getCountry()))
			return HEBREW_LOCALE;

		// Search through available locales (although we don't support all languages, this is used by google in reverse geocoding)
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (Locale locale : availableLocales)
		{
			if (locale.getCountry().equals(countryCode))
				return locale;
		}

		Log.w(TAG, "Failed to find locale for countryCode " + countryCode + ". Using default locale");
		return Locale.getDefault();
	}

	private class Initialize extends AsyncTask<Void, Void, Void>
	{
		private Context context;

		public Initialize(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			if (PreferencesUtils.getBoolean(context, FIRST_RUN_PROPERTY, true))
			{
				initProgressBar.setVisibility(View.VISIBLE);
				initTextView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			long startTime = System.currentTimeMillis();

			// Check if we need to do first time init
			// PENDING: Consider ignoring debug mode when querying this property
			if (PreferencesUtils.getBoolean(context, FIRST_RUN_PROPERTY, true))
			{
				// On first run, schedule our bg services (after reboot, they will be started automatically)
				RebootBroadcastReceiver.setNavigationIntegrationServiceEnabled(context, true);
				RebootBroadcastReceiver.scheduleVerifyLoginService(context);

				// Set initial locale by users current location
				//Locale defaultLocale = getLocaleAccordingToLocation(context);
				// PENDING: For now we'll use Hebrew locale as the default locale 
				Locale defaultLocale = HEBREW_LOCALE;

				// Save locale in preferences
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
				Editor editor = sharedPref.edit();
				editor.putString(SettingsActivity.DISPLAY_SETTINGS_LANGUAGE_PREF_KEY, defaultLocale.getLanguage());
				editor.commit();

				// Update property
				PreferencesUtils.putBoolean(context, FIRST_RUN_PROPERTY, false);
			}

			ReportUtils.initResources(context);

			long elapsedTime = System.currentTimeMillis() - startTime;

			Log.i(TAG, "Initialization took : " + elapsedTime + "ms");

			try
			{
				if (elapsedTime < SPLASH_MINIMAL_DISPLAY_TIME)
					Thread.sleep(SPLASH_MINIMAL_DISPLAY_TIME - elapsedTime);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void param)
		{
			Intent mainActivityIntent = new Intent(context, RoadwatchMainActivity.class);

			startActivity(mainActivityIntent);

			// close this activity
			finish();
		}
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices()
	{
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS)
		{
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
			{
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			}
			else
			{
				String errorMessage = "This device is not supported, exiting RoadWatch";
				Log.e(TAG, errorMessage);
				Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
				finish();
			}
			return false;
		}
		return true;
	}

	@Override
	public void onBackPressed()
	{
		// Prevent pressing back while in splash screen!
	}
}