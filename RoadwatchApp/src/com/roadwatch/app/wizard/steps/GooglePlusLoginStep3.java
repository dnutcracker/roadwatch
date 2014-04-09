package com.roadwatch.app.wizard.steps;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.PlusClient;
import com.roadwatch.app.R;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.Utils;

public class GooglePlusLoginStep3 extends WizardStep implements OnClickListener, ConnectionCallbacks, OnConnectionFailedListener
{
	// Logging tag for the activity
	private static final String TAG = GooglePlusLoginStep3.class.getSimpleName();

	public static final int DIALOG_GET_GOOGLE_PLAY_SERVICES = 1;

	private static final int REQUEST_CODE_SIGN_IN = 1;
	public static final int REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES = 2;

	private final TextWatcher registerTextWatcher = new TextWatcher()
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			// No-op			
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
			// No-op			
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			updateStep();
		}
	};

	private EditText usernameField;
	private EditText passwordField;
	private EditText confirmPasswordField;
	private SignInButton gplusButton;
	private TextView errorField;
	private ProgressBar progressBar;
	@ContextVariable
	private String licensePlate;
	@ContextVariable
	private String username;
	@ContextVariable
	private String email;

	private PlusClient mPlusClient;
	private ConnectionResult mConnectionResult;

	//You must have an empty constructor for every step
	public GooglePlusLoginStep3()
	{
	}

	//Set your layout here
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.login_wizard_step3, container, false);

		//usernameField = (EditText) v.findViewById(R.id.username_text);
		passwordField = (EditText) v.findViewById(R.id.password_text);
		//confirmPasswordField = (EditText) v.findViewById(R.id.confirm_password_text);
		errorField = (TextView) v.findViewById(R.id.login_error_text);
		progressBar = (ProgressBar) v.findViewById(R.id.login_progress);
		//gplusButton = (SignInButton) v.findViewById(R.id.gplus_sign_in_button);
		gplusButton.setOnClickListener(this);

		mPlusClient = new PlusClient.Builder(getActivity(), this, this)/*.setActions("http://schemas.google.com/AddActivity")*/.build();

		return v;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// Initialize listeners here since doing it in onCreate() causes a recursive exception
		usernameField.addTextChangedListener(registerTextWatcher);
		passwordField.addTextChangedListener(registerTextWatcher);
		confirmPasswordField.addTextChangedListener(registerTextWatcher);

		if (!mPlusClient.isConnected())
			mPlusClient.connect();

		// PENDING: Super patch!
		//((LoginWizardActivity)getActivity()).setFinalStep(this);
	}

	@Override
	public void onStop()
	{
		super.onStop();

		mPlusClient.disconnect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result)
	{
		mConnectionResult = result;
	}

	@Override
	public void onConnected(Bundle connectionHint)
	{
		username = mPlusClient.getCurrentPerson() != null ? mPlusClient.getCurrentPerson().getDisplayName() : "Unknown person";
		email = mPlusClient.getAccountName();
		gplusButton.setEnabled(false);
		new RegisterUserTask(getActivity()).execute();
	}

	@Override
	public void onDisconnected()
	{
		mPlusClient.connect();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v)
	{
		int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
		if (available != ConnectionResult.SUCCESS)
		{
			getActivity().showDialog(DIALOG_GET_GOOGLE_PLAY_SERVICES);
			return;
		}

		try
		{
			//mSignInStatus.setText(getString(R.string.signing_in_status));
			mConnectionResult.startResolutionForResult(getActivity(), REQUEST_CODE_SIGN_IN);
		}
		catch (IntentSender.SendIntentException e)
		{
			// Fetch a new result to start.
			mPlusClient.connect();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_CODE_SIGN_IN || requestCode == REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES)
		{
			if (resultCode == Activity.RESULT_OK && !mPlusClient.isConnected() && !mPlusClient.isConnecting())
			{
				// This time, connect should succeed.
				mPlusClient.connect();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void updateStep()
	{
		boolean usernameValid = usernameField.getText().toString().length() > 1;
		boolean passwordValid = passwordField.getText().toString().length() > 3;
		boolean confirmPasswordValid = confirmPasswordField.getText().toString().equals(passwordField.getText().toString());

		notifyCompleted(usernameValid && passwordValid && confirmPasswordValid);
	}

	/**
	 * Called whenever the wizard proceeds to the next step or goes back to the previous step
	 */
	@Override
	public void onExit(int exitCode)
	{
		switch (exitCode)
		{
		case WizardStep.EXIT_NEXT:
			// Register on server
			new RegisterUserTask(getActivity()).execute();
			break;
		case WizardStep.EXIT_PREVIOUS:
			//Do nothing...
			break;
		}
	}

	/**
	 * An AsyncTask that registers the new user with the server.
	 */
	private class RegisterUserTask extends AsyncTask<Void, Void, JSONObject>
	{
		private Context context;

		public RegisterUserTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			notifyCompleted(false);
			errorField.setText("");
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected JSONObject doInBackground(Void... params)
		{
			ServerAPI dbManager = new ServerAPI(context);
			return dbManager.registerUser(licensePlate, username, email, "XXXXXXX");
		}

		@Override
		protected void onPostExecute(JSONObject jsonUser)
		{
			if (!ServerErrorCodes.hasError(jsonUser))
			{
				Utils.storeLoginInformation(context, jsonUser);
				Log.i(TAG, "User registered successfully");
				// Close Wizard
				getActivity().setResult(Activity.RESULT_OK);
				getActivity().finish();
			}
			else
			{
				// Notify about the error
				String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonUser);
				Log.e(TAG, "Failed to register user : " + errorMessage);

				errorField.setText(errorMessage);
			}

			// Remove the register progress indicator
			progressBar.setVisibility(View.INVISIBLE);

			// Re-enable the button to allow re-sending to server in case of an error
			notifyCompleted(true);
		}
	}
}