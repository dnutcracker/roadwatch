package com.roadwatch.server.report;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slim3.datastore.S3QueryResultList;

import com.google.gson.JsonObject;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.DatastoreConstants;
import com.roadwatch.server.datastore.ReportsAPI;
import com.roadwatch.server.model.Report;

/**
 * Get reports from the datastore.
 */
@SuppressWarnings("serial")
public class GetReportsServlet extends BaseServlet
{
	public static final String REPORT_KEY_REPORTED_PLATE = "licensePlate";
	public static final String REPORT_KEY_REPORTED_BY = "reportedBy";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonObject = getJSONObject(req);
		String cursor = jsonObject.has(DatastoreConstants.QUERY_KEY_CURSOR) ? jsonObject.get(DatastoreConstants.QUERY_KEY_CURSOR).getAsString() : "";
		int queryPageSize = jsonObject.has(DatastoreConstants.QUERY_KEY_PAGE_SIZE) ? jsonObject.get(DatastoreConstants.QUERY_KEY_PAGE_SIZE).getAsInt() : DatastoreConstants.DEFAULT_QUERY_PAGE_SIZE;

		if (jsonObject.has(REPORT_KEY_REPORTED_PLATE))
		{
			String licensePlate = jsonObject.get(REPORT_KEY_REPORTED_PLATE).getAsString();
			logger.info("Searching for reports on license plate " + licensePlate);
			S3QueryResultList<Report> reportsByLicensePlate = ReportsAPI.findReportsByLicensePlate(licensePlate, cursor, queryPageSize);
			logger.info("Returning reports by cursor : " + cursor);
			setSuccess(resp, reportsByLicensePlate);
		}
		else
		{
			String licensePlate = jsonObject.get(REPORT_KEY_REPORTED_BY).getAsString();
			long fromTime = jsonObject.has(DatastoreConstants.QUERY_KEY_FROM_TIME) ? jsonObject.get(DatastoreConstants.QUERY_KEY_FROM_TIME).getAsLong() : 0;
			long toTime = jsonObject.has(DatastoreConstants.QUERY_KEY_TO_TIME) ? jsonObject.get(DatastoreConstants.QUERY_KEY_TO_TIME).getAsLong() : System.currentTimeMillis();
			String timeFrameDescription = fromTime != 0 ? " (last 24 hours)" : "";
			logger.info("Searching for reports sent by license plate " + licensePlate + timeFrameDescription);
			S3QueryResultList<Report> reportsByLicensePlate = ReportsAPI.findReportsByReporter(licensePlate, fromTime, toTime, cursor, queryPageSize);
			logger.info("Returning reports by cursor : " + cursor);
			setSuccess(resp, reportsByLicensePlate);
		}
	}
}