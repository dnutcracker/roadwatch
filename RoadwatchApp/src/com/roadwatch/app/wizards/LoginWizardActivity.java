package com.roadwatch.app.wizards;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.roadwatch.app.R;

public class LoginWizardActivity extends FragmentActivity
{
	private Fragment fragmentStep;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.login_wizard);
	}

	@Override
	public void onBackPressed()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String skipMessage = "Only registered users can send reports or recieve notifications. It only takes a few seconds!";
		builder.setTitle("Skip Registration ?").setIcon(R.drawable.rw_launcher).setMessage(skipMessage);
		builder.setNegativeButton("Fine, I'll register", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Do nothing
			}
		});
		builder.setPositiveButton("I said skip!", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				LoginWizardActivity.super.onBackPressed();
			}
		});

		builder.create().show();
	}

	public void setOnFragmentResult(Fragment fragmentStep)
	{
		this.fragmentStep = fragmentStep;
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2)
	{
		fragmentStep.onActivityResult(arg0, arg1, arg2);
	}

	//	@SuppressWarnings("deprecation")
	//	@Override
	//	protected Dialog onCreateDialog(int id)
	//	{
	//		if (id != LoginStep3.DIALOG_GET_GOOGLE_PLAY_SERVICES)
	//		{
	//			return super.onCreateDialog(id);
	//		}
	//
	//		int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	//		if (available == ConnectionResult.SUCCESS)
	//		{
	//			return null;
	//		}
	//		if (GooglePlayServicesUtil.isUserRecoverableError(available))
	//		{
	//			return GooglePlayServicesUtil.getErrorDialog(available, this, LoginStep3.REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES);
	//		}
	//		return new AlertDialog.Builder(this).setMessage("Sign in with Google is not available").setCancelable(true).create();
	//	}
}