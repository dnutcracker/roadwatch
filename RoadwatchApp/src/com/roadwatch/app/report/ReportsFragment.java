package com.roadwatch.app.report;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.login.EditUserAccountActivity;
import com.roadwatch.app.login.VerifyLoginService;
import com.roadwatch.app.settings.SettingsActivity;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.wizards.LoginWizardActivity;

/**
 *  
 */
public class ReportsFragment extends Fragment
{
	// Logging tag
	private static final String ACT_TAG = ReportsFragment.class.getSimpleName();

	private static final int GET_LOGIN_DATA = 1000;
	private static final String DISPLAYED_LOGIN_WIZARD_KEY = "displayed_login_wizard";
	private static final String LOGGED_IN_KEY = "logged_in";

	private boolean displayedLoginWizard;
	private boolean loggedIn;

	private Button unsentReportsButton;

	private TextView signInLink;

	private Animation shakeAnimation;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (container == null)
		{
			// Currently in a layout without a container, so no reason to create our view.
			return null;
		}

		View rootView = inflater.inflate(R.layout.reports_fragment, container, false);

		shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

		unsentReportsButton = (Button) rootView.findViewById(R.id.unsent_reports_button);

		// By default display the sign in link
		signInLink = (Button) rootView.findViewById(R.id.statusTextView);

		// Retrieve previous state if available
		if (savedInstanceState != null && savedInstanceState.containsKey(LOGGED_IN_KEY))
		{
			loggedIn = savedInstanceState.getBoolean(LOGGED_IN_KEY);
			displayedLoginWizard = savedInstanceState.getBoolean(DISPLAYED_LOGIN_WIZARD_KEY);
		}
		else
		{
			String loginToken = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
			// Check if login was successful
			loggedIn = !loginToken.isEmpty();

			// Initiate an explicit background credentials check after login
			verifyLoginInBackground();
		}

		if (!loggedIn && !displayedLoginWizard)
		{
			Log.w(ACT_TAG, "No login token - user is not logged in");

			// Register with GCM and open register activity
			startLoginWizardActivity();

			// Remember that we already showed this once (and the user might have cancelled it)
			displayedLoginWizard = true;
		}

		Log.i(ACT_TAG, "ReportsFragment created");

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.reports_actionbar_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// handle item selection
		switch (item.getItemId())
		{
		case R.id.action_settings:
			Intent intent = new Intent(getActivity(), SettingsActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void verifyLoginInBackground()
	{
		Intent verifyLoginIntent = new Intent(getActivity(), VerifyLoginService.class);
		verifyLoginIntent.putExtra(VerifyLoginService.EXTRA_EXPLICIT_LOGIN, true);
		getActivity().startService(verifyLoginIntent);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		showLoggedInInfo();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if (unsentReportsButton != null)
		{
			outState.putBoolean(LOGGED_IN_KEY, loggedIn);
			outState.putBoolean(DISPLAYED_LOGIN_WIZARD_KEY, displayedLoginWizard);
		}
	}

	public void showLoggedInInfo()
	{
		String loginToken = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_LOGIN_TOKEN);
		if (!loginToken.isEmpty())
		{
			// Show logged-in user information
			String username = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_NAME);
			String licensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

			String signedInMessage = getString(R.string.signed_in_message, username, licensePlate);
			if (Utils.isDebugMode(getActivity()))
				signedInMessage += " [DEBUG MODE]";
			signInLink.setText(signedInMessage);
			// PENDING: Add 'sign in as someone else' listener
			signInLink.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showUserAccount();
				}
			});
		}
		else
		{
			signInLink.setText(R.string.tap_here_to_sign_in);
			//signInLink.setTextColor(Color.parseColor("#21dbd4"));
			signInLink.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startLoginWizardActivity();
				}
			});

			// Remind the user that he's not logged in
			signInLink.startAnimation(shakeAnimation);
		}
	}

	private void showUserAccount()
	{
		Log.i(ACT_TAG, "Start Edit User Account");
		Intent intent = new Intent(getActivity(), EditUserAccountActivity.class);
		startActivity(intent);
	}

	private void startLoginWizardActivity()
	{
		Log.i(ACT_TAG, "Starting Login Wizard");
		Intent intent = new Intent(getActivity(), LoginWizardActivity.class);
		startActivityForResult(intent, GET_LOGIN_DATA);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (requestCode == GET_LOGIN_DATA)
		{
			getActivity();
			if (resultCode == Activity.RESULT_OK)
			{
				Log.d(ACT_TAG, "Result OK from register/login activity");

				loggedIn = true;

				// Cancel 'Need to login' notification if displayed
				NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.cancel(VerifyLoginService.USER_CREDENTIALS_VERIFICATION_FAILED_NOTIFICATION_ID);

				// Initiate an explicit background credentials check after login
				verifyLoginInBackground();
			}
			else
			{
				// Register/Login Activity was cancelled
				Log.d(ACT_TAG, "Register/Login activity was cancelled");
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		int numberOfUnsentReports = JSONUtils.loadJSONObjects(getActivity(), JSONUtils.UNSENT_REPORTS_FILENAME).size();
		if (numberOfUnsentReports > 0)
		{
			unsentReportsButton.setText(getString(R.string.unsent_reports) + " (" + numberOfUnsentReports + ")");
			unsentReportsButton.setEnabled(true);
		}
		else
		{
			unsentReportsButton.setText(getString(R.string.unsent_reports));
			unsentReportsButton.setEnabled(false);
		}
	}
}