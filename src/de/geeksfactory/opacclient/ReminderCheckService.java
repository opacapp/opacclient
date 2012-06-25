package de.geeksfactory.opacclient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class ReminderCheckService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		Log.i("service", "start");
		
  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ReminderCheckService.this);
  	  	if(sp.getBoolean("notification_service", false) == false || sp.getString("opac_password", "").equals("")){
  	  		stopSelf();
  			return START_STICKY;
  	  	}
        
  	  	if(((OpacClient) getApplication()).isOnline()){
  			new CheckTask().execute();
  	  	}else{
			Intent i = new Intent(ReminderCheckService.this, ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(ReminderCheckService.this, OpacClient.BROADCAST_REMINDER, i, PendingIntent.FLAG_UPDATE_CURRENT);
	        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+(1000*3600*1), sender);
	        stopSelf();
  	  	}
  	  	
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public class CheckTask extends AsyncTask<Object, Object, Long[]> {

		@Override
		protected Long[] doInBackground(Object... params) {
	  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ReminderCheckService.this);
			OpacWebApi ohc = new OpacWebApi(sp.getString("opac_url", getResources().getString(R.string.opac_mannheim)), ReminderCheckService.this, ((OpacClient) getApplication()).get_bib());
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			long now = new Date().getTime();
			long warning = Long.decode(sp.getString("notification_warning", "367200000"));
			long E = 0;
			long abs = 0;
			long last = sp.getLong("notification_last", 0);
			try {
        		List<List<String[]>> res = ohc.account(sp.getString("opac_usernr", ""), sp.getString("opac_password", ""));
        		if(res != null){
					for(int i = 0; i < res.get(0).size(); i++){
						try {
							Date expiring = sdf.parse(res.get(0).get(i)[3]);
							if((expiring.getTime()-now) < warning){
								abs++;
								if(expiring.getTime() >= last){
									E++;
								}
							}
							if(expiring.getTime() > last){
								last = expiring.getTime();
							}
							Log.i("book", (expiring.getTime()-now)+" "+warning);
						}catch(Exception e){
			            	e.printStackTrace();
						}
					}
        		}
        		Long[] r = {E, abs, last};
        		return r;
            }catch(Exception e){
            	e.printStackTrace();
            }
			return null;
		}
		
		protected void onPostExecute(Long[] result) {
			long E = result[0];
			long abs = result[1];
			long last = result[2];
			if(E == 0) return;
			
	  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ReminderCheckService.this);
		    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		    int icon = android.R.drawable.stat_notify_error;
		    
		    long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, getString(R.string.notif_ticker, abs), when);
		    
		    Context context = getApplicationContext();
		    CharSequence contentTitle = getString(R.string.notif_title);
		    CharSequence contentText = getString(R.string.notif_ticker, abs);
		    Intent notificationIntent = new Intent(ReminderCheckService.this, AccountActivity.class);
		    notificationIntent.putExtra("notif_last", last);
		    PendingIntent contentIntent = PendingIntent.getActivity(ReminderCheckService.this, 0, notificationIntent, 0);

		    if(!sp.getString("notification_sound", "").equals("")){
		    	notification.sound = Uri.parse(sp.getString("notification_sound", ""));
		    }
		    notification.number = (int) E;
		    
		    notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		    mNotificationManager.notify(OpacClient.NOTIF_ID, notification);
	  	  	
			Intent i = new Intent(ReminderCheckService.this, ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(ReminderCheckService.this, OpacClient.BROADCAST_REMINDER, i, PendingIntent.FLAG_UPDATE_CURRENT);
	 
	        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+(1000*3600*5), sender);
	        
		    stopSelf();
		}
		
	}

}
