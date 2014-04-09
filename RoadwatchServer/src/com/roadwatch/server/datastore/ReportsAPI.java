package com.roadwatch.server.datastore;

import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slim3.datastore.Datastore;
import org.slim3.datastore.EntityNotFoundRuntimeException;
import org.slim3.datastore.S3QueryResultList;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.meta.ReportMeta;
import com.roadwatch.server.model.Report;
import com.roadwatch.server.model.User;
import com.roadwatch.server.utils.GCMUtils;
import com.roadwatch.server.utils.Utils;

public class ReportsAPI
{
	private static final Logger logger = Logger.getLogger(ReportsAPI.class.getName());

	private static final Key REPORTS_ANCESTOR_KEY = Datastore.createKey("ReportsData", "ReportsAncestorKey");

	public static S3QueryResultList<Report> findReportsByLicensePlate(String licensePlate)
	{
		return findReportsByLicensePlate(licensePlate, "", 1000);
	}

	/**
	 * 
	 * @param licensePlate
	 * @return a List of reports on the specified license plate sorted from new to old
	 */
	public static S3QueryResultList<Report> findReportsByLicensePlate(String licensePlate, String cursor, int pageSize)
	{
		ReportMeta reportMeta = ReportMeta.get();

		// Return pages according to provided cursor		
		S3QueryResultList<Report> results = cursor.isEmpty() ? Datastore.query(reportMeta).filter(reportMeta.licensePlate.equal(licensePlate)).sort(reportMeta.reportTime.desc).limit(pageSize)
				.asQueryResultList() : Datastore.query(reportMeta).encodedStartCursor(cursor).filter(reportMeta.licensePlate.equal(licensePlate)).sort(reportMeta.reportTime.desc).limit(pageSize)
				.asQueryResultList();

		return results;
	}

	public static S3QueryResultList<Report> findReportsByReporter(String licensePlate)
	{
		return findReportsByReporter(licensePlate, 0, System.currentTimeMillis(), "", 1000);
	}

	/**
	 * 
	 * @param licensePlate
	 * @return a List of reports reported by the specified license plate sorted from new to old
	 */
	public static S3QueryResultList<Report> findReportsByReporter(String licensePlate, long fromTime, long toTime, String cursor, int pageSize)
	{
		ReportMeta reportMeta = ReportMeta.get();

		// Return pages according to provided cursor		
		S3QueryResultList<Report> results = cursor.isEmpty() ? Datastore.query(reportMeta)
				.filter(reportMeta.reportedBy.equal(licensePlate), reportMeta.reportTime.greaterThanOrEqual(Long.valueOf(fromTime)), reportMeta.reportTime.lessThanOrEqual(Long.valueOf(toTime)))
				.sort(reportMeta.reportTime.desc).limit(pageSize).asQueryResultList() : Datastore.query(reportMeta).encodedStartCursor(cursor).filter(reportMeta.reportedBy.equal(licensePlate))
				.sort(reportMeta.reportTime.desc).limit(pageSize).asQueryResultList();

		return results;
	}

	public static Report findReport(Report report)
	{
		// We can safely assume that we have only one reporter since client will not allow updating reports that have more than 1 reporter
		String reportedBy = report.getReportedBy().get(0);
		long reportTime = report.getReportTime();
		Key reportKey = Datastore.createKey(REPORTS_ANCESTOR_KEY, Report.class, Report.generateUniqueStringKey(reportedBy, reportTime));
		try
		{
			return Datastore.get(Report.class, reportKey);
		}
		catch (EntityNotFoundRuntimeException e)
		{
			return null;
		}
	}

	/**
	 *
	 * @param newReport
	 * @return a List of reports on the specified license with the specified report code and with 30 seconds distance
	 */
	public static Report findSimilarReport(Transaction txn, Report newReport)
	{
		final int MAX_TIME_DIFF_TO_CONSIDER_AS_SAME_REPORT = 45 * 1000;
		ReportMeta reportMeta = ReportMeta.get();
		Report sameReport = Datastore
				.query(txn, reportMeta, REPORTS_ANCESTOR_KEY)
				.filter(reportMeta.licensePlate.equal(newReport.getLicensePlate()), reportMeta.reportCode.equal(Integer.valueOf(newReport.getReportCode())),
						reportMeta.reportTime.lessThanOrEqual(Long.valueOf(newReport.getReportTime() + MAX_TIME_DIFF_TO_CONSIDER_AS_SAME_REPORT)),
						reportMeta.reportTime.greaterThanOrEqual(Long.valueOf(newReport.getReportTime() - MAX_TIME_DIFF_TO_CONSIDER_AS_SAME_REPORT))).asSingle();

		return sameReport;
	}

	/**
	 * 
	 * @param newReport
	 * @return true if this user tried to report the same car within less than 22-hours.
	 */
	public static boolean hasAlreadyReportedThisCarRecently(Transaction txn, Report newReport)
	{
		final int MIN_TIME_ALLOWED_BETWEEN_REPORTS_ON_THE_SAME_CAR = 22 * 60 * 60 * 1000;
		long mostRecentTimeAllowed = newReport.getReportTime() - MIN_TIME_ALLOWED_BETWEEN_REPORTS_ON_THE_SAME_CAR;
		ReportMeta reportMeta = ReportMeta.get();
		Report recentReport = Datastore
				.query(txn, reportMeta, REPORTS_ANCESTOR_KEY)
				.filter(reportMeta.reportedBy.equal(newReport.getReportedBy().get(0)), reportMeta.licensePlate.equal(newReport.getLicensePlate()),
						reportMeta.reportTime.greaterThanOrEqual(Long.valueOf(mostRecentTimeAllowed))).asSingle();

		return recentReport != null;
	}

