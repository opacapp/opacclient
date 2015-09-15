package de.geeksfactory.opacclient.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderHelper {
    private OpacClient app;
    private SharedPreferences sp;

    public ReminderHelper(OpacClient app) {
        this.app = app;
        sp = PreferenceManager.getDefaultSharedPreferences(app);
    }

    /**
     * Save alarms for expiring media to the DB and schedule them using {@link
     * android.app.AlarmManager}
     */
    public void generateAlarms() {
        long warning = Long.parseLong(sp.getString("notification_warning", "367200000"));

        AccountDataSource data = new AccountDataSource(app);
        data.open();
        List<Map<String, String>> items = data.getAllLentItems();

        // Sort lent items by deadline
        Map<Long, List<Long>> arrangedIds = new HashMap<>();
        for (Map<String, String> item : items) {
            long deadline = Long.parseLong(item.get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP));
            if (!arrangedIds.containsKey(deadline)) {
                arrangedIds.put(deadline, new ArrayList<Long>());
            }
            arrangedIds.get(deadline).add(Long.parseLong(item.get("id")));
        }

        // Remove alarms with no corresponding media
        for (Alarm alarm : data.getAllAlarms()) {
            if (!arrangedIds.containsKey(alarm.deadlineTimestamp)) {
                data.removeAlarm(alarm);
            }
        }

        // Find and add/update corresponding alarms for current lent media
        for (Map.Entry<Long, List<Long>> entry : arrangedIds.entrySet()) {
            long deadline = entry.getKey();
            long[] media = toArray(entry.getValue());
            Alarm alarm = data.getAlarmByDeadline(deadline);
            if (alarm != null) {
                if (!Arrays.equals(media, alarm.media)) {
                    alarm.media = media;
                    data.updateAlarm(alarm);
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.i("OpacClient",
                            "scheduling alarm for " + media.length + " items with deadline on " +
                                    new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                                            .format(new Date(deadline)));
                }
                data.addAlarm(deadline, media, deadline - warning);
            }
        }

        data.close();

        scheduleAlarms();
    }

    /**
     * Update alarms when the warning period setting is changed
     *
     * @param oldWarning previous warning period
     * @param newWarning new warning period
     */
    public void updateAlarms(long oldWarning, long newWarning) {
        // TODO: implement
    }

    /**
     * (re-)schedule alarms using {@link android.app.AlarmManager}
     */
    public void scheduleAlarms() {
        AccountDataSource data = new AccountDataSource(app);
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        data.open();

        List<Alarm> alarms = data.getAllAlarms();
        for (Alarm alarm : alarms) {
            if (!alarm.notified) {
                Intent i = new Intent(app, ReminderBroadcastReceiver.class);
                i.setAction(ReminderBroadcastReceiver.ACTION_SHOW_NOTIFICATION);
                i.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
                PendingIntent pi = PendingIntent
                        .getBroadcast(app, (int) alarm.id, i, PendingIntent.FLAG_UPDATE_CURRENT);
                // If the alarm's timestamp is in the past, AlarmManager will trigger it
                // immediately.
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.notificationTimestamp, pi);
            }
        }

        data.close();
    }

    private long[] toArray(List<Long> list) {
        long[] array = new long[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
