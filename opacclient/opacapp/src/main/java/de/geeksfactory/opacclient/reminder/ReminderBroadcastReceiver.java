package de.geeksfactory.opacclient.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.PeriodFormat;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;
import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.SnoozeDatePickerActivity;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.DataIntegrityException;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALARM_ID = "alarmId";
    public static final String ACTION_SHOW_NOTIFICATION = "show";
    public static final String ACTION_NOTIFICATION_DELETED = "deleted";
    public static final String ACTION_NOTIFICATION_DONT_REMIND_AGAIN = "dontremindagain";
    public static final String NOTIFICATION_CHANNEL_REMINDER = "reminder";
    private static final String LOG_TAG = "ReminderBroadcastRcvr";

    private AccountDataSource adata;
    private Alarm alarm;
    private Context context;
    private Intent intent;
    private SharedPreferences prefs;

    private void setUpChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_REMINDER,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(false);
        channel.setSound(null, null);

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        adata = new AccountDataSource(context);
        alarm = adata.getAlarm(intent.getLongExtra(EXTRA_ALARM_ID, -1));

        if (alarm == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_SHOW_NOTIFICATION:
                showNotification();
                break;
            case ACTION_NOTIFICATION_DELETED:
                notificationDeleted();
                break;
            case ACTION_NOTIFICATION_DONT_REMIND_AGAIN:
                notificationDontRemindAgain();
                break;
        }
        // reschedule alarms
        new ReminderHelper((OpacClient) context.getApplicationContext()).scheduleAlarms();
    }

    private void showNotification() {
        setUpChannel();

        if (!prefs.getBoolean("notification_service", false)) {
            if (BuildConfig.DEBUG) Log.i(LOG_TAG, "not showing notification because disabled");
            return;
        }
        if (alarm.notified) {
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "not showing notification because already notified");
            }
            return;
        }
        if (BuildConfig.DEBUG) Log.i(LOG_TAG, "showing notification");

        List<LentItem> expiringItems = new ArrayList<>();
        List<Long> updatedItemIds = new ArrayList<>();

        for (long mediaId : alarm.media) {
            LentItem item = adata.getLentItem(mediaId);
            if (item == null) {
                if (BuildConfig.DEBUG) {
                    throw new DataIntegrityException(
                            "Unknown media ID " + mediaId + " in alarm with deadline " +
                                    alarm.deadline.toString());
                }
            } else {
                expiringItems.add(item);
                updatedItemIds.add(mediaId);
            }
        }

        if (expiringItems.size() == 0) {
            adata.removeAlarm(alarm);
            return;
        }

        alarm.media = listToLongArray(updatedItemIds);

        String notificationText;
        String notificationTitle;
        // You can still return the item on the day it expires on, so don't consider it to have
        // expired before the next day
        if (alarm.deadline.isBefore(LocalDate.now())) {
            notificationText = context.getResources().getQuantityString(
                    R.plurals.notif_ticker_expired, expiringItems.size(), expiringItems.size());
            notificationTitle = context.getString(R.string.notif_title_expired);
        } else {
            notificationText = context.getResources().getQuantityString(
                    R.plurals.notif_ticker, expiringItems.size(), expiringItems.size());
            notificationTitle = context.getString(R.string.notif_title);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_REMINDER)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText).setTicker(notificationText);

        // Display list of items in notification
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (int i = 0; i < 5 && i < expiringItems.size(); i++) {
            style.addLine(expiringItems.get(i).getTitle());
        }
        if (expiringItems.size() > 5) {
            style.setSummaryText(
                    context.getString(R.string.notif_plus_more, expiringItems.size() - 5));
        }
        style.setBigContentTitle(notificationText);


        builder.setStyle(style).setSmallIcon(R.drawable.ic_stat_notification)
               .setWhen(alarm.deadline.toDateTimeAtStartOfDay().getMillis())
               .setNumber(expiringItems.size())
               .setColor(context.getResources().getColor(R.color.primary_red)).setSound(null)
               .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        // Intent for when notification is deleted
        Intent deleteIntent = new Intent(context, ReminderBroadcastReceiver.class);
        deleteIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_DELETED);
        deleteIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent deletePendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            deletePendingIntent = PendingIntent
                    .getBroadcast(context, (int) alarm.id, deleteIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            deletePendingIntent = PendingIntent
                    .getBroadcast(context, (int) alarm.id, deleteIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.setDeleteIntent(deletePendingIntent);

        // Intent for snooze button
        Intent snoozeIntent = new Intent(context, SnoozeDatePickerActivity.class);
        snoozeIntent.putExtra(EXTRA_ALARM_ID, alarm.id);
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent snoozePendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            snoozePendingIntent = PendingIntent
                    .getActivity(context, (int) alarm.id, snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            snoozePendingIntent = PendingIntent
                    .getActivity(context, (int) alarm.id, snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.addAction(R.drawable.ic_action_alarms, context.getString(R.string.notif_snooze),
                snoozePendingIntent);

        // Intent for "don't remind again" button
        Intent notAgainIntent = new Intent(context, ReminderBroadcastReceiver.class);
        notAgainIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_DONT_REMIND_AGAIN);
        notAgainIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent notAgainPendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            notAgainPendingIntent = PendingIntent
                    .getBroadcast(context, (int) alarm.id, notAgainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            notAgainPendingIntent = PendingIntent
                    .getBroadcast(context, (int) alarm.id, notAgainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.addAction(R.drawable.ic_action_delete,
                context.getString(R.string.notif_dont_remind_again), notAgainPendingIntent);

        // Intent for when notification is clicked
        Intent clickIntent = new Intent(context, ((OpacClient) context.getApplicationContext()).getMainActivity());
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        clickIntent.putExtra(EXTRA_ALARM_ID, alarm.id);

        PendingIntent clickPendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            clickPendingIntent = PendingIntent
                    .getActivity(context, (int) alarm.id, clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            clickPendingIntent = PendingIntent
                    .getActivity(context, (int) alarm.id, clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.setContentIntent(clickPendingIntent);


        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) alarm.id, builder.build());

        alarm.notified = true;
        adata.updateAlarm(alarm);
    }

    private static long[] listToLongArray(List<Long> list) {
        long[] array = new long[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private void notificationDeleted() {
        if (prefs.getBoolean("notification_repeat", true)) {
            snooze(adata, alarm, calculateSnoozeDuration(alarm));
        } else {
            finished(adata, alarm );
        }
    }

    public static void finished(AccountDataSource adata, Alarm alarm) {
        if (BuildConfig.DEBUG) Log.i(LOG_TAG, "alarm finished");
        alarm.finished = true;
        adata.updateAlarm(alarm);
    }

    public static ReadablePeriod calculateSnoozeDuration(Alarm alarm) {
        // we assume that the average library closes at 6 PM on the day the item expires
        Duration timeLeft = new Duration(alarm.deadline.toDateTime(new LocalTime(18, 0)),
                DateTime.now());

        if (timeLeft.isLongerThan(Days.ONE.toStandardDuration())) {
            return Hours.hours(12);
        } else if (timeLeft.isShorterThan(new Duration(0))) {
            // Don't annoy the user every 2 hours when it's already too late
            return Hours.hours(12);
        } else {
            return Hours.hours(2);
        }
    }

    public static void snooze(AccountDataSource adata, Alarm alarm, ReadablePeriod duration) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "snoozing notification for " + PeriodFormat.wordBased().print(duration));
        }
        alarm.notified = false;
        alarm.notificationTime = DateTime.now().plus(duration);
        adata.updateAlarm(alarm);
    }

    private void notificationDontRemindAgain() {
        finished(adata, alarm);
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel((int) alarm.id);
    }
}
