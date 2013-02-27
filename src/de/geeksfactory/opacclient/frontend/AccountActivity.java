package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ProgressDialog;
import org.json.JSONException;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountActivity extends OpacActivity {

	protected ProgressDialog dialog;

	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_NOUSER = 1;
	public static final int STATUS_FAILED = 2;

	public static final long MAX_CACHE_AGE = (1000 * 3600 * 2);

	private LoadTask lt;
	private CancelTask ct;
	private ProlongTask pt;

	private Account account;

	private boolean refreshing = false;
	private long refreshtime;
	private boolean fromcache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(app);

		if (getIntent().getExtras() != null) {
			if (getIntent().getExtras().getLong("notif_last") > 0) {
				SharedPreferences.Editor spe = sp.edit();
				spe.putLong("notification_last", getIntent().getExtras()
						.getLong("notif_last"));
				spe.commit();
				if (getIntent().getExtras().getLong("account") != app
						.getAccount().getId()) {
					app.setAccount(getIntent().getExtras().getLong("account"));
					accountSelected();
				}
				NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nMgr.cancel(OpacClient.NOTIF_ID);
			}
			if (getIntent().getExtras().getBoolean("showmenu", false)) {
				final Handler handler = new Handler();
				// Just show the menu to explain that is there if people start
				// version 2 for the first time.
				// We need a handler because if we just put this in onCreate
				// nothing happens. I don't have any idea, why.
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						getSlidingMenu().showMenu(true);
					}
				}, 500);
			}
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Handler handler = new Handler();
		// schedule alarm here and post runnable as soon as scheduled
		handler.post(new Runnable() {
			@Override
			public void run() {
				refreshage();
				handler.postDelayed(this, 60000);
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// see https://github.com/raphaelm/opacclient/issues/70
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_account, menu);
		if (refreshing) {
			// We want it to look as good as possible everywhere
			if (Build.VERSION.SDK_INT >= 14) {
				menu.findItem(R.id.action_refresh).setActionView(
						R.layout.loading_indicator);
			} else {
				menu.findItem(R.id.action_refresh).setVisible(false);
				setSupportProgressBarIndeterminateVisibility(true);
			}
		} else {
			if (Build.VERSION.SDK_INT >= 14) {
				menu.findItem(R.id.action_refresh).setActionView(null);
			} else {
				menu.findItem(R.id.action_refresh).setActionView(null);
				setSupportProgressBarIndeterminateVisibility(false);
			}
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			refresh();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void accountSelected() {
		onStart();
		super.accountSelected();
	}

	@Override
	public void onStart() {
		super.onStart();
		refreshing = false;
		invalidateOptionsMenu();
		setContentView(R.layout.loading);

		account = app.getAccount();
		if (!app.getApi().isAccountSupported(app.getLibrary())
				&& !app.getApi().isAccountExtendable()) {
			// Not supported with this api at all
			setContentView(R.layout.unsupported_error);
			((TextView) findViewById(R.id.tvErrBody))
					.setText(R.string.account_unsupported_api);
			((Button) findViewById(R.id.btSend)).setText(R.string.write_mail);
			((Button) findViewById(R.id.btSend))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent emailIntent = new Intent(
									android.content.Intent.ACTION_SEND);
							emailIntent
									.putExtra(
											android.content.Intent.EXTRA_EMAIL,
											new String[] { "raphael+opac@geeksfactory.de" });
							emailIntent
									.putExtra(
											android.content.Intent.EXTRA_SUBJECT,
											"Bibliothek "
													+ app.getLibrary()
															.getIdent());
							emailIntent.putExtra(
									android.content.Intent.EXTRA_TEXT,
									"Ich bin interessiert, zu helfen.");
							emailIntent.setType("text/plain");
							startActivity(Intent.createChooser(emailIntent,
									getString(R.string.write_mail)));
						}
					});

		} else if (account.getPassword() == null
				|| account.getPassword().equals("null")
				|| account.getPassword().equals("")
				|| account.getName() == null
				|| account.getName().equals("null")
				|| account.getName().equals("")) {
			// No credentials entered

			setContentView(R.layout.answer_error);
			((Button) findViewById(R.id.btPrefs))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(AccountActivity.this,
									AccountEditActivity.class);
							intent.putExtra(
									AccountEditActivity.EXTRA_ACCOUNT_ID, app
											.getAccount().getId());
							startActivity(intent);
						}
					});
			((TextView) findViewById(R.id.tvErrHead)).setText("");
			((TextView) findViewById(R.id.tvErrBody))
					.setText(R.string.status_nouser);

		} else if (!app.getApi().isAccountSupported(app.getLibrary())) {

			// We need help
			setContentView(R.layout.unsupported_error);

			((TextView) findViewById(R.id.tvErrBody))
					.setText(R.string.account_unsupported);
			((Button) findViewById(R.id.btSend))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog = ProgressDialog.show(AccountActivity.this,
									"", getString(R.string.report_sending),
									true, true, new OnCancelListener() {
										@Override
										public void onCancel(
												DialogInterface arg0) {
											finish();
										}
									});
							dialog.show();
							new SendTask().execute(this);
						}
					});

		} else {
			// Supported

			AccountDataSource adatasource = new AccountDataSource(this);
			adatasource.open();
			refreshtime = adatasource.getCachedAccountDataTime(account);
			if (refreshtime > 0) {
				displaydata(adatasource.getCachedAccountData(account), true);
				if (System.currentTimeMillis() - refreshtime > MAX_CACHE_AGE) {
					refresh();
				}
			} else {
				refresh();
			}
			adatasource.close();
		}
	}

	public void refresh() {
		refreshing = true;
		invalidateOptionsMenu();
		lt = new LoadTask();
		lt.execute(app, getIntent().getIntExtra("item", 0));
	}

	protected void cancel(final String a) {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || fromcache) {
			Toast.makeText(this, R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.cancel_confirm)
				.setCancelable(true)
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface d, int id) {
								d.cancel();
							}
						})
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface d, int id) {
								d.dismiss();
								dialog = ProgressDialog.show(
										AccountActivity.this, "",
										getString(R.string.doing_cancel), true);
								dialog.show();
								ct = new CancelTask();
								ct.execute(app, a);
							}
						})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface d) {
						if (d != null)
							d.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void cancel_done(int result) {
		if (result == STATUS_SUCCESS) {
			invalidateData();
		}
	}

	protected void prolong(final String a) {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || age > MAX_CACHE_AGE) {
			Toast.makeText(this, R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		dialog = ProgressDialog.show(AccountActivity.this, "",
				getString(R.string.doing_prolong), true);
		dialog.show();
		pt = new ProlongTask();
		pt.execute(app, a);
	}

	public class SendTask extends AsyncTask<Object, Object, Integer> {

		@Override
		protected Integer doInBackground(Object... arg0) {
			DefaultHttpClient dc = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
					"http://opacapp.de/crashreport.php");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("traceback", ""));
			try {
				nameValuePairs
						.add(new BasicNameValuePair("version",
								getPackageManager().getPackageInfo(
										getPackageName(), 0).versionName));
			} catch (Exception e) {
				e.printStackTrace();
			}

			nameValuePairs.add(new BasicNameValuePair("android",
					android.os.Build.VERSION.RELEASE));
			nameValuePairs.add(new BasicNameValuePair("sdk", ""
					+ android.os.Build.VERSION.SDK_INT));
			nameValuePairs.add(new BasicNameValuePair("device",
					android.os.Build.MANUFACTURER + " "
							+ android.os.Build.MODEL));
			nameValuePairs.add(new BasicNameValuePair("bib", app.getLibrary()
					.getIdent()));

			try {
				nameValuePairs.add(new BasicNameValuePair("html", app.getApi()
						.getAccountExtendableInfo(app.getAccount())));
			} catch (Exception e1) {
				e1.printStackTrace();
				return 1;
			}

			try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			HttpResponse response;
			try {
				response = dc.execute(httppost);
				response.getEntity().consumeContent();
			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			Button btSend = (Button) findViewById(R.id.btSend);
			btSend.setEnabled(false);
			if (result == 0) {
				Toast toast = Toast.makeText(AccountActivity.this,
						getString(R.string.report_sent), Toast.LENGTH_SHORT);
				toast.show();
			} else {
				Toast toast = Toast.makeText(AccountActivity.this,
						getString(R.string.report_error), Toast.LENGTH_SHORT);
				toast.show();
			}

		}
	}

	public void invalidateData() {
		AccountDataSource adatasource = new AccountDataSource(this);
		adatasource.open();
		adatasource.invalidateCachedAccountData(account);
		adatasource.close();
		onStart();
	}

	public void prolong_done(int result) {
		if (result == STATUS_SUCCESS) {
			invalidateData();
		} else if (result == STATUS_FAILED) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Der Web-Opac meldet: " + app.getApi().getLast_error())
					.setCancelable(true)
					.setNegativeButton(R.string.dismiss,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	public class LoadTask extends OpacTask<AccountData> {

		private boolean success = true;
		private Exception exception;

		@Override
		protected AccountData doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				AccountData res = app.getApi().account(
						((OpacClient) getApplication()).getAccount());
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				success = false;
				exception = e;
			} catch (java.net.SocketException e) {
				success = false;
				exception = e;
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(AccountData result) {
			if (success) {
				loaded(result);
			} else {
				refreshing = false;
				invalidateOptionsMenu();

				show_connectivity_error(exception);
			}
		}
	}

	public void show_connectivity_error(Exception e) {
		View tvError = findViewById(R.id.tvError);
		if (tvError != null) {
			tvError.setVisibility(View.VISIBLE);
			((TextView) tvError).setText(R.string.error_connection);
		} else {
			setContentView(R.layout.connectivity_error);
			if (e != null && e instanceof NotReachableException)
				((TextView) findViewById(R.id.tvErrBody))
						.setText(R.string.connection_error_detail_nre);
			((Button) findViewById(R.id.btRetry))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							onStart();
						}
					});
		}
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		setContentView(R.layout.answer_error);
		((Button) findViewById(R.id.btPrefs))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(AccountActivity.this,
								AccountEditActivity.class);
						intent.putExtra(AccountEditActivity.EXTRA_ACCOUNT_ID,
								account.getId());
						startActivity(intent);
					}
				});
		((TextView) findViewById(R.id.tvErrBody)).setText(s);
	}

	public void loaded(final AccountData result) {

		if (result == null) {
			refreshing = false;
			invalidateOptionsMenu();

			if (app.getApi().getLast_error() == null
					|| app.getApi().getLast_error().equals("")) {
				show_connectivity_error(null);
			} else {
				AccountDataSource adatasource = new AccountDataSource(this);
				adatasource.open();
				adatasource.invalidateCachedAccountData(account);
				adatasource.close();
				dialog_wrong_credentials(app.getApi().getLast_error(), true);
			}
			return;
		}

		if (mFrag != null)
			mFrag.reload();

		AccountDataSource adatasource = new AccountDataSource(this);
		adatasource.open();
		adatasource.storeCachedAccountData(
				adatasource.getAccount(result.getAccount()), result);
		adatasource.close();

		if (result.getAccount() == account.getId()) {
			// The account this data is for is still visible

			refreshing = false;
			invalidateOptionsMenu();

			refreshtime = System.currentTimeMillis();

			displaydata(result, false);
		}

	}

	public void displaydata(AccountData result, boolean fromcache) {
		setContentView(R.layout.account_activity);

		this.fromcache = fromcache;

		((TextView) findViewById(R.id.tvAccLabel)).setText(account.getLabel());
		((TextView) findViewById(R.id.tvAccUser)).setText(account.getName());
		TextView tvAccCity = (TextView) findViewById(R.id.tvAccCity);
		Library lib;
		try {
			lib = ((OpacClient) getApplication()).getLibrary(account
					.getLibrary());
			if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
				tvAccCity.setText(lib.getCity() + " · " + lib.getTitle());
			} else {
				tvAccCity.setText(lib.getCity());
			}
		} catch (IOException e) {
			ACRA.getErrorReporter().handleException(e);
			e.printStackTrace();
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
		}

		LinearLayout llLent = (LinearLayout) findViewById(R.id.llLent);
		llLent.removeAllViews();

		if (result.getLent().size() == 0) {
			TextView t1 = new TextView(this);
			t1.setText(R.string.entl_none);
			llLent.addView(t1);
		} else {
			for (ContentValues item : result.getLent()) {
				View v = getLayoutInflater().inflate(R.layout.lent_listitem,
						null);

				if (item.containsKey(AccountData.KEY_LENT_TITLE)) {
					((TextView) v.findViewById(R.id.tvTitel)).setText(Html
							.fromHtml(item
									.getAsString(AccountData.KEY_LENT_TITLE)));
				}
				if (item.containsKey(AccountData.KEY_LENT_AUTHOR)) {
					((TextView) v.findViewById(R.id.tvVerfasser)).setText(Html
							.fromHtml(item
									.getAsString(AccountData.KEY_LENT_AUTHOR)));
				}

				((TextView) v.findViewById(R.id.tvStatus))
						.setVisibility(View.VISIBLE);
				if (item.containsKey(AccountData.KEY_LENT_STATUS)
						&& item.containsKey(AccountData.KEY_LENT_DEADLINE)) {
					((TextView) v.findViewById(R.id.tvStatus))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_LENT_DEADLINE)
									+ " ("
									+ item.getAsString(AccountData.KEY_LENT_STATUS)
									+ ")"));
				} else if (item.containsKey(AccountData.KEY_LENT_STATUS)) {
					((TextView) v.findViewById(R.id.tvStatus)).setText(Html
							.fromHtml(item
									.getAsString(AccountData.KEY_LENT_STATUS)));
				} else if (item.containsKey(AccountData.KEY_LENT_DEADLINE)) {
					((TextView) v.findViewById(R.id.tvStatus))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_LENT_DEADLINE)));
				} else {
					((TextView) v.findViewById(R.id.tvStatus))
							.setVisibility(View.GONE);
				}

				if (item.containsKey(AccountData.KEY_LENT_LENDING_BRANCH)) {
					((TextView) v.findViewById(R.id.tvZst))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_LENT_LENDING_BRANCH)));
					((TextView) v.findViewById(R.id.tvZst))
							.setVisibility(View.VISIBLE);
				} else if (item.containsKey(AccountData.KEY_LENT_BRANCH)) {
					((TextView) v.findViewById(R.id.tvZst)).setText(Html
							.fromHtml(item
									.getAsString(AccountData.KEY_LENT_BRANCH)));
					((TextView) v.findViewById(R.id.tvZst))
							.setVisibility(View.VISIBLE);
				} else {
					((TextView) v.findViewById(R.id.tvZst))
							.setVisibility(View.GONE);
				}

				if (item.containsKey(AccountData.KEY_LENT_LINK)) {
					v.findViewById(R.id.ivProlong).setTag(
							item.getAsString(AccountData.KEY_LENT_LINK));
					((ImageView) v.findViewById(R.id.ivProlong))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									prolong((String) arg0.getTag());
								}
							});
					((ImageView) v.findViewById(R.id.ivProlong))
							.setVisibility(View.VISIBLE);
				} else {
					((ImageView) v.findViewById(R.id.ivProlong))
							.setVisibility(View.INVISIBLE);
				}

				llLent.addView(v);
			}
		}

		LinearLayout llRes = (LinearLayout) findViewById(R.id.llReservations);
		llRes.removeAllViews();

		if (result.getReservations().size() == 0) {
			TextView t1 = new TextView(this);
			t1.setText(R.string.reservations_none);
			llRes.addView(t1);
		} else {
			for (ContentValues item : result.getReservations()) {
				View v = getLayoutInflater().inflate(
						R.layout.reservation_listitem, null);

				if (item.containsKey(AccountData.KEY_RESERVATION_TITLE)) {
					((TextView) v.findViewById(R.id.tvTitel))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_RESERVATION_TITLE)));
				}
				if (item.containsKey(AccountData.KEY_RESERVATION_AUTHOR)) {
					((TextView) v.findViewById(R.id.tvVerfasser))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_RESERVATION_AUTHOR)));
				}

				if (item.containsKey(AccountData.KEY_RESERVATION_READY)) {
					((TextView) v.findViewById(R.id.tvStatus))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_RESERVATION_READY)));
					((TextView) v.findViewById(R.id.tvStatus))
							.setVisibility(View.VISIBLE);
				} else {
					((TextView) v.findViewById(R.id.tvStatus))
							.setVisibility(View.GONE);
				}

				if (item.containsKey(AccountData.KEY_RESERVATION_BRANCH)) {
					((TextView) v.findViewById(R.id.tvZst))
							.setText(Html.fromHtml(item
									.getAsString(AccountData.KEY_RESERVATION_BRANCH)));
					((TextView) v.findViewById(R.id.tvZst))
							.setVisibility(View.VISIBLE);
				} else {
					((TextView) v.findViewById(R.id.tvZst))
							.setVisibility(View.GONE);
				}

				if (item.containsKey(AccountData.KEY_RESERVATION_CANCEL)) {
					v.findViewById(R.id.ivCancel)
							.setTag(item
									.getAsString(AccountData.KEY_RESERVATION_CANCEL));
					((ImageView) v.findViewById(R.id.ivCancel))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									cancel((String) arg0.getTag());
								}
							});
					((ImageView) v.findViewById(R.id.ivCancel))
							.setVisibility(View.VISIBLE);
				} else {
					((ImageView) v.findViewById(R.id.ivCancel))
							.setVisibility(View.INVISIBLE);
				}
				llRes.addView(v);
			}
		}
		refreshage();
	}

	public void refreshage() {
		if (findViewById(R.id.tvAge) == null)
			return;

		long age = System.currentTimeMillis() - refreshtime;
		if (age < (3600 * 1000)) {
			((TextView) findViewById(R.id.tvAge)).setText(getResources()
					.getQuantityString(R.plurals.account_age_minutes,
							(int) (age / (60 * 1000)),
							(int) (age / (60 * 1000))));
		} else if (age < 24 * 3600 * 1000) {
			((TextView) findViewById(R.id.tvAge)).setText(getResources()
					.getQuantityString(R.plurals.account_age_hours,
							(int) (age / (3600 * 1000)),
							(int) (age / (3600 * 1000))));

		} else {
			((TextView) findViewById(R.id.tvAge)).setText(getResources()
					.getQuantityString(R.plurals.account_age_days,
							(int) (age / (24 * 3600 * 1000)),
							(int) (age / (24 * 3600 * 1000))));
		}
	}

	public class CancelTask extends OpacTask<Integer> {
		private boolean success = true;

		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			String a = (String) arg0[1];
			try {
				app.getApi().cancel(account, a);
				success = true;
			} catch (java.net.UnknownHostException e) {
				success = false;
			} catch (java.net.SocketException e) {
				success = false;
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return STATUS_SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();

			if (success) {
				cancel_done(result);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						AccountActivity.this);
				builder.setMessage(R.string.connection_error)
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}

	public class ProlongTask extends OpacTask<Integer> {
		private boolean success = true;

		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			String a = (String) arg0[1];
			try {
				boolean res = app.getApi().prolong(account, a);
				success = true;
				if (res) {
					return STATUS_SUCCESS;
				} else {
					return STATUS_FAILED;
				}
			} catch (java.net.UnknownHostException e) {
				success = false;
			} catch (java.net.SocketException e) {
				success = false;
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return STATUS_SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();

			if (success) {
				prolong_done(result);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						AccountActivity.this);
				builder.setMessage(R.string.connection_error)
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (dialog != null) {
			if (dialog.isShowing()) {
				dialog.cancel();
			}
		}

		try {
			if (lt != null) {
				if (!lt.isCancelled()) {
					lt.cancel(true);
				}
			}
			if (ct != null) {
				if (!ct.isCancelled()) {
					ct.cancel(true);
				}
			}
			if (pt != null) {
				if (!pt.isCancelled()) {
					pt.cancel(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
