package de.geeksfactory.opacclient.frontend;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.app.ProgressDialog;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchResult;

public class SearchResultsActivity extends OpacActivity {

	protected ProgressDialog dialog;
	protected List<SearchResult> items;
	private int page = 1;

	private SearchStartTask st;
	private SearchPageTask sst;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		((TextView) findViewById(R.id.tvLoading))
				.setText(R.string.loading_results);

		st = new SearchStartTask();
		st.execute(app, getIntent().getBundleExtra("query"));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class SearchStartTask extends OpacTask<List<SearchResult>> {
		private boolean success;

		@Override
		protected List<SearchResult> doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Bundle query = (Bundle) arg0[1];

			try {
				List<SearchResult> res = app.getApi().search(query);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
			} catch (de.geeksfactory.opacclient.NotReachableException e) {
				success = false;
			} catch (java.lang.IllegalStateException e) {
				success = false;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}

			return null;
		}

		protected void onPostExecute(List<SearchResult> result) {
			if (success) {
				if (result == null) {
					setContentView(R.layout.connectivity_error);
					((TextView) findViewById(R.id.tvErrBody)).setText(app
							.getApi().getLast_error());
					((Button) findViewById(R.id.btRetry))
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									onCreate(null);
								}
							});
				} else {
					items = result;
					loaded();
				}
			} else {
				setContentView(R.layout.connectivity_error);
				((Button) findViewById(R.id.btRetry))
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								onCreate(null);
							}
						});
			}
		}
	}

	private void loaded() {
		setContentView(R.layout.search_results_activity);

		ListView lv = (ListView) findViewById(R.id.lvResults);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(SearchResultsActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item", (int) items.get(position).getNr());
				intent.putExtra("item_id", items.get(position).getId());
				startActivity(intent);
			}
		});

		final ImageView btPrev = (ImageView) findViewById(R.id.btPrev);
		if (page == 1) {
			btPrev.setVisibility(View.INVISIBLE);
		}
		btPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setContentView(R.layout.loading);
				((TextView) findViewById(R.id.tvLoading))
						.setText(R.string.loading_results);
				page--;
				sst = new SearchPageTask();
				sst.execute(app, page);
				if (page == 1) {
					btPrev.setVisibility(View.INVISIBLE);
				}
			}
		});

		final ImageView btNext = (ImageView) findViewById(R.id.btNext);
		btNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setContentView(R.layout.loading);
				((TextView) findViewById(R.id.tvLoading))
						.setText(R.string.loading_results);
				page++;
				sst = new SearchPageTask();
				sst.execute(app, page);
				if (page > 1) {
					btPrev.setVisibility(View.VISIBLE);
				}
			}
		});

		TextView rn = (TextView) findViewById(R.id.tvResultNum);
		rn.setText(app.getApi().getResults());

		lv.setAdapter(new ResultsAdapter(this, (items)));
		lv.setTextFilterEnabled(true);
	}

	public class SearchPageTask extends OpacTask<List<SearchResult>> {
		private boolean success;

		@Override
		protected List<SearchResult> doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Integer page = (Integer) arg0[1];

			try {
				List<SearchResult> res = app.getApi().searchGetPage(page);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				publishProgress(e, "ioerror");
			} catch (java.io.IOException e) {
				success = false;
			} catch (de.geeksfactory.opacclient.NotReachableException e) {
				success = false;
			} catch (java.lang.IllegalStateException e) {
				success = false;
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return null;
		}

		protected void onPostExecute(List<SearchResult> result) {
			if (success) {
				items = result;
				loaded();
			} else {
				setContentView(R.layout.connectivity_error);
				((Button) findViewById(R.id.btRetry))
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								onCreate(null);
							}
						});
			}
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
			if (st != null) {
				if (!st.isCancelled()) {
					st.cancel(true);
				}
			}
			if (sst != null) {
				if (!sst.isCancelled()) {
					sst.cancel(true);
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
}
