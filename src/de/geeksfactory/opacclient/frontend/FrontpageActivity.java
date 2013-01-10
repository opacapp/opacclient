package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.app.ProgressDialog;
import com.actionbarsherlock.view.Menu;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class FrontpageActivity extends OpacActivity {
	protected ProgressDialog dialog;

	public void urlintent() {
		Uri d = getIntent().getData();

		if (d.getHost().equals("de.geeksfactory.opacclient")) {
			String medianr = d.getQueryParameter("id");

			if (medianr != null) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item_id", medianr);
				startActivity(intent);
				finish();
				return;
			}

			String titel = d.getQueryParameter("titel");
			String verfasser = d.getQueryParameter("verfasser");
			String schlag_a = d.getQueryParameter("schlag_a");
			String schlag_b = d.getQueryParameter("schlag_b");
			String isbn = d.getQueryParameter("isbn");
			String jahr_von = d.getQueryParameter("jahr_von");
			String jahr_bis = d.getQueryParameter("jahr_bis");
			String verlag = d.getQueryParameter("verlag");
			Intent myIntent = new Intent(FrontpageActivity.this,
					SearchResultsActivity.class);
			myIntent.putExtra("titel", (titel != null ? titel : ""));
			myIntent.putExtra("verfasser", (verfasser != null ? verfasser : ""));
			myIntent.putExtra("schlag_a", (schlag_a != null ? schlag_a : ""));
			myIntent.putExtra("schlag_b", (schlag_b != null ? schlag_b : ""));
			myIntent.putExtra("isbn", (isbn != null ? isbn : ""));
			myIntent.putExtra("jahr_von", (jahr_von != null ? jahr_von : ""));
			myIntent.putExtra("jahr_bis", (jahr_bis != null ? jahr_bis : ""));
			myIntent.putExtra("verlag", (verlag != null ? verlag : ""));
			startActivity(myIntent);
			finish();
		} else if (d.getHost().equals("opacapp.de")) {
			String[] split = d.getPath().split(":");
			String bib = split[1];
			if (!app.getLibrary().getIdent().equals(bib)) {
				Intent i = new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://opacapp.de/web" + d.getPath()));
				startActivity(i);
				return;
			}
			String medianr = split[2];
			Intent intent = new Intent(FrontpageActivity.this,
					SearchResultDetailsActivity.class);
			intent.putExtra("item_id", medianr);
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getSupportActionBar().hide();

		if (app.getLibrary() == null) {
			// Migrate
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(this);
			if (!sp.getString("opac_bib", "").equals("")) {
				Library lib = null;
				try {
					lib = app.getLibrary(sp.getString("opac_bib", ""));
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (lib != null) {
					AccountDataSource data = new AccountDataSource(this);
					data.open();
					Account acc = new Account();
					acc.setBib(lib.getIdent());
					acc.setLabel(getString(R.string.default_account_name));
					if (!sp.getString("opac_usernr", "").equals("")) {
						acc.setName(sp.getString("opac_usernr", ""));
						acc.setPassword(sp.getString("opac_password", ""));
					}
					long insertedid = data.addAccount(acc);
					data.close();

					sp.edit()
							.putLong(OpacClient.PREF_SELECTED_ACCOUNT,
									insertedid).commit();

					dialog = ProgressDialog.show(this, "",
							getString(R.string.connecting_initially), true);
					dialog.show();

					new InitTask().execute(app);

					Toast.makeText(
							this,
							"Neue Version! Alte Accountdaten wurden wiederhergestellt.",
							Toast.LENGTH_LONG);

					return;

				} else {
					Toast.makeText(
							this,
							"Neue Version! Wiederherstellung alter Zugangsdaten ist fehlgeschlagen.",
							Toast.LENGTH_LONG);
				}
			}

			// Create new
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
			return;
		}

		setContentView(R.layout.frontpage_activity);

		ImageView ivSearch = (ImageView) findViewById(R.id.ivGoSearch);
		ImageView ivScan = (ImageView) findViewById(R.id.ivGoScan);
		ImageView ivAccount = (ImageView) findViewById(R.id.ivGoAccount);
		ImageView ivStarred = (ImageView) findViewById(R.id.ivGoStarred);

		ImageView ivMAccs = (ImageView) findViewById(R.id.ivMAcc);
		ImageView ivMPrefs = (ImageView) findViewById(R.id.ivMPrefs);
		ImageView ivMInfo = (ImageView) findViewById(R.id.ivMInfo);

		ivSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchActivity.class);
				startActivity(intent);
			}
		});
		ivAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						AccountActivity.class);
				startActivity(intent);
			}
		});
		ivScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchActivity.class);
				intent.putExtra("barcode", true);
				startActivity(intent);
			}
		});
		ivStarred.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						StarredActivity.class);
				startActivity(intent);
			}
		});
		ivMAccs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				selectaccount(new OpacActivity.AccountSelectedListener() {
					@Override
					public void accountSelected(Account account) {
						TextView tvBn = (TextView) findViewById(R.id.tvBibname);
						if (app.getLibrary().getTitle() != null
								&& !app.getLibrary().getTitle().equals("null"))
							tvBn.setText(app.getLibrary().getCity() + "\n"
									+ app.getLibrary().getTitle());
						else
							tvBn.setText(app.getLibrary().getCity());

						try {
							if (app.getLibrary().getData()
									.getString("information") != null) {
								if (!app.getLibrary().getData()
										.getString("information")
										.equals("null")) {
									((ImageView) findViewById(R.id.ivMInfo))
											.setVisibility(View.VISIBLE);
								} else {
									((ImageView) findViewById(R.id.ivMInfo))
											.setVisibility(View.GONE);
								}
							} else {
								((ImageView) findViewById(R.id.ivMInfo))
										.setVisibility(View.GONE);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		ivMPrefs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						MainPreferenceActivity.class);
				startActivity(intent);
			}
		});
		ivMInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						InfoActivity.class);
				startActivity(intent);
			}
		});

		if (getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (app.getLibrary() != null) {
			TextView tvBn = (TextView) findViewById(R.id.tvBibname);
			if (app.getLibrary().getTitle() != null
					&& !app.getLibrary().getTitle().equals("null"))
				tvBn.setText(app.getLibrary().getCity() + "\n"
						+ app.getLibrary().getTitle());
			else
				tvBn.setText(app.getLibrary().getCity());

			try {
				if (app.getLibrary().getData().getString("information") != null) {
					if (!app.getLibrary().getData().getString("information")
							.equals("null")) {
						((ImageView) findViewById(R.id.ivMInfo))
								.setVisibility(View.VISIBLE);
					} else {
						((ImageView) findViewById(R.id.ivMInfo))
								.setVisibility(View.GONE);
					}
				} else {
					((ImageView) findViewById(R.id.ivMInfo))
							.setVisibility(View.GONE);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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
			Intent intent = new Intent(FrontpageActivity.this,
					FrontpageActivity.class);
			startActivity(intent);
		}
	}

}
