package de.geeksfactory.opacclient.reminder;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private OpacApi api1;
    private OpacApi api2;

    // example data
    private List<Account> accounts;
    private Account account1;
    private Account account2;
    private Library library;

    @Before
    public void setUp() throws Exception {
        app = mock(OpacClient.class);
        sp = mock(SharedPreferences.class);
        data = mock(AccountDataSource.class);
        helper = mock(ReminderHelper.class);
        api1 = mock(OpacApi.class);
        api2 = mock(OpacApi.class);
        service = new SyncAccountService();

        when(sp.contains("update_151_clear_cache")).thenReturn(true);

        accounts = new ArrayList<>();
        account1 = new Account();
        account1.setId(0);
        account2 = new Account();
        account2.setId(1);
        accounts.add(account1);
        accounts.add(account2);
        library = new Library();
        library.setAccountSupported(true);
    }

    @Test
    public void shouldDoNothingWithoutAccounts() {
        when(data.getAccountsWithPassword()).thenReturn(new ArrayList<Account>());
        assertFalse(service.syncAccounts(app, data, sp, helper));
        verify(data).getAccountsWithPassword();
        verifyNoMoreInteractions(data);
    }


    @Test
    public void exceptionShouldNotAffectOtherAccounts()
            throws IOException, JSONException, OpacApi.OpacErrorException,
            OpacClient.LibraryRemovedException {
        setUpAccountsAndTwoApis();

        when(api1.account(any(Account.class))).thenThrow(new OpacApi.OpacErrorException("error"));
        when(api2.account(any(Account.class))).thenReturn(mock(AccountData.class));

        verifyErrorShouldNotAffectOtherAccounts();
    }

    @Test
    public void nullShouldNotAffectOtherAccounts()
            throws IOException, JSONException, OpacApi.OpacErrorException,
            OpacClient.LibraryRemovedException {
        setUpAccountsAndTwoApis();

        when(api1.account(any(Account.class))).thenReturn(null);
        when(api2.account(any(Account.class))).thenReturn(mock(AccountData.class));

        verifyErrorShouldNotAffectOtherAccounts();
    }

    private void verifyErrorShouldNotAffectOtherAccounts()
            throws IOException, JSONException, OpacApi.OpacErrorException {
        assertTrue(service.syncAccounts(app, data, sp, helper));

        verify(api1).account(account1);
        verify(api2).account(account2);
        verify(data, never()).storeCachedAccountData(eq(account1), any(AccountData.class));
        verify(data).storeCachedAccountData(eq(account2), any(AccountData.class));
        verify(helper).generateAlarms();
    }

    private void setUpAccountsAndTwoApis()
            throws IOException, JSONException, OpacClient.LibraryRemovedException {
        when(data.getAccountsWithPassword()).thenReturn(accounts);
        when(app.getLibrary(anyString())).thenReturn(library);
        when(app.getNewApi(library)).thenReturn(api1, api2);
    }
}
