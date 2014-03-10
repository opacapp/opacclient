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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.app.ProgressDialog;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Button;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.ProlongResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountFragment extends Fragment implements AccountSelectedListener {

	protected ProgressDialog dialog;
	protected AlertDialog adialog;
	protected OpacClient app;

	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_NOUSER = 1;
	public static final int STATUS_FAILED = 2;

	public static final long MAX_CACHE_AGE = (1000 * 3600 * 2);

	private LoadTask lt;
	private CancelTask ct;
	private OpacTask<?> pt;
	private OpacTask<Uri> dt;
	private BookingTask bt;

	private Account account;

	private boolean refreshing = false;
	private long refreshtime;
	private boolean fromcache;
	protected View view;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = inflater.inflate(R.layout.fragment_account, container, false);
		app = (OpacClient) getActivity().getApplication();
		account = app.getAccount();
		
		// TODO:
//		if (getIntent().getExtras() != null) {
//			if (getIntent().getExtras().containsKey("notifications")) {
//
//				AccountDataSource adata = new AccountDataSource(this);
//				adata.open();
//				Bundle notif = getIntent().getExtras().getBundle(
//						"notifications");
//				Set<String> keys = notif.keySet();
//				for (String key : keys) {
//					long[] val = notif.getLongArray(key);
//					adata.notificationSave(val[0], val[1]);
//				}
//				adata.close();
//
//				if (getIntent().getExtras().getLong("account") != app
//						.getAccount().getId()) {
//					app.setAccount(getIntent().getExtras().getLong("account"));
//					accountSelected();
//				}
//				NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//				nMgr.cancel(OpacClient.NOTIF_ID);
//			}

		setHasOptionsMenu(true);
		
		accountSelected(app.getAccount());
		
		final Handler handler = new Handler();
		// schedule alarm here and post runnable as soon as scheduled
		handler.post(new Runnable() {
			@Override
			public void run() {
				refreshage();
				handler.postDelayed(this, 60000);
			}
		});
		
		return view;
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.activity_account, menu);
		if (refreshing) {
			// We want it to look as good as possible everywhere
			if (Build.VERSION.SDK_INT >= 14) {
				menu.findItem(R.id.action_refresh).setActionView(
						R.layout.actionbar_loading_indicator);
				getSupportActivity().setSupportProgressBarIndeterminateVisibility(false);
			} else {
				// TODO: Does this crash on pre-14?
				menu.findItem(R.id.action_refresh).setVisible(false);
				getSupportActivity().setSupportProgressBarIndeterminateVisibility(true);
			}
		} else {
			if (Build.VERSION.SDK_INT >= 14) {
				menu.findItem(R.id.action_refresh).setActionView(null);
				getSupportActivity().setSupportProgressBarIndeterminateVisibility(false);
			} else {
				// TODO: Does this crash on pre-14?
				menu.findItem(R.id.action_refresh).setActionView(null);
				getSupportActivity().setSupportProgressBarIndeterminateVisibility(false);
			}
		}
		if ((app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL) != 0) {
			menu.findItem(R.id.action_prolong_all).setVisible(true);
		} else {
			menu.findItem(R.id.action_prolong_all).setVisible(false);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	private void prolongAll() {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || age > MAX_CACHE_AGE) {
			Toast.makeText(getActivity(), R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.prolong_all_confirm)
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
										getActivity(), "",
										getString(R.string.doing_prolong_all),
										true);
								dialog.show();
								pt = new ProlongAllTask();
								pt.execute(app);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			refresh();
		} else if (item.getItemId() == R.id.action_prolong_all) {
			prolongAll();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void accountSelected(Account account) {

		view.findViewById(R.id.svAccount).setVisibility(View.GONE);
		view.findViewById(R.id.unsupported_error).setVisibility(View.GONE);
		view.findViewById(R.id.answer_error).setVisibility(View.GONE);
		((FrameLayout) view.findViewById(R.id.error_view)).removeAllViews();
		view.findViewById(R.id.llLoading).setVisibility(View.VISIBLE);
		
		refreshing = false;
		getActivity().supportInvalidateOptionsMenu();

		account = app.getAccount();
		if (!app.getApi().isAccountSupported(app.getLibrary())
				&& (app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_EXTENDABLE) == 0) {
			// Not supported with this api at all
			view.findViewById(R.id.llLoading).setVisibility(View.GONE);
			view.findViewById(R.id.unsupported_error).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.tvErrBody))
					.setText(R.string.account_unsupported_api);
			((Button) view.findViewById(R.id.btSend)).setText(R.string.write_mail);
			((Button) view.findViewById(R.id.btSend))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent emailIntent = new Intent(
									android.content.Intent.ACTION_SEND);
							emailIntent.putExtra(
									android.content.Intent.EXTRA_EMAIL,
									new String[] { "info@opacapp.de" });
							emailIntent
									.putExtra(
											android.content.Intent.EXTRA_SUBJECT,
											"Bibliothek "
													+ app.getLibrary()
															.getIdent());
							emailIntent.putExtra(
									android.content.Intent.EXTRA_TEXT,
									"Ich bin interessiert zu helfen.");
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
			view.findViewById(R.id.llLoading).setVisibility(View.GONE);
			view.findViewById(R.id.answer_error).setVisibility(View.VISIBLE);
			((Button) view.findViewById(R.id.btPrefs))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(getActivity(),
									AccountEditActivity.class);
							intent.putExtra(
									AccountEditActivity.EXTRA_ACCOUNT_ID, app
											.getAccount().getId());
							startActivity(intent);
						}
					});
			((TextView) view.findViewById(R.id.tvErrHead)).setText("");
			((TextView) view.findViewById(R.id.tvErrBody))
					.setText(R.string.status_nouser);

		} else if (!app.getApi().isAccountSupported(app.getLibrary())) {

			// We need help
			view.findViewById(R.id.llLoading).setVisibility(View.GONE);
			view.findViewById(R.id.unsupported_error).setVisibility(View.VISIBLE);

			((TextView) view.findViewById(R.id.tvErrBody))
					.setText(R.string.account_unsupported);
			((Button) view.findViewById(R.id.btSend))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog = ProgressDialog.show(getActivity(),
									"", getString(R.string.report_sending),
									true, true, new OnCancelListener() {
										@Override
										public void onCancel(
												DialogInterface arg0) {
											//TODO: finish();
										}
									});
							dialog.show();
							new SendTask().execute(this);
						}
					});

		} else {
			// Supported

			AccountDataSource adatasource = new AccountDataSource(getActivity());
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
		
		view.findViewById(R.id.svAccount).setVisibility(View.GONE);
		view.findViewById(R.id.unsupported_error).setVisibility(View.GONE);
		view.findViewById(R.id.answer_error).setVisibility(View.GONE);
		((FrameLayout) view.findViewById(R.id.error_view)).removeAllViews();
		
		getActivity().supportInvalidateOptionsMenu();
		lt = new LoadTask();
		lt.execute(app);
	}

	protected void cancel(final String a) {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || fromcache || age > MAX_CACHE_AGE) {
			Toast.makeText(getActivity(), R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
										getActivity(), "",
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
			Toast.makeText(getActivity(), R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		prolongDo(a);
	}

	protected void download(final String a) {
		if (app.getApi() instanceof EbookServiceApi) {
			dialog = ProgressDialog.show(getActivity(), "",
					getString(R.string.doing_download), true);
			dialog.show();
			dt = new DownloadTask();
			dt.execute(app, a);
		}
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
								getActivity().getPackageManager().getPackageInfo(
										getActivity().getPackageName(), 0).versionName));
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
			Button btSend = (Button) view.findViewById(R.id.btSend);
			btSend.setEnabled(false);
			if (result == 0) {
				Toast toast = Toast.makeText(getActivity(),
						getString(R.string.report_sent), Toast.LENGTH_SHORT);
				toast.show();
			} else {
				Toast toast = Toast.makeText(getActivity(),
						getString(R.string.report_error), Toast.LENGTH_SHORT);
				toast.show();
			}

		}
	}

	public void invalidateData() {
		AccountDataSource adatasource = new AccountDataSource(getActivity());
		adatasource.open();
		adatasource.invalidateCachedAccountData(account);
		adatasource.close();
		accountSelected(account);
	}

	public void prolong_done(int result) {
		if (result == STATUS_SUCCESS) {
			invalidateData();
		} else if (result == STATUS_FAILED) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
				AccountData res = app.getApi().account(app.getAccount());
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
				getActivity().supportInvalidateOptionsMenu();

				show_connectivity_error(exception);
			}
		}
	}

	public void show_connectivity_error(Exception e) {
		final FrameLayout errorView = (FrameLayout) getView().findViewById(R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(R.layout.error_connectivity, errorView);
		
		if (e != null && e instanceof NotReachableException)
			((TextView) connError.findViewById(R.id.tvErrBody))
					.setText(R.string.connection_error_detail_nre);
		((Button) connError.findViewById(R.id.btRetry))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						refresh();
					}
				});
		view.findViewById(R.id.llLoading).setVisibility(View.GONE);
		view.findViewById(R.id.svAccount).setVisibility(View.GONE);
		connError.setVisibility(View.VISIBLE);			
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		view.findViewById(R.id.llLoading).setVisibility(View.GONE);
		view.findViewById(R.id.answer_error).setVisibility(View.VISIBLE);
		((Button) view.findViewById(R.id.btPrefs))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								AccountEditActivity.class);
						intent.putExtra(AccountEditActivity.EXTRA_ACCOUNT_ID,
								account.getId());
						startActivity(intent);
					}
				});
		((TextView) view.findViewById(R.id.tvErrBody)).setText(s);
	}

	public void loaded(final AccountData result) {

		if (result == null) {
			refreshing = false;
			getActivity().supportInvalidateOptionsMenu();

			if (app.getApi().getLast_error() == null
					|| app.getApi().getLast_error().equals("")) {
				show_connectivity_error(null);
			} else {
				AccountDataSource adatasource = new AccountDataSource(getActivity());
				adatasource.open();
				adatasource.invalidateCachedAccountData(account);
				adatasource.close();
				dialog_wrong_credentials(app.getApi().getLast_error(), true);
			}
			return;
		}

		AccountDataSource adatasource = new AccountDataSource(getActivity());
		adatasource.open();
		adatasource.storeCachedAccountData(
				adatasource.getAccount(result.getAccount()), result);
		adatasource.close();

		if (result.getAccount() == account.getId()) {
			// The account this data is for is still visible

			refreshing = false;
			getActivity().supportInvalidateOptionsMenu();

			refreshtime = System.currentTimeMillis();

			displaydata(result, false);
		}

	}
	

	public void displaydata(AccountData result, boolean fromcache) {
		view.findViewById(R.id.svAccount).setVisibility(View.VISIBLE);
		view.findViewById(R.id.llLoading).setVisibility(View.GONE);

		this.fromcache = fromcache;

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final long tolerance = Long.decode(sp.getString("notification_warning",
				"367200000"));

		((TextView) view.findViewById(R.id.tvAccLabel)).setText(account.getLabel());
		((TextView) view.findViewById(R.id.tvAccUser)).setText(account.getName());
		TextView tvAccCity = (TextView) view.findViewById(R.id.tvAccCity);
		Library lib;
		try {
			lib = app.getLibrary(account
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

		LinearLayout llLent = (LinearLayout) view.findViewById(R.id.llLent);
		llLent.removeAllViews();

		boolean notification_on = sp.getBoolean("notification_service", false);
		boolean notification_problems = false;

		if (result.getLent().size() == 0) {
			TextView t1 = new TextView(getActivity());
			t1.setText(R.string.entl_none);
			llLent.addView(t1);
		} else {
			for (ContentValues item : result.getLent()) {
				View v = getLayoutInflater().inflate(R.layout.listitem_account_lent,
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
						&& !"".equals(item
								.containsKey(AccountData.KEY_LENT_STATUS))
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
				if (item.containsKey(AccountData.KEY_LENT_FORMAT)) {
					((TextView) v.findViewById(R.id.tvFmt)).setText(Html
							.fromHtml(item
									.getAsString(AccountData.KEY_LENT_FORMAT)));
					((TextView) v.findViewById(R.id.tvFmt))
							.setVisibility(View.VISIBLE);
				} else {
					((TextView) v.findViewById(R.id.tvFmt))
							.setVisibility(View.GONE);
				}

				try {
					if (notification_on
							&& item.containsKey(AccountData.KEY_LENT_DEADLINE)) {
						if (!item.getAsString(AccountData.KEY_LENT_DEADLINE)
								.equals("")) {
							if ((!item
									.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP) || item
									.getAsLong(AccountData.KEY_LENT_DEADLINE_TIMESTAMP) < 1)
									&& !"Onleihe"
											.equals(item
													.getAsString(AccountData.KEY_LENT_BRANCH))) {
								notification_problems = true;
							}
						}
					}
				} catch (Exception e) {
					notification_problems = true;
				}

				// Color codes for return dates
				if (item.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) {
					if (item.getAsLong(AccountData.KEY_LENT_DEADLINE_TIMESTAMP) < System
							.currentTimeMillis()) {
						v.findViewById(R.id.vStatusColor).setBackgroundColor(
								getResources().getColor(R.color.date_overdue));
					} else if ((item
							.getAsLong(AccountData.KEY_LENT_DEADLINE_TIMESTAMP) - System
							.currentTimeMillis()) <= tolerance) {
						v.findViewById(R.id.vStatusColor).setBackgroundColor(
								getResources().getColor(R.color.date_warning));
					} else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
						v.findViewById(R.id.vStatusColor).setBackgroundColor(
								getResources().getColor(R.color.account_downloadable));
					}
				} else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
					v.findViewById(R.id.vStatusColor).setBackgroundColor(
							getResources().getColor(R.color.account_downloadable));
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
				} else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)
						&& app.getApi() instanceof EbookServiceApi) {
					v.findViewById(R.id.ivDownload).setTag(
							item.getAsString(AccountData.KEY_LENT_DOWNLOAD));
					((ImageView) v.findViewById(R.id.ivDownload))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									download((String) arg0.getTag());
								}
							});
					((ImageView) v.findViewById(R.id.ivProlong))
							.setVisibility(View.GONE);
					((ImageView) v.findViewById(R.id.ivDownload))
							.setVisibility(View.VISIBLE);
				} else {
					((ImageView) v.findViewById(R.id.ivProlong))
							.setVisibility(View.INVISIBLE);
				}

				llLent.addView(v);
			}
		}

		if (notification_problems) {
			View tvError = view.findViewById(R.id.tvError);
			if (tvError != null) {
				tvError.setVisibility(View.VISIBLE);
				((TextView) tvError).setText(R.string.notification_problems);
			}
		}

		LinearLayout llRes = (LinearLayout) view.findViewById(R.id.llReservations);
		llRes.removeAllViews();

		if (result.getReservations().size() == 0) {
			TextView t1 = new TextView(getActivity());
			t1.setText(R.string.reservations_none);
			llRes.addView(t1);
		} else {
			for (ContentValues item : result.getReservations()) {
				View v = getLayoutInflater().inflate(
						R.layout.listitem_account_reservation, null);

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
				} else if (item.containsKey(AccountData.KEY_RESERVATION_EXPIRE)
						&& item.getAsString(AccountData.KEY_RESERVATION_EXPIRE)
								.length() > 6) {
					((TextView) v.findViewById(R.id.tvStatus))
							.setText(Html.fromHtml("bis "
									+ item.getAsString(AccountData.KEY_RESERVATION_EXPIRE)));
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

				if (item.containsKey(AccountData.KEY_RESERVATION_BOOKING)) {
					v.findViewById(R.id.ivBooking)
							.setTag(item
									.getAsString(AccountData.KEY_RESERVATION_BOOKING));
					((ImageView) v.findViewById(R.id.ivBooking))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									bookingStart((String) arg0.getTag());
								}
							});
					((ImageView) v.findViewById(R.id.ivBooking))
							.setVisibility(View.VISIBLE);
					((ImageView) v.findViewById(R.id.ivCancel))
							.setVisibility(View.GONE);
				} else if (item.containsKey(AccountData.KEY_RESERVATION_CANCEL)) {
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
					((ImageView) v.findViewById(R.id.ivBooking))
							.setVisibility(View.GONE);
				} else {
					((ImageView) v.findViewById(R.id.ivCancel))
							.setVisibility(View.INVISIBLE);
					((ImageView) v.findViewById(R.id.ivBooking))
							.setVisibility(View.GONE);
				}
				llRes.addView(v);
			}
		}

		if (result.getPendingFees() != null) {
			view.findViewById(R.id.tvPendingFeesLabel).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvPendingFees).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.tvPendingFees)).setText(result
					.getPendingFees());
		} else {
			view.findViewById(R.id.tvPendingFeesLabel).setVisibility(View.GONE);
			view.findViewById(R.id.tvPendingFees).setVisibility(View.GONE);
		}
		if (result.getValidUntil() != null) {
			view.findViewById(R.id.tvValidUntilLabel).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvValidUntil).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.tvValidUntil)).setText(result
					.getValidUntil());
		} else {
			view.findViewById(R.id.tvValidUntilLabel).setVisibility(View.GONE);
			view.findViewById(R.id.tvValidUntil).setVisibility(View.GONE);
		}
		refreshage();
	}

	public void refreshage() {
		try {
			if (view.findViewById(R.id.tvAge) == null)
				return;

			long age = System.currentTimeMillis() - refreshtime;
			if (age < (3600 * 1000)) {
				((TextView) view.findViewById(R.id.tvAge))
						.setText(getResources().getQuantityString(
								R.plurals.account_age_minutes,
								(int) (age / (60 * 1000)),
								(int) (age / (60 * 1000))));
			} else if (age < 24 * 3600 * 1000) {
				((TextView) view.findViewById(R.id.tvAge))
						.setText(getResources().getQuantityString(
								R.plurals.account_age_hours,
								(int) (age / (3600 * 1000)),
								(int) (age / (3600 * 1000))));

			} else {
				((TextView) view.findViewById(R.id.tvAge))
						.setText(getResources().getQuantityString(
								R.plurals.account_age_days,
								(int) (age / (24 * 3600 * 1000)),
								(int) (age / (24 * 3600 * 1000))));
			}
		} catch (java.lang.IllegalStateException e) {
			// as this is called from a handler it may be called
			// without an activity attached to this fragment
			// we do nothing about it
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
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

	public class DownloadTask extends OpacTask<Uri> {
		private boolean success = true;

		@Override
		protected Uri doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			String a = (String) arg0[1];
			Uri url = ((EbookServiceApi) app.getApi()).downloadItem(account, a);
			return url;
		}

		@Override
		protected void onPostExecute(final Uri result) {
			dialog.dismiss();
			if (result.toString().contains("acsm")) {
				String[] download_clients = new String[] {
						"com.android.aldiko", "com.aldiko.android",
						"com.bluefirereader",
						"com.mantano.reader.android.lite",
						"com.datalogics.dlreader",
						"com.mantano.reader.android.normal",
						"com.mantano.reader.android", "com.neosoar" };
				boolean found = false;
				PackageManager pm = getActivity().getPackageManager();
				for (String id : download_clients) {
					try {
						pm.getPackageInfo(id, 0);
						found = true;
					} catch (NameNotFoundException e) {
					}
				}
				final SharedPreferences sp = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				if (!found && !sp.contains("reader_needed_ignore")) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setMessage(R.string.reader_needed)
							.setCancelable(true)
							.setNegativeButton(R.string.reader_needed_cancel,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									})
							.setNeutralButton(R.string.reader_needed_ignore,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											Intent i = new Intent(
													Intent.ACTION_VIEW);
											i.setData(result);
											sp.edit()
													.putBoolean(
															"reader_needed_ignore",
															true).commit();
											startActivity(i);
										}
									})
							.setPositiveButton(R.string.reader_needed_download,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
											Intent i = new Intent(
													Intent.ACTION_VIEW,
													Uri.parse("market://details?id=de.bluefirereader"));
											startActivity(i);
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
					return;
				}
			}
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(result);
			startActivity(i);
		}
	}

	public class ProlongTask extends OpacTask<ProlongResult> {
		private boolean success = true;
		private String media;

		@Override
		protected ProlongResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			media = (String) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				ProlongResult res = app.getApi().prolong(media, account,
						useraction, selection);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(ProlongResult res) {
			dialog.dismiss();

			if (!success || res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
				return;
			}

			prolongResult(media, res);
		}
	}

	public class ProlongAllTask extends OpacTask<Integer> {
		private boolean success = true;

		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				boolean res = app.getApi().prolongAll(account);
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
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

	public void bookingStart(String booking_info) {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || fromcache || age > MAX_CACHE_AGE) {
			Toast.makeText(getActivity(), R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		bookingDo(booking_info);
	}

	public void bookingDo(String booking_info) {
		bookingDo(booking_info, 0, null);
	}

	public void bookingDo(String booking_info, int useraction, String selection) {
		if (dialog == null) {
			dialog = ProgressDialog.show(getActivity(), "",
					getString(R.string.doing_booking), true);
			dialog.show();
		} else if (!dialog.isShowing()) {
			dialog = ProgressDialog.show(getActivity(), "",
					getString(R.string.doing_booking), true);
			dialog.show();
		}

		bt = new BookingTask();
		bt.execute(app, booking_info, useraction, selection);
	}

	public void bookingResult(String booking_info, BookingResult result) {
		AccountDataSource adata = new AccountDataSource(getActivity());
		adata.open();
		adata.invalidateCachedAccountData(app.getAccount());
		adata.close();
		switch (result.getStatus()) {
		case CONFIRMATION_NEEDED:
			bookingConfirmation(booking_info, result);
			break;
		case SELECTION_NEEDED:
			bookingSelection(booking_info, result);
			break;
		case ERROR:
			dialog_wrong_credentials(result.getMessage(), false);
			break;
		case OK:
			invalidateData();
			break;
		case UNSUPPORTED:
			// TODO: Show dialog
			break;
		default:
			break;
		}
	}

	public void bookingConfirmation(final String booking_info,
			final BookingResult result) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_reservation_details, null);

		TableLayout table = (TableLayout) view.findViewById(R.id.tlDetails);

		if (result.getDetails().size() == 1
				&& result.getDetails().get(0).length == 1) {
			((RelativeLayout) view.findViewById(R.id.rlConfirm))
					.removeView(table);
			TextView tv = new TextView(getActivity());
			tv.setText(result.getDetails().get(0)[0]);
			tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			((RelativeLayout) view.findViewById(R.id.rlConfirm)).addView(tv);
		} else {
			for (String[] detail : result.getDetails()) {
				TableRow tr = new TableRow(getActivity());
				if (detail.length == 2) {
					TextView tv1 = new TextView(getActivity());
					tv1.setText(Html.fromHtml(detail[0]));
					tv1.setTypeface(null, Typeface.BOLD);
					tv1.setPadding(0, 0, 8, 0);
					TextView tv2 = new TextView(getActivity());
					tv2.setText(Html.fromHtml(detail[1]));
					tv2.setEllipsize(TruncateAt.END);
					tv2.setSingleLine(false);
					tr.addView(tv1);
					tr.addView(tv2);
				} else if (detail.length == 1) {
					TextView tv1 = new TextView(getActivity());
					tv1.setText(Html.fromHtml(detail[0]));
					tv1.setPadding(0, 2, 0, 2);
					TableRow.LayoutParams params = new TableRow.LayoutParams(0);
					params.span = 2;
					tv1.setLayoutParams(params);
					tr.addView(tv1);
				}
				table.addView(tr);
			}
		}

		builder.setTitle(R.string.confirm_title)
				.setView(view)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								bookingDo(booking_info,
										MultiStepResult.ACTION_CONFIRMATION,
										"confirmed");
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						});
		adialog = builder.create();
		adialog.show();
	}

	public void bookingSelection(final String booking_info,
			final BookingResult result) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_simple_list, null);

		ListView lv = (ListView) view.findViewById(R.id.lvBibs);
		final Object[] possibilities = result.getSelection().valueSet()
				.toArray();

		lv.setAdapter(new SelectionAdapter(getActivity(), possibilities));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				adialog.dismiss();
				bookingDo(booking_info, result.getActionIdentifier(),
						((Entry<String, Object>) possibilities[position])
								.getKey());
			}
		});
		switch (result.getActionIdentifier()) {
		case ReservationResult.ACTION_BRANCH:
			builder.setTitle(R.string.zweigstelle);
		}
		builder.setView(view).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						adialog.cancel();
					}
				});
		adialog = builder.create();
		adialog.show();
	}

	public void prolongStart(String media) {
		long age = System.currentTimeMillis() - refreshtime;
		if (refreshing || fromcache || age > MAX_CACHE_AGE) {
			Toast.makeText(getActivity(), R.string.account_no_concurrent,
					Toast.LENGTH_LONG).show();
			if (!refreshing) {
				refresh();
			}
			return;
		}
		prolongDo(media);
	}

	public void prolongDo(String media) {
		prolongDo(media, 0, null);
	}

	public void prolongDo(String media, int useraction, String selection) {
		if (dialog == null) {
			dialog = ProgressDialog.show(getActivity(), "",
					getString(R.string.doing_prolong), true);
			dialog.show();
		} else if (!dialog.isShowing()) {
			dialog = ProgressDialog.show(getActivity(), "",
					getString(R.string.doing_prolong), true);
			dialog.show();
		}

		pt = new ProlongTask();
		pt.execute(app, media, useraction, selection);
	}

	public void prolongResult(String media, ProlongResult result) {
		AccountDataSource adata = new AccountDataSource(getActivity());
		adata.open();
		adata.invalidateCachedAccountData(app.getAccount());
		adata.close();
		switch (result.getStatus()) {
		case CONFIRMATION_NEEDED:
			prolongConfirmation(media, result);
			break;
		case SELECTION_NEEDED:
			prolongSelection(media, result);
			break;
		case ERROR:
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(result.getMessage())
					.setCancelable(true)
					.setNegativeButton(R.string.dismiss,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface d, int id) {
									d.cancel();
								}
							})
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface d) {
									if (d != null)
										d.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			break;
		case OK:
			invalidateData();
			break;
		case UNSUPPORTED:
			// TODO: Show dialog
			break;
		default:
			break;
		}
	}

	public void prolongConfirmation(final String media,
			final ProlongResult result) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_reservation_details, null);

		TableLayout table = (TableLayout) view.findViewById(R.id.tlDetails);

		if (result.getDetails().size() == 1
				&& result.getDetails().get(0).length == 1) {
			((RelativeLayout) view.findViewById(R.id.rlConfirm))
					.removeView(table);
			TextView tv = new TextView(getActivity());
			tv.setText(result.getDetails().get(0)[0]);
			tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			((RelativeLayout) view.findViewById(R.id.rlConfirm)).addView(tv);
		} else {
			for (String[] detail : result.getDetails()) {
				TableRow tr = new TableRow(getActivity());
				if (detail.length == 2) {
					TextView tv1 = new TextView(getActivity());
					tv1.setText(Html.fromHtml(detail[0]));
					tv1.setTypeface(null, Typeface.BOLD);
					tv1.setPadding(0, 0, 8, 0);
					TextView tv2 = new TextView(getActivity());
					tv2.setText(Html.fromHtml(detail[1]));
					tv2.setEllipsize(TruncateAt.END);
					tv2.setSingleLine(false);
					tr.addView(tv1);
					tr.addView(tv2);
				} else if (detail.length == 1) {
					TextView tv1 = new TextView(getActivity());
					tv1.setText(Html.fromHtml(detail[0]));
					tv1.setPadding(0, 2, 0, 2);
					TableRow.LayoutParams params = new TableRow.LayoutParams(0);
					params.span = 2;
					tv1.setLayoutParams(params);
					tr.addView(tv1);
				}
				table.addView(tr);
			}
		}

		builder.setTitle(R.string.confirm_title)
				.setView(view)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								prolongDo(media,
										MultiStepResult.ACTION_CONFIRMATION,
										"confirmed");
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						});
		adialog = builder.create();
		adialog.show();
	}

	public void prolongSelection(final String media, final ProlongResult result) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_simple_list, null);

		ListView lv = (ListView) view.findViewById(R.id.lvBibs);
		final Object[] possibilities = result.getSelection().valueSet()
				.toArray();

		lv.setAdapter(new SelectionAdapter(getActivity(), possibilities));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				adialog.dismiss();

				prolongDo(media, result.getActionIdentifier(),
						((Entry<String, Object>) possibilities[position])
								.getKey());
			}
		});
		switch (result.getActionIdentifier()) {
		case ReservationResult.ACTION_BRANCH:
			builder.setTitle(R.string.zweigstelle);
		}
		builder.setView(view).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						adialog.cancel();
					}
				});
		adialog = builder.create();
		adialog.show();
	}

	public class SelectionAdapter extends ArrayAdapter<Object> {

		private Object[] objects;

		@Override
		public View getView(int position, View contentView, ViewGroup viewGroup) {
			View view = null;

			if (objects[position] == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.listitem_branch, viewGroup,
						false);
				return view;
			}

			String item = ((Entry<String, Object>) objects[position])
					.getValue().toString();

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.listitem_branch, viewGroup,
						false);
			} else {
				view = contentView;
			}

			TextView tvText = (TextView) view.findViewById(android.R.id.text1);
			tvText.setText(item);
			return view;
		}

		public SelectionAdapter(Context context, Object[] objects) {
			super(context, R.layout.simple_spinner_item, objects);
			this.objects = objects;
		}

	}

	public class BookingTask extends OpacTask<BookingResult> {
		private boolean success;
		private String booking_info;

		@Override
		protected BookingResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			booking_info = (String) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				BookingResult res = ((EbookServiceApi) app.getApi()).booking(
						booking_info, app.getAccount(), useraction, selection);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(BookingResult res) {
			dialog.dismiss();

			if (!success || res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
				return;
			}

			bookingResult(booking_info, res);
		}
	}

	@Override
	public void onStop() {
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
			if (dt != null) {
				if (!dt.isCancelled()) {
					dt.cancel(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
