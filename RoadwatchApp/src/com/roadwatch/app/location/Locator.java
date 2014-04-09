package com.roadwatch.app.location;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

/**
 * Helps get the current location either through updates or by retrieving the last known location.
 *  
 * @author Nati created at : 02:07:38 06/11/2013
 */
public class Locator implements LocationListener
{
	// Logging tag
	private static final String ACT_TAG = Locator.class.getSimpleName();

	public static float MINIMAL_GPS_ACCURACY_REQUIRED_IN_METERS = 15;

	private Context context;
	private boolean passive;
	private LocationManager locationManager;
	private LocationListener gpsLocationListener;
	private LocationListener networkLocationListener;

	private LocatorListener locatorListener;
	private Location currentLocation;
	private float minimalDistanceForUpdate;

	public Locator(Context context)
	{
		this(context, true);
	}

	/**
	 * Utilizes the LocationClient for A-GPS data
	 * 
	 * @param context
	 * @param passive true if we are operating within a navigation app (which requests location updates by itself)
	 */
	public Locator(Context context, boolean passive)
	{
		this.context = context;
		this.passive = passive;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void setMinimalDistanceForUpdate(float smallestDisplacement)
	{
		this.minimalDistanceForUpdate = smallestDisplacement;
	}

	/**
	 * 
	 * @return The best last known location between the GPS or Network provided last known location
	 */
	private Location getBestLastKnownLocation()
	{
		if (passive)
			return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

		Location lastKnownLocationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastKnownLocationByGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		return isBetterLocation(lastKnownLocationByGPS, lastKnownLocationByNetwork) ? lastKnownLocationByGPS : lastKnownLocationByNetwork;
	}

	public Location getLastKnownLocation()
	{
		return getBestLastKnownLocation();
	}

	/**
	 * Invokes the provided <code>LocationListener</code> when we are
	 * within <code>MINIMAL_GPS_ACCUARCY_REQUIRED_IN_METERS</code> of the current location.
	 * @param locatorListener
	 */
	public void connect(LocatorListener locatorListener)
	{
		this.locatorListener = locatorListener;
		this.currentLocation = getBestLastKnownLocation();
		if (currentLocation != null)
		{
			Log.d(ACT_TAG, "Got best last known location from provider  " + currentLocation.getProvider() + " : " + currentLocation);
			locatorListener.onLocationChanged(currentLocation);
		}
		else
			Log.d(ACT_TAG, "Last known location is null");

		if (isGPSProviderEnabled() || isNetworkProviderEnabled())
		{
			requestLocationUpdates();
		}
		else
		// GPS Disabled
		{
			if (context instanceof FragmentActivity)
				buildAlertMessageNoLocationProviders();
		}
	}

	private boolean isGPSProviderEnabled()
	{
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	private boolean isNetworkProviderEnabled()
	{
		return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	private void requestLocationUpdates()
	{
		// If we're working along side the navigation app use the passive provider
		if (passive)
		{
			locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, minimalDistanceForUpdate, this);
		}
		else
		{
			// If not passive, request a single update from both the GPS and Network
			if (isGPSProviderEnabled())
			{
				gpsLocationListener = new ProviderLocationListener();
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
			}
			if (isNetworkProviderEnabled())
			{
				networkLocationListener = new ProviderLocationListener();
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);
			}
		}
	}

	private void buildAlertMessageNoLocationProviders()
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Your GPS and mobile network location seem to be disabled, do you want to enable at least one of them ?").setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(final DialogInterface dialog, final int id)
					{
						context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(final DialogInterface dialog, final int id)
					{
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onLocationChanged(Location location)
	{
		// When using passive provider we use every location update
		if (passive || isBetterLocation(location, currentLocation))// || isLocationByGPS(location))
		{
			if (!passive)
				Log.d(ACT_TAG, "Got a BETTER location update from " + location.getProvider() + " provider : " + location);

			currentLocation = location;
			locatorListener.onLocationChanged(currentLocation);
		}
		else
		{
			Log.d(ACT_TAG, "Location is NOT better from " + location.getProvider() + " provider");
		}
	}

	//	private boolean isLocationByGPS(Location newLocation)
	//	{
	//		return newLocation.getProvider().equals(LocationManager.GPS_PROVIDER);
	//	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		// No-op
	}

	@Override
	public void onProviderEnabled(String provider)
	{
		// No-op		
	}

	@Override
	public void onProviderDisabled(String provider)
	{
		// No-op		
	}

	/**
	 * Disconnects GPS tracker. 
	 * If already disconnected - does nothing.
	 */
	public void disconnect()
	{
		locationManager.removeUpdates(this);
		if (gpsLocationListener != null)
			locationManager.removeUpdates(gpsLocationListener);
		if (networkLocationListener != null)
			locationManager.removeUpdates(networkLocationListener);
	}

	/** Determines whether one Location reading is better than the current Location fix
	  * @param newLocation  The new Location that you want to evaluate
	  * @param currentLocation  The current Location fix, to which you want to compare the new one
	  */
	private static boolean isBetterLocation(Location newLocation, Location currentLocation)
	{
		//final int TWO_MINUTES = 1000 * 60 * 2;
		if (currentLocation == null)
		{
			// A new location is always better than no location
			return true;
		}

		if (newLocation == null)
		{
			// If new location is null - we stick with what we already have
			return false;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		//boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		//boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		//		if (isSignificantlyNewer)
		//		{
		//			return true;
		//			// If the new location is more than two minutes older, it must be worse
		//		}
		//		else if (isSignificantlyOlder)
		//		{
		//			return false;
		//		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), currentLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate)
		{
			return true;
		}
		else if (isNewer && !isLessAccurate)
		{
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
		{
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2)
	{
		if (provider1 == null)
		{
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private class ProviderLocationListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location)
		{
			Locator.this.onLocationChanged(location);
			locationManager.removeUpdates(this);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}

		@Override
		public void onProviderEnabled(String provider)
		{
		}

		@Override
		public void onProviderDisabled(String provider)
		{
		}
	}
}