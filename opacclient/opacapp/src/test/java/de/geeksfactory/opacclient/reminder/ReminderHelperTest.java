package de.geeksfactory.opacclient.reminder;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReminderHelperTest {
    private ReminderHelper rh;
    private OpacClient app;
    private SharedPreferences sp;
    private AccountDataSource data;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;

    @Before
    public void setUp() throws Exception {
        app = mock(OpacClient.class);
        sp = mock(SharedPreferences.class);
        data = mock(AccountDataSource.class);
        rh = new ReminderHelper(app, sp, data);

        notificationManager = mock(NotificationManager.class);
        when(app.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager);

        alarmManager = mock(AlarmManager.class);
        when(app.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager);

        when(sp.getString(eq("notification_warning"), anyString())).thenReturn("3");
    }

    @Test
    public void shouldOnlyResetNotifiedOnReminderOff() throws Exception {
        setNotificationsEnabled(false);

        rh.generateAlarms();
        verify(data, times(1)).resetNotifiedOnAllAlarams();
        verifyNoMoreInteractions(data);
    }

    @Test
    public void shouldNotCreateAlarmsWithoutLentItems() throws Exception {
        setNotificationsEnabled(true);
        when(data.getAllLentItems()).thenReturn(new ArrayList<LentItem>());

        rh.generateAlarms();
        verify(data, never())
                .addAlarm(any(LocalDate.class), any(long[].class), any(DateTime.class));
    }

    @Test
    public void shouldCreateAlarmForLentItem() throws Exception {
        setNotificationsEnabled(true);

        LocalDate date = LocalDate.now();
        long dbId = 1L;

        stubLentItem(date, dbId);

        rh.generateAlarms();
        verify(data, times(1)).addAlarm(date, new long[]{dbId},
                date.toDateTimeAtStartOfDay().minus(Days.days(3)));
    }

    @Test
    public void shouldRemoveAlarmForRemovedItem() throws Exception {
        setNotificationsEnabled(true);
        when(data.getAllLentItems()).thenReturn(new ArrayList<LentItem>());

        Alarm alarm = stubAlarm();

        rh.generateAlarms();
        verify(data, times(1)).removeAlarm(alarm);
    }

    @Test
    public void shouldUpdateAlarmForChangedItemIds() throws Exception {
        setNotificationsEnabled(true);

        stubLentItem(LocalDate.now(), 2L);
        stubAlarm();

        rh.generateAlarms();

        verify(data, times(1)).updateAlarm(argThat(new ArgumentMatcher<Alarm>() {

            @Override
            public boolean matches(Object argument) {
                Alarm alarm = (Alarm) argument;
                return alarm.media.length == 1 && alarm.media[0] == 2L;
            }
        }));
    }

    @SuppressLint("CommitPrefEdits")
    @Test
    public void shouldUpdateWarningPeriodFromMillisToDays() {
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(sp.getString(eq("notification_warning"), anyString())).thenReturn("259200000");
        when(data.getAllLentItems()).thenReturn(new ArrayList<LentItem>());
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(sp.edit()).thenReturn(editor);

        rh.generateAlarms();

        verify(editor).putString("notification_warning", "3");
        verify(editor).apply();
        verifyNoMoreInteractions(editor);
    }

    private void setNotificationsEnabled(boolean on) {
        when(sp.getBoolean(eq("notification_service"), anyBoolean())).thenReturn(on);
    }

    private LentItem stubLentItem(LocalDate date, long dbId) {
        ArrayList<LentItem> lentItems = new ArrayList<>();
        LentItem item = new LentItem();

        item.setDeadline(date);
        item.setDbId(dbId);

        lentItems.add(item);
        when(data.getAllLentItems()).thenReturn(lentItems);
        return item;
    }

    @NonNull
    private Alarm stubAlarm() {
        ArrayList<Alarm> alarms = new ArrayList<>();
        Alarm alarm = new Alarm();
        alarm.deadline = LocalDate.now();
        alarm.finished = false;
        alarm.notified = false;
        alarm.id = 1L;
        alarm.media = new long[]{1L};
        alarm.notificationTime = DateTime.now();
        alarms.add(alarm);

        when(data.getAllAlarms()).thenReturn(alarms);
        when(data.getAlarmByDeadline(alarm.deadline)).thenReturn(alarm);
        return alarm;
    }
}