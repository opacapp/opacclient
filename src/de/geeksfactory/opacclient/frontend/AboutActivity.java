package de.geeksfactory.opacclient.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.app.Dialog;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;

public class AboutActivity extends SherlockPreferenceActivity {

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.about);

		try {
			findPreference("version")
					.setSummary(
							getPackageManager().getPackageInfo(
									getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		findPreference("website").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse("http://opacapp.de"));
						startActivity(i);
						return false;
					}
				});

		findPreference("developer").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse("http://www.raphaelmichel.de"));
						startActivity(i);
						return false;
					}
				});

		findPreference("feedback").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent emailIntent = new Intent(
								android.content.Intent.ACTION_SEND);
						emailIntent
								.putExtra(
										android.content.Intent.EXTRA_EMAIL,
										new String[] { "raphael+opac@geeksfactory.de" });
						emailIntent.setType("text/plain");
						startActivity(Intent.createChooser(emailIntent,
								getString(R.string.write_mail)));
						return false;
					}
				});

		findPreference("rate_play").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						try {
							Intent i = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("market://details?id=de.geeksfactory.opacclient"));
							startActivity(i);
						} catch (ActivityNotFoundException e) {
							Log.i("rate_play", "no market installed");
						}
						return false;
					}
				});

		findPreference("rate_am").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri
								.parse("http://www.amazon.com/dp/B00946RJQO/"));
						startActivity(i);
						return false;
					}
				});

		findPreference("source").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri
								.parse("http://github.com/raphaelm/opacclient"));
						startActivity(i);
						return false;
					}
				});

		findPreference("osl").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {

						final Dialog dialog = new Dialog(AboutActivity.this);
						dialog.setContentView(R.layout.osl_dialog);
						dialog.setTitle(R.string.osl);
						TextView textview1 = (TextView) dialog
								.findViewById(R.id.textView1);

						String text = "";

						StringBuilder builder = new StringBuilder();
						InputStream fis;
						try {
							fis = getAssets().open("licenses.html");
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(fis, "utf-8"));
							String line;
							while ((line = reader.readLine()) != null) {
								builder.append(line);
							}

							text = builder.toString();
							fis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}

						textview1.setText(Html.fromHtml(text));

						Button dialogButton = (Button) dialog
								.findViewById(R.id.button1);
						// if button is clicked, close the custom dialog
						dialogButton.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});

						dialog.show();

						return false;
					}
				});
		findPreference("thanks").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {

						final Dialog dialog = new Dialog(AboutActivity.this);
						dialog.setContentView(R.layout.osl_dialog);
						dialog.setTitle(R.string.thanks);
						TextView textview1 = (TextView) dialog
								.findViewById(R.id.textView1);

						String text = "";

						StringBuilder builder = new StringBuilder();
						InputStream fis;
						try {
							fis = getAssets().open("thanks.html");
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(fis, "utf-8"));
							String line;
							while ((line = reader.readLine()) != null) {
								builder.append(line);
							}

							text = builder.toString();
							fis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}

						textview1.setText(Html.fromHtml(text));

						Button dialogButton = (Button) dialog
								.findViewById(R.id.button1);
						// if button is clicked, close the custom dialog
						dialogButton.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});

						dialog.show();

						return false;
					}
				});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
