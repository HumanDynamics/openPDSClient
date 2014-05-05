package edu.mit.media.openpds.client.funf;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Context;
import android.util.Log;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.openpds.client.PersonalDataStore;

public class FunfPDS extends PersonalDataStore {

	private Context mContext;
	private static final String TAG = "FunfPDS";
	
	public FunfPDS(Context context) throws Exception {
		super(context);
		mContext = context;
	}
	
	public boolean savePipelineConfig(String name, Pipeline pipeline) {
		Log.v(TAG, "savePipeline for: " + name + " and pipeline: " + pipeline);
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

	public Map<String, Pipeline> getPipelines() { 
		Log.v(TAG, "getPipelines");
		Map<String, Pipeline> pipelines = new HashMap<String, Pipeline>();
		JsonArray pipelinesJsonArray = getPipelinesJsonArray();		
		
		if (pipelinesJsonArray != null) {
			Log.v(TAG, "pipelinesJsonArray is not null");
			Gson gson = FunfManager.getGsonBuilder(mContext).create();
			for (JsonElement pipelineJsonElement : getPipelinesJsonArray()) {
				try {
					JsonObject pipelineJsonObject = pipelineJsonElement.getAsJsonObject();
					Log.v(TAG, "pipelineJsonObject name: " + pipelineJsonObject.has("name"));
					if (pipelineJsonObject.has("name") && pipelineJsonObject.has("config")) {
						Log.v(TAG,pipelineJsonObject.get("config").toString());
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
		Log.v(TAG, "getPipelinesJsonArray");
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

	public String getFunfUploadUrl() {
		Log.v(TAG, "Funf upload url is:");
		Log.v(TAG, buildAbsoluteApiUrl("/funf_connector/set_funf_data"));
		return buildAbsoluteApiUrl("/funf_connector/set_funf_data");
	}
	
	public boolean uploadFunfData(File file) {
		Log.v(TAG, "uploadFunfData");
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
			conn.setRequestProperty("Authorization", "Bearer " + mPrefs.getAccessToken()); 
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
}
