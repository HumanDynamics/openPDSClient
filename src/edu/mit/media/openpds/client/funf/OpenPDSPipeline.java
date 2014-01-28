package edu.mit.media.openpds.client.funf;


import static edu.mit.media.funf.util.AsyncSharedPrefs.async;

import java.io.IOException;

import com.google.android.gms.gcm.GoogleCloudMessaging;
// // NOTE: Commented out GCM support
//import com.google.android.gcm.GCMRegistrar;
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.openpds.client.PersonalDataStore;
import edu.mit.media.openpds.client.PreferencesWrapper;
import edu.mit.media.openpds.client.R;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.content.IntentSender;
import android.text.TextUtils;
import android.util.Log;

public class OpenPDSPipeline extends BasicPipeline {

	public static final String LAST_DATA_UPLOAD = "LAST_DATA_UPLOAD";	
	private AsyncTask<Void, Void, Void> mRegisterTask;
	private GoogleCloudMessaging mGcm;
	
	@Configurable
	private String GCMSenderId = null;
	
	@Override
	public void onCreate(final FunfManager manager) {
		super.onCreate(manager);	
		this.manager = manager;	
		// Handle GCM registration
		if (!TextUtils.isEmpty(GCMSenderId)) {
			final PersonalDataStore pds;
			try {
				pds = new PersonalDataStore(manager);
			} catch (Exception ex) {
				Log.e("OpenPDSPipeline", ex.getMessage());
				return;
			}
			
			mRegisterTask = new AsyncTask<Void,Void,Void>() {
	
				@Override
				protected Void doInBackground(Void... params) {
					PreferencesWrapper prefs = new PreferencesWrapper(manager);
					if (mGcm == null) {
						mGcm = GoogleCloudMessaging.getInstance(manager);
					}
					String regId = prefs.getGCMRegistrationId();
					if (regId == null) {
						try {
							regId = mGcm.register(GCMSenderId);
						} catch (IOException e) {
							Log.w("OpenPDSPipeline", "GCM registration failed", e);
						}
						// NOTE: the following method for registering GCM id on the server returns true if successful, false otherwise...
						// In the case of a failure, not persisting the regId locally means that we'll automatically retry the next time this service starts
						// Also - at the moment, we're not persisting app version for anything except GCM id... maybe these two writes should be combined
						if (pds.registerGCMDevice(regId)) {
							prefs.setGCMRegistrationId(regId);
							prefs.saveCurrentAppVersion();
						}
					}
					return null;
				}
				
				protected void onPostExecute(Void result) {
					mRegisterTask = null;
				}
			};
			
			mRegisterTask.execute(null, null, null);
		}
	}
	
	@Override
	public void onRun(String action, JsonElement config) {
		super.onRun(action, config);
		if (action.equalsIgnoreCase("archive")) {
			archiveData();
		}
		if (action.equalsIgnoreCase("upload")){
			uploadData();
		}
		if (action.equalsIgnoreCase("notify")) {
			checkForNotifications();
		}
		if (action.equalsIgnoreCase("update")) {
			updatePipelines();
		}
		if (action.equalsIgnoreCase("save")) {
			saveToPDS();
		}
	}
	
	
	@Override
	public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
		super.onDataReceived(probeConfig, data);
		String probeName = probeConfig.getAsJsonPrimitive("@type").getAsString();//probeConfig.get("@type").toString();
		long timestamp = data.get("timestamp").getAsLong();

		storeData(probeName, timestamp, data);
	}
		
	public void updatePipelines() {
//		new Thread() {
//			@Override
//			public void run() {
//				PDSWrapper pds = new PDSWrapper(manager);			
//				Map<String, Pipeline> pipelines = pds.getPipelines();
//				
//				for (String name : pipelines.keySet()) {
//					manager.registerPipeline(name, pipelines.get(name));
//				}
//			}
//		}.start();
	}
	
	public void saveToPDS() { 
//		new Thread() {
//			@Override
//			public void run() {
//				PDSWrapper pds = new PDSWrapper(manager);
//				pds.savePipelineConfig(manager.getPipelineName(MainPipelineV4.this), MainPipelineV4.this);
//			}
//		}.start();
	}
	
	private void checkForNotifications() {
//		Intent i = new Intent(manager, NotificationService.class);
//		manager.startService(i);
	}
	
	private void storeData(String name, long timestamp, IJsonObject data) {
		Bundle b = new Bundle();
		b.putString(NameValueDatabaseService.DATABASE_NAME_KEY,  manager.getPipelineName(this));
		b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
		b.putString(NameValueDatabaseService.NAME_KEY, name);
		b.putString(NameValueDatabaseService.VALUE_KEY, data.toString());
		Intent i = new Intent(manager, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_RECORD);
		i.putExtras(b);
		manager.startService(i);
	}
	
	public void archiveData() {
		Intent i = new Intent(manager, getDatabaseServiceClass());
		i.setAction(DatabaseService.ACTION_ARCHIVE);
		i.putExtra(DatabaseService.DATABASE_NAME_KEY, manager.getPipelineName(this));
		manager.startService(i);
	}
	
	public void uploadData() {		
		archiveData();
		
		String archiveName = manager.getPipelineName(this);
		Intent i = new Intent(manager, getUploadServiceClass());
		i.putExtra(UploadService.ARCHIVE_ID, archiveName);
		// Note: for our purposes, I don't think we really use the name...
		i.putExtra(UploadService.REMOTE_ARCHIVE_ID, "OpenPDSRemoteArchive");
		manager.startService(i);
	
		getSystemPrefs(manager).edit().putLong(LAST_DATA_UPLOAD, System.currentTimeMillis()).commit();
	}
	
	public static SharedPreferences getSystemPrefs(Context context) {
	
		return async(context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE));
	}
	
	public Class<? extends DatabaseService> getDatabaseServiceClass() {
		return NameValueDatabaseService.class;
	}
	
	public Class<? extends UploadService> getUploadServiceClass() {
		return HttpsUploadService.class;
	}
}
