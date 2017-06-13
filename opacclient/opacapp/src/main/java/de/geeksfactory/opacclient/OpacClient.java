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
package de.geeksfactory.opacclient;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.annotation.ReportsCrashes;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.AccountListActivity;
import de.geeksfactory.opacclient.frontend.LibraryListActivity;
import de.geeksfactory.opacclient.frontend.MainActivity;
import de.geeksfactory.opacclient.frontend.MainPreferenceActivity;
import de.geeksfactory.opacclient.frontend.SearchResultListActivity;
import de.geeksfactory.opacclient.i18n.AndroidStringProvider;
import de.geeksfactory.opacclient.networking.AndroidHttpClientFactory;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.reminder.SyncAccountAlarmListener;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.PreferenceDataSource;
import de.geeksfactory.opacclient.storage.StarContentProvider;
import de.geeksfactory.opacclient.utils.DebugTools;
import de.geeksfactory.opacclient.utils.ErrorReporter;
import de.geeksfactory.opacclient.utils.Utils;
import de.geeksfactory.opacclient.webservice.LibraryConfigUpdateService;
import de.geeksfactory.opacclient.webservice.UpdateHandler;
import de.geeksfactory.opacclient.webservice.WebserviceReportHandler;

@ReportsCrashes(mailTo = "info@opacapp.de",
        mode = org.acra.ReportingInteractionMode.NOTIFICATION,
        sendReportsInDevMode = false,
        resToastText = R.string.crash_toast_text)
public class OpacClient extends Application {

    public static final String PREF_SELECTED_ACCOUNT = "selectedAccount";
    public static final String PREF_HOME_BRANCH_PREFIX = "homeBranch_";
    public static final String ASSETS_BIBSDIR = "bibs";
    public static int NOTIF_ID = 1;
    public static int BROADCAST_REMINDER = 2;
    public static Context context;
    public static String versionName = "unknown";
    private static OpacClient instance;
    public final boolean SLIDING_MENU = true;
    private final Uri STAR_PROVIDER_STAR_URI = StarContentProvider.STAR_URI;
    protected Account account;
    protected OpacApi api;
    protected Library library;
    protected String currentLang;
    private SharedPreferences sp;

    public OpacClient() {
        super();
        instance = this;
    }

    public static Context getEmergencyContext() {
        return instance.getApplicationContext();
    }

