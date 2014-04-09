package com.roadwatch.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that unregisters a device, whose registration id is identified by
 * {@link #PARAMETER_REG_ID}.
 * <p>
 * The client app should call this servlet everytime it receives a
 * {@code com.google.android.c2dm.intent.REGISTRATION} with an
 * {@code unregistered} extra.
 */
@SuppressWarnings("serial")
public class UnregisterServlet extends BaseServlet
{
	private static final String PARAMETER_REG_ID = "regId";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String regId = getParameter(req, PARAMETER_REG_ID);
		//DatastoreAPI.unregister(regId);
		setSuccess(resp, null);
	}

}
