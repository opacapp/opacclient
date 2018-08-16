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

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import io.sentry.Sentry;

public class SyncAccountJob extends Job {

    static final String TAG = "SyncAccountJob";
    static final String TAG_RETRY = "SyncAccountJob_retry";
    static final String TAG_IMMEDIATE = "SyncAccountJob_immediate";

    public static void scheduleJob(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.HOURS.toMillis(12), TimeUnit.HOURS.toMillis(3))
                .setRequiredNetworkType(sp.getBoolean("notification_service_wifionly", false) ?
                        JobRequest.NetworkType.UNMETERED : JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void scheduleRetryJob(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        new JobRequest.Builder(TAG_RETRY)
                .setExecutionWindow(TimeUnit.MINUTES.toMillis(30), TimeUnit.MINUTES.toMillis(60))
                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(30),
                        JobRequest.BackoffPolicy.EXPONENTIAL)
                .setRequiredNetworkType(sp.getBoolean("notification_service_wifionly", false) ?
                        JobRequest.NetworkType.UNMETERED : JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void runImmediately() {
        new JobRequest.Builder(TAG_IMMEDIATE)
                .startNow()
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        if (BuildConfig.DEBUG) Log.i(TAG, "SyncAccountJob started");

        if (getParams().getTag().equals(TAG_RETRY) && getParams().getFailureCount() >= 4) {
            // too many retries, give up and wait for the next regular scheduled run
            return Result.SUCCESS;
        }

        if (!getParams().getTag().equals(TAG_RETRY)) {
            updateLibraryConfig();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (!sp.getBoolean(SyncAccountJobCreator.PREF_SYNC_SERVICE, false)) {
            if (BuildConfig.DEBUG) Log.i(TAG, "notifications are disabled");
            return Result.SUCCESS;
        }

        OpacClient app = getApp();
        AccountDataSource data = new AccountDataSource(getContext());
        ReminderHelper helper = new ReminderHelper(app);
        boolean failed = syncAccounts(app, data, sp, helper);

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "SyncAccountJob finished " +
                    (failed ? " with errors" : " " + "successfully"));
        }

        if (failed && params.getTag().equals(TAG)) {
            // only schedule a retry job if this is not already a retry
            scheduleRetryJob(getContext());
        }
        if (params.getTag().equals(TAG_RETRY)) {
            return failed ? Result.RESCHEDULE : Result.SUCCESS;
        } else {
            return failed ? Result.FAILURE : Result.SUCCESS;
        }
    }

    private OpacClient getApp() {
        Context ctx = getContext();
        if (ctx instanceof Service) {
            return (OpacClient) ((Service) ctx).getApplication();
        } else if (ctx instanceof OpacClient) {
            return (OpacClient) ctx;
        } else {
            return (OpacClient) ctx.getApplicationContext();
        }
    }

    private void updateLibraryConfig() {
        PreferenceDataSource prefs = new PreferenceDataSource(getContext());
        if (prefs.getLastLibraryConfigUpdate() != null
                && prefs.getLastLibraryConfigUpdate()
                        .isAfter(DateTime.now().minus(Hours.ONE))) {
            Log.d(TAG, "Do not run updateLibraryConfig as last run was less than an hour ago.");
            return;
        }

        WebService service = WebServiceManager.getInstance();
        File filesDir =
                new File(getContext().getFilesDir(), LibraryConfigUpdateService.LIBRARIES_DIR);
        filesDir.mkdirs();
        try {
            int count = getApp().getUpdateHandler().updateConfig(
                    service, prefs,
                    new LibraryConfigUpdateService.FileOutput(filesDir),
                    new JsonSearchFieldDataSource(getContext()));
            Log.d(TAG, "updated config for " + String.valueOf(count) + " libraries");
            getApp().resetCache();
            if (!BuildConfig.DEBUG) {
                Sentry.getContext().addExtra(OpacClient.SENTRY_DATA_VERSION,
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
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Loading data for Account " + account.toString());
            }

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
