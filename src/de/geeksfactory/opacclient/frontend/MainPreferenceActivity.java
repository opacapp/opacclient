package de.geeksfactory.opacclient.frontend;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.ReminderCheckService;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

public class MainPreferenceActivity extends OpacPreferenceActivity {
	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.settings);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH
				|| !getPackageManager()
						.hasSystemFeature("android.hardware.nfc")) {
			findPreference("nfc_search").setEnabled(false);
		}

		Preference assistant = findPreference("accounts");
		assistant.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						AccountListActivity.class);
				startActivity(intent);
				return false;
			}
		});

		Preference meta = findPreference("meta_clear");
		meta.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				MetaDataSource data = new SQLMetaDataSource(
						MainPreferenceActivity.this);
				data.open();
				data.clearMeta();
				data.close();
				AccountDataSource adata = new AccountDataSource(
						MainPreferenceActivity.this);
				adata.open();
				adata.invalidateCachedData();
				adata.close();
				PreferenceManager
						.getDefaultSharedPreferences(
								MainPreferenceActivity.this).edit()
						.putLong("notification_last", 0).commit();
				Intent i = new Intent(MainPreferenceActivity.this,
						ReminderCheckService.class);
				startService(i);
				return false;
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		}
		return super.onOptionsItemSelected(item);
	}
}
