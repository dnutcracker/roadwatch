package com.roadwatch.server.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.User;

public class GCMUtils
{
	private static final Logger logger = Logger.getLogger(GCMUtils.class.getName());

	// dev-road-watch API key : AIzaSyAwOkz5V28M3YLNOIDsFPkJRz6cMadA9rc
	// road-watch API key : AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs
	private static final String API_KEY = "AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs";

	public static final int MULTICAST_MAX_SIZE = 1000;

	public static void sendMessageToUser(Message message, User user)
	{
		logger.info("Sending notification for single user with license plate " + user.getLicensePlate());
		List<User> singleUser = new ArrayList<User>();
		singleUser.add(user);
		sendMulticastMessage(message, singleUser);
	}

	//	public static void sendMessageToUsers(Message message, List<User> users)
	//	{
	//		logger.info("Sending notification to " + users.size() + " users");
	//		// send a multicast message using JSON, must split in chunks of 1000 devices (GCM limit)
	//		int total = users.size();
	//		List<User> usersChunk = new ArrayList<User>(total);
	//		int counter = 0;
	//		for (User user : users)
	//		{
	//			counter++;
	//			usersChunk.add(user);
	//			int partialSize = usersChunk.size();
	//			if (partialSize == MULTICAST_MAX_SIZE || counter == total)
	//			{
	//				sendMulticastMessage(message, usersChunk);
	//				usersChunk.clear();
	//			}
	//		}
	//	}

	private static List<String> getUsersRegIDs(List<User> users)
	{
		List<String> regIDs = new ArrayList<String>(users.size());
		for (User user : users)
		{
			if (!user.getAllGcmDestinationIDs().isEmpty())
				regIDs.addAll(user.getAllGcmDestinationIDs());
			else
				logger.log(Level.WARNING, "Failed to send notification to " + user + "GCM registration IDs list is empty");
		}

		return regIDs;
	}

	private static void sendMulticastMessage(Message message, List<User> users)
	{
		Sender sender = new Sender(API_KEY);
		boolean allDone = false;
		while (!allDone)
		{
			List<String> regIds = getUsersRegIDs(users);
			if (regIds.isEmpty())
				return;

			MulticastResult multicastResult;
			try
			{
				multicastResult = sender.sendNoRetry(message, regIds);
			}
			catch (IOException e)
			{
				logger.log(Level.SEVERE, "Exception posting " + message, e);
				return;
			}

			// check if any registration id must be updated
			if (multicastResult.getCanonicalIds() != 0)
			{
				List<Result> results = multicastResult.getResults();
				logger.info("Updating registration ids for " + results.size() + " devices");
				for (int i = 0; i < results.size(); i++)
				{
					String canonicalRegId = results.get(i).getCanonicalRegistrationId();
					if (canonicalRegId != null)
					{
						String regId = regIds.get(i);
						UsersAPI.updateRegistration(regId, canonicalRegId);
					}
				}
			}

			if (multicastResult.getFailure() != 0)
			{
				// there were failures, check if any could be retried
				List<Result> results = multicastResult.getResults();
				List<String> retriableRegIds = new ArrayList<String>();
				for (int i = 0; i < results.size(); i++)
				{
					String error = results.get(i).getErrorCodeName();
					if (error != null)
					{
						String regId = regIds.get(i);
						logger.warning("Got error (" + error + ") for regId " + regId);
						if (error.equals(Constants.ERROR_NOT_REGISTERED))
						{
							// application has been removed from device - unregister it
							logger.warning("regId" + regId + " : " + error);
							UsersAPI.unregisterRegIDAndRemoveEmptyUsers(regId);
						}
						if (error.equals(Constants.ERROR_UNAVAILABLE))
						{
							logger.warning("regId" + regId + " : ERROR_UNAVAILABLE, adding to retriabledRegIds array");
							retriableRegIds.add(regId);
						}
						else
						// All other errors
						{
							logger.severe(Constants.ERROR_MISMATCH_SENDER_ID);
						}
					}
				}

				if (!retriableRegIds.isEmpty())
				{
					allDone = false;
					regIds = retriableRegIds;
				}
				else
					allDone = true;
			}
			else
			// No multicast failures
			{
				allDone = true;
			}
		}

		logger.info("Finished sending notifications to " + users.size() + " users");
	}
}