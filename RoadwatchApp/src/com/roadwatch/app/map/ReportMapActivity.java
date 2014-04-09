package com.roadwatch.app.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.roadwatch.app.R;
import com.roadwatch.app.report.EditReportActivity;
import com.roadwatch.app.report.EditReportActivity.ReportViewMode;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.ReportUtils;

/**
 * Ensure that the following correspond to what is in the API Console: 
 * Package Name: com.roadwatch.app, 
 * API Key: AIzaSyAZ-X-TYUwnRGUng043hU42hUa-JBZjwLI, 
 * Certificate Fingerprint: 183A0ABDC473D4A4AC2116CD54FF39A879DCC67C
 * 
 * PENDING:
 * 1. Add tool bar actions '<' and '>' to go through the displayed locations.
 * 
 * @author Nati created at : 13:20:34
 */
public class ReportMapActivity extends FragmentActivity
{
	private static final String TAG = ReportMapActivity.class.getSimpleName();
	private static final int EDIT_REPORT_FROM_MAP_REQUEST_CODE = 5000;

	public static final String EXTRA_REPORT_LIST = "reports";
	public static final String EXTRA_SELECTED_REPORT_INDEX = "selected_report_index";

	private GoogleMap map;
	private PinResultsToMapTask pinResultsToMapTask;
	private Map<String, HashMap<String, String>> markerIdToReportMap = new HashMap<String, HashMap<String, String>>();
	private Marker selectedMarker;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.map);
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		map.getUiSettings().setCompassEnabled(true);

		map.setInfoWindowAdapter(new InfoWindowAdapter()
		{
			private final View contents = getLayoutInflater().inflate(R.layout.map_info, null);

			@Override
			public View getInfoWindow(Marker marker)
			{
				return null;
			}

			@Override
			public View getInfoContents(Marker marker)
			{
				TextView txtTitle = (TextView) contents.findViewById(R.id.infoWindowTitle);
				TextView txtType = (TextView) contents.findViewById(R.id.infoWindowData);

				// Super patch !
				// If not prefixed with English chars - Hebrew is not displayed (google bug!)
				// So we prefix with an 'X' and color it white.
				String title = "X" + marker.getTitle();
				SpannableStringBuilder textSpan = new SpannableStringBuilder(title);
				textSpan.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 1, 0);
				txtTitle.setText(textSpan);

				String snippet = "X" + marker.getSnippet();
				SpannableStringBuilder snippetSpan = new SpannableStringBuilder(snippet);
				snippetSpan.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 1, 0);
				txtType.setText(snippetSpan);

				return contents;
			}
		});

		List<HashMap<String, String>> reports = (List<HashMap<String, String>>) getIntent().getSerializableExtra(EXTRA_REPORT_LIST);
		int selectedResultIndex = getIntent().getIntExtra(EXTRA_SELECTED_REPORT_INDEX, -1);
		if (selectedResultIndex != -1)
		{
			showSelectedReport(reports.get(selectedResultIndex));
			pinResultsToMapTask = new PinResultsToMapTask();
			pinResultsToMapTask.execute(reports);
		}
		else
		{
			showReportsArea(reports);

			map.setOnInfoWindowClickListener(new OnInfoWindowClickListener()
			{
				@Override
				public void onInfoWindowClick(Marker marker)
				{
					// PENDING: Handle when clicked from the 'Sent Report' activity
					// Open the edit report activity
					selectedMarker = marker;
					HashMap<String, String> reportMap = markerIdToReportMap.get(selectedMarker.getId());
					Intent editReportIntent = new Intent(ReportMapActivity.this, EditReportActivity.class);
					editReportIntent.putExtra(EditReportActivity.EXTRA_SELECTED_REPORT, reportMap);
					editReportIntent.putExtra(EditReportActivity.EXTRA_REPORT_VIEW_MODE, ReportViewMode.EDIT_BEFORE_SENDING);
					startActivityForResult(editReportIntent, EDIT_REPORT_FROM_MAP_REQUEST_CODE);
				}
			});
		}

		// Zoom in, animating the camera. (commented - slows down the map loading)
		//map.animateCamera(CameraUpdateFactory.zoomTo(14), 3000, null);		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == EDIT_REPORT_FROM_MAP_REQUEST_CODE)
		{
			if (resultCode == RESULT_OK)
			{
				selectedMarker.remove();
				markerIdToReportMap.remove(selectedMarker.getId());
				if (markerIdToReportMap.isEmpty())
				{
					// No more reports to show
					finish();
				}
				else
				{
					// Show area containing the remaining markers
					Builder latLngbuilder = LatLngBounds.builder();
					Set<Entry<String, HashMap<String, String>>> entrySet = markerIdToReportMap.entrySet();
					for (Entry<String, HashMap<String, String>> entry : entrySet)
					{
						MarkerOptions markerOptions = getReportAsMarkerOptions(entry.getValue());
						latLngbuilder = latLngbuilder.include(markerOptions.getPosition());
					}

					moveCameraToShowBounds(latLngbuilder.build());
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void showSelectedReport(HashMap<String, String> selectedReport)
	{
		map.addMarker(getReportAsMarkerOptions(selectedReport)).showInfoWindow();

		double latitude = Double.parseDouble(selectedReport.get(JSONKeys.REPORT_KEY_LATITUDE));
		double longitude = Double.parseDouble(selectedReport.get(JSONKeys.REPORT_KEY_LONGITUDE));
		LatLng selectedReportPos = new LatLng(latitude, longitude);

		// Move the camera instantly to report location with a zoom of 14		
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedReportPos, 14));

		String licensePlate = selectedReport.get(JSONKeys.REPORT_KEY_LICENSE_PLATE);
		setTitle(getString(R.string.title_activity_map_for, licensePlate));
	}

	/**
	 * Show the map with all the reports visible
	 * @param reports
	 */
	private void showReportsArea(List<HashMap<String, String>> reports)
	{
		Builder latLngbuilder = LatLngBounds.builder();
		Marker marker = null;
		for (int i = 0; i < reports.size(); i++)
		{
			HashMap<String, String> reportMap = reports.get(i);
			final MarkerOptions markerOptions = getReportAsMarkerOptions(reportMap);
			marker = map.addMarker(markerOptions);
			markerIdToReportMap.put(marker.getId(), reportMap);
			latLngbuilder = latLngbuilder.include(markerOptions.getPosition());
		}

		if (reports.size() == 1)
		{
			marker.showInfoWindow();
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 14));
		}
		else
		{
			final LatLngBounds containingAll = latLngbuilder.build();
			map.setOnCameraChangeListener(new OnCameraChangeListener()
			{
				@Override
				public void onCameraChange(CameraPosition position)
				{
					moveCameraToShowBounds(containingAll);

					// Remove our one-time listener
					map.setOnCameraChangeListener(null);
				}
			});
		}
	}

	private void moveCameraToShowBounds(LatLngBounds bounds)
	{
		// We pad the bounds or else the top most marker won't be visible
		map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70));

		// Make sure we're not zoomed more than factor 17
		if (map.getCameraPosition().zoom > 17)
			map.moveCamera(CameraUpdateFactory.zoomTo(17));
	}

	private MarkerOptions getReportAsMarkerOptions(HashMap<String, String> result)
	{
		long reportTime = Long.parseLong(result.get(JSONKeys.REPORT_KEY_TIME));
		String formattedTime = DateUtils.getRelativeDateTimeString(this, reportTime, DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
		int reportCode = result.get(JSONKeys.REPORT_KEY_CODE) != null ? Integer.parseInt(result.get(JSONKeys.REPORT_KEY_CODE)) : -1;
		String description = reportCode != -1 ? ReportUtils.getReportDescription(this, reportCode) : getString(R.string.tap_to_send);
		double latitude = Double.parseDouble(result.get(JSONKeys.REPORT_KEY_LATITUDE));
		double longitude = Double.parseDouble(result.get(JSONKeys.REPORT_KEY_LONGITUDE));

		LatLng reportPos = new LatLng(latitude, longitude);
		MarkerOptions markerOptions = new MarkerOptions().position(reportPos).title(description).snippet(formattedTime);

		Bitmap bitmap = ReportUtils.getReportMapPinBitmap(this, result);
		// Bitmap is null when we don't know yet the report code
		if (bitmap != null)
			markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));

		return markerOptions;
	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class PinResultsToMapTask extends AsyncTask<List<HashMap<String, String>>, Void, Void>
	{
		public PinResultsToMapTask()
		{

		}

		@Override
		protected Void doInBackground(List<HashMap<String, String>>... params)
		{
			final List<HashMap<String, String>> reports = params[0];
			for (int i = 0; i < reports.size(); i++)
			{
				if (isCancelled())
					return null;

				final HashMap<String, String> reportMap = reports.get(i);
				final MarkerOptions markerOptions = getReportAsMarkerOptions(reportMap);

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (!isCancelled() && !isFinishing())
						{
							Log.d(TAG, "Adding marker : " + markerOptions);
							markerIdToReportMap.put(map.addMarker(markerOptions).getId(), reportMap);
						}
					}
				});
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void param)
		{
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (pinResultsToMapTask != null && pinResultsToMapTask.getStatus() != Status.FINISHED)
			pinResultsToMapTask.cancel(false);
	}
}