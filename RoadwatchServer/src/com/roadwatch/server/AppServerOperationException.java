package com.roadwatch.server;

import java.util.logging.Level;

public class AppServerOperationException extends Exception
{
	private int 	serverErrorCode;
	private Level	severityLevel;
	
	public AppServerOperationException(int serverErrorCode, String errorMessage)
	{
		this(serverErrorCode, Level.SEVERE, errorMessage, null);
	}
	
	public AppServerOperationException(int serverErrorCode, String errorMessage, Throwable cause)
	{
		this(serverErrorCode, Level.SEVERE, errorMessage, cause);
	}
	
	public AppServerOperationException(int serverErrorCode, Level severityLevel, String errorMessage)
	{
		this(serverErrorCode, severityLevel, errorMessage, null);
	}	

	public AppServerOperationException(int serverErrorCode, Level severityLevel, String errorMessage, Throwable cause)
	{
		super(errorMessage, cause);
		this.serverErrorCode = serverErrorCode;
		this.severityLevel = severityLevel;
	}
	
	public int getServerErrorCode()
	{
		return serverErrorCode;
	}
	
	public Level getSeverityLevel()
	{
		return severityLevel;
	}
}
