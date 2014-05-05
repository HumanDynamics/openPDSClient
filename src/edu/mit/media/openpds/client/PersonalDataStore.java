package edu.mit.media.openpds.client;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.mit.media.funf.util.LogUtil;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class PersonalDataStore {

	protected Context mContext; 
	protected PreferencesWrapper mPrefs;
	private static final String TAG = "PersonalDataStore";
	
	public PersonalDataStore(Context context) throws Exception {
		mContext = context;
		mPrefs = new PreferencesWrapper(context);
		if (mPrefs.getAccessToken() == null && mPrefs.getPDSLocation() == null && mPrefs.getUUID() == null) {
			throw new Exception("SharedPreferences do not contain the necessary entries to construct a PDS");
		}
	}
	
	public Context getContext() {
		return mContext;
	}
	
	public String buildAbsoluteUrl(String relativeUrl) {
		String cleanedUrl = relativeUrl.replace("pds:/", "");
		String separator = relativeUrl.contains("?")? "&" : "?";
		Log.v(TAG, mPrefs.getPDSLocation() + ", "  + cleanedUrl);
		
		return String.format("%s%s%sbearer_token=%s&datastore_owner=%s", mPrefs.getPDSLocation(), cleanedUrl, separator, mPrefs.getAccessToken(), mPrefs.getUUID());
	}
	
	public String buildAbsoluteUrl(int resId) {
		return buildAbsoluteUrl(mContext.getString(resId));
	}
	
	protected String buildAbsoluteApiUrl(String relativeUrl) {
		String separator = relativeUrl.contains("?")? "&" : "?";
		return String.format("%s%s%sbearer_token=%s&datastore_owner__uuid=%s", mPrefs.getPDSLocation(), relativeUrl, separator, mPrefs.getAccessToken(), mPrefs.getUUID());
	}
	
	private String getNotificationApiUrl() {
		return buildAbsoluteApiUrl(mContext.getString(R.string.notification_api_relative_url));
	}
	
	public JSONObject getAnswer(String key) {
		String answerListUrl = buildAbsoluteApiUrl("/api/personal_data/answer/");
		HttpGet getAnswerListRequest = new HttpGet(answerListUrl);
		getAnswerListRequest.addHeader("Content-Type", "application/json");
		
		HttpClient client = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = null;
		
		try {
			responseBody = client.execute(getAnswerListRequest, responseHandler);
		} catch (ClientProtocolException e) {
	        client.getConnectionManager().shutdown();  
			return new JSONObject();
		} catch (IOException e) {
	        client.getConnectionManager().shutdown();  
			return new JSONObject();
		}
		
		try {
			JSONObject responseJson = new JSONObject(responseBody);
			JSONArray objectsJson = responseJson.getJSONArray("objects");
			
			// We're only interested in the first answer, for now - we might want to change this later
			if (objectsJson.length() > 0) {
				return objectsJson.getJSONObject(0).getJSONObject("value");
			}						
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new JSONObject();		
	}
	
	public JSONArray getAnswerList(String key) {
		String answerListUrl = buildAbsoluteApiUrl("/api/personal_data/answerlist/");
		HttpGet getAnswerListRequest = new HttpGet(answerListUrl);
		getAnswerListRequest.addHeader("Content-Type", "application/json");
		
		HttpClient client = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = null;
		
		try {
			responseBody = client.execute(getAnswerListRequest, responseHandler);
		} catch (ClientProtocolException e) {
	        client.getConnectionManager().shutdown();  
			return null;
		} catch (IOException e) {
	        client.getConnectionManager().shutdown();  
			return null;
		}
		
		try {
			JSONObject responseJson = new JSONObject(responseBody);
			JSONArray objectsJson = responseJson.getJSONArray("objects");
			
			// We're only interested in the first answer, for now - we might want to change this later
			if (objectsJson.length() > 0) {
				return objectsJson.getJSONObject(0).getJSONArray("value");
			}						
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new JSONArray();		
	}
	
	/**
	 * Register this device with GCM on the PDS
	 * NOTE: this method blocks on server communication - DO NOT RUN IN THE UI THREAD!
	 * @return true if registration was successful, false otherwise
	 */
	public boolean registerGCMDevice(String regId) {	
		JsonObject deviceJsonObject = new JsonObject();
		JsonObject datastoreOwner = new JsonObject();
		datastoreOwner.addProperty("uuid", mPrefs.getUUID());
		deviceJsonObject.add("datastore_owner", datastoreOwner);
		deviceJsonObject.addProperty("gcm_reg_id", regId);
		
		String deviceUrl = buildAbsoluteApiUrl(getContext().getString(R.string.device_api_relative_url));
		
		HttpPost deviceRequest = new HttpPost(deviceUrl);
		
		if (postOrPut(deviceRequest, deviceJsonObject.toString())) {
			storeRegistrationId(regId);
			return true;
		}	
		// If we got this far, and regId is not empty, we succeeded
		return false;
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(String regId) {
		mPrefs.saveCurrentAppVersion();
		mPrefs.setGCMRegistrationId(regId);
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
			Log.w(LogUtil.TAG, "Error posting or putting to PDS.");
			return false;
		} catch (IOException e) {
			Log.w(LogUtil.TAG, "IO Exception posting or putting to PDS.");
			return false;
		} catch (Exception e) {
			Log.w(LogUtil.TAG, "Generic Exception posting or putting to PDS.", e);
			return false;
		}
		
		if 	(response != null &&
			(response.getStatusLine().getStatusCode() == 204 || response.getStatusLine().getStatusCode() == 201)) {
			return true;
		}
		
		if (response != null) {
			try {
				String responseContent = inputStreamToString(response.getEntity().getContent(), Charset.defaultCharset().name());
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
	
	public Map<Integer, Notification> getNotifications() {
		HttpGet getNotificationsRequest = new HttpGet(getNotificationApiUrl());
		getNotificationsRequest.addHeader("Content-Type", "application/json");
		
		HttpClient client = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = null;
		
		try {
			responseBody = client.execute(getNotificationsRequest, responseHandler);
		} catch (ClientProtocolException e) {
	        client.getConnectionManager().shutdown();  
			return null;
		} catch (IOException e) {
	        client.getConnectionManager().shutdown();  
			return null;
		}
		
		Map<Integer, Notification> notifications = new HashMap<Integer, Notification>();
		ArrayList<String> notificationsToDelete = new ArrayList<String>();
		
		try {
			JSONObject notificationsBody = new  JSONObject(responseBody);
			JSONArray notificationsArray = notificationsBody.getJSONArray("objects");
			
			for (int i = 0; i < notificationsArray.length(); i++) {
				JSONObject notification = notificationsArray.optJSONObject(i);
				
				if (notification != null) {
					NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
					builder.setContentTitle(notification.getString("title")).setContentText(notification.getString("content")).setSmallIcon(R.drawable.ic_launcher).setVibrate(new long[] { 0, 100, 50, 100, 50, 100 });
				
					if (!TextUtils.isEmpty(notification.optString("uri"))) {
						Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(notification.getString("uri")));
						builder.setContentIntent(PendingIntent.getActivity(getContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
					}
					
					notifications.put(notification.getInt("type"), builder.build());
					
					if (notification.getInt("type") > 0) {
						notificationsToDelete.add(notification.getString("resource_uri"));
					}
				}
			}			
		} catch (JSONException e) {
			return null;
		}
		
		for (String uriToDelete : notificationsToDelete) {		
			// if we've gotten this far, we've successfully parsed all of the notifications, so clear the list on the server
			HttpDelete deleteNotificationsRequest = new HttpDelete(buildAbsoluteApiUrl(uriToDelete));
			
			try {
				// We don't care about the response here - if it succeeds, then no exception is thrown and the response has no content
				client.execute(deleteNotificationsRequest);
			} catch (ClientProtocolException e) {
				// Log something here
			} catch (IOException e) {
				// Log something here
			}
		}
		
		client.getConnectionManager().shutdown();  
				
		return notifications;
	}
	
	public static String inputStreamToString(InputStream is, String encoding) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(is, encoding);
		int read;
		do {
		  read = in.read(buffer, 0, buffer.length);
		  if (read>0) {
		    out.append(buffer, 0, read);
		  }
		} while (read>=0);
		return out.toString();
	}
}
