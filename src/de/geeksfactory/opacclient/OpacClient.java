package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import de.geeksfactory.opacclient.apis.Bond26;
import de.geeksfactory.opacclient.frontend.ErrorActivity;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class OpacClient extends Application {

	public Exception last_exception;

	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;
	public static final String PREF_SELECTED_ACCOUNT = "selectedAccount";

	private SharedPreferences sp;

	private Account account;
	private OpacApi api;
	private Library library;

	public boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	private OpacApi initApi(Library lib) throws ClientProtocolException,
			SocketException, IOException, NotReachableException {
		OpacApi api = null;
		if (lib.getApi().equals("bond26"))
			api = new Bond26();
		else
			return null;

		api.init(this, lib.getData());
		return api;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotReachableException e) {
			// TODO Auto-generated catch block
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
		Account acc = data.getAccount(sp.getLong(PREF_SELECTED_ACCOUNT, 0));
		data.close();
		return acc;
	}

	public void setAccount(long id) {
		sp.edit().putLong(OpacClient.PREF_SELECTED_ACCOUNT, id).commit();
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
			library = getLibrary(getAccount().getBib());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		sp = PreferenceManager.getDefaultSharedPreferences(this);
	}

	public void web_error(Exception e) {
		web_error(e, getApi().getLast_error());
	}

	public void web_error(Exception e, String t) {
		Intent intent = new Intent(this, ErrorActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("e", Log.getStackTraceString(e));
		intent.putExtra("t", t);
		last_exception = e;
		startActivity(intent);
	}

}
