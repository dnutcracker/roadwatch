package com.roadwatch.server;

import java.io.IOException;

import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.roadwatch.server.utils.Utils;

/**
 * Get reports from the datastore.
 */
@SuppressWarnings("serial")
public class LogClientExceptionServlet extends BaseServlet
{
	private static final String APP_VERSION_NAME = "APP_VERSION_NAME";
	//private static final String APP_VERSION_CODE = "APP_VERSION_CODE";
	private static final String PHONE_MODEL = "PHONE_MODEL";
	private static final String ANDROID_VERSION = "ANDROID_VERSION";
	private static final String USER_APP_START_DATE = "USER_APP_START_DATE";
	private static final String STACK_TRACE = "STACK_TRACE";
	private static final String LOG_CAT = "LOG_CAT";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonException = getJSONObject(req);

		StringBuffer messageBody = new StringBuffer();
		messageBody.append("App Version " + getProperty(jsonException, APP_VERSION_NAME) + " on device " + getProperty(jsonException, PHONE_MODEL) + " running Android "
				+ getProperty(jsonException, ANDROID_VERSION) + "\n");
		messageBody.append("Application started at : " + getProperty(jsonException, USER_APP_START_DATE) + "\n\n");
		messageBody.append("StackTrace : \n");
		messageBody.append(getProperty(jsonException, STACK_TRACE) + "\n");
		if (jsonException.has(LOG_CAT))
		{
			messageBody.append("LogCat : \n");
			messageBody.append(jsonException.get(LOG_CAT).getAsString() + "\n");
		}

		// Send the email
		Utils.sendEmail(new InternetAddress(Utils.ADMIN_EMAIL, "RoadWatch Server"), new InternetAddress(Utils.ROADWATCH_BUG_REPORT_EMAIL, "RoadWatch Bug Report"),
				"RoadWatch Automatic Crash Report", messageBody.toString());

		logger.info("Crash report email sent");
	}

	private String getProperty(JsonObject jsonObject, String propertyName)
	{
		return jsonObject.has(propertyName) ? jsonObject.get(propertyName).getAsString() : "[property missing]";
	}

	@Override
	protected boolean requiresClientAuthorization()
	{
		return false;
	}
}