package de.geeksfactory.opacclient.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.geeksfactory.opacclient.OpacClient;

public class ReminderBootBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		
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
