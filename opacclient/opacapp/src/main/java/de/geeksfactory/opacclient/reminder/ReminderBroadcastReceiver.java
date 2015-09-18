package de.geeksfactory.opacclient.reminder;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadablePeriod;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.MainActivity;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALARM_ID = "alarmId";
    public static final String ACTION_SHOW_NOTIFICATION = "show";
    public static final String ACTION_NOTIFICATION_DELETED = "deleted";
    public static final String ACTION_NOTIFICATION_SNOOZE = "snooze";
    public static final String ACTION_NOTIFICATION_CLICK = "click";
    public static final String ACTION_NOTIFICATION_DONT_REMIND_AGAIN = "dontremindagain";

    private AccountDataSource adata;
    private Alarm alarm;
    private Context context;
    private Intent intent;
    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        adata = new AccountDataSource(context);
        adata.open();
        alarm = adata.getAlarm(intent.getLongExtra(EXTRA_ALARM_ID, -1));

        switch (intent.getAction()) {
            case ACTION_SHOW_NOTIFICATION:
                showNotification();
                break;
            case ACTION_NOTIFICATION_DELETED:
                notificationDeleted();
                break;
            case ACTION_NOTIFICATION_SNOOZE:
                notificationSnooze();
                break;
            case ACTION_NOTIFICATION_CLICK:
                notificationClick();
                break;
            case ACTION_NOTIFICATION_DONT_REMIND_AGAIN:
                notificationDontRemindAgain();
                break;
        }
        adata.close();
    }

    private void showNotification() {
        alarm.notified = true;
        adata.updateAlarm(alarm);

        List<LentItem> expiringItems = new ArrayList<>();
        for (long mediaId : alarm.media) {
            expiringItems.add(adata.getLentItem(mediaId));
        }

        String notificationText;
        // You can still return the item on the day it expires on, so don't consider it to have
        // expired before the next day
        if (alarm.deadline.isBefore(LocalDate.now())) {
            notificationText = context
                    .getString(R.string.notif_ticker_expired, expiringItems.size());
        } else {
            notificationText = context.getString(R.string.notif_ticker, expiringItems.size());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(notificationText)
                .setTicker(context.getString(R.string.notif_title));

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
                .setColor(context.getResources().getColor(R.color.primary_red)).setSound(null);

        // Intent for when notification is deleted
        Intent deleteIntent = new Intent(context, ReminderBroadcastReceiver.class);
        deleteIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_DELETED);
        deleteIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent deletePendingIntent = PendingIntent
                .getBroadcast(context, (int) alarm.id, deleteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(deletePendingIntent);

        // Intent for snooze button
        Intent snoozeIntent = new Intent(context, ReminderBroadcastReceiver.class);
        snoozeIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_DELETED);
        snoozeIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent snoozePendingIntent = PendingIntent
                .getBroadcast(context, (int) alarm.id, snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_action_alarms, context.getString(R.string.notif_snooze),
                snoozePendingIntent);

        // Intent for "don't remind again" button
        Intent notAgainIntent = new Intent(context, ReminderBroadcastReceiver.class);
        notAgainIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_DELETED);
        notAgainIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent notAgainPendingIntent = PendingIntent
                .getBroadcast(context, (int) alarm.id, notAgainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_action_delete,
                context.getString(R.string.notif_dont_remind_again), notAgainPendingIntent);

        // Intent for when notification is clicked
        Intent clickIntent = new Intent(context, ReminderBroadcastReceiver.class);
        clickIntent.setAction(ReminderBroadcastReceiver.ACTION_NOTIFICATION_CLICK);
        clickIntent.putExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, alarm.id);
        PendingIntent clickPendingIntent = PendingIntent
                .getBroadcast(context, (int) alarm.id, clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(clickPendingIntent);


        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) alarm.id, builder.build());
    }

    private void notificationDeleted() {
        if (prefs.getBoolean("notification_repeat", true)) {
            snooze(calculateSnoozeDuration());
        } else {
            finished();
        }
    }

    private void finished() {
        alarm.finished = true;
        adata.updateAlarm(alarm);
    }

    private ReadablePeriod calculateSnoozeDuration() {
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

    private void notificationSnooze() {
        // TODO: implement
    }

    private void snooze(ReadablePeriod duration) {
        alarm.notified = false;
        alarm.notificationTime.plus(duration);
        adata.updateAlarm(alarm);
    }

    private void notificationClick() {
        if (prefs.getBoolean("notification_repeat", true)) {
            snooze(calculateSnoozeDuration());
        } else {
            finished();
        }
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    private void notificationDontRemindAgain() {
        finished();
    }
}
