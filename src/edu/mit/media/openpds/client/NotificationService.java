package edu.mit.media.openpds.client;

import java.util.Map;

import edu.mit.media.funf.util.LogUtil;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class NotificationService extends IntentService {

	private WakeLock mWakeLock;
	
	public NotificationService() {
		super("NotificationService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {		
		//Intent openAppIntent = new Intent(this, MainActivity.class);		
		//PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_CANCEL_CURRENT)
		
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		mWakeLock.acquire();
		try {
			PersonalDataStore pds = new PersonalDataStore(this);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Map<Integer, Notification> notifications = pds.getNotifications();
			
			if (notifications != null) {
				for (Integer notificationType : notifications.keySet()) {			
					notificationManager.notify(notificationType, notifications.get(notificationType));
				}
			}
		} catch (Exception ex) {
			Log.e("NotificationService", ex.getMessage());
		}
		finally {
			if (mWakeLock.isHeld()) {
				mWakeLock.release();	
			}
		}
	}
	
}
