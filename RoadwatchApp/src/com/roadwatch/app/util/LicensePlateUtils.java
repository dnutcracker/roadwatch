package com.roadwatch.app.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.roadwatch.app.report.LicensePlateType;
import com.roadwatch.app.widgets.LicensePlateEditText;

public class LicensePlateUtils
{
	/**
	 * This method is used for regular textfields that wish to enforce a specific license plate type format.
	 * @param licensePlateText
	 * @param type
	 */
	public static void configureLicensePlateEditText(EditText licensePlateText, LicensePlateType type)
	{
		switch (type)
		{
		case ISRAEL:
			configureIsraeliLicensePlateEditText(licensePlateText);
			break;
		case ISRAEL_POLICE:
			//configureIsraeliPoliceLicensePlateEditText(licensePlateText);
			break;
		case NETHERLANDS:
			// PENDING: Implement!
			break;
		case SPAIN:
			configureSpainLicensePlateEditText(licensePlateText);
			break;
		}
	}

	public static void configureLicensePlateEditText(LicensePlateEditText licensePlateText)
	{
		LicensePlateType type = licensePlateText.getType();
		switch (type)
		{
		case ISRAEL:
			configureIsraeliLicensePlateEditText(licensePlateText);
			break;
		case ISRAEL_POLICE:
			configureIsraeliPoliceLicensePlateEditText(licensePlateText);
			break;
		case NETHERLANDS:
			// PENDING: Implement!
			break;
		case SPAIN:
			configureSpainLicensePlateEditText(licensePlateText);
			break;
		}
	}

