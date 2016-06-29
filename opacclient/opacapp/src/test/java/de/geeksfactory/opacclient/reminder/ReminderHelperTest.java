package de.geeksfactory.opacclient.reminder;

import android.content.SharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
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

    @Before
    public void setUp() throws Exception {
        app = mock(OpacClient.class);
        sp = mock(SharedPreferences.class);
        data = mock(AccountDataSource.class);
        rh = new ReminderHelper(app, sp, data);

        when(sp.getString(eq("notification_warning"), anyString())).thenReturn("3");
    }

    @Test
    public void shouldOnlyResetNotifiedOnReminderOff() throws Exception {
        when(sp.getBoolean(eq("notification_service"), anyBoolean())).thenReturn(false);

        rh.generateAlarms();
        verify(data, times(1)).resetNotifiedOnAllAlarams();
        verifyNoMoreInteractions(data);
    }

    @Test
    public void shouldNotCreateAlarmsWithoutLentItems() throws Exception {
        when(sp.getBoolean(eq("notification_service"), anyBoolean())).thenReturn(true);
        when(data.getAllLentItems()).thenReturn(new ArrayList<LentItem>());

        rh.generateAlarms();
        verify(data, never())
                .addAlarm(any(LocalDate.class), any(long[].class), any(DateTime.class));
    }

    @Test
    public void shouldCreateAlarmForLentItem() throws Exception {
        when(sp.getBoolean(eq("notification_service"), anyBoolean())).thenReturn(true);
        ArrayList<LentItem> lentItems = new ArrayList<>();

        LentItem item = new LentItem();
        LocalDate date = LocalDate.now();
        long dbId = 1L;

        item.setDeadline(date);
        item.setDbId(dbId);

        lentItems.add(item);
        when(data.getAllLentItems()).thenReturn(lentItems);

        rh.generateAlarms();
        verify(data, times(1)).addAlarm(date, new long[]{dbId},
                date.toDateTimeAtStartOfDay().minus(Days.days(3)));
    }

    @After
    public void tearDown() throws Exception {

    }
}