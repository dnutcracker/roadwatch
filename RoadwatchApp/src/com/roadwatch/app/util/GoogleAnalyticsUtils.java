package com.roadwatch.app.util;

import android.content.Context;

import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.roadwatch.app.ApplicationData;

public class GoogleAnalyticsUtils
{
	private static ExceptionParser exceptionParser;

	/**
	 * Returns the exception parser used by the default uncaught exception handler
	 * @param context
	 * @return
	 */
	private static ExceptionParser getExceptionParser(Context context)
	{
		if (exceptionParser == null)
		{
			Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			if (uncaughtExceptionHandler instanceof ExceptionReporter)
			{
				ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
				return exceptionReporter.getExceptionParser();
			}
			else
			{
				// Context and optional collection of package names to be used in reporting the exception.
				new StandardExceptionParser(context, null);
			}
		}

		return exceptionParser;
	}

	public static void sendException(Context context, Exception e, boolean fatal)
	{
		ApplicationData.getTracker().send(MapBuilder.createException(getExceptionParser(context).getDescription(Thread.currentThread().getName(), e), Boolean.valueOf(fatal)).build());
	}
}
