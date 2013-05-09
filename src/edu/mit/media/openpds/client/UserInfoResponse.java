package edu.mit.media.openpds.client;

import org.json.JSONObject;
import android.util.Log;

public class UserInfoResponse extends RegistryResponse {	

	public UserInfoResponse(JSONObject responseJson) {		
		try {
			if (responseJson == null) {
				setErrorInfo("NullResponse", "Unable to parse UserInfo response");
			} else if (responseJson.has("error")) {
				setErrorInfo(responseJson.getString("error"), responseJson.getString("error_description"));				
				Log.e(LOG_TAG, String.format("Error while getting user info: %s - %s", getError(), getErrorDescription()));				
			} else if (!responseJson.has("id") || !responseJson.has("pds_location")) {
				setErrorInfo("ResponseIncomplete", "UserInfo response did not contain either the uuid or the pds_location for the user");		
			} else {
				setUUID(responseJson.getString("id"));
				setPDSLocation(responseJson.getString("pds_location"));
			}
		} catch (Exception e) {
			setErrorInfo("MalformedResponse", e.getMessage());
			Log.e(LOG_TAG, "Error getting user info - " + e.getMessage());
		}
	}
}
