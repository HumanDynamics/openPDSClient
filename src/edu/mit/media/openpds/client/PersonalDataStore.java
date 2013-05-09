package edu.mit.media.openpds.client;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// // NOTE: Commented out GCM support
//import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.openpds.client.funf.OpenPDSPipeline;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PersonalDataStore {

	private Context mContext; 
	private PreferencesWrapper mPrefs;
	
	public PersonalDataStore(Context context) {
		mContext = context;
		mPrefs = new PreferencesWrapper(context);
		assert(mPrefs.getAccessToken() != null && mPrefs.getPDSLocation() != null && mPrefs.getUUID() != null);
	}
	
	public String getFunfUploadUrl() {
		return buildAbsoluteApiUrl("/funf_connector/set_funf_data");
	}
	
	public String buildAbsoluteUrl(String relativeUrl) {
		String cleanedUrl = relativeUrl.replace("pds:/", "");
		String separator = relativeUrl.contains("?")? "&" : "?";
		
		return String.format("%s%s%sbearer_token=%s&datastore_owner=%s", mPrefs.getPDSLocation(), cleanedUrl, separator, mPrefs.getAccessToken(), mPrefs.getUUID());
	}
	
	public String buildAbsoluteUrl(int resId) {
		return buildAbsoluteUrl(mContext.getString(resId));
	}
	
	protected String buildAbsoluteApiUrl(String relativeUrl) {
		return String.format("%s%s?bearer_token=%s&datastore_owner__uuid=%s", mPrefs.getPDSLocation(), relativeUrl, mPrefs.getAccessToken(), mPrefs.getUUID());
	}
	
	private String getNotificationApiUrl() {
		return buildAbsoluteApiUrl(mContext.getString(R.string.notification_api_relative_url));
	}
	
	public Boolean savePipelineConfig(String name, Pipeline pipeline) {
		String resourceUrl = buildAbsoluteApiUrl("/api/personal_data/funfconfig/");
		JsonArray pipelinesJsonArray = getPipelinesJsonArray();
		JsonObject pipelineJsonObject = null;
		Boolean exists = false;
		Gson gson = FunfManager.getGsonBuilder(mContext).create();
		
		if (pipelinesJsonArray != null) {
			for (JsonElement pipelineJsonElement : pipelinesJsonArray) { 
				pipelineJsonObject = pipelineJsonElement.getAsJsonObject(); 
				if (pipelineJsonObject.has("name") && pipelineJsonObject.get("name").getAsString().equals(name)) {
					if (pipelineJsonObject.has("config")) {
						pipelineJsonObject.remove("config");
					}
					pipelineJsonObject.add("config", gson.toJsonTree(pipeline));
					resourceUrl = buildAbsoluteApiUrl(pipelineJsonObject.get("resource_uri").getAsString());
					exists = true;
				}				
			}
		}
		
		if (pipelineJsonObject == null) {
			pipelineJsonObject = new JsonObject();			 
			pipelineJsonObject.addProperty("name", name);
			pipelineJsonObject.add("config", gson.toJsonTree(pipeline));
		}
		
		HttpEntityEnclosingRequestBase savePipelineRequest = (exists)? new HttpPut(resourceUrl) : new HttpPost(resourceUrl);

		return postOrPut(savePipelineRequest, pipelineJsonObject.toString());		
	}
	
	/**
	 * Register this device with GCM on the PDS
	 * NOTE: this method blocks on server communication - DO NOT RUN IN THE UI THREAD!
	 * @return true if registration was successful, false otherwise
	 */
	public boolean registerGCMDevice(String regId) {	
// // NOTE: commented out GCM support
//		JsonObject deviceJsonObject = new JsonObject();
//		JsonObject datastoreOwner = new JsonObject();
//		datastoreOwner.addProperty("uuid", mPrefs.getUUID());
//		deviceJsonObject.add("datastore_owner", datastoreOwner);
//		deviceJsonObject.addProperty("gcm_reg_id", regId);
//		
//		String deviceUrl = buildAbsoluteApiUrl(mContext.getString(R.string.device_api_relative_url));
//		
//		HttpPost deviceRequest = new HttpPost(deviceUrl);
//		
//		if (postOrPut(deviceRequest, deviceJsonObject.toString())) {
//			GCMRegistrar.setRegisteredOnServer(mContext, true);
//			return true;
//		}	
//		// If we got this far, and regId is not empty, we succeeded
		return false;
	}

	// always verify the host - dont check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};


	/**
	 * Trust every server - dont check for any certificate
	 */
	public static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
			.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean uploadFunfData(File file) {
		if (getFunfUploadUrl() == null || getFunfUploadUrl().length() == 0) {
			return false;
		}
		
		HttpURLConnection conn = null; 
		DataOutputStream dos = null; 

		String lineEnd = "\r\n"; 
		String twoHyphens = "--"; 
		String boundary =  "*****"; 

		int bytesRead, bytesAvailable, bufferSize; 
		byte[] buffer; 
		int maxBufferSize = 64*1024; //old value 1024*1024 

		boolean isSuccess = true;
		try 
		{ 
			Log.d("UPLOADDATA", "starting file upload");
			//------------------ CLIENT REQUEST 
			FileInputStream fileInputStream = null;
			//Log.i("FNF","UploadService Runnable: 1"); 
			try {
				fileInputStream = new FileInputStream(file); 
			}catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e("Funf", "file not found");
			}
			// open a URL connection to the Servlet 
			Log.d("UPLOADDATA", "upload url: "+getFunfUploadUrl());
			URL url = new URL(getFunfUploadUrl()); 
			// Open a HTTP connection to the URL 
			if (url.getProtocol().toLowerCase().equals("https")) {
				trustAllHosts();
				HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			// Allow Inputs 
			conn.setDoInput(true); 
			// Allow Outputs 
			conn.setDoOutput(true); 
			// Don't use a cached copy. 
			conn.setUseCaches(false); 
			// set timeout
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			// Use a post method. 
			conn.setRequestMethod("POST"); 
			conn.setRequestProperty("Connection", "Keep-Alive"); 
			conn.setRequestProperty("Authorization", "Bearer " +mPrefs.getAccessToken()); 
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary); 

			dos = new DataOutputStream( conn.getOutputStream() ); 
			dos.writeBytes(twoHyphens + boundary + lineEnd); 
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + file.getName() +"\"" + lineEnd); 
			dos.writeBytes(lineEnd); 

			//Log.i("FNF","UploadService Runnable:Headers are written"); 

			// create a buffer of maximum size 
			bytesAvailable = fileInputStream.available(); 
			bufferSize = Math.min(bytesAvailable, maxBufferSize); 
			buffer = new byte[bufferSize]; 

			// read file and write it into form... 
			bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
			while (bytesRead > 0) 
			{ 
				dos.write(buffer, 0, bufferSize); 
				bytesAvailable = fileInputStream.available(); 
				bufferSize = Math.min(bytesAvailable, maxBufferSize); 
				bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
			} 

			// send multipart form data necesssary after file data... 
			dos.writeBytes(lineEnd); 
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd); 

			// close streams 
			//Log.i("FNF","UploadService Runnable:File is written"); 
			fileInputStream.close(); 
			dos.flush(); 
			dos.close(); 
		} 
		catch (Exception e) 
		{ 
			Log.e("FNF", "UploadService Runnable:Client Request error", e);
			isSuccess = false;
		} 

		//------------------ read the SERVER RESPONSE 
		try {
			if (conn.getResponseCode() != 200) {
				isSuccess = false;
			}
		} catch (Exception e) {
			Log.e("FNF", "Connection error", e);
			isSuccess = false;
		}

		Log.d("UPLOADDATA", "ending file upload");
		return isSuccess;
	}
	
	private Boolean postOrPut(HttpEntityEnclosingRequestBase request, String data) {
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

	public Map<String, Pipeline> getPipelines() { 
		Map<String, Pipeline> pipelines = new HashMap<String, Pipeline>();
		JsonArray pipelinesJsonArray = getPipelinesJsonArray();		
		
		if (pipelinesJsonArray != null) {
			Gson gson = FunfManager.getGsonBuilder(mContext).create();
			for (JsonElement pipelineJsonElement : getPipelinesJsonArray()) {
				try {
					JsonObject pipelineJsonObject = pipelineJsonElement.getAsJsonObject();
					if (pipelineJsonObject.has("name") && pipelineJsonObject.has("config")) {
						Pipeline pipeline = gson.fromJson(pipelineJsonObject.get("config"), OpenPDSPipeline.class);
						pipelines.put(pipelineJsonObject.get("name").getAsString(), pipeline);
					}
				} catch (Exception e) {
					Log.w(LogUtil.TAG, "Error creating pipelines from PDS configs", e);
				}
			}
		}
		
		return pipelines;
	}
	
	protected JsonArray getPipelinesJsonArray() {
		HttpGet getPipelinesRequest = new HttpGet(buildAbsoluteApiUrl("/api/personal_data/funfconfig/"));
		getPipelinesRequest.addHeader("Content-Type", "application/json");		
		HttpClient client = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = null;
		JsonParser parser = new JsonParser();
		
		try {
			responseBody = client.execute(getPipelinesRequest, responseHandler);
		} catch (ClientProtocolException e) {
	        client.getConnectionManager().shutdown();  
			return new JsonArray();
		} catch (IOException e) {
	        client.getConnectionManager().shutdown();  
			return new JsonArray();
		}
		
		try {			
			JsonObject pipelinesBody = parser.parse(responseBody).getAsJsonObject();
			
			if (pipelinesBody.has("objects")) {
				return pipelinesBody.getAsJsonArray("objects");
			}
		} catch (Exception e) {
			Log.w(LogUtil.TAG, "Error parsing pipeline updates");			
		}
		
		return new JsonArray();
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
					builder.setContentTitle(notification.getString("title")).setContentText(notification.getString("content")).setSmallIcon(R.drawable.ic_launcher);
					
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
}
