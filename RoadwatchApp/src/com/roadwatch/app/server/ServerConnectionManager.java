package com.roadwatch.app.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.roadwatch.app.util.GoogleAnalyticsUtils;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.Utils;

/**
 * A singleton serving a connection to the server
 * 
 * PENDING:
 * 1. Use Velocity library.
 * 
 * @author Nati
 */
public enum ServerConnectionManager
{
	INSTANCE;

	// Debugging tag for the activity
	private static final String ACT_TAG = ServerConnectionManager.class.getSimpleName();

	// HTTP timeout configuration (we need at least 15 seconds, since spin-up of an instance of the app engine server might take for ~12 seconds)
	private static final int CONNECTION_TIMOUT_IN_SECONDS = 15;
	// Max attempts on a server call before giving up
	private static final int MAX_ATTEMPTS = 5;

	private static final int INITIAL_BACKOFF_IN_MS = 1000;

	/**
	 * This is the project number we got from the API Console, as described in "Getting Started."
	 */
	// Project dev-road-watch ID : 386534561197
	// Project road-watch ID : 758739695885
	public static final String SENDER_ID = "758739695885";
	// road-watch API key : AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs
	// dev-road-watch API key : AIzaSyAwOkz5V28M3YLNOIDsFPkJRz6cMadA9rc
	public static final String DEBUG_API_KEY = "AIzaSyAwOkz5V28M3YLNOIDsFPkJRz6cMadA9rc";
	public static final String PROD_API_KEY = "AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs";

	private static final int SERVER_VERSION = 14;

	// Google app engine debug URL
	private static final String DEBUG_APP_ENGINE_URL = "https://" + SERVER_VERSION + "-dot-dev-road-watch.appspot.com/";
	// Google app engine production URL
	private static final String PROD_APP_ENGINE_URL = "https://" + SERVER_VERSION + "-dot-road-watch.appspot.com/";

	private static boolean initialized;

	// Initializes on the first call to the server
	private void init(Context context)
	{
		if (!initialized)
		{
			enableHttpResponseCache(context);
			initialized = true;
		}
	}

	/**
	 * Returns the URL of the RoadWatch app engine.
	 * If we're in debug mode all non user-related operations are diverged to the debug server.
	 * @param context
	 * @param servletName 
	 * @return the URL of the RoadWatch app engine
	 */
	private String getAppEngineURLString(Context context, String servletName)
	{
		String localAppEngineFromEmulatorURL = "http://" + (Utils.isAVD() ? "10.0.2.2" : "192.168.1.100") + ":8888/";
		String remoteAppEngineURL = Utils.isDebugMode(context) ? DEBUG_APP_ENGINE_URL : PROD_APP_ENGINE_URL;

		// All user verifications we perform against the production server 
		String googleServerURL = Utils.isEmulator() ? localAppEngineFromEmulatorURL : remoteAppEngineURL;

		return googleServerURL + servletName;
	}

	private void enableHttpResponseCache(Context context)
	{
		try
		{
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			File httpCacheDir = new File(context.getCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class).invoke(null, httpCacheDir, Long.valueOf(httpCacheSize));
		}
		catch (Exception httpResponseCacheNotAvailable)
		{
		}
	}

	/**
	 * Get server response using exponential back-off.
	 * We also use this method to register with Google Cloud Messaging service. (GCM)
	 * 
	 * @param servletName The servlet we want to communicate with
	 * @param jsonObjectToSend
	 * @return
	 */
	public List<JSONObject> sendServerAndGetResponse(Context context, String servletName, JSONObject jsonObjectToSend)
	{
		init(context);

		String errorMessage = "";
		int errorCode = 199;

		long backoff = INITIAL_BACKOFF_IN_MS;
		for (int i = 1; i <= MAX_ATTEMPTS; i++)
		{
			try
			{
				String appEngineURL = getAppEngineURLString(context, servletName);
				return sendAndReceive(context, appEngineURL, jsonObjectToSend);
			}
			catch (IOException e)
			{
				// Google Analytics
				GoogleAnalyticsUtils.sendException(context, e, false);

				Log.e(ACT_TAG, "Failed to connect to server (attempt " + i + " of " + MAX_ATTEMPTS + ")");
				Log.e(ACT_TAG, e.toString());

				//				if(i==1 && e instanceof InterruptedIOException && isUserWaiting(context))
				//				{
				//					Toast.makeText(context, "Server is initializing, please wait...",  Toast.LENGTH_SHORT).show();
				//				}

				if (i == MAX_ATTEMPTS)
				{
					GoogleAnalyticsUtils.sendException(context, e, true);
					// We're here in-case of ConnectTimeoutException or SocketTimeoutException
					errorMessage = e.getMessage();
					errorCode = 100;

					List<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
					jsonObjectList.add(JSONUtils.createJSONObjectWithError(errorCode, errorMessage));
					return jsonObjectList;
				}

				try
				{
					Log.d(ACT_TAG, "Sleeping for " + backoff + "ms before retry");
					Thread.sleep(backoff);
				}
				catch (InterruptedException e1)
				{
					// Activity finished before we complete - exit.
					Log.d(ACT_TAG, "Thread interrupted: aborting remaining retries!");
					Thread.currentThread().interrupt();
					return null;
				}
				// Increase back-off exponentially
				backoff *= 2;
			}
			catch (AppEngineException e)
			{
				// Google Analytics
				GoogleAnalyticsUtils.sendException(context, e, false);

				Log.e(ACT_TAG, e.toString());
				errorMessage = e.toString();

				// Error from the app server does not require retries
				break;
			}
		}

		List<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
		jsonObjectList.add(JSONUtils.createJSONObjectWithError(errorCode, errorMessage));
		return jsonObjectList;
	}

