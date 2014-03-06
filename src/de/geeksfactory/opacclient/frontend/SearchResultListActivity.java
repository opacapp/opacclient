package de.geeksfactory.opacclient.frontend;

import org.acra.ACRA;
import org.holoeverywhere.app.Activity;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.R.id;
import de.geeksfactory.opacclient.R.layout;
import de.geeksfactory.opacclient.objects.SearchRequestResult;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * An activity representing a list of SearchResults. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link SearchResultDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link SearchResultListFragment} and the item details (if present) is a
 * {@link SearchResultDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link SearchResultListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class SearchResultListActivity extends OpacActivity implements
		SearchResultListFragment.Callbacks, SearchResultDetailFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	
	protected SearchRequestResult searchresult;
	private SparseArray<SearchRequestResult> cache = new SparseArray<SearchRequestResult>();
	private int page;

	private SearchStartTask st;
	//private SearchPageTask sst;
	
	private SearchResultListFragment listFragment;
	private SearchResultDetailFragment detailFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		listFragment = (SearchResultListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.searchresult_list);

		if (findViewById(R.id.searchresult_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			listFragment.setActivateOnItemClick(true);
		}
		
		page = 1;
		performsearch();
	}
	
	public void performsearch() {
		if (page == 1) {
			st = new SearchStartTask();
			st.execute(app, getIntent().getBundleExtra("query"));
		} else {
//TODO:			sst = new SearchPageTask();
//			sst.execute(app, page);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method from {@link SearchResultListFragment.Callbacks}
	 * indicating that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(int nr, String id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putInt(SearchResultDetailFragment.ARG_ITEM_NR, nr);
			if(id != null)
				arguments.putString(SearchResultDetailFragment.ARG_ITEM_ID, id);
			detailFragment = new SearchResultDetailFragment();
			detailFragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.searchresult_detail_container, detailFragment)
					.commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this,
					SearchResultDetailActivity.class);
			detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_NR, nr);
			if(id != null)
				detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
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

//TODO:					if (app.getApi().getLast_error().equals("is_a_redirect")) {    (what is this for?)
//						Intent intent = new Intent(SearchResultsListActivity.this,
//								SearchResultDetailsActivity.class);
//						startActivity(intent);
//						finish();
//						return;
//					}

//TODO:					setContentView(R.layout.connectivity_error);
//					((TextView) findViewById(R.id.tvErrBody)).setText(app
//							.getApi().getLast_error());
//					((Button) findViewById(R.id.btRetry))
//							.setOnClickListener(new OnClickListener() {
//								@Override
//								public void onClick(View v) {
//									performsearch();
//								}
//							});
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
//TODO:				setContentView(R.layout.connectivity_error);
//				if (exception != null
//						&& exception instanceof NotReachableException)
//					((TextView) findViewById(R.id.tvErrBody))
//							.setText(R.string.connection_error_detail_nre);
//				((Button) findViewById(R.id.btRetry))
//						.setOnClickListener(new OnClickListener() {
//							@Override
//							public void onClick(View v) {
//								performsearch();
//							}
//						});
			}
		}
	}

	protected void loaded() {
//		lv.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				Intent intent = new Intent(SearchResultsActivity.this,
//						SearchResultDetailsActivity.class);
//				intent.putExtra("item",
//						(int) searchresult.getResults().get(position).getNr());
//
//				if (searchresult.getResults().get(position).getId() != null)
//					intent.putExtra("item_id",
//							searchresult.getResults().get(position).getId());
//				startActivity(intent);
//			}
//		});
		
		listFragment.setSearchResult(searchresult);
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_searchresult_list;
	}

	@Override
	public void removeFragment() {
		getSupportFragmentManager().beginTransaction().remove(detailFragment).commit();
	}
}
