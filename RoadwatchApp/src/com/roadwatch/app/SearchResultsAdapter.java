package com.roadwatch.app;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.ReportUtils;

public class SearchResultsAdapter extends ArrayAdapter<HashMap<String, String>>
{
	private final Activity context;

	static class ViewHolder
	{
		public TextView reportDescriptionView;
		public TextView reportTimeView;
		public TextView reportAddressView;
		public ImageView reportImageView;
	}

	public SearchResultsAdapter(Activity context, List<HashMap<String, String>> resultRows)
	{
		super(context, R.layout.search_result_row, resultRows);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View rowView = convertView;
		if (rowView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.search_result_row, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.reportDescriptionView = (TextView) rowView.findViewById(R.id.report_description);
			viewHolder.reportTimeView = (TextView) rowView.findViewById(R.id.report_time);
			viewHolder.reportAddressView = (TextView) rowView.findViewById(R.id.report_address);
			viewHolder.reportImageView = (ImageView) rowView.findViewById(R.id.report_image);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		HashMap<String, String> reportRow = getItem(position);
		holder.reportDescriptionView.setText(reportRow.get(JSONKeys.REPORT_KEY_DESCRIPTION));
		holder.reportTimeView.setText(reportRow.get(JSONKeys.REPORT_KEY_TIME_FOR_DISPLAY));
		holder.reportAddressView.setText(reportRow.get(JSONKeys.REPORT_KEY_ADDRESS_FOR_DISPLAY));
		Bitmap bitmap = ReportUtils.getReportMapPinBitmap(context, reportRow);
		holder.reportImageView.setImageBitmap(bitmap);
		return rowView;
	}

	//	public void clear()
	//	{
	//
	//	}
}