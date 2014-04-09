package com.roadwatch.server.model;

import java.util.Date;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;

@Model
public class Report
{
	@Attribute(primaryKey = true)
	private Key key;

	private String licensePlate;
	private double latitude;
	private double longitude;
	private int reportCode;
	private long reportTime;
	private Date reportTimeForDisplay;
	private List<String> reportedBy;
	
	public String getUniqueStringKey()
	{
		return generateUniqueStringKey(reportedBy.get(0), reportTime);
	}
	
	public String getSyncKey()
	{
		return licensePlate + "&" + reportCode;
	}
	
	public static String generateUniqueStringKey(String reportedBy, long reportTime)
	{
		return reportedBy + "@" + reportTime;
	}

	public Key getKey()
	{
		return key;
	}

	public void setKey(Key key)
	{
		this.key = key;
	}

	public String getLicensePlate()
	{
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate)
	{
		this.licensePlate = licensePlate;
	}
	
	public double getLatitude()
	{
		return latitude;
	}

	public void setLatitude(double latitude)
	{
		this.latitude = latitude;
	}	

	public double getLongitude()
	{
		return longitude;
	}

	public void setLongitude(double longitude)
	{
		this.longitude = longitude;
	}

	public int getReportCode()
	{
		return reportCode;
	}

	public void setReportCode(int reportCode)
	{
		this.reportCode = reportCode;
	}

	public long getReportTime()
	{
		return reportTime;
	}

	public void setReportTimeForDisplay(Date reportDate)
	{
		this.reportTimeForDisplay = reportDate;
	}

	public Date getReportTimeForDisplay()
	{
		return reportTimeForDisplay;
	}

	public void setReportTime(long reportTime)
	{
		this.reportTime = reportTime;
		this.reportTimeForDisplay = new Date(reportTime);
	}

	public List<String> getReportedBy()
	{
		return reportedBy;
	}

	public void setReportedBy(List<String> reportedBy)
	{
		this.reportedBy = reportedBy;
	}
	
	@Override
	public String toString()
	{
		return "[" + reportedBy.get(0) + "] reported [" + licensePlate + "] with reportCode=" + reportCode + " on " + reportTimeForDisplay; 
	}
}