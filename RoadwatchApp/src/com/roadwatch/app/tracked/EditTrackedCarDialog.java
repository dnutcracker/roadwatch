package com.roadwatch.app.tracked;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;

import com.roadwatch.app.R;
import com.roadwatch.app.report.LicensePlateType;
import com.roadwatch.app.util.LicensePlateUtils;

public class EditTrackedCarDialog extends Dialog
{
	private EditText nameEditText;
	private EditText licensePlateEditText;
	private Button updateButton;
	private Button removeButton;

	public EditTrackedCarDialog(Context context, String name, String licensePlate)
	{
		super(context);

		setContentView(R.layout.tracked_car_edit_dialog);
		setTitle(R.string.edit_tracked_car_dialog_title);

		nameEditText = (EditText) findViewById(R.id.name);
		nameEditText.setText(name);

		licensePlateEditText = (EditText) findViewById(R.id.new_tracked_license_plate);
		LicensePlateUtils.configureLicensePlateEditText(licensePlateEditText, LicensePlateType.ISRAEL);
		licensePlateEditText.setText(licensePlate);

		updateButton = (Button) findViewById(R.id.tracked_car_update_button);
		removeButton = (Button) findViewById(R.id.tracked_car_remove_button);
	}

	public String getTrackedName()
	{
		return nameEditText.getText().toString();
	}

	public String getTrackedLicensePlate()
	{
		return licensePlateEditText.getText().toString();
	}

	public Button getUpdateButton()
	{
		return updateButton;
	}

	public Button getRemoveButton()
	{
		return removeButton;
	}
}