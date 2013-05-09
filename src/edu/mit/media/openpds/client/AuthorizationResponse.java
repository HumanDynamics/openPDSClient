package edu.mit.media.openpds.client;

import org.json.JSONObject;
import android.util.Log;

public class AuthorizationResponse extends RegistryResponse {

	private String accessToken;
	private String refreshToken;
	private long tokenExpirationTime;
	private boolean shouldRetry = false;

	public AuthorizationResponse(JSONObject responseJson) {
		try {			
			if (responseJson == null) { 
				setErrorInfo("NullResponse", "Unable to parse UserInfo response");
			} else if (responseJson.has("error")) {
				setErrorInfo(responseJson.getString("error"), responseJson.getString("error_description"));
				Log.e(LOG_TAG, String.format("Error response to login: %s - %s", responseJson.getString("error"), responseJson.getString("error_description")));
				shouldRetry = true;
			} else if (responseJson.has("access_token") && responseJson.has("refresh_token") && responseJson.has("expires_in")) {
				setAccessToken( responseJson.getString("access_token"));
				setRefreshToken(responseJson.getString("refresh_token"));
				setTokenExpirationTime(System.currentTimeMillis() + (responseJson.getLong("expires_in") * 1000));
			}			
		} catch (Exception e) {
			setErrorInfo("MalformedResponse", e.getMessage());
			Log.e(LOG_TAG, "Error during authorization - " + e.getMessage());
		}
	}	
	
	public boolean shouldRetry() {
		return shouldRetry;
	}
	
	public String getAccessToken() {
		return accessToken;
	}

	protected void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	protected void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public long getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	protected void setTokenExpirationTime(long tokenExpirationTime) {
		this.tokenExpirationTime = tokenExpirationTime;
	}
}