	/**
	 * Enforce format XX-XXX-XX of the license plate number 
	 * @param editText
	 */
	private static void configureIsraeliLicensePlateEditText(EditText editText)
	{
		editText.addTextChangedListener(new TextWatcher()
		{
			private String textBefore;
			private String textOn;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				textBefore = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				textOn = s.toString();
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				boolean textInserted = textBefore.length() < textOn.length();
				boolean textRemoved = textBefore.length() > textOn.length();

				if (textInserted && !isCharLegit(s))
					s.delete(s.length() - 1, s.length());

				if (textInserted && (s.length() == 2 || s.length() == 6))
					s.append('-');
				if (textRemoved && (s.length() == 2 || s.length() == 6))
					s.delete(s.length() - 1, s.length());
			}

			/**
			 * Only digits are allowed (or dash at indices 3 and 7) 
			 * @param s
			 * @return
			 */
			private boolean isCharLegit(Editable s)
			{
				char lastChar = s.charAt(s.length() - 1);
				boolean isDigit = Character.isDigit(lastChar);
				boolean isLegitDash = (s.length() == 3 || s.length() == 7) && lastChar == '-';

				return isDigit || isLegitDash;
			}
		});
	}

	/**
	 * Enforce format XX-XXX-î of the license plate number 
	 * @param editText
	 */
	private static void configureIsraeliPoliceLicensePlateEditText(EditText editText)
	{
		editText.addTextChangedListener(new TextWatcher()
		{
			private String textBefore;
			private String textOn;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				textBefore = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				textOn = s.toString();
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				boolean textInserted = textBefore.length() < textOn.length();
				boolean textRemoved = textBefore.length() > textOn.length();

				if (textInserted && !isCharLegit(s))
					s.delete(s.length() - 1, s.length());

				if (textInserted && (s.length() == 2))
					s.append('-');
				if (textInserted && (s.length() == 6))
					s.append('-');
				if (textInserted && (s.length() == 7))
					s.append('î');			
				
				if (textRemoved && (s.length() == 2))
					s.delete(s.length() - 1, s.length());
				
				if (textRemoved && (s.length() == 7))
					s.delete(s.length() - 2, s.length());
			}

			/**
			 * Only digits are allowed (or dash at indices 3 and 7) 
			 * @param s
			 * @return
			 */
			private boolean isCharLegit(Editable s)
			{
				char lastChar = s.charAt(s.length() - 1);
				boolean isDigit = Character.isDigit(lastChar);
				boolean isLegitDash = (s.length() == 3 || s.length() == 7) && lastChar == '-';
				boolean isLegitM = s.length() == 8 && lastChar=='î';

				return isDigit || isLegitDash || isLegitM;
			}
		});
	}

	/**
	 * Enforce format XX-XXX-XX of the license plate number 
	 * @param editText
	 */
	private static void configureSpainLicensePlateEditText(EditText editText)
	{
		editText.addTextChangedListener(new TextWatcher()
		{
			private String textBefore;
			private String textOn;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				textBefore = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				textOn = s.toString();
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				boolean textInserted = textBefore.length() < textOn.length();
				boolean textRemoved = textBefore.length() > textOn.length();

				// Prevent typing wrong characters
				if (textInserted && !isCharLegit(s))
					s.delete(s.length() - 1, s.length());

				if (textInserted && s.length() == 4)
					s.append(' ');
				if (textRemoved && s.length() == 4)
					s.delete(s.length() - 1, s.length());
			}

			/**
			 * Only digits are allowed (or dash at indices 3 and 7) 
			 * @param s
			 * @return
			 */
			private boolean isCharLegit(Editable s)
			{
				char lastChar = s.charAt(s.length() - 1);
				return (s.length() < 5 && Character.isDigit(lastChar)) || (s.length() == 5 && lastChar == ' ') || (s.length() > 5 && Character.isLetter(lastChar));
			}
		});
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * PENDING: All invoking methods should be revised to specify the license plate type!
	 */
	public static boolean isValidLicensePlate(String licensePlate)
	{
		return isValidLicensePlate(licensePlate, LicensePlateType.ISRAEL);
	}

	public static boolean isValidLicensePlate(String licensePlate, LicensePlateType type)
	{
		switch (type)
		{
		case ISRAEL:
			return isValidIsraeliLicensePlate(licensePlate);
		case ISRAEL_POLICE:
			return isValidIsraeliPoliceLicensePlate(licensePlate);
		case NETHERLANDS:
			return isValidNLLicensePlate(licensePlate);
		case SPAIN:
			return isValidSpainLicensePlate(licensePlate);
		default:
			return true;
		}
	}

	private static boolean isValidIsraeliLicensePlate(String licensePlate)
	{
		final String ISRAELI_REGULAR_PATTERN = "^\\d{2}-\\d{3}-\\d{2}$";
		final String ISRAELI_COLLECTORS_PATTERN = "^\\d{3}-\\d{3}d$";

		return licensePlate.matches(ISRAELI_REGULAR_PATTERN) || licensePlate.matches(ISRAELI_COLLECTORS_PATTERN);
	}

	private static boolean isValidIsraeliPoliceLicensePlate(String licensePlate)
	{
		final String ISRAELI_POLICE_CAR_PATTERN = "^\\d{2}-\\d{3}-î$";

		return licensePlate.matches(ISRAELI_POLICE_CAR_PATTERN);
	}

	/**
	 * @see http://en.wikipedia.org/wiki/Vehicle_registration_plates_of_the_Netherlands
	 * 
	 * @param licensePlate
	 * @return
	 */
	private static boolean isValidNLLicensePlate(String licensePlate)
	{
		final String SIDE_CODE_6_PATTERN = "^\\d{2}-[ABDFGHJKLMNPRSTVWXZ]{2}-\\d{2}$";
		final String SIDE_CODE_8_PATTERN = "^\\d-[ABDFGHJKLMNPRSTVWXZ]{3}-\\d{2}$";

		return licensePlate.matches(SIDE_CODE_8_PATTERN) || licensePlate.matches(SIDE_CODE_6_PATTERN);
	}

	private static boolean isValidSpainLicensePlate(String licensePlate)
	{
		final String SPANISH_REGULAR_SIDE_PATTERN = "^\\d{4} [BCDFGHJKLMNPRSTVWXYZ]{3}$";

		return licensePlate.matches(SPANISH_REGULAR_SIDE_PATTERN);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	

	public static boolean isFakeLicensePlate(String licensePlate)
	{
		// PENDING: Retrieve from settings
		LicensePlateType type = LicensePlateType.ISRAEL;
		switch (type)
		{
		case ISRAEL:
			return isFakeIsraeliLicensePlate(licensePlate);
		case NETHERLANDS:
			// PENDING: Implement
			return false;
		default:
			return false;
		}
	}

	private static boolean isFakeIsraeliLicensePlate(String licensePlate)
	{
		if (licensePlate.isEmpty())
			return false;

		// Check against some predefined fake license plates
		String[] fakesLicensePlates = new String[]
		{ "01-234-56", "12-345-67", "65-432-10", "76-543-21" };
		for (int i = 0; i < fakesLicensePlates.length; i++)
		{
			if (licensePlate.equals(fakesLicensePlates[i]))
				return true;
		}

		int numberOfChanges = 0;
		char currentDigit = licensePlate.charAt(0);
		for (int i = 1; i < licensePlate.length(); i++)
		{
			char digit = licensePlate.charAt(i);
			if (digit != '-' && currentDigit != digit)
			{
				currentDigit = digit;
				numberOfChanges++;
			}
		}

		return numberOfChanges < 2;
	}
}