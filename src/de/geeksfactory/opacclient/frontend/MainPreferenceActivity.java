package de.geeksfactory.opacclient.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.ReminderCheckService;
import de.geeksfactory.opacclient.storage.MetaDataSource;

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

		Preference about = (Preference) findPreference("about");
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						AboutActivity.class);
				startActivity(intent);
				return false;
			}
		});

		Preference changelog = (Preference) findPreference("changelog");
		changelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						ChangelogActivity.class);
				startActivity(intent);
				return false;
			}
		});

		Preference meta = (Preference) findPreference("meta_clear");
		meta.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				MetaDataSource data = new MetaDataSource(
						MainPreferenceActivity.this);
				data.open();
				data.clearMeta();
				data.close();
				Intent i = new Intent(MainPreferenceActivity.this, ReminderCheckService.class);
				startService(i);
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
