package com.roadwatch.server.datastore;

/**
 * Utility class which allow convenient access to the Datastore for different queries and commits. 
 * 
 */
public interface DatastoreConstants
{
	public static final String QUERY_KEY_CURSOR = "queryCursor";
	public static final String QUERY_KEY_ENCODED_CURSOR = "encodedCursor";
	public static final String QUERY_KEY_PAGE_SIZE = "queryPageSize";
	public static final int DEFAULT_QUERY_PAGE_SIZE = 20;
	public static final String QUERY_KEY_FROM_TIME = "queryFromTime";
	public static final String QUERY_KEY_TO_TIME = "queryToTime";
}