package com.roadwatch.server.login;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.User;

/**
 * Get users according to the specified parameters
 */
@SuppressWarnings("serial")
public class GetUsersServlet extends BaseServlet
{
	private static final String USER_KEY_LICENSE_PLATE = "licensePlate";
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		JsonObject jsonRequest = getJSONObject(req);
		
		if(jsonRequest.has(USER_KEY_LICENSE_PLATE))
		{
			String licensePlate = jsonRequest.get(USER_KEY_LICENSE_PLATE).getAsString();
			User user = UsersAPI.findUserByLicensePlate(licensePlate);
			if(!licensePlate.isEmpty())
			{				
				if(user!=null)
					logger.info("Found " + user);
				else
					logger.info("No registered user found with license plate [" + licensePlate + "]");		
			}
			else
				logger.info("Server wakup call by unregistered user");
				
			setSuccess(resp, user);
		}
	}
}