package edu.mit.media.openpds.client;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserLoginTask extends AsyncTask<String, Void, String> {
	
	private static final String LOG_TAG = "UserLoginTask";
	
	private Activity mActivity;
	private PreferencesWrapper mPrefs;
	private RegistryClient mRegistryClient;
	
	public UserLoginTask(Activity activity, PreferencesWrapper prefs, RegistryClient registryClient) {
		mActivity = activity;
		mRegistryClient = registryClient;
		mPrefs = prefs;
	}
	
	private void showToast(final String message) {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	@Override
	protected String doInBackground(String... params)  {
		if (params.length != 2) {
			Log.e(LOG_TAG, "UserLoginTask requires username and password as parameters.");
			throw new IllegalArgumentException("UserLoginTask requires username and password as parameters.");
		}
		
		String username = params[0];
		String password = params[1];

		AuthorizationResponse authResponse = null;
		
		try {
			authResponse = mRegistryClient.authorize(username, password);			
			
			if (!authResponse.success() && !authResponse.shouldRetry()) { 
				showToast("Registry server is broken. Please contact brian717@media.mit.edu");
				return null;
			} else if (!authResponse.success() && authResponse.shouldRetry()) {
				showToast("Login failed - please check your username and password.");
				return null;
			} else if (authResponse.success()) {
				mPrefs.setAccessToken(authResponse.getAccessToken());
				mPrefs.setRefreshToken(authResponse.getRefreshToken());
				mPrefs.setTokenExpirationTime(authResponse.getTokenExpirationTime());
				
				return authResponse.getAccessToken();
			}			
		} catch (Exception e) {				
			showToast("Failed contacting the server. Please try again later.");
			Log.e(LOG_TAG, String.format("Error during login: %s", e.getMessage()));
		}

		return null;
	}

	@Override
	protected void onPostExecute(final String token) {

		if (token != null) {
			showToast("Login Successful");
			
			UserInfoTask userInfoTask = new UserInfoTask(mActivity, mPrefs, mRegistryClient);			
			userInfoTask.execute(token);
			onComplete();
		} else {
			onError();
		}
	}
	
	protected void onComplete() {
		return;
	}
	
	protected void onError() {
		return;
	}
}