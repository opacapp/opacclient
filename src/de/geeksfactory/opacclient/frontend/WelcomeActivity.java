package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.holoeverywhere.app.ProgressDialog;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class WelcomeActivity extends OpacActivity {
	protected ProgressDialog dialog;
	private List<Library> libraries;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_activity);

		final SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(WelcomeActivity.this);
		ListView lv = (ListView) findViewById(R.id.lvBibs);
		try {
			libraries = app.getLibraries();
			lv.setAdapter(new LibraryListAdapter(this, libraries));
		} catch (JSONException e) {
			app.web_error(e, "jsonerror");
		} catch (IOException e) {
			app.web_error(e, "jsonerror");
		}

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				AccountDataSource data = new AccountDataSource(
						WelcomeActivity.this);
				data.open();
				Account acc = new Account();
				acc.setBib(libraries.get(position).getIdent());
				acc.setLabel(getString(R.string.default_account_name));
				long insertedid = data.addAccount(acc);
				data.close();

				sp.edit().putLong(OpacClient.PREF_SELECTED_ACCOUNT, insertedid)
						.commit();

				dialog = ProgressDialog.show(WelcomeActivity.this, "",
						getString(R.string.connecting_initially), true);
				dialog.show();

				new InitTask().execute(app);
			}
		});
	}

	public class InitTask extends OpacTask<Integer> {
		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				app.getApi().start();
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return 0;
		}

		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			Intent intent = new Intent(WelcomeActivity.this,
					FrontpageActivity.class);
			startActivity(intent);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (dialog != null) {
			if (dialog.isShowing()) {
				dialog.cancel();
			}
		}
	}

}