	//	/**
	//	 * Check if multiple reports with same license plate and report code arrived.
	//	 * If so, allow only adding them one by one since we want to merge these reports together into
	//	 * a single report with multiple reporters.
	//	 * @param report
	//	 */
	//	private static void syncReportIfNeeded(Report report)
	//	{	    	
	//	    String syncKey = report.getSyncKey();
	//
	//	    // If we already have someone using this sync key - wait for it to be cleared
	//	    while(syncCache.increment(syncKey, 1l, Long.valueOf(0l))!=Long.valueOf(1))
	//	    {
	//	    	try
	//			{
	//	    		logger.info("Sync needed on syncKey : " + syncKey + ". Sleeping for 100ms");
	//				Thread.sleep(100);
	//			}
	//			catch (InterruptedException ignore)
	//			{
	//				// No-op
	//			}
	//	    }
	//	}

	/**
	 * Adds a new report to the datastore.<BR>
	 * If a report with the same reported license plate and description code was reported with 30 seconds of this report - it will be merged.(i.e. the reportedBy data will be added
	 * to the existing report and this report will be considered to be seen by at least 2 users)
	 * 
	 * PENDING:
	 * 1. Notify user if the report was added or not and why.
	 * 2. Use Tasks
	 * 
	 * @param newReport
	 * @return
	 * @throws AppServerOperationException 
	 */
	public static void addReport(Report newReport) throws AppServerOperationException
	{
		logger.info(newReport.getReportedBy() + " has reported [" + newReport.getLicensePlate() + "]");
		boolean newWitness = false;

		Transaction txn = Datastore.beginTransaction();

		Key reportKey = Datastore.createKey(REPORTS_ANCESTOR_KEY, Report.class, newReport.getUniqueStringKey());
		newReport.setKey(reportKey);
		try
		{
			if (!Utils.isDebugServer() && hasAlreadyReportedThisCarRecently(txn, newReport))
				throw new AppServerOperationException(12, Level.WARNING, "User[" + newReport.getReportedBy().get(0) + "] is trying to report the same car within less than 22 hours - ignoring report");

			// Check if we already have this report in the datastore (reported by other watchers)
			Report similarReport = findSimilarReport(txn, newReport);
			if (similarReport == null)
			{
				// This is a new report
				Datastore.put(txn, newReport);
			}
			else
			{
				newWitness = true;
				// New reports always contain a single reporter
				String reportedByLicensePlate = newReport.getReportedBy().get(0);
				// Prevent adding duplicate witnesses (may happen only in debug mode)
				if (!similarReport.getReportedBy().contains(reportedByLicensePlate))
				{
					// Add new witness
					similarReport.getReportedBy().add(reportedByLicensePlate);

					// Update report
					Datastore.put(txn, similarReport);
				}

				logger.info("Found similar report - Added new witness license plate : " + reportedByLicensePlate);
			}

			txn.commit();

			if (newWitness)
			{
				// Notify reporting user by GCM - 'Good work - your report has been also witnessed by other road watchers!'
				User reporter = UsersAPI.findUserByLicensePlate(newReport.getReportedBy().get(0));
				GCMUtils.sendMessageToUser(Utils.createSimpleMessageFromCode(1), reporter);
			}
		}
		catch (IllegalArgumentException | EntityNotFoundRuntimeException e)
		{
			// Failed to add report 
			e.printStackTrace();
			throw new AppServerOperationException(10, "Failed to add report on license plate [" + newReport.getLicensePlate() + "] reported by " + newReport.getReportedBy(), e);
		}
		catch (ConcurrentModificationException e)
		{
			// Caused by too much contention (currently client retries should suffice, if not, consider using shards)
			throw new AppServerOperationException(10, " Too much contention! Client will retry to add report on license plate [" + newReport.getLicensePlate() + "] reported by "
					+ newReport.getReportedBy(), e);
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	public static void updateReport(Report existingReport, Report updatedReport) throws AppServerOperationException
	{
		logger.info("Updating report on " + updatedReport.getLicensePlate());
		Transaction txn = Datastore.beginTransaction();

		updatedReport.setKey(existingReport.getKey());
		try
		{
			// Replace the existing report with the updated report
			Datastore.put(txn, updatedReport);
			txn.commit();
		}
		catch (ConcurrentModificationException e)
		{
			// Caused by too much contention (consider using shards)
			throw new AppServerOperationException(10, "Failed to update report on license plate : " + updatedReport.getLicensePlate() + " reported by " + updatedReport.getReportedBy(), e);
		}
		finally
		{
			if (txn.isActive())
			{
				txn.rollback();
			}
		}
	}

	public static void removeReport(long reportTime, String reportedBy) throws AppServerOperationException
	{
		Key reportKey = Datastore.createKey(REPORTS_ANCESTOR_KEY, Report.class, Report.generateUniqueStringKey(reportedBy, reportTime));
		try
		{
			Transaction txn = Datastore.beginTransaction();

			// Make sure that this report exists in our datastore
			Datastore.get(txn, reportKey);
			// Delete it
			Datastore.delete(txn, reportKey);

			txn.commit();
			
			logger.info("Successfully removed report by key : " + reportKey);
		}
		catch (EntityNotFoundRuntimeException e)
		{
			logger.info("Failed to delete report. Report key not found : " + reportKey);
		}
	}
}
