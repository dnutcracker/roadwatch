package com.roadwatch.app.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.location.ResolveAddressTask;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.widgets.LicensePlateEditText;

/** 
 * @author Nati
 */
public class EditReportActivity extends ActionBarActivity
{
	// Logging tag
	private static final String ACT_TAG = EditReportActivity.class.getSimpleName();

	public enum ReportViewMode
	{
		/** Used when editing unsent reports. */
		EDIT_BEFORE_SENDING,
		/** Used to fix reports that were sent in the last 24 hours. */
		FIX_SENT_REPORT,
		/** Used to view reports send more than 24 hours ago. */
		READ_ONLY;
	}

	public static final String EXTRA_SELECTED_REPORT = "selected_report";
	public static final String EXTRA_REPORT_VIEW_MODE = "report_view_mode";

	// Report details
	private String loggedInLicensePlate;
	private double latitude;
	private double longitude;
	private long reportTime;

	// Handles to UI widgets
	private TextView locationTextView;
	private LicensePlateEditText licensePlateEditText;
	private Button danger1Button;
	private Button danger2Button;
	private Button danger3Button;
	private TextView reportDescriptionText;
	private Button reportButton;
	private ProgressBar progressBar;

	private List<Entry<String, Integer>> level1ReportDescriptions;
	private List<Entry<String, Integer>> level2ReportDescriptions;
	private List<Entry<String, Integer>> level3ReportDescriptions;

	private Map<String, String> reportMap;

	private SendOrRemoveReportTask sendReportTask;

	private ReportViewMode viewMode;
	private Button removeReportButton;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		viewMode = (ReportViewMode) getIntent().getSerializableExtra(EXTRA_REPORT_VIEW_MODE);
		reportMap = (HashMap<String, String>) getIntent().getSerializableExtra(EXTRA_SELECTED_REPORT);

		setContentView(R.layout.report);

		switch (viewMode)
		{
		case EDIT_BEFORE_SENDING:
			setTitle(R.string.title_activity_edit_report);
			break;
		case FIX_SENT_REPORT:
			setTitle(R.string.title_activity_fix_report);
			break;
		case READ_ONLY:
			setTitle(R.string.title_activity_view_report);
			break;
		}

		// Show the Up button in the action bar.
		setupActionBar();

		// Get handles to the UI view objects
		locationTextView = (TextView) findViewById(R.id.location_found_text);

		licensePlateEditText = (LicensePlateEditText) findViewById(R.id.license_plate);
		LicensePlateUtils.configureLicensePlateEditText(licensePlateEditText);
		licensePlateEditText.setFocusable(false);

		((ImageButton) findViewById(R.id.action_button)).setVisibility(View.GONE);

		danger1Button = (Button) findViewById(R.id.danger_button1);
		danger2Button = (Button) findViewById(R.id.danger_button2);
		danger3Button = (Button) findViewById(R.id.danger_button3);

