package edu.mit.media.openpds.client;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.widget.Toast;


public class UserInfoTask extends AsyncTask<String, Void, Boolean>  {

	private static final String LOG_TAG = "UserInfoTask";
	
	private Activity mActivity;
	private PreferencesWrapper mPrefs;
	private RegistryClient mRegistryClient;
	
	public UserInfoTask(Activity activity, PreferencesWrapper prefs, RegistryClient registryClient) {
		mActivity = activity;
		mPrefs = prefs;
		mRegistryClient = registryClient;
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		if (params.length != 1) {
			throw new IllegalArgumentException("UserInfoTask requires a token as a parameter.");
		}
		String token = params[0];	
		
		try {
			UserInfoResponse userInfoResponse = mRegistryClient.getUserInfo(token);

			mRegistryClient.fileBugReport(userInfoResponse.toJson().toString(2));
			if (!userInfoResponse.success()) {
				showToast("Registry server user info is broken. Please contact brian717@media.mit.edu");
				mRegistryClient.fileBugReport(userInfoResponse.toJson().toString(2));
				Log.e(LOG_TAG, "Unable to parse response from getUserInfo.");
				return false;
			}
			
			return mPrefs.setUUID(userInfoResponse.getUUID()) && mPrefs.setPDSLocation(userInfoResponse.getPDSLocation());
			
		} catch (Exception e) {
			showToast("Failed contacting the server. Please try again later.");
			e.printStackTrace();
			Log.e(LOG_TAG, "Error during login - " + e.getMessage());
		}
		
		return false;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		
		if (result) {
			Intent mainActivityIntent = NavUtils.getParentActivityIntent(mActivity);
			if (mainActivityIntent != null) {
				mActivity.startActivity(mainActivityIntent);
			}
			mActivity.finish();				
		}
	}		
	
	private void showToast(final String message) {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
			}
		});
	}
}