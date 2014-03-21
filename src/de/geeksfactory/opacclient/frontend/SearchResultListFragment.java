package de.geeksfactory.opacclient.frontend;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

/**
 * A list fragment representing a list of SearchResults. This fragment also
 * supports tablet devices by allowing list items to be given an 'activated'
 * state upon selection. This helps indicate which item is currently being
 * viewed in a {@link SearchResultDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class SearchResultListFragment extends ListFragment {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	private SearchRequestResult searchresult;

	public ResultsAdapter adapter;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 * @param nr 
		 */
		public void onItemSelected(int nr, String id, int page);
		public void reload();
		public void loadMoreData(int page);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(int nr, String id, int page) {
		}
		@Override
		public void reload() {			
		}
		@Override
		public void loadMoreData(int page) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SearchResultListFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceSate) {
	
		setRetainInstance(true);
		
		return inflater.inflate(R.layout.fragment_searchresult_list);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(searchresult.getResults().get(position).getNr(),
				searchresult.getResults().get(position).getId(),
				searchresult.getResults().get(position).getPage());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != AdapterView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == AdapterView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	public void setSearchResult(SearchRequestResult searchresult, boolean clear) {
		for(SearchResult result:searchresult.getResults()) {
			result.setPage(searchresult.getPage_index());
		}
		if(clear) {
			if (searchresult.getTotal_result_count() >= 0)
				getSupportActionBar().setSubtitle(
						getString(R.string.result_number,
								searchresult.getTotal_result_count()));
	
			if (searchresult.getResults().size() == 0
					&& searchresult.getTotal_result_count() == 0) {
				setEmptyText(getString(R.string.no_results));
			}
			this.searchresult = searchresult;
			adapter = new ResultsAdapter(getActivity(), (searchresult.getResults()));
			setListAdapter(adapter);
			getListView().setTextFilterEnabled(true);
			getListView().setOnScrollListener(new EndlessScrollListener() {	
				@Override
				public void onLoadMore(int page, int totalItemsCount) {
					Log.d("Opac", "total: " + String.valueOf(SearchResultListFragment.this.searchresult.getTotal_result_count()));
					Log.d("Opac", "current: " + String.valueOf(totalItemsCount));
					Log.d("Opac", String.valueOf(totalItemsCount < SearchResultListFragment.this.searchresult.getTotal_result_count()));
					if(totalItemsCount < SearchResultListFragment.this.searchresult.getTotal_result_count()); {
						mCallbacks.loadMoreData(page);
					}
				}		
			});
			setListShown(true);
		} else {
			adapter.addAll(searchresult.getResults());
			adapter.notifyDataSetChanged();
		}
	}
	
	public void showConnectivityError() {
		showConnectivityError(null);
	}
	
	public void showConnectivityError(String description) {
		LinearLayout progressContainer = (LinearLayout) getView().findViewById(R.id.progressContainer);
		final FrameLayout errorView = (FrameLayout) getView().findViewById(R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(R.layout.error_connectivity, errorView);
		
		((Button) connError.findViewById(R.id.btRetry))
		.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				errorView.removeAllViews();
				setListShown(false);
				mCallbacks.reload();
			}
		});
		
		if(description != null) {
			((TextView) connError.findViewById(R.id.tvErrBody))
			.setText(description);
		}
		
		progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out));
		connError.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
		progressContainer.setVisibility(View.GONE);
		connError.setVisibility(View.VISIBLE);
	}
	
}
