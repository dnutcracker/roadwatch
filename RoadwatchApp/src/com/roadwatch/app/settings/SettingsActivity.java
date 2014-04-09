package com.roadwatch.app.settings;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.roadwatch.app.R;
import com.roadwatch.app.util.Utils;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private static final String DISPLAY_SETTINGS_ACTION = "com.roadwatch.app.settings.DISPLAY_SETTINGS";
	public static final String DISPLAY_SETTINGS_LANGUAGE_PREF_KEY = "pref_key_display_language";

	private static final String NAV_INTEGRATION_ACTION = "com.roadwatch.app.settings.NAV_INTEGRATION_SETTINGS";
	public static final String NAV_INTEGRATION_USE_NAV_INTEGRATION_PREF_KEY = "pref_key_use_nav_integration";
	public static final String NAV_INTEGRATION_USE_NAV_INTEGRATION_ONLY_WHEN_DRIVING_PREF_KEY = "pref_key_use_nav_integration_only_when_driving";

	private static final String VOICE_RECOGNITION_ACTION = "com.roadwatch.app.settings.VOICE_RECOGNITION_SETTINGS";
	public static final String VOICE_RECOGNITION_SETTINGS_USE_DISPLAY_LANGUAGE_PREF_KEY = "pref_key_voice_recognition_use_display_language";
	public static final String VOICE_RECOGNITION_SETTINGS_LANGUAGE_PREF_KEY = "pref_key_voice_recognition_language";

	private static final String CAR_SHARING_ACTION = "com.roadwatch.app.settings.CAR_SHARING_SETTINGS";
	public static final String CAR_SHARING_ENABLE_CAR_SHARING_PREF_KEY = "pref_key_enable_car_sharing";

	private static final String ABOUT_ACTION = "com.roadwatch.app.settings.ABOUT_SETTINGS";
	public static final String ABOUT_VERSION_PREF_KEY = "pref_key_about_version";

	private int debugClickCount;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String action = getIntent().getAction();
		if (action != null && action.equals(DISPLAY_SETTINGS_ACTION))
		{
			addPreferencesFromResource(R.xml.preferences_display);
		}
		else if (action != null && action.equals(NAV_INTEGRATION_ACTION))
		{
			addPreferencesFromResource(R.xml.preferences_nav_integration);
		}
		else if (action != null && action.equals(VOICE_RECOGNITION_ACTION))
		{
			addPreferencesFromResource(R.xml.preferences_voice_recognition);
		}
		else if (action != null && action.equals(CAR_SHARING_ACTION))
		{
			addPreferencesFromResource(R.xml.preferences_car_sharing);
		}
		else if (action != null && action.equals(ABOUT_ACTION))
		{
			addPreferencesFromResource(R.xml.preferences_about);
		}
		else if (Utils.isGingerbread())
		{
			// Load the legacy preferences headers
			addPreferencesFromResource(R.xml.preference_headers_legacy);
		}

		if (Utils.isGingerbread())
		{
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
				SettingsUtils.initSummary(this, getPreferenceScreen().getPreference(i));
		}
	}

	// Called only on Honeycomb and later
	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target)
	{
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	// Needed for KitKat or later
	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		return true;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (Utils.isGingerbread())
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (Utils.isGingerbread())
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	@Deprecated
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		// Allow to enable/disable debug mode
		if (ABOUT_VERSION_PREF_KEY.equals(preference.getKey()))
		{
			debugClickCount++;
			if (debugClickCount == 20)
				SettingsUtils.toggleDebugMode(this);
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	/**
	 * Auto fill selected value in list preferences as summary. 
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		SettingsUtils.updatePrefSummary(this, findPreference(key));
	}
}