package com.roadwatch.app.login;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;

public class EditUserAccountActivity extends ActionBarActivity
{
	// Logging tag
	private static final String TAG = EditUserAccountActivity.class.getSimpleName();

	private static final String KEY_UPDATE_BUTTON_ENABLED = "key_update_button_enabled";
	private static final String KEY_VERIFY_PASSWORD_FIELD_VISABILITY = "key_verify_password_field_visability";

	private final TextWatcher textWatcher = new TextWatcher()
	{
		private boolean userEdit;
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			userEdit = (count == 1 || before == 1);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
			// No-op			
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			if (!editingStarted && userEdit)
			{
				editingStarted = true;
				updateButton.setEnabled(true);
			}
		}
	};

	private EditText nameField;
	private EditText emailField;
	private EditText passwordField;
	private EditText verifyPasswordField;
	private TextView errorText;
	private ProgressBar progressBar;

	private Button updateButton;
	private Button cancelButton;

	private boolean editedPassword;
	private boolean editingStarted;

	private UpdateUserAccountTask registerUserTask;

	//You must have an empty constructor for every step
	public EditUserAccountActivity()
	{
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.edit_user_account);

		nameField = (EditText) findViewById(R.id.name_text);
		emailField = (EditText) findViewById(R.id.email_text);
		passwordField = (EditText) findViewById(R.id.password_text);
		verifyPasswordField = (EditText) findViewById(R.id.verify_password_text);
		errorText = (TextView) findViewById(R.id.login_error_text);
		progressBar = (ProgressBar) findViewById(R.id.login_progress);
		updateButton = (Button) findViewById(R.id.update_button);
		cancelButton = (Button) findViewById(R.id.cancel_button);

		String username = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_USER_NAME);
		String email = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_USER_EMAIL);
		// May happen for old users
		if (email.isEmpty())
			email = Utils.getUserEmailAddress(this);
		nameField.setText(username);
		nameField.addTextChangedListener(textWatcher);
		emailField.setText(email);
		emailField.addTextChangedListener(textWatcher);
		passwordField.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void afterTextChanged(Editable s)
			{
				if (!editedPassword && s.length()>0)
				{
					// Show verify password field only if user start editing the current password for the first time
					editedPassword = true;
					verifyPasswordField.setVisibility(View.VISIBLE);
					editingStarted = true;
					updateButton.setEnabled(true);
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		});

		updateButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (validateInput())
					new UpdateUserAccountTask(EditUserAccountActivity.this).execute();
			}
		});

		nameField.requestFocus();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(KEY_UPDATE_BUTTON_ENABLED, updateButton.isEnabled());
		outState.putInt(KEY_VERIFY_PASSWORD_FIELD_VISABILITY, verifyPasswordField.getVisibility());

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		// Maintain widgets state between orientation changes
		updateButton.setEnabled(savedInstanceState.getBoolean(KEY_UPDATE_BUTTON_ENABLED));
		verifyPasswordField.setVisibility(savedInstanceState.getInt(KEY_VERIFY_PASSWORD_FIELD_VISABILITY));
	}

	private boolean validateInput()
	{
		errorText.setText("");

		boolean nameValid = nameField.getText().toString().trim().length() > 1;
		boolean emailValid = emailField.getText().toString().trim().length() > 10 && emailField.getText().toString().indexOf("@") != -1;
		boolean passwordValid = !editedPassword || passwordField.getText().toString().trim().length() > 3;
		boolean verifyPasswordValid = !editedPassword || verifyPasswordField.getText().toString().equals(passwordField.getText().toString());
		if (!nameValid)
		{
			errorText.setText(R.string.login_wizard_step3_name_error);
			nameField.requestFocus();
			return false;
		}
		else if (!emailValid)
		{
			errorText.setText(R.string.login_wizard_step3_email_error);
			emailField.requestFocus();
			return false;
		}
		else if (!passwordValid)
		{
			errorText.setText(R.string.login_wizard_step3_password_error);
			passwordField.requestFocus();
			return false;
		}
		else if (!verifyPasswordValid)
		{
			errorText.setText(R.string.login_wizard_step3_verify_password_error);
			verifyPasswordField.requestFocus();
			return false;
		}
		else
		{
			return true;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// Stop task if running
		if (registerUserTask != null && registerUserTask.getStatus() != Status.FINISHED)
			registerUserTask.cancel(true);
	}

	/**
	 * An AsyncTask that register/login the user on the server.
	 */
	private class UpdateUserAccountTask extends AsyncTask<Void, Void, JSONObject>
	{
		private Context context;

		public UpdateUserAccountTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			errorText.setText("");
			progressBar.setVisibility(View.VISIBLE);
			updateButton.setEnabled(false);
			cancelButton.setEnabled(false);
		}

		@Override
		protected JSONObject doInBackground(Void... params)
		{
			ServerAPI server = new ServerAPI(context);

			String username = nameField.getText().toString().trim();
			String email = emailField.getText().toString().trim();
			String password = editedPassword ? passwordField.getText().toString().trim() : "";

			return server.autoLoginUser(username, email, password);
		}

		@Override
		protected void onPostExecute(JSONObject jsonUser)
		{
			if (!ServerErrorCodes.hasError(jsonUser))
			{
				Utils.storeLoginInformation(context, jsonUser);
				Toast.makeText(context, R.string.updated_user_account, Toast.LENGTH_LONG).show();
				setResult(Activity.RESULT_OK);
				finish();
			}
			else
			{
				// Notify about the error
				String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonUser);
				Log.e(TAG, "Failed to update user account  : " + errorMessage);
				errorText.setText(errorMessage);

				updateButton.setEnabled(true);
				cancelButton.setEnabled(true);
			}

			// Remove the register progress indicator
			progressBar.setVisibility(View.INVISIBLE);
		}
	}
}