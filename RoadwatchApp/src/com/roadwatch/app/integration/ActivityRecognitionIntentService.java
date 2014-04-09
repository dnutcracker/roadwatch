package com.roadwatch.app.integration;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Service that receives ActivityRecognition updates. It receives updates
 * in the background, even if the main Activity is not visible.
 */
public class ActivityRecognitionIntentService extends IntentService
{
	private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();

	public static final String BROADCAST_DRIVING_ACTIVITY = "com.roadwatch.app.integration.broadcast_driving_activity";

	public ActivityRecognitionIntentService()
	{
		super("ActivityRecognitionIntentService");
	}

	/**
	 * Called when a new activity detection update is available.
	 */
	@Override
	protected void onHandleIntent(Intent intent)
	{
		// If the intent contains an update
		if (ActivityRecognitionResult.hasResult(intent))
		{
			// Get the update
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

			// Get the most probable activity from the list of activities in the update
			DetectedActivity mostProbableActivity = result.getMostProbableActivity();

			// Get the confidence percentage for the most probable activity
			int confidence = mostProbableActivity.getConfidence();

			// Get the type of activity
			int activityType = mostProbableActivity.getType();

			String activityName = getNameFromType(activityType);

			//Log.d(TAG, "Detected activity : " + activityName +"("+confidence+"%)");

			if (activityType == DetectedActivity.IN_VEHICLE && confidence > 50)
			{
				// Broadcast activity type
				Intent localIntent = new Intent(BROADCAST_DRIVING_ACTIVITY);
				LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

				Log.d(TAG, "User is Driving ! : " + activityName + "(" + confidence + "%)");
			}
		}
	}

	/**
	 * Map detected activity types to strings
	 *
	 * @param activityType The detected activity type
	 * @return A user-readable name for the type
	 */
	private String getNameFromType(int activityType)
	{
		switch (activityType)
		{
		case DetectedActivity.IN_VEHICLE:
			return "in_vehicle";
		case DetectedActivity.ON_BICYCLE:
			return "on_bicycle";
		case DetectedActivity.ON_FOOT:
			return "on_foot";
		case DetectedActivity.STILL:
			return "still";
		case DetectedActivity.UNKNOWN:
			return "unknown";
		case DetectedActivity.TILTING:
			return "tilting";
		}
		return "unknown";
	}
}
