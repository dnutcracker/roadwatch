package com.roadwatch.app.report;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.roadwatch.app.R;
import com.roadwatch.app.TabListener;
import com.roadwatch.app.util.Utils;

/**
 * Show the reports sent by the user divided into 'last 24 hours' and 'older'
 * 
 * @author Nati created at : 16:21:12. 23/12/2013
 */
public class SentReportsPagerActivity extends ActionBarActivity
{
	// Log tag for this activity
	private static final String ACT_TAG = SentReportsPagerActivity.class.getSimpleName();

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

		setContentView(R.layout.sent_reports_pager);

		actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		pagerAdapter = new MyAdapter(getSupportFragmentManager());
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);
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
				Log.d(ACT_TAG, "onPageSelected: " + index);
				actionBar.getTabAt(index).select();
				if (index != 1)
					Utils.closeSoftKeyboard(SentReportsPagerActivity.this, viewPager.getWindowToken());
			}
		});

		Tab last24HoursReportsTab = actionBar.newTab().setText(R.string.last_24_hours_reports).setTabListener(new TabListener<android.support.v4.app.Fragment>(this, 0 + "", viewPager));
		actionBar.addTab(last24HoursReportsTab);
		Tab olderReportsTab = actionBar.newTab().setText(R.string.older_reports).setTabListener(new TabListener<android.support.v4.app.Fragment>(this, 1 + "", viewPager));
		actionBar.addTab(olderReportsTab);
	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu)
	//	{
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		getMenuInflater().inflate(R.menu.sent_reports_actionbar_menu, menu);
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

	private class MyAdapter extends FragmentPagerAdapter
	{
		public MyAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public android.support.v4.app.Fragment getItem(int position)
		{
			SentReportsFragment sentReportsFragment = new SentReportsFragment();
			Bundle args = new Bundle();
			switch (position)
			{
			case 0:
				args.putBoolean(SentReportsFragment.LAST_24_HOURS_KEY, true);
				sentReportsFragment.setArguments(args);
				return sentReportsFragment;
			case 1:
				sentReportsFragment.setArguments(args);
				return sentReportsFragment;
			}

			return null;
		}
	}
}