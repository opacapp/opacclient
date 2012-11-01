package de.geeksfactory.opacclient.frontend;

import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;

public class AccountActivity extends OpacActivity {

	protected ProgressDialog dialog;

	public static int STATUS_SUCCESS = 0;
	public static int STATUS_NOUSER = 1;
	public static int STATUS_FAILED = 2;

	protected LoadTask lt;
	protected CancelTask ct;
	protected ProlongTask pt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(app);

		if (getIntent().getExtras() != null) {
			if (getIntent().getExtras().getLong("notif_last") > 0) {
				SharedPreferences.Editor spe = sp.edit();
				spe.putLong("notification_last", getIntent().getExtras()
						.getLong("notif_last"));
				spe.commit();
				NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nMgr.cancel(OpacClient.NOTIF_ID);
			}
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

	@Override
	public void onResume() {
		super.onResume();
		setContentView(R.layout.loading);
		((TextView) findViewById(R.id.tvLoading)).setText(R.string.loading_account);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(app);
		if (sp.getString("opac_usernr", "").equals("")
				|| sp.getString("opac_password", "").equals("")) {
			dialog_no_user(true);
		} else {
			lt = new LoadTask();
			lt.execute(app, getIntent().getIntExtra("item", 0));
		}
	}

	protected void cancel(final String a) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.cancel_confirm)
				.setCancelable(true)
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface d, int id) {
								d.cancel();
							}
						})
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
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
					public void onCancel(DialogInterface d) {
						if (d != null)
							d.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void cancel_done(int result) {
		dialog.dismiss();

		if (result == STATUS_SUCCESS) {
			dialog = ProgressDialog.show(this, "",
					getString(R.string.loading_account), true);
			dialog.show();

			new LoadTask().execute(app, getIntent().getIntExtra("item", 0));
		}
	}

	protected void prolong(final String a) {
		dialog = ProgressDialog.show(AccountActivity.this, "",
				getString(R.string.doing_prolong), true);
		dialog.show();
		pt = new ProlongTask();
		pt.execute(app, a);
	}

	public void prolong_done(int result) {
		dialog.dismiss();

		if (result == STATUS_SUCCESS) {
			dialog = ProgressDialog.show(this, "",
					getString(R.string.loading_account), true);
			dialog.show();

			lt = new LoadTask();
			lt.execute(app, getIntent().getIntExtra("item", 0));
		} else if (result == STATUS_FAILED) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Der Web-Opac meldet: " + app.ohc.getLast_error())
					.setCancelable(false)
					.setNegativeButton(R.string.dismiss,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	public class LoadTask extends OpacTask<List<List<String[]>>> {

		@Override
		protected List<List<String[]>> doInBackground(Object... arg0) {
			app = (OpacClient) arg0[0];

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(app);
			try {
				List<List<String[]>> res = app.ohc.account(
						sp.getString("opac_usernr", ""),
						sp.getString("opac_password", ""));
				return res;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}

			return null;
		}

		protected void onPostExecute(List<List<String[]>> result) {
			loaded(result);
		}
	}

	public void loaded(final List<List<String[]>> result) {
		if (result == null) {
			dialog_wrong_credentials(app.ohc.getLast_error(), true);
			return;
		}

		setContentView(R.layout.account_activity);

		TableLayout td = (TableLayout) findViewById(R.id.tlMedien);
		td.removeAllViews();
		if(result.get(0).size() == 0){
			TableRow row = new TableRow(this);
			TextView t1 = new TextView(this);
			t1.setText(R.string.entl_none);
			row.addView(t1);
			td.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
		for (int i = 0; i < result.get(0).size(); i++) {
			TableRow row = new TableRow(this);

			TextView t1 = new TextView(this);
			t1.setText(Html.fromHtml(result.get(0).get(i)[0] + "<br />"
					+ result.get(0).get(i)[1] + "<br />"
					+ result.get(0).get(i)[2]));
			t1.setPadding(0, 0, 10, 10);
			row.addView(t1);

			TextView t2 = new TextView(this);
			t2.setText(Html.fromHtml(result.get(0).get(i)[3] + " ("
					+ result.get(0).get(i)[4] + ")<br />"
					+ result.get(0).get(i)[6]));
			row.addView(t2);

			if (result.get(0).get(i)[7] != null) {
				final int j = i;
				ImageView b1 = new ImageView(this);
				b1.setImageResource(android.R.drawable.ic_input_add);

				b1.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						AccountActivity.this.prolong(result.get(0).get(j)[7]);
					}
				});
				row.addView(b1);
			}

			td.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}

		TableLayout tr = (TableLayout) findViewById(R.id.tlReservations);
		tr.removeAllViews();
		if(result.get(1).size() == 0){
			TableRow row = new TableRow(this);
			TextView t1 = new TextView(this);
			t1.setText(R.string.reservations_none);
			row.addView(t1);
			tr.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
		for (int i = 0; i < result.get(1).size(); i++) {
			TableRow row = new TableRow(this);

			TextView t1 = new TextView(this);
			t1.setText(Html.fromHtml(result.get(1).get(i)[0] + "<br />"
					+ result.get(1).get(i)[1]));
			t1.setPadding(0, 0, 10, 10);
			row.addView(t1);

			TextView t2 = new TextView(this);
			t2.setText(Html.fromHtml(result.get(1).get(i)[2] + "<br />"
					+ result.get(1).get(i)[3]));
			row.addView(t2);

			if (result.get(1).get(i)[4] != null) {
				final int j = i;
				ImageView b1 = new ImageView(this);
				b1.setImageResource(android.R.drawable.ic_delete);

				b1.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						cancel(result.get(1).get(j)[4]);
					}
				});
				row.addView(b1);
			}

			tr.addView(row, new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}

	public class CancelTask extends OpacTask<Integer> {

		@Override
		protected Integer doInBackground(Object... arg0) {
			app = (OpacClient) arg0[0];
			String a = (String) arg0[1];
			try {
				app.ohc.cancel(a);
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return STATUS_SUCCESS;
		}

		protected void onPostExecute(Integer result) {
			cancel_done(result);
		}
	}

	public class ProlongTask extends OpacTask<Integer> {

		@Override
		protected Integer doInBackground(Object... arg0) {
			app = (OpacClient) arg0[0];
			String a = (String) arg0[1];
			try {
				boolean res = app.ohc.prolong(a);
				if (res) {
					return STATUS_SUCCESS;
				} else {
					return STATUS_FAILED;
				}
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return STATUS_SUCCESS;
		}

		protected void onPostExecute(Integer result) {
			prolong_done(result);
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
