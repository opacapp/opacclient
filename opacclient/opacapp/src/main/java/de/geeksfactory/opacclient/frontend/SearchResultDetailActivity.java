package de.geeksfactory.opacclient.frontend;

import android.os.Bundle;

import de.geeksfactory.opacclient.R;

/**
 * An activity representing a single SearchResult detail screen. This activity
 * is only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a
 * {@link SearchResultListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link SearchResultDetailFragment}.
 */
public class SearchResultDetailActivity extends OpacActivity implements SearchResultDetailFragment.Callbacks {

    SearchResultDetailFragment detailFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(
                    SearchResultDetailFragment.ARG_ITEM_NR,
                    getIntent().getIntExtra(
                            SearchResultDetailFragment.ARG_ITEM_NR, 0));
            if (getIntent().hasExtra(
                    SearchResultDetailFragment.ARG_ITEM_ID))
                arguments.putString(
                        SearchResultDetailFragment.ARG_ITEM_ID,
                        getIntent().getStringExtra(
                                SearchResultDetailFragment.ARG_ITEM_ID));
            if (getIntent().hasExtra(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP)) {
                arguments.putParcelable(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP, getIntent().getParcelableExtra(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP));
            }
            detailFragment = new SearchResultDetailFragment();
            detailFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.searchresult_detail_container, detailFragment).commit();
        }
    }

    @Override
    public void removeFragment() {
        supportFinishAfterTransition();
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_searchresult_detail;
    }
}
