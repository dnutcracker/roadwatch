package com.roadwatch.server.utils;

import static com.google.appengine.api.utils.SystemProperty.environment;
import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Development;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.appengine.api.datastore.Key;
import com.google.apphosting.api.ApiProxy;

public class Utils
{
	private static final String DEBUG_APP_ID = "dev-road-watch";

	public static final String ADMIN_EMAIL = "dnutcracker@gmail.com";
	public static final String ROADWATCH_BUG_REPORT_EMAIL = "roadwatch.bugreports@gmail.com";
	public static final String ROADWATCH_SUPPORT_EMAIL = "roadwatch.help@gmail.com";

	private static final String SERVER_SIMPLE_MESSAGE_NOTIFICATION_KEY = "gcmServerMessageSimpleMessageNotification";

	/**
	 * 
	 * @return true if we're running on the Roadwatch debug server
	 */
	public static boolean isDebugServer()
	{
		String appId = ApiProxy.getCurrentEnvironment().getAppId();
		return appId.indexOf(DEBUG_APP_ID) != -1;
	}

	public static boolean isProductionKey(Key userKey)
	{
		String appId = userKey.getAppId();
		return appId.indexOf(DEBUG_APP_ID) == -1;
	}

	public static boolean isLocalServer()
	{
		return environment.value() == Development;
	}

	public static void sendEmail(InternetAddress from, InternetAddress to, String subject, String messageBody)
	{
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try
		{
			Message msg = new MimeMessage(session);
			msg.setFrom(from);
			msg.addRecipient(Message.RecipientType.TO, to);
			msg.setSubject(subject);
			msg.setText(messageBody.toString());
			Transport.send(msg);
		}
		catch (AddressException e)
		{
			e.printStackTrace();
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
		}
	}

	public static String getReportCodeDescription(int reportCode)
	{
		switch (reportCode)
		{
		// Danger level 1
		case 5426:
			return "Hazard(1): Entering a busy intersection";
		case 5435:
			return "Hazard(1): Driving on shoulder";
		case 5424:
			return "Hazard(1): Disturbing or delaying traffic";
		case 5457:
			return "Hazard(1): Not keeping right";
		case 5807:
			return "Hazard(1): Littering from vehicle";
			// Danger level 2
		case 5439:
			return "Danger(2): Weaving";
		case 5480:
			return "Danger(2): Illegal U-turn";
		case 5430:
			return "Danger(2): Wrong way on one-way street";
		case 5952:
			return "Danger(2): Driving over a safety zone";
		case 6738:
			return "Danger(2): Illegal turn from wrong lane";
			// Danger level 3
		case 2432:
			return "Threat(3): Running a red light";
		case 6458:
			return "Threat(3): Not stopping for pedestrians";
		case 3477:
			return "Threat(3): Not stopping at a stop sign";
		case 6192:
			return "Threat(3): Failure to yield";
		case 6153:
			return "Threat(3): Overtaking on a solid line";

		default:
			return "Unknown report code : " + reportCode;
		}

	}

	public static com.google.android.gcm.server.Message createSimpleMessageFromCode(int messageCode)
	{
		com.google.android.gcm.server.Message message = new com.google.android.gcm.server.Message.Builder().addData(SERVER_SIMPLE_MESSAGE_NOTIFICATION_KEY, String.valueOf(messageCode)).build();
		return message;
	}
}