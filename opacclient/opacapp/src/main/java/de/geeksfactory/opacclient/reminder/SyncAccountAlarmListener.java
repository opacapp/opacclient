package de.geeksfactory.opacclient.reminder;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.joda.time.Minutes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SyncAccountAlarmListener implements WakefulIntentService.AlarmListener {

    public static final String PREF_SYNC_INTERVAL = "sync_interval";
    private boolean onePeriodBeforeStart = false;

    /**
     * @return a SyncAccountAlarmListener that will wait for one period before the first scheduled
     * alarm. Used for re-scheduling alarms from {@link SyncAccountService}.
     */
    public static SyncAccountAlarmListener withOnePeriodBeforeStart() {
        SyncAccountAlarmListener listener = new SyncAccountAlarmListener();
        listener.onePeriodBeforeStart = true;
        return listener;
    }

    @Override
    public void scheduleAlarms(AlarmManager am, PendingIntent pi, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long interval = prefs.getLong(PREF_SYNC_INTERVAL, AlarmManager.INTERVAL_HALF_DAY);
        long firstStart;
        if (onePeriodBeforeStart) {
            // Re-scheduled from SyncAccountService: Wait one period before the first alarm.
            firstStart = System.currentTimeMillis() + interval;
        } else {
            // After reboot/app install/force close: Wait only 10 minutes before the first alarm
            firstStart = System.currentTimeMillis() +
                    Minutes.minutes(10).toStandardDuration().getMillis();
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstStart, interval, pi);
    }

    @Override
    public void sendWakefulWork(Context context) {
        WakefulIntentService.sendWakefulWork(context, SyncAccountService.class);
    }

    @Override
    public long getMaxAge() {
        return (long) (AlarmManager.INTERVAL_HALF_DAY * 1.25);
    }
}
