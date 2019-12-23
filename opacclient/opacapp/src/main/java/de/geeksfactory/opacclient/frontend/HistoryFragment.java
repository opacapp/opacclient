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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.Days;
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
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.frontend.adapter.AccountAdapter;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.HistoryItem;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.HistoryDataSource;
import de.geeksfactory.opacclient.storage.HistoryDatabase;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.utils.CompatibilityUtils;

public class HistoryFragment extends Fragment implements
        LoaderCallbacks<Cursor>, AccountSelectedListener {

    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String JSON_LIBRARY_NAME = "library_name";
    private static final String JSON_HISTORY_LIST = "history_list";
    private static final int REQUEST_CODE_EXPORT = 123;
    private static final int REQUEST_CODE_IMPORT = 124;

    private static int REQUEST_CODE_DETAIL = 1; // siehe AccountFragment.REQUEST_DETAIL

    protected View view;
    protected OpacClient app;
    private ItemListAdapter adapter;
    private Callback callback;
    private ListView listView;
    private int activatedPosition = ListView.INVALID_POSITION;
    private TextView tvWelcome;
    private HistoryItem historyItem;

    private String sortOrder = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        view = inflater.inflate(R.layout.fragment_history, container, false);
        app = (OpacClient) getActivity().getApplication();

        adapter = new ItemListAdapter();

        listView = (ListView) view.findViewById(R.id.lvHistory);
        tvWelcome = (TextView) view.findViewById(R.id.tvHistoryWelcome);

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                HistoryItem item = (HistoryItem) view.findViewById(R.id.ivDelete)
                                             .getTag();
                if (item.getMNr() == null || item.getMNr().equals("null")
                        || item.getMNr().equals("")) {

                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    List<SearchQuery> query = new ArrayList<>();
                    List<SearchField> fields = new JsonSearchFieldDataSource(
                            app).getSearchFields(app.getLibrary().getIdent());
                    if (fields != null) {
                        SearchField title_field = null, free_field = null;
                        for (SearchField field : fields) {
                            if (field.getMeaning() == Meaning.TITLE) {
                                title_field = field;
                            } else if (field.getMeaning() == Meaning.FREE) {
                                free_field = field;
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
                        app.startSearch(getActivity(), query);
                    } else {
                        Toast.makeText(getActivity(), R.string.no_search_cache,
                                Toast.LENGTH_LONG).show();
                    }
                } else {
//                  callback.showDetail(item.getMNr());
                    showDetailActivity(item, view);
                }
            }
        });
        listView.setClickable(true);
        listView.setTextFilterEnabled(true);

        getActivity().getSupportLoaderManager()
                     .initLoader(0, null, this);
        listView.setAdapter(adapter);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }

        setActivateOnItemClick(((OpacActivity) getActivity()).isTablet());

        return view;
    }

    private void showDetailActivity(AccountItem item, View view) {
        Intent intent = new Intent(getContext(), AccountItemDetailActivity.class);
        intent.putExtra(AccountItemDetailActivity.EXTRA_ITEM, item);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(getActivity(), view,
                        getString(R.string.transition_background));

        ActivityCompat
                .startActivityForResult(getActivity(), intent, REQUEST_CODE_DETAIL, options.toBundle());
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu,
            MenuInflater inflater) {
        inflater.inflate(R.menu.activity_history, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
            return true;
        } else if (item.getItemId() == R.id.action_sort_author) {
            sort("author");
            return true;
        } else if (item.getItemId() == R.id.action_sort_title) {
            sort("title");
            return true;
        } else if (item.getItemId() == R.id.action_sort_firstDate) {
            sort("firstDate");
            return true;
        } else if (item.getItemId() == R.id.action_sort_lastDate) {
            sort("lastDate");
            return true;
        } else if (item.getItemId() == R.id.action_sort_prolongCount) {
            sort("prolongCount");
            return true;
        } else if (item.getItemId() == R.id.action_sort_duration) {
            sort("julianday(lastDate) - julianday(firstDate)");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sort(String orderby) {

        if (sortOrder == null) {
            // bisher nicht sortiert
            sortOrder = orderby + " DESC";
        } else if (sortOrder.startsWith(orderby)) {
            // bereits nach dieser Spalte sortiert
            // d.h. ASC/DESC swappen
            if (sortOrder.equals(orderby + " ASC")) {
                sortOrder = orderby + " DESC";
            } else {
                sortOrder = orderby + " ASC";
            }
        } else {
            // bisher nach anderer Spalte sortiert
            // zunächst ASC
            sortOrder = orderby + " ASC";
        }

        // Loader restarten
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void accountSelected(Account account) {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
    }

    public void remove(HistoryItem item) {
        HistoryDataSource data = new HistoryDataSource(getActivity());
        historyItem = item;
        showSnackBar();
        data.remove(item);
    }

    //Added code to show SnackBar when clicked on Remove button in Favorites screen
    private void showSnackBar() {
        Snackbar snackbar =
                Snackbar.make(view, getString(R.string.history_removed), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.history_removed_undo, new OnClickListener() {

            @Override
            public void onClick(View view) {
                HistoryDataSource data = new HistoryDataSource(getActivity());
                // String bib = app.getLibrary().getIdent();
                data.insertHistoryItem(historyItem);
            }
        });
        snackbar.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        if (app.getLibrary() != null) {
            return new CursorLoader(getActivity(),
                    app.getHistoryProviderHistoryUri(), HistoryDatabase.COLUMNS,
                    HistoryDatabase.HIST_WHERE_LIB, new String[]{app
                    .getLibrary().getIdent()}, sortOrder);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
        if (cursor.getCount() == 0) {
            tvWelcome.setVisibility(View.VISIBLE);
        } else {
            tvWelcome.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        adapter.swapCursor(null);
    }

    protected void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(CompatibilityUtils.getNewDocumentIntentFlag());

        StringBuilder text = new StringBuilder();

        HistoryDataSource data = new HistoryDataSource(getActivity());
        List<HistoryItem> items = data.getAllItems(app.getLibrary().getIdent());
        for (HistoryItem item : items) {
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
                    "webopac_history_" + app.getLibrary().getIdent() + ".json");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
        } else {        // <android 4.4; share json as text
            intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getEncodedHistoryItemObjects().toString());
            Intent chooser =
                    Intent.createChooser(intent, getString(R.string.export_history_to_storage));
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

    private JSONObject getEncodedHistoryItemObjects() {
        JSONObject history = new JSONObject();
        try {
            HistoryDataSource data = new HistoryDataSource(getActivity());
            JSONArray items = data.getAllItemsAsJson(app.getLibrary().getIdent());
            /*
            JSONArray items = new JSONArray();
            List<HistoryItem> libItems = data.getAllItems(app.getLibrary().getIdent());
            for (HistoryItem libItem : libItems) {
                JSONObject item = new JSONObject();
                item.put(JSON_ITEM_MNR, libItem.getMNr());
                item.put(JSON_ITEM_TITLE, libItem.getTitle());
                item.put(JSON_ITEM_MEDIATYPE, libItem.getMediaType());
                items.put(item);
            }
            */
            history.put(JSON_LIBRARY_NAME, app.getLibrary().getIdent());
            history.put(JSON_HISTORY_LIST, items);
        } catch (JSONException e) {
            showExportError();
        }
        return history;
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
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_EXPORT && resultCode == Activity.RESULT_OK) {
            Log.i("HistoryItemFragment", intent.toString());
            Uri uri = intent.getData();
            try {
                OutputStream os = getActivity().getContentResolver().openOutputStream(uri);
                if (os != null) {
                    JSONObject history = getEncodedHistoryItemObjects();
                    PrintWriter pw = new PrintWriter(os, true);
                    pw.write(history.toString());
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
            Uri uri = intent.getData();
            InputStream is = null;
            try {
                HistoryDataSource dataSource = new HistoryDataSource(getActivity());
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
                    JSONObject savedList = new JSONObject(list);
                    String bib = savedList.getString(JSON_LIBRARY_NAME);

                    //disallow import if from different library than current library
                    if (bib != null && !bib.equals(app.getLibrary().getIdent())) {
                        Snackbar.make(getView(), R.string.info_different_library,
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    int countUpdate = 0;
                    int countInsert = 0;
                    JSONArray items = savedList.getJSONArray(JSON_HISTORY_LIST);
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject entry = items.getJSONObject(i);
                        HistoryDataSource.ChangeType ct = dataSource.insertOrUpdate(bib, entry);
                        switch (ct) {
                            case UPDATE: countUpdate++; break;
                            case INSERT: countInsert++; break;
                        }
                    }
                    if(countInsert>0 || countUpdate>0) {
                        adapter.notifyDataSetChanged();
                        Snackbar.make(getView(),
                                getString(R.string.info_history_updated_count, countInsert, countUpdate),
                                Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(getView(), R.string.info_history_updated,
                                Snackbar.LENGTH_SHORT).show();
                    }
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
        } else if ((requestCode == REQUEST_CODE_DETAIL) && (intent != null)) {
            String data = intent.getStringExtra(AccountItemDetailActivity.EXTRA_DATA);
            switch (resultCode) {
                case AccountItemDetailActivity.RESULT_PROLONG:
                    // TODO implement prolong from History
                    // prolong(data);
                    break;
                case AccountItemDetailActivity.RESULT_BOOKING:
                    // TODO implement booking from History
                    // bookingStart(data);
                    break;
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
                    + " must implement HistoryFragment.Callback");
        }
    }

    @Override
    public void onResume() {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
        super.onResume();
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
            listView.setItemChecked(activatedPosition, false);
        } else {
            listView.setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activatedPosition != AdapterView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
        }
    }

    public interface Callback {
        public void showDetail(String mNr);

        public void removeFragment();
    }

    private class ItemListAdapter extends SimpleCursorAdapter {

        public ItemListAdapter() {
            super(getActivity(), R.layout.listitem_history_item, null,
                    new String[]{"bib"}, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            HistoryItem item = HistoryDataSource.cursorToItem(cursor);

            TextView tvTitleAndAuthor = (TextView) view.findViewById(R.id.tvTitleAndAuthor);

            // von AccountAdapter:
            // Overview (Title/Author, Status/Deadline, Branch)
            SpannableStringBuilder builder = new SpannableStringBuilder();
            if (item.getTitle() != null) {
                builder.append(item.getTitle());
                builder.setSpan(new StyleSpan(Typeface.BOLD), 0, item.getTitle().length(), 0);
                if (!TextUtils.isEmpty(item.getAuthor())) builder.append(". ");
            }
            if (!TextUtils.isEmpty(item.getAuthor())) {
                builder.append(item.getAuthor().split("¬\\[",2)[0]);
            }
            setTextOrHide(builder, tvTitleAndAuthor);
            // statt von StarFragment
            /*
            if (item.getTitle() != null) {
                tvTitleAndAuthor.setText(Html.fromHtml(item.getTitle()));
            } else {
                tvTitleAndAuthor.setText("");
            }
            */

            TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            TextView tvBranch = (TextView) view.findViewById(R.id.tvBranch);

            DateTimeFormatter fmt = DateTimeFormat.shortDate();

            builder = new SpannableStringBuilder();
            if (item.getFirstDate() != null) {
                int start = builder.length();
                builder.append(fmt.print(item.getFirstDate()));
                // setSpan with a span argument is not supported before API 21
                /*
                builder.setSpan(new ForegroundColorSpan(textColorPrimary),
                        start, start + fmt.print(item.getDeadline()).length(), 0);
                 */
                int countDays = 0;
                if (item.getLastDate() != null) {
                    builder.append(" – ");
                    builder.append(fmt.print(item.getLastDate()));
                    Days daysBetween = Days.daysBetween(item.getFirstDate(), item.getLastDate());
                    countDays = 1 + daysBetween.getDays();
                }
                String status = "?";
                int resId = 0;
                String fmtFirstDate = fmt.print(item.getFirstDate());
                if (countDays == 1) {
                    resId = item.isLending() ?  R.string.history_status_lending_1 : R.string.history_status_finished_1;
                } else {
                    resId = item.isLending() ?  R.string.history_status_lending : R.string.history_status_finished;
                }
                status = getString(resId, fmtFirstDate, countDays);
                setTextOrHide(status, tvStatus);
            }
            // setTextOrHide(builder, tvStatus);

            if (item.getHomeBranch() != null) {
                setTextOrHide(Html.fromHtml(item.getHomeBranch()), tvBranch);
            }

            tvBranch.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw () {
                    tvBranch.getViewTreeObserver().removeOnPreDrawListener(this);
                    // place tvBranch next to or below tvStatus to prevent overlapping
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)tvBranch.getLayoutParams();
                    if (tvStatus.getPaint().measureText(tvStatus.getText().toString()) <
                            tvStatus.getWidth() / 2 - 4){
                        lp.addRule(RelativeLayout.BELOW, 0);  //removeRule only since API 17
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        lp.addRule(RelativeLayout.BELOW, R.id.tvStatus);
                    }
                    tvBranch.setLayoutParams(lp);
                    return true;
                }
            });

            ImageView ivType = (ImageView) view.findViewById(R.id.ivMediaType);
            if (item.getMediaType() != null) {
                ivType.setImageResource(ResultsAdapter.getResourceByMediaType(item.getMediaType()));
            } else {
                ivType.setImageBitmap(null);
            }

            ImageView ivDelete = (ImageView) view.findViewById(R.id.ivDelete);
            if (ivDelete != null) {
                ivDelete.setFocusableInTouchMode(false);
                ivDelete.setFocusable(false);
                ivDelete.setTag(item);
                ivDelete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        HistoryItem item = (HistoryItem) arg0.getTag();
                        remove(item);
                        callback.removeFragment();
                    }
                });
             }
        }
    }

    protected static void setTextOrHide(CharSequence value, TextView tv) {
        if (!TextUtils.isEmpty(value)) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(value);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private class WrongFileFormatException extends Exception {
    }
}
