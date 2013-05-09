package edu.mit.media.openpds.client.funf;


import static edu.mit.media.funf.util.AsyncSharedPrefs.async;


// // NOTE: Commented out GCM support
//import com.google.android.gcm.GCMRegistrar;
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.openpds.client.R;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.content.IntentSender;

public class OpenPDSPipeline extends BasicPipeline {

	public static final String LAST_DATA_UPLOAD = "LAST_DATA_UPLOAD";	
	private AsyncTask<Void, Void, Void> mRegisterTask;
	
	@Override
	public void onCreate(final FunfManager manager) {
		super.onCreate(manager);	
		this.manager = manager;	
		// Handle GCM registration
		//final PersonalDataStore pds = new PersonalDataStore(manager);
		
		mRegisterTask = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
// // NOTE: commented out GCM support
//				GCMRegistrar.checkDevice(manager);
//				GCMRegistrar.checkManifest(manager);
//				String regId = GCMRegistrar.getRegistrationId(manager);
//				if (regId.equals("")) {
//					GCMRegistrar.register(manager, manager.getString(R.string.gcm_sender_id));
//					// NOTE: don't need to register with server here as the GCMIntentService will handle that
//				} else if (!GCMRegistrar.isRegisteredOnServer(manager) && !pds.registerGCMDevice(regId)) {
//					GCMRegistrar.unregister(manager);					
//				}	
				return null;
			}
			
			protected void onPostExecute(Void result) {
				mRegisterTask = null;
			}
		};
		
		mRegisterTask.execute(null, null, null);
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
