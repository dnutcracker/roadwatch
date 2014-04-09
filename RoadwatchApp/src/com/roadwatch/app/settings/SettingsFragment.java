package com.roadwatch.app.settings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.roadwatch.app.R;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	private static final String SETTINGS_KEY = "settings";
	private static final String DISPLAY_SETTINGS_VALUE = "display";
	private static final String NAV_INTEGRATION_SETTINGS_VALUE = "nav_integration";
	private static final String VOICE_RECOGNITION_SETTINGS_VALUE = "voice_recognition";
	private static final String CAR_SHARING_SETTINGS_VALUE = "car_sharing";
	private static final String ABOUT_VALUE = "about";

	private int debugClickCount;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String settings = getArguments().getString(SETTINGS_KEY);

		if (DISPLAY_SETTINGS_VALUE.equals(settings))
		{
			addPreferencesFromResource(R.xml.preferences_display);
		}
		else if (NAV_INTEGRATION_SETTINGS_VALUE.equals(settings))
		{
			addPreferencesFromResource(R.xml.preferences_nav_integration);
		}
		else if (VOICE_RECOGNITION_SETTINGS_VALUE.equals(settings))
		{
			addPreferencesFromResource(R.xml.preferences_voice_recognition);
		}
		else if (CAR_SHARING_SETTINGS_VALUE.equals(settings))
		{
			addPreferencesFromResource(R.xml.preferences_car_sharing);
		}
		else if (ABOUT_VALUE.equals(settings))
		{
			addPreferencesFromResource(R.xml.preferences_about);
		}

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
			SettingsUtils.initSummary(getActivity(), getPreferenceScreen().getPreference(i));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Auto fill selected value in list preferences as summary. 
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		SettingsUtils.updatePrefSummary(getActivity(), findPreference(key));
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		// Allow to enable/disable to debug mode
		if (preference.getKey().equals(SettingsActivity.ABOUT_VERSION_PREF_KEY))
		{
			debugClickCount++;
			if (debugClickCount == 20)
				SettingsUtils.toggleDebugMode(getActivity());
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}