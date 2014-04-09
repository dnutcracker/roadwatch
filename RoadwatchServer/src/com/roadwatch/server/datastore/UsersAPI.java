package com.roadwatch.server.datastore;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slim3.datastore.Datastore;
import org.slim3.datastore.EntityNotFoundRuntimeException;

import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.meta.UserMeta;
import com.roadwatch.server.model.User;
import com.roadwatch.server.model.User.UserType;
import com.roadwatch.server.utils.EncryptionUtils;

public class UsersAPI
{
	private static final Logger logger = Logger.getLogger(UsersAPI.class.getName());

	private static final Key USERS_ANCESTOR_KEY = Datastore.createKey("UsersData", "UsersAncestorKey");

	/**
	 * Registers a device.
	 *
	 * @param regId device's registration id.
	 * @throws AppServerOperationException 
	 */
	public static User registerUser(User newUser) throws AppServerOperationException
	{
		logger.info("Registering " + newUser);

		// Password might be empty if the user chose to sign in with Facebook/Google+
		if (!newUser.getPassword().isEmpty())
			newUser.setEncryptedPassword(EncryptionUtils.encrypt(newUser.getPassword()));

		Transaction txn = Datastore.beginTransaction();
		try
		{
			String licensePlate = newUser.getLicensePlate();
			
			if (findUserByLicensePlate(txn, licensePlate) != null)
				throw new AppServerOperationException(3, Level.WARNING, "License plate [" + licensePlate + "] already exists");

			// Allow only registering a single license plate per device(regID) so 
			// we unregister this device(regID) from any other license plate (if exists)
			// Without this check the user might have logged in and out on the same device using multiple license plates 
			// and receiving notifications for all of them (thus bypassing the need for the tracking license plate mechanism)
			if (newUser.getOwnGcmIds() != null && !newUser.getOwnGcmIds().isEmpty())
				removeRegIDFromAllUsers(txn, newUser.getOwnGcmIds().get(0));

			Key userKey = Datastore.createKey(USERS_ANCESTOR_KEY, User.class, newUser.getLicensePlate());
			newUser.setKey(userKey);

			// If a TRACKED user already existed with this license plate - copy the tracking ids into the new user.
			User existingTrackedUser = findUserByLicensePlate(txn, licensePlate, true);
			if (existingTrackedUser != null)
				newUser.setTrackingGcmIds(existingTrackedUser.getTrackingGcmIds());

			Datastore.put(txn, newUser);
			txn.commit();

			return newUser;
		}
		catch (DatastoreFailureException | ConcurrentModificationException e)
		{
			// Failed to register user
			e.printStackTrace();
			throw new AppServerOperationException(4, "Failed to register user : " + newUser, e);
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	/**
	 * Auto login user according to GCM registration ID.
	 * Updates user <code>lastLoginAt</code> field and <code>gcmRegistraionID</code> if necessary.
	 * 
	 * @param uuid
	 * @param newRegID
	 * @param gcmRegIDsList 
	 * @param androidVersion 
	 * @return The updated user if successful, null otherwise 
	 */
	public static User autoLoginUser(String uuid, String username, String email, String password, String appVersion, String androidVersion, String newRegID, List<String> gcmRegIDsList)
			throws AppServerOperationException
	{
		Key key = null;
		try
		{
			key = Datastore.stringToKey(uuid);
		}
		catch (IllegalArgumentException e)
		{
			// This should only happen when the client is unregistered yet and sends the user's email instead of UUID just for logging)
			throw new AppServerOperationException(2, Level.INFO, "Auto login invoked by unregistered user (email=" + uuid + ", appVersion=" + appVersion + ")");
		}

		Transaction tx = Datastore.beginTransaction();
		try
		{
			User user = Datastore.get(tx, User.class, key);
			if (user != null)
			{
				boolean userChanged = false;

				// Update only changed properties
				if (!username.isEmpty() && !user.getUsername().equals(username))
				{
					logger.info(user + " updated username " + user.getUsername() + " to " + username);
					user.setUsername(username);
					userChanged = true;
				}
				if (!email.isEmpty() && !user.getEmail().equals(email))
				{
					logger.info(user + " updated email " + user.getEmail() + " to " + email);
					user.setEmail(email);
					userChanged = true;
				}
				if (!password.isEmpty())
				{
					String encryptedPassword = EncryptionUtils.encrypt(password);
					logger.info(user + " updated password");
					user.setEncryptedPassword(encryptedPassword);
					userChanged = true;
				}
				if (!newRegID.isEmpty() && !user.getOwnGcmIds().contains(newRegID))
				{
					logger.info(user + " updated with a new GCM registration ID");
					user.addOwnGcmId(newRegID);
					userChanged = true;
				}
				if (!appVersion.isEmpty() && !user.getAppVersion().equals(appVersion))
				{
					logger.info(user + " upgraded app version " + user.getAppVersion() + " to " + appVersion);
					user.setAppVersion(appVersion);
					userChanged = true;
				}
				if (!androidVersion.isEmpty() && !user.getAndroidVersion().equals(androidVersion))
				{
					logger.info(user + " upgraded android version " + user.getAndroidVersion() + " to " + androidVersion);
					user.setAndroidVersion(androidVersion);
					userChanged = true;
				}

				// Check if we need to update the user's device registration IDs (Usually happens when user has logged in from a new device)
				if (!gcmRegIDsList.isEmpty())
				{
					user.setOwnGcmIds(gcmRegIDsList);
					userChanged = true;
					logger.info(user + " was set with a new GCM device registration IDs list");
				}

				if (userChanged)
				{
					Datastore.put(tx, user);
					tx.commit();
				}

				return user;
			}
			else
			{
				// Should not happen since an exception would be thrown (PENDING: remove)
				throw new AppServerOperationException(0, "???");
			}
		}
		catch (EntityNotFoundRuntimeException e)
		{
			// EntityNotFoundRuntimeException is thrown by Datastore.get() if user no longer exists
			throw new AppServerOperationException(2, Level.WARNING, "Failed to auto login user - UUID doesn't exist");
		}
		catch (DatastoreFailureException | ConcurrentModificationException e)
		{
			// Cannot auto-login due to a datastore failure reason 
			e.printStackTrace();
			throw new AppServerOperationException(0, "Failed to auto login user", e);
		}
		catch (IllegalArgumentException e)
		{
			// Cannot auto-login (usually happen due to user logged in from a different app engine)
			e.printStackTrace();
			throw new AppServerOperationException(2, "Failed to auto login user", e);
		}
		finally
		{
			if (tx.isActive())
			{
				tx.rollback();
			}
		}
	}

	/**
	 * Login user with license plate and password credentials.
	 * Updates user's <code>appVersion</code>and <code>androidVersion</code>
	 * 
	 * PENDING:
	 * 1. Check if the GCM reg ID that the user is registering/logging-in with exists already with another LP.
	 * For example, the user might clear-data/uninstall and login/register with a different LP on the same device - 
	 * we don't allow that - receiving notifications on multiple LPs on the same device is a paid feature.
	 * If this is the case, remove any previous GCM reg IDs from other LPs.
	 * No need for client message - do it automatically.
	 * (Client Message 'You already logged-in from this device with a different license plate)   
	 * 
	 * @param licensePlate
	 * @param password
	 * @param androidVersion 
	 * @param appVersion 
	 * @return The updated user if successful, null otherwise
	 */
	public static User loginUser(String licensePlate, String password, String appVersion, String androidVersion) throws AppServerOperationException
	{
		Transaction txn = Datastore.beginTransaction();
		try
		{
			User user = findUserByLicensePlate(txn, licensePlate);
			if (user != null)
			{
				// Check if password match
				String decryptedPassword = EncryptionUtils.decrypt(user.getEncryptedPassword());
				if (decryptedPassword.equals(password))
				{
					// Update latest user information					
					user.setAppVersion(appVersion);
					user.setAndroidVersion(androidVersion);

					// Store
					Datastore.put(txn, user);
					txn.commit();

					return user;
				}
				else
				// Password mismatch
				{
					throw new AppServerOperationException(1, Level.WARNING, "Failed to login. Bad credentails for user : " + user);
				}
			}
			else
			// License plate does not exist
			{
				throw new AppServerOperationException(1, Level.WARNING, "Failed to login. Bad credentails for user : " + user);
			}
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	/**
	 * Removes the specified reg ID from all existing users 
	 * 
	 * @param tx
	 * @param regIdToRemove
	 */
	private static void removeRegIDFromAllUsers(Transaction tx, String regIdToRemove)
	{
		UserMeta userMeta = UserMeta.get();

		// Merge lists (PENDING: any more efficient way of doing it ?)
		List<User> usersList = Datastore.query(tx, userMeta, USERS_ANCESTOR_KEY).filterInMemory(userMeta.trackingGcmIds.contains(regIdToRemove)).asList();
		usersList.addAll(Datastore.query(tx, userMeta, USERS_ANCESTOR_KEY).filterInMemory(userMeta.ownGcmIds.contains(regIdToRemove)).asList());

		for (User foundUser : usersList)
			removeRegIDFromUser(tx, regIdToRemove, foundUser);
	}

	/**
	 * Removes the specified reg ID from the specified User.
	 * It also removes the user from the datastore with the following conditions :
	 * 1. If after removal for a REGULAR user its ownRegIds and trackingIDs is empty 
	 * 2. If after removal for a TRACKED user its trackingIDs is empty

	 * @param tx
	 * @param regIdToRemove
	 * @param user
	 */
	private static void removeRegIDFromUser(Transaction tx, String regIdToRemove, User user)
	{
		user.getOwnGcmIds().remove(regIdToRemove);
		user.getTrackingGcmIds().remove(regIdToRemove);
		Datastore.put(tx, user);

		// Check if this REGULAR user has no more devices associated with him
		if (user.getType() == UserType.REGULAR && user.getOwnGcmIds().isEmpty())
		{
			// Remove user if there are no more devices associated and no one is tracking him
			if (user.getTrackingGcmIds().isEmpty())
			{
				Datastore.delete(user.getKey());
				logger.info("Removed REGULAR user with no associated gcm or tracking ids! : " + user);
			}
			else
			{
				// Others still want to track this user - change it to a TRACKED user
				user.setType(UserType.TRACKED);
				Datastore.put(tx, user);

				logger.info(user + " changed from REGULAR to TRACKED");
			}
		}

		// Remove user if it's of type TRACKED and there is no one tracking him any more
		if (user.getType() == UserType.TRACKED && user.getTrackingGcmIds().isEmpty())
		{
			Datastore.delete(user.getKey());
			logger.info("Removed TRACKED user with no trackers! : " + user);
		}
	}

	/**
	 * Find all REGULAR users with the specified license plate.<BR>
	 * Users sharing the same car (Family, Co-workers...etc) or users that exists only for tracking will not be returned.
	 * 
	 * @param txn 
	 * 
	 * @param licensePlate
	 * @return
	 */
	public static User findUserByLicensePlate(Transaction txn, String licensePlate)
	{
		return findUserByLicensePlate(txn, licensePlate, false);
	}

	/**
	 * Find all users according to the specified <code>includeTrackedUsers</code> flag -<BR>
	 * If <code>true</code> it will return all REGUALR and TRACKED users with the specified license plate.<BR>
	 * If <code>false</code> it will return only REGUALR users.
	 * 
	 * @param txn
	 * @param licensePlate
	 * @param includeTrackedUsers
	 * @return
	 */
	public static User findUserByLicensePlate(Transaction txn, String licensePlate, boolean includeTrackedUsers)
	{
		User user = null;
		UserMeta userMeta = UserMeta.get();
		List<User> usersList = Datastore.query(txn, userMeta, USERS_ANCESTOR_KEY)
				.filter(userMeta.licensePlate.equal(licensePlate), includeTrackedUsers ? userMeta.type.notEqual(UserType.SHARED) : userMeta.type.equal(UserType.REGULAR)).asList();

		if (!usersList.isEmpty())
			user = usersList.get(0);

		return user;
	}

	public static User findUserByLicensePlate(String licensePlate)
	{
		return findUserByLicensePlate(licensePlate, false);
	}

	public static User findUserByLicensePlate(String licensePlate, boolean includeTrackedUsers)
	{
		Transaction txn = Datastore.beginTransaction();
		try
		{
			return findUserByLicensePlate(txn, licensePlate, includeTrackedUsers);
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	public static void unregisterRegIDAndRemoveEmptyUsers(String regId)
	{
		Transaction txn = Datastore.beginTransaction();
		try
		{
			removeRegIDFromAllUsers(txn, regId);
			txn.commit();
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	/**
	 * Update the users GcmRegIds if necessary
	 *
	 * @param regId device's registration id.
	 * @param canonicalRegId
	 */
	public static void updateRegistration(String regId, String canonicalRegId)
	{
		Transaction txn = Datastore.beginTransaction();
		try
		{
			UserMeta userMeta = UserMeta.get();
			List<User> usersToUpdate = Datastore.query(userMeta).filterInMemory(userMeta.ownGcmIds.contains(regId), userMeta.trackingGcmIds.contains(regId)).asList();
			for (User userToUpdate : usersToUpdate)
			{
				if (userToUpdate.getOwnGcmIds().contains(regId))
				{
					userToUpdate.getOwnGcmIds().remove(regId);
					userToUpdate.getOwnGcmIds().add(canonicalRegId);
				}
				else
				{
					userToUpdate.getTrackingGcmIds().remove(regId);
					userToUpdate.getTrackingGcmIds().add(canonicalRegId);
				}
			}

			Datastore.put(txn, usersToUpdate);
			txn.commit();
			logger.info("Updated GCM reg ID for " + usersToUpdate.size() + " users");
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	/**
	 * Gets the number of total devices.
	 */
	public static List<User> getUsers()
	{
		return Datastore.query(UserMeta.get()).asList();
	}

	/**
	 * Add a new tracked license plate for the specified license plate or updates an existing one.
	 * 
	 * @param trackingUserLicensePlate The license plate of the tracking user
	 * @param newTrackedLicensePlate The license plate of the newly tracked user
	 * @param newTrackedName The user given name for the newly tracked user
	 * @param existingTrackedLicensePlate The license plate of an existing tracked license plate to update
	 * @return The updated tracking user object
	 * @throws AppServerOperationException
	 */
	public static User manageTrackedLicensePlates(String trackingUserLicensePlate, String newTrackedLicensePlate, String newTrackedName, String existingTrackedLicensePlate)
			throws AppServerOperationException
	{
		Transaction txn = Datastore.beginTransaction();
		User trackingUser = null;
		try
		{
			// Get the tracking user (userLicensePlate)
			trackingUser = findUserByLicensePlate(txn, trackingUserLicensePlate);

			// If new tracking license plate is empty - we're removing a tracked car
			if (newTrackedLicensePlate.isEmpty())
			{
				int trackedLicensePlateIndex = trackingUser.getTrackedLicensePlates().indexOf(existingTrackedLicensePlate);

				// Remove tracked car from list				
				trackingUser.getTrackedLicensePlates().remove(trackedLicensePlateIndex);
				trackingUser.getTrackedNames().remove(trackedLicensePlateIndex);

				// Unregisters the tracking user from the tracked user
				User trackedUser = findUserByLicensePlate(txn, existingTrackedLicensePlate, true);
				for (String ownGcmId : trackingUser.getOwnGcmIds())
					removeRegIDFromUser(txn, ownGcmId, trackedUser);
			}
			else
			{
				// Check if we're replacing a tracked car with another 
				if (!existingTrackedLicensePlate.isEmpty())
				{
					int trackedLicensePlateIndex = trackingUser.getTrackedLicensePlates().indexOf(existingTrackedLicensePlate);

					// Replace tracked lp and name with new ones
					trackingUser.getTrackedLicensePlates().set(trackedLicensePlateIndex, newTrackedLicensePlate);
					trackingUser.getTrackedNames().set(trackedLicensePlateIndex, newTrackedName);

					// Remove the tracking user ids from previous tracked license plate
					User oldTrackedUser = findUserByLicensePlate(txn, existingTrackedLicensePlate, true);
					for (String ownGcmId : trackingUser.getOwnGcmIds())
						removeRegIDFromUser(txn, ownGcmId, oldTrackedUser);
				}
				else
				{
					// Add the new tracked car info
					trackingUser.addTracked(newTrackedLicensePlate, newTrackedName);
				}

				// Get the tracked user (newTrackedLicensePlate)
				User trackedUser = findUserByLicensePlate(txn, newTrackedLicensePlate, true);

				// If tracked user does not exists - create it with type=TRACKED
				if (trackedUser == null)
				{
					trackedUser = new User();
					Key trackedUserKey = Datastore.createKey(USERS_ANCESTOR_KEY, User.class, newTrackedLicensePlate);
					trackedUser.setKey(trackedUserKey);
					trackedUser.setType(UserType.TRACKED);
					trackedUser.setLicensePlate(newTrackedLicensePlate);
				}

				// Add the tracking user's gcm reg ID to the tracked user trackingGcmIds
				trackedUser.addAllTrackingGcmIds(trackingUser.getOwnGcmIds());

				// Update the tracked user
				Datastore.put(txn, trackedUser);
			}

			// Update the tracking user
			Datastore.put(txn, trackingUser);

			// Commit
			txn.commit();
		}
		catch (DatastoreFailureException | ConcurrentModificationException e)
		{
			// Failed to register user
			e.printStackTrace();
			throw new AppServerOperationException(4, "Failed to add/update tracked license plate to user [" + trackingUserLicensePlate + "]", e);
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}

		return trackingUser;
	}

	//	/**
	//	 * Creates a persistent record with the devices to be notified using a multicast message.
	//	 *
	//	 * @param devices registration ids of the devices.
	//	 * @return encoded key for the persistent record.
	//	 */
	//	public static String createMulticast(List<String> devices)
	//	{
	//		logger.info("Storing multicast for " + devices.size() + " devices");
	//		String encodedKey;
	//		Transaction txn = Datastore.beginTransaction();
	//		try
	//		{
	//			Entity entity = new Entity(MULTICAST_TYPE);
	//			entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
	//			Datastore.put(txn, entity);
	//			Key key = entity.getKey();
	//			encodedKey = Datastore.keyToString(key);
	//			logger.fine("multicast key: " + encodedKey);
	//			txn.commit();
	//		}
	//		finally
	//		{
	//			if (txn.isActive())
	//			{
	//				txn.rollback();
	//			}
	//		}
	//		return encodedKey;
	//	}
	//
	//	/**
	//	 * Gets a persistent record with the devices to be notified using a	multicast message.
	//	 *
	//	 * @param encodedKey encoded key for the persistent record.
	//	 */
	//	public static List<String> getMulticast(String encodedKey)
	//	{
	//		Key key = Datastore.stringToKey(encodedKey);
	//		Transaction txn = Datastore.beginTransaction();
	//		try
	//		{
	//			Entity entity = Datastore.get(txn, key);
	//			@SuppressWarnings("unchecked")
	//			List<String> devices = (List<String>) entity.getProperty(MULTICAST_REG_IDS_PROPERTY);
	//			txn.commit();
	//			return devices;
	//		}
	//		catch (EntityNotFoundRuntimeException e)
	//		{
	//			logger.severe("No entity for key " + key);
	//			return Collections.emptyList();
	//		}
	//		finally
	//		{
	//			if (txn.isActive())
	//			{
	//				txn.rollback();
	//			}
	//		}
	//	}
	//
	//	/**
	//	 * Updates a persistent record with the devices to be notified using a multicast message.
	//	 *
	//	 * @param encodedKey encoded key for the persistent record.
	//	 * @param devices new list of registration ids of the devices.
	//	 */
	//	public static void updateMulticast(String encodedKey, List<String> devices)
	//	{
	//		Key key = KeyFactory.stringToKey(encodedKey);
	//		Entity entity;
	//		Transaction txn = Datastore.beginTransaction();
	//		try
	//		{
	//			try
	//			{
	//				entity = Datastore.get(key);
	//			}
	//			catch (EntityNotFoundRuntimeException e)
	//			{
	//				logger.severe("No entity for key " + key);
	//				return;
	//			}
	//			entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
	//			Datastore.put(txn, entity);
	//			txn.commit();
	//		}
	//		finally
	//		{
	//			if (txn.isActive())
	//			{
	//				txn.rollback();
	//			}
	//		}
	//	}
	//
	//	/**
	//	 * Deletes a persistent record with the devices to be notified using a multicast message.
	//	 *
	//	 * @param encodedKey encoded key for the persistent record.
	//	 */
	//	public static void deleteMulticast(String encodedKey)
	//	{
	//		Transaction txn = Datastore.beginTransaction();
	//		try
	//		{
	//			Key key = Datastore.stringToKey(encodedKey);
	//			Datastore.delete(txn, key);
	//			txn.commit();
	//		}
	//		finally
	//		{
	//			if (txn.isActive())
	//			{
	//				txn.rollback();
	//			}
	//		}
	//	}	
}