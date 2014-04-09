package com.roadwatch.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slim3.datastore.S3QueryResultList;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.roadwatch.server.datastore.DatastoreConstants;
import com.roadwatch.server.utils.Utils;

/**
 * Base class for all servlets
 */
public abstract class BaseServlet extends HttpServlet
{
	protected final Logger logger = Logger.getLogger(getClass().getName());

	protected String getParameter(HttpServletRequest req, String parameter) throws ServletException
	{
		String value = req.getParameter(parameter);
		if (isEmptyOrNull(value))
			throw new ServletException("Parameter " + parameter + " not found");

		return value.trim();
	}
	
	private boolean isAuthorized(String apiKey)
	{
		if(!requiresClientAuthorization() || Utils.isLocalServer()) 
			return true;
		
		return apiKey.equals(ApiKeyInitializer.API_KEY);
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// Authenticate client
		String apiKey = req.getHeader("ClientAuth");
		if(isAuthorized(apiKey))
		{
			// Run the servlet code
			super.service(req, resp);
		}
		else
		{
			// Unauthorized access
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			PrintWriter out = resp.getWriter();			
			out.print("<html>");
			out.print("<head>");
			out.print("<meta http-equiv='Content-Type' content='text/html;charset=UTF-8'/>");
			out.print("<title>Forbidden Access</title>");
			out.print("</head>");
			out.print("<body>");
			out.print("<center>");
			out.print("<h2>Forbidden Access</h2>");
			out.print("</body>");
			out.print("</html>");
			
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			
			logger.log(Level.WARNING, "Unauthorized Access");			
		}
	}
	
	protected boolean requiresClientAuthorization()
	{
		return true;
	}
	
	protected String getParameter(HttpServletRequest req, String parameter, String defaultValue)
	{
		String value = req.getParameter(parameter);
		if (isEmptyOrNull(value))
			value = defaultValue;
			
		return value.trim();
	}
	
	protected boolean isEmptyOrNull(String value)
	{
		return value == null || value.trim().length() == 0;
	}

	@SuppressWarnings("rawtypes")
	protected void setSuccess(HttpServletResponse resp, Object objectToReturn) throws JsonIOException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setStatus(HttpServletResponse.SC_OK);
		
		new Gson().toJson(objectToReturn, resp.getWriter());
		
		// When returning a result list, add the cursor information to returned results
		if(objectToReturn instanceof S3QueryResultList)
		{
			S3QueryResultList s3QueryResultList = (S3QueryResultList) objectToReturn;
			JsonObject jsonCursor = new JsonObject();
			if(s3QueryResultList.hasNext())
				jsonCursor.addProperty(DatastoreConstants.QUERY_KEY_ENCODED_CURSOR, s3QueryResultList.getEncodedCursor());
			new Gson().toJson(jsonCursor, resp.getWriter());
		}
	}
	
	protected void setFailure(HttpServletResponse resp,AppServerOperationException e) throws JsonIOException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		// We use SC_SERVICE_UNAVAILABLE(503) to indicate client should retry when server encountered too much contention (overloaded)
		if(e.getCause() instanceof ConcurrentModificationException)
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		else
			resp.setStatus(HttpServletResponse.SC_OK);
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("error", e.getMessage());	
		jsonObject.addProperty("errorCode", Integer.valueOf(e.getServerErrorCode()));
		
		if(e.getCause()!=null)
			logger.log(e.getSeverityLevel(), e.getMessage() + " Caused By " + e.getCause());
		else
			logger.log(e.getSeverityLevel(), e.getMessage());
		
		new Gson().toJson(jsonObject, resp.getWriter());
	}
	
	/**
	 * Write given Java object as JSON to the output of the current response.
	 * @param object Any Java Object to be written as JSON to the output of the current response. 
	 * @throws IOException If something fails at IO level.
	 */
	protected void writeJson(HttpServletResponse response, Object object) throws IOException
	{
		String json = new Gson().toJson(object);
		response.getWriter().write(json);
	}
	
	protected JsonObject getJSONObject(HttpServletRequest req)
	{
		try
		{
			return (JsonObject) new JsonParser().parse(req.getReader());
		}
		catch (JsonIOException | JsonSyntaxException | IOException e)
		{
			e.printStackTrace();
			return null;
		}		
	}
}