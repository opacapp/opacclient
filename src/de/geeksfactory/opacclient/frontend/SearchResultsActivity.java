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

import java.net.URL;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.SparseArray;
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
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

public class SearchResultsActivity extends OpacActivity {

	protected SearchRequestResult searchresult;
	private SparseArray<SearchRequestResult> cache = new SparseArray<SearchRequestResult>();
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
		if (item.getItemId() == R.id.action_prev) {
			setContentView(R.layout.loading);
			if (sst != null) {
				sst.cancel(false);
			}
			page--;
			if (cache.get(page) != null) {
				searchresult = cache.get(page);
				loaded();
			} else {
				searchresult = null;
				sst = new SearchPageTask();
				sst.execute(app, page);
			}
			invalidateOptionsMenu();
			return true;
		} else if (item.getItemId() == R.id.action_next) {
			setContentView(R.layout.loading);
			if (sst != null) {
				sst.cancel(false);
			}
			page++;
			if (cache.get(page) != null) {
				searchresult = cache.get(page);
				loaded();
			} else {
				searchresult = null;
				sst = new SearchPageTask();
				sst.execute(app, page);
			}
			invalidateOptionsMenu();
			return true;
		} else if (item.getItemId() == android.R.id.home) {
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

	public class SearchStartTask extends OpacTask<SearchRequestResult> {
		private boolean success;
		private Exception exception;

		@Override
		protected SearchRequestResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Bundle query = (Bundle) arg0[1];

			try {
				SearchRequestResult res = app.getApi().search(query);
				//Load cover images, if search worked and covers available
				if (res != null) loadCovers(res);
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
		protected void onPostExecute(SearchRequestResult result) {
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
					searchresult = result;
					if (searchresult != null) {
						if (searchresult.getResults().size() > 0) {
							if (searchresult.getResults().get(0).getId() != null)
								cache.put(page, searchresult);
						}
					}
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

	protected void loaded() {
		setContentView(R.layout.search_results_activity);

		ListView lv = (ListView) findViewById(R.id.lvResults);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(SearchResultsActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item",
						(int) searchresult.getResults().get(position).getNr());

				if (searchresult.getResults().get(position).getId() != null)
					intent.putExtra("item_id",
							searchresult.getResults().get(position).getId());
				startActivity(intent);
			}
		});

		TextView rn = (TextView) findViewById(R.id.tvResultNum);
		if (searchresult.getTotal_result_count() >= 0)
			rn.setText(getString(R.string.result_number,
					searchresult.getTotal_result_count()));

		if (searchresult.getResults().size() == 0
				&& searchresult.getTotal_result_count() == 0) {
			setContentView(R.layout.no_results);
		}

		lv.setAdapter(new ResultsAdapter(this, (searchresult.getResults())));
		lv.setTextFilterEnabled(true);
	}

	public class SearchPageTask extends OpacTask<SearchRequestResult> {
		private boolean success;

		@Override
		protected SearchRequestResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			Integer page = (Integer) arg0[1];

			try {
				SearchRequestResult res = app.getApi().searchGetPage(page);
				//Load cover images, if search worked and covers available
				if (res != null) loadCovers(res);
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
		protected void onPostExecute(SearchRequestResult result) {
			if (success) {
				searchresult = result;
				if (searchresult != null) {
					if (searchresult.getResults().size() > 0) {
						if (searchresult.getResults().get(0).getId() != null)
							cache.put(page, searchresult);
					}
				}
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
	
	private void loadCovers(SearchRequestResult res) {
		URL newurl;
		for(SearchResult item:res.getResults()) {
			if (item.getCover() != null && item.getCoverBitmap() == null) {
				try {
					newurl = new URL(item.getCover());
					Bitmap mIcon_val = BitmapFactory.decodeStream(newurl
							.openConnection().getInputStream());
					if(mIcon_val.getHeight() > 1 && mIcon_val.getWidth() > 1) {
						item.setCoverBitmap(mIcon_val);
					} else {
						//When images embedded from Amazon aren't available, a 1x1
						//pixel image is returned (iOPAC)
						item.setCover(null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		}
	}
}
