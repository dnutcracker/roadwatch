package com.roadwatch.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
@SuppressWarnings("serial")
public class SendAllMessagesServlet extends BaseServlet
{
	/**
	 * Processes the request to add a new message.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
	{
//		List<User> users = DatastoreAPI.getUsers();
//		String status;
//		if (users.isEmpty())
//		{
//			status = "Message ignored as there is no device registered!";
//		}
//		else
//		{
//			Queue queue = QueueFactory.getQueue("gcm");
//			// send a multicast message using JSON
//			// must split in chunks of 1000 devices (GCM limit)
//			int total = users.size();
//			List<String> partialDevices = new ArrayList<String>(total);
//			int counter = 0;
//			int tasks = 0;
//			for (User user : users)
//			{
//				counter++;
//				String regId = user.getGcmRegistrationID();					
//				partialDevices.add(regId);
//				int partialSize = partialDevices.size();
//				if (partialSize == GCMUtils.MULTICAST_MAX_SIZE || counter == total)
//				{
//					String multicastKey = DatastoreAPI.createMulticast(partialDevices);
//					logger.fine("Queuing " + partialSize + " devices on multicast " + multicastKey);
//					TaskOptions taskOptions = TaskOptions.Builder.withUrl("/send").param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey).method(Method.POST);
//					queue.add(taskOptions);
//					partialDevices.clear();
//					tasks++;
//				}
//			}
//			status = "Queued tasks to send " + tasks + " multicast messages to " + total + " devices";
//		}
//		req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, status.toString());
//		getServletContext().getRequestDispatcher("/home").forward(req, resp);
	}
}