/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.Starred;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.Branch;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;
import de.geeksfactory.opacclient.storage.StarBranchItem;
import de.geeksfactory.opacclient.storage.StarContentProvider;
import de.geeksfactory.opacclient.storage.StarDataSource;
import de.geeksfactory.opacclient.storage.StarDatabase;
import de.geeksfactory.opacclient.utils.CompatibilityUtils;

public class StarredFragment extends Fragment implements
        LoaderCallbacks<Cursor>, AccountSelectedListener {

    private static final Logger LOGGER = new Logger(StarredFragment.class);

    public static final String STATE_ACTIVATED_POSITION = "activated_position";
    public static final String STATE_FILTER_BRANCH = "filter_branch";
    public static final String STATE_FILTER_BIB = "filter_bib";

    private static final String JSON_LIBRARY_NAME = "library_name";
    private static final String JSON_STARRED_LIST = "starred_list";
    private static final String JSON_ITEM_MNR = "item_mnr";
    private static final String JSON_ITEM_TITLE = "item_title";
    private static final String JSON_ITEM_MEDIATYPE = "item_mediatype";
    private static final String JSON_ITEM_BRANCHES = "item_branches";
    private static final int REQUEST_CODE_EXPORT = 123;
    private static final int REQUEST_CODE_IMPORT = 124;
    private static final int LOADER_ID = 0; // !=1 wie bei History

    protected View view;
    protected OpacClient app;
    private ItemListAdapter adapter;
    private Callback callback;
    private ListView listView;
    private TextView tvWelcome;
    private TextView tvHeader;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int FILTER_BRANCH_NONE = -1;
    private static final int FILTER_BRANCH_ALL = 0;
    private int currentFilterBranchId;

    private static class Logger {
        private String tag;

        public Logger(@NonNull Class<?> clazz) {
            tag = clazz.getSimpleName();
            if (tag.length()>23) {
                tag = tag.substring(0, 23);
            }
        }

        public void d(String fmt, Object ... args) {
            if (!Log.isLoggable(tag, Log.INFO)) {
                return;
            }
            String msg = String.format(fmt, args);
            Log.i(tag, msg);
        }
    }

    private static void logDebug(String format, Object ... args) {
        LOGGER.d(format, args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        view = inflater.inflate(R.layout.fragment_starred, container, false);
        app = (OpacClient) getActivity().getApplication();

        adapter = new ItemListAdapter();

        listView = view.findViewById(R.id.lvStarred);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvHeader = view.findViewById(R.id.tvStarredHeader);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        swipeRefreshLayout = view.findViewById(R.id.swipeStarred);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_red);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                logDebug("onRefresh called. currentFilterBranchId = %d",currentFilterBranchId);
                refreshBranches();
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Starred item = (Starred) view.findViewById(R.id.ivDelete)
                                             .getTag();
                if (item.getMNr() == null || item.getMNr().equals("null")
                        || item.getMNr().equals("")) {
                    // keine eindeutige Id, via search
                    startSearch(item);
                } else {
                    // warum ?
                    // getActivity().invalidateOptionsMenu();
                    callback.showDetail(item.getMNr(), item.getMediaType());
                }
            }
        });
        listView.setClickable(true);
        listView.setTextFilterEnabled(true);

        getActivity().getSupportLoaderManager()
                     .initLoader(LOADER_ID, null, this);
        listView.setAdapter(adapter);

        restoreState(savedInstanceState);

        setActivateOnItemClick(((OpacActivity) getActivity()).isTablet());

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getContext());

        String bib = app.getLibrary().getIdent();
        String savedBib = sp.getString(STATE_FILTER_BIB, null);
        if (!bib.equals(savedBib)) {
            // Status gehört zu einer anderen Bibliothek,
            // daher nichts restoren. Fertig!
            return;
        }

        currentFilterBranchId = sp.getInt(STATE_FILTER_BRANCH, FILTER_BRANCH_ALL);
        if (sp.contains(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(sp
                    .getInt(STATE_ACTIVATED_POSITION, AdapterView.INVALID_POSITION));
        }

        if (savedInstanceState != null) {
            // Restore the previously serialized activated item position.
            if (savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
                setActivatedPosition(savedInstanceState
                        .getInt(STATE_ACTIVATED_POSITION));
            }

            if (savedInstanceState.containsKey(STATE_FILTER_BRANCH)) {
                currentFilterBranchId = savedInstanceState.getInt(STATE_FILTER_BRANCH);
            }
        }
    }

    private void startSearch(Starred item) {

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        List<SearchField> fields = new JsonSearchFieldDataSource(
                app).getSearchFields(app.getLibrary().getIdent());

        if (fields == null) {
            Toast.makeText(getActivity(), R.string.no_search_cache,
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<SearchQuery> query = new ArrayList<>();

        SearchField title_field = null, free_field = null, category_field = null;
        for (SearchField field : fields) {
            if (field.getMeaning() == Meaning.TITLE) {
                title_field = field;
            } else if (field.getMeaning() == Meaning.FREE) {
                free_field = field;
//            } else if (field.getMeaning() == Meaning.CATEGORY) {
//                category_field = field;
            } else if (field.getMeaning() == Meaning.HOME_BRANCH) {
                query.add(new SearchQuery(field, sp.getString(
                        OpacClient.PREF_HOME_BRANCH_PREFIX
                                + app.getAccount().getId(),
                        null)));
            }
        }
        if (title_field != null) {
            query.add(new SearchQuery(title_field, item
                    .getTitle()));
        } else if (free_field != null) {
            query.add(new SearchQuery(free_field, item
                    .getTitle()));
        }
        /*
        if (category_field != null) {
            // Leider nicht so einfach
            // category ist ein Drop-Down;
            // Text-MediaType-Zuordnung in Adis protected Hashmap types und mehrdeutig
            query.add(new SearchQuery(category_field, item
                    .getMediaType().toString()));
        }*/
        app.startSearch(getActivity(), query);
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu,
            MenuInflater inflater) {

        inflater.inflate(R.menu.activity_starred, menu);

        // FilterMenu um Branches erweitern
        if (true) {
            addSubMenuBranchesFromDb(menu);
        } else {
            addSubMenuBranchesFromSearchField(menu);
        }
        enableRefreshBranches(menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void enableRefreshBranches(android.view.Menu menu) {
        StarDataSource data = new StarDataSource(getActivity());
        String bib = app.getLibrary().getIdent();

        int countItemWithValidMnr = data.getCountItemWithValidMnr(bib);

        // MenuItem verstecken
        MenuItem item = menu.findItem(R.id.action_refresh_branches);
        item.setVisible(countItemWithValidMnr > 0);
        // No SwipeRefresh
        swipeRefreshLayout.setEnabled(countItemWithValidMnr > 0);
    }

    private void addSubMenuBranchesFromDb(android.view.Menu menu) {
        MenuItem itemFilter = menu.findItem(R.id.action_filter);
        // menu.addSubMenu(Menu.NONE, R.id.action_filter, Menu.NONE,"Menu1");
        SubMenu subMenu = itemFilter.getSubMenu();

        StarDataSource data = new StarDataSource(getActivity());
        String bib = app.getLibrary().getIdent();
        List<Branch> branches = data.getStarredBranches(bib);

        int countStarredWithoutBranch = data.getCountStarredWithoutBranch(bib);
        logDebug("addSubMenuBranchesFromDb - branches.size = %s, countStarredWithoutBranch = %s"
                , branches.size(), countStarredWithoutBranch);

        if (((null == branches) || branches.isEmpty()) && (countStarredWithoutBranch == 0)){
            // auch wenn noch keine Branches zugeordnet sind Filter anzeigen?
            // Ja, damit auf "Ohne Branch selektiert werden kann und
            // diese Auswahl dann refresh werden kann
            itemFilter.setVisible(true);
            return;
        }

        MenuItem itemFilterNoBranch = menu.findItem(R.id.action_filter_no_branch);
        if (countStarredWithoutBranch > 0) {
            String text = getString(R.string.starred_filter_no_branch_count, countStarredWithoutBranch);
            itemFilterNoBranch.setTitle(text);
            itemFilterNoBranch.setVisible(true);
        } else {
            itemFilterNoBranch.setVisible(false);
        }

        final int groupId = Menu.NONE;
        for (Branch branch : branches) {
            String text = String.format("%s (%d)", branch.getName(), branch.getCount());
            MenuItem menuItem = subMenu.add(groupId, branch.getId(), Menu.NONE, text);
            menuItem.setCheckable(false);
        }
    }

    private void addSubMenuBranchesFromSearchField(android.view.Menu menu) {
        MenuItem itemFilter = menu.findItem(R.id.action_filter);
        // menu.addSubMenu(Menu.NONE, R.id.action_filter, Menu.NONE,"Menu1");
        SubMenu subMenu = itemFilter.getSubMenu();
        SearchField field = findSearchField(Meaning.BRANCH);
        if (null == field) {
            itemFilter.setVisible(false);
            return;
        }

        if (!(field instanceof DropdownSearchField)) {
            itemFilter.setVisible(false);
            return;
        }

        DropdownSearchField ddSearchField = (DropdownSearchField) field;
        if ((ddSearchField.getDropdownValues() == null) || ddSearchField.getDropdownValues().isEmpty()){
            itemFilter.setVisible(false);
            return;
        }

        // final int groupId = Menu.NONE;
        // final int groupId = R.id.group_filter;
        // subMenu.setGroupCheckable(groupId, true, true);
        // final RadioGroup radioGroup = (RadioGroup) menu.findViewById(R.id.group_filter);
        for (DropdownSearchField.Option value : ddSearchField.getDropdownValues()) {
            // MenuItem menuItem = subMenu.add(groupId, R.id.action_filter_branch, Menu.NONE,value.getValue());
            MenuItem menuItem = subMenu.add(value.getValue());
            menuItem.setCheckable(true);
        }
    }

    private SearchField findSearchField(Meaning meaning) {
        SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(app);
        List<SearchField> fields = dataSource.getSearchFields(app.getLibrary().getIdent());

        for (SearchField field : fields) {
            if (field.getMeaning() == meaning) {
                return field;
            }
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            share();
            return true;
        } else if (item.getItemId() == R.id.action_export_to_storage) {
            exportToStorage();
            return true;
        } else if (item.getItemId() == R.id.action_import_from_storage) {
            importFromStorage();
            refreshViewAfterChange();
            return true;
        } else if (item.getItemId() == R.id.action_refresh_branches) {
            swipeRefreshLayout.setRefreshing(true);
            refreshBranches();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            return super.onOptionsItemSelected(item);
        } else if (item.getItemId() == R.id.action_remove_all) {
            if (true) {
                removeAllWithAlertDialog();
            } else {
                removeAllWithSnackbar();
            }
            refreshViewAfterChange();
            return true;
        } else if (item.getItemId() == R.id.action_filter_no_branch) {
            currentFilterBranchId = FILTER_BRANCH_NONE;
            refreshViewAfterChange();
            return true;

        } else if (item.getItemId() == R.id.action_filter_all) {
            // clear selection
            currentFilterBranchId = FILTER_BRANCH_ALL;
            refreshViewAfterChange();
            return true;

        } else {
            // Hier FilterSubMenu
            if (item.getItemId() == currentFilterBranchId) {
                // Bereits ausgewählt:  Do nothing
                return true;
            } else {
                // new selection
                currentFilterBranchId = item.getItemId();

                StarDataSource data = new StarDataSource(getActivity());
                data.updateBranchFiltertimestamp(item.getItemId(), new Date().getTime());

                refreshViewAfterChange();
                return true;
            }
        }
    }

    @Override
    public void accountSelected(Account account) {
        currentFilterBranchId = FILTER_BRANCH_ALL;
        refreshViewAfterChange();
    }

    private void refreshViewAfterChange() {
        updateHeader();
        getActivity().invalidateOptionsMenu();
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    private void refreshBranches() {
        if (currentFilterBranchId == FILTER_BRANCH_ALL) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        FetchBranchesTask ft = new FetchBranchesTask(currentFilterBranchId);
        ft.execute();
    }


    //Added code to show SnackBar when clicked on Remove button in Favorites screen
    private void remove(Starred item) {
        Snackbar snackbar =
                Snackbar.make(view, getString(R.string.starred_removed), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.starred_removed_undo, new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                /*
                StarDataSource data = new StarDataSource(getActivity());
                String bib = app.getLibrary().getIdent();
                data.star(sItem.getMNr(), sItem.getTitle(), bib, sItem.getMediaType());
                 */
            }
        });

        // https://stackoverflow.com/questions/30926380/how-can-i-be-notified-when-a-snackbar-has-dismissed-itself
        snackbar.addCallback(new Snackbar.Callback() {

            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                //see Snackbar.Callback docs for event details
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    // Snackbar closed on its own
                    // also kein Undo, also remove item!
                    StarDataSource data = new StarDataSource(getActivity());
                    String bib = app.getLibrary().getIdent();
                    data.remove(item);

                    refreshViewAfterChange();
                }
            }

        });
        snackbar.show();
    }

    private void removeAllWithSnackbar() {
        // TODO ev. besser mit Dialog? https://material.io/components/dialogs
        Snackbar snackbar =
                Snackbar.make(view, getString(R.string.starred_remove_all_sure), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.starred_remove_all_ok, new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                StarDataSource data = new StarDataSource(getActivity());
                String bib = app.getLibrary().getIdent();
                data.removeAll(bib);

                currentFilterBranchId = FILTER_BRANCH_ALL;
                refreshViewAfterChange();
            }
        });
        snackbar.show();
    }

    // Bei AccountEdit in Activity sein
    private void removeAllWithAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.starred_remove_all_sure)
               .setCancelable(true)
               .setNegativeButton(R.string.no,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface d, int id) {
                               d.cancel();
                           }
                       })
               .setPositiveButton(R.string.delete,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface d, int id) {
                               d.dismiss();
                               StarDataSource data = new StarDataSource(getActivity());
                               String bib = app.getLibrary().getIdent();
                               data.removeAll(bib);

                               currentFilterBranchId = FILTER_BRANCH_ALL;

                               refreshViewAfterChange();
                           }
                       })
               .setOnCancelListener(
                       new DialogInterface.OnCancelListener() {
                           @Override
                           public void onCancel(DialogInterface d) {
                               if (d != null) {
                                   d.cancel();
                               }
                           }
                       });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        logDebug("onCreateLoader - currentFilterBranchId = %s", currentFilterBranchId);

        if (app.getLibrary() == null) {
            return null;
        }

        final String bib = app.getLibrary().getIdent();
        final String[] projection = {"starred.id AS _id", "medianr", "starred.bib AS bib", "title", "mediatype"
                , "id_branch", "status", "statusTime", "returnDate"};

        if (currentFilterBranchId == FILTER_BRANCH_ALL) {
            // Nur auf Library selektieren
            return new CursorLoader(getActivity(),
                    StarContentProvider.STAR_JOIN_STAR_BRANCH_URI,
                    projection, // StarDatabase.COLUMNS,
                    StarDatabase.STAR_WHERE_LIB,
                    new String[]{bib},
                    null);
        } else if (currentFilterBranchId == FILTER_BRANCH_NONE) {
            // Auf Library und id_branch = null selektieren
            return new CursorLoader(getActivity(),
                    StarContentProvider.STAR_JOIN_STAR_BRANCH_URI,
                    projection,
                    StarDatabase.STAR_WHERE_LIB_BRANCH_IS_NULL,
                    new String[]{bib},
                    null);
        } else {
            // Auf Library und id_branch selektieren
            return new CursorLoader(getActivity(),
                    StarContentProvider.STAR_JOIN_STAR_BRANCH_URI,
                    projection,
                    StarDatabase.STAR_WHERE_LIB_BRANCH,
                    new String[]{bib, Integer.toString(currentFilterBranchId)},
                    null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        logDebug("onCreateLoader- cursor.getCount() = %d", cursor.getCount());
        adapter.swapCursor(cursor);
        if (cursor.getCount() == 0) {
            tvWelcome.setVisibility(View.VISIBLE);
        } else {
            tvWelcome.setVisibility(View.GONE);
            updateHeader();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        logDebug("onLoaderReset- loader = %s", loader);
        adapter.swapCursor(null);
    }

    private void updateHeader() {
        // getString needs context
        if (getContext() == null) {
            return;
        }

        String text = null;
        int countItems = adapter.getCount();
        if (currentFilterBranchId == FILTER_BRANCH_ALL) {
            text = getString(R.string.starred_header, countItems);
        } else if (currentFilterBranchId == FILTER_BRANCH_NONE) {
            text = getString(R.string.starred_without_branch, countItems);
        } else {
            StarDataSource data = new StarDataSource(getActivity());
            String bib = app.getLibrary().getIdent();
            Branch branch = data.getBranch(bib, currentFilterBranchId);
            if (branch == null) {
                logDebug("No Branch for bib=%s branchId=%d", bib, currentFilterBranchId);
            }
            text = getString(R.string.starred_header_branch, countItems,
                    branch.getName());
            long minStatusTime = branch.getMinStatusTime();
            if (minStatusTime>0) {
                text = text + " (" + getAge(minStatusTime) + ")";
            }
        }
        tvHeader.setText(text);
    }

    private String getAge(long minStatusTime) {
        // age in milliseconds
        long age = System.currentTimeMillis() - minStatusTime;

        age /= 1000; // age in seconds
        if (age < 60) {
            // less than a minute
            return getResources().getString(R.string.starred_up_to_date);
        }
        age /= 60; // age in minutes
        if (age < 60) {
            // less than an hour
            return getResources()
                    .getQuantityString(R.plurals.starred_age_minutes, (int) age, (int) age);
        }
        age /= 60; // age in hours
        if (age < 24) {
            // less than a day
            return getResources()
                    .getQuantityString(R.plurals.starred_age_hours, (int) age, (int) age);
        }
        // more than 1 day

        age /= 24; // age in days
        return  getResources().getQuantityString(R.plurals.starred_age_days, (int) age, (int) age);
    }

    protected void share() {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(CompatibilityUtils.getNewDocumentIntentFlag());

        StringBuilder text = new StringBuilder();

        StarDataSource data = new StarDataSource(getActivity());
        List<Starred> items = data.getAllItems(app.getLibrary().getIdent());
        for (Starred item : items) {
            text.append(item.getTitle());
            text.append("\n");
            String shareUrl;
            try {
                shareUrl = app.getApi().getShareUrl(item.getMNr(),
                        item.getTitle());
            } catch (OpacClient.LibraryRemovedException e) {
                return;
            }
            if (shareUrl != null) {
                text.append(shareUrl);
                text.append("\n");
            }
            text.append("\n");
        }

        intent.putExtra(Intent.EXTRA_TEXT, text.toString().trim());
        startActivity(Intent.createChooser(intent,
                getResources().getString(R.string.share)));
    }

    public void exportToStorage() {
        Intent intent = null;
        //android 4.4+; use Storage Access Framework
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // Create a file with the requested MIME type.
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE,
                    "webopac_starred_" + app.getLibrary().getIdent() + ".json");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
        } else {        // <android 4.4; share json as text
            intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getEncodedStarredObjects().toString());
            Intent chooser =
                    Intent.createChooser(intent, getString(R.string.export_starred_to_storage));
            startActivity(chooser);
        }
    }

    /**
     * Mainly to handle exceptions as well as the regular use cases
     */
    private void showExportError() {
        Snackbar.make(getView(), R.string.failed_exporting_file,
                Snackbar.LENGTH_SHORT).show();
    }

    private void showImportError() {
        Snackbar.make(getView(), R.string.failed_importing_file,
                Snackbar.LENGTH_SHORT).show();
    }


    private void showImportErrorNoPickerApp() {
        Snackbar.make(getView(), R.string.failed_importing_file_picker_app,
                Snackbar.LENGTH_SHORT).show();
    }


    private void showImportWrongFormatError() {
        Snackbar.make(getView(), R.string.failed_importing_file_format,
                Snackbar.LENGTH_SHORT).show();
    }

    private JSONObject getEncodedStarredObjects() {
        JSONObject starred = new JSONObject();
        try {
            starred.put(JSON_LIBRARY_NAME, app.getLibrary().getIdent());
            JSONArray items = new JSONArray();
            StarDataSource data = new StarDataSource(getActivity());
            List<Starred> libItems = data.getAllItems(app.getLibrary().getIdent());
            for (Starred libItem : libItems) {
                JSONObject item = new JSONObject();
                item.put(JSON_ITEM_MNR, libItem.getMNr());
                item.put(JSON_ITEM_TITLE, libItem.getTitle());
                item.put(JSON_ITEM_MEDIATYPE, libItem.getMediaType());

                List<String> branches = data.getBranches(libItem.getId());
                if ((branches != null) && (!branches.isEmpty())) {
                    JSONArray branchItems = new JSONArray();
                    for (String branch: branches) {
                        branchItems.put(branch);
                    }
                    item.put(JSON_ITEM_BRANCHES, branchItems);
                }

                items.put(item);
            }
            starred.put(JSON_STARRED_LIST, items);
        } catch (JSONException e) {
            showExportError();
        }
        return starred;
    }

    private void importJson(StarDataSource dataSource, String list) throws JSONException {
        JSONObject savedList = new JSONObject(list);
        String bib = savedList.getString(JSON_LIBRARY_NAME);
        //disallow import if from different library than current library
        if (bib != null && !bib.equals(app.getLibrary().getIdent())) {
            Snackbar.make(getView(), R.string.info_different_library,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        JSONArray items = savedList.getJSONArray(JSON_STARRED_LIST);
        for (int i = 0; i < items.length(); i++) {
            JSONObject entry = items.getJSONObject(i);
            if (entry.has(JSON_ITEM_MNR) &&
                    !dataSource.isStarred(bib, entry.getString(JSON_ITEM_MNR)) ||
                    !entry.has(JSON_ITEM_MNR) && !dataSource.isStarredTitle(bib,
                            entry.getString(JSON_ITEM_TITLE))) { //disallow dupes

                String mediatype = entry.optString(JSON_ITEM_MEDIATYPE, null);

                if (entry.has(JSON_ITEM_BRANCHES)) {
                    List<Copy> copies = new ArrayList<Copy>();
                    JSONArray branchItems = entry.getJSONArray(JSON_ITEM_BRANCHES);
                    for (int j = 0; j < branchItems.length(); j++) {
                        String branch = branchItems.getString(j);
                        Copy copy = new Copy();
                        copy.setBranch(branch);
                        copies.add(copy);
                    }
                    dataSource.star(entry.optString(JSON_ITEM_MNR),
                            entry.getString(JSON_ITEM_TITLE), bib,
                            mediatype != null ? SearchResult.MediaType.valueOf(mediatype) :
                                    null, copies);
                } else {
                    dataSource.star(entry.optString(JSON_ITEM_MNR),
                            entry.getString(JSON_ITEM_TITLE), bib,
                            mediatype != null ? SearchResult.MediaType.valueOf(mediatype) :
                                    null);
                }
            }
        }
    }

    public void importFromStorage() {
        //Use SAF
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        } else {    //let user use a custom picker
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        } catch (ActivityNotFoundException e) {
            showImportErrorNoPickerApp();//No picker app installed!
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXPORT && resultCode == Activity.RESULT_OK) {
            Log.i("StarredFragment", data.toString());
            Uri uri = data.getData();
            try {
                OutputStream os = getActivity().getContentResolver().openOutputStream(uri);
                if (os != null) {
                    JSONObject starred = getEncodedStarredObjects();
                    PrintWriter pw = new PrintWriter(os, true);
                    pw.write(starred.toString());
                    pw.close();
                    os.close();
                } else {
                    showExportError();
                }
            } catch (FileNotFoundException e) {
                showExportError();
            } catch (IOException e) {
                showExportError();
            }
        } else if (requestCode == REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            InputStream is = null;
            try {
                StarDataSource dataSource = new StarDataSource(getActivity());
                is = getActivity().getContentResolver().openInputStream(uri);
                if (is != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String line = "";

                    char[] chars = new char[1];
                    reader.read(chars);
                    if (chars[0] != '{') {
                        throw new WrongFileFormatException();
                    }
                    builder.append(chars);

                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    String list = builder.toString();

                    importJson(dataSource, list);

                    refreshViewAfterChange();
                    adapter.notifyDataSetChanged();
                    Snackbar.make(getView(), R.string.info_starred_updated,
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    showImportError();
                }
            } catch (JSONException | IOException e) {
                showImportError();
            } catch (WrongFileFormatException e) {
                showImportWrongFormatError();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement StarredFragment.Callback");
        }
    }

    @Override
    public void onResume() {
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        if (getContext() != null) {
            restoreState(null);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (getContext() != null) {
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sp.edit();

            String currentBib = app.getLibrary().getIdent();
            if (currentBib != null) {
                editor.putString(STATE_FILTER_BIB, currentBib);
            }

            if (currentFilterBranchId != 0) {
                editor.putInt(STATE_FILTER_BRANCH, currentFilterBranchId);
            }

            int activatedPosition = listView.getFirstVisiblePosition();
            if (activatedPosition != AdapterView.INVALID_POSITION) {
                // Serialize and persist the activated item position.
                editor.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
            }

            editor.apply();
        }

        super.onPause();
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    private void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        listView.setChoiceMode(activateOnItemClick ? AbsListView.CHOICE_MODE_SINGLE
                : AbsListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == AdapterView.INVALID_POSITION) {
            listView.setSelection(position);
        } else {
            listView.setSelection(position);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int activatedPosition = listView.getFirstVisiblePosition();
        if (activatedPosition != AdapterView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
        }
        if (currentFilterBranchId > 0) {
            outState.putInt(STATE_FILTER_BRANCH, currentFilterBranchId);
        }
    }

    public interface Callback {
        public void showDetail(String mNr, SearchResult.MediaType mediatype);

        public void removeFragment();
    }

    private class ItemListAdapter extends SimpleCursorAdapter {

        public ItemListAdapter() {
            super(getActivity(), R.layout.listitem_starred, null,
                    new String[]{"bib"}, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            StarBranchItem item = StarDataSource.cursorToStarBranchItem(cursor);

            TextView tv = (TextView) view.findViewById(R.id.tvTitle);
            if (item.getTitle() != null) {
                tv.setText(Html.fromHtml(item.getTitle()));
            } else {
                tv.setText("");
            }

            TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            ImageView ivStatus= (ImageView) view.findViewById(R.id.ivStatus);

            if (currentFilterBranchId > 0) {
                if (item.getStatus() == null) {
                    if (item.getReturnDate()>0) {
                        // nur ReturnDate
                        DateTimeFormatter fmt = DateTimeFormat.shortDate();
                        tvStatus.setText(fmt.print(item.getReturnDate()));
                    } else {
                        // weder Status noch ReturnDate
                        tvStatus.setText("");
                    }
                    ivStatus.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.VISIBLE);
                } else {
                    if (item.isAusleihbar()) {
                        tvStatus.setVisibility(View.INVISIBLE);
                        ivStatus.setVisibility(View.VISIBLE);
                        ivStatus.setImageResource(R.drawable.status_light_green_check);
                    } else {
                        // TODO !isAusleihbar(), dann status_red_cross anzeigen?
                        ivStatus.setVisibility(View.GONE);
                        tvStatus.setVisibility(View.VISIBLE);
                        if (item.getReturnDate()>0) {
                            DateTimeFormatter fmt = DateTimeFormat.shortDate();
                            tvStatus.setText(fmt.print(item.getReturnDate()));
                        } else {
                            tvStatus.setText(item.getStatus());
                        }
                    }
                }
            } else {
                // items zu allen Branches werden angezeigt
                // in diesem Fall kein Status beim item anzeigen
                tvStatus.setVisibility(View.INVISIBLE);
                ivStatus.setVisibility(View.GONE);
            }

            ImageView ivType = (ImageView) view.findViewById(R.id.ivMediaType);
            if (item.getMediaType() != null) {
                ivType.setImageResource(ResultsAdapter.getResourceByMediaType(item.getMediaType()));
            } else {
                ivType.setImageBitmap(null);
            }

            ImageView ivDelete = (ImageView) view.findViewById(R.id.ivDelete);
            ivDelete.setFocusableInTouchMode(false);
            ivDelete.setFocusable(false);
            ivDelete.setTag(item);
            ivDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Starred item = (Starred) arg0.getTag();
                    remove(item);
                    callback.removeFragment();
                }
            });
        }
    }

    private class FetchBranchesResult {
        int starId;
        List<Copy> copies;
        public FetchBranchesResult(int starId, List<Copy> copies) {
            this.starId = starId;
            this.copies = copies;
        }
    }

    public class FetchBranchesTask extends AsyncTask<Void, Integer, List<FetchBranchesResult>> {
        protected int branchId;
        protected boolean success = true;
        protected String message;
        protected List<StarBranchItem> starredList;

        public FetchBranchesTask(int branchId) {
            logDebug("FetchBranchesTask(branchId = %s)", branchId);
            this.branchId = branchId;
            message = "";

            String bib = app.getLibrary().getIdent();
            StarDataSource dataSource = new StarDataSource(getActivity());
            starredList = dataSource.getStarredInBranch(bib, branchId);
            logDebug("FetchBranchesTask - bib = %s, starredList.size = %d", bib, starredList.size());
        }

        @Override
        protected List<FetchBranchesResult> doInBackground(Void... voids) {
            List<FetchBranchesResult> res = null;
            try {
                SharedPreferences sp = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                String homebranch = sp.getString(
                        OpacClient.PREF_HOME_BRANCH_PREFIX
                                + app.getAccount().getId(), null);

                // reservation notwendig??
                if (getActivity().getIntent().hasExtra("reservation")
                        && getActivity().getIntent().getBooleanExtra(
                        "reservation", false)) {
                    app.getApi().start();
                }

                int nProgress = 0;
                res = new ArrayList<FetchBranchesResult>();
                for (Starred starred: starredList) {
                    logDebug("FetchBranchesTask.doInBackground starred.title = %s", starred.getTitle());
                    logDebug("FetchBranchesTask.doInBackground starrd.mnr = %s", starred.getMNr());

                    if (starred.getMNr() == null || starred.getMNr().isEmpty()) {
                        // Mediennummer ist notwendig für getResultById
                        continue;
                    }

                    publishProgress(++nProgress);

                    DetailedItem di = app.getApi().getResultById(starred.getMNr(), homebranch);
                    // MediaType auch setzen??
                    if (di.getMediaType() == null && (starred.getMediaType() != null)) {
                        di.setMediaType(starred.getMediaType());
                    }
                    res.add(new FetchBranchesResult(starred.getId(), di.getCopies()));
                }
                success = true;
                return res;
            } catch (Exception e) {
                message = e.getMessage();
                success = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setMax(starredList.size());
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        @SuppressLint("NewApi")
        protected void onPostExecute(List<FetchBranchesResult> res) {
            if (getActivity() == null) {
                return;
            }

            swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);

            if (!success || res == null || res.isEmpty()) {
                String text = getString(R.string.starred_update_branch_fail, message);
                Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                return;
            }

            String bib = app.getLibrary().getIdent();
            StarDataSource dataSource = new StarDataSource(getActivity());

            logDebug("FetchBranchesTask.onPostExecute res.size = %s", res.size());
            for (FetchBranchesResult itemRes: res)  {
                List<Copy> copies = itemRes.copies;
                if ((copies == null) || (copies.isEmpty())) {
                    // TODO: Toast?
                } else {
                    // Branches updaten/inserten
                    logDebug("FetchBranchesTask.onPostExecute starId = %s, copies.size = %s"
                            , itemRes.starId, copies.size());
                    dataSource.insertBranches(bib, itemRes.starId, copies);
                }
            }

            refreshViewAfterChange();

            String text = getString(R.string.starred_update_branch_success, res.size());
            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if ((values == null) || (values.length==0)) return;
            int step = values[0];
            logDebug("FetchBranchesTask.onProgressUpdate = %d)", step);
            progressBar.setProgress(step);
        }
    }

    private class WrongFileFormatException extends Exception {
    }
}
