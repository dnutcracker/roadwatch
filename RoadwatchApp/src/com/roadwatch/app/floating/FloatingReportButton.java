package com.roadwatch.app.floating;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.roadwatch.app.R;
import com.roadwatch.app.report.ReportActivity;
import com.roadwatch.app.report.UnsentReportsActivity;
import com.roadwatch.app.util.JSONUtils;

/**
 * This floating button appears inside a navigation app allowing to quickly open the report activity.
 * Keep in mind that the <code>StandOutWindow</code> is actually a service.
 * 
 * @author Nati created at : 13/10/13 23:05:53
 *
 */
public class FloatingReportButton extends StandOutWindow
{
	// Logging tag
	private static final String TAG = FloatingReportButton.class.getSimpleName();

	@Override
	public void createAndAttachView(int id, FrameLayout frame)
	{
		// create a new layout from body.xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View rootView = inflater.inflate(R.layout.floating_report_button, frame, true);

		// Allow quick access to 'Unsent Reports' activity from within the navigation app 
		// by a long press the button
		Button floatingButton = (Button) rootView.findViewById(R.id.floatingReportButton);
		floatingButton.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				int unsentReportsNumber = JSONUtils.loadJSONObjects(FloatingReportButton.this, JSONUtils.UNSENT_REPORTS_FILENAME).size();
				if (unsentReportsNumber > 0)
					openUnsentReportsActivity(v);
				else
					Toast.makeText(FloatingReportButton.this, R.string.no_unsent_reports, Toast.LENGTH_SHORT).show();

				return true;
			}
		});

		//		UiModeManager manager = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
		//
		//		String modeType = "";
		//		switch (manager.getCurrentModeType())
		//		{
		//		case Configuration.UI_MODE_TYPE_NORMAL:
		//			modeType = "NORMAL";
		//			break;
		//		case Configuration.UI_MODE_TYPE_DESK:
		//			modeType = "DESK";
		//			break;
		//		case Configuration.UI_MODE_TYPE_CAR:
		//			modeType = "CAR";
		//			break;
		//		case Configuration.UI_MODE_TYPE_TELEVISION:
		//			modeType = "TELVISION";
		//			break;
		//		default:
		//			modeType = "Unknown Mode Type";
		//		}
		//		Log.d(TAG, "Mode type = " + modeType);
		//
		//		String nightMode = "";
		//		switch (manager.getNightMode())
		//		{
		//		case UiModeManager.MODE_NIGHT_AUTO:
		//			nightMode = "MODE_NIGHT_AUTO";
		//			break;
		//		case UiModeManager.MODE_NIGHT_YES:
		//			nightMode = "MODE_NIGHT_YES";
		//			break;
		//		case UiModeManager.MODE_NIGHT_NO:
		//			nightMode = "MODE_NIGHT_NO";
		//			break;
		//		default:
		//			nightMode = "Unknown";
		//		}
		//		Log.d(TAG, "Night mode = " + nightMode);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		// Make sure window exists before trying to update its layout
		if (isExistingId(DEFAULT_ID))
		{
			if ((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
				Log.w(TAG, "We are in night mode !");

			updateViewLayout(DEFAULT_ID, getOrientationParams(newConfig.orientation));
		}
	}

	@Override
	public StandOutLayoutParams getParams(int id, Window window)
	{
		return getOrientationParams(getResources().getConfiguration().orientation);
	}

	@SuppressWarnings("deprecation")
	private StandOutLayoutParams getOrientationParams(int orientation)
	{
		Bitmap buttonIconBitmap = ((BitmapDrawable) this.getResources().getDrawable(R.drawable.floating_report_button)).getBitmap();
		int buttonWidth = buttonIconBitmap.getWidth();
		int buttonHeight = buttonIconBitmap.getHeight();
		//int windowWidth = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		int windowHeight = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
		int screenBottomEdge = (int) (29 * getResources().getDisplayMetrics().density);
		int screenLeftEdge = (int) (2 * getResources().getDisplayMetrics().density);
		int x = screenLeftEdge + (orientation == Configuration.ORIENTATION_PORTRAIT ? 0 : buttonWidth);
		int y = windowHeight - (orientation == Configuration.ORIENTATION_PORTRAIT ? 2 * buttonHeight : buttonHeight) - screenBottomEdge;
		//		Log.d(TAG, "Screen Edge size is : " + screenEdge);
		//		Log.d(TAG, "Default Display size is : (" + windowWidth + "," + windowHeight + ")");
		//		Log.d(TAG, "Display floating button at : (" + x + "," + y + ")");

		return new StandOutLayoutParams(DEFAULT_ID, buttonWidth, buttonHeight, x, y);
	}

	//	@SuppressWarnings("deprecation")
	//	private StandOutLayoutParams getOrientationParams(int orientation)
	//	{
	//		Bitmap buttonIconBitmap = ((BitmapDrawable) this.getResources().getDrawable(R.drawable.floating_report_button)).getBitmap();
	//		int buttonWidth = buttonIconBitmap.getWidth();
	//		int buttonHeight = buttonIconBitmap.getHeight();
	//		int windowWidth = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
	//		int windowHeight = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
	//		int screenBottomEdge = (int) (30 * getResources().getDisplayMetrics().density);
	//		int screenRightEdge = (int) (2 * getResources().getDisplayMetrics().density);
	//		int x = windowWidth - (screenRightEdge + (orientation == Configuration.ORIENTATION_PORTRAIT ? buttonWidth : 2 * buttonWidth));
	//		int y = windowHeight - (orientation == Configuration.ORIENTATION_PORTRAIT ? 2 * buttonHeight : buttonHeight) - screenBottomEdge;
	//		//		Log.d(TAG, "Screen Edge size is : " + screenEdge);
	//		//		Log.d(TAG, "Default Display size is : (" + windowWidth + "," + windowHeight + ")");
	//		//		Log.d(TAG, "Display floating button at : (" + x + "," + y + ")");
	//
	//		return new StandOutLayoutParams(DEFAULT_ID, buttonWidth, buttonHeight, x, y);
	//	}

	// move the window by dragging the view
	@Override
	public int getFlags(int id)
	{
		return super.getFlags(id) /*| StandOutFlags.FLAG_BODY_MOVE_ENABLE*/| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
	}

	/**
	 * Defined in the XML to be invoked when pressing the floating button
	 * @param view
	 */
	public void openReportActivity(View view)
	{
		// Open report activity
		Intent reportIntent = new Intent(this, ReportActivity.class);
		reportIntent.putExtra(ReportActivity.EXTRA_VOICE_RECOGNITION_MODE, "true");
		reportIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(reportIntent);
	}

	/**
	 * Invoked when long pressing the floating button.
	 * @param view
	 */
	private void openUnsentReportsActivity(View view)
	{
		// Open unsent reports activity
		Intent unsentReportsIntent = new Intent(this, UnsentReportsActivity.class);
		unsentReportsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(unsentReportsIntent);
	}

	//	@Override
	//	public boolean onShow(int id, Window window)
	//	{
	//		window.setDrawingCacheEnabled(true);
	//		window.buildDrawingCache();
	//		Bitmap windowDrawingCacheBitmap = window.getDrawingCache();
	//		if(windowDrawingCacheBitmap!=null)
	//		{
	//			int color = windowDrawingCacheBitmap.getPixel(0, 0);		
	//			Log.d("", "Button Edge color is : " + color);
	//		}
	//		else
	//			Log.d("", "windowDrawingCacheBitmap is null !");
	//
	//		return super.onShow(id, window);
	//	}

	@Override
	public String getAppName()
	{
		return getString(R.string.app_name);
	}

	@Override
	public int getAppIcon()
	{
		return R.drawable.ic_action_about;
	}

	@Override
	public String getPersistentNotificationTitle(int id)
	{
		return getAppName();
	}

	@Override
	public String getPersistentNotificationMessage(int id)
	{
		return getString(R.string.integrated_with_nav_app);
	}

	/**
	 * We must have this intent or else Android 2.3 will crash.
	 * @see http://stackoverflow.com/questions/11947928/startforeground-bad-notification-error
	 */
	@Override
	public Intent getPersistentNotificationIntent(int id)
	{
		Intent emptyIntent = new Intent();
		return emptyIntent;//StandOutWindow.getCloseIntent(this, FloatingReportButton.class, id);
	}
}