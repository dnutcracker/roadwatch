package com.roadwatch.app.util;

public interface JSONKeys
{
	// All entries in the datastore contains this key
	public static final String DATASTORE_KEY = "key";

	// Server responses JSON property names
	public static final String RESPNOSE_KEY_ERROR = "error";
	public static final String RESPNOSE_KEY_ERROR_CODE = "errorCode";

	// Server query JSON property names
	public static final String QUERY_KEY_CURSOR = "queryCursor";
	public static final String QUERY_KEY_ENCODED_CURSOR = "encodedCursor";
	public static final String QUERY_KEY_PAGE_SIZE = "queryPageSize";
	public static final String QUERY_KEY_FROM_TIME = "queryFromTime";
	public static final String QUERY_KEY_TO_TIME = "queryToTime";

	// 'Report' object JSON property names
	public static final String REPORT_KEY_LICENSE_PLATE = "licensePlate";
	public static final String REPORT_KEY_LATITUDE = "latitude";
	public static final String REPORT_KEY_LONGITUDE = "longitude";
	public static final String REPORT_KEY_CODE = "reportCode";
	public static final String REPORT_KEY_DESCRIPTION = "reportDescription";
	public static final String REPORT_KEY_TIME = "reportTime";
	public static final String REPORT_KEY_REPORTED_BY = "reportedBy";
	public static final String REPORT_KEY_TIME_FOR_DISPLAY = "reportTimeForDisplay";
	public static final String REPORT_KEY_ADDRESS_FOR_DISPLAY = "addressForDisplay";

	// 'User' object JSON property names
	public static final String USER_KEY_CREATED_AT = "createdAt";
	public static final String USER_KEY_LICENSE_PLATE = "licensePlate";
	public static final String USER_KEY_UUID = "uuid";
	public static final String USER_KEY_APP_VERSION = "appVersion";
	public static final String USER_KEY_ANDROID_VERSION = "androidVersion";
	public static final String USER_KEY_TYPE = "type";
	public static final String USER_KEY_USERNAME = "username";
	public static final String USER_KEY_EMAIL = "email";
	public static final String USER_KEY_PASSWORD = "password";
	public static final String USER_KEY_OWN_GCM_ID = "ownGcmId";
	public static final String USER_KEY_OWN_GCM_IDS = "ownGcmIds";
	public static final String USER_KEY_TRACKED_LICENSE_PLATE = "trackedLicensePlate";
	public static final String USER_KEY_TRACKED_LICENSE_PLATES = "trackedLicensePlates";
	public static final String USER_KEY_NEW_TRACKED_LICENSE_PLATE = "newTrackedLicensePlate";
	public static final String USER_KEY_TRACKED_NAME = "trackedName";
	public static final String USER_KEY_TRACKED_NAMES = "trackedNames";
	public static final String USER_KEY_PURCHASED_TRACKED_LICENSE_PLATE_SIZE = "purchasedTrackedLicensePlatesSize";
	

	// 'UserCurrentLocation' object JSON property names (Used only locally for saving user location while driving)
	public static final String USER_CURRENT_LOCATION_KEY_LICENSE_PLATE = "currentLocati1onLicensePlate";
	public static final String USER_CURRENT_LOCATION_KEY_LATITUDE = "currentLocationLatitude";
	public static final String USER_CURRENT_LOCATION_KEY_LONGITUDE = "currentLocationLongitude";
	public static final String USER_CURRENT_LOCATION_KEY_TIME = "currentLocationTime";
}