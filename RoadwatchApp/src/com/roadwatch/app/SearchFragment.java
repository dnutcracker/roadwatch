package com.roadwatch.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.MapBuilder;
import com.roadwatch.app.location.ResolveAddressTask;
import com.roadwatch.app.map.ReportMapActivity;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.ReportUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.widgets.LicensePlateEditText;

/**
 * Display the license plate search results
 */
public class SearchFragment extends ListFragment
{
	// Logging tag
	private static final String TAG = SearchFragment.class.getSimpleName();

	private static final String RESULTS_KEY = "results";
	private static final String NEED_TO_RESOLVE_ADDRESS_KEY = "resolve_address";

	private LinearLayout progressLayout;
	private LicensePlateEditText searchLicensePlate;
	private ImageButton searchButton;
	private TextView searchSummary;

	private boolean searchPerformed;

	private ResolveAddressTask resolveAddressTask;

	private LayoutAnimationController rowsAnimation;

	private ArrayList<HashMap<String, String>> resultRows = new ArrayList<HashMap<String, String>>();

	private SearchReportsTask searchReportsTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (container == null)
		{
			// Currently in a layout without a container, so no reason to create our view.
			return null;
		}

		rowsAnimation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.row_animation);

		View rootView = inflater.inflate(R.layout.search_fragment, container, false);

		progressLayout = (LinearLayout) rootView.findViewById(R.id.search_progress_layout);
		searchLicensePlate = (LicensePlateEditText) rootView.findViewById(R.id.license_plate);
		searchLicensePlate.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		searchButton = (ImageButton) rootView.findViewById(R.id.action_button);
		searchSummary = (TextView) rootView.findViewById(R.id.search_summary);
		searchButton.setImageResource(R.drawable.ic_action_search);

		setupAdapterData();

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.search_actionbar_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// handle item selection
		switch (item.getItemId())
		{
		case R.id.action_search_fav:
			// Google Analytics
			ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "touch", "Search My Car", null).build());

			// Search using the user's license plate
			String loggedInLicensePlate = PreferencesUtils.getString(getActivity(), RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);
			searchReports(loggedInLicensePlate);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onViewStateRestored(Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);

		searchButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearPreviousSearch();

				String licensePlate = searchLicensePlate.getText().toString();
				if (licensePlate.isEmpty() || licensePlate.length() == 9)
				{
					if (licensePlate.length() == 9)
					{
						if (!searchPerformed)
						{
							// User clicked the 'search' button
							Utils.closeSoftKeyboard(getActivity(), searchLicensePlate.getWindowToken());

							searchReports();
						}
						else
						{
							// User clicked the 'clear search' button
							searchLicensePlate.setText("");
							Utils.openSoftKeyboard(getActivity(), searchLicensePlate.getWindowToken());
						}
					}
					else
					// Open keyboard if user presses 'search' on an empty license plate
					{
						Utils.openSoftKeyboard(getActivity(), searchLicensePlate.getWindowToken());
					}
				}
				else
				// action is 'clear'
				{
					searchLicensePlate.setText("");
				}
			}
		});

		LicensePlateUtils.configureLicensePlateEditText(searchLicensePlate);
		searchLicensePlate.addTextChangedListener(new TextWatcher()
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
				// If user typed in - we'll need to perform a new search
				searchPerformed = false;

				boolean emptyOfFull = s.length() == 0 || s.length() == 9;
				searchButton.setImageResource(emptyOfFull ? R.drawable.ic_action_search : R.drawable.ic_action_remove);

				if (s.length() == 9)
					searchButton.performClick();
			}
		});

		searchLicensePlate.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_SEARCH)
				{
					String licensePlate = searchLicensePlate.getText().toString();
					if (licensePlate.length() == 9)
					{
						clearPreviousSearch();
						Utils.closeSoftKeyboard(getActivity(), searchButton.getWindowToken());
						searchReports();
						return true;
					}
				}

				return false;
			}
		});

		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(RESULTS_KEY))
			{
				ArrayList<HashMap<String, String>> reportRows = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(RESULTS_KEY);
				resultRows.addAll(reportRows);
				((SearchResultsAdapter) getListAdapter()).notifyDataSetChanged();
				updateListSummary();
			}

			// Reactivate task if necessary (it will resolve only not yet resolved addresses)
			if (savedInstanceState.containsKey(NEED_TO_RESOLVE_ADDRESS_KEY))
			{
				resolveAddressTask = new ResolveAddressTask(getActivity(), getListView());
				resolveAddressTask.execute();
			}
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		showResultsOnMap(position);
	}

	private void showResultsOnMap(int selectedResultIndex)
	{
		Intent showMapIntent = new Intent(getActivity(), ReportMapActivity.class);
		showMapIntent.putExtra(ReportMapActivity.EXTRA_SELECTED_REPORT_INDEX, selectedResultIndex);

		ArrayList<HashMap<String, String>> reportList = getRows();
		showMapIntent.putExtra(ReportMapActivity.EXTRA_REPORT_LIST, reportList);
		startActivity(showMapIntent);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		if (getListAdapter() != null && !getListAdapter().isEmpty())
		{
			ArrayList<HashMap<String, String>> reportList = getRows();
			outState.putSerializable(RESULTS_KEY, reportList);
		}

		// Check if resolve address task is currently running
		if (resolveAddressTask != null && resolveAddressTask.getStatus() != Status.FINISHED)
		{
			outState.putBoolean(NEED_TO_RESOLVE_ADDRESS_KEY, true);
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		stopRunningTasks();
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

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "Search fragment destroyed");
	}

	private void clearPreviousSearch()
	{
		searchSummary.setText("");

		stopRunningTasks();

		// Clear previous search
		resultRows.clear();

		// Update view
		((SearchResultsAdapter) SearchFragment.this.getListAdapter()).notifyDataSetChanged();
	}

	private void stopRunningTasks()
	{
		// Stop fetching results for previous search (if active)
		if (searchReportsTask != null && searchReportsTask.getStatus() != Status.FINISHED)
			searchReportsTask.cancel(true);

		// Stop address resolving for previous search (if active)
		if (resolveAddressTask != null && resolveAddressTask.getStatus() != Status.FINISHED)
			resolveAddressTask.cancel(true);
	}

	private void searchReports()
	{
		// Google Analytics
		ApplicationData.getTracker().send(MapBuilder.createEvent("UX", "Touch", "Search", null).build());

		searchReportsTask = new SearchReportsTask(getActivity());
		searchReportsTask.execute(searchLicensePlate.getText().toString());
	}

	/** 
	 * Invoked by RoadWatchMainActivity when pressing the 'My Record' button
	 * @param licensePlate
	 */
	/*package*/void searchReports(String licensePlate)
	{
		searchLicensePlate.setText(licensePlate);
	}

	private void updateListSummary()
	{
		int resultsSize = resultRows.size();
		if (resultsSize > 8)
			searchSummary.setText(getString(R.string.list_summary, Integer.valueOf(resultsSize)));
	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class SearchReportsTask extends AsyncTask<String, Void, Void>
	{
		private Context context;

		public SearchReportsTask(Context context)
		{
			this.context = context;
		}

		@Override
		protected void onPreExecute()
		{
			// We might still have residue of the last cancelled report search 
			resultRows.clear();
			((SearchResultsAdapter) SearchFragment.this.getListAdapter()).notifyDataSetChanged();

			// Remove empty results message and display progress bar
			getListView().getEmptyView().setVisibility(View.GONE);
			progressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(String... params)
		{
			ServerAPI serverAPI = new ServerAPI(getActivity());
			String licensePlate = params[0];
			JSONObject jsonCursor = null;

			do
			{

				List<JSONObject> jsonReportList = serverAPI.getReportsOn(licensePlate, jsonCursor);
				jsonCursor = jsonReportList.remove(jsonReportList.size() - 1);

				if (isCancelled())
				{
					Log.d(TAG, "Search Task cancelled!");
					return null;
				}

				ArrayList<HashMap<String, String>> resultAsRows = ReportUtils.getJsonReportsAsListRows(context, jsonReportList);
				Log.i(TAG, "Processed " + resultAsRows.size() + " results for licensePlate " + licensePlate);
				resultRows.addAll(resultAsRows);

				// Check that we didn't get detached while running
				Activity activity = SearchFragment.this.getActivity();
				if (activity != null)
				{
					// Update UI
					activity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							// clear the progress indicator if visible (first batch arrived)
							if (progressLayout.getVisibility() == View.VISIBLE)
							{
								searchPerformed = true;
								progressLayout.setVisibility(View.GONE);
								getListView().setLayoutAnimation(rowsAnimation);
								// Switch search icon with 'X' icon to allow clearing the text field
								searchButton.setImageResource(R.drawable.ic_action_remove);
							}

							if (!isCancelled())
								((SearchResultsAdapter) SearchFragment.this.getListAdapter()).notifyDataSetChanged();
						}
					});
				}
			} while (jsonCursor.has(JSONKeys.QUERY_KEY_ENCODED_CURSOR));

			return null;
		}

		@Override
		protected void onPostExecute(Void params)
		{
			// Make sure we weren't not detached
			if (getActivity() != null)
			{
				updateListSummary();

				// Start resolving address (PENDING: Do it incrementally instead of at the end)
				resolveAddressTask = new ResolveAddressTask(getActivity(), getListView());
				resolveAddressTask.execute();
			}
		}
	}

	private void setupAdapterData()
	{
		setListAdapter(new SearchResultsAdapter(getActivity(), resultRows));
	}

	//	/**
	//	 * An AsyncTask that calls getFromLocation() in the background. The class uses the following generic types: Location - A {@link android.location.Location} object containing the current location,
	//	 * passed as the input parameter to doInBackground() Void - indicates that progress units are not used by this subclass String - An address passed to onPostExecute()
	//	 */
	//	private class ResolveAddressTask extends AsyncTask<Void, Void, Void>
	//	{
	//		private SearchFragment parentFragment;
	//
	//		// Constructor called by the system to instantiate the task
	//		public ResolveAddressTask(SearchFragment parentFragment)
	//		{
	//			this.parentFragment = parentFragment;
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
	//					Toast.makeText(parentFragment.getActivity(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
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
	//			int rowCount = parentFragment.getListAdapter().getCount();
	//			for (int i = 0; i < rowCount; i++)
	//			{
	//				if (isCancelled())
	//					return null;
	//
	//				parentFragment.getListAdapter().getItem(i);
	//				HashMap<String, String> row = (HashMap<String, String>) getListAdapter().getItem(i);
	//
	//				// Check if this row might already have an address (may happen when, for example, user changed device orientation while this task is running)
	//				if (row.get(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY) == null)
	//				{
	//					double latitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LATITUDE));
	//					double longtitude = Double.parseDouble(row.get(JSONKeys.REPORT_KEY_LONGITUDE));
	//					Address address = ReportUtils.resolveLocationToAddress(parentFragment.getActivity(), latitude, longtitude);
	//
	//					// Check if the reverse geocode returned an address
	//					String addressStr = ReportUtils.getSingleLineAddress(parentFragment.getActivity(), address);
	//					row.put(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, addressStr);
	//
	//					int top = getListView().getFirstVisiblePosition();
	//					int bottom = getListView().getLastVisiblePosition();
	//
	//					// Update visible elements as fast as they come
	//					if (i >= top && i <= bottom)
	//						updateUI();
	//					else // Update out of screen elements in groups of 7s
	//					if (i % 7 == 0)
	//						updateUI();
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
	//			// Fragment might be detached while we're running
	//			if (parentFragment.getActivity() != null)
	//			{
	//				parentFragment.getActivity().runOnUiThread(new Runnable()
	//				{
	//					@Override
	//					public void run()
	//					{
	//						if (!isCancelled())
	//							((SearchResultsAdapter) parentFragment.getListAdapter()).notifyDataSetChanged();
	//					}
	//				});
	//			}
	//		}
	//	}
}