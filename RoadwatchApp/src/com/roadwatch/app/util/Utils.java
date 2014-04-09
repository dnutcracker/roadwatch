package com.roadwatch.app.util;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.roadwatch.app.ApplicationData;
import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerErrorCodes;

public class Utils
{
	public static final String DEFAULT_CHARSET = "UTF-8";

	// Logging tag for the class
	private static final String TAG = Utils.class.getSimpleName();

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	private static ActivityManager manager;

	public static void beep(Context context)
	{
		try
		{
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(context, notification);
			r.play();
		}
		catch (Exception e)
		{
		}
	}

	public static boolean isGingerbread()
	{
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
	}

	/**
	 * Returns true if the version running was signed by a debug signer (like eclipse)
	 * 
	 * @param ctx
	 * @return true if the version running was signed by a debug signer (like eclipse)
	 */
	public static boolean isDebugAPK(Context ctx)
	{
		boolean debuggable = false;

		try
		{
			PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
			Signature signatures[] = pinfo.signatures;

			for (int i = 0; i < signatures.length; i++)
			{
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
				X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
				debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
				if (debuggable)
					break;
			}

		}
		catch (NameNotFoundException e)
		{
			//debuggable variable will remain false
		}
		catch (CertificateException e)
		{
			//debuggable variable will remain false
		}
		return debuggable;
	}

	//	public static boolean isConnectedViaADB()
	//	{		
	//	    AndroidDebugBridge debugBridge = AndroidDebugBridge.createBridge();//"D:\\android\\adt-bundle-windows-x86_64-20130717\\sdk\\platform-tools\\adb.exe", true);
	//	    if (debugBridge == null) 
	//	    {
	//	        Log.d(TAG, "Invalid ADB location.");
	//	        return false;
	//	    }
	//	    
	//	    return debugBridge.isConnected();	
	//	}

	//@SuppressWarnings("deprecation")
	/**
	 * When ADB is enabled device will connect to the local development server app engine
	 * @param context
	 * @return
	 */
	public static boolean isADBEnabled(Context context)
	{
		return false;//Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) != 0;
	}

