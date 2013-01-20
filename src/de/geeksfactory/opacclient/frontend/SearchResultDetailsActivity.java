package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;

import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ProgressDialog;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;
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

		if (getIntent().hasExtra("item_id")
				&& !getIntent().getStringExtra("item_id").equals("")) {
			id = getIntent().getStringExtra("item_id");
			fst = new FetchSubTask();
			fst.execute(app, id);
		} else if (getIntent().getIntExtra("item", -1) != -1) {
			ft = new FetchTask();
			ft.execute(app, getIntent().getIntExtra("item", 0));
		} else {
			fst = new FetchSubTask();
			fst.execute(app, id);
		}
	}

	protected void dialog_no_user() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.status_nouser)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						})
				.setPositiveButton(R.string.accounts_edit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(
										SearchResultDetailsActivity.this,
										AccountListActivity.class);
								startActivity(intent);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void reservation() {
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		final List<Account> accounts = data.getAllAccounts(app.getLibrary()
				.getIdent());
		data.close();
		if (accounts.size() == 0) {
			dialog_no_user();
			return;
		} else if (accounts.size() > 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Get the layout inflater
			LayoutInflater inflater = getLayoutInflater();

			View view = inflater.inflate(R.layout.account_add_liblist_dialog,
					null);

			ListView lv = (ListView) view.findViewById(R.id.lvBibs);
			AccountListAdapter adapter = new AccountListAdapter(this, accounts);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					if (accounts.get(position).getId() != app.getAccount()
							.getId()) {
						app.setAccount(accounts.get(position).getId());

						new RestoreSessionTask().execute();
					}
					adialog.dismiss();
					reservation_zst();
				}
			});
			builder.setTitle(R.string.account_select)
					.setView(view)
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									adialog.cancel();
								}
							})
					.setNeutralButton(R.string.accounts_edit,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
									Intent intent = new Intent(
											SearchResultDetailsActivity.this,
											AccountListActivity.class);
									startActivity(intent);
								}
							});
			adialog = builder.create();
			adialog.show();
		} else {
			reservation_zst();
		}
	}

	public void reservation_zst() {
		MetaDataSource data = new SQLMetaDataSource(this);
		data.open();
		final List<ContentValues> zst = data.getMeta(app.getLibrary()
				.getIdent(), MetaDataSource.META_TYPE_BRANCH);
		data.close();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Get the layout inflater
		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.account_add_liblist_dialog, null);

		ListView lv = (ListView) view.findViewById(R.id.lvBibs);

		lv.setAdapter(new OpacActivity.MetaAdapter(this, zst,
				R.layout.zst_listitem));
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				adialog.dismiss();

				dialog = ProgressDialog.show(SearchResultDetailsActivity.this,
						"", getString(R.string.doing_res), true);
				dialog.show();

				rt = new ResTask();
				rt.execute(app, zst.get(position).getAsString("key"));
			}
		});
		builder.setTitle(getString(R.string.res_zst))
				.setView(view)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						});
		adialog = builder.create();
		adialog.show();
	}

	public void reservation_done(int result) {
		if (result == STATUS_SUCCESS) {
			Intent intent = new Intent(SearchResultDetailsActivity.this,
					AccountActivity.class);
			startActivity(intent);
		}
	}

	public class RestoreSessionTask extends OpacTask<Integer> {
		@Override
		protected Integer doInBackground(Object... arg0) {
			try {
				app.getApi().getResultById(SearchResultDetailsActivity.this.id);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}

		protected void onPostExecute(Integer result) {
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
				if (res.getCover() != null) {
					try {
						newurl = new URL(res.getCover());
						Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
								.openConnection().getInputStream());
						res.setCoverBitmap(mIcon_val);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				success = true;
				return res;
			} catch (Exception e) {
				e.printStackTrace();
				success = false;
			}
			return null;
		}

		@SuppressLint("NewApi")
		protected void onPostExecute(DetailledItem result) {
			if (!success || result == null) {
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

			LinearLayout llDetails = (LinearLayout) findViewById(R.id.llDetails);
			for (Detail detail : result.getDetails()) {
				View v = getLayoutInflater().inflate(R.layout.detail_listitem,
						null);
				((TextView) v.findViewById(R.id.tvDesc)).setText(detail
						.getDesc());
				((TextView) v.findViewById(R.id.tvContent)).setText(detail
						.getContent());
				llDetails.addView(v);
			}

			LinearLayout llCopies = (LinearLayout) findViewById(R.id.llCopies);
			if (result.getVolumesearch() != null) {
				TextView tvC = (TextView) findViewById(R.id.tvCopies);
				tvC.setText(R.string.baende);
				Button btnVolume = new Button(SearchResultDetailsActivity.this);
				btnVolume.setText(R.string.baende_volumesearch);
				btnVolume.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent myIntent = new Intent(
								SearchResultDetailsActivity.this,
								SearchResultsActivity.class);
						myIntent.putExtra("query", item.getVolumesearch());
						startActivity(myIntent);
					}
				});
				llCopies.addView(btnVolume);

			} else if (result.getBaende().size() > 0) {
				TextView tvC = (TextView) findViewById(R.id.tvCopies);
				tvC.setText(R.string.baende);

				for (final ContentValues band : result.getBaende()) {
					View v = getLayoutInflater().inflate(
							R.layout.band_listitem, null);
					((TextView) v.findViewById(R.id.tvTitel)).setText(band
							.getAsString(DetailledItem.KEY_CHILD_TITLE));

					v.findViewById(R.id.llItem).setOnClickListener(
							new OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent(
											SearchResultDetailsActivity.this,
											SearchResultDetailsActivity.class);
									intent.putExtra(
											"item_id",
											band.getAsString(DetailledItem.KEY_CHILD_ID));
									startActivity(intent);
								}
							});
					llCopies.addView(v);
				}
			} else {
				for (ContentValues copy : result.getCopies()) {
					View v = getLayoutInflater().inflate(
							R.layout.copy_listitem, null);

					if (copy.containsKey(DetailledItem.KEY_COPY_LOCATION)) {
						((TextView) v.findViewById(R.id.tvLocation))
								.setText(copy
										.getAsString(DetailledItem.KEY_COPY_LOCATION));
						((TextView) v.findViewById(R.id.tvLocation))
								.setVisibility(View.VISIBLE);
					} else if (copy.containsKey(DetailledItem.KEY_COPY_BARCODE)) {
						((TextView) v.findViewById(R.id.tvLocation))
								.setText(copy
										.getAsString(DetailledItem.KEY_COPY_BARCODE));
						((TextView) v.findViewById(R.id.tvLocation))
								.setVisibility(View.VISIBLE);
					} else {
						((TextView) v.findViewById(R.id.tvLocation))
								.setVisibility(View.GONE);
					}
					if (copy.containsKey(DetailledItem.KEY_COPY_BRANCH)) {
						((TextView) v.findViewById(R.id.tvZst)).setText(copy
								.getAsString(DetailledItem.KEY_COPY_BRANCH));
						((TextView) v.findViewById(R.id.tvZst))
								.setVisibility(View.VISIBLE);
					} else {
						((TextView) v.findViewById(R.id.tvZst))
								.setVisibility(View.GONE);
					}
					if (copy.containsKey(DetailledItem.KEY_COPY_STATUS)) {
						((TextView) v.findViewById(R.id.tvStatus)).setText(copy
								.getAsString(DetailledItem.KEY_COPY_STATUS));
						((TextView) v.findViewById(R.id.tvStatus))
								.setVisibility(View.VISIBLE);
					} else {
						((TextView) v.findViewById(R.id.tvStatus))
								.setVisibility(View.GONE);
					}
					if (copy.containsKey(DetailledItem.KEY_COPY_RESERVATIONS)) {
						((TextView) v.findViewById(R.id.tvVorbestellt))
								.setText(getString(R.string.res)
										+ ": "
										+ copy.getAsString(DetailledItem.KEY_COPY_RESERVATIONS));
						((TextView) v.findViewById(R.id.tvVorbestellt))
								.setVisibility(View.VISIBLE);
					} else {
						((TextView) v.findViewById(R.id.tvVorbestellt))
								.setVisibility(View.GONE);
					}
					if (copy.containsKey("rueckgabe")) {
						((TextView) v.findViewById(R.id.tvRueckgabe))
								.setText(getString(R.string.ret)
										+ ": "
										+ copy.getAsString(DetailledItem.KEY_COPY_RETURN));
						((TextView) v.findViewById(R.id.tvRueckgabe))
								.setVisibility(View.VISIBLE);
					} else {
						((TextView) v.findViewById(R.id.tvRueckgabe))
								.setVisibility(View.GONE);
					}

					llCopies.addView(v);
				}
			}

			if (id == null || id.equals("")) {
				id = item.getId();
			}

			invalidateOptionsMenu();
		}
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.opac_error) + " " + s)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (finish)
									finish();
							}
						})
				.setPositiveButton(R.string.prefs,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(
										SearchResultDetailsActivity.this,
										MainPreferenceActivity.class);
								startActivity(intent);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
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
					e.printStackTrace();
				}
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
				e.printStackTrace();
			} catch (java.lang.IllegalStateException e) {
				success = false;
				e.printStackTrace();
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

			try {
				OpacApi.ReservationResult res = app.getApi().reservation(zst,
						app.getAccount());
				success = true;
				if (res == OpacApi.ReservationResult.ERROR)
					return STATUS_WRONGCREDENTIALS;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
				e.printStackTrace();
			} catch (java.lang.IllegalStateException e) {
				success = false;
				e.printStackTrace();
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return STATUS_SUCCESS;
		}

		protected void onPostExecute(Integer result) {
			dialog.dismiss();

			if (result == STATUS_WRONGCREDENTIALS) {
				dialog_wrong_credentials(app.getApi().getLast_error(), false);
				return;
			}

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
			} else {
				reservation_done(result);
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

		if (item != null) {
			if (item.isReservable()) {
				menu.findItem(R.id.action_reservation).setVisible(true);
			} else {
				menu.findItem(R.id.action_reservation).setVisible(false);
			}
		} else {
			menu.findItem(R.id.action_reservation).setVisible(false);
		}

		String bib = app.getLibrary().getIdent();
		StarDataSource data = new StarDataSource(this);
		data.open();
		if ((id == null || id.equals("")) && item != null) {
			if (data.isStarredTitle(bib, title)) {
				menu.findItem(R.id.action_star).setIcon(
						R.drawable.ic_action_star_1);
			}
		} else {
			if (data.isStarred(bib, id)) {
				menu.findItem(R.id.action_star).setIcon(
						R.drawable.ic_action_star_1);
			}
		}
		data.close();

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		String bib = app.getLibrary().getIdent();
		switch (item.getItemId()) {
		case R.id.action_reservation:
			reservation();
			return true;

		case android.R.id.home:
			finish();
			return true;

		case R.id.action_share:
			if (this.item == null) {
				Toast toast = Toast.makeText(SearchResultDetailsActivity.this,
						getString(R.string.share_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else {
				Intent intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

				// Add data to the intent, the receiving app will decide
				// what to do with it.
				intent.putExtra(Intent.EXTRA_SUBJECT, title);

				String t = title;
				try {
					bib = java.net.URLEncoder.encode(app.getLibrary()
							.getIdent(), "UTF-8");
					t = java.net.URLEncoder.encode(t, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				}

				intent.putExtra(Intent.EXTRA_TEXT, "http://opacapp.de/:" + bib
						+ ":" + id + ":" + t);
				startActivity(Intent.createChooser(intent, getResources()
						.getString(R.string.share)));
			}
			return true;

		case R.id.action_star:
			StarDataSource star = new StarDataSource(
					SearchResultDetailsActivity.this);
			star.open();

			if (this.item == null) {
				Toast toast = Toast.makeText(SearchResultDetailsActivity.this,
						getString(R.string.star_wait), Toast.LENGTH_SHORT);
				toast.show();
			} else if (id == null || id.equals("")) {
				if (star.isStarredTitle(bib, title)) {
					star.remove(star.getItemTitle(bib, title));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(null, title, bib);
					Toast toast = Toast.makeText(
							SearchResultDetailsActivity.this,
							getString(R.string.starred), Toast.LENGTH_SHORT);
					toast.show();
					item.setIcon(R.drawable.ic_action_star_1);
				}
			} else {
				if (star.isStarred(bib, id)) {
					star.remove(star.getItem(bib, id));
					item.setIcon(R.drawable.ic_action_star_0);
				} else {
					star.star(id, title, bib);
					Toast toast = Toast.makeText(
							SearchResultDetailsActivity.this,
							getString(R.string.starred), Toast.LENGTH_SHORT);
					toast.show();
					item.setIcon(R.drawable.ic_action_star_1);
				}
			}
			star.close();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
