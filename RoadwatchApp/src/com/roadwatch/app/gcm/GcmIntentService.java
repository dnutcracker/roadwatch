package com.roadwatch.app.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.roadwatch.app.util.Utils;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService
{
	// Logging tag for the class
	private static final String TAG = GcmIntentService.class.getSimpleName();

	private static final int GCM_GENERAL_NOTIFICATION_ID = 1;

	private GCMHandler gcmHandler;

	public GcmIntentService()
	{
		super("GcmIntentService");
		gcmHandler = new GCMHandler(this);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty())
		{ // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM will be
			 * extended in the future with new message types, just ignore any message types you're
			 * not interested in, or that you don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType))
			{
				Log.d(TAG, "Got MESSAGE_TYPE_SEND_ERROR from server");
				Utils.showNotification(this, "GCM Send error", extras.toString(), extras.toString(), GCM_GENERAL_NOTIFICATION_ID);
			}
			else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType))
			{
				Log.d(TAG, "Got MESSAGE_TYPE_DELETED from server");
				Utils.showNotification(this, "GCM Deleted messages on server", extras.toString(), extras.toString(), GCM_GENERAL_NOTIFICATION_ID);
			}
			else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType))
			{
				Log.d(TAG, "Got GCM from server : " + extras.toString());
				gcmHandler.handleMessage(extras);
			}
		}

		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

}