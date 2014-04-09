package com.roadwatch.server.login;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.User;

/**
 * Login an existing user with the supplied credentials or auto-login an existing user using the GCM registration ID.
 * Also invoked when user updates its account information.
 * 
 */
@SuppressWarnings("serial")
public class LoginServlet extends BaseServlet
{
	private static final String USER_KEY_UUID = "uuid";
	private static final String USER_KEY_APP_VERSION = "appVersion";
	private static final String USER_KEY_ANDROID_VERSION = "androidVersion";
	private static final String USER_KEY_LICENSE_PLATE = "licensePlate";
	private static final String USER_KEY_USERNAME = "username";
	private static final String USER_KEY_EMAIL = "email";
	private static final String USER_KEY_PASSWORD = "password";
	private static final String USER_KEY_OWN_GCM_IDS = "ownGcmIds";
	private static final String USER_KEY_OWN_GCM_ID = "ownGcmId";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonObject = getJSONObject(req);
		String appVersion = jsonObject.has(USER_KEY_APP_VERSION) ? jsonObject.get(USER_KEY_APP_VERSION).getAsString() : "";
		String androidVersion = jsonObject.has(USER_KEY_ANDROID_VERSION) ? jsonObject.get(USER_KEY_ANDROID_VERSION).getAsString() : "";

		try
		{
			// Check if we're using auto-login (also used to update user account info)
			if (jsonObject.has(USER_KEY_UUID))
			{
				String uuid = jsonObject.get(USER_KEY_UUID).getAsString();
				String newGcmDestination = jsonObject.has(USER_KEY_OWN_GCM_ID) ? jsonObject.get(USER_KEY_OWN_GCM_ID).getAsString() : "";
				JsonArray gcmRegIDsJsonArray = jsonObject.has(USER_KEY_OWN_GCM_IDS) ? jsonObject.get(USER_KEY_OWN_GCM_IDS).getAsJsonArray() : null;
				String newUsername = jsonObject.has(USER_KEY_USERNAME) ? jsonObject.get(USER_KEY_USERNAME).getAsString() : "";
				String newEmail = jsonObject.has(USER_KEY_EMAIL) ? jsonObject.get(USER_KEY_EMAIL).getAsString() : "";
				String newPassword = jsonObject.has(USER_KEY_PASSWORD) ? jsonObject.get(USER_KEY_PASSWORD).getAsString() : "";
				List<String> gcmDestinations = new ArrayList<String>();
				if (gcmRegIDsJsonArray != null)
				{
					for (int i = 0; i < gcmRegIDsJsonArray.size(); i++)
						gcmDestinations.add(gcmRegIDsJsonArray.get(i).getAsString());
				}
				User user = UsersAPI.autoLoginUser(uuid, newUsername, newEmail, newPassword, appVersion, androidVersion, newGcmDestination, gcmDestinations);

				// auto-login successful
				setSuccess(resp, user);
			}
			else
			// Regular manual login using credentials
			{
				String licensePlate = jsonObject.get(USER_KEY_LICENSE_PLATE).getAsString();
				String password = jsonObject.get(USER_KEY_PASSWORD).getAsString();

				User user = UsersAPI.loginUser(licensePlate, password, appVersion, androidVersion);
				setSuccess(resp, user);
			}
		}
		catch (AppServerOperationException e)
		{
			setFailure(resp, e);
		}
	}
}