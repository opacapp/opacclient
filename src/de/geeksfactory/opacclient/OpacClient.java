package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import de.geeksfactory.opacclient.apis.Bond26;
import de.geeksfactory.opacclient.apis.OCLC2011;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.Zones22;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

@ReportsCrashes(formKey = "", mailTo = "raphael+opac@geeksfactory.de", mode = org.acra.ReportingInteractionMode.DIALOG, resToastText = R.string.crash_toast_text, resDialogText = R.string.crash_dialog_text)
public class OpacClient extends Application {

	public Exception last_exception;

	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;
	public static final String PREF_SELECTED_ACCOUNT = "selectedAccount";
	public static final String PREF_HOME_BRANCH_PREFIX = "homeBranch_";

	private SharedPreferences sp;

	private Account account;
	private OpacApi api;
	private Library library;

	public boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public OpacApi getNewApi(Library lib) throws ClientProtocolException,
			SocketException, IOException, NotReachableException {
		OpacApi newApiInstance = null;
		if (lib.getApi().equals("bond26"))
			newApiInstance = new Bond26();
		else if (lib.getApi().equals("oclc2011"))
			newApiInstance = new OCLC2011();
		else if (lib.getApi().equals("zones22"))
			newApiInstance = new Zones22();
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

	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (getLibrary() != null) {
			ACRA.getErrorReporter().putCustomData("library",
					getLibrary().getIdent());
		}
	}

}
