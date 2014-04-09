package com.roadwatch.app.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.map.ReportMapActivity;
import com.roadwatch.app.report.EditReportActivity.ReportViewMode;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;

/**
 * PENDING:
 * 1. Allow user to delete a sent report.
 * 2. Refresh sent reports after a successful update
 * 
 * @author Nati created at : 00:04:48 08/11/2013
 *
 */
public class SentReportsFragment extends ListFragment
{
	private static final String TAG = SentReportsFragment.class.getSimpleName();

	private static final String RESULTS_KEY = "results";

	public static final String LAST_24_HOURS_KEY = "last_24_hours";

	private LayoutAnimationController rowsAnimation;

	private LinearLayout progressLayout;
	private TextView listSummary;

	private String loggedInLicensePlate;
	private ArrayList<HashMap<String, String>> resultRows = new ArrayList<HashMap<String, String>>();

	private LoadSentReportsTask loadSentReportsTask;

	private MenuItem showMapMenuItem;

	private boolean last24Hours;

	public SentReportsFragment()
	{
		super();
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (container == null)
		{
			// Currently in a layout without a container, so no reason to create our view.
			return null;
		}

		rowsAnimation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.row_animation);

		View rootView = inflater.inflate(R.layout.sent_reports, container, false);

		progressLayout = (LinearLayout) rootView.findViewById(R.id.sent_reports_progress_layout);
		listSummary = (TextView) rootView.findViewById(R.id.sent_reports_summary);

		loggedInLicensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

		last24Hours = getArguments().getBoolean(LAST_24_HOURS_KEY, false);

		setupAdapterData();

		return rootView;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onViewStateRestored(Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);

		// For the old history of send reports we only load it once
		if (!last24Hours)
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
				else
				{
					loadSentReports();
				}
			}
			else
				loadSentReports();
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

	private void loadSentReports()
	{
		loadSentReportsTask = new LoadSentReportsTask(getActivity(), last24Hours);
		loadSentReportsTask.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.unsent_reports_actionbar_menu, menu);
		showMapMenuItem = menu.getItem(0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.show_report_map:
			if (!resultRows.isEmpty())
				showResultsOnMap();
			break;

		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// For the 24-hours history - we refresh it every time it is shown (to reflect changes in reports that were edited or removed)
		if (last24Hours)
			loadSentReports();
	}

	private void showResultsOnMap()
	{
		Intent showMapIntent = new Intent(getActivity(), ReportMapActivity.class);
		ArrayList<HashMap<String, String>> reportList = getRows();
		showMapIntent.putExtra(ReportMapActivity.EXTRA_REPORT_LIST, reportList);
		startActivity(showMapIntent);
	}

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
		private boolean last24Hours;

		public LoadSentReportsTask(Context context, boolean last24Hours)
		{
			this.context = context;
			this.last24Hours = last24Hours;
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

		private long getFromTime()
		{
			long now = System.currentTimeMillis();
			long twentyFourHours = 24 * 60 * 60 * 1000;
			return last24Hours ? now - twentyFourHours : 0;
		}

		private long getToTime()
		{
			long now = System.currentTimeMillis();
			long twentyFourHours = 24 * 60 * 60 * 1000;

			return last24Hours ? now : now - twentyFourHours;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			ServerAPI serverAPI = new ServerAPI(context);
			JSONObject jsonCursor = null;

			do
			{
				List<JSONObject> jsonReportList = serverAPI.getReportsBy(loggedInLicensePlate, getFromTime(), getToTime(), jsonCursor);
				jsonCursor = jsonReportList.remove(jsonReportList.size() - 1);

				if (isCancelled())
				{
					Log.d(TAG, "Search Task cancelled!");
					return null;
				}

				ArrayList<HashMap<String, String>> resultAsRows = ReportUtils.getJsonReportsAsListRows(context, jsonReportList);
				Log.i(TAG, "Processed " + resultAsRows.size() + " results for licensePlate " + loggedInLicensePlate);
				resultRows.addAll(resultAsRows);

				// Check that we didn't get detached while running
				Activity activity = SentReportsFragment.this.getActivity();
				if (activity != null)
				{
					// Update UI
					activity.runOnUiThread(new Runnable()
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
				}

			} while (jsonCursor.has(JSONKeys.QUERY_KEY_ENCODED_CURSOR));

			return null;
		}

		@Override
		protected void onPostExecute(Void params)
		{
			if (getActivity() != null)
				updateListSummary();
		}
	}

	private void setupAdapterData()
	{
		SimpleAdapter adapter = new SimpleAdapter(getActivity(), resultRows, R.layout.sent_report_row, new String[]
		{ JSONKeys.REPORT_KEY_DESCRIPTION, JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, JSONKeys.REPORT_KEY_TIME_FOR_DISPLAY }, new int[]
		{ R.id.report_description, R.id.report_address, R.id.report_time });

		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		// Open edit report
		Intent editReportIntent = new Intent(getActivity(), EditReportActivity.class);
		editReportIntent.putExtra(EditReportActivity.EXTRA_SELECTED_REPORT, getRows().get(position));
		editReportIntent.putExtra(EditReportActivity.EXTRA_REPORT_VIEW_MODE, last24Hours ? ReportViewMode.FIX_SENT_REPORT : ReportViewMode.READ_ONLY);
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
}