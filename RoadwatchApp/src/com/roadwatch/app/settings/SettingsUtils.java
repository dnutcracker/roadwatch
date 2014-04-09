package com.roadwatch.app.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.widget.Toast;

import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.Utils;

public class SettingsUtils
{
	public static void initSummary(Context context, Preference p)
	{
		if (p instanceof PreferenceCategory)
		{
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++)
			{
				initSummary(context, pCat.getPreference(i));
			}
		}
		else
		{
			updatePrefSummary(context, p);
		}
	}

	public static void updatePrefSummary(Context context, Preference p)
	{
		if (p instanceof ListPreference)
		{
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference)
		{
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
		if (p instanceof Preference)
		{
			if (SettingsActivity.ABOUT_VERSION_PREF_KEY.equals(p.getKey()))
				p.setSummary(Utils.getAppVersionName(context));
		}
	}

	public static void toggleDebugMode(Context context)
	{
		boolean debugMode = Utils.isDebugMode(context);

		Toast.makeText(context, "Debug Mode " + (debugMode ? "Disabled" : "Enabled !"), Toast.LENGTH_LONG).show();

		// Update debug mode state
		Utils.setDebugMode(context, !debugMode);

		// Clear the user data cache (force reload from server)
		JSONUtils.deleteJSONObjects(context, JSONUtils.LOGGED_IN_USER_DATA);
	}
}