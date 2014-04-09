package com.roadwatch.app.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.targets.ViewTarget;
import com.google.analytics.tracking.android.MapBuilder;
import com.roadwatch.app.ApplicationData;
import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.location.Locator;
import com.roadwatch.app.location.LocatorListener;
import com.roadwatch.app.location.ResolveAddressTask;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;
import com.roadwatch.app.speech.SpeechManager;
import com.roadwatch.app.speech.SpeechRecognitionUtil;
import com.roadwatch.app.speech.WordDetectorListener;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.widgets.LicensePlateEditText;

/**
 * PENDING:    
 * 1. Auto-save report for later when user press 'back' before a successful sent. 
 * 
 * @author Nati
 */
public class ReportActivity extends ActionBarActivity implements LocatorListener, WordDetectorListener
{
	// Logging tag
	private static final String TAG = ReportActivity.class.getSimpleName();

	private static final String FIRST_TIME_REPORT_PROPERTY = "firstReport";

	private static final int MAX_VOICE_ATTEMPTS = 3;

	private static final String KEY_TIME = "key_time";
	private static final String KEY_LICENSE_PLATE = "key_license_plate";
	private static final String KEY_REPORT_DESCRIPTION = "key_report_description";
	private static final String KEY_CURRENT_LOCATION = "key_current_location";
	private static final String KEY_STREET = "key_street";
	private static final String KEY_CITY = "key_city";
	private static final String KEY_COUNTRY = "key_country";
	private static final String KEY_REPORT_BUTTON_ENABLED = "key_report_button_enabled";
	private static final String KEY_REPORT_BUTTON_VISABILITY = "key_report_button_visability";
	private static final String KEY_COUNT_DOWN_ACTIVE = "key_count_down_active";
	private static final String KEY_PROGRESS_BAR_VISABILITY = "key_progress_bar_visability";

	public static final String EXTRA_VOICE_RECOGNITION_MODE = "extra_voice_recognition_mode";
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1111;
	private static final int SEND_REPORT_FAILED_NOTIFICATION_ID = 2001;

	// Cache the address for sending
	private String loggedInLicensePlate;
	private long reportTime;
	private String street;
	private String city;
	private String country;

	// Handles to UI widgets
	private TextView locationFoundTextView;
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

	private Locator locationHelper;
	private Location currentLocation;
	private transient AsyncTask<Location, Void, Address> resolveAddressTask;

	private SpeechManager speecher;
	private int processVoiceAttempts;
	private boolean handsFreeMode;
	private CountDownTimer pressButtonTimer;

	private SendOrSaveReportTask sendOrSaveReportTask;

	private int counter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		handsFreeMode = getIntent().hasExtra(EXTRA_VOICE_RECOGNITION_MODE);

