package de.geeksfactory.opacclient;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ReminderBootBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
  	  	if(sp.getBoolean("notification_service", false) == false || sp.getString("opac_password", "").equals("")){
  			return;
  	  	}
  	  	
		Intent i = new Intent(context, ReminderAlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context,
				OpacClient.BROADCAST_REMINDER, i,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000,
				sender);
	}
}
