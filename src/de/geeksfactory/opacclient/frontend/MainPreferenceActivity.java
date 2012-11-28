package de.geeksfactory.opacclient.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;

public class MainPreferenceActivity extends SherlockPreferenceActivity {
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.settings);

		Preference assistant = (Preference) findPreference("accounts");
		assistant.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						AccountListActivity.class);
				startActivity(intent);
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
