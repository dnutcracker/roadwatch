package com.roadwatch.app.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferencesUtils
{
	// Logging tag
	private static final String TAG = PreferencesUtils.class.getSimpleName();

	public static final String ROADWATCH_APP_PREFERENCES = "roadwatch_preferences";
	public static final String ROADWATCH_APP_DEBUG_PREFERENCES = "roadwatch_debug_preferences";

	private static SharedPreferences getSharedPreferences(Context context)
	{
		return context.getSharedPreferences(Utils.isDebugMode(context) ? ROADWATCH_APP_DEBUG_PREFERENCES : ROADWATCH_APP_PREFERENCES, Context.MODE_PRIVATE);
	}

	public static String getString(Context context, String key)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		return sharedPref.getString(key, "");
	}

	public static boolean putString(Context context, String key, String value)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, value);
		return editor.commit();
	}

	public static boolean putStrings(Context context, Map<String, String> values)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		Set<Entry<String, String>> entrySet = values.entrySet();
		for (Entry<String, String> entry : entrySet)
			editor.putString(entry.getKey(), entry.getValue());

		return editor.commit();
	}

	public static boolean getBoolean(Context context, String key, boolean defaultValue)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		return sharedPref.getBoolean(key, defaultValue);
	}

	public static boolean putBoolean(Context context, String key, boolean value)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(key, value);
		return editor.commit();
	}
	
	public static long getLong(Context context, String key, long defaultValue)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		return sharedPref.getLong(key, defaultValue);
	}

	public static boolean putLong(Context context, String key, long value)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong(key, value);
		return editor.commit();
	}	
	
	public static boolean exists(Context context, String key)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		return !sharedPref.getString(key, "").isEmpty();
	}
	
	public static boolean remove(Context context, String key)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.remove(key);
		return editor.commit();
	}

	public static void printAll(Context context)
	{
		SharedPreferences sharedPref = getSharedPreferences(context);
		Log.d(TAG, "-- Stored Preferences --");
		Log.d(TAG, sharedPref.getAll().toString());
	}
}