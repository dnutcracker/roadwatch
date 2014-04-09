package com.roadwatch.app.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;

/**
 * PENDING:
 * 1. Allow user to delete a sent report.
 * 
 * @author Nati created at : 00:04:48 08/11/2013
 *
 */
public class SentReportsActivity extends ListActivity
{
	private static final String TAG = SentReportsActivity.class.getSimpleName();

	private static final String RESULTS_KEY = "results";

	private LayoutAnimationController rowsAnimation;

	private LinearLayout progressLayout;
	private TextView listSummary;

	private String loggedInLicensePlate;
	private ArrayList<HashMap<String, String>> resultRows = new ArrayList<HashMap<String, String>>();

	private LoadSentReportsTask loadSentReportsTask;

	private MenuItem showMapMenuItem;

	//private int currentScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sent_reports);

		rowsAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.row_animation);

		progressLayout = (LinearLayout) findViewById(R.id.sent_reports_progress_layout);
		listSummary = (TextView) findViewById(R.id.sent_reports_summary);

		loggedInLicensePlate = PreferencesUtils.getString(this, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		// Show the Up button in the action bar.
		setupActionBar();

		LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(this, R.anim.row_animation);
		getListView().setLayoutAnimation(controller);

		setupAdapterData();

		handleSavedInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		//		getListView().setOnScrollListener(new OnScrollListener()
		//		{
		//			@Override
		//			public void onScrollStateChanged(AbsListView view, int scrollState)
		//			{
		//				// Check if list stopped flinging
		//				if (currentScrollState == OnScrollListener.SCROLL_STATE_FLING && scrollState != OnScrollListener.SCROLL_STATE_FLING)
		//					new ResolveAddressOnVisualRowsTask().execute();
		//
		//				// Check if user stopped touch scrolling
		//				if (currentScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && scrollState == OnScrollListener.SCROLL_STATE_IDLE)
		//					new ResolveAddressOnVisualRowsTask().execute();
		//
		//				currentScrollState = scrollState;
		//			}
		//
		//			@Override
		//			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
		//			{
		//				// No-op				
		//			}
		//		});
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if (getListAdapter() != null && !getListAdapter().isEmpty())
		{
			ArrayList<HashMap<String, String>> reportList = getRows();
			outState.putSerializable(RESULTS_KEY, reportList);
		}
	}

	/**
	 * PENDING:
	 * 1. Handle orientation change in the middle of paging data from the server
	 * @param savedInstanceState
	 */
	@SuppressWarnings("unchecked")
	private void handleSavedInstanceState(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(RESULTS_KEY))
			{
				ArrayList<HashMap<String, String>> reportRows = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(RESULTS_KEY);
				this.resultRows.addAll(reportRows);
				((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
				updateListSummary();
			}
		}
		else
			loadSentReports();
	}

	private void loadSentReports()
	{
		loadSentReportsTask = new LoadSentReportsTask(this);
		loadSentReportsTask.execute();
	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu)
	//	{
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		getMenuInflater().inflate(R.menu.unsent_reports_actionbar_menu, menu);
	//		showMapMenuItem = menu.getItem(0);
	//
	//		//showMapActionView = MenuItemCompat.getActionView(menu.findItem(R.id.show_report_map));
	//
	//		// Associate searchable configuration with the SearchView
	//		//		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
	//		//		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	//		//		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	//		//		searchView.setIconifiedByDefault(false);
	//
	//		return super.onCreateOptionsMenu(menu);
	//	}

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
	//		case R.id.show_report_map:
	//			if (!resultRows.isEmpty())
	//				showResultsOnMap();
	//			break;
	//
	//		}
	//
	//		return super.onOptionsItemSelected(item);
	//	}

	//	private void showResultsOnMap()
	//	{
	//		Intent showMapIntent = new Intent(this, ReportMapActivity.class);
	//		ArrayList<HashMap<String, String>> reportList = getRows();
	//		showMapIntent.putExtra(ReportMapActivity.EXTRA_REPORT_LIST, reportList);
	//		startActivity(showMapIntent);
	//	}

	private void updateListSummary()
	{
		if (showMapMenuItem != null)
			showMapMenuItem.setEnabled(!resultRows.isEmpty());

		int resultsSize = resultRows.size();
		if (resultsSize > 8)
			listSummary.setText(getString(R.string.list_summary, Integer.valueOf(resultsSize)));
	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class LoadSentReportsTask extends AsyncTask<Void, Void, Void>
	{
		private Context context;

		public LoadSentReportsTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			// We might still have residue of the last cancelled report search 
			resultRows.clear();
			((SimpleAdapter) getListAdapter()).notifyDataSetChanged();

			// Remove empty results message and display progress bar
			getListView().getEmptyView().setVisibility(View.GONE);
			progressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			ServerAPI serverAPI = new ServerAPI(context);
			JSONObject jsonCursor = null;

			do
			{
				List<JSONObject> jsonReportList = serverAPI.getReportsBy(loggedInLicensePlate, 0, 0, jsonCursor);
				jsonCursor = jsonReportList.remove(jsonReportList.size() - 1);

				if (isCancelled())
				{
					Log.d(TAG, "Search Task cancelled!");
					return null;
				}

				ArrayList<HashMap<String, String>> resultAsRows = ReportUtils.getJsonReportsAsListRows(context, jsonReportList);
				Log.i(TAG, "Processed " + resultAsRows.size() + " results for licensePlate " + loggedInLicensePlate);
				resultRows.addAll(resultAsRows);

				// Update UI
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// If this is the first batch, clear the progress indicator
						if (progressLayout.getVisibility() == View.VISIBLE)
						{
							progressLayout.setVisibility(View.GONE);
							getListView().setLayoutAnimation(rowsAnimation);
							//new ResolveAddressOnVisualRowsTask().execute();
						}

						if (!isCancelled())
							((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
					}
				});

			} while (jsonCursor.has(JSONKeys.QUERY_KEY_ENCODED_CURSOR));

			return null;
		}

		@Override
		protected void onPostExecute(Void params)
		{
			// Update summary text only when relevant
			updateListSummary();
		}
	}

	private void setupAdapterData()
	{
		SimpleAdapter adapter = new SimpleAdapter(this, resultRows, R.layout.sent_report_row, new String[]
		{ JSONKeys.REPORT_KEY_DESCRIPTION, JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, JSONKeys.REPORT_KEY_TIME_FOR_DISPLAY }, new int[]
		{ R.id.report_description, R.id.report_address, R.id.report_time });

		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		// Open edit report
		Intent editReportIntent = new Intent(this, EditReportActivity.class);
		editReportIntent.putExtra(EditReportActivity.EXTRA_SELECTED_REPORT, getRows().get(position));
		startActivity(editReportIntent);
	}

	@SuppressWarnings("unchecked")
	private ArrayList<HashMap<String, String>> getRows()
	{
		int rowCount = getListAdapter().getCount();
		ArrayList<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>(rowCount);
		for (int i = 0; i < rowCount; i++)
		{
			HashMap<String, String> row = (HashMap<String, String>) getListAdapter().getItem(i);
			results.add(row);
		}

		return results;
	}

	//	private class ResolveAddressOnVisualRowsTask extends AsyncTask<Void, Void, Void>
	//	{
	//		public ResolveAddressOnVisualRowsTask()
	//		{
	//
	//		}
	//
	//		@Override
	//		protected void onPreExecute()
	//		{
	//			// In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
	//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
	//			{
	//				if (!Geocoder.isPresent())
	//				{
	//					cancel(false);
	//
	//					Log.e(TAG, "Geocode.isPresent() returned false ! Cannot resolve address");
	//
	//					// No geocoder is present. Issue an error message (Happens on Genymotion emulator)
	//					Toast.makeText(SentReportsActivity.this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
	//				}
	//			}
	//		}
	//
	//		/**
	//		 * Get a geocoding service instance, pass latitude and longitude to it, format the returned address, and return the address to the UI thread.
	//		 */
	//		@SuppressWarnings("unchecked")
	//		@Override
	//		protected Void doInBackground(Void... params)
	//		{
	//			int from = getListView().getFirstVisiblePosition();
	//			int to = getListView().getLastVisiblePosition();
	//			Log.d(TAG, "Starting to resolve address from " + from + " to " + to);
	//
	//			for (int i = from; i <= to; i++)
	//			{
	//				if (isCancelled())
	//					return null;
	//
	//				getListAdapter().getItem(i);
	//				HashMap<String, String> row = (HashMap<String, String>) getListAdapter().getItem(i);
	//
	//				// Check if this row might already have an address (may happen when, for example, user changed device orientation while this task is running)
	//				if (row.get(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY) == null)
	//				{
	//					double latitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LATITUDE));
	//					double longtitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LONGITUDE));
	//					Address address = ReportUtils.resolveLocationToAddress(SentReportsActivity.this, latitude, longtitude);
	//
	//					// Check if the reverse geocode returned an address
	//					String addressStr = ReportUtils.getSingleLineAddress(SentReportsActivity.this, address);
	//					row.put(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, addressStr);
	//					updateUI();
	//				}
	//			}
	//
	//			updateUI();
	//
	//			return null;
	//		}
	//
	//		private void updateUI()
	//		{
	//			runOnUiThread(new Runnable()
	//			{
	//				@Override
	//				public void run()
	//				{
	//					if (!isCancelled())
	//						((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
	//				}
	//			});
	//		}
	//	}
}