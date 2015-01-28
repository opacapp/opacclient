package de.geeksfactory.opacclient.frontend;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.acra.ACRA;

import java.io.InterruptedIOException;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.SSLSecurityException;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

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
		SearchResultListFragment.Callbacks,
		SearchResultDetailFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	protected boolean mTwoPane;

	protected SearchRequestResult searchresult;

	protected SearchResultListFragment listFragment;
	protected SearchResultDetailFragment detailFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null)
			setup();

		if (findViewById(R.id.searchresult_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;
		}
	}

	protected void setup() {
		if ("com.google.android.gms.actions.SEARCH_ACTION".equals(getIntent()
				.getAction())) {
			listFragment = SearchResultListFragment
					.getGoogleSearchInstance(getIntent()
							.getStringExtra(SearchManager.QUERY));
		} else if (getIntent().hasExtra("volumeQuery")) {
			listFragment = SearchResultListFragment
					.getVolumeSearchInstance(getIntent().getBundleExtra(
							"volumeQuery"));
		} else {
			listFragment = SearchResultListFragment.getInstance(getIntent()
					.getBundleExtra("query"));
		}
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.searchresult_list_container, listFragment)
				.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_search_results, menu);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Callback method from {@link SearchResultListFragment.Callbacks}
	 * indicating that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(SearchResult result, View coverView) {
		if ((app.getApi().getSupportFlags() & OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING) == 0
				&& result.getPage() != listFragment.getLastLoadedPage()) {
			new ReloadOldPageTask().execute(app, result, coverView);
		} else {
			showDetail(result, coverView);
		}
	}

	public void showDetail(SearchResult res, View coverView) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putInt(SearchResultDetailFragment.ARG_ITEM_NR, res.getNr());
			if (res.getId() != null)
				arguments.putString(SearchResultDetailFragment.ARG_ITEM_ID, res.getId());
			detailFragment = new SearchResultDetailFragment();
			detailFragment.setArguments(arguments);
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.searchresult_detail_container, detailFragment)
					.commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this,
					SearchResultDetailActivity.class);
			detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_NR, res.getNr());
			if (res.getId() != null)
				detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
						res.getId());
            if (res.getCoverBitmap() != null) {
                detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP, (Parcelable) res.getCoverBitmap());
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, coverView, getString(R.string.transition_cover));
                ActivityCompat.startActivity(this, detailIntent, options.toBundle());
            } else {
                startActivity(detailIntent);
            }
		}
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_searchresult_list;
	}

	@Override
	public void removeFragment() {
		getSupportFragmentManager().beginTransaction().remove(detailFragment)
				.commit();
	}

	public class ReloadOldPageTask extends OpacTask<SearchRequestResult> {
		SearchResult searchResult;
		Exception exception;
        View coverView;

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected SearchRequestResult doInBackground(Object... arg0) {
			searchResult = (SearchResult) arg0[1];
            coverView = (View) arg0[2];
			OpacClient app = (OpacClient) arg0[0];

			try {
				SearchRequestResult res = app.getApi().searchGetPage(searchResult.getPage());
				return res;
			} catch (java.net.UnknownHostException e) {
				exception = e;
				e.printStackTrace();
			} catch (java.net.SocketException e) {
				exception = e;
			} catch (InterruptedIOException e) {
				exception = e;
			} catch (OpacErrorException e) {
				exception = e;
			} catch (Exception e) {
				exception = e;
				ACRA.getErrorReporter().handleException(e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(SearchRequestResult result) {
			setProgressBarIndeterminateVisibility(false);
			if (result == null) {

				if (exception instanceof OpacErrorException) {
					listFragment.showConnectivityError(exception.getMessage());
				} else if (exception instanceof SSLSecurityException) {
					listFragment.showConnectivityError(getResources()
							.getString(R.string.connection_error_detail_security));
				} else if (exception instanceof NotReachableException) {
					listFragment.showConnectivityError(getResources()
							.getString(R.string.connection_error_detail_nre));
				} else {
					listFragment.showConnectivityError();
				}
			} else {
				// Everything ran correctly, show Detail
				listFragment.setLastLoadedPage(searchResult.getPage());
				showDetail(searchResult, coverView);
			}
		}
	}

	@Override
	public boolean isTwoPane() {
		return mTwoPane;
	}
}