	public static boolean isDebugMode(Context context)
	{
		if (context != null)
		{
			SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesUtils.ROADWATCH_APP_PREFERENCES, Context.MODE_PRIVATE);
			return sharedPreferences.getBoolean(ApplicationData.DEBUG_MODE_PROPERTY, false);
		}
		else
			return false;
	}

	public static boolean setDebugMode(Context context, boolean isDebug)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesUtils.ROADWATCH_APP_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(ApplicationData.DEBUG_MODE_PROPERTY, isDebug);
		return editor.commit();
	}

	public static boolean isEmulator()
	{
		boolean isEmulator = isAVD() || isGenymotion();
		return isEmulator;
	}

	public static boolean isAVD()
	{
		String model = Build.MODEL;
		String product = Build.PRODUCT;

		boolean isAVD = product != null && (product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_") || model.contains("Emulator"));
		return isAVD;
	}

	public static boolean isGenymotion()
	{
		String product = Build.PRODUCT;

		boolean isGenymotion = product != null && product.contains("vbox");
		return isGenymotion;
	}

	public static void closeSoftKeyboard(Activity activity, IBinder windowToken)
	{
		// Close the soft keyboard
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(windowToken, 0);
	}

	public static void openSoftKeyboard(Activity activity, IBinder windowToken)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.toggleSoftInputFromWindow(windowToken, InputMethodManager.SHOW_FORCED, 0);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static String getAppVersionNumber(Context context)
	{
		try
		{
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return String.valueOf(packageInfo.versionCode);
		}
		catch (NameNotFoundException e)
		{
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static String getAppVersionName(Context context)
	{
		try
		{
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return String.valueOf(packageInfo.versionName);
		}
		catch (NameNotFoundException e)
		{
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public static String getAndroidVersionAndDevice()
	{
		return Build.VERSION.RELEASE + "(API " + Build.VERSION.SDK_INT + ") on " + Build.MANUFACTURER + "(" + Build.MODEL + ")";
	}

	/**
	 * Saves user login information to local storage.
	 * 
	 * @param context
	 * @param jsonUser
	 */
	public static void storeLoginInformation(Context context, JSONObject jsonUser)
	{
		try
		{
			String loginToken = (String) jsonUser.get(JSONKeys.USER_KEY_UUID);
			String username = (String) jsonUser.get(JSONKeys.USER_KEY_USERNAME);
			String email = (String) jsonUser.get(JSONKeys.USER_KEY_EMAIL);
			String licensePlate = (String) jsonUser.get(JSONKeys.USER_KEY_LICENSE_PLATE);

			// Store values to allow auto login next time
			Map<String, String> props = new HashMap<String, String>();
			props.put(RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN, loginToken);
			props.put(RoadwatchMainActivity.PROPERTY_USER_NAME, username);
			props.put(RoadwatchMainActivity.PROPERTY_USER_EMAIL, email);
			props.put(RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE, licensePlate);
			props.put(RoadwatchMainActivity.PROPERTY_APP_VERSION, Utils.getAppVersionNumber(context));
			PreferencesUtils.putStrings(context, props);
		}
		catch (JSONException e) // Bad login information from server
		{
			// Notify about the error
			String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonUser);
			Log.e(TAG, errorMessage);
		}
	}

	public static void showNotification(Context context, String title, String shortMessage, int notificationID)
	{
		showNotification(context, title, shortMessage, null, 0, 0, null, notificationID);
	}

	public static void showNotification(Context context, String title, String shortMessage, Intent tapIntent, int notificationID)
	{
		showNotification(context, title, shortMessage, null, 0, 0, tapIntent, notificationID);
	}

	public static void showNotification(Context context, String title, String shortMessage, String longMessage, int notificationID)
	{
		showNotification(context, title, shortMessage, longMessage, 0, 0, null, notificationID);
	}

	public static void showNotification(Context context, String title, String shortMessage, String longMessage, long when, int notificationID)
	{
		showNotification(context, title, shortMessage, longMessage, when, 0, null, notificationID);
	}

	public static void showNotification(Context context, String title, String shortMessage, String longMessage, long when, int iconId, Intent tapIntent, int notificationID)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(iconId).setContentTitle(title).setContentText(shortMessage);

		builder.setSmallIcon(iconId != 0 ? iconId : R.drawable.rw_launcher);

		if (longMessage != null)
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(longMessage));

		builder.setPriority(NotificationCompat.PRIORITY_MAX);
		if (when > 0)
			builder.setWhen(when);

		// Tap 'turn off' to Stop listening service
		//		int dismissIcon = R.drawable.dismiss_32;
		//		builder.addAction(dismissIcon, "Stop listening", PendingIntent.getService(this, 0, makeServiceStopIntent(this), 0));

		// Set the intent when tapping the notification
		if (tapIntent != null)
		{
			builder.setContentIntent(PendingIntent.getActivity(context, 0, tapIntent, 0));
		}
		else
		// Gingerbread always expects an intent (or else nothing is shown)
		{
			builder.setContentIntent(PendingIntent.getBroadcast(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT));
		}

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(notificationID, notification);
	}

	public static void hideNotification(Context context, int notificationId)
	{
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationId);
	}

	private static ActivityManager getActivityManager(Context context)
	{
		if (manager == null)
			manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		return manager;
	}

	public static boolean isServiceRunning(Context context, Class<?> serviceClass)
	{
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
		{
			if (serviceClass.getName().equals(service.service.getClassName()))
				return true;
		}
		return false;
	}

	public static boolean isTopActivity(Context context, Class<?> activityClass)
	{
		return isTopActivity(context, activityClass.getName());
	}

	public static boolean isTopActivity(Context context, String activityClassName)
	{
		try
		{
			//			System.out.println("-------------------SERVICES----------------------");
			//			List<RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
			//			RunningServiceInfo runningService = runningServices.get(runningServices.size() - 1);
			//			System.out.println("Latest Service : " + runningService.service.getClassName());
			//			System.out.println("Latest Service Client Count : " + runningService.clientCount);
			//			System.out.println("Latest Service Client Label: " + runningService.clientLabel);

			return activityClassName.equals(getActivityManager(context).getRunningTasks(1).get(0).topActivity.getClassName());
			//			for (int i = 0; i < runningTasks.size(); i++)
			//			{
			//				if (activityClassName.equals(runningTasks.get(i).topActivity.getClassName()))
			//					return true;
			//			}
			//			return false;
		}
		catch (NullPointerException e)
		{
			/** 
			 * PENDING: Happens on Nexus with KitKat - waiting for a fix.
			 * https://code.google.com/p/android/issues/detail?id=62119#makechanges
			 */
			Log.e(TAG, "isTopActivity failed due to NPE", e);
			return false;
		}
	}

	public static boolean isAppRunning(Context context, String packageName, boolean inForeground)
	{
		List<RunningAppProcessInfo> runningAppProcesses = getActivityManager(context).getRunningAppProcesses();
		if (runningAppProcesses != null)
		{
			for (RunningAppProcessInfo process : runningAppProcesses)
			{
				if (packageName.equals(process.processName))
				{
					//Log.d(TAG, packageName + " importance is " + process.importance);
					//Log.d(TAG, packageName + " importanceReasonCode is " + process.importanceReasonCode);
					//Log.d(TAG, packageName + " importanceReasonComponent is " + process.importanceReasonComponent);
					//Log.d(TAG, packageName + " lru is " + process.lru);
					return inForeground ? process.importance <= RunningAppProcessInfo.IMPORTANCE_FOREGROUND : true;
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @param context
	 * @return true if the device is connected to at least one network (wi-fi or mobile)
	 */
	public static boolean isDeviceOnline(Context context)
	{
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public static void setAsBackroundView(View view, boolean asBackground)
	{
		float alpha = asBackground ? 0.2f : 1f;
		AlphaAnimation alphaAnim = new AlphaAnimation(alpha, alpha);
		alphaAnim.setDuration(0); // Make animation instant
		alphaAnim.setFillAfter(true); // Tell it to persist after the animation ends
		view.startAnimation(alphaAnim);
	}

	//	public static Bitmap putOverlay(Context context, Bitmap bitmap, Bitmap overlay, int x, int y)
	//	{
	//		Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
	//		Canvas canvas = new Canvas(bitmapCopy);
	//		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
	//		canvas.drawBitmap(overlay, x, y, paint);
	//		return bitmapCopy;
	//	}

	public static Bitmap addTextOverlay(Context context, Bitmap bitmap, String text, int x, int y)
	{
		final float density = context.getResources().getDisplayMetrics().density;
		Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(bitmapCopy);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.YELLOW);
		paint.setTextSize(12 * density);
		//paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);
		//paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		canvas.drawText(text, x * density, y * density, paint);

		return bitmapCopy;
	}

	public static String getUserEmailAddress(Context context)
	{
		AccountManager mAccountManager = AccountManager.get(context);

		Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		if (accounts.length > 0)
			return accounts[0].name;

		return null;
	}
}