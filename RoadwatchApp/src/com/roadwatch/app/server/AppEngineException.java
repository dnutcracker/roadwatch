package com.roadwatch.app.server;

/**
 * Exception is thrown when the server has returned an error while trying to execute a remote operation.
 *  
 * @author Nati created at : 13:01:43
 *
 */
public class AppEngineException extends RuntimeException
{
	public AppEngineException()
	{
		super();
	}

	public AppEngineException(String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
	}

	public AppEngineException(String detailMessage)
	{
		super(detailMessage);
	}

	public AppEngineException(Throwable throwable)
	{
		super(throwable);
	}
}
