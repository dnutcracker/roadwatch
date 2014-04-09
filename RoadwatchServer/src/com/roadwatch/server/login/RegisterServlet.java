package com.roadwatch.server.login;

import java.io.IOException;

import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.roadwatch.server.AppServerOperationException;
import com.roadwatch.server.BaseServlet;
import com.roadwatch.server.datastore.UsersAPI;
import com.roadwatch.server.model.User;
import com.roadwatch.server.utils.Utils;

/**
 * Servlet that registers a new user.
 */
public class RegisterServlet extends BaseServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		User newUser = new Gson().fromJson(req.getReader(), User.class);

		try
		{
			newUser = UsersAPI.registerUser(newUser);			
			
			int numberOfUsers = UsersAPI.getUsers().size();
			// Send email
			Utils.sendEmail(new InternetAddress(Utils.ADMIN_EMAIL, "Roadwatch Server"),
					new InternetAddress(Utils.ROADWATCH_SUPPORT_EMAIL, "Roadwatch Support"),
					"RoadWatcher Number " + numberOfUsers + " Registered!", newUser.toString());
			
			setSuccess(resp, newUser);
		}
		catch (AppServerOperationException e)
		{
			setFailure(resp, e);
		}
	}
}