		if (handsFreeMode)
		{
			// PENDING: Make activity look more Waze-like ?
			//setTheme(R.style.Theme_Transparent);

			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "floating report button", null).build());
		}
		else
		{
			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "report button", null).build());
		}

		setContentView(R.layout.report);

		// Get handles to the UI view objects
		licensePlateEditText = (LicensePlateEditText) findViewById(R.id.license_plate);
		final ImageButton speakButton = (ImageButton) findViewById(R.id.action_button);
		speakButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Disable any running timer
				stopButtonTimer();

				processVoiceAttempts = 0;
				startSpeechRecognition();
			}
		});
		if (!SpeechRecognizer.isRecognitionAvailable(this))
			speakButton.setEnabled(false);

		danger1Button = (Button) findViewById(R.id.danger_button1);
		danger2Button = (Button) findViewById(R.id.danger_button2);
		danger3Button = (Button) findViewById(R.id.danger_button3);
		reportDescriptionText = (TextView) findViewById(R.id.report_description);
		locationFoundTextView = (TextView) findViewById(R.id.location_found_text);
		reportButton = (Button) findViewById(R.id.report_button);

		licensePlateEditText.addTextChangedListener(new TextWatcher()
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
				String licensePlate = licensePlateEditText.getText().toString();
				updateReportButton();
				if (licensePlate.length() == 9)
				{
					Utils.closeSoftKeyboard(ReportActivity.this, licensePlateEditText.getWindowToken());
					if (handsFreeMode)
						startButtonTimer();
				}
				else
				{
					stopButtonTimer();
				}
			}
		});

		level1ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_1_report_codes);
		level2ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_2_report_codes);
		level3ReportDescriptions = ReportUtils.getReportEntryList(this, R.array.danger_level_3_report_codes);

		configureDangerButtons();

		progressBar = (ProgressBar) findViewById(R.id.send_report_progress);

		loggedInLicensePlate = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		locationHelper = new Locator(this, handsFreeMode);

		handleSavedInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// Prevent default activity restore - we do everything in handlSavedInstanceState()
	}

	/**
	 * Initializes activity with previously saved data
	 * 
	 * PENDING:
	 * 1. Don't start speech recognition on orientation change if was already displayed once
	 * 
	 * @param savedInstanceState
	 */
	private void handleSavedInstanceState(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			reportTime = savedInstanceState.getLong(KEY_TIME);

			if (savedInstanceState.containsKey(KEY_LICENSE_PLATE))
			{
				licensePlateEditText.setText(savedInstanceState.getString(KEY_LICENSE_PLATE));
			}
			else
			{
				if (!handsFreeMode)
				{
					// Auto open key-board for license plate text view					
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				}
				else
				{
					// Check if we need to activate license plate voice recognition				
					startSpeechRecognition();
				}
			}

			if (savedInstanceState.containsKey(KEY_REPORT_DESCRIPTION))
				reportDescriptionText.setText(savedInstanceState.getString(KEY_REPORT_DESCRIPTION));

			if (savedInstanceState.containsKey(KEY_CURRENT_LOCATION))
			{
				currentLocation = savedInstanceState.getParcelable(KEY_CURRENT_LOCATION);

				// When resolve is completed, we always have at least a Country address. 
				if (savedInstanceState.containsKey(KEY_COUNTRY))
				{
					street = savedInstanceState.getString(KEY_STREET);
					city = savedInstanceState.getString(KEY_CITY);
					country = savedInstanceState.getString(KEY_COUNTRY);

					String address = ReportUtils.getSingleLineAddress(street, city, country);
					locationFoundTextView.setText(getString(R.string.location_found) + (!address.isEmpty() ? " (" + address + ")" : ""));
				}
				else
				// We found location but still need to resolved address 
				{
					locationFoundTextView.setText(getString(R.string.location_found));
					resolveAddressTask = new ResolveAddressTask(this, locationFoundTextView).execute(currentLocation);
				}
			}

			updateReportButton();

			// Maintain report button and progress bar state between orientation changes
			reportButton.setEnabled(savedInstanceState.getBoolean(KEY_REPORT_BUTTON_ENABLED));
			reportButton.setVisibility(savedInstanceState.getInt(KEY_REPORT_BUTTON_VISABILITY));
			progressBar.setVisibility(savedInstanceState.getInt(KEY_PROGRESS_BAR_VISABILITY));

			// Restart button timer if necessary
			if (savedInstanceState.getBoolean(KEY_COUNT_DOWN_ACTIVE))
				startButtonTimer();
		}
		else
		// No previously saved state
		{
			reportTime = System.currentTimeMillis();

			if (!handsFreeMode)
			{
				// Check if we need to do display first time report wizard
				if (PreferencesUtils.getBoolean(this, FIRST_TIME_REPORT_PROPERTY, true))
				{
					showFirstTimeWizard();
				}
				else
				{
					// Open keyboard
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				}
			}
			else
			{
				// Check if we need to activate license plate voice recognition
				if (handsFreeMode)
					startSpeechRecognition();
			}
		}
	}

	private void showFirstTimeWizard()
	{
		Utils.setAsBackroundView(findViewById(R.id.action_description_label), true);
		Utils.setAsBackroundView(findViewById(R.id.nested_linear_layout), true);
		Utils.setAsBackroundView(findViewById(R.id.location_found_text), true);
		Utils.setAsBackroundView(findViewById(R.id.report_button), true);

		final ShowcaseView showcaseView = ShowcaseView.insertShowcaseView(new ViewTarget(findViewById(R.id.license_plate)), this, getString(R.string.report_wizard_step_title, Integer.valueOf(1)),
				getString(R.string.report_wizard_step1));
		showcaseView.overrideButtonClick(new View.OnClickListener()
		{
			@SuppressLint("NewApi")
			@Override
			public void onClick(View view)
			{
				switch (counter)
				{
				case 0:
					showcaseView.setShowcase(new ViewTarget(findViewById(R.id.action_button)), true);
					showcaseView.setText(getString(R.string.report_wizard_step_title, Integer.valueOf(2)), getString(R.string.report_wizard_step2));
					showcaseView.setScaleMultiplier(0.3f);
					break;

				case 1:
					Utils.setAsBackroundView(findViewById(R.id.license_plate_layout), true);
					Utils.setAsBackroundView(findViewById(R.id.lp_title), true);
					Utils.setAsBackroundView(findViewById(R.id.action_description_label), false);
					Utils.setAsBackroundView(findViewById(R.id.nested_linear_layout), false);
					showcaseView.setShowcase(new ViewTarget(danger2Button), true);
					showcaseView.setText(getString(R.string.report_wizard_step_title, Integer.valueOf(3)), getString(R.string.report_wizard_step3));
					showcaseView.setScaleMultiplier(1f);
					break;

				case 2:
					Utils.setAsBackroundView(findViewById(R.id.nested_linear_layout), true);
					Utils.setAsBackroundView(findViewById(R.id.action_description_label), true);
					Utils.setAsBackroundView(findViewById(R.id.report_button), false);
					showcaseView.setShowcase(new ViewTarget(reportButton), true);
					showcaseView.setText(getString(R.string.report_wizard_step_title, Integer.valueOf(4)), getString(R.string.report_wizard_step4));
					showcaseView.setScaleMultiplier(0.7f);
					//showcaseView.animateGesture(0, 0, 0, 0);
					break;

				case 3:
					Utils.setAsBackroundView(findViewById(R.id.license_plate_layout), false);
					Utils.setAsBackroundView(findViewById(R.id.lp_title), false);
					Utils.setAsBackroundView(findViewById(R.id.action_description_label), false);
					Utils.setAsBackroundView(findViewById(R.id.nested_linear_layout), false);
					Utils.setAsBackroundView(findViewById(R.id.location_found_text), false);

					showcaseView.hide();
					// Auto open key-board for license plate text view
					Utils.openSoftKeyboard(ReportActivity.this, licensePlateEditText.getWindowToken());

					// Update property
					PreferencesUtils.putBoolean(ReportActivity.this, FIRST_TIME_REPORT_PROPERTY, false);

					break;
				}
				counter++;
			}
		});
	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu)
	//	{
	//		this.actionBarMenu = menu;
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		//getMenuInflater().inflate(R.menu.report_actionbar_menu, menu);
	//		
	//		MenuItem item = actionBarMenu.add(Menu.NONE, R.id.action_save_report, 2, R.string.save_report_button);
	//		
	//		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	//		item.setIcon(R.drawable.ic_action_search);
	//		
	//		actionBarMenu.setGroupVisible(Menu.NONE, true);
	//
	//		return super.onCreateOptionsMenu(menu);
	//	}
	//
	//	@Override
	//	public boolean onOptionsItemSelected(MenuItem item)
	//	{
	//		switch (item.getItemId())
	//		{
	//		case android.R.id.home:
	//			// This ID represents the Home or Up button. In the case of this
	//			// activity, the Up button is shown. Use NavUtils to allow users
	//			// to navigate up one level in the application structure. For
	//			// more details, see the Navigation pattern on Android Design:
	//			//
	//			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
	//			//
	//			NavUtils.navigateUpFromSameTask(this);
	//			return true;
	//
	//		case R.id.action_save_report:
	//			break;
	//		case R.id.action_send_report:
	//			break;
	//			
	//		}
	//
	//		return super.onOptionsItemSelected(item);
	//	}

	/**
	 * 
	 */
	private void updateReportButton()
	{
		boolean licensePlateValid = LicensePlateUtils.isValidLicensePlate(licensePlateEditText.getText().toString());
		boolean locationFound = currentLocation != null;

		boolean allowSave = licensePlateValid && locationFound;
		reportButton.setEnabled(allowSave);
		if (allowSave)
		{
			//MenuItem item = actionBarMenu.add(Menu.NONE, R.id.action_save_report, 2, R.string.save_report_button);
			//item.setIcon(R.drawable.ic_action_search);

			boolean allowSend = !reportDescriptionText.getText().toString().isEmpty();
			reportButton.setText(allowSend ? R.string.send_report_button : R.string.save_report_button);
		}
		//else
		//actionBarMenu.removeItem(R.id.action_save_report);
	}

	private void startSpeechRecognition()
	{
		startSpeechRecognition(getString(R.string.say_license_plate));
	}

	/**
	 * Display google voice recognition dialog
	 */
	private void startSpeechRecognition(String prompt)
	{
		try
		{
			speecher = new SpeechManager(this);
		}
		catch (RuntimeException e)
		{
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}

		Intent intent = SpeechRecognitionUtil.getRecognizerIntent(this);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);

		//Start the Voice recognizer activity for the result.
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)
		{
			//If Voice recognition is successful then it returns RESULT_OK
			if (resultCode == RESULT_OK)
			{
				List<String> resultList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				float[] confidenceArray = data.getFloatArrayExtra("android.speech.extra.CONFIDENCE_SCORES");// RecognizerIntent.EXTRA_CONFIDENCE_SCORES constant is supported only from API level 14

				recieveResults(resultList, confidenceArray);
			}
			else if (resultCode == RESULT_CANCELED)
			{
				recieveResults(null, null);
			}
			else if (resultCode == RecognizerIntent.RESULT_AUDIO_ERROR)
			{
				Toast.makeText(this, "Audio Error", Toast.LENGTH_LONG).show();
			}
			else if (resultCode == RecognizerIntent.RESULT_CLIENT_ERROR)
			{
				Toast.makeText(this, "Client Error", Toast.LENGTH_LONG).show();
			}
			else if (resultCode == RecognizerIntent.RESULT_NETWORK_ERROR)
			{
				Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show();
			}
			else if (resultCode == RecognizerIntent.RESULT_NO_MATCH)
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Voice Recognition", "Cancelled", null).build());

				if (licensePlateEditText.getText().toString().isEmpty())
				{
					if (handsFreeMode)
					{
						reportButton.setText(R.string.close_report_button);
						reportButton.setEnabled(true);
						startButtonTimer();
					}

					// Auto open key-board for license plate text view
					//Utils.openSoftKeyboard(this, licensePlateEditText.getWindowToken());
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				}
			}
			else if (resultCode == RecognizerIntent.RESULT_SERVER_ERROR)
			{
				Toast.makeText(this, "Server Error", Toast.LENGTH_LONG).show();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Attempt to parse license plate number by voice
	 * PENDING:
	 * 1. Test saying '0' and '66'
	 * 2. Handle partial results
	 * 3. Last attempt will save it to a file?
	 */
	@Override
	public void recieveResults(List<String> heard, float[] scores)
	{
		if (heard != null && !heard.isEmpty())
		{
			processVoiceAttempts++;
			Log.i(TAG, "All : " + heard);
			String bestHeard = getBestHeard(heard);
			Log.i(TAG, "Best heard : " + bestHeard);
			List<Integer> in = processVoiceInput(bestHeard);
			Log.i(TAG, "Processed voice input : " + in);

			if (in.size() == 7)
			{
				// Google Analytics
				ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Voice Recognition", "Successfull", null).build());

				speecher.say("Got it");
				licensePlateEditText.setText("" + in.get(0) + in.get(1) + "-" + in.get(2) + in.get(3) + in.get(4) + "-" + in.get(5) + in.get(6));
				Utils.closeSoftKeyboard(this, licensePlateEditText.getWindowToken());
				if (handsFreeMode)
					startButtonTimer();
			}
			else
			// Couldn't get a complete license plate number by voice - try again.
			{
				if (processVoiceAttempts < MAX_VOICE_ATTEMPTS)
				{
					// Google Analytics
					ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Voice Recognition", "Unsuccessfull", null).build());

					startSpeechRecognition(getString(R.string.say_license_plate_again, bestHeard));
				}
				else
				{
					// Google Analytics
					ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Voice Recognition", "Failed", null).build());

					speecher.say("Please try again");
					// PENDING: Save to file ?
				}
			}
		}
		else
		// Nothing was heard by the voice recognition engine
		{
			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Voice Recognition", "Heared Nothing", null).build());

			Log.i(TAG, "Nothing to report");
			// If user hasn't started to type in the license plate manually - close the report activity
			if (handsFreeMode)
			{
				if (licensePlateEditText.getText().toString().isEmpty())
				{
					reportButton.setText(R.string.close_report_button);
					reportButton.setEnabled(true);
					startButtonTimer();
				}
			}

			// Auto open key-board for license plate text view
			//Utils.openSoftKeyboard(this, licensePlateEditText.getWindowToken());
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
	}

	/**
	 * Start timer for button and set button's text according to the specified text resource id.
	 */
	private void startButtonTimer()
	{
		if (pressButtonTimer == null)
		{
			pressButtonTimer = new CountDownTimer(8000, 1000)
			{
				private String reportButtonText = reportButton.getText().toString();

				@Override
				public void onTick(long millisUntilFinished)
				{
					reportButton.setText(reportButtonText + " (" + (millisUntilFinished / 1000) + ")");
				}

				@Override
				public void onFinish()
				{
					if (!isFinishing())
					{
						// Google Analytics
						ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Button Timer", "Reached 0", null).build());

						reportButton.setText(reportButtonText + " (0)");
						reportButton.performClick();
					}
				}
			}.start();
		}
	}

	/**
	 * Stop timer button if active and remove timer text from button.
	 */
	private void stopButtonTimer()
	{
		if (pressButtonTimer != null)
		{
			pressButtonTimer.cancel();
			pressButtonTimer = null;
		}

		if (reportButton.getText().toString().endsWith(")"))
		{
			// Remove the (X) timer from the button's label
			String originalText = reportButton.getText().toString().substring(0, reportButton.getText().length() - 3).trim();
			reportButton.setText(originalText);
		}
	}

	/**
	 * Can improve this method to recognize numbers said in words like "one, two, hundred. etc...
	 * 
	 * @param spokenNumber
	 * @return true if number is an integer number (1,2, 125, etc...) and not (one, two, hundred, etc...)
	 */
	private boolean isIntegerNumber(String spokenNumber)
	{
		try
		{
			Integer.valueOf(spokenNumber);

			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	/**
	 * Return the first string that is exactly of size 9.
	 * PENDING: Improve by counting 7 digits
	 * @param allHeard
	 * @return
	 */
	private String getBestHeard(List<String> allHeard)
	{
		for (String heard : allHeard)
		{
			heardLoop: if (heard.trim().length() == 9)
			{
				for (String strNumber : heard.split(" "))
				{
					if (!isIntegerNumber(strNumber))
						break heardLoop;
				}
				return heard;
			}
		}

		return allHeard.get(0);
	}

	private List<Integer> processVoiceInput(String spokenNumber)
	{
		List<Integer> numbers = new ArrayList<Integer>();
		for (String strNumber : spokenNumber.split(" "))
		{
			// Verify we heard a number
			if (isIntegerNumber(strNumber))
			{
				// Convert to int, char by char
				char[] chars = strNumber.toCharArray();
				for (int i = 0; i < chars.length; i++)
				{
					if (Character.isDigit(chars[i]))
						numbers.add(Integer.valueOf(chars[i] - '0'));
				}
			}
			else
			{
				Log.d(TAG, "Skipping '" + strNumber + "' - not a number");
			}
		}

		return numbers;
	}

	private void configureDangerButtons()
	{
		danger1Button.setOnClickListener(new DangerOnClickListener(1, R.string.danger_button1, R.drawable.danger1, level1ReportDescriptions));
		danger2Button.setOnClickListener(new DangerOnClickListener(2, R.string.danger_button2, R.drawable.danger2, level2ReportDescriptions));
		danger3Button.setOnClickListener(new DangerOnClickListener(3, R.string.danger_button3, R.drawable.danger3, level3ReportDescriptions));
	}

	private class DangerOnClickListener implements OnClickListener
	{
		private int index;
		private int titleId;
		private int titleIconId;
		private CharSequence[] dialogItems;
		private AlertDialog alertDialog;

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
			if (alertDialog == null)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(ReportActivity.this);
				builder.setTitle(titleId).setIcon(titleIconId).setItems(dialogItems, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						// The 'which' argument contains the index position of the selected item
						reportDescriptionText.setText(dialogItems[which]);

						updateReportButton();

						if (handsFreeMode)
							startButtonTimer();

						// Hide unselected buttons
						Utils.setAsBackroundView(danger1Button, index != 1);
						Utils.setAsBackroundView(danger2Button, index != 2);
						Utils.setAsBackroundView(danger3Button, index != 3);
					}
				});
				alertDialog = builder.create();
			}

			alertDialog.show();

			stopButtonTimer();

			Utils.closeSoftKeyboard(ReportActivity.this, licensePlateEditText.getWindowToken());
		}
	};

	@Override
	protected void onStart()
	{
		super.onStart();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (currentLocation == null)
			locationHelper.connect(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putLong(KEY_TIME, reportTime);

		String licensePlate = licensePlateEditText.getText().toString();
		if (!licensePlate.isEmpty())
			outState.putString(KEY_LICENSE_PLATE, licensePlate);

		String reportDescription = reportDescriptionText.getText().toString();
		if (!reportDescription.isEmpty())
			outState.putString(KEY_REPORT_DESCRIPTION, reportDescription);

		if (currentLocation != null)
			outState.putParcelable(KEY_CURRENT_LOCATION, currentLocation);

		// country is not null only after address resolving is complete
		if (country != null)
		{
			outState.putString(KEY_STREET, street);
			outState.putString(KEY_CITY, city);
			outState.putString(KEY_COUNTRY, country);
		}

		outState.putBoolean(KEY_REPORT_BUTTON_ENABLED, reportButton.isEnabled());
		outState.putInt(KEY_REPORT_BUTTON_VISABILITY, reportButton.getVisibility());
		outState.putBoolean(KEY_COUNT_DOWN_ACTIVE, pressButtonTimer != null);
		outState.putInt(KEY_PROGRESS_BAR_VISABILITY, progressBar.getVisibility());

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		locationHelper.disconnect();
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		if (speecher != null)
			speecher.stop();

		if (resolveAddressTask != null)
			resolveAddressTask.cancel(true);

		if (pressButtonTimer != null)
		{
			pressButtonTimer.cancel();
			pressButtonTimer = null;
		}
	}

	/**
	 * Invoked when we get an updated location from the GPS.
	 * We keep requesting for a location until we reach the minimal required accuracy radius.
	 */
	@Override
	public void onLocationChanged(Location location)
	{
		// Reference the updated location
		currentLocation = location;

		Log.d(TAG, location.getProvider() + " provider found location within accuracy of " + location.getAccuracy() + " meters");

		// Show the user that we got a location
		if (locationFoundTextView.getText().toString().isEmpty())
			locationFoundTextView.setText(R.string.location_found);

		// Resolve address in background task
		resolveAddressTask = new ResolveAddressTask(this, locationFoundTextView).execute(currentLocation);

		updateReportButton();
	}

	/**
	 * Invoked by the "Send Report" button.
	 * 
	 * @param v
	 *            The view object associated with this method, in this case a Button.
	 */
	public void sendOrSaveReport(View v)
	{
		Boolean closingReport = Boolean.valueOf(reportButton.getText().toString().startsWith(getString(R.string.close_report_button)));
		if (closingReport.booleanValue())
		{
			finish();
			return;
		}

		// We don't allow self reporting
		if (ReportUtils.isReportingSelf(this, licensePlateEditText.getText().toString()))
		{
			Toast.makeText(getApplicationContext(), getString(R.string.reporting_self_error), Toast.LENGTH_SHORT).show();
			return;
		}

		if (ReportUtils.isExcessiveReporting(this, reportTime))
		{
			Toast.makeText(getApplicationContext(), getString(R.string.reporting_excessivly_error), Toast.LENGTH_SHORT).show();
			return;
		}

		Boolean sendingReport = Boolean.valueOf(reportButton.getText().toString().startsWith(getString(R.string.send_report_button)));
		if (sendingReport.booleanValue())
		{
			if (!loggedInLicensePlate.isEmpty())
			{
				// Start the background task of sending report
				new ReportActivity.SendOrSaveReportTask(this).execute(sendingReport);

				// When opened from nav app - sending report is done in the bg
				if (handsFreeMode)
				{
					// Notify the user we're started the sending bg process
					Toast.makeText(getApplicationContext(), getString(R.string.sending_report), Toast.LENGTH_SHORT).show();

					finish();
				}
			}
			else
			{
				// Notify that only registered users can send reports
				Toast.makeText(getApplicationContext(), getString(R.string.unregistered_user_cannot_send), Toast.LENGTH_LONG).show();
				if (handsFreeMode)
				{
					// Save report since sending is not allowed for unregistered users
					new ReportActivity.SendOrSaveReportTask(this).execute(Boolean.FALSE);
				}
			}
		}
		else
		{
			sendOrSaveReportTask = new SendOrSaveReportTask(this);
			sendOrSaveReportTask.execute(sendingReport);
		}
	}

	/**
	 * An AsyncTask that sends a new report to the server.
	 */
	private class SendOrSaveReportTask extends AsyncTask<Boolean, Void, String>
	{
		private final int MAX_WAIT_TIME_FOR_ADDRESS_RESOLVE_IN_MILLIS = 8000;
		// Store the context passed to the AsyncTask when the system instantiates it.
		private Context context;

		private boolean sendReport;

		// Constructor called by the system to instantiate the task
		public SendOrSaveReportTask(Context context)
		{
			// Required by the semantics of AsyncTask
			super();

			// Set a Context for the background task
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			reportButton.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected String doInBackground(Boolean... params)
		{
			// Make sure location is found and address is resolved
			int attempts = 0;
			while (!isLocationFoundAndResolved() && attempts * 500 < MAX_WAIT_TIME_FOR_ADDRESS_RESOLVE_IN_MILLIS)
			{
				attempts++;
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					// No-op
				}
			}

			sendReport = params[0].booleanValue();
			// Add report to DB 
			ServerAPI server = new ServerAPI(context);
			String reportedPlate = licensePlateEditText.getText().toString();
			double latitude = currentLocation != null ? currentLocation.getLatitude() : 0;
			double longitude = currentLocation != null ? currentLocation.getLongitude() : 0;
			int descriptionCode = ReportUtils.getReportCode(context, reportDescriptionText.getText());

			JSONObject jsonRemoteReport = null;
			String errorMessage = "";
			if (sendReport)
			{
				// Send the report to the remote DB
				jsonRemoteReport = server.addReport(reportedPlate, latitude, longitude, descriptionCode, reportTime, loggedInLicensePlate);

				if (!ServerErrorCodes.hasError(jsonRemoteReport))
				{
					// Google Analytics
					ApplicationData.getTracker().send(MapBuilder.createEvent("Server Action", "Send Report", "Success", null).build());

					Log.d(TAG, getString(R.string.report_sent));
				}
				else
				// Failed to add report to server DB, notify the user
				{
					String longErrorMessage = "";

					if (ServerErrorCodes.isKnownError(jsonRemoteReport))
					{
						longErrorMessage = errorMessage = ServerErrorCodes.getErrorMessage(context, jsonRemoteReport, reportedPlate);
					}
					else
					{
						errorMessage = getString(R.string.send_report_general_error_short);
						longErrorMessage = getString(R.string.send_report_general_error_long);
						// Notify the user we failed to send report
						Utils.showNotification(context, getString(R.string.notification_report_error_title), errorMessage, longErrorMessage, SEND_REPORT_FAILED_NOTIFICATION_ID);
					}

					// Google Analytics
					ApplicationData.getTracker().send(MapBuilder.createEvent("Server Action", "Send Report", errorMessage, null).build());

					// Log the error					
					Log.e(TAG, "Failed to send report : " + errorMessage);
				}
			}

			// If the user pressed the 'save' button or we failed to send it due to a server fault error - save the report locally
			if (!sendReport || (sendReport && ServerErrorCodes.hasError(jsonRemoteReport) && !ServerErrorCodes.isErrorCausedByUser(jsonRemoteReport)))
			{
				// Convert report to json object and save
				JSONObject jsonLocalReport = JSONUtils.convertReportToJSONObject(reportedPlate, latitude, longitude, descriptionCode, reportTime, loggedInLicensePlate);
				JSONUtils.saveJSONObject(context, JSONUtils.UNSENT_REPORTS_FILENAME, jsonLocalReport);
			}

			return errorMessage;
		}

		private boolean isLocationFoundAndResolved()
		{
			return resolveAddressTask != null && resolveAddressTask.getStatus() == Status.FINISHED;
		}

		/**
		 * A method that's called once doInBackground() completes.
		 */
		@Override
		protected void onPostExecute(String errorMessage)
		{
			progressBar.setVisibility(View.INVISIBLE);
			if (errorMessage.isEmpty())
			{
				// Report sent/saved successfully
				Toast.makeText(getApplicationContext(), getString(sendReport ? R.string.report_sent : R.string.saved_report), Toast.LENGTH_SHORT).show();

				finish();

				if (sendReport)
				{
					// Remember the time of the last send report
					PreferencesUtils.putLong(context, ReportUtils.PROPERTY_LAST_REPORT_TIME, reportTime);
				}
			}
			else
			{
				Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
				if (!handsFreeMode)
					reportButton.setVisibility(View.VISIBLE);
			}
		}
	}
}