		level1ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_1_report_codes);
		level2ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_2_report_codes);
		level3ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_3_report_codes);

		configureDangerButtons();

		reportDescriptionText = (TextView) findViewById(R.id.report_description);

		reportButton = (Button) findViewById(R.id.report_button);
		reportButton.setText(R.string.send_report_button);
		if (viewMode == ReportViewMode.FIX_SENT_REPORT)
		{
			removeReportButton = (Button) findViewById(R.id.remove_report_button);
			removeReportButton.setVisibility(View.VISIBLE);
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			p.addRule(RelativeLayout.ALIGN_PARENT_LEFT); //android:layout_alignParentLeft="true"
			p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			p.addRule(RelativeLayout.LEFT_OF, removeReportButton.getId()); //android:layout_toLeftOf="@+id/remove_report_button"			
			reportButton.setLayoutParams(p);
		}

		progressBar = (ProgressBar) findViewById(R.id.send_report_progress);

		loggedInLicensePlate = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		setReportData();

		if (viewMode == ReportViewMode.READ_ONLY || viewMode == ReportViewMode.EDIT_BEFORE_SENDING)
			updateSendReportButton();
	}

	/**  
	 * Fill in the offline report data.
	 */
	private void setReportData()
	{
		licensePlateEditText.setText(reportMap.get(JSONKeys.REPORT_KEY_LICENSE_PLATE));

		if (reportMap.get(JSONKeys.REPORT_KEY_CODE) != null)
		{
			int reportCode = Integer.parseInt(reportMap.get(JSONKeys.REPORT_KEY_CODE));
			if (reportCode != -1)
			{
				String reportDescription = ReportUtils.getReportDescription(this, reportCode);
				reportDescriptionText.setText(reportDescription);

				// Hide unselected buttons
				int dangerLevel = ReportUtils.getReportDangerLevel(this, reportCode);
				Utils.setAsBackroundView(danger1Button, dangerLevel != 1);
				Utils.setAsBackroundView(danger2Button, dangerLevel != 2);
				Utils.setAsBackroundView(danger3Button, dangerLevel != 3);

				if (viewMode == ReportViewMode.READ_ONLY)
				{
					danger1Button.setVisibility(dangerLevel != 1 ? View.INVISIBLE : View.VISIBLE);
					danger1Button.setEnabled(dangerLevel == 1);
					danger2Button.setVisibility(dangerLevel != 2 ? View.INVISIBLE : View.VISIBLE);
					danger2Button.setEnabled(dangerLevel == 2);
					danger3Button.setVisibility(dangerLevel != 3 ? View.INVISIBLE : View.VISIBLE);
					danger3Button.setEnabled(dangerLevel == 3);
				}
			}
		}

		// Get report location
		latitude = Double.parseDouble(reportMap.get(JSONKeys.REPORT_KEY_LATITUDE));
		longitude = Double.parseDouble(reportMap.get(JSONKeys.REPORT_KEY_LONGITUDE));

		// Get pre-resolved address	
		String addressStr = reportMap.get(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY);//!=null ? reportMap.get(JSONKeys.REPORT_KEY_STREET) : ReportUtils.getSingleLineAddress(this, ReportUtils.resolveLocationToAddress(this, latitude, longitude));
		if (addressStr != null && !addressStr.isEmpty())
			locationTextView.setText(addressStr);
		else
		{
			// Resolve address in async task
			Location l = new Location("Manual");
			l.setLatitude(latitude);
			l.setLongitude(longitude);
			new ResolveAddressTask(this, locationTextView).execute(l);
		}

		// Get report time
		reportTime = Long.parseLong(reportMap.get(JSONKeys.REPORT_KEY_TIME));

		if (viewMode == ReportViewMode.READ_ONLY)
			reportButton.setText(R.string.close_report_button);
		else if (viewMode == ReportViewMode.FIX_SENT_REPORT)
			reportButton.setText(R.string.update);
	}

	private void updateSendReportButton()
	{
		boolean licensePlateValid = LicensePlateUtils.isValidLicensePlate(licensePlateEditText.getText().toString());
		boolean reportDescriptionSelected = !reportDescriptionText.getText().toString().isEmpty();

		reportButton.setEnabled(licensePlateValid && reportDescriptionSelected);
	}

	//	@Override
	//	protected void onSaveInstanceState(Bundle outState)
	//	{
	//		outState.putSerializable(EXTRA_SELECTED_REPORT, (Serializable) reportMap);
	//		outState.putSerializable(EXTRA_REPORT_VIEW_MODE, viewMode);
	//
	//		super.onSaveInstanceState(outState);
	//	}

	private void configureDangerButtons()
	{
		if (viewMode != ReportViewMode.READ_ONLY)
		{
			danger1Button.setOnClickListener(new DangerOnClickListener(1, R.string.danger_button1, R.drawable.danger1, level1ReportDescriptions));
			danger2Button.setOnClickListener(new DangerOnClickListener(2, R.string.danger_button2, R.drawable.danger2, level2ReportDescriptions));
			danger3Button.setOnClickListener(new DangerOnClickListener(3, R.string.danger_button3, R.drawable.danger3, level3ReportDescriptions));
		}
	}

	private class DangerOnClickListener implements OnClickListener
	{
		private int index;
		private int titleId;
		private int titleIconId;
		private CharSequence[] dialogItems;

		public DangerOnClickListener(int index, int titleId, int titleIconId, List<Entry<String, Integer>> reportEntries)
		{
			this.index = index;
			this.titleId = titleId;
			this.titleIconId = titleIconId;
			this.dialogItems = new CharSequence[reportEntries.size()];
			for (int i = 0; i < dialogItems.length; i++)
			{
				String description = reportEntries.get(i).getKey();
				dialogItems[i] = description;
			}
		}

		@Override
		public void onClick(View v)
		{
			Utils.closeSoftKeyboard(EditReportActivity.this, licensePlateEditText.getWindowToken());

			AlertDialog.Builder builder = new AlertDialog.Builder(EditReportActivity.this);
			builder.setTitle(titleId).setIcon(titleIconId).setItems(dialogItems, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					// The 'which' argument contains the index position of the selected item
					reportDescriptionText.setText(dialogItems[which]);

					updateSendReportButton();

					// Hide unselected buttons
					Utils.setAsBackroundView(danger1Button, index != 1);
					Utils.setAsBackroundView(danger2Button, index != 2);
					Utils.setAsBackroundView(danger3Button, index != 3);
				}
			});

			builder.create().show();
		}
	};

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar()
	{
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * Invoked by the "Send/Edit Report" button. 
	 */
	public void sendOrSaveReport(View v)
	{
		Boolean closingReport = Boolean.valueOf(reportButton.getText().toString().startsWith(getString(R.string.close_report_button)));
		// User pressed the 'close' button
		if (closingReport.booleanValue())
		{
			// Simply close the activity
			finish();

			return;
		}

		// Prevent self reporting
		if (ReportUtils.isReportingSelf(this, licensePlateEditText.getText().toString()))
		{
			Toast.makeText(getApplicationContext(), getString(R.string.reporting_self_error), Toast.LENGTH_SHORT).show();
			return;
		}

		// No limit on editing report
		if (viewMode != ReportViewMode.FIX_SENT_REPORT && ReportUtils.isExcessiveReporting(this, reportTime))
		{
			Toast.makeText(getApplicationContext(), getString(R.string.reporting_excessivly_error), Toast.LENGTH_SHORT).show();
			return;
		}

		if (!loggedInLicensePlate.isEmpty())
		{
			sendReportTask = new SendOrRemoveReportTask(this, false);
			sendReportTask.execute();
		}
		else
		// User is not registered
		{
			Toast.makeText(getApplicationContext(), getString(R.string.unregistered_user_cannot_send), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Invoked by the "Remove Report" button. 
	 */
	public void removeReport(View v)
	{
		sendReportTask = new SendOrRemoveReportTask(this, true);
		sendReportTask.execute();
	}

	/**
	 * An AsyncTask that sends a waiting report or edits an existing one.
	 */
	private class SendOrRemoveReportTask extends AsyncTask<Void, Void, String>
	{
		private Context context;
		private boolean remove;

		public SendOrRemoveReportTask(Context context, boolean remove)
		{
			this.context = context;
			this.remove = remove;
		}

		@Override
		protected void onPreExecute()
		{
			// Show progress bar and hide buttons
			progressBar.setVisibility(View.VISIBLE);
			reportButton.setVisibility(View.INVISIBLE);
			if (removeReportButton != null)
				removeReportButton.setVisibility(View.INVISIBLE);
		}

		@Override
		protected String doInBackground(Void... params)
		{
			String reportedPlate = licensePlateEditText.getText().toString();
			int descriptionCode = ReportUtils.getReportCode(context, reportDescriptionText.getText());
			String reportedBy = PreferencesUtils.getString(context, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

			ServerAPI server = new ServerAPI(context);

			if (!remove)
			{
				// Send the report to the remote DB
				JSONObject jsonRemoteReport = server.addReport(reportedPlate, latitude, longitude, descriptionCode, reportTime, reportedBy);
				// Verify we got a good answer from the server
				if (!ServerErrorCodes.hasError(jsonRemoteReport))
				{
					Log.d(ACT_TAG, getString(R.string.report_sent));

					// Remove sent report from offline storage
					ReportUtils.deleteReport(context, jsonRemoteReport);

					return "";
				}
				else
				// Failed to add report to server DB
				{
					// Log the server error
					String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonRemoteReport, reportedPlate);
					Log.e(ACT_TAG, errorMessage);

					// If the error is the user's fault - we can remove the offline report
					if (ServerErrorCodes.isErrorCausedByUser(jsonRemoteReport))
					{
						ReportUtils.deleteReport(context, reportTime);
						setResult(RESULT_OK);
					}

					return errorMessage;
				}
			}
			else
			{
				// Remove report
				if (ReportUtils.getNumberOfWitnesses(reportMap) == 1)
				{
					JSONArray reportedByArray = null;
					try
					{
						reportedByArray = new JSONArray(reportMap.get(JSONKeys.REPORT_KEY_REPORTED_BY));
						JSONObject jsonRemoteReport = server.removeReport(reportedByArray, reportTime);
						if (!ServerErrorCodes.hasError(jsonRemoteReport))
						{
							return "";
						}
						else
						{
							String errorMessage = ServerErrorCodes.getErrorMessage(context, jsonRemoteReport, reportedPlate);
							Log.e(ACT_TAG, errorMessage);
							return errorMessage;
						}
					}
					catch (JSONException e)
					{
						Log.e(ACT_TAG, e.getMessage(), e);
						return e.getMessage();
					}
				}
				else
				{
					// Cannot remove a report that was witnessed by other people
					return getString(R.string.report_cannot_remove);
				}
			}
		}

		@Override
		protected void onPostExecute(String errorMessage)
		{
			if (errorMessage.isEmpty())
			{
				if (!remove)
				{
					if (viewMode != ReportViewMode.FIX_SENT_REPORT)
					{
						Toast.makeText(context, R.string.report_sent, Toast.LENGTH_LONG).show();

						// Remember the time of the last send report
						PreferencesUtils.putLong(context, ReportUtils.PROPERTY_LAST_REPORT_TIME, reportTime);
					}
					else
					{
						Toast.makeText(context, R.string.report_updated, Toast.LENGTH_LONG).show();
					}
				}
				else
				{
					Toast.makeText(context, R.string.report_deleted, Toast.LENGTH_LONG).show();
				}

				setResult(RESULT_OK);
				// Close activity
				finish();
			}
			else
			{
				// Failed to send/edit/remove report
				Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();

				// Hide progress bar and hide buttons
				progressBar.setVisibility(View.INVISIBLE);
				reportButton.setVisibility(View.VISIBLE);
				if (removeReportButton != null)
					removeReportButton.setVisibility(View.VISIBLE);
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onNavigateUp()
	{
		boolean up = super.onNavigateUp();

		cancelAsyncTask();

		//overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);

		return up;
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();

		cancelAsyncTask();

		//overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}

	private void cancelAsyncTask()
	{
		if (sendReportTask != null && sendReportTask.getStatus() != AsyncTask.Status.FINISHED)
		{
			if (sendReportTask.cancel(true))
				Toast.makeText(this, getString(R.string.report_send_cancelled), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void finish()
	{
		super.finish();

		overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
	}
}