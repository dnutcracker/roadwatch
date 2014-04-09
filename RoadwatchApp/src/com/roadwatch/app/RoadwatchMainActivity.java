package com.roadwatch.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.roadwatch.app.report.ReportActivity;
import com.roadwatch.app.report.ReportsFragment;
import com.roadwatch.app.report.SentReportsPagerActivity;
import com.roadwatch.app.report.UnsentReportsActivity;
import com.roadwatch.app.settings.SettingsActivity;
import com.roadwatch.app.tracked.TrackedCarsFragment;
import com.roadwatch.app.util.JSONUtils;
import com.roadwatch.app.util.Utils;

/**
 * 
 * 
 */
public class RoadwatchMainActivity extends ActionBarActivity implements OnSharedPreferenceChangeListener
{
	// Log tag for this activity
	//private static final String ACT_TAG = RoadwatchMainActivity.class.getSimpleName();

	public static final String PROPERTY_APP_VERSION = "appVersion";
	public static final String PROPERTY_GCM_REGISTRATION_ID = "gcmRegistrationID";
	public static final String PROPERTY_LOGIN_TOKEN = "loginToken";
	public static final String PROPERTY_USER_NAME = "username";
	public static final String PROPERTY_USER_EMAIL = "email";
	public static final String PROPERTY_USER_LICENSE_PLATE = "userLicensePlate";

	private MyAdapter pagerAdapter;
	private ViewPager viewPager;
	public ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Make the action bar use the activity bg
		//getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.main_pager);

		actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		pagerAdapter = new MyAdapter(getSupportFragmentManager());
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageScrollStateChanged(int arg0)
			{

			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{

			}

			@Override
			public void onPageSelected(int index)
			{
				actionBar.getTabAt(index).select();
				if (index != 1)
					Utils.closeSoftKeyboard(RoadwatchMainActivity.this, viewPager.getWindowToken());
			}
		});

		Tab reportsTab = actionBar.newTab().setText(getString(R.string.reports)).setTabListener(new TabListener<Fragment>(this, 0 + "", viewPager));
		actionBar.addTab(reportsTab);
		Tab searchTab = actionBar.newTab().setText(getString(R.string.search)).setTabListener(new TabListener<Fragment>(this, 1 + "", viewPager));
		actionBar.addTab(searchTab);
		Tab myCarsTab = actionBar.newTab().setText(getString(R.string.tracked_cars)).setTabListener(new TabListener<Fragment>(this, 2 + "", viewPager));
		actionBar.addTab(myCarsTab);
		//Tab sharingTab = actionBar.newTab().setText(getString(R.string.sharing)).setTabListener(new TabListener<Fragment>(this, 3 + "", viewPager));
		//actionBar.addTab(sharingTab);
	}

	//	@Override
	//	protected void onNewIntent(Intent intent)
	//	{
	//		super.onNewIntent(intent);
	//
	//		// Get the intent, verify the action and get the query
	//		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
	//		{
	//			// Switch to  search results tab
	//			actionBar.getTabAt(1).select();
	//
	//			// Send the searched license plate
	//			String query = intent.getStringExtra(SearchManager.QUERY);
	//			searchFragment.setSearchedLicensePlate(query);
	//			Log.d(ACT_TAG, "Search for : " + query);
	//		}
	//	}

	/**
	 *   
	 */
	@Override
	public void onBackPressed()
	{
		// If not on first tab - go back to the first tab
		if (viewPager.getCurrentItem() > 0)
		{
			viewPager.setCurrentItem(0, true);
		}
		else
		{
			int unsentReportsNumber = JSONUtils.loadJSONObjects(this, JSONUtils.UNSENT_REPORTS_FILENAME).size();
			if (unsentReportsNumber > 0)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				String exitMessage = unsentReportsNumber == 1 ? getString(R.string.exit_dialog_message_single) : getString(R.string.exit_dialog_message_multiple, Integer.valueOf(unsentReportsNumber));
				builder.setTitle(R.string.exit_dialog_title).setIcon(R.drawable.rw_launcher).setMessage(exitMessage);
				builder.setNegativeButton(R.string.exit_dialog_cancel_button, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						// Do nothing
					}
				});
				builder.setPositiveButton(R.string.exit_dialog_exit_button, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						RoadwatchMainActivity.super.onBackPressed();
					}
				});

				builder.create().show();
			}
			else
				RoadwatchMainActivity.super.onBackPressed();
		}
	}

	/*
	 * Called when the Activity is restarted, even before it becomes visible.
	 */
	@Override
	public void onStart()
	{
		super.onStart();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		ApplicationData.getTracker().set(Fields.SCREEN_NAME, "Main Activity");

		ApplicationData.getTracker().send(MapBuilder.createAppView().build());
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressLint("NewApi")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (SettingsActivity.DISPLAY_SETTINGS_LANGUAGE_PREF_KEY.equals(key))
		{
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			{
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
			else
				recreate();
		}
	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu)
	//	{
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		getMenuInflater().inflate(R.menu.main_actionbar_menu, menu);
	//
	//		if (Utils.isDebugAPK(this))
	//		{
	//			MenuItem item = menu.getItem(0);
	//			if (item != null)
	//				item.setVisible(true);
	//		}
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
	//		case R.id.action_settings:
	//			Intent intent = new Intent(this, SettingsActivity.class);
	//			startActivity(intent);
	//			return true;
	//			//		case R.id.debug_stress_server_test:
	//			//			intent = new Intent(this, ServerStressTestActivity.class);
	//			//			startActivity(intent);
	//			//			return true;
	//		}
	//
	//		return super.onOptionsItemSelected(item);
	//	}

	public class MyAdapter extends FragmentPagerAdapter
	{
		public MyAdapter(FragmentManager fm)
		{
			super(fm);
		}

		//		@Override
		//		public Object instantiateItem(ViewGroup container, int position)
		//		{
		//			Fragment fragment = (Fragment) super.instantiateItem(container, position);
		//			switch (position)
		//			{
		//								case 0:
		//									reportsFragemnt = (ReportsFragment) fragment;
		//									break;
		//								case 1:
		//									searchFragment = (SearchFragment) fragment;
		//									break;
		//								case 2:
		//									notificationsFragment = (NotificationsFragment) fragment;
		//									break;
		//			}
		//			return fragment;
		//		}

		@Override
		public int getCount()
		{
			return 3;
		}

		@Override
		public android.support.v4.app.Fragment getItem(int position)
		{
			switch (position)
			{
			case 0:
				return new ReportsFragment();
			case 1:
				return new SearchFragment();
			case 2:
				return new TrackedCarsFragment();
			case 3:
				return new SharingFragment();
			}

			return null;
		}
	}

	///////////////////////////////////////// Methods defined in the layout XML /////////////////////////////////////////

	/**
	 * Invoked by the "Report!" button.
	 * 
	 * Starts the report activity.
	 * 
	 * @param v The view object associated with this method, in this case a Button.
	 */
	public void report(View v)
	{
		// Show report activity
		Intent intent = new Intent(this, ReportActivity.class);
		startActivity(intent);
	}

	public void showUnsentReports(View v)
	{
		// Show unset reports activity
		Intent intent = new Intent(this, UnsentReportsActivity.class);
		startActivity(intent);
	}

	public void showSentReports(View v)
	{
		// Show sent reports activity
		Intent intent = new Intent(this, SentReportsPagerActivity.class);
		startActivity(intent);
	}
}