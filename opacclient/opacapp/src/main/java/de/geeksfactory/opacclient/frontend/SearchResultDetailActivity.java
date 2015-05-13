package de.geeksfactory.opacclient.frontend;

import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionSet;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.ui.CircularRevealTransition;
import de.geeksfactory.opacclient.ui.VerticalExplodeTransition;

/**
 * An activity representing a single SearchResult detail screen. This activity is only used on
 * handset devices. On tablet-size devices, item details are presented side-by-side with a list of
 * items in a {@link SearchResultListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing more than a {@link
 * SearchResultDetailFragment}.
 */
public class SearchResultDetailActivity extends OpacActivity
        implements SearchResultDetailFragment.Callbacks {

    public static final String ARG_TOUCH_POSITION_X = "touchX";
    public static final String ARG_TOUCH_POSITION_Y = "touchY";
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
                    SearchResultDetailFragment.ARG_ITEM_ID)) {
                arguments.putString(
                        SearchResultDetailFragment.ARG_ITEM_ID,
                        getIntent().getStringExtra(
                                SearchResultDetailFragment.ARG_ITEM_ID));
            }
            if (getIntent().hasExtra(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP)) {
                arguments.putParcelable(SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP,
                        getIntent().getParcelableExtra(
                                SearchResultDetailFragment.ARG_ITEM_COVER_BITMAP));
            }
            detailFragment = new SearchResultDetailFragment();
            detailFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                                       .add(R.id.searchresult_detail_container, detailFragment)
                                       .commit();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int touchX = getIntent().getIntExtra(ARG_TOUCH_POSITION_X, 0);
                int touchY = getIntent().getIntExtra(ARG_TOUCH_POSITION_Y, 0);

                TransitionSet enterTransition = new TransitionSet();
                enterTransition.addTransition(new CircularRevealTransition()
                        .setStartPosition(this, touchX, touchY)
                        .addTarget(R.id.coverBackground)
                        .addTarget(R.id.gradient_top)
                        .addTarget(R.id.gradient_bottom));
                enterTransition.addTransition(
                        new VerticalExplodeTransition()
                                .excludeTarget(R.id.coverBackground, true)
                                .excludeTarget(R.id.gradient_bottom, true)
                                .excludeTarget(R.id.gradient_top, true));
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true);
                getWindow().setEnterTransition(enterTransition);

                TransitionSet exitTransition = new TransitionSet();
                exitTransition.addTransition(
                        new Fade().addTarget(R.id.gradient_bottom).addTarget(R.id.gradient_top));
                exitTransition.addTransition(new VerticalExplodeTransition()
                        .excludeTarget(R.id.gradient_bottom, true)
                        .excludeTarget(R.id.gradient_top, true));
                exitTransition.excludeTarget(android.R.id.statusBarBackground, true);
                getWindow().setReturnTransition(exitTransition);
                getWindow().setSharedElementsUseOverlay(false);
            }
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
