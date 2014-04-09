package com.roadwatch.server.track;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.User;

/**
 * Adds a new tracked license plate.(for which we would like to recieve a notification when reported)
 * 
 */
public class ManageTrackedLicensePlatesServlet extends BaseServlet
{
	private static final String USER_KEY_LICENSE_PLATE = "licensePlate";
	private static final String USER_KEY_NEW_TRACKED_LICENSE_PLATE = "newTrackedLicensePlate";
	private static final String USER_KEY_TRACKED_LICENSE_PLATE = "trackedLicensePlate";
	private static final String USER_KEY_TRACKED_NAME = "trackedName";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonData = getJSONObject(req);
		String trackingUserLicensePlate = jsonData.get(USER_KEY_LICENSE_PLATE).getAsString();
		boolean isRemove = !jsonData.has(USER_KEY_TRACKED_NAME);
		boolean isUpdate = jsonData.has(USER_KEY_NEW_TRACKED_LICENSE_PLATE);

		try
		{
			User user = null;
			if (isRemove)
			{
				// Remove an existing tracked car
				String trackedLicensePlate = jsonData.get(USER_KEY_TRACKED_LICENSE_PLATE).getAsString();
				user = UsersAPI.manageTrackedLicensePlates(trackingUserLicensePlate, "", "", trackedLicensePlate);
				logger.info("Tracked license plate [" + trackedLicensePlate + "] was removed from " + user);
			}
			else if (isUpdate)
			{
				// Update an existing tracked car
				String newTrackedName = jsonData.get(USER_KEY_TRACKED_NAME).getAsString();
				String newTrackedLicensePlate = jsonData.get(USER_KEY_NEW_TRACKED_LICENSE_PLATE).getAsString();
				String existingTrackedLicensePlate = jsonData.get(USER_KEY_TRACKED_LICENSE_PLATE).getAsString();
				user = UsersAPI.manageTrackedLicensePlates(trackingUserLicensePlate, newTrackedLicensePlate, newTrackedName, existingTrackedLicensePlate);
				logger.info("Tracked license plate [" + existingTrackedLicensePlate + "] was updated to [" + newTrackedLicensePlate + "] for " + user);
			}
			else
			{
				// Add new tracked car
				String newTrackedName = jsonData.get(USER_KEY_TRACKED_NAME).getAsString();
				String newTrackedLicensePlate = jsonData.get(USER_KEY_TRACKED_LICENSE_PLATE).getAsString();
				user = UsersAPI.manageTrackedLicensePlates(trackingUserLicensePlate, newTrackedLicensePlate, newTrackedName, "");
				logger.info("Added tracked license plate [" + newTrackedLicensePlate + "] to " + user);
			}

			setSuccess(resp, user);
		}
		catch (AppServerOperationException e)
		{
			setFailure(resp, e);
		}
	}
}