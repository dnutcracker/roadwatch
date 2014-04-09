package com.roadwatch.server.report;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.android.gcm.server.Message;
import com.google.gson.Gson;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.ReportsAPI;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.Report;
import com.roadwatch.server.model.User;
import com.roadwatch.server.utils.GCMUtils;
import com.roadwatch.server.utils.Utils;

/**
 * Adds a new report to the datastore and sends the reported user a notification (if he's registered)
 * PENDING:
 * 1. Separate the servlet into addReport and a task for the rest of the code.
 * 
 */
public class AddReportServlet extends BaseServlet
{
	private static final boolean SEND_MAIL_ON_NEW_REPORT = true;

	public static final String REPORT_NOTIFICATION_KEY = "gcmReportNotification";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Report report = new Gson().fromJson(req.getReader(), Report.class);
		try
		{
			Report existingReport = ReportsAPI.findReport(report);
			if (existingReport != null)
			{
				// This exact report already exists - it means the user is updating an existing report
				ReportsAPI.updateReport(existingReport, report);
			}
			else
			{
				// Add a new report
				ReportsAPI.addReport(report);

				// Send the reported user a message
				User reportedUser = UsersAPI.findUserByLicensePlate(report.getLicensePlate(), true);
				if (reportedUser != null)
					GCMUtils.sendMessageToUser(createReportMessage(report), reportedUser);
				else
					logger.info("[" + report.getLicensePlate() + "] is not registered");

				if (!Utils.isDebugServer() && SEND_MAIL_ON_NEW_REPORT)
				{
					User reporterUser = UsersAPI.findUserByLicensePlate(report.getReportedBy().get(0));
					String message = reporterUser + " has reported" + (reportedUser!=null ? " " + reportedUser : " [" + report.getLicensePlate() + "] (unregistered)") + 
							"\nReport Content : " + Utils.getReportCodeDescription(report.getReportCode()) + 							
							"\nReport Location: http://www.latlong.net/c/?lat=" + report.getLatitude() + "&long=" + report.getLongitude();
					Utils.sendEmail(new InternetAddress(Utils.ADMIN_EMAIL, "Roadwatch Server"), new InternetAddress(Utils.ROADWATCH_SUPPORT_EMAIL, "Roadwatch Support"), "New Report!", message);
				}
			}

			setSuccess(resp, report);
		}
		catch (AppServerOperationException e)
		{
			setFailure(resp, e);
		}
	}

	private static Message createReportMessage(Report report)
	{
		try
		{
			// PENDING: Make more efficient by sending only needed report fields
			String reportJsonString = URLEncoder.encode(new Gson().toJson(report), "UTF-8");
			Message message = new Message.Builder().addData(REPORT_NOTIFICATION_KEY, reportJsonString).build();
			return message;
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}