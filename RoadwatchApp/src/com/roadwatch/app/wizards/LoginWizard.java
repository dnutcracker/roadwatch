package com.roadwatch.app.wizards;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.layouts.BasicWizardLayout;
import org.codepond.wizardroid.persistence.ContextVariable;

import com.roadwatch.app.wizard.steps.LoginStep1;
import com.roadwatch.app.wizard.steps.LoginStep2;
import com.roadwatch.app.wizard.steps.LoginStep3;

/**
 * A sample to demonstrate a form in multiple steps.
 */
public class LoginWizard extends BasicWizardLayout
{
	/**
	 * Tell WizarDroid that these are context variables and set default values.
	 * These values will be automatically bound to any field annotated with {@link ContextVariable}.
	 * NOTE: Context Variable names are unique and therefore must
	 * have the same name and type wherever you wish to use them.
	 */
	@ContextVariable
	private String licensePlate;

	public LoginWizard()
	{
		super();
	}

	@Override
	public WizardFlow onSetup()
	{
		return new WizardFlow.Builder().addStep(LoginStep1.class).addStep(LoginStep2.class, true).addStep(LoginStep3.class).create();
	}

	@Override
	public void onWizardComplete()
	{
		// We let the last page finish the wizard		
		//super.onWizardComplete();
	}
}