package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.ReminderCheckService;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

public class MainPreferenceActivity extends OpacPreferenceActivity {

	public static void openAccountList(Activity ctx) {
		Intent intent = new Intent(ctx, AccountListActivity.class);
		ctx.startActivity(intent);
	}

	protected boolean ebooksSupported() {
		return false;
	}

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
				openAccountList(MainPreferenceActivity.this);
				return false;
			}
		});

		if (!ebooksSupported()) {
			((PreferenceCategory) findPreference("cat_web_opac"))
					.removePreference(findPreference("email"));
		}

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
				adata.notificationClearCache(true);
				adata.close();
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
