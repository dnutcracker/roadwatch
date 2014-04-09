package com.roadwatch.app.wizard.steps;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.roadwatch.app.R;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;
import com.roadwatch.app.util.Utils;
import com.roadwatch.app.widgets.LicensePlateEditText;

/**
 * This step is used to acquire the user's license plate number.
 */
public class LoginStep2 extends WizardStep
{
	// Logging tag
	private static final String TAG = LoginStep2.class.getSimpleName();
	public static final String LOGIN_WIZARD_LICENSE_PLATE_PROPERTY = "loginWizardLicensePlateProperty";

	private LicensePlateEditText licensePlateEditText;
	private TextView errorText;

	@ContextVariable
	private String licensePlate;

	public static class InternalURLSpan extends ClickableSpan
	{
		private OnClickListener listener;

		public InternalURLSpan(OnClickListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void onClick(View widget)
		{
			listener.onClick(widget);
		}
	}

	public LoginStep2()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.login_wizard_step2, container, false);
		final ImageButton speakButton = (ImageButton) v.findViewById(R.id.action_button);
		speakButton.setVisibility(View.GONE);

		licensePlateEditText = (LicensePlateEditText) v.findViewById(R.id.license_plate);
		errorText = (TextView) v.findViewById(R.id.lp_error_text);
		Button questionLink = (Button) v.findViewById(R.id.question_button);
		SpannableString spanString = new SpannableString(getString(R.string.login_wizard_step_2_question_text));
		//		spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
		//		spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
		//		spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
		questionLink.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
		questionLink.setText(spanString);
		questionLink.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showPopupAnswer();
			}
		});

		return v;
	}

	private void showPopupAnswer()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		String explanationMessage = getString(R.string.login_wizard_step2_explanation_text);
		builder.setTitle(R.string.login_wizard_step2_dialog_title).setIcon(R.drawable.rw_launcher).setMessage(explanationMessage);
		builder.setPositiveButton("Got it", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{

			}
		});

		builder.create().show();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// Initialize listeners here since doing it in onCreate() causes a recursive exception
		LicensePlateUtils.configureLicensePlateEditText(licensePlateEditText);
		licensePlateEditText.addTextChangedListener(new TextWatcher()
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
				String lp = licensePlateEditText.getText().toString();
				boolean licensePlateValid = LicensePlateUtils.isValidLicensePlate(lp);
				boolean probablyFake = LicensePlateUtils.isFakeLicensePlate(lp);
				boolean licensePlateOK = licensePlateValid && !probablyFake;
				if (licensePlateOK)
				{
					Utils.closeSoftKeyboard(getActivity(), licensePlateEditText.getWindowToken());
					licensePlate = licensePlateEditText.getText().toString();
					// Saving value instead of using the wizard framework 'value injection' which sometimes doesn't work
					PreferencesUtils.putString(getActivity(), LOGIN_WIZARD_LICENSE_PLATE_PROPERTY, licensePlate);
					notifyCompleted(true);
				}
				else
					notifyCompleted(false);

				if (licensePlateValid && probablyFake)
				{
					errorText.setText(R.string.fake_license_plate_detected);
					Log.w(TAG, "User is probably trying to register with a fake license plate: " + licensePlateEditText.getText().toString());
				}
				else
					errorText.setText("");
			}
		});
		licensePlateEditText.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// Patch for Android 2.3 to allow keyboard to show
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				{
					licensePlateEditText.clearFocus();
					licensePlateEditText.requestFocus();
				}
				return false;
			}
		});
	}
}