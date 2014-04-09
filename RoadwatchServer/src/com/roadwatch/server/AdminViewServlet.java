package com.roadwatch.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roadwatch.server.datastore.ReportsAPI;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.Report;
import com.roadwatch.server.model.User;
import com.roadwatch.server.model.User.UserType;
import com.roadwatch.server.utils.Utils;

/**
 * Servlet that adds display number of devices and button to send a message.
 * <p>
 * This servlet is used just by the browser (i.e., not device) and contains the
 * main page of the demo app.
 */
@SuppressWarnings("serial")
public class AdminViewServlet extends BaseServlet
{
	private static final String ATTRIBUTE_STATUS = "status";

	/**
	 * Displays the existing messages and offer the option to send a new one.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();

		//ServletContext sc = getServletContext();  
		//String filename = sc.getRealPath("rw_splash_logo.png"); 

		out.print("<html>");
		out.print("<head>");
		out.print("<meta http-equiv='Content-Type' content='text/html;charset=UTF-8'/>");
		out.print("<title>RoadWatch App (Alpha)</title>");
		out.print("</head>");
		out.print("<body>");
		out.print("<center>");
		out.print("<h2>Welcome to RoadWatch" + (Utils.isDebugServer() ? " (Dev Server)" : "") + "</h2>");
		out.print("<img src='../../rw_launcher.png' alt='RoadWatch Logo'/>");

		String status = (String) req.getAttribute(ATTRIBUTE_STATUS);
		if (status != null)
		{
			out.print(status);
		}

		// Don't show TRACKED users (only REGULAR or SHARED)
		List<User> allUsers = UsersAPI.getUsers();
		List<User> users = new ArrayList<>();
		for (User user : allUsers)
		{
			if (!UserType.TRACKED.equals(user.getType()))
				users.add(user);
		}

		int numberOfUsers = users.size();
		if (numberOfUsers == 0)
		{
			out.print("<h2>No users registered! :( </h2>");
		}
		else
		{
			out.print("<table border='1'>");
			out.print("<tr>");
			out.print("<th></th>");
			out.print("<th>Username</th>");
			out.print("<th>License Plate</th>");
			out.print("<th>RoadWatch Version</th>");
			out.print("<th>Android Version</th>");
			out.print("<th>Reports Sent</th>");
			out.print("<th>Last Report At</th>");
			out.print("<th>Cars Tracked</th>");
			out.print("</tr>");

			SimpleDateFormat dateAndTimeFormat = new SimpleDateFormat("dd/MM/yy HH:mm");
			int reportsCounter = 0;
			for (int i = 0; i < numberOfUsers; i++)
			{
				User user = users.get(i);
				String licensePlate = users.get(i).getLicensePlate();
				List<Report> reports = ReportsAPI.findReportsByReporter(licensePlate);
				out.print("<tr>");
				out.print("<td>" + (i + 1) + ".</td>");
				out.print("<td>" + user.getUsername() + "</td>");
				out.print("<td>" + licensePlate + "</td>");
				out.print("<td>" + user.getAppVersion() + "</td>");
				out.print("<td>" + user.getAndroidVersion() + "</td>");
				out.print("<td>" + reports.size() + "</td>");
				String lastReportDate = !reports.isEmpty() ? dateAndTimeFormat.format(new Date(reports.get(0).getReportTime())) : "Not yet";
				out.print("<td>" + lastReportDate + "</td>");
				out.print("<td>" + user.getTrackedLicensePlates().size() + "</td>");
				out.print("</tr>");

				reportsCounter += reports.size();
			}
			out.print("</table><br>");

			out.print("<h2>" + numberOfUsers + " users registered, " + reportsCounter + " reports sent</h2>");

			out.print("<form name='form' method='POST' action='sendAll'>");
			out.print("<input type='submit' value='Send Message' />");
			out.print("</form>");
		}
		out.print("</body></html>");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected boolean requiresClientAuthorization()
	{
		return false;
	}
}