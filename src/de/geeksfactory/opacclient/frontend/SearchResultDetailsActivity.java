package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.storage.StarDataSource;

public class SearchResultDetailsActivity extends OpacActivity {

	protected ProgressDialog dialog;
	protected DetailledItem item;
	protected String id;
	protected String title;

	public static int STATUS_SUCCESS = 0;
	public static int STATUS_NOUSER = 1;
	public static int STATUS_WRONGCREDENTIALS = 2;

	protected String[] items;
	protected FetchTask ft;
	protected FetchSubTask fst;
	protected ResTask rt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		((TextView) findViewById(R.id.tvLoading))
				.setText(R.string.loading_details);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (getIntent().getStringExtra("item_id") != null) {
			id = getIntent().getStringExtra("item_id");
		} else {
			finish();
		}

		if (getIntent().getIntExtra("item", -1) != -1) {
			ft = new FetchTask();
			ft.execute(app, getIntent().getIntExtra("item", 0));
		} else if (getIntent().getStringExtra("item_id") != null) {
			fst = new FetchSubTask();
			fst.execute(app, getIntent().getStringExtra("item_id"));
		}
	}

	protected void reservation() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		items = sp.getString("opac_zst", "00:").split("~");
		if (items[0].startsWith(":")) {
			List<String> tmp = new ArrayList<String>(Arrays.asList(items));
			tmp.remove(0);
			String[] tmp2 = new String[tmp.size()];
			items = tmp.toArray(tmp2);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.res_zst));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int item) {
				dialog = ProgressDialog.show(SearchResultDetailsActivity.this,
						"", getString(R.string.doing_res), true);
				dialog.show();

				rt = new ResTask();
				rt.execute(app, items[item].split(":", 2)[0]);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void reservation_done(int result) {
		if (result == STATUS_NOUSER) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.status_nouser)
					.setCancelable(true)
					.setNegativeButton(R.string.dismiss,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		} else if (result == STATUS_SUCCESS) {
			Intent intent = new Intent(SearchResultDetailsActivity.this,
					AccountActivity.class);
			startActivity(intent);
		}
	}

	public class FetchTask extends OpacTask<DetailledItem> {
		protected boolean success = true;

		@Override
		protected DetailledItem doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Integer nr = (Integer) arg0[1];

			try {
				DetailledItem res = app.getApi().getResult(nr);
				URL newurl;
				try {
					newurl = new URL(res.getCover());
					Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
							.openConnection().getInputStream());
					res.setCoverBitmap(mIcon_val);
				} catch (Exception e) {
				}
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
			} catch (java.lang.IllegalStateException e) {
				success = false;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return null;
		}

		protected void onPostExecute(DetailledItem result) {
			if (!success) {
				setContentView(R.layout.connectivity_error);
				((Button) findViewById(R.id.btRetry))
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								onCreate(null);
							}
						});
				return;
			}

			item = result;

			setContentView(R.layout.result_details_activity);

			try {
				Log.i("result", item.toString());
			} catch (Exception e) {
				app.web_error(e, "ioerror");
			}
			ImageView iv = (ImageView) findViewById(R.id.ivCover);

			if (item.getCoverBitmap() != null) {
				iv.setVisibility(View.VISIBLE);
				iv.setImageBitmap(item.getCoverBitmap());
			} else {
				iv.setVisibility(View.GONE);
			}

			TextView tvTitel = (TextView) findViewById(R.id.tvTitle);
			tvTitel.setText(item.getTitle());
			title = item.getTitle();

			TableLayout td = (TableLayout) findViewById(R.id.tlDetails);

			for (int i = 0; i < result.getDetails().size(); i++) {
				TableRow row = new TableRow(SearchResultDetailsActivity.this);

				if (result.getDetails().get(i)[0]
						.equals("Annotation/ Beschreibung:")) {
					TextView t2 = new TextView(SearchResultDetailsActivity.this);
					t2.setText(Html.fromHtml("<i>"
							+ result.getDetails().get(i)[1] + "</i>"));
					TableRow.LayoutParams params = new TableRow.LayoutParams();
					params.span = 2;
					t2.setPadding(20, 0, 0, 0);
					row.addView(t2, params);
				} else {
					TextView t1 = new TextView(SearchResultDetailsActivity.this);
					t1.setText(Html.fromHtml(result.getDetails().get(i)[0]));
					t1.setPadding(0, 0, 10, 0);
					row.addView(t1);

					TextView t2 = new TextView(SearchResultDetailsActivity.this);
					t2.setText(Html.fromHtml(result.getDetails().get(i)[1]
							.replace("\n", "<br />")));
					row.addView(t2);
				}
				td.addView(row, new TableLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			}

			if (result.getBaende().size() > 0) {
				TextView tvC = (TextView) findViewById(R.id.tvCopies);
				tvC.setText(R.string.baende);
				TableLayout tc = (TableLayout) findViewById(R.id.tlExemplare);

				for (int i = 0; i < result.getBaende().size(); i++) {
					TableRow row = new TableRow(
							SearchResultDetailsActivity.this);

					TextView t1 = new TextView(SearchResultDetailsActivity.this);
					t1.setText(Html.fromHtml(result.getBaende().get(i)[1]));
					row.addView(t1);
					final String a = result.getBaende().get(i)[0];

					Button b2 = new Button(SearchResultDetailsActivity.this);
					b2.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							Intent intent = new Intent(
									SearchResultDetailsActivity.this,
									SearchResultDetailsActivity.class);
							intent.putExtra("item_id", a);
							startActivity(intent);
						}
					});
					b2.setText(R.string.details);
					row.addView(b2);
					tc.addView(row, new TableLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
				}
			} else {
				TableLayout tc = (TableLayout) findViewById(R.id.tlExemplare);

				for (int i = 0; i < result.getCopies().size(); i++) {
					TableRow row = new TableRow(
							SearchResultDetailsActivity.this);

					TextView t1 = new TextView(SearchResultDetailsActivity.this);
					String t1t = "";
					if (!result.getCopies().get(i)[0].equals("?")) {
						t1t = t1t + result.getCopies().get(i)[0] + "<br />";
					}
					if (!result.getCopies().get(i)[1].equals("?")) {
						t1t = t1t + result.getCopies().get(i)[1];
					}
					t1.setText(Html.fromHtml(t1t));
					row.addView(t1);

					TextView t2 = new TextView(SearchResultDetailsActivity.this);
					String status = result.getCopies().get(i)[4] + "<br />";
					if (!result.getCopies().get(i)[5].equals("")
							&& !result.getCopies().get(i)[5].equals("?")) {
						status = status + getString(R.string.ret) + ": "
								+ result.getCopies().get(i)[5] + "<br />";
					}
					t2.setPadding(10, 0, 0, 0);
					if (!result.getCopies().get(i)[6].equals("")
							&& !result.getCopies().get(i)[6].equals("?")) {
						status = status + getString(R.string.res) + ": "
								+ result.getCopies().get(i)[6];
					}
					t2.setText(Html.fromHtml(status));
					row.addView(t2);

					tc.addView(row, new TableLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
				}
			}

			if (item.isReservable()) {
				Button btres = (Button) findViewById(R.id.btReserve);
				btres.setVisibility(View.VISIBLE);
				btres.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						reservation();
					}
				});
			}

		}
	}

	public class FetchSubTask extends FetchTask {
		@Override
		protected DetailledItem doInBackground(Object... arg0) {
			this.a = (OpacClient) arg0[0];
			String a = (String) arg0[1];

			try {
				DetailledItem res = app.getApi().getResultById(a);
				URL newurl;
				try {
					newurl = new URL(res.getCover());
					Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
							.openConnection().getInputStream());
					res.setCoverBitmap(mIcon_val);
				} catch (Exception e) {
				}
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
			} catch (java.lang.IllegalStateException e) {
				success = false;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}

			return null;
		}
	}

	public class ResTask extends OpacTask<Integer> {
		private boolean success;
		
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			app = (OpacClient) arg0[0];
			String zst = (String) arg0[1];
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(app);
			try {
				if (sp.getString("opac_usernr", "").equals("")
						|| sp.getString("opac_password", "").equals("")) {
					success = true;
					return STATUS_NOUSER;
				} else {
					Boolean res = app.getApi().reservation(zst,
							sp.getString("opac_usernr", ""),
							sp.getString("opac_password", ""));
					success = true;
					if (res == null)
						return STATUS_WRONGCREDENTIALS;
				}
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
			} catch (java.lang.IllegalStateException e) {
				success = false;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return STATUS_SUCCESS;
		}

		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			
			if (!success) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						SearchResultDetailsActivity.this);
				builder.setMessage(R.string.connection_error)
						.setCancelable(true)
						.setNegativeButton(R.string.dismiss,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
				return;
			}
			
			if (result == STATUS_WRONGCREDENTIALS) {
				dialog_wrong_credentials(app.getApi().getLast_error(), false);
				return;
			}
			reservation_done(result);
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
			if (ft != null) {
				if (!ft.isCancelled()) {
					ft.cancel(true);
				}
			}
			if (fst != null) {
				if (!fst.isCancelled()) {
					fst.cancel(true);
				}
			}
			if (rt != null) {
				if (!rt.isCancelled()) {
					rt.cancel(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindDrawables(findViewById(R.id.rootView));
		System.gc();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.search_result_details_activity, menu);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		String bib = sp.getString("opac_bib", "");

		StarDataSource data = new StarDataSource(this);
		data.open();
		if (data.isStarred(bib, id)) {
			menu.findItem(R.id.action_star).setIcon(R.drawable.ic_ab_star_1);
		}
		data.close();

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		String bib = sp.getString("opac_bib", "");

		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		case R.id.action_share:
			Intent intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

			// Add data to the intent, the receiving app will decide
			// what to do with it.
			intent.putExtra(Intent.EXTRA_SUBJECT, title);

			String t = title;
			try {
				bib = java.net.URLEncoder.encode(bib, "UTF-8");
				t = java.net.URLEncoder.encode(t, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}

			intent.putExtra(Intent.EXTRA_TEXT,
					"http://www.raphaelmichel.de/opacclient/bibproxy.php/go?bib="
							+ bib + "&id=" + id + "&title=" + t);
			startActivity(Intent.createChooser(intent, getResources()
					.getString(R.string.share)));
			return true;

		case R.id.action_star:
			StarDataSource star = new StarDataSource(
					SearchResultDetailsActivity.this);
			star.open();

			if (star.isStarred(bib, id)) {
				star.remove(star.getItem(bib, id));
				item.setIcon(R.drawable.ic_ab_star_0);
			} else {
				star.star(id, title, bib);
				Toast toast = Toast.makeText(SearchResultDetailsActivity.this,
						getString(R.string.starred), Toast.LENGTH_SHORT);
				toast.show();
				item.setIcon(R.drawable.ic_ab_star_1);
			}
			star.close();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
