package com.roadwatch.server.report;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.ReportsAPI;

/**
 * Adds a new report to the datastore and sends the reported user a notification (if he's registered)
 * PENDING:
 * 1. Separate the serverlt into addReport and a task for the rest of the code.
 * 
 */
@SuppressWarnings("serial")
public class RemoveReportServlet extends BaseServlet
{
	private static final String REPORT_KEY_REPORT_TIME = "reportTime";
	private static final String REPORT_KEY_REPORTED_BY = "reportedBy";
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonObject = getJSONObject(req);
		try
		{
			long reportTime = jsonObject.get(REPORT_KEY_REPORT_TIME).getAsLong();
			JsonArray jsonReportedByArray = jsonObject.get(REPORT_KEY_REPORTED_BY).getAsJsonArray();
			if(jsonReportedByArray.size()==1)
			{				
				ReportsAPI.removeReport(reportTime, jsonReportedByArray.get(0).getAsString());			
				setSuccess(resp, jsonObject);
			}
			else
			{
				// Also blocked by the client
				throw new AppServerOperationException(13, Level.WARNING, "Ignoring remove report request since it was reported by more than 1 reporter");
			}
		}
		catch(AppServerOperationException e)
		{
			setFailure(resp, e);
		}
	}	
}