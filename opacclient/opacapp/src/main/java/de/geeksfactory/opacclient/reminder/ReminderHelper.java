package de.geeksfactory.opacclient.reminder;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.LentItem;
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
        int warning = Integer.parseInt(sp.getString("notification_warning", "3"));

        AccountDataSource data = new AccountDataSource(app);
        data.open();
        List<LentItem> items = data.getAllLentItems();

        // Sort lent items by deadline
        Map<LocalDate, List<Long>> arrangedIds = new HashMap<>();
        for (LentItem item : items) {
            LocalDate deadline = item.getDeadline();
            if (!arrangedIds.containsKey(deadline)) {
                arrangedIds.put(deadline, new ArrayList<Long>());
            }
            arrangedIds.get(deadline).add(item.getDbId());
        }

        // Remove alarms with no corresponding media
        for (Alarm alarm : data.getAllAlarms()) {
            if (!arrangedIds.containsKey(alarm.deadline)) {
                data.removeAlarm(alarm);
            }
        }

        // Find and add/update corresponding alarms for current lent media
        for (Map.Entry<LocalDate, List<Long>> entry : arrangedIds.entrySet()) {
            LocalDate deadline = entry.getKey();
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
                                    DateTimeFormat.shortDate().print(deadline) + " on " +
                                    DateTimeFormat.shortDate().print(deadline.minusDays(warning)));
                }
                data.addAlarm(deadline, media,
                        deadline.minusDays(warning).toDateTimeAtStartOfDay());
            }
        }

        data.close();

        scheduleAlarms();
    }

    /**
     * Update alarms when the warning period setting is changed
     */
    public void updateAlarms() {
        // We could do this better, but for now, let's simply recreate all alarms. This can
        // result in some notifications being shown immediately.
        AccountDataSource data = new AccountDataSource(app);
        data.open();
        data.clearAlarms();
        data.close();
        generateAlarms();
    }

    /**
     * (re-)schedule alarms using {@link android.app.AlarmManager}
     */
    public void scheduleAlarms() {
        AccountDataSource data = new AccountDataSource(app);
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);

        data.open();
        List<Alarm> alarms = data.getAllAlarms();
        data.close();

        for (Alarm alarm : alarms) {
            if (!alarm.notified) {
                Intent i = new Intent(app, ReminderBroadcastReceiver.class);
                i.setAction(ReminderBroadcastReceiver.ACTION_SHOW_NOTIFICATION);
                i.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
                PendingIntent pi = PendingIntent
                        .getBroadcast(app, (int) alarm.id, i, PendingIntent.FLAG_UPDATE_CURRENT);
                // If the alarm's timestamp is in the past, AlarmManager will trigger it
                // immediately.
                alarmManager
                        .setExact(AlarmManager.RTC_WAKEUP, alarm.notificationTime.getMillis(), pi);
            }
        }
    }

    private long[] toArray(List<Long> list) {
        long[] array = new long[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