    public static Bundle queryToBundle(List<SearchQuery> query) {
        if (query == null) {
            return null;
        }
        Bundle b = new Bundle();
        for (SearchQuery q : query) {
            try {
                b.putString(q.getSearchField().toJSON().toString(),
                        q.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return b;
    }

    // public Class getSearchResultsActivityClass() {
    // return SearchResultsActivity.class;
    // }

    public static List<SearchQuery> bundleToQuery(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<SearchQuery> query = new ArrayList<>();
        for (String e : bundle.keySet()) {
            try {
                query.add(new SearchQuery(SearchField
                        .fromJSON(new JSONObject(e)), bundle.getString(e)));
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return query;
    }

    public static Bundle mapToBundle(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Bundle b = new Bundle();
        for (Entry<String, String> e : map.entrySet()) {
            b.putString(e.getKey(), e.getValue());
        }
        return b;
    }

    public static Map<String, String> bundleToMap(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (String e : bundle.keySet()) {
            map.put(e, (String) bundle.get(e));
        }
        return map;
    }

    public Uri getStarProviderStarUri() {
        return STAR_PROVIDER_STAR_URI;
    }

    public void addFirstAccount(Activity activity) {
        Intent intent = new Intent(activity, LibraryListActivity.class);
        intent.putExtra(LibraryListActivity.EXTRA_WELCOME, true);
        activity.startActivity(intent);
        activity.finish();
    }

    public void startSearch(Activity caller, List<SearchQuery> query) {
        startSearch(caller, query, null);
    }

    public void startSearch(Activity caller, List<SearchQuery> query, Bundle bundle) {
        Intent myIntent = new Intent(caller, SearchResultListActivity.class);
        myIntent.putExtra("query", queryToBundle(query));
        ActivityCompat.startActivity(caller, myIntent, bundle);
    }

    public void startVolumeSearch(Activity caller, Map<String, String> query) {
        Intent myIntent = new Intent(caller, SearchResultListActivity.class);
        myIntent.putExtra("volumeQuery", mapToBundle(query));
        caller.startActivity(myIntent);
    }

    public boolean isOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public OpacApi getNewApi(Library lib) throws LibraryRemovedException {
        if (!lib.isActive()) throw new LibraryRemovedException();
        currentLang = getResources().getConfiguration().locale.getLanguage();
        return OpacApiFactory
                .create(lib, new AndroidStringProvider(), new AndroidHttpClientFactory(),
                        currentLang, new WebserviceReportHandler());
    }

    private OpacApi initApi(Library lib) throws LibraryRemovedException {
        api = getNewApi(lib);
        return api;
    }

    public void resetCache() {
        account = null;
        api = null;
        library = null;
    }

    public OpacApi getApi() throws LibraryRemovedException {
        if (account != null && api != null) {
            if (sp.getLong(PREF_SELECTED_ACCOUNT, 0) == account.getId()
                    && getResources().getConfiguration().locale.getLanguage()
                                                               .equals(currentLang)) {
                return api;
            }
        }
        api = initApi(getLibrary());
        return api;
    }

    public Account getAccount() {
        if (account != null) {
            if (sp.getLong(PREF_SELECTED_ACCOUNT, 0) == account.getId()) {
                return account;
            }
        }
        AccountDataSource data = new AccountDataSource(this);
        account = data.getAccount(sp.getLong(PREF_SELECTED_ACCOUNT, 0));
        return account;
    }

    public void setAccount(long id) {
        sp.edit().putLong(OpacClient.PREF_SELECTED_ACCOUNT, id).commit();
        resetCache();
        if (getLibrary() != null && !BuildConfig.DEBUG) {
            ACRA.getErrorReporter().putCustomData("library",
                    getLibrary().getIdent());
        }
    }

    public Library getLibrary(String ident) throws IOException, JSONException {
        String filename = ident + ".json";
        String json;

        File filesDir = getLibrariesDir();
        PreferenceDataSource prefs = getPreferenceDataSource();

        if (fileExists(filesDir, filename) && (
                !prefs.hasBundledConfiguration() ||
                        prefs.getLastLibraryConfigUpdateVersion() == BuildConfig.VERSION_CODE)) {
            // only use files if they were downloaded using the current app version
            json = Utils.readStreamToString(openFile(filesDir, filename));
        } else {
            json = Utils.readStreamToString(getAssets().open(ASSETS_BIBSDIR + "/" + filename));
        }
        return Library.fromJSON(ident, new JSONObject(json));
    }

    @NonNull
    PreferenceDataSource getPreferenceDataSource() {
        return new PreferenceDataSource(this);
    }

    InputStream openFile(File filesDir, String filename) throws FileNotFoundException {
        File file = new File(filesDir, filename);
        return new FileInputStream(file);
    }

    boolean fileExists(File filesDir, String filename) {
        File file = new File(filesDir, filename);
        return file.exists();
    }

    @NonNull
    protected File getFile(File filesDir, String filename) {
        return new File(filesDir, filename);
    }

    @NonNull
    protected File getLibrariesDir() {
        File filesDir = new File(getFilesDir(), LibraryConfigUpdateService.LIBRARIES_DIR);
        filesDir.mkdirs();
        return filesDir;
    }

    public Library getLibrary() {
        if (getAccount() == null) {
            return null;
        }
        if (account != null && library != null) {
            if (sp.getLong(PREF_SELECTED_ACCOUNT, 0) == account.getId()) {
                return library;
            }
        }
        try {
            library = getLibrary(getAccount().getLibrary());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            ErrorReporter.handleException(e);
        }
        return library;
    }

    @SuppressWarnings("UnusedDeclaration") // ProgressCallback should not be required
    public List<Library> getLibraries() throws IOException {
        return getLibraries(null);
    }

    public List<Library> getLibraries(ProgressCallback callback)
            throws IOException {
        AssetManager assets = getAssets();
        String[] files = assets.list(ASSETS_BIBSDIR);
        String[] additionalFiles = getLibrariesDir().list();
        Set<String> allFiles = new HashSet<>();
        Collections.addAll(allFiles, files);
        Collections.addAll(allFiles, additionalFiles);
        int num = allFiles.size();

        List<Library> libs = new ArrayList<>();

        int i = 0;
        for (String file : allFiles) {
            try {
                Library lib =
                        getLibrary(file.substring(0, file.length() - ".json".length()));
                if ((!lib.getApi().equals("test") || BuildConfig.DEBUG) && lib.isActive()) {
                    libs.add(lib);
                }
            } catch (JSONException e) {
                Log.w("JSON library files", "Failed parsing library " + file);
                e.printStackTrace();
            }
            if (callback != null && i % 10 == 0 && i > 0) {
                // reporting progress for every 10 loaded files should be enough
                callback.publishProgress(((double) i) / num);
            }
            i++;
        }

        return libs;
    }

    public void toPrefs(Activity activity) {
        Intent intent = new Intent(activity, MainPreferenceActivity.class);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (!BuildConfig.DEBUG) {
            ACRAConfiguration config = ACRA.getNewDefaultConfig(this);
            config.setResToastText(R.string.crash_toast_text);
            config.setResDialogText(R.string.crash_dialog_text);
            config.setResToastText(R.string.crash_toast_text);
            config.setResNotifTickerText(R.string.crash_notif_ticker_text);
            config.setResNotifTitle(R.string.crash_notif_title);
            config.setResNotifText(R.string.crash_notif_text);
            config.setResNotifIcon(android.R.drawable.stat_notify_error);
            config.setResDialogText(R.string.crash_dialog_text);
            ACRA.init(this, config);

            if (getLibrary() != null) {
                ACRA.getErrorReporter().putCustomData("library",
                        getLibrary().getIdent());
            }
            DateTime lastUpdate = new PreferenceDataSource(getApplicationContext())
                    .getLastLibraryConfigUpdate();
            ACRA.getErrorReporter().putCustomData("data_version",
                    lastUpdate != null ? lastUpdate.toString() : "null");
        }
        DebugTools.init(this);

        OpacClient.context = getApplicationContext();

        try {
            OpacClient.versionName = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Schedule alarms
        WakefulIntentService.scheduleAlarms(new SyncAccountAlarmListener(), this);
    }

    public boolean getSlidingMenuEnabled() {
        return SLIDING_MENU;
    }

    public Class<?> getMainActivity() {
        return MainActivity.class;
    }

    public void openAccountList(Activity ctx) {
        Intent intent = new Intent(ctx, AccountListActivity.class);
        ctx.startActivity(intent);
    }

    public interface ProgressCallback {
        public void publishProgress(double progress);
    }

    public static class LibraryRemovedException extends Exception {
    }

    public UpdateHandler getUpdateHandler() {
        return new UpdateHandler();
    }

    public boolean promotePlusApps() {
        return true;
    }
}
