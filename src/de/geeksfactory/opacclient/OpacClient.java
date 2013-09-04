package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

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
import de.geeksfactory.opacclient.apis.BiBer1992;
import de.geeksfactory.opacclient.apis.Bibliotheca;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.Pica;
import de.geeksfactory.opacclient.apis.SISIS;
import de.geeksfactory.opacclient.apis.Zones22;
import de.geeksfactory.opacclient.frontend.MainPreferenceActivity;
import de.geeksfactory.opacclient.frontend.NavigationFragment;
import de.geeksfactory.opacclient.frontend.SearchResultsActivity;
import de.geeksfactory.opacclient.frontend.WelcomeActivity;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;
import de.geeksfactory.opacclient.storage.StarContentProvider;

@ReportsCrashes(formKey = "", mailTo = "info@opacapp.de", mode = org.acra.ReportingInteractionMode.DIALOG)
public class OpacClient extends Application {

	public Exception last_exception;

	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;
	public static final String PREF_SELECTED_ACCOUNT = "selectedAccount";
	public static final String PREF_HOME_BRANCH_PREFIX = "homeBranch_";

	public final String LIMIT_TO_LIBRARY = null;

	private SharedPreferences sp;

	private Account account;
	private OpacApi api;
	private Library library;

	public static Context context;
	public static String versionName = "unknown";

	private final Uri STAR_PROVIDER_STAR_URI = StarContentProvider.STAR_URI;

	public Uri getStarProviderStarUri() {
		return STAR_PROVIDER_STAR_URI;
	}

	public void addFirstAccount(Activity activity) {
		Intent intent = new Intent(activity, WelcomeActivity.class);
		activity.startActivity(intent);
		activity.finish();
	}

	public NavigationFragment newNavigationFragment() {
		return new NavigationFragment();
	}

	public void startSearch(Activity caller, Bundle query) {
		Intent myIntent = new Intent(caller, SearchResultsActivity.class);
		myIntent.putExtra("query", query);
		caller.startActivity(myIntent);
	}

	public boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public OpacApi getNewApi(Library lib) throws ClientProtocolException,
			SocketException, IOException, NotReachableException {
		OpacApi newApiInstance = null;
		if (lib.getApi().equals("bond26") || lib.getApi().equals("bibliotheca"))
			// Backwardscompatibility
			newApiInstance = new Bibliotheca();
		else if (lib.getApi().equals("oclc2011")
				|| lib.getApi().equals("sisis"))
			// Backwards compatibility
			newApiInstance = new SISIS();
		else if (lib.getApi().equals("zones22"))
			newApiInstance = new Zones22();
		else if (lib.getApi().equals("biber1992"))
			newApiInstance = new BiBer1992();
		else if (lib.getApi().equals("pica"))
			newApiInstance = new Pica();
		else
			return null;

		newApiInstance.init(new SQLMetaDataSource(this), lib);
		return newApiInstance;
	}

	private OpacApi initApi(Library lib) throws ClientProtocolException,
			SocketException, IOException, NotReachableException {
		api = getNewApi(lib);
		return api;
	}

	public void resetCache() {
		account = null;
		api = null;
		library = null;
	}

	public OpacApi getApi() {
		if (account != null && api != null) {
			if (sp.getLong(PREF_SELECTED_ACCOUNT, 0) == account.getId()) {
				return api;
			}
		}
		try {
			api = initApi(getLibrary());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return api;
	}

	public Account getAccount() {
		if (account != null) {
			if (sp.getLong(PREF_SELECTED_ACCOUNT, 0) == account.getId()) {
				return account;
			}
		}
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		account = data.getAccount(sp.getLong(PREF_SELECTED_ACCOUNT, 0));
		data.close();
		return account;
	}

	public void setAccount(long id) {
		sp.edit().putLong(OpacClient.PREF_SELECTED_ACCOUNT, id).commit();
		resetCache();
		if (getLibrary() != null) {
			ACRA.getErrorReporter().putCustomData("library",
					getLibrary().getIdent());
		}
	}

	public Library getLibrary(String ident) throws IOException, JSONException {
		String line;

		StringBuilder builder = new StringBuilder();
		InputStream fis = getAssets().open(
				ASSETS_BIBSDIR + "/" + ident + ".json");

		BufferedReader reader = new BufferedReader(new InputStreamReader(fis,
				"utf-8"));
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}

		fis.close();
		String json = builder.toString();
		return Library.fromJSON(ident, new JSONObject(json));
	}

	public Library getLibrary() {
		if (getAccount() == null)
			return null;
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
			ACRA.getErrorReporter().handleException(e);
		}
		return library;
	}

	public static final String ASSETS_BIBSDIR = "bibs";

	public List<Library> getLibraries() throws IOException, JSONException {
		AssetManager assets = getAssets();
		String[] files = assets.list(ASSETS_BIBSDIR);
		int num = files.length;

		List<Library> libs = new ArrayList<Library>();

		StringBuilder builder = null;
		BufferedReader reader = null;
		InputStream fis = null;
		String line = null;
		String json = null;

		for (int i = 0; i < num; i++) {
			builder = new StringBuilder();
			fis = assets.open(ASSETS_BIBSDIR + "/" + files[i]);

			reader = new BufferedReader(new InputStreamReader(fis, "utf-8"));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			fis.close();
			json = builder.toString();
			libs.add(Library.fromJSON(files[i].replace(".json", ""),
					new JSONObject(json)));
		}

		return libs;
	}

	public void toPrefs(Activity activity) {
		Intent intent = new Intent(activity,
				MainPreferenceActivity.class);
		activity.startActivity(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		ACRAConfiguration config = ACRA.getNewDefaultConfig(this);
		config.setResToastText(R.string.crash_toast_text);
		config.setResDialogText(R.string.crash_dialog_text);
		ACRA.setConfig(config);
		ACRA.init(this);

		if (getLibrary() != null) {
			ACRA.getErrorReporter().putCustomData("library",
					getLibrary().getIdent());
		}

		OpacClient.context = getApplicationContext();

		try {
			OpacClient.versionName = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

}
