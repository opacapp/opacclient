package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class OpacClient extends Application {

	public OpacWebApi ohc;
	public JSONObject bibs;

	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;

	protected boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public JSONArray get_bib() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		try {
			Log.i("get_bib", sp.getString("opac_bib", "Mannheim"));
			return bibs.getJSONArray(sp.getString("opac_bib", "Mannheim"));
		} catch (JSONException e) {
			web_error(e, "jsonerror");
			return null;
		}
	}

	protected void load_bibs() {
		try {
			StringBuilder builder = new StringBuilder();
			InputStream fis = getAssets().open("bibs.json");

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, "utf-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			fis.close();
			String json = builder.toString();
			bibs = new JSONObject(json).getJSONObject("bibs");
		} catch (IOException e) {
			web_error(e, "ioerror");
		} catch (JSONException e) {
			web_error(e, "jsonerror");
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);

		load_bibs();

		ohc = new OpacWebApi(sp.getString("opac_url",
				getResources().getString(R.string.opac_mannheim)), this,
				this.get_bib());
	}

	public void web_error(Exception e) {
		web_error(e, ohc.getLast_error());
	}

	public void web_error(Exception e, String t) {
		Intent intent = new Intent(this, ErrorActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("e", Log.getStackTraceString(e));
		intent.putExtra("t", t);
		startActivity(intent);
	}

}
