package de.geeksfactory.opacclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, ReminderCheckService.class);
		context.startService(i);
	}

}
