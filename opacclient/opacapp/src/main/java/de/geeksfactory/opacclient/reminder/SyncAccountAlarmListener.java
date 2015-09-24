package de.geeksfactory.opacclient.reminder;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

public class SyncAccountAlarmListener implements WakefulIntentService.AlarmListener {
    // We could use a longer interval like 3 hours, but on API < 19, only the AlarmManager
    // .INTERVAL_... constants make the AlarmManager use inexact alarm delivery for better
    // battery efficiency. The next larger constant available would be AlarmManager
    // .INTERVAL_HALF_DAY.
    private static final long INTERVAL = AlarmManager.INTERVAL_HOUR;


    @Override
    public void scheduleAlarms(AlarmManager am, PendingIntent pi, Context context) {
        // Wait something around half an hour after system has booted
        long firstStart = System.currentTimeMillis() + (1000 * 1800);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstStart, INTERVAL, pi);
    }

    @Override
    public void sendWakefulWork(Context context) {
        WakefulIntentService.sendWakefulWork(context, SyncAccountService.class);
    }

    @Override
    public long getMaxAge() {
        return INTERVAL * 2;
    }
}
