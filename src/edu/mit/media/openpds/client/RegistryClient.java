package edu.mit.media.openpds.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

/***
 * Represents a registry server within a Trust Framework, along with operations to signup, authorize clients, and
 * retrieve information about a user associated with an authorized token.
 * @author Brian Sweatt
 *
 */
public class RegistryClient {

	private static final String LOG_TAG = "RegistryServer";
	private static final String LOGIN_URL = "/account/login";
	private static final String SIGNUP_URL= "/account/signup";
	private static final String CLIENTS_URL = "/account/clients";
	private static final String TOKEN_URL = "/oauth2/token/";
	private static final String USERINFO_URL = "/oauth2/userinfo";	
	
	private RegistryConfig mConfig;
		
	public RegistryClient(RegistryConfig config) {
		mConfig = config;
	}
	
	private String getAbsoluteUrl(String relativeUrl, NameValuePair... params) {
		String url = String.format("%s%s?", mConfig.getRegistryUrl(), relativeUrl);
		
		for (NameValuePair param : params) {
			url += String.format("%s=%s&", param.getName(), param.getValue());
		}
		
		return url;
	}
		
	protected JSONObject getJSON(HttpResponse response) { 

		try {
			BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
			InputStream inputStream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			StringBuilder stringBuilder = new StringBuilder();
	
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
	
			return new JSONObject(stringBuilder.toString());
		} catch (JSONException jsonException) {
			Log.e(LOG_TAG, "Error parsing response to JSON: " + jsonException.getMessage());
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error parsing response: " + e.getMessage());
		}
		
		return null;
	}
	
	/***
	 * Calls the authorization endpoint on the registry server and parses the response as JSON. 
	 * The entire JSON is returned in order to allow consumers to handle response fields and errors as they please
	 * @param username The username of the user logging in
	 * @param password The user's password
	 * @return A JSONObject representing either a successful OAuth2 authorize response or an authorization error, or null if an exception occurred while processing the response
	 * @throws IOException Thrown if an error occurred while contacting the server - this typically means the server is having connectivity issues.
	 */
	public AuthorizationResponse authorize(String username, String password) throws IOException {
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					
		nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
		nameValuePairs.add(new BasicNameValuePair("client_id", mConfig.getClientKey()));
		nameValuePairs.add(new BasicNameValuePair("client_secret", mConfig.getClientSecret()));
		nameValuePairs.add(new BasicNameValuePair("scope", mConfig.getScopes()));
		nameValuePairs.add(new BasicNameValuePair("username", username));
		nameValuePairs.add(new BasicNameValuePair("password", password));

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(getAbsoluteUrl(TOKEN_URL));
		httppost.addHeader("AUTHORIZATION", mConfig.getBasicAuth());
		
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost);
			return new AuthorizationResponse(getJSON(response));
		} catch (UnsupportedEncodingException e) {
			Log.e(LOG_TAG, e.getMessage());
		}
		
		return null;	
	}
	
	/***
	 * Retrieves user information associated with an OAuth2 token from the registry server
	 * @param token The OAuth token to retrieve information about
	 * @return A JSONObject with fields for pds_location, id, name, and email for the user associated with the given token, or null if no such user exists.
	 * @throws IOException Thrown if an error occurred while contacting the registry server.
	 */
	public UserInfoResponse getUserInfo(String token) throws IOException {
		String url = getAbsoluteUrl(USERINFO_URL, new BasicNameValuePair("bearer_token", token));
		
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		
		HttpResponse response = httpClient.execute(httpGet);

		return new UserInfoResponse(getJSON(response));
	}
	
	
}
