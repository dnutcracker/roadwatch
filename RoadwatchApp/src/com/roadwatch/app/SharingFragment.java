package com.roadwatch.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 *  
 */
public class SharingFragment extends Fragment
{
	// Logging tag
	//private static final String ACT_TAG = SharingFragment.class.getSimpleName();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (container == null)
		{
			// Currently in a layout without a container, so no reason to create our view.
			return null;
		}

		View rootView = inflater.inflate(R.layout.sharing_fragment, container, false);

		return rootView;
	}
}