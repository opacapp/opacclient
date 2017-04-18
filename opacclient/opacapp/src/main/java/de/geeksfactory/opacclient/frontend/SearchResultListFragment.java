package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.CustomListFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.frontend.ResultsAdapterEndless.OnLoadMoreListener;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;
import de.geeksfactory.opacclient.utils.ErrorReporter;

/**
 * A list fragment representing a list of SearchResults. This fragment also supports tablet devices
 * by allowing list items to be given an 'activated' state upon selection. This helps indicate which
 * item is currently being viewed in a {@link SearchResultDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks} interface.
 */
public class SearchResultListFragment extends CustomListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the activated item position.
     * Only used on tablets.
     */
    protected static final String STATE_ACTIVATED_POSITION = "activated_position";

    protected static final String ARG_QUERY = "query";
    protected static final String ARG_VOLUME_QUERY = "volumeQuery";
    protected static final String ARG_GOOGLE_QUERY = "googleQuery";
    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when
     * this fragment is not attached to an activity.
     */
    private static Callbacks dummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(SearchResult result, View coverView, int touchX, int touchY) {
        }

        public boolean isTwoPane() {
            return false;
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    protected Callbacks callbacks = dummyCallbacks;
    public ResultsAdapterEndless adapter;
    /**
     * The current activated item position. Only used on tablets.
     */
    protected int activatedPosition = ListView.INVALID_POSITION;
    protected SearchRequestResult searchresult;
    protected OpacClient app;
    protected int lastLoadedPage;
    protected SearchStartTask st;
    protected LinearLayout progressContainer;
    protected FrameLayout errorView;
    private int touchPositionX = 0;
    private int touchPositionY = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
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
        View view = inflater.inflate(R.layout.fragment_searchresult_list, container, false);
        progressContainer = (LinearLayout) view
                .findViewById(R.id.progressContainer);
        errorView = (FrameLayout) view.findViewById(
                R.id.error_view);
        setupIds(view);
        return view;
    }

    public void performsearch() {
        if (getArguments().containsKey(ARG_GOOGLE_QUERY)) {
            String query = getArguments().getString(ARG_GOOGLE_QUERY);
            performGoogleSearch(query);
        } else {
            if (getArguments().containsKey(ARG_VOLUME_QUERY)) {
                st = new SearchStartTask(OpacClient.bundleToMap(getArguments().getBundle(
                        ARG_VOLUME_QUERY)));
            } else {
                st = new SearchStartTask(OpacClient.bundleToQuery(getArguments().getBundle(
                        ARG_QUERY)));
            }
            st.execute();
        }
    }

    private void performGoogleSearch(final String query) {
        AccountDataSource data = new AccountDataSource(getActivity());
        final List<Account> accounts = data.getAllAccounts();

        if (accounts.size() == 0) {
            Toast.makeText(getActivity(), R.string.welcome_select,
                    Toast.LENGTH_LONG).show();
        } else if (accounts.size() == 1) {
            startGoogleSearch(accounts.get(0), query);
        } else {
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
    }

    private void startGoogleSearch(Account account, String query) {
        app.setAccount(account.getId());
        new GoogleSearchTask().execute(query);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setActivateOnItemClick(callbacks.isTwoPane());

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }

        if (savedInstanceState == null && searchresult == null) {
            performsearch();
        } else if (searchresult != null) {
            if (searchresult.getTotal_result_count() >= 0) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(
                        getResources().getQuantityString(R.plurals.result_number,
                                searchresult.getTotal_result_count(),
                                searchresult.getTotal_result_count()));
            }
        }

        getListView().setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                touchPositionX = (int) event.getX();
                touchPositionY = (int) event.getY();
                return false;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        callbacks = (Callbacks) activity;
        app = (OpacClient) activity.getApplication();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        callbacks = dummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
            long id) {
        super.onListItemClick(listView, view, position, id);

        setActivatedPosition(position);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        callbacks.onItemSelected(searchresult.getResults().get(position),
                view.findViewById(R.id.ivType), touchPositionX, touchPositionY);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activatedPosition != AdapterView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
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
            getListView().setItemChecked(activatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    public void setSearchResult(SearchRequestResult searchresult) {
        for (SearchResult result : searchresult.getResults()) {
            result.setPage(searchresult.getPage_index());
        }
        if (searchresult.getTotal_result_count() >= 0) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(
                    getResources().getQuantityString(R.plurals.result_number,
                            searchresult.getTotal_result_count(),
                            searchresult.getTotal_result_count()));
        }

        if (searchresult.getResults().size() == 0
                && searchresult.getTotal_result_count() == 0) {
            setEmptyText(getString(R.string.no_results));
        }
        this.searchresult = searchresult;
        OpacApi api = null;
        try {
            api = app.getApi();
        } catch (OpacClient.LibraryRemovedException ignored) {

        }
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
                            } else if (e instanceof SSLSecurityException) {
                                showConnectivityError(getResources().getString(
                                        R.string.connection_error_detail_security));
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

                        {
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(
                                    getResources().getQuantityString(R.plurals.result_number,
                                            resultCount, resultCount));
                        }
                    }
                }, api);
        setListAdapter(adapter);
        getListView().setTextFilterEnabled(true);
        setListShown(true);
    }

    public void showConnectivityError() {
        showConnectivityError(null);
    }

    public void showConnectivityError(String description) {
        if (getView() == null || getActivity() == null) {
            return;
        }
        errorView.removeAllViews();
        View connError = getActivity().getLayoutInflater().inflate(
                R.layout.error_connectivity, errorView);

        connError.findViewById(R.id.btRetry)
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
                getActivity(), android.R.anim.fade_out));
        connError.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                android.R.anim.fade_in));
        progressContainer.setVisibility(View.GONE);
        connError.setVisibility(View.VISIBLE);
    }

    public int getLastLoadedPage() {
        return lastLoadedPage;
    }

    public void setLastLoadedPage(int lastLoadedPage) {
        this.lastLoadedPage = lastLoadedPage;
    }

    public void loaded(SearchRequestResult searchresult) {
        try {
            if (searchresult.getPage_index() == 0 && searchresult.getTotal_result_count() > 0
                    && searchresult.getResults().size() == 0) {
                showConnectivityError(getResources().getString(R.string.connection_error_detail));
            }
            setListShown(true);
            setSearchResult(searchresult);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * A callback interface that all activities containing this fragment must implement. This
     * mechanism allows activities to be notified of item selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(SearchResult result, View coverView, int touchX, int touchY);

        public boolean isTwoPane();
    }

    public class SearchStartTask extends AsyncTask<Void, Void, SearchRequestResult> {
        protected Exception exception;
        protected Map<String, String> volumeQuery = null;
        protected List<SearchQuery> query = null;

        public SearchStartTask(Map<String, String> volumeQuery) {
            this.volumeQuery = volumeQuery;
        }

        public SearchStartTask(List<SearchQuery> query) {
            this.query = query;
        }

        @Override
        protected SearchRequestResult doInBackground(Void... voids) {
            OpacApi api;
            try {
                api = app.getApi();
            } catch (OpacClient.LibraryRemovedException e) {
                exception = e;
                return null;
            }
            if (volumeQuery != null) {
                try {
                    return api.volumeSearch(volumeQuery);
                } catch (IOException | OpacErrorException e) {
                    exception = e;
                    e.printStackTrace();
                } catch (Exception e) {
                    exception = e;
                    ErrorReporter.handleException(e);
                }
            } else if (query != null) {
                try {
                    // Load cover images, if search worked and covers available
                    return api.search(query);
                } catch (IOException | OpacErrorException e) {
                    exception = e;
                    e.printStackTrace();
                } catch (Exception e) {
                    exception = e;
                    ErrorReporter.handleException(e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(SearchRequestResult result) {
            if (result == null) {

                if (exception instanceof OpacErrorException) {
                    showConnectivityError(exception.getMessage());
                } else if (exception instanceof OpacClient.LibraryRemovedException) {
                    if (getActivity() != null) {
                        showConnectivityError(getResources().getString(
                                R.string.library_removed_error));
                    }
                } else if (exception instanceof SSLSecurityException) {
                    if (getActivity() != null) {
                        showConnectivityError(getResources().getString(
                                R.string.connection_error_detail_security));
                    }
                } else if (exception instanceof NotReachableException) {
                    if (getActivity() != null) {
                        showConnectivityError(getResources().getString(
                                R.string.connection_error_detail_nre));
                    }
                } else {
                    showConnectivityError();
                }
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
                    if (getActivity() == null) {
                        return null;
                    }
                    if (fields.size() == 0) {
                        throw new OpacErrorException(
                                getString(R.string.no_fields_found));
                    }
                    return fields;
                } catch (JSONException | IOException | OpacErrorException | OpacClient
                        .LibraryRemovedException e) {
                    exception = e;
                    e.printStackTrace();
                }
                return null;
            }
        }

        protected void onPostExecute(List<SearchField> result) {
            if (getActivity() == null) {
                return;
            }
            if (exception != null) {
                if (exception instanceof OpacErrorException) {
                    showConnectivityError(exception.getMessage());
                } else {
                    showConnectivityError();
                }
                return;
            }
            SearchField fieldToUse = findSearchFieldByMeaning(result,
                    Meaning.FREE);
            if (fieldToUse == null) {
                fieldToUse = findSearchFieldByMeaning(result, Meaning.TITLE);
            }
            if (fieldToUse == null) {
                showConnectivityError(getString(R.string.no_fields_found));
                return;
            }
            List<SearchQuery> query = new ArrayList<>();
            query.add(new SearchQuery(fieldToUse, queryString));
            st = new SearchStartTask(query);
            st.execute();
        }

        private SearchField findSearchFieldByMeaning(List<SearchField> fields,
                Meaning meaning) {
            for (SearchField field : fields) {
                if (field.getMeaning() == meaning) {
                    return field;
                }
            }
            return null;
        }
    }

}
