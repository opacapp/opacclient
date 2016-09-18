package de.geeksfactory.opacclient.frontend;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.utils.BitmapUtils;
import de.geeksfactory.opacclient.utils.ErrorReporter;

/**
 * An activity representing a list of SearchResults. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link SearchResultDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a {@link
 * SearchResultListFragment} and the item details (if present) is a {@link
 * SearchResultDetailFragment}.
 * <p/>
 * This activity also implements the required {@link SearchResultListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class SearchResultListActivity extends OpacActivity implements
        SearchResultListFragment.Callbacks,
        SearchResultDetailFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    protected boolean twoPane;

    protected SearchResultListFragment listFragment;
    protected SearchResultDetailFragment detailFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            setup();
        } else {
            if (savedInstanceState.containsKey("listFragment")) {
                listFragment =
                        (SearchResultListFragment) getSupportFragmentManager()
                                .getFragment(savedInstanceState, "listFragment");
            }
            if (savedInstanceState.containsKey("detailFragment")) {
                detailFragment =
                        (SearchResultDetailFragment) getSupportFragmentManager()
                                .getFragment(savedInstanceState, "detailFragment");
            }
        }

        if (findViewById(R.id.searchresult_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true;
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
     * Callback method from {@link SearchResultListFragment.Callbacks} indicating that the item with
     * the given ID was selected.
     */
    @Override
    public void onItemSelected(SearchResult result, View coverView, int touchX, int touchY) {
        if (result.getChildQuery() != null) {
            app.startSearch(this, result.getChildQuery());
        } else {
            OpacApi api;
            try {
                api = app.getApi();
            } catch (OpacClient.LibraryRemovedException e) {
                return;
            }
            if ((api.getSupportFlags() & OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING) == 0
                    && result.getPage() != listFragment.getLastLoadedPage()) {
                new ReloadOldPageTask(result, coverView).execute();
            } else {
                showDetail(result, coverView, touchX, touchY);
            }
        }
    }

    public void showDetail(SearchResult res, View coverView, int touchX, int touchY) {
        Bitmap cover = BitmapUtils.bitmapFromBytes(res.getCoverBitmap());
        Bitmap smallCover;
        if (cover != null && cover.getWidth() * cover.getHeight() > 300 * 300) {
            // Android's Parcelable implementation doesn't like huge images
            int max = Math.max(cover.getWidth(), cover.getHeight());
            int width = (int) ((300f / max) * cover.getWidth());
            int height = (int) ((300f / max) * cover.getHeight());
            smallCover = Bitmap.createScaledBitmap(cover, width, height, false);
        } else {
            smallCover = cover;
        }

        if (twoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(SearchResultDetailFragment.ARG_ITEM_NR, res.getNr());
            if (res.getId() != null) {
                arguments.putString(SearchResultDetailFragment.ARG_ITEM_ID, res.getId());
            }
            if (res.getCoverBitmap() != null) {
                arguments.putParcelable(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP,
                        smallCover);
            }
            if (res.getType() != null) {
                arguments.putString(SearchResultDetailFragment.ARG_ITEM_MEDIATYPE,
                        res.getType().toString());
            }
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
            if (res.getId() != null) {
                detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
                        res.getId());
            }
            if (res.getType() != null) {
                detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_MEDIATYPE,
                        res.getType().toString());
            }
            if (res.getCoverBitmap() != null) {
                detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP,
                        smallCover);
                detailIntent.putExtra(SearchResultDetailActivity.ARG_TOUCH_POSITION_X, touchX);
                detailIntent.putExtra(SearchResultDetailActivity.ARG_TOUCH_POSITION_Y, touchY);
                @SuppressWarnings("unchecked")
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        new Pair<>(coverView, getString(R.string.transition_cover)),
                        new Pair<>((View) toolbar, getString(R.string.transition_toolbar)));
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

    @Override
    public boolean isTwoPane() {
        return twoPane;
    }

    public class ReloadOldPageTask extends AsyncTask<Void, Void, SearchRequestResult> {
        private SearchResult searchResult;
        private Exception exception;
        private View coverView;
        private int touchX = 0;
        private int touchY = 0;

        public ReloadOldPageTask(SearchResult searchResult, View coverView, int touchX,
                int touchY) {
            this.searchResult = searchResult;
            this.coverView = coverView;
            this.touchX = touchX;
            this.touchY = touchY;
        }

        public ReloadOldPageTask(SearchResult searchResult, View coverView) {
            this.searchResult = searchResult;
            this.coverView = coverView;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected SearchRequestResult doInBackground(Void... voids) {
            try {
                return app.getApi().searchGetPage(searchResult.getPage());
            } catch (IOException | OpacErrorException e) {
                exception = e;
                e.printStackTrace();
            } catch (Exception e) {
                exception = e;
                ErrorReporter.handleException(e);
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
                showDetail(searchResult, coverView, touchX, touchY);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (listFragment != null) {
            getSupportFragmentManager().putFragment(outState, "listFragment", listFragment);
        }
        if (detailFragment != null) {
            getSupportFragmentManager().putFragment(outState, "detailFragment", detailFragment);
        }
    }
}
