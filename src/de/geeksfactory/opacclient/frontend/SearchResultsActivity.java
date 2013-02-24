package de.geeksfactory.opacclient.frontend;

import java.util.List;

import org.acra.ACRA;

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

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchResult;

public class SearchResultsActivity extends OpacActivity {

	protected List<SearchResult> items;
	private int page;

	private SearchStartTask st;
	private SearchPageTask sst;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSlidingMenu().setSlidingEnabled(false);
		setContentView(R.layout.loading);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		page = 1;
		performsearch();
	}

	public void performsearch() {
		setContentView(R.layout.loading);
		if (page == 1) {
			st = new SearchStartTask();
			st.execute(app, getIntent().getBundleExtra("query"));
		} else {
			sst = new SearchPageTask();
			sst.execute(app, page);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_prev:
			setContentView(R.layout.loading);
			page--;
			sst = new SearchPageTask();
			sst.execute(app, page);
			invalidateOptionsMenu();
			return true;
		case R.id.action_next:
			setContentView(R.layout.loading);
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
		private Exception exception;

		@Override
		protected List<SearchResult> doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Bundle query = (Bundle) arg0[1];

			try {
				List<SearchResult> res = app.getApi().search(query);
				success = true;
				return res;
			} catch (java.net.UnknownHostException e) {
				success = false;
				exception = e;
				e.printStackTrace();
			} catch (java.net.SocketException e) {
				success = false;
				exception = e;
			} catch (Exception e) {
				exception = e;
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}

			return null;
		}

		@Override
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
									performsearch();
								}
							});
				} else {
					items = result;
					loaded();
				}
			} else {
				setContentView(R.layout.connectivity_error);
				if (exception != null
						&& exception instanceof NotReachableException)
					((TextView) findViewById(R.id.tvErrBody))
							.setText(R.string.connection_error_detail_nre);
				((Button) findViewById(R.id.btRetry))
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								performsearch();
							}
						});
			}
		}
	}

	private void loaded() {
		setContentView(R.layout.search_results_activity);

		ListView lv = (ListView) findViewById(R.id.lvResults);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
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
			} catch (java.net.UnknownHostException e) {
				success = false;
				e.printStackTrace();
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
								performsearch();
							}
						});
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

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
