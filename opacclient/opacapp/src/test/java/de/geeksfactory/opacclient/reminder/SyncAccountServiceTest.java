package de.geeksfactory.opacclient.reminder;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SyncAccountServiceTest {
    private SyncAccountService service;

    // mocks
    private OpacClient app;
    private SharedPreferences sp;
    private AccountDataSource data;
    private ReminderHelper helper;
    private OpacApi api;

    // example data
    private List<Account> accounts;
    private Library library;

    @Before
    public void setUp() throws Exception {
        app = mock(OpacClient.class);
        sp = mock(SharedPreferences.class);
        data = mock(AccountDataSource.class);
        helper = mock(ReminderHelper.class);
        api = mock(OpacApi.class);
        service = new SyncAccountService();

        when(sp.contains("update_151_clear_cache")).thenReturn(true);

        accounts = new ArrayList<>();
        Account account1 = new Account();
        accounts.add(account1);
        library = new Library();
    }

    @Test
    public void shouldDoNothingWithoutAccounts() {
        when(data.getAccountsWithPassword()).thenReturn(new ArrayList<Account>());
        service.syncAccounts(app, data, sp, helper);
        verify(data).getAccountsWithPassword();
        verifyNoMoreInteractions(data);
    }

}
