package com.roadwatch.app.tracked;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.roadwatch.app.R;
import com.roadwatch.app.RoadwatchMainActivity;
import com.roadwatch.app.report.LicensePlateType;
import com.roadwatch.app.server.ServerAPI;
import com.roadwatch.app.util.JSONKeys;
import com.roadwatch.app.util.LicensePlateUtils;
import com.roadwatch.app.util.PreferencesUtils;

/**
 * PENDING:
 * 1. Hide purchase option for now
 * 2. Perform validation + toast message before exiting dialog
 * 
 * @author Nati created at : 02:59:20
 *
 */
public class AddTrackedCarActivity extends Activity
{
	// Debug tag, for logging
	private static final String TAG = AddTrackedCarActivity.class.getSimpleName();

	// SKU for tracked car
	private static final String SKU_TRACKED_CAR = "tracked_car_slot";

	// (arbitrary) request code for the purchase flow
	private static final int RC_REQUEST = 10001;

	public static final String EXTRA_TRACKED_NAME = "tracked_name";
	public static final String EXTRA_TRACKED_LICENSE_PLATE = "tracked_license_plate";

	// Listener that's called when we finish querying the items and subscriptions we own
	private IabHelper.QueryInventoryFinishedListener gotInventoryListener = new IabHelper.QueryInventoryFinishedListener()
	{
		public void onQueryInventoryFinished(IabResult result, Inventory inventory)
		{
			Log.d(TAG, "Query inventory finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null)
				return;

			// Is it a failure?
			if (result.isFailure())
			{
				complain("Failed to query inventory: " + result);
				return;
			}

			Log.d(TAG, "Query inventory was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! 
			 * See verifyDeveloperPayload().
			 */
			// Check for tracked car purchase . if we have it, we need to comsume it immediately
			Purchase trackedCarPurchase = inventory.getPurchase(SKU_TRACKED_CAR);
			if (trackedCarPurchase != null && verifyDeveloperPayload(trackedCarPurchase))
			{
				Log.w(TAG, "Found an unsconsumed tracked car purchase. Consuming it now.");
				mHelper.consumeAsync(inventory.getPurchase(SKU_TRACKED_CAR), mConsumeFinishedListener);
				return;
			}

			new GetPurchasedTrackedCarsSizeTask().execute();

			setWaitScreen(false);
			Log.d(TAG, "Initial inventory query finished; enabling main UI.");
		}
	};

	// Callback for when a purchase is finished
	private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener()
	{
		public void onIabPurchaseFinished(IabResult result, Purchase purchase)
		{
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

			// if we were disposed of in the meantime, quit.
			if (mHelper == null)
				return;

			if (result.isFailure())
			{
				Log.e(TAG, "Purchase cancelled : " + result.getMessage());
				Toast.makeText(AddTrackedCarActivity.this, getString(R.string.purchase_canclled), Toast.LENGTH_LONG).show();

				setWaitScreen(false);
				return;
			}
			if (!verifyDeveloperPayload(purchase))
			{
				complain("Error purchasing. Authenticity verification failed.");
				setWaitScreen(false);
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(SKU_TRACKED_CAR))
			{
				// bought a tracked car
				Log.d(TAG, "Purchased a tracked cars. Updating server");

				mHelper.consumeAsync(purchase, mConsumeFinishedListener);
			}
		}
	};

	// Called when consumption is complete
	private IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener()
	{
		public void onConsumeFinished(Purchase purchase, IabResult result)
		{
			Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

			// if we were disposed of in the meantime, quit.
			if (mHelper == null)
				return;

			// We know this is the "tracked car" sku because it's the only one we consume,
			// so we don't check which sku was consumed. If you have more than one
			// sku, you probably should check...
			if (result.isSuccess())
			{
				Log.d(TAG, "Consumption successful. Updating server.");
				// successfully consumed, so we apply the effects of the item in our
				// game world's logic, which in our case means filling the gas tank a bit

				// PENDING: Update server with additional tracked car here

				Log.d(TAG, "Server updated");
			}
			else
			{
				complain("Error while consuming: " + result);
			}

			updateUI();
			setWaitScreen(false);
			Log.d(TAG, "End consumption flow.");
		}
	};

	// The helper object
	private IabHelper mHelper;

	private int availableTrackedCars;

	private TextView description;
	private EditText nameEditText;
	private EditText licensePlateEditText;
	private TextView availableTrackedCarsText;
	private Button addButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracked_car_add);

		setTitle(R.string.add_tracked_car_title);

		nameEditText = (EditText) findViewById(R.id.name);
		licensePlateEditText = (EditText) findViewById(R.id.new_tracked_license_plate);
		LicensePlateUtils.configureLicensePlateEditText(licensePlateEditText, LicensePlateType.ISRAEL);

		availableTrackedCarsText = (TextView) findViewById(R.id.available_tracked_cars_text);

		addButton = (Button) findViewById(R.id.tracked_car_add_button);

		description = (TextView) findViewById(R.id.add_tracked_car_dialog_description);

		/*
		 * Instead of just storing the entire literal string here embedded in the
		 * program,  construct the key at runtime from pieces or
		 * use bit manipulation (for example, XOR with some other string) to hide
		 * the actual key.  The key itself is not secret information, but we don't
		 * want to make it easy for an attacker to replace the public key with one
		 * of their own and then fake messages from the server.
		 */
		final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqHP9SHcTOw4ooX29k1Gq" + "/7Q3n/TMFj6s/U2qlwJprAp5tZOEAfo4qQ5uxdUKbz1ac/m3x34QMhXrRDkYBbzv"
				+ "BziTM4FoVC7e/33+EBvDy4aWHRf8BBL4WGG2joLRQVWg3xmbcbq03D3EqzoQoAXP" + "Qrx64nAQBjFfA6ZIDPISCOgBMWGtNflkhCnDRh1EFA0BMkwJY+IdseSRtprzX813"
				+ "1tnuNdekOIHFs2Lj4ji+aAyozy57VIpeVkbgCpJF1//K/7DQidOHPUVtLAD39aR6" + "zurKFXk/gz4id4uUbAPR+sjya28DTPjvR3+oj+P2mpprUMyw0baHzaxC5jNjo4y9awIDAQAB";

		// Create the helper, passing it our context and the public key to verify signatures with
		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(this, base64EncodedPublicKey);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(true);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
		{
			public void onIabSetupFinished(IabResult result)
			{
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess())
				{
					// Oh noes, there was a problem.
					complain("Problem setting up in-app billing: " + result);
					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null)
					return;

				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(gotInventoryListener);
			}
		});
	}

	// User clicked the "+" button
	public void purchaseTrackedCar(View v)
	{
		Log.d(TAG, "Purchase tracked car button clicked.");

		// launch the gas purchase UI flow.
		// We will be notified of completion via mPurchaseFinishedListener
		setWaitScreen(true);
		Log.d(TAG, "Launching purchase flow for gas.");

		/* TODO: for security, generate your payload here for verification. See the comments on
		 *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
		 *        an empty string, but on a production app you should carefully generate this. */
		String payload = "";

		mHelper.launchPurchaseFlow(this, SKU_TRACKED_CAR, RC_REQUEST, mPurchaseFinishedListener, payload);
	}

	// User clicked the add tracked car button
	public void addTrackedCar(View v)
	{
		Intent trackedData = new Intent();
		trackedData.putExtra(EXTRA_TRACKED_NAME, nameEditText.getText().toString());
		trackedData.putExtra(EXTRA_TRACKED_LICENSE_PLATE, licensePlateEditText.getText().toString());
		setResult(RESULT_OK, trackedData);
		finish();
	}

	// User clicked the cancel button
	public void cancel(View v)
	{
		Log.d(TAG, "Cancel button clicked.");
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
		if (mHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
		{
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
		else
		{
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}

	/** Verifies the developer payload of a purchase. */
	boolean verifyDeveloperPayload(Purchase p)
	{
		//String payload = p.getDeveloperPayload();

		/*
		 * TODO: verify that the developer payload of the purchase is correct. It will be
		 * the same one that you sent when initiating the purchase.
		 *
		 * WARNING: Locally generating a random string when starting a purchase and
		 * verifying it here might seem like a good approach, but this will fail in the
		 * case where the user purchases an item on one device and then uses your app on
		 * a different device, because on the other device you will not have access to the
		 * random string you originally generated.
		 *
		 * So a good developer payload has these characteristics:
		 *
		 * 1. If two different users purchase an item, the payload is different between them,
		 *    so that one user's purchase can't be replayed to another user.
		 *
		 * 2. The payload must be such that you can verify it even when the app wasn't the
		 *    one who initiated the purchase flow (so that items purchased by the user on
		 *    one device work on other devices owned by the user).
		 *
		 * Using your own server to store and verify developer payloads across app
		 * installations is recommended.
		 */

		return true;
	}

	// We're being destroyed. It's important to dispose of the helper here!
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// very important:
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null)
		{
			mHelper.dispose();
			mHelper = null;
		}
	}

	// updates UI to reflect model
	public void updateUI()
	{
		//		availableTrackedCarsText.setText(AddTrackedCarActivity.this.getString(R.string.tracked_cars_available, Integer.valueOf(availableTrackedCars)));
		//		description.setEnabled(availableTrackedCars > 0);
		//		nameEditText.setEnabled(availableTrackedCars > 0);
		//		licensePlateEditText.setEnabled(availableTrackedCars > 0);
		//		addButton.setEnabled(availableTrackedCars > 0);
	}

	// Enables or disables the "please wait" screen.
	void setWaitScreen(boolean set)
	{
		//findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
		//findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
	}

	void complain(String message)
	{
		Log.e(TAG, "**** TrivialDrive Error: " + message);
		alert("Error: " + message);
	}

	void alert(String message)
	{
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(TAG, "Showing alert dialog: " + message);
		bld.create().show();
	}

	/**
	 * An AsyncTask that retrieves the reports of the searched license plate from the server.
	 */
	private class GetPurchasedTrackedCarsSizeTask extends AsyncTask<Void, Void, JSONObject>
	{
		@Override
		protected void onPreExecute()
		{
			// Remove empty results message and display progress bar
			//progressLayout.setVisibility(View.VISIBLE);
		}

		@Override
		protected JSONObject doInBackground(Void... params)
		{
			ServerAPI serverAPI = new ServerAPI(AddTrackedCarActivity.this);
			String loggedInLicensePlate = PreferencesUtils.getString(AddTrackedCarActivity.this, RoadwatchMainActivity.PROPERTY_USER_LICENSE_PLATE);

			// Get user by license plate
			List<JSONObject> jsonUsers = serverAPI.getUserByLicensePlate(loggedInLicensePlate);

			JSONObject jsonUser = null;

			// Check if it is logged in
			if (!jsonUsers.isEmpty())
				jsonUser = jsonUsers.get(0);
			else
				Log.w(TAG, "User is not logged in!");

			return jsonUser;
		}

		@Override
		protected void onPostExecute(JSONObject jsonUser)
		{
			// Hide progress bar
			//progressLayout.setVisibility(View.GONE);

			if (jsonUser != null)
			{
				int purchasedTrackedCarsSize = jsonUser.optInt(JSONKeys.USER_KEY_PURCHASED_TRACKED_LICENSE_PLATE_SIZE, 0);
				JSONArray trackedCarsJSONArray = jsonUser.optJSONArray(JSONKeys.USER_KEY_TRACKED_LICENSE_PLATES);
				int usedTrackedCarsSize = trackedCarsJSONArray != null ? trackedCarsJSONArray.length() : 0;
				Log.i(TAG, "Purchased tracked cars : " + purchasedTrackedCarsSize + ". Used tracked cars : " + usedTrackedCarsSize);

				availableTrackedCars = purchasedTrackedCarsSize - usedTrackedCarsSize;
				updateUI();
			}
		}
	}
}