package com.roadwatch.app.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.roadwatch.app.R;
import com.roadwatch.app.integration.NavigationIntegrationService;
import com.roadwatch.app.location.ResolveAddressTask;
import com.roadwatch.app.map.ReportMapActivity;
import com.roadwatch.app.report.EditReportActivity.ReportViewMode;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.ReportUtils;

/**
 * 
 * @author Nati created at : 30 בספט 2013 
 */
public class UnsentReportsActivity extends ListActivity
{
	private static final String TAG = UnsentReportsActivity.class.getSimpleName();

	private ArrayList<HashMap<String, String>> reportRows = new ArrayList<HashMap<String, String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.unsent_reports);

		// Show the Up button in the action bar.
		setupActionBar();

		//getListView().setLayoutTransition(new LayoutTransition());
		LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(this, R.anim.row_animation);
		getListView().setLayoutAnimation(controller);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			addContextMenuActions();

		cancelNotification();

		//buildUnsentReportsList();
	}

	private void cancelNotification()
	{
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NavigationIntegrationService.UNSENT_REPORTS_NOTIFICATION_ID);
	}

	@SuppressLint("NewApi")
	private void addContextMenuActions()
	{
		ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener()
		{

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
			{
				// Here you can do something when items are selected/de-selected,
				// such as update the title in the CAB
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item)
			{
				// Respond to clicks on the actions in the CAB
				switch (item.getItemId())
				{
				case R.id.delete_report:
					deleteSelectedReports();
					mode.finish(); // Action picked, so close the CAB
					if (getRows().isEmpty())
						finish();
					return true;
				default:
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu)
			{
				// Inflate the menu for the CAB
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.unsent_reports_selection_actionbar_menu, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode)
			{
				// Here you can make any necessary updates to the activity when
				// the CAB is removed. By default, selected items are deselected/unchecked.
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu)
			{
				// Here you can perform updates to the CAB due to
				// an invalidate() request
				return false;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void deleteSelectedReports()
	{
		SparseBooleanArray selectedItems = getListView().getCheckedItemPositions();
		ArrayList<HashMap<String, String>> reportsToRemove = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < selectedItems.size(); i++)
		{
			// Check if we have a mapping at this index
			if (selectedItems.valueAt(i))
			{
				// Get the mapped value
				int position = selectedItems.keyAt(i);
				// Get the report itself
				Object item = getListAdapter().getItem(position);
				reportsToRemove.add((HashMap<String, String>) item);
				Log.d(TAG, "Removing unsent report : " + item);
			}
		}

		// Remove the reports from local storage
		for (HashMap<String, String> hashMap : reportsToRemove)
		{
			long reportTime = Long.parseLong(hashMap.get(JSONKeys.REPORT_KEY_TIME));
			ReportUtils.deleteReport(this, reportTime);
		}

		// Rebuild list from storage
		buildUnsentReportsList();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.unsent_reports_selection_actionbar_menu, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.unsent_reports_actionbar_menu, menu);

		// Associate searchable configuration with the SearchView
		//		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		//		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		//		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		//		searchView.setIconifiedByDefault(false);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;

		case R.id.show_report_map:
			showResultsOnMap();
			break;

		}

		return super.onOptionsItemSelected(item);
	}

	private void showResultsOnMap()
	{
		Intent showMapIntent = new Intent(this, ReportMapActivity.class);
		ArrayList<HashMap<String, String>> reportList = getRows();
		showMapIntent.putExtra(ReportMapActivity.EXTRA_REPORT_LIST, reportList);
		startActivity(showMapIntent);
	}

	private void buildUnsentReportsList()
	{
		reportRows.clear();

		List<JSONObject> reports = JSONUtils.loadJSONObjects(this, JSONUtils.UNSENT_REPORTS_FILENAME);
		ArrayList<HashMap<String, String>> rows = ReportUtils.getJsonReportsAsListRows(this, reports);
		reportRows.addAll(rows);

		SimpleAdapter adapter = new SimpleAdapter(this, reportRows, R.layout.report_row, new String[]
		{ JSONKeys.REPORT_KEY_LICENSE_PLATE, JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY, JSONKeys.REPORT_KEY_TIME_FOR_DISPLAY }, new int[]
		{ R.id.row_title, R.id.row_details1, R.id.row_details2 });

		setListAdapter(adapter);

		new ResolveAddressTask(this, getListView()).execute();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent editReportIntent = new Intent(this, EditReportActivity.class);
		editReportIntent.putExtra(EditReportActivity.EXTRA_SELECTED_REPORT, getRows().get(position));
		editReportIntent.putExtra(EditReportActivity.EXTRA_REPORT_VIEW_MODE, ReportViewMode.EDIT_BEFORE_SENDING);
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

	@Override
	protected void onResume()
	{
		super.onResume();

		buildUnsentReportsList();

		// If we dont have any more unsent reports, go back
		if (getRows().isEmpty())
			finish();
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
}