package com.roadwatch.server.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.CreationDate;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;

@Model
public class User
{
	@Attribute(primaryKey = true)
	private Key key;

	private String uuid;
	private String appVersion;
	private String androidVersion;

	public enum UserType
	{
		/** The default user which owns the car */
		REGULAR,

		/** This type is used when this user only exists in order to be tracked by others.
		 * (which means that the owner of its license plate had never registered with the system)
		 * Once the owner will be registered this user type will be changed to REGULAR.*/
		TRACKED,

		/** This user is not the car owner but is using(sharing) the same car(license plate) */
		SHARED
	}

	private UserType type;
	private String licensePlate;
	private String username;
	private String email;	
	@Attribute(persistent = false)
	private String password;
	private String encryptedPassword;
	
	/**
	 * Contains the registration ids of all the devices owned by this user.
	 */
	private List<String> ownGcmIds;
	
	/**
	 * Contains the registration ids of all the users that are tracking this license plate
	 */
	private List<String> trackingGcmIds;

	/**
	 * 2 list that contain the license plates that we wish to track (i.e. get notified in case they are reported)
	 * and their corresponding names.(These are user-friendly names are given by the user)
	 */
	private List<String> trackedLicensePlates;
	private List<String> trackedNames;

	/**
	 * User can increase the value of this field by purchasing more tracked license plates. 
	 */
	private int purchasedTrackedLicensePlatesSize;
	
	@Attribute(listener = CreationDate.class)
	private Date createdAt;
	
	/**
	 * List of keys of all the users that are sharing this car
	 */
	private List<Key> sharingUserKeys;

	public Key getKey()
	{
		return key;
	}

	public void setKey(Key key)
	{
		this.key = key;
		this.uuid = Datastore.keyToString(key);
	}

	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public String getAppVersion()
	{
		return appVersion;
	}

	public void setAppVersion(String appVersion)
	{
		this.appVersion = appVersion;
	}

	public String getAndroidVersion()
	{
		return androidVersion;
	}

	public void setAndroidVersion(String androidVersion)
	{
		this.androidVersion = androidVersion;
	}

	public UserType getType()
	{
		return type;
	}

	public void setType(UserType type)
	{
		this.type = type;
	}

	public String getLicensePlate()
	{
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate)
	{
		this.licensePlate = licensePlate;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getEncryptedPassword()
	{
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword)
	{
		this.encryptedPassword = encryptedPassword;
	}

	public List<String> getOwnGcmIds()
	{
		return ownGcmIds;
	}

	public void setOwnGcmIds(List<String> gcmDestinations)
	{
		this.ownGcmIds = gcmDestinations;
	}
	
	public void addOwnGcmId(String newOwnGcmId)
	{
		if (!ownGcmIds.contains(newOwnGcmId))
			ownGcmIds.add(newOwnGcmId);
	}
	
	public List<String> getTrackingGcmIds()
	{
		if(trackingGcmIds==null)
			trackingGcmIds = new ArrayList<>();
			
		return trackingGcmIds;
	}

	public void setTrackingGcmIds(List<String> trackingGcmIds)
	{
		this.trackingGcmIds = trackingGcmIds;
	}

	public void addTrackingGcmId(String newTrackingGcmId)
	{
		if(trackingGcmIds==null)
			trackingGcmIds = new ArrayList<>();
			
		if (!trackingGcmIds.contains(newTrackingGcmId))
			trackingGcmIds.add(newTrackingGcmId);
	}
	
	public void addAllTrackingGcmIds(List<String> newTrackingGcmIds)
	{
		if(trackingGcmIds==null)
			trackingGcmIds = new ArrayList<>();
			
		if (!trackingGcmIds.containsAll(newTrackingGcmIds))
		{
			trackingGcmIds.removeAll(newTrackingGcmIds);
			trackingGcmIds.addAll(newTrackingGcmIds);
		}
	}	
	
	/**
	 * A convenient method for getting all the GCM destinations IDs
	 * @return A merged list of ownGCMIds list and trackingGcmIds list.
	 */
	public List<String> getAllGcmDestinationIDs()
	{
		List<String> allGcmIds = new ArrayList<>();
		allGcmIds.addAll(ownGcmIds);
		if(trackingGcmIds!=null)
			allGcmIds.addAll(trackingGcmIds);
		
		return allGcmIds;
	}

	public List<String> getTrackedLicensePlates()
	{
		if(trackedLicensePlates==null)
			trackedLicensePlates = new ArrayList<>();
			
		return trackedLicensePlates;
	}

	public void setTrackedLicensePlates(List<String> trackedLicensePlates)
	{
		this.trackedLicensePlates = trackedLicensePlates;
		purchasedTrackedLicensePlatesSize = trackedLicensePlates.size();		
	}
	
	public List<String> getTrackedNames()
	{
		return trackedNames;
	}

	public void setTrackedNames(List<String> trackedLicensePlateNames)
	{
		this.trackedNames = trackedLicensePlateNames;
	}
	
	/**
	 * Use this method to add a new tracked car.
	 * 
	 * @param newTrackedLicensePlate
	 * @param newTrackedName
	 */
	public void addTracked(String newTrackedLicensePlate, String newTrackedName)
	{
		if(trackedLicensePlates==null)
		{
			trackedLicensePlates = new ArrayList<>();
			trackedNames = new ArrayList<>();
		}
		
		if(!trackedLicensePlates.contains(newTrackedLicensePlate))
		{
			trackedLicensePlates.add(newTrackedLicensePlate);
			trackedNames.add(newTrackedName);
		}
	}

	public int getPurchasedTrackedLicensePlatesSize()
	{
		return purchasedTrackedLicensePlatesSize;
	}

	public void setPurchasedTrackedLicensePlatesSize(int purchasedTrackedLicensePlatesSize)
	{
		this.purchasedTrackedLicensePlatesSize = purchasedTrackedLicensePlatesSize;
	}

	public Date getCreatedAt()
	{
		return createdAt;
	}

	public void setCreatedAt(Date createdAt)
	{
		this.createdAt = createdAt;
	}

	public List<Key> getSharingUserKeys()
	{
		return sharingUserKeys;
	}

	public void setSharingUserKeys(List<Key> sharingUsers)
	{
		this.sharingUserKeys = sharingUsers;
	}
	
	@Override
	public String toString()
	{
		return (username!=null ? "Username : " + username : "") + "[" + licensePlate + "]" + (type!=UserType.REGULAR ? " " + type : "");
	}
}