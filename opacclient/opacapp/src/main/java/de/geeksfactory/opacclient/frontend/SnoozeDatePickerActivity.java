package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.joda.time.DateTime;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.Alarm;
import de.geeksfactory.opacclient.reminder.ReminderBroadcastReceiver;
import de.geeksfactory.opacclient.reminder.ReminderHelper;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.DataIntegrityException;

public class SnoozeDatePickerActivity extends Activity
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private DateTime dt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // close notification drawer
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(it);

        dt = DateTime.now();
        new DatePickerDialog(this, R.style.Theme_AppCompat_Light_Dialog, this, dt.getYear(),
                dt.getMonthOfYear() - 1, // Joda time has 1-based months
                dt.getDayOfMonth()).show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        dt = dt.withYear(year).withMonthOfYear(monthOfYear + 1) // Joda time has 1-based months
                .withDayOfMonth(dayOfMonth);

        new TimePickerDialog(this, R.style.Theme_AppCompat_Light_Dialog, this, dt.getHourOfDay(),
                dt.getMinuteOfHour(), DateFormat.is24HourFormat(this)).show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        dt = dt.withHourOfDay(hourOfDay).withMinuteOfHour(minute);

        long alarmId = getIntent().getLongExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, -1);
        AccountDataSource adata = new AccountDataSource(this);
        Alarm alarm = adata.getAlarm(alarmId);
        if (alarm == null) {
            throw new DataIntegrityException("Trying to snooze unknown alarm ID " + alarmId);
        }

        alarm.notified = false;
        alarm.notificationTime = dt;
        adata.updateAlarm(alarm);

        // dismiss notification
        NotificationManager manager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.cancel((int) alarm.id);

        // reschedule alarms
        new ReminderHelper((OpacClient) getApplication()).scheduleAlarms();

        finish();
    }
}
