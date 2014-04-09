package edu.mit.media.openpds.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;
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
	private static final String PROFILE_API_URL = "/account/api/v1/profile/";
	private static final String AUTHORIZATION_URL = "/oauth2/authorize";
	private static final String SHIBBOLETH_LOGIN_URL = "/Shibboleth.sso/Login";
	
	private RegistryConfig mConfig;
		
	public RegistryClient(RegistryConfig config) {
		mConfig = config;
	}	
	
	public RegistryClient(String url, String clientKey, String clientSecret, String scopes, String basicAuth) {
		assert(url != null && clientKey != null && clientSecret != null && scopes != null && basicAuth != null);
		mConfig = new RegistryConfig(url, clientKey, clientSecret, scopes, basicAuth);
	}
	
	public String getShibbolethAuthorizationUrl(String redirectUri) {
		String authUrl = "";
		try {
			authUrl = String.format("%s?client_id=%s&response_type=token&redirect_uri=%s", 
					RegistryClient.AUTHORIZATION_URL, 
					mConfig.getClientKey(),
					URLEncoder.encode(redirectUri, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(LOG_TAG, "Error while encoding Registry querystring parameter. Details follow.");
			Log.e(LOG_TAG, e.getMessage());
			return "";
		}
		
		return getAbsoluteUrl(RegistryClient.SHIBBOLETH_LOGIN_URL, new BasicNameValuePair("target", authUrl));
	}
	
	private String getAbsoluteUrl(String relativeUrl, NameValuePair... params) {
		String url = String.format("%s%s?", mConfig.getRegistryUrl(), relativeUrl);
		
		for (NameValuePair param : params) {
			try {
				url += String.format("%s=%s&", param.getName(), URLEncoder.encode(param.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Log.e(LOG_TAG, "Error while encoding Registry querystring parameter. Details follow.");
				Log.e(LOG_TAG, e.getMessage());
				return "";
			}
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
	 * Calls the authorization endpoint on the registry server and parses the response as an AuthorizationResponse.
	 * @param username The username of the user logging in
	 * @param password The user's password
	 * @return An AuthorizationResponse representing either a successful OAuth2 authorize response or an authorization error, or null if an exception occurred while processing the response
	 * @throws IOException Thrown if an error occurred while contacting the server - this typically means the server is having connectivity issues.
	 */
	public AuthorizationResponse authorize(String username, String password) throws IOException {		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					
		nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
		nameValuePairs.add(new BasicNameValuePair("username", username));
		nameValuePairs.add(new BasicNameValuePair("password", password));
		
		return authorize(nameValuePairs);	
	}
	
	/**
	 * Calls the authorization endpoint on the registry server and parses the response as an AuthorizationResponse
	 * @param code The authorization code used to contruct the token
	 * @return An AuthorizationResponse representing either a successful OAuth2 authorize response or an authentication error, or null if an exception occurred while processing the reponse
	 * @throws IOException Thrown if an error occurred while contacting the serve - this typically means the server or device is having connectivity issues.
	 */
	public AuthorizationResponse authorize(String code) throws IOException {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		
		nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
		nameValuePairs.add(new BasicNameValuePair("code", code));
		
		return authorize(nameValuePairs);	
	}
	
	protected AuthorizationResponse authorize(List<NameValuePair> nameValuePairs) throws IOException {
		nameValuePairs.add(new BasicNameValuePair("client_id", mConfig.getClientKey()));
		nameValuePairs.add(new BasicNameValuePair("client_secret", mConfig.getClientSecret()));
		nameValuePairs.add(new BasicNameValuePair("scope", mConfig.getScopes()));
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
	
	public AuthorizationResponse createProfileAndAuthorize(String email, String password, String firstName, String lastName) throws IOException {
		RegistryResponse createProfileResponse = createProfile(email, password, firstName, lastName);
		if (!createProfileResponse.success()) {
			return null;
		}
		
		return authorize(email, password);
	}
	
	public RegistryResponse createProfile(String email, String password, String firstName, String lastName) {
		JsonObject user = new JsonObject();
		user.addProperty("username", email);
		user.addProperty("email", email);
		user.addProperty("password", password);
		user.addProperty("first_name", firstName);
		user.addProperty("last_name", lastName);
		JsonObject profile = new JsonObject();
		profile.add("user", user);
		
		HttpPost request = new HttpPost(getAbsoluteUrl(PROFILE_API_URL));
		
		RegistryResponse response = new RegistryResponse();
		
		if (!postOrPut(request, profile.toString())) {
			response.setErrorInfo("Error creating profile", "Post to profile endpoint returned false");
		}
		
		return response;
	}
	
	protected boolean postOrPut(HttpEntityEnclosingRequestBase request, String data) {
		HttpResponse response = null;
		
		try {		
			StringEntity contentEntity = new StringEntity(data);
			request.setEntity(contentEntity);
			
			request.addHeader("Content-Type", "application/json");
			
			HttpClient client = new DefaultHttpClient();
			response = client.execute(request);
		} catch (ClientProtocolException e) {
			Log.w(LogUtil.TAG, "Error saving Pipeline to PDS");
			return false;
		} catch (IOException e) {
			Log.w(LogUtil.TAG, "IO Exception saving Pipeline to PDS");
			return false;
		}
		
		if 	(response != null &&
			(response.getStatusLine().getStatusCode() == 204 || response.getStatusLine().getStatusCode() == 201)) {
			return true;
		}
		
		if (response != null) {
			try {
				String responseContent = IOUtil.inputStreamToString(response.getEntity().getContent(), Charset.defaultCharset().name());
				Log.w(LogUtil.TAG, responseContent);
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	
}
