package com.roadwatch.app.wizard.steps;

import org.codepond.wizardroid.WizardStep;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roadwatch.app.R;

public class LoginStep1 extends WizardStep
{
	public LoginStep1()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.login_wizard_step1, container, false);
		return v;
	}
}
