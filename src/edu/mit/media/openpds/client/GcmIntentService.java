package edu.mit.media.openpds.client;

import com.google.android.gms.gcm.GoogleCloudMessaging;


import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class GcmIntentService extends IntentService {
    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	Log.w("GcmIntentService", "GCM SEND_ERROR message type");
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.w("GcmIntentService", "Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
        		if (extras.containsKey("action")) {
        			String action = extras.getString("action");
        			
        			if (action.equalsIgnoreCase("notify")) {
        				Intent notificationServiceIntent = new Intent(this, NotificationService.class);
        				startService(notificationServiceIntent);
        			} else if (action.equalsIgnoreCase("update")) {
        				// Run pipeline update here...
        			} else if (action.equalsIgnoreCase("save")) {
        				// Run pipeline save here...
        			}
        		}	
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}