	private List<JSONObject> sendAndReceive(Context context, String serverUrl, JSONObject jsonObjectToSend) throws IOException
	{
		HttpURLConnection urlConnection = null;

		try
		{
			byte[] jsonBytes = jsonObjectToSend.toString().getBytes("UTF8");

			urlConnection = (HttpURLConnection) new URL(serverUrl).openConnection();
			urlConnection.setReadTimeout(1000 * CONNECTION_TIMOUT_IN_SECONDS);
			urlConnection.setConnectTimeout(1000 * CONNECTION_TIMOUT_IN_SECONDS);
			urlConnection.setDoOutput(true); // Automatically sets to request method 'POST' 		
			urlConnection.setFixedLengthStreamingMode(jsonBytes.length);
			urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			urlConnection.addRequestProperty("ClientAuth", Utils.isDebugMode(context) ? DEBUG_API_KEY : PROD_API_KEY);
			// PENDING : Might resolve the occasional EOFException we see
			if (Build.VERSION.SDK_INT > 13)
				urlConnection.setRequestProperty("Connection", "close");

			BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
			out.write(jsonBytes);
			out.flush();
			out.close();

			// Make sure we got a normal response from the server before attempting to read it.
			int responseStatus = urlConnection.getResponseCode();
			String responseMessage = urlConnection.getResponseMessage();
			if (responseStatus != 200)
			{
				if (responseStatus == 503)
					throw new IOException(responseMessage + " (HTTP " + responseStatus + ")");
				else
					throw new AppEngineException(responseMessage + " (HTTP " + responseStatus + ")");
			}

			return readInputStream(urlConnection.getInputStream());
		}
		finally
		{
			urlConnection.disconnect();
		}
	}

	private List<JSONObject> readInputStream(InputStream is)
	{
		List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, Utils.DEFAULT_CHARSET));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
				sb.append(line + "\n");

			is.close();

			return JSONUtils.convertStringToJSONArray(sb.toString());
		}
		catch (IOException e)
		{
			String errorMessage = "Failed to parse JSON object from server";
			Log.e(errorMessage, e.toString());
			jsonObjects.add(JSONUtils.createJSONObjectWithError(101, errorMessage + ": " + e.getMessage()));
			return jsonObjects;
		}
	}

	/**
	 * Contact GCM and wrap the result in a JSONObject in order to return it through our server API method.
	 * 
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public List<JSONObject> registerWithGCM(Context context)
	{
		String errorMessage = "";
		long backoff = INITIAL_BACKOFF_IN_MS;
		for (int i = 1; i <= MAX_ATTEMPTS; i++)
		{
			try
			{
				// Register device with google cloud messaging server.
				String GCMRegId = GoogleCloudMessaging.getInstance(context).register(SENDER_ID);

				// Return the result as a JSONObject
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(JSONKeys.USER_KEY_OWN_GCM_ID, GCMRegId);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				List<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
				jsonObjectList.add(jsonObject);

				return jsonObjectList;
			}
			catch (IOException e)
			{
				// Google Analytics
				GoogleAnalyticsUtils.sendException(context, e, i == MAX_ATTEMPTS ? true : false);

				Log.e(ACT_TAG, "Failed to register device with google servers (attempt " + i + " of " + MAX_ATTEMPTS + ")");
				Log.e(ACT_TAG, errorMessage = e.toString());

				if (i == MAX_ATTEMPTS)
				{
					List<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
					jsonObjectList.add(JSONUtils.createJSONObjectWithError(150, errorMessage));
					return jsonObjectList;
				}
			}

			try
			{
				Log.d(ACT_TAG, "Sleeping for " + backoff + "ms before retry");
				Thread.sleep(backoff);
			}
			catch (InterruptedException e1)
			{
				// Activity finished before we complete - exit.
				Log.d(ACT_TAG, "Thread interrupted: aborting remaining retries!");
				Thread.currentThread().interrupt();
				return null;
			}
			// Increase back-off exponentially
			backoff *= 2;
		}

		// We never reach this code
		return null;
	}

	// Old method that uses apache HttpClient class
	//	public JSONObject getJSONFromUrl2(String url, JSONObject jsonObject)
	//	{
	//		try
	//		{
	//			// Making HTTP request
	//			DefaultHttpClient httpClient = new DefaultHttpClient();
	//			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 1000 * HTTP_CONNECTION_TIMOUT_IN_SECONDS);
	//			HttpConnectionParams.setSoTimeout(httpClient.getParams(), 1000 * HTTP_SOCKET_TIMOUT_IN_SECONDS);
	//			HttpPost httpPost = new HttpPost(url);
	//
	//			httpPost.setEntity(new ByteArrayEntity(jsonObject.toString().getBytes("UTF8")));
	//			httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
	//
	//			HttpResponse httpResponse = httpClient.execute(httpPost);
	//			HttpEntity httpEntity = httpResponse.getEntity();
	//			return readInputStream(httpEntity.getContent());
	//		}
	//		catch (Exception e)
	//		{
	//			Log.e("Failed to retrieve JSON from server", e.toString());
	//			e.printStackTrace();
	//
	//			String errorMessage = e.toString();
	//			int errorCode = 199;
	//
	//			if (e instanceof InterruptedIOException)
	//			{
	//				// We're here in-case of ConnectTimeoutException or SocketTimeoutException
	//				errorMessage = localContext.getString(R.string.connection_timeout);
	//				errorCode = 100;
	//			}
	//
	//			Log.e(errorMessage, e.toString());
	//			return createJSONObjectWithError(errorMessage, errorCode);
	//		}
	//	}	
}