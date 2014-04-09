package com.roadwatch.app;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class TabListener<T extends Fragment> implements ActionBar.TabListener
{
	//private Fragment fragment;
	//private final Activity activity;
	private final String tag;
	//private final Class<T> mClass;
	private ViewPager pager;

	/** Constructor used each time a new tab is created.
	  * @param activity  The host Activity, used to instantiate the fragment
	  * @param tag  The identifier tag for the fragment
	  * @param clz  The fragment's Class, used to instantiate the fragment
	  */
	public TabListener(Activity activity, String tag, ViewPager pager)
	{
		//this.activity = activity;
		this.tag = tag;
		this.pager = pager;
	}

	/** 
	 * The following are each of the <code>ActionBar.TabListener</code> callbacks 
	 **/
	public void onTabSelected(Tab tab, FragmentTransaction ft)
	{
		pager.setCurrentItem(Integer.parseInt(tag));
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{
		/*if (mFragment != null) {
		    // Detach the fragment, because another one is being attached
		    ft.detach(mFragment);
			}*/
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{
		// User selected the already selected tab. Usually do nothing.
	}
}