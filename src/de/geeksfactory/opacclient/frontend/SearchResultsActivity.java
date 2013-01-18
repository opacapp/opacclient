package de.geeksfactory.opacclient.frontend;

import java.util.List;

import org.holoeverywhere.app.ProgressDialog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchResult;

public class SearchResultsActivity extends OpacActivity {

	protected ProgressDialog dialog;
	protected List<SearchResult> items;
	private int page;

	private SearchStartTask st;
	private SearchPageTask sst;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		((TextView) findViewById(R.id.tvLoading))
				.setText(R.string.loading_results);

		page = 1;

		st = new SearchStartTask();
		st.execute(app, getIntent().getBundleExtra("query"));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_prev:
			setContentView(R.layout.loading);
			((TextView) findViewById(R.id.tvLoading))
					.setText(R.string.loading_results);
			page--;
			sst = new SearchPageTask();
			sst.execute(app, page);
			invalidateOptionsMenu();
			return true;
		case R.id.action_next:
			setContentView(R.layout.loading);
			((TextView) findViewById(R.id.tvLoading))
					.setText(R.string.loading_results);
			page++;
			sst = new SearchPageTask();
			sst.execute(app, page);
			invalidateOptionsMenu();
			return true;
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_search_results, menu);

		if (page == 1) {
			menu.findItem(R.id.action_prev).setVisible(false);
		} else {

			menu.findItem(R.id.action_prev).setVisible(true);
		}

		return super.onCreateOptionsMenu(menu);
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
			} catch (Exception e) {
				e.printStackTrace();
				success = false;
			}

			return null;
		}

		protected void onPostExecute(List<SearchResult> result) {
			if (success) {
				if (result == null) {

					if (app.getApi().getLast_error().equals("is_a_redirect")) {
						Intent intent = new Intent(SearchResultsActivity.this,
								SearchResultDetailsActivity.class);
						startActivity(intent);
						finish();
						return;
					}

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

				if (items.get(position).getId() != null)
					intent.putExtra("item_id", items.get(position).getId());
				startActivity(intent);
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
			} catch (Exception e) {
				publishProgress(e, "ioerror");
				e.printStackTrace();
				success = false;
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
	protected void onStop() {
		super.onStop();
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
