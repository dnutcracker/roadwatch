package com.roadwatch.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.roadwatch.app.R;
import com.roadwatch.app.report.LicensePlateType;
import com.roadwatch.app.util.LicensePlateUtils;

/**
 * PENDING: Currently the background color does not include the 'action' button attached to the license plate - find a way to change it together with the license plate bg.
 * 
 * PENDING: Probably best way to handle multiple license plate types will be a custome IME (see - http://developer.android.com/guide/topics/text/creating-input-method.html)
 * 
 * @author Nati created at : 10/03/2014 15:14:08
 *
 */
public class LicensePlateEditText extends EditText
{
	private LicensePlateType type;

	public LicensePlateEditText(Context context)
	{
		super(context);
		setType(LicensePlateType.ISRAEL);
	}

	public LicensePlateEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setType(LicensePlateType.ISRAEL);
	}

	public LicensePlateEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setType(LicensePlateType.ISRAEL);
	}

	public void setType(LicensePlateType type)
	{
		if (this.type == type)
			return;

		this.type = type;
		switch (this.type)
		{
		case ISRAEL_POLICE:
			setBackgroundResource(R.color.lp_red);
			setTextColor(0xFFFFFFFF);
			setHint("XX-XXX-î");
			break;
		default:
			setBackgroundResource(R.color.lp_yellow);
			setTextColor(0xFF000000);
			setHint("XX-XXX-XX");
		}

		LicensePlateUtils.configureLicensePlateEditText(this);
	}

	public LicensePlateType getType()
	{
		return type;
	}
}