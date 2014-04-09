package com.roadwatch.server;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slim3.datastore.Datastore;
import org.slim3.datastore.EntityNotFoundRuntimeException;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.roadwatch.server.utils.Utils;

/**
 * Context initializer that loads the API key from the App Engine datastore.
 */
public class ApiKeyInitializer implements ServletContextListener
{
	static final String ATTRIBUTE_ACCESS_KEY = "apiKey";

	private static final String ENTITY_KIND = "Settings";
	private static final String ENTITY_KEY = "MyKey";
	private static final String ACCESS_KEY_FIELD = "ApiKey";

	// road-watch API key : AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs
	// dev-road-watch API key : AIzaSyAwOkz5V28M3YLNOIDsFPkJRz6cMadA9rc
	private static final String DEV_ROAD_WATCH_API_KEY = "AIzaSyAwOkz5V28M3YLNOIDsFPkJRz6cMadA9rc";
	private static final String PROD_ROAD_WATCH_API_KEY = "AIzaSyCoWTx40Eug0tf8ZneugrrLsK2ghnwrpxs";
	
	public static final String API_KEY = Utils.isDebugServer() ? DEV_ROAD_WATCH_API_KEY : PROD_ROAD_WATCH_API_KEY;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public void contextInitialized(ServletContextEvent event)
	{
		Key key = Datastore.createKey(ENTITY_KIND, ENTITY_KEY);
		
		Entity entity;
		try
		{
			entity = Datastore.get(key);
		}
		catch (EntityNotFoundRuntimeException e)
		{
			entity = new Entity(key);
			// NOTE: it's not possible to change entities in the local server, so
			// it will be necessary to hardcode the API key below if you are running it locally.
			

			entity.setProperty(ACCESS_KEY_FIELD, API_KEY);
			Datastore.put(entity);
			logger.severe("Created fake key. Please go to App Engine admin " + "console, change its value to your API Key (the entity " + "type is '" + ENTITY_KIND
					+ "' and its field to be changed is '" + ACCESS_KEY_FIELD + "'), then restart the server!");
		}
		String accessKey = (String) entity.getProperty(ACCESS_KEY_FIELD);
		event.getServletContext().setAttribute(ATTRIBUTE_ACCESS_KEY, accessKey);
	}

	public void contextDestroyed(ServletContextEvent event)
	{
	}

}
