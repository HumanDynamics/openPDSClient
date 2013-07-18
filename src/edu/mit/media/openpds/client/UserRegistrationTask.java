package edu.mit.media.openpds.client;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class UserRegistrationTask extends AsyncTask<String, Void, String> {
	
	private static final String LOG_TAG = "UserRegistrationTask";
	
	private Activity mActivity;
	private PreferencesWrapper mPrefs;
	private RegistryClient mRegistryClient;
	
	public UserRegistrationTask(Activity activity, PreferencesWrapper prefs, RegistryClient registryClient) {
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
		if (params.length != 3) {
			Log.e(LOG_TAG, "UserLoginTask requires username and password as parameters.");
			throw new IllegalArgumentException("UserRegistrationTask requires username, password, and name as parameters.");
		}
		
		String name = params[0];
		String email = params[1];
		String password = params[2];
		String[] nameParts = name.split(" ");
		
		String firstName = nameParts.length > 0? nameParts[0] : "";
		String lastName = nameParts.length > 1? nameParts[1] : "";
		
		AuthorizationResponse authResponse = null;
		
		try {
			authResponse = mRegistryClient.createProfileAndAuthorize(email, password, firstName, lastName);			
			
			if (authResponse == null) {
				showToast("An account with that email already exists.");
				return null;
			} else if (!authResponse.success() && !authResponse.shouldRetry()) { 
				showToast("Registry server is broken. Please contact the system administrators.");
				return null;
			} else if (!authResponse.success() && authResponse.shouldRetry()) {
				showToast("Registration failed. Please try again.");
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
		} 
	}
}