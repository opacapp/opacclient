package de.geeksfactory.opacclient;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class MainPreferenceActivity extends SherlockPreferenceActivity {
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		Preference assistant = (Preference) findPreference("welcome_assistant");
		assistant.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						WelcomeActivity.class);
				startActivity(intent);
				return false;
			}
		});
	}
}
