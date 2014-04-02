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
import java.util.Set;

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
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.EbookServiceApi;
import de.geeksfactory.opacclient.apis.EbookServiceApi.BookingResult;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.CancelResult;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.apis.OpacApi.ProlongAllResult;
import de.geeksfactory.opacclient.apis.OpacApi.ProlongResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.Callback;
import de.geeksfactory.opacclient.frontend.MultiStepResultHelper.StepTask;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountFragment extends Fragment implements
		AccountSelectedListener {

	protected ProgressDialog dialog;
	protected AlertDialog adialog;
	protected OpacClient app;

	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_NOUSER = 1;
	public static final int STATUS_FAILED = 2;

	public static final long MAX_CACHE_AGE = (1000 * 3600 * 2);

	private LoadTask lt;
	private CancelTask ct;
	private OpacTask<Uri> dt;
	private BookingTask bt;

	private Account account;

	private boolean refreshing = false;
	private long refreshtime;
	private boolean fromcache;
	protected View view;
	private boolean supported = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = inflater.inflate(R.layout.fragment_account, container, false);
		app = (OpacClient) getActivity().getApplication();
		account = app.getAccount();

		if (getActivity().getIntent().getExtras() != null) {
			if (getActivity().getIntent().getExtras()
					.containsKey("notifications")) {
				AccountDataSource adata = new AccountDataSource(getActivity());
				adata.open();
				Bundle notif = getActivity().getIntent().getExtras()
						.getBundle("notifications");
				Set<String> keys = notif.keySet();
				for (String key : keys) {
					long[] val = notif.getLongArray(key);
					adata.notificationSave(val[0], val[1]);
				}
				adata.close();

				if (getActivity().getIntent().getExtras().getLong("account") != app
						.getAccount().getId()) {
					app.setAccount(getActivity().getIntent().getExtras()
							.getLong("account"));
					((OpacActivity) getActivity()).accountSelected(app
							.getAccount());
				}
				NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nMgr.cancel(OpacClient.NOTIF_ID);
			}
		}

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
				getSupportActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
			} else {
				menu.findItem(R.id.action_refresh).setVisible(false);
				getSupportActivity()
						.setSupportProgressBarIndeterminateVisibility(true);
			}
		} else {
			if (Build.VERSION.SDK_INT >= 14) {
				menu.findItem(R.id.action_refresh).setActionView(null);
				getSupportActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
			} else {
				menu.findItem(R.id.action_refresh).setVisible(true);
				getSupportActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
			}
		}
		if ((app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL) != 0) {
			menu.findItem(R.id.action_prolong_all).setVisible(true);
		} else {
			menu.findItem(R.id.action_prolong_all).setVisible(false);
		}
		menu.findItem(R.id.action_refresh).setVisible(supported);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			refresh();
		} else if (item.getItemId() == R.id.action_prolong_all) {
			prolongAllStart();
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
		supported = true;

		this.account = app.getAccount();
		if (!app.getApi().isAccountSupported(app.getLibrary())
				&& (app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_EXTENDABLE) == 0) {
			supported = false;
			// Not supported with this api at all
			view.findViewById(R.id.llLoading).setVisibility(View.GONE);
			view.findViewById(R.id.unsupported_error).setVisibility(
					View.VISIBLE);
			((TextView) view.findViewById(R.id.tvErrBodyU))
					.setText(R.string.account_unsupported_api);
			((Button) view.findViewById(R.id.btSend))
					.setText(R.string.write_mail);
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

		} else if (!app.getApi().isAccountSupported(app.getLibrary())) {
			supported = false;

			// We need help
			view.findViewById(R.id.llLoading).setVisibility(View.GONE);
			view.findViewById(R.id.unsupported_error).setVisibility(
					View.VISIBLE);

			((TextView) view.findViewById(R.id.tvErrBodyU))
					.setText(R.string.account_unsupported);
			((Button) view.findViewById(R.id.btSend))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog = ProgressDialog.show(getActivity(), "",
									getString(R.string.report_sending), true,
									true, new OnCancelListener() {
										@Override
										public void onCancel(
												DialogInterface arg0) {
										}
									});
							dialog.show();
							new SendTask().execute(this);
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
			((TextView) view.findViewById(R.id.tvErrHeadA)).setText("");
			((TextView) view.findViewById(R.id.tvErrBodyA))
					.setText(R.string.status_nouser);

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

		getActivity().supportInvalidateOptionsMenu();
	}

	public void refresh() {

		if ((!app.getApi().isAccountSupported(app.getLibrary()) && (app
				.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ACCOUNT_EXTENDABLE) == 0)
				|| !app.getApi().isAccountSupported(app.getLibrary())
				|| account.getPassword() == null
				|| account.getPassword().equals("null")
				|| account.getPassword().equals("")
				|| account.getName() == null
				|| account.getName().equals("null")
				|| account.getName().equals("")) {
			return;
		}

		refreshing = true;

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

								MultiStepResultHelper msrhCancel = new MultiStepResultHelper(
										getSupportActivity(), a,
										R.string.doing_cancel);
								msrhCancel.setCallback(new Callback() {
									@Override
									public void onSuccess(MultiStepResult result) {
										invalidateData();
									}

									@Override
									public void onError(MultiStepResult result) {
										AlertDialog.Builder builder = new AlertDialog.Builder(
												getActivity());
										builder.setMessage(result.getMessage())
												.setCancelable(true)
												.setNegativeButton(
														R.string.dismiss,
														new DialogInterface.OnClickListener() {
															@Override
															public void onClick(
																	DialogInterface d,
																	int id) {
																d.cancel();
															}
														})
												.setOnCancelListener(
														new DialogInterface.OnCancelListener() {
															@Override
															public void onCancel(
																	DialogInterface d) {
																if (d != null)
																	d.cancel();
															}
														});
										AlertDialog alert = builder.create();
										alert.show();
									}

									@Override
									public void onUnhandledResult(
											MultiStepResult result) {
									}

									@Override
									public void onUserCancel() {
									}

									@Override
									public StepTask<?> newTask() {
										return new CancelTask();
									}
								});
								msrhCancel.start();
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

		MultiStepResultHelper msrhProlong = new MultiStepResultHelper(
				getSupportActivity(), a, R.string.doing_prolong);
		msrhProlong.setCallback(new Callback() {
			@Override
			public void onSuccess(MultiStepResult result) {
				invalidateData();

				if (result.getMessage() != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
					builder.setMessage(result.getMessage())
							.setCancelable(false)
							.setNegativeButton(R.string.dismiss,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}

			@Override
			public void onError(MultiStepResult result) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(result.getMessage())
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d,
											int id) {
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
			}

			@Override
			public void onUnhandledResult(MultiStepResult result) {
			}

			@Override
			public void onUserCancel() {
			}

			@Override
			public StepTask<?> newTask() {
				return new ProlongTask();
			}
		});
		msrhProlong.start();
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
								getActivity().getPackageManager()
										.getPackageInfo(
												getActivity().getPackageName(),
												0).versionName));
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
		view.findViewById(R.id.svAccount).setVisibility(View.GONE);
		accountSelected(account);
	}

	public class LoadTask extends OpacTask<AccountData> {

		private Exception exception;

		@Override
		protected AccountData doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				AccountData res = app.getApi().account(app.getAccount());
				return res;
			} catch (java.net.UnknownHostException e) {
				exception = e;
			} catch (java.net.SocketException e) {
				exception = e;
			} catch (OpacErrorException e) {
				exception = e;
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(AccountData result) {
			if (exception == null) {
				loaded(result);
			} else {
				refreshing = false;
				getActivity().supportInvalidateOptionsMenu();

				show_connectivity_error(exception);
			}
		}
	}

	public void show_connectivity_error(Exception e) {
		e.printStackTrace();
		if (e instanceof OpacErrorException) {
			AccountDataSource adatasource = new AccountDataSource(getActivity());
			adatasource.open();
			adatasource.invalidateCachedAccountData(account);
			adatasource.close();
			dialog_wrong_credentials(e.getMessage(), true);
			return;
		}
		final FrameLayout errorView = (FrameLayout) getView().findViewById(
				R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(
				R.layout.error_connectivity, errorView);

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
		((TextView) view.findViewById(R.id.tvErrBodyA)).setText(s);
	}

	public void loaded(final AccountData result) {

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

	@SuppressWarnings("deprecation")
	public void displaydata(AccountData result, boolean fromcache) {
		view.findViewById(R.id.svAccount).setVisibility(View.VISIBLE);
		view.findViewById(R.id.llLoading).setVisibility(View.GONE);
		view.findViewById(R.id.unsupported_error).setVisibility(View.GONE);
		view.findViewById(R.id.answer_error).setVisibility(View.GONE);
		((FrameLayout) view.findViewById(R.id.error_view)).removeAllViews();

		this.fromcache = fromcache;

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final long tolerance = Long.decode(sp.getString("notification_warning",
				"367200000"));

		((TextView) view.findViewById(R.id.tvAccLabel)).setText(account
				.getLabel());
		((TextView) view.findViewById(R.id.tvAccUser)).setText(account
				.getName());
		TextView tvAccCity = (TextView) view.findViewById(R.id.tvAccCity);
		Library lib;
		try {
			lib = app.getLibrary(account.getLibrary());
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
			for (final ContentValues item : result.getLent()) {
				View v = getLayoutInflater().inflate(
						R.layout.listitem_account_lent, null);

				if (item.containsKey(AccountData.KEY_LENT_ID)) {
					View.OnClickListener gotoDetails = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(getActivity(),
									SearchResultDetailActivity.class);
							intent.putExtra(
									SearchResultDetailFragment.ARG_ITEM_ID,
									item.getAsString(AccountData.KEY_LENT_ID));
							startActivity(intent);
						}
					};
					v.findViewById(R.id.tvTitel)
							.setOnClickListener(gotoDetails);
					v.findViewById(R.id.tvVerfasser).setOnClickListener(
							gotoDetails);
					v.findViewById(R.id.tvStatus).setOnClickListener(
							gotoDetails);
				}

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
								getResources().getColor(
										R.color.account_downloadable));
					}
				} else if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
					v.findViewById(R.id.vStatusColor).setBackgroundColor(
							getResources().getColor(
									R.color.account_downloadable));
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
					if (item.containsKey(AccountData.KEY_LENT_RENEWABLE)) {
						((ImageView) v.findViewById(R.id.ivProlong))
								.setAlpha(item.getAsString(
										AccountData.KEY_LENT_RENEWABLE).equals(
										"Y") ? 255 : 100);
					}
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

		LinearLayout llRes = (LinearLayout) view
				.findViewById(R.id.llReservations);
		llRes.removeAllViews();

		if (result.getReservations().size() == 0) {
			TextView t1 = new TextView(getActivity());
			t1.setText(R.string.reservations_none);
			llRes.addView(t1);
		} else {
			for (final ContentValues item : result.getReservations()) {
				View v = getLayoutInflater().inflate(
						R.layout.listitem_account_reservation, null);

				if (item.containsKey(AccountData.KEY_RESERVATION_ID)) {
					View.OnClickListener gotoDetails = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(getActivity(),
									SearchResultDetailActivity.class);
							intent.putExtra(
									SearchResultDetailFragment.ARG_ITEM_ID,
									item.getAsString(AccountData.KEY_RESERVATION_ID));
							startActivity(intent);
						}
					};
					v.findViewById(R.id.tvTitel)
							.setOnClickListener(gotoDetails);
					v.findViewById(R.id.tvVerfasser).setOnClickListener(
							gotoDetails);
					v.findViewById(R.id.tvStatus).setOnClickListener(
							gotoDetails);
				}

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
			view.findViewById(R.id.tvPendingFeesLabel).setVisibility(
					View.VISIBLE);
			view.findViewById(R.id.tvPendingFees).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.tvPendingFees)).setText(result
					.getPendingFees());
		} else {
			view.findViewById(R.id.tvPendingFeesLabel).setVisibility(View.GONE);
			view.findViewById(R.id.tvPendingFees).setVisibility(View.GONE);
		}
		if (result.getValidUntil() != null) {
			view.findViewById(R.id.tvValidUntilLabel).setVisibility(
					View.VISIBLE);
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

	public class CancelTask extends StepTask<CancelResult> {

		@Override
		protected CancelResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			String a = (String) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];
			try {
				return app.getApi().cancel(a, account, useraction, selection);
			} catch (java.net.UnknownHostException e) {
				e.printStackTrace();
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(CancelResult result) {
			if (getActivity() == null)
				return;

			if (result == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
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

			super.onPostExecute(result);
		}
	}

	public class DownloadTask extends OpacTask<Uri> {

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
			if (getActivity() == null)
				return;
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
					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
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

	public class ProlongTask extends
			MultiStepResultHelper.StepTask<ProlongResult> {
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
			if (getActivity() == null)
				return;

			if (!success || res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
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

			super.onPostExecute(res);
		}
	}

	public class ProlongAllTask extends
			MultiStepResultHelper.StepTask<ProlongAllResult> {

		@Override
		protected ProlongAllResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				ProlongAllResult res = app.getApi().prolongAll(account,
						useraction, selection);
				return res;
			} catch (java.net.UnknownHostException e) {
			} catch (java.net.SocketException e) {
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(ProlongAllResult result) {
			if (getActivity() == null)
				return;

			if (result == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
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

			super.onPostExecute(result);
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
		DetailledItem item = new DetailledItem();
		item.setBookable(true);
		item.setBooking_info(booking_info);
		MultiStepResultHelper msrhBooking = new MultiStepResultHelper(
				getSupportActivity(), item, R.string.doing_booking);
		msrhBooking.setCallback(new Callback() {
			@Override
			public void onSuccess(MultiStepResult result) {
				invalidateData();
			}

			@Override
			public void onError(MultiStepResult result) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(result.getMessage())
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d,
											int id) {
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
			}

			@Override
			public void onUnhandledResult(MultiStepResult result) {
			}

			@Override
			public void onUserCancel() {
			}

			@Override
			public StepTask<?> newTask() {
				return new BookingTask();
			}
		});
		msrhBooking.start();
	}

	public void prolongAllStart() {
		if (refreshing) {
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
								prolongAllDo();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();

	}

	public void prolongAllDo() {

		MultiStepResultHelper msrhProlong = new MultiStepResultHelper(
				getSupportActivity(), null, R.string.doing_prolong_all);
		msrhProlong.setCallback(new Callback() {
			@Override
			public void onSuccess(MultiStepResult result) {
				ProlongAllResult res = (ProlongAllResult) result;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());

				LayoutInflater inflater = getLayoutInflater();

				View view = inflater.inflate(R.layout.dialog_simple_list, null);

				ListView lv = (ListView) view.findViewById(R.id.lvBibs);

				lv.setAdapter(new ProlongAllResultAdapter(getActivity(), res
						.getResults().toArray()));
				switch (result.getActionIdentifier()) {
				case ReservationResult.ACTION_BRANCH:
					builder.setTitle(R.string.zweigstelle);
				}
				builder.setView(view).setNeutralButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
								invalidateData();
							}
						});
				adialog = builder.create();
				adialog.show();
			}

			@Override
			public void onError(MultiStepResult result) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(result.getMessage())
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d,
											int id) {
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
			}

			@Override
			public void onUnhandledResult(MultiStepResult result) {
			}

			@Override
			public void onUserCancel() {
			}

			@Override
			public StepTask<?> newTask() {
				return new ProlongAllTask();
			}
		});
		msrhProlong.start();
	}

	public class ProlongAllResultAdapter extends ArrayAdapter<Object> {

		private Object[] objects;

		@Override
		public View getView(int position, View contentView, ViewGroup viewGroup) {
			View view = null;

			if (objects[position] == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(
						R.layout.listitem_prolongall_result, viewGroup, false);
				return view;
			}

			ContentValues item = ((ContentValues) objects[position]);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(
						R.layout.listitem_prolongall_result, viewGroup, false);
			} else {
				view = contentView;
			}

			TextView tvAuthor = (TextView) view.findViewById(R.id.tvAuthor);
			tvAuthor.setVisibility(item
					.containsKey(ProlongAllResult.KEY_LINE_AUTHOR) ? View.VISIBLE
					: View.GONE);
			tvAuthor.setText(item.getAsString(ProlongAllResult.KEY_LINE_AUTHOR));
			TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
			tvTitle.setText(item.getAsString(ProlongAllResult.KEY_LINE_TITLE));
			TextView tvOld = (TextView) view.findViewById(R.id.tvOld);
			tvOld.setText(item
					.getAsString(ProlongAllResult.KEY_LINE_OLD_RETURNDATE));
			TextView tvNew = (TextView) view.findViewById(R.id.tvNew);
			tvNew.setText(item
					.getAsString(ProlongAllResult.KEY_LINE_NEW_RETURNDATE));
			TextView tvMsg = (TextView) view.findViewById(R.id.tvMsg);
			tvMsg.setText(item.getAsString(ProlongAllResult.KEY_LINE_MESSAGE));
			return view;
		}

		public ProlongAllResultAdapter(Context context, Object[] objects) {
			super(context, R.layout.simple_spinner_item, objects);
			this.objects = objects;
		}

	}

	public class BookingTask extends StepTask<BookingResult> {
		private DetailledItem item;

		@Override
		protected BookingResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			item = (DetailledItem) arg0[1];
			int useraction = (Integer) arg0[2];
			String selection = (String) arg0[3];

			try {
				BookingResult res = ((EbookServiceApi) app.getApi()).booking(
						item, app.getAccount(), useraction, selection);
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.net.SocketException e) {
				e.printStackTrace();
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(BookingResult res) {
			if (getActivity() == null)
				return;

			if (res == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
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

			super.onPostExecute(res);
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
