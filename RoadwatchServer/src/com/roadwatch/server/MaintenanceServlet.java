package com.roadwatch.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet is invoked by a cron process every 48 hours.(see cron.xml)
 * Reserved for future use
 */
public class MaintenanceServlet extends BaseServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		logger.info("Invoked by cron - reserved for future use");
	}
	
	@Override
	protected boolean requiresClientAuthorization()
	{
		return false;
	}
}