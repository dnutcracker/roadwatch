package com.roadwatch.app.wizard.steps;

import java.util.ArrayList;
import java.util.List;

import org.codepond.wizardroid.WizardStep;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.login.VerifyLoginService;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;

public class LoginStep3 extends WizardStep
{
	// Logging tag
	private static final String TAG = LoginStep3.class.getSimpleName();

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

	private EditText nameField;
	private EditText emailField;
	private EditText passwordField;
	private TextView errorText;
	private TextView helpText;
	private ProgressBar progressBar;

	private RegisterUserTask registerUserTask;

	//You must have an empty constructor for every step
	public LoginStep3()
	{
	}

	//Set your layout here
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.login_wizard_step3, container, false);

		nameField = (EditText) v.findViewById(R.id.name_text);
		emailField = (EditText) v.findViewById(R.id.email_text);
		passwordField = (EditText) v.findViewById(R.id.password_text);
		errorText = (TextView) v.findViewById(R.id.login_error_text);
		helpText = (TextView) v.findViewById(R.id.wrongCredentialsHelpText);
		progressBar = (ProgressBar) v.findViewById(R.id.login_progress);

		return v;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// Initialize listeners here since doing it in onCreate() causes a recursive exception
		String email = Utils.getUserEmailAddress(getActivity());
		String username = email.split("@")[0];
		nameField.setText(username);
		nameField.addTextChangedListener(registerTextWatcher);
		emailField.setText(email);
		emailField.addTextChangedListener(registerTextWatcher);
		passwordField.addTextChangedListener(registerTextWatcher);
		passwordField.requestFocus();
	}

	private void updateStep()
	{
		errorText.setText("");
		helpText.setVisibility(View.INVISIBLE);
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
			boolean nameValid = nameField.getText().toString().trim().length() > 1;
			boolean emailValid = emailField.getText().toString().trim().length() > 10 && emailField.getText().toString().indexOf("@") != -1;
			boolean passwordValid = passwordField.getText().toString().trim().length() > 3;
			if (!nameValid)
			{
				errorText.setText(R.string.login_wizard_step3_name_error);
				nameField.requestFocus();
			}
			else if (!emailValid)
			{
				errorText.setText(R.string.login_wizard_step3_email_error);
				emailField.requestFocus();
			}
			else if (!passwordValid)
			{
				errorText.setText(R.string.login_wizard_step3_password_error);
				passwordField.requestFocus();
			}
			else
			{
				registerUserTask = new RegisterUserTask(getActivity());
				registerUserTask.execute();
			}
			break;
		case WizardStep.EXIT_PREVIOUS:
			//Do nothing...
			break;
		}
	}

	//	@Override
	//	public void onActivityResult(int requestCode, int resultCode, Intent data)
	//	{
	//		if (requestCode == RegisterWithGoogleAccount.REQUEST_CODE_RECOVER_FROM_AUTH_ERROR) //132073
	//		{
	//			handleAuthorizeResult(resultCode, data);
	//			return;
	//		}
	//		super.onActivityResult(requestCode, resultCode, data);
	//	}
	//
	//	private void handleAuthorizeResult(int resultCode, Intent data)
	//	{
	//		if (data == null)
	//		{
	//			errorField.setText("Unknown error, click the button again");
	//			return;
	//		}
	//		if (resultCode == Activity.RESULT_OK)
	//		{
	//			Log.i(TAG, "Retrying");
	//			new RegisterWithGoogleAccount(this, email).execute();
	//			return;
	//		}
	//		if (resultCode == Activity.RESULT_CANCELED)
	//		{
	//			errorField.setText("User rejected authorization.");
	//			return;
	//		}
	//		errorField.setText("Unknown error, click the button again");
	//	}
	//
	//	/**
	//	 * This example shows how to fetch tokens if you are creating a foreground task/activity and handle
	//	 * auth exceptions.
	//	 */
	//	private class RegisterWithGoogleAccount extends AsyncTask<Void, Void, String>
	//	{
	//		public static final String SCOPE = "audience:server:client_id:386534561197-0vdo3acvot5mkuj73gaadgi2d3vj0d59.apps.googleusercontent.com";//"oauth2:https://www.googleapis.com/auth/userinfo.profile";
	//
	//		public static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
	//		public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
	//
	//		private Fragment fragment;
	//		private String email;
	//		private String token;
	//
	//		public RegisterWithGoogleAccount(Fragment fragment, String email)
	//		{
	//			this.fragment = fragment;
	//			this.email = email;
	//		}
	//
	//		@Override
	//		protected String doInBackground(Void... params)
	//		{
	//			try
	//			{
	//				return fetchToken();
	//			}
	//			catch (IOException ex)
	//			{
	//				Toast.makeText(getActivity(), "Following Error occured, please try again. " + ex.getMessage(), Toast.LENGTH_LONG).show();
	//			}
	////			catch (JSONException e)
	////			{
	////				errorField.setText("Bad response: " + e.getMessage());
	////			}
	//			return null;
	//		}
	//
	////		private JSONObject fetchProfileServer() throws IOException, JSONException
	////		{
	////			String token = fetchToken();
	////			if (token == null)
	////			{
	////				// error has already been handled in fetchToken()
	////				return null;
	////			}
	////			URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + token);
	////			HttpURLConnection con = (HttpURLConnection) url.openConnection();
	////			int sc = con.getResponseCode();
	////			if (sc == 200)
	////			{
	////				return new JSONObject(readResponse(con, false));
	////			}
	////			else if (sc == 401)
	////			{
	////				GoogleAuthUtil.invalidateToken(fragment.getActivity(), token);
	////				runOnUIThread(new Runnable()
	////				{
	////					@Override
	////					public void run()
	////					{
	////						errorField.setText("Server auth error, please try again.");
	////					}
	////				});
	////
	////				Log.i(TAG, "Server auth error: " + readResponse(con, true));
	////				return null;
	////			}
	////			else
	////			{
	////				errorField.setText("Server returned the following error code: " + sc);
	////				return null;
	////			}
	////		}
	////
	////		/**
	////		 * Reads the response from the input stream and returns it as a string.
	////		 */
	////		private String readResponse(HttpURLConnection con, boolean error) throws IOException
	////		{
	////			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	////			InputStream is = error ? con.getErrorStream() : con.getInputStream();
	////
	////			try
	////			{
	////				byte[] data = new byte[2048];
	////				int len = 0;
	////				while ((len = is.read(data, 0, data.length)) >= 0)
	////				{
	////					bos.write(data, 0, len);
	////				}
	////				return new String(bos.toByteArray(), "UTF-8");
	////			}
	////			finally
	////			{
	////				is.close();
	////			}
	////		}
	//
	//		/**
	//		 * Get a authentication token if one is not available. If the error is not recoverable then
	//		 * it displays the error message on parent activity right away.
	//		 */
	//		protected String fetchToken() throws IOException
	//		{
	//			try
	//			{
	//				token = GoogleAuthUtil.getToken(fragment.getActivity(), email, SCOPE);
	//				return token;
	//			}
	//			catch (final GooglePlayServicesAvailabilityException playEx)
	//			{
	//				// GooglePlayServices.apk is either old, disabled, or not present.
	//				fragment.getActivity().runOnUiThread(new Runnable()
	//				{
	//					@Override
	//					public void run()
	//					{
	//						Dialog d = GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(), fragment.getActivity(), REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
	//						d.show();
	//					}
	//				});
	//			}
	//			catch (UserRecoverableAuthException userRecoverableException)
	//			{
	//				// Unable to authenticate, but the user can fix this.
	//				// Forward the user to the appropriate activity.
	//				startActivityForResult(userRecoverableException.getIntent(), REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
	//			}
	//			catch (final GoogleAuthException fatalException)
	//			{
	//				runOnUIThread(new Runnable()
	//				{
	//					@Override
	//					public void run()
	//					{
	//						errorField.setText("Unrecoverable error : " + fatalException.getMessage());
	//					}
	//				});
	//
	//			}
	//			return null;
	//		}
	//
	//		private void runOnUIThread(Runnable runnable)
	//		{
	//			fragment.getActivity().runOnUiThread(runnable);
	//		}
	//
	//		@Override
	//		protected void onPostExecute(String token)
	//		{
	//			if (token != null)
	//			{
	//				System.out.println("Token : " + token);
	//				// PENDING: Use the token as password
	//			}
	//
	//		}
	//	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		// Stop task if running
		if (registerUserTask != null && registerUserTask.getStatus() != Status.FINISHED)
			registerUserTask.cancel(true);
	}

	/**
	 * An AsyncTask that register/login the user on the server.
	 */
	private class RegisterUserTask extends AsyncTask<Void, Void, JSONObject>
	{
		private Context context;
		private boolean existingUser;

		public RegisterUserTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			notifyCompleted(false);
			errorText.setText("");
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected JSONObject doInBackground(Void... params)
		{
			ServerAPI server = new ServerAPI(context);			
			// Patching over the wizard framework 'value injection' which sometimes doesn't work			
			String licensePlate = PreferencesUtils.getString(context, LoginStep2.LOGIN_WIZARD_LICENSE_PLATE_PROPERTY);
			List<JSONObject> jsonUsers = server.getUserByLicensePlate(licensePlate);
			if (jsonUsers.isEmpty())
				existingUser = false;
			else if (!ServerErrorCodes.hasError(jsonUsers.get(0)))
				existingUser = true;
			else
				// Return the error (encapsulated inside the json object)
				return jsonUsers.get(0);

			String username = nameField.getText().toString().trim();
			String email = emailField.getText().toString().trim();
			String password = passwordField.getText().toString().trim();

			if (existingUser)
			{
				return server.loginUser(licensePlate, password);
			}
			else
			{
				return server.registerUser(licensePlate, email, username, password);
			}
		}

		@Override
		protected void onPostExecute(JSONObject jsonUser)
		{
			if (!ServerErrorCodes.hasError(jsonUser))
			{
				Utils.storeLoginInformation(context, jsonUser);
				if (existingUser)
				{
					Log.i(TAG, "User signed in successfully");

					manageLoginFromMultipleDevices(jsonUser);
				}
				else
				{
					Log.i(TAG, "New user registered successfully");
					Activity activity = getActivity();
					if (activity != null)
					{
						// Close Wizard
						activity.setResult(Activity.RESULT_OK);
						activity.finish();
					}
				}
				
				// Clear the temporary value used between the wizard steps
				PreferencesUtils.remove(context, LoginStep2.LOGIN_WIZARD_LICENSE_PLATE_PROPERTY);
			}
			else
			{
				// Notify about the error
				String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonUser);
				Log.e(TAG, "Sign in failed : " + errorMessage);

				// Check if this is a bad credentials error
				if (ServerErrorCodes.getErrorCode(jsonUser) == 1)
					helpText.setVisibility(View.VISIBLE);

				errorText.setText(errorMessage);
			}

			// Remove the register progress indicator
			progressBar.setVisibility(View.INVISIBLE);

			// Re-enable the button to allow re-sending to server in case of an error
			notifyCompleted(true);
		}

		private void manageLoginFromMultipleDevices(JSONObject jsonUser)
		{
			try
			{
				JSONArray regIDsJsonArray = jsonUser.getJSONArray(JSONKeys.USER_KEY_OWN_GCM_IDS);
				final List<String> gcmRegIDList = new ArrayList<String>();
				for (int i = 0; i < regIDsJsonArray.length(); i++)
					gcmRegIDList.add(regIDsJsonArray.getString(i));

				final String gcmRegID = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_GCM_REGISTRATION_ID);

				if (!gcmRegID.isEmpty() && !gcmRegIDList.contains(gcmRegID))
				{
					// User has logged in from a new device
					Log.i(TAG, "Detected login from a new device");

					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					String exitMessage = "Do you wish to recieve notifications to this device?";
					builder.setTitle("New device detected").setIcon(R.drawable.rw_launcher).setMessage(exitMessage);
					builder.setNegativeButton("No", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							// No need to do anything - just close the wizard
							getActivity().setResult(Activity.RESULT_OK);
							getActivity().finish();
						}
					});
					builder.setPositiveButton("Yes, exclusively", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							// Remove all other registration IDs
							gcmRegIDList.clear();
							gcmRegIDList.add(gcmRegID);
							updateRegistrationIds(gcmRegIDList);
						}
					});
					builder.setNeutralButton("Yes", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							// Add this device
							gcmRegIDList.add(gcmRegID);
							updateRegistrationIds(gcmRegIDList);
						}
					});

					builder.create().show();
				}
				else
				// GCM reg id not retrieved yet or not detected login from a new device
				{
					// Close Wizard
					getActivity().setResult(Activity.RESULT_OK);
					getActivity().finish();
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		/**
		 * Sends the user's updated GCM registration IDs list to the server
		 * @param loginToken 
		 * @param gcmRegIds
		 */
		@SuppressWarnings("unchecked")
		private void updateRegistrationIds(List<String> gcmRegIDList)
		{
			// Update
			new UpdateUserGCMRegistrationIDsTask(context).execute(gcmRegIDList);

			// Clear 'need to re-login' notification (if showing)
			Utils.hideNotification(context, VerifyLoginService.USER_CREDENTIALS_VERIFICATION_FAILED_NOTIFICATION_ID);

			// No need to do anything - just close the wizard
			getActivity().setResult(Activity.RESULT_OK);
			getActivity().finish();
		}

		/**
		 * An AsyncTask that updates the user's GCM registration ids
		 */
		private class UpdateUserGCMRegistrationIDsTask extends AsyncTask<List<String>, Void, JSONObject>
		{
			private Context context;

			public UpdateUserGCMRegistrationIDsTask(Context context)
			{
				this.context = context;
			}

			@Override
			protected JSONObject doInBackground(List<String>... params)
			{
				ServerAPI server = new ServerAPI(context);
				List<String> gcmRegIDList = params[0];
				return server.autoLoginUser(gcmRegIDList);
			}

			@Override
			protected void onPostExecute(JSONObject jsonUser)
			{
				if (!ServerErrorCodes.hasError(jsonUser))
				{
					Log.i(TAG, "User GCM registration IDs updated successfully");
				}
				else
				{
					// Notify about the error
					String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonUser);
					Log.i(TAG, "Failed to update user GCM registration IDs : " + errorMessage);
				}
			}
		}

	}
}