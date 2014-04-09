package com.roadwatch.app.test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.server.ServerErrorCodes;

/**
 * PENDING:
 * 1. add Login with Facebook support
 * 
 * @author Nati
 */
@SuppressLint("NewApi")
public class ServerStressTestActivity extends Activity
{
	// Logging tag for the activity
	private static final String TAG = ServerStressTestActivity.class.getSimpleName();

	/** Copied from AsyncTask class to support Android 2.3 */
	private static final int CORE_POOL_SIZE = 10;
	private static final int MAXIMUM_POOL_SIZE = 50;
	private static final int KEEP_ALIVE = 1;
	private static final ThreadFactory sThreadFactory = new ThreadFactory()
	{
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r)
		{
			return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
		}
	};
	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(128);
	public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
	/** End of copied code */

	public static final int LOGIN_REQUEST = 10;

	private EditText numberOfReportersText;
	private EditText numberOfReportsToSendText;
	private EditText delayText;

	private TextView loginErrorMsg;
	private ProgressBar testProgressBar;
	private Button startTestButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		numberOfReportersText = (EditText) findViewById(R.id.no_of_reporters_text);
		numberOfReportsToSendText = (EditText) findViewById(R.id.no_of_reports_text);
		delayText = (EditText) findViewById(R.id.delay_text);
		loginErrorMsg = (TextView) findViewById(R.id.error_text);
		testProgressBar = (ProgressBar) findViewById(R.id.test_progress);
		startTestButton = (Button) findViewById(R.id.start_test_button);
		// Login button Click Event
		startTestButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				testProgressBar.setVisibility(View.VISIBLE);
				startTestButton.setEnabled(false);
				loginErrorMsg.setText("");

				int numberOfReporters = Integer.parseInt(numberOfReportersText.getText().toString());
				for (int i = 0; i < numberOfReporters; i++)
				{
					new StartTestTask(ServerStressTestActivity.this, (i + 1)).executeOnExecutor(THREAD_POOL_EXECUTOR);

					// Take a random delay between each reporter (so they don't all start at the same time)
					int randomDelay = (int) (Math.random() * Integer.parseInt(delayText.getText().toString()));
					try
					{
						Thread.sleep(randomDelay);
					}
					catch (InterruptedException ignore)
					{
					}
				}
			}
		});
	}

	/**
	 * An AsyncTask that logs-in the user.
	 */
	private class StartTestTask extends AsyncTask<Void, Void, Void>
	{
		private Context context;
		private int reporterNumber;

		public StartTestTask(Context context, int reporterNumber)
		{
			this.context = context;
			this.reporterNumber = reporterNumber;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			ServerAPI server = new ServerAPI(context);

			int numberOfReportsToSend = Integer.parseInt(numberOfReportsToSendText.getText().toString());
			int delay = Integer.parseInt(delayText.getText().toString());
			for (int i = 0; i < numberOfReportsToSend; i++)
			{
				String loggedInLicensePlate = getReporterLicensePlate();
				String reportedPlate = getLicensePlate(i);
				JSONObject jsonReportResponse = server.addReport(reportedPlate, 0.0, 0.0, 5439, System.currentTimeMillis(), loggedInLicensePlate);

				if (ServerErrorCodes.hasError(jsonReportResponse))
				{
					Log.e(TAG, ServerErrorCodes.getErrorMessage(context, jsonReportResponse, reportedPlate));
				}
				else
				{
					Log.i(TAG, "Reporter " + reporterNumber + " : Report on [" + reportedPlate + "] send successfully");
				}

				try
				{
					Thread.sleep(delay);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			return null;
		}

		/**
		 * Reporter license plate is [reporterNumber]-000-00
		 * @return
		 */
		private String getReporterLicensePlate()
		{
			String reporterNumberString = "";
			if (reporterNumber < 10)
				reporterNumberString = "0" + reporterNumber;
			else
				reporterNumberString = "" + reporterNumber;

			return reporterNumberString + "-000-00";
		}

		/**
		 * Test license plate is [reporterNumber]-[reportNumber]-00
		 * 
		 * @param reportNumber
		 * @return
		 */
		private String getLicensePlate(int reportNumber)
		{
			String reportNumberString = "";
			if (reportNumber < 10)
				reportNumberString = "00" + reportNumber;
			else if (reportNumber < 100)
				reportNumberString = "0" + reportNumber;
			else
				reportNumberString = "" + reportNumber;

			return "00-" + reportNumberString + "-00";
		}

		@Override
		protected void onPostExecute(Void params)
		{
			// Remove the register progress indicator
			testProgressBar.setVisibility(View.INVISIBLE);

			// Re-enable the button to allow re-login in case of an error
			startTestButton.setEnabled(true);
		}
	}
}