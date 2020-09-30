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
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
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
import io.sentry.core.Sentry;

public class SyncAccountJob extends Worker {

    static final String TAG = "SyncAccountJob";
    static final String TAG_RETRY = "SyncAccountJob_retry";
    static final String TAG_IMMEDIATE = "SyncAccountJob_immediate";

    public static final String PREF_SYNC_SERVICE = "notification_service";

    public SyncAccountJob(@NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void scheduleJob(Context ctx) {
        WorkManager wm = WorkManager.getInstance(ctx);
        wm.cancelAllWorkByTag(TAG);

        WorkRequest wr =
                new PeriodicWorkRequest.Builder(SyncAccountJob.class, 12, TimeUnit.HOURS)
                        .addTag(TAG)
                        .setConstraints(getConstraints(ctx))
                        .build();
        wm.enqueue(wr);
    }

    private static Constraints getConstraints(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return new Constraints.Builder()
                .setRequiredNetworkType(
                        sp.getBoolean("notification_service_wifionly", false) ?
                                NetworkType.UNMETERED : NetworkType.CONNECTED)
                .build();
    }

    public static void scheduleRetryJob(Context ctx) {
        WorkManager wm = WorkManager.getInstance(ctx);
        wm.cancelAllWorkByTag(TAG_RETRY);

        WorkRequest wr = new OneTimeWorkRequest.Builder(SyncAccountJob.class)
                .addTag(TAG_RETRY)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setConstraints(getConstraints(ctx))
                .build();
        wm.enqueue(wr);
    }

    public static void runImmediately(Context ctx) {
        WorkManager wm = WorkManager.getInstance(ctx);
        wm.cancelAllWorkByTag(TAG_IMMEDIATE);

        WorkRequest wr = new OneTimeWorkRequest.Builder(SyncAccountJob.class)
                .addTag(TAG_IMMEDIATE)
                .setInitialDelay(0, TimeUnit.MINUTES)
                .build();
        wm.enqueue(wr);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (BuildConfig.DEBUG) Log.i(TAG, "SyncAccountJob started");

        if (getTags().contains(TAG_RETRY) && getRunAttemptCount() >= 4) {
            // too many retries, give up and wait for the next regular scheduled run
            return Result.success();
        }

        if (!getTags().contains(TAG_RETRY)) {
            updateLibraryConfig();
        }

        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (!sp.getBoolean(PREF_SYNC_SERVICE, false)) {
            if (BuildConfig.DEBUG) Log.i(TAG, "notifications are disabled");
            return Result.success();
        }

        OpacClient app = getApp();
        AccountDataSource data = new AccountDataSource(getApplicationContext());
        ReminderHelper helper = new ReminderHelper(app);
        boolean failed = syncAccounts(app, data, sp, helper);

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "SyncAccountJob finished " +
                    (failed ? " with errors" : " " + "successfully"));
        }

        if (failed && getTags().contains(TAG)) {
            // only schedule a retry job if this is not already a retry
            scheduleRetryJob(getApplicationContext());
        }
        if (getTags().contains(TAG_RETRY)) {
            return failed ? Result.retry() : Result.success();
        } else {
            return failed ? Result.failure() : Result.success();
        }
    }

    private OpacClient getApp() {
        Context ctx = getApplicationContext();
        if (ctx instanceof Service) {
            return (OpacClient) ((Service) ctx).getApplication();
        } else if (ctx instanceof OpacClient) {
            return (OpacClient) ctx;
        } else {
            return (OpacClient) ctx.getApplicationContext();
        }
    }

    private void updateLibraryConfig() {
        PreferenceDataSource prefs = new PreferenceDataSource(getApplicationContext());
        if (prefs.getLastLibraryConfigUpdate() != null
                && prefs.getLastLibraryConfigUpdate()
                        .isAfter(DateTime.now().minus(Hours.ONE))) {
            Log.d(TAG, "Do not run updateLibraryConfig as last run was less than an hour ago.");
            return;
        }

        WebService service = WebServiceManager.getInstance();
        File filesDir =
                new File(getApplicationContext().getFilesDir(),
                        LibraryConfigUpdateService.LIBRARIES_DIR);
        filesDir.mkdirs();
        try {
            int count = getApp().getUpdateHandler().updateConfig(
                    service, prefs,
                    new LibraryConfigUpdateService.FileOutput(filesDir),
                    new JsonSearchFieldDataSource(getApplicationContext()));
            Log.d(TAG, "updated config for " + String.valueOf(count) + " libraries");
            getApp().resetCache();
            if (!BuildConfig.DEBUG) {
                Sentry.setExtra(OpacClient.SENTRY_DATA_VERSION,
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
