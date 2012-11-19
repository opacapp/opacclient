package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import de.geeksfactory.opacclient.frontend.ErrorActivity;
import de.geeksfactory.opacclient.objects.Library;

public class OpacClient extends Application {

	public OpacWebApi ohc;
	public Exception last_exception;

	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;

	public boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public Library getLibrary() {
		return null;
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
			libs.add(Library.fromJSON(files[i], new JSONObject(json)));
		}

		return libs;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
	}

	public void web_error(Exception e) {
		web_error(e, ohc.getLast_error());
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
