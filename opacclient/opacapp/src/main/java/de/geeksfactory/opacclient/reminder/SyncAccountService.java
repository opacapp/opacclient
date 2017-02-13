/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.reminder;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.acra.ACRA;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.PreferenceDataSource;
import de.geeksfactory.opacclient.webservice.LibraryConfigUpdateService;
import de.geeksfactory.opacclient.webservice.WebService;
import de.geeksfactory.opacclient.webservice.WebServiceManager;

public class SyncAccountService extends WakefulIntentService {

    private static final String NAME = "SyncAccountService";

    public SyncAccountService() {
        super(NAME);
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        if (BuildConfig.DEBUG) Log.i(NAME, "SyncAccountService started");

        updateLibraryConfig();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (!sp.getBoolean(SyncAccountAlarmListener.PREF_SYNC_SERVICE, false)) {
            if (BuildConfig.DEBUG) Log.i(NAME, "notifications are disabled");
            return;
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean failed;
        if (networkInfo != null) {
            if (!sp.getBoolean("notification_service_wifionly", false) ||
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                    networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                OpacClient app = (OpacClient) getApplication();
                AccountDataSource data = new AccountDataSource(this);
                ReminderHelper helper = new ReminderHelper(app);
                failed = syncAccounts(app, data, sp, helper);
            } else {
                failed = true;
            }
        } else {
            failed = true;
        }

        if (BuildConfig.DEBUG) {
            Log.i(NAME, "SyncAccountService finished " +
                    (failed ? " with errors" : " " + "successfully"));
        }

        long previousPeriod = sp.getLong(SyncAccountAlarmListener.PREF_SYNC_INTERVAL, 0);
        long newPeriod = failed ? AlarmManager.INTERVAL_HOUR : AlarmManager.INTERVAL_HALF_DAY;
        if (previousPeriod != newPeriod) {
            sp.edit().putLong(SyncAccountAlarmListener.PREF_SYNC_INTERVAL, newPeriod).apply();
            WakefulIntentService.cancelAlarms(this);
            WakefulIntentService
                    .scheduleAlarms(SyncAccountAlarmListener.withOnePeriodBeforeStart(), this);
        }
    }

    private void updateLibraryConfig() {
        PreferenceDataSource prefs = new PreferenceDataSource(this);
        if (prefs.getLastLibraryConfigUpdate() != null
                && prefs.getLastLibraryConfigUpdate()
                        .isAfter(DateTime.now().minus(Hours.ONE))) {
            Log.d(NAME, "Do not run updateLibraryConfig as last run was less than an hour ago.");
            return;
        }

        WebService service = WebServiceManager.getInstance();
        File filesDir = new File(getFilesDir(), LibraryConfigUpdateService.LIBRARIES_DIR);
        filesDir.mkdirs();
        try {
            int count = ((OpacClient) getApplication()).getUpdateHandler().updateConfig(
                    service, prefs,
                    new LibraryConfigUpdateService.FileOutput(filesDir),
                    new JsonSearchFieldDataSource(this));
            Log.d(NAME, "updated config for " + String.valueOf(count) + " libraries");
            ((OpacClient) getApplication()).resetCache();
            if (!BuildConfig.DEBUG) {
                ACRA.getErrorReporter().putCustomData("data_version",
                        prefs.getLastLibraryConfigUpdate().toString());
            }
        } catch (IOException | JSONException ignore) {

        }
    }

    boolean syncAccounts(OpacClient app, AccountDataSource data, SharedPreferences sp,
            ReminderHelper helper) {
        boolean failed = false;
        List<Account> accounts = data.getAccountsWithPassword();

        if (!sp.contains("update_151_clear_cache")) {
            data.invalidateCachedData();
            sp.edit().putBoolean("update_151_clear_cache", true).apply();
       }

        for (Account account : accounts) {
            if (BuildConfig.DEBUG)
                Log.i(NAME, "Loading data for Account " + account.toString());

            AccountData res;
            try {
                Library library = app.getLibrary(account.getLibrary());
                if (!library.isAccountSupported()) {
                    data.deleteAccountData(account);
                    continue;
                }
                OpacApi api = app.getNewApi(library);
                res = api.account(account);
                if (res == null) {
                    failed = true;
                    continue;
                }
            } catch (JSONException | IOException | OpacApi.OpacErrorException e) {
                e.printStackTrace();
                failed = true;
                continue;
            } catch (OpacClient.LibraryRemovedException e) {
                continue;
            }

            account.setPasswordKnownValid(true);
            try {
                data.update(account);
                data.storeCachedAccountData(account, res);
            } finally {
                helper.generateAlarms();
            }
        }
        return failed;
    }

}
