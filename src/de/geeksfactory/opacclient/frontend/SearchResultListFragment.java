package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;
import org.apache.http.NoHttpResponseException;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;
import org.json.JSONException;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.frontend.ResultsAdapterEndless.OnLoadMoreListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.AndroidMeaningDetector;
import de.geeksfactory.opacclient.searchfields.MeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;

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
	protected static final String STATE_ACTIVATED_POSITION = "activated_position";

	protected static final String ARG_QUERY = "query";
	protected static final String ARG_VOLUME_QUERY = "volumeQuery";
	protected static final String ARG_GOOGLE_QUERY = "googleQuery";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	protected Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	protected int mActivatedPosition = ListView.INVALID_POSITION;

	protected SearchRequestResult searchresult;

	public ResultsAdapterEndless adapter;

	protected OpacClient app;

	protected int lastLoadedPage;

	protected SearchStartTask st;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 * 
		 * @param nr
		 */
		public void onItemSelected(int nr, String id, int pageToLoad);

		public boolean isTwoPane();
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(int nr, String id, int pageToLoad) {
		}

		public boolean isTwoPane() {
			return false;
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SearchResultListFragment() {
	}

	public static SearchResultListFragment getInstance(Bundle query) {
		SearchResultListFragment frag = new SearchResultListFragment();
		Bundle args = new Bundle();
		args.putBundle(ARG_QUERY, query);
		frag.setArguments(args);
		return frag;
	}

	public static SearchResultListFragment getVolumeSearchInstance(Bundle query) {
		SearchResultListFragment frag = new SearchResultListFragment();
		Bundle args = new Bundle();
		args.putBundle(ARG_VOLUME_QUERY, query);
		frag.setArguments(args);
		return frag;
	}

	public static SearchResultListFragment getGoogleSearchInstance(String query) {
		SearchResultListFragment frag = new SearchResultListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_GOOGLE_QUERY, query);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceSate) {
		setRetainInstance(true);
		setHasOptionsMenu(true);

		return inflater.inflate(R.layout.fragment_searchresult_list);
	}

	public void performsearch() {
		if (getArguments().containsKey(ARG_GOOGLE_QUERY)) {
			String query = getArguments().getString(ARG_GOOGLE_QUERY);
			performGoogleSearch(query);
		} else {
			st = new SearchStartTask();
			if (getArguments().containsKey(ARG_VOLUME_QUERY)) {
				st.execute(
						app,
						OpacClient.bundleToMap(getArguments().getBundle(
								ARG_VOLUME_QUERY)));
			} else {
				st.execute(
						app,
						OpacClient.bundleToQuery(getArguments().getBundle(
								ARG_QUERY)));
			}
		}
	}

	private void performGoogleSearch(final String query) {
		AccountDataSource data = new AccountDataSource(getActivity());
		data.open();
		final List<Account> accounts = data.getAllAccounts();
		data.close();

		if (accounts.size() == 0)
			Toast.makeText(getActivity(), R.string.welcome_select,
					Toast.LENGTH_LONG).show();
		else if (accounts.size() == 1)
			startGoogleSearch(accounts.get(0), query);
		else
			new AlertDialog.Builder(getActivity())
					.setTitle(R.string.account_select)
					.setAdapter(
							new AccountListAdapter(getActivity(), accounts)
									.setHighlightActiveAccount(false),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									startGoogleSearch(accounts.get(which),
											query);
								}
							}).create().show();
	}

	private void startGoogleSearch(Account account, String query) {
		app.setAccount(account.getId());
		new GoogleSearchTask().execute(query);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setActivateOnItemClick(mCallbacks.isTwoPane());

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}

		if (savedInstanceState == null && searchresult == null) {
			performsearch();
		} else if (searchresult != null) {
			if (searchresult.getTotal_result_count() >= 0)
				getSupportActionBar().setSubtitle(
						getString(R.string.result_number,
								searchresult.getTotal_result_count()));
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
		app = (OpacClient) activity.getApplication();
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
		mCallbacks.onItemSelected(searchresult.getResults().get(position)
				.getNr(), searchresult.getResults().get(position).getId(),
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

	public void setSearchResult(SearchRequestResult searchresult) {
		for (SearchResult result : searchresult.getResults()) {
			result.setPage(searchresult.getPage_index());
		}
		if (searchresult.getTotal_result_count() >= 0)
			getSupportActionBar().setSubtitle(
					getString(R.string.result_number,
							searchresult.getTotal_result_count()));

		if (searchresult.getResults().size() == 0
				&& searchresult.getTotal_result_count() == 0) {
			setEmptyText(getString(R.string.no_results));
		}
		this.searchresult = searchresult;
		adapter = new ResultsAdapterEndless(getActivity(), searchresult,
				new OnLoadMoreListener() {
					@Override
					public SearchRequestResult onLoadMore(int page)
							throws Exception {
						SearchRequestResult res = app.getApi().searchGetPage(
								page);
						setLastLoadedPage(page);

						return res;
					}

					@Override
					public void onError(Exception e) {
						if (getActivity() != null) {
							if (e instanceof OpacErrorException) {
								showConnectivityError(e.getMessage());
							} else if (e instanceof NotReachableException) {
								showConnectivityError(getResources().getString(
										R.string.connection_error_detail_nre));
							} else {
								e.printStackTrace();
								showConnectivityError();
							}
						}
					}

					@Override
					public void updateResultCount(int resultCount) {
						/*
						 * When IOpac finds more than 200 results, the real
						 * result count is not known until the second page is
						 * loaded
						 */
						if (resultCount >= 0 && getActivity() != null)
							getSupportActionBar().setSubtitle(
									getString(R.string.result_number,
											resultCount));
					}
				});
		setListAdapter(adapter);
		getListView().setTextFilterEnabled(true);
		setListShown(true);
	}

	public void showConnectivityError() {
		showConnectivityError(null);
	}

	public void showConnectivityError(String description) {
		if (getView() == null || getActivity() == null)
			return;
		final LinearLayout progressContainer = (LinearLayout) getView()
				.findViewById(R.id.progressContainer);
		final FrameLayout errorView = (FrameLayout) getView().findViewById(
				R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(
				R.layout.error_connectivity, errorView);

		((Button) connError.findViewById(R.id.btRetry))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						errorView.removeAllViews();
						setListShown(false);
						progressContainer.setVisibility(View.VISIBLE);
						performsearch();
					}
				});

		if (description != null) {
			((TextView) connError.findViewById(R.id.tvErrBody))
					.setText(description);
		}

		setListShown(false);
		progressContainer.startAnimation(AnimationUtils.loadAnimation(
				getActivity(), R.anim.fade_out));
		connError.startAnimation(AnimationUtils.loadAnimation(getActivity(),
				R.anim.fade_in));
		progressContainer.setVisibility(View.GONE);
		connError.setVisibility(View.VISIBLE);
	}

	public int getLastLoadedPage() {
		return lastLoadedPage;
	}

	public void setLastLoadedPage(int lastLoadedPage) {
		this.lastLoadedPage = lastLoadedPage;
	}

	public class SearchStartTask extends OpacTask<SearchRequestResult> {
		protected Exception exception;

		@Override
		protected SearchRequestResult doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			if (arg0[1] == null)
				return null;
			if (arg0[1] instanceof Map<?, ?>) {
				Map<String, String> query = (Map<String, String>) arg0[1];
				try {
					SearchRequestResult res = app.getApi().volumeSearch(query);
					// Load cover images, if search worked and covers available
					return res;
				} catch (java.net.UnknownHostException e) {
					exception = e;
					e.printStackTrace();
				} catch (java.net.SocketException e) {
					exception = e;
				} catch (NoHttpResponseException e) {
					exception = e;
				} catch (OpacErrorException e) {
					exception = e;
				} catch (InterruptedIOException e) {
					exception = e;
				} catch (Exception e) {
					exception = e;
					ACRA.getErrorReporter().handleException(e);
				}
			} else {
				List<SearchQuery> query = (List<SearchQuery>) arg0[1];

				try {
					SearchRequestResult res = app.getApi().search(query);
					// Load cover images, if search worked and covers available
					return res;
				} catch (java.net.UnknownHostException e) {
					exception = e;
					e.printStackTrace();
				} catch (java.net.SocketException e) {
					exception = e;
				} catch (NoHttpResponseException e) {
					exception = e;
				} catch (OpacErrorException e) {
					exception = e;
				} catch (InterruptedIOException e) {
					exception = e;
				} catch (Exception e) {
					exception = e;
					ACRA.getErrorReporter().handleException(e);
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(SearchRequestResult result) {
			if (result == null) {

				if (exception instanceof OpacErrorException) {
					if (exception.getMessage().equals("is_a_redirect")
							&& getActivity() != null) {
						// Some libraries (SISIS) do not show a result list if
						// only one result
						// is found but instead directly show the result
						// details.
						Intent intent = new Intent(getActivity(),
								SearchResultDetailActivity.class);
						intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID,
								(String) null);
						startActivity(intent);
						getActivity().finish();
						return;
					}

					showConnectivityError(exception.getMessage());
				} else if (exception instanceof NotReachableException) {
					if (getActivity() != null)
						showConnectivityError(getResources().getString(
								R.string.connection_error_detail_nre));
				} else
					showConnectivityError();
			} else {
				loaded(result);
			}
		}
	}

	public class GoogleSearchTask extends
			AsyncTask<String, Void, List<SearchField>> {
		protected Exception exception;
		protected String queryString;

		@Override
		protected List<SearchField> doInBackground(String... arg0) {
			queryString = arg0[0];
			SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(
					app);
			if (dataSource.hasSearchFields(app.getLibrary().getIdent())) {
				return dataSource.getSearchFields(app.getLibrary().getIdent());
			} else {
				try {
					List<SearchField> fields = app.getApi().getSearchFields();
					if (getActivity() == null)
						return null;
					if (fields.size() == 0)
						throw new OpacErrorException(
								getString(R.string.no_fields_found));
					if (app.getApi().shouldUseMeaningDetector()) {
						MeaningDetector md = new AndroidMeaningDetector(
								getActivity(), app.getLibrary());
						for (int i = 0; i < fields.size(); i++) {
							fields.set(i, md.detectMeaning(fields.get(i)));
						}
						Collections.sort(fields,
								new SearchField.OrderComparator());
					}
					return fields;
				} catch (JSONException e) {
					exception = e;
					e.printStackTrace();
				} catch (OpacErrorException e) {
					exception = e;
					e.printStackTrace();
				} catch (IOException e) {
					exception = e;
					e.printStackTrace();
				}
				return null;
			}
		}

		protected void onPostExecute(List<SearchField> result) {
			if (getActivity() == null)
				return;
			if (exception != null) {
				if (exception instanceof OpacErrorException)
					showConnectivityError(exception.getMessage());
				else
					showConnectivityError();
				return;
			}
			SearchField fieldToUse = findSearchFieldByMeaning(result,
					Meaning.FREE);
			if (fieldToUse == null)
				fieldToUse = findSearchFieldByMeaning(result, Meaning.TITLE);
			if (fieldToUse == null) {
				showConnectivityError(getString(R.string.no_fields_found));
				return;
			}
			List<SearchQuery> query = new ArrayList<SearchQuery>();
			query.add(new SearchQuery(fieldToUse, queryString));
			st = new SearchStartTask();
			st.execute(app, query);
		}

		private SearchField findSearchFieldByMeaning(List<SearchField> fields,
				Meaning meaning) {
			for (SearchField field : fields) {
				if (field.getMeaning() == meaning)
					return field;
			}
			return null;
		}
	}

	public void loaded(SearchRequestResult searchresult) {
		try {
			setListShown(true);
			setSearchResult(searchresult);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

}
