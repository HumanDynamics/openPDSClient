package edu.mit.media.openpds.client.funf;

import edu.mit.media.funf.FunfManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LauncherReceiver extends BroadcastReceiver {
	
	private static boolean launched = false;
	
	public static void launch(Context context) {
		try {
			FunfPDS pds = new FunfPDS(context);
		} catch (Exception ex) {
			// If we can't construct a PDS, don't collect data just yet.
			return;
		}
		startService(context.getApplicationContext(), FunfManager.class); // Ensure main funf system is running
		launched = true;
	}
	
	public static void startService(final Context context, final Class<? extends Service> serviceClass) {
		if (!launched) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					Intent i = new Intent(context.getApplicationContext(), serviceClass);
					context.getApplicationContext().startService(i);					
				}
			});
			
			thread.start();
		}
	}
	
	public static boolean isLaunched() {
		return launched;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		launch(context);
	}	
}
