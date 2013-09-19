/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.holoeverywhere.app.Dialog;

import android.content.ActivityNotFoundException;
import android.content.Intent;
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

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

public class AboutActivity extends OpacPreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		populate();
	}

	@SuppressWarnings("deprecation")
	protected void populate() {
		addPreferencesFromResource(R.xml.about);

		String version = OpacClient.versionName;

		try {
			String text = "";

			StringBuilder builder = new StringBuilder();
			InputStream fis;
			fis = getAssets().open("buildnum.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, "utf-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			text = builder.toString();
			fis.close();
			if (!text.equals(version))
				version += " (Build: " + text + ")";
		} catch (IOException e) {
			e.printStackTrace();
		}

		findPreference("version").setSummary(version);

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
						emailIntent.putExtra(
								android.content.Intent.EXTRA_EMAIL,
								new String[] { "info@opacapp.de" });
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
						asset_dialog("licenses.html", R.string.osl);

						return false;
					}
				});

		findPreference("privacy").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						asset_dialog("privacy.html", R.string.privacy);
						return false;
					}
				});

		findPreference("thanks").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						asset_dialog("thanks.html", R.string.changelog);
						return false;
					}
				});

		findPreference("changelog").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						asset_dialog("changelog.html", R.string.changelog);
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

	private void asset_dialog(String filename, int title) {

		final Dialog dialog = new Dialog(AboutActivity.this);
		dialog.setContentView(R.layout.osl_dialog);
		dialog.setTitle(title);
		TextView textview1 = (TextView) dialog.findViewById(R.id.textView1);

		String text = "";

		StringBuilder builder = new StringBuilder();
		InputStream fis;
		try {
			fis = getAssets().open(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, "utf-8"));
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

		Button dialogButton = (Button) dialog.findViewById(R.id.button1);
		// if button is clicked, close the custom dialog
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}
}
