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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;

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

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.Starred;
import de.geeksfactory.opacclient.objects.Tag;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;
import de.geeksfactory.opacclient.storage.StarDatabase;
import de.geeksfactory.opacclient.utils.CompatibilityUtils;

public class StarredFragment extends Fragment implements
        LoaderCallbacks<Cursor>, AccountSelectedListener {

    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String JSON_LIBRARY_NAME = "library_name";
    private static final String JSON_STARRED_LIST = "starred_list";
    private static final String JSON_ITEM_MNR = "item_mnr";
    private static final String JSON_ITEM_TITLE = "item_title";
    private static final String JSON_ITEM_MEDIATYPE = "item_mediatype";
    private static final int REQUEST_CODE_EXPORT = 123;
    private static final int REQUEST_CODE_IMPORT = 124;

    protected View view;
    protected OpacClient app;
    private ItemListAdapter adapter;
    private Callback callback;
    private ListView listView;
    private int activatedPosition = ListView.INVALID_POSITION;
    private TextView tvWelcome;
    private Starred sItem;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        view = inflater.inflate(R.layout.fragment_starred, container, false);
        app = (OpacClient) getActivity().getApplication();

        adapter = new ItemListAdapter();

//        NachoTextView tagFilter = (NachoTextView) view.findViewById(R.id.searchFilter);

        listView = (ListView) view.findViewById(R.id.lvStarred);
        tvWelcome = (TextView) view.findViewById(R.id.tvWelcome);

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Starred item = (Starred) view.findViewById(R.id.ivDelete)
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
                    callback.showDetail(item.getMNr());
                }
            }
        });
        listView.setClickable(true);
        listView.setTextFilterEnabled(true);

        getActivity().getSupportLoaderManager()
                     .initLoader(0, null, this);
        listView.setAdapter(adapter);


//        ArrayAdapter<String> nachoTextViewadapter = new ArrayAdapter<>(getContext(),
//                android.R.layout.simple_dropdown_item_1line, getAllTagNames());
//        tagFilter.setThreshold(0);
//        tagFilter.setAdapter(nachoTextViewadapter);
//        List<String> tagSearchInput = tagFilter.getChipValues();

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }

        setActivateOnItemClick(((OpacActivity) getActivity()).isTablet());

        return view;
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu,
            MenuInflater inflater) {
        inflater.inflate(R.menu.activity_starred, menu);
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
        } else if (item.getItemId() == R.id.action_filter_by_tags) {
            filterByTags();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void accountSelected(Account account) {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
    }

    public void remove(Starred item) {
        StarDataSource data = new StarDataSource(getActivity());
        sItem = item;
        showSnackBar();
        data.remove(item);
    }

    //Added code to show SnackBar when clicked on Remove button in Favorites screen
    private void showSnackBar() {
        Snackbar snackbar =
                Snackbar.make(view, getString(R.string.starred_removed), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.starred_removed_undo, new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                StarDataSource data = new StarDataSource(getActivity());
                String bib = app.getLibrary().getIdent();
                data.star(sItem.getMNr(), sItem.getTitle(), bib, sItem.getMediaType());
            }
        });
        snackbar.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        if (app.getLibrary() != null && arg1 != null) {
            List<String> selectionArgsList = arg1.getStringArrayList("listOfTagIds");
            selectionArgsList.add(0, app.getLibrary().getIdent());
            String[] selectionArgs = selectionArgsList.toArray(new String[0]);
            Uri uri = Uri.parse(app.getStarProviderStarUri() + "/withTags");
            return new CursorLoader(getActivity(), uri, StarDatabase.COLUMNS, null, selectionArgs, null);
        } else if(app.getLibrary() != null) {
            return new CursorLoader(getActivity(),
                    app.getStarProviderStarUri(), StarDatabase.COLUMNS,
                    StarDatabase.STAR_WHERE_LIB, new String[]{app
                    .getLibrary().getIdent()}, null);
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
                items.put(item);
            }
            starred.put(JSON_STARRED_LIST, items);
        } catch (JSONException e) {
            showExportError();
        }
        return starred;
    }

    public void importFromStorage() {
        //Use SAF
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
        } else {    //let user use a custom picker
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        } catch (ActivityNotFoundException e) {
            showImportErrorNoPickerApp();//No picker app installed!
        }
    }

    private void filterByTags() {
        List<Tag> listOfAllTags = this.getAllTags();
        ArrayList<String> listOfSelectedTagIds = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Filter By Tags");

        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.tag_menu, null);
        builder.setView(customLayout);

        // create list view
        ListView tagsListView = (ListView) customLayout.findViewById(R.id.lvTags);
        ArrayAdapter<Tag> tagAdapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_multichoice,
                listOfAllTags);
        tagsListView.setAdapter(tagAdapter);

        tagsListView.setOnItemClickListener((parent, view, position, id) -> {
            // When clicked, toggle checkbox and add it to list of
            Tag currentTag = (Tag) parent.getItemAtPosition(position);
            String currentTagId = Integer.toString(currentTag.getId());
            CheckedTextView ctv = ((CheckedTextView) view);
            ctv.toggle();
            if (ctv.isChecked()) {
                listOfSelectedTagIds.add(currentTagId);
            } else {
                listOfSelectedTagIds.remove(currentTagId);
            }
        });

        builder.setNegativeButton("Back", (dialog, which) -> dialog.cancel());
        builder.setPositiveButton("Filter", (dialog, which) -> {
            // Initialize button and then override it so as to add the click listener
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Bundle b = new Bundle();
            if (listOfSelectedTagIds == null || listOfSelectedTagIds.isEmpty()) {
                b = null;
            } else {
                b.putStringArrayList("listOfTagIds", listOfSelectedTagIds);
            }

            getActivity().getSupportLoaderManager().restartLoader(0, b, StarredFragment.this);
            dialog.dismiss();
        });
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
            try {
                StarDataSource dataSource = new StarDataSource(getActivity());
                InputStream is = getActivity().getContentResolver().openInputStream(uri);
                if (is != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String line = "";
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
                    JSONArray items = savedList.getJSONArray(JSON_STARRED_LIST);
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject entry = items.getJSONObject(i);
                        if (entry.has(JSON_ITEM_MNR) &&
                                !dataSource.isStarred(bib, entry.getString(JSON_ITEM_MNR)) ||
                                !entry.has(JSON_ITEM_MNR) && !dataSource.isStarredTitle(bib,
                                        entry.getString(JSON_ITEM_TITLE))) { //disallow dupes
                            dataSource.star(entry.optString(JSON_ITEM_MNR),
                                    entry.getString(JSON_ITEM_TITLE), bib,
                                    SearchResult.MediaType
                                            .valueOf(entry.getString(JSON_ITEM_MEDIATYPE)));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Snackbar.make(getView(), R.string.info_starred_updated,
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    showImportError();
                }
            } catch (JSONException | IOException e) {
                showImportError();
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
                    + " must implement SearchFragment.Callback");
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

    /**
     * Adds tag to the database and the starred item.
     * @param item
     * @param tagName
     * @return updated tag list
     */
    private Tag addTag(Starred item, String tagName) {
        StarDataSource data = new StarDataSource(getActivity());
        sItem = item;
        data.addTag(item, tagName);
        Tag tagFromDatabase = data.getTagByTagName(tagName);
        item.addTag(tagFromDatabase);
        return tagFromDatabase;
    }

    /**
     * Removes tag from the database and the starred item.
     * @param tag
     */
    private void removeTag(Tag tag) {
        StarDataSource data = new StarDataSource(getActivity());
        data.removeTag(data.getTagByTagName(tag.getTagName()));
    }

    /**
     * Returns latest the tag list from the database.
     * @param item
     * @return the updated tag list
     */
    private List<Tag> getTags(Starred item) {
        StarDataSource data = new StarDataSource(getActivity());
        return data.getAllTags(item);
    }

    private List<String> getTagNames(Starred item) {
        StarDataSource data = new StarDataSource(getActivity());
        return data.getAllTagNames(item);
    }

    private List<String> getAllTagNamesExceptThisItem(Starred item) {
        StarDataSource data = new StarDataSource(getActivity());
        return data.getAllTagNamesExceptThisItem(item);
    }

    private List<Tag> getAllTags() {
        StarDataSource data = new StarDataSource(getActivity());
        return data.getAllTagsInDatabase();
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

    private class ItemListAdapter extends SimpleCursorAdapter implements Filterable {

        List<String> itemNames;
        List<Starred> items = new ArrayList<>();

        public ItemListAdapter() {
            super(getActivity(), R.layout.listitem_starred, null,
                    new String[]{"bib"}, null, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Starred item = StarDataSource.cursorToItem(cursor);
            items.add(item);

            TextView tv = (TextView) view.findViewById(R.id.tvTitle);
            if (item.getTitle() != null) {
                tv.setText(Html.fromHtml(item.getTitle()));
            } else {
                tv.setText("");
            }

            ImageView ivType = (ImageView) view.findViewById(R.id.ivMediaType);
            if (item.getMediaType() != null) {
                ivType.setImageResource(ResultsAdapter.getResourceByMediaType(item.getMediaType()));
            } else {
                ivType.setImageBitmap(null);
            }

            ImageView ivTagMenu = (ImageView) view.findViewById(R.id.ivTagMenu);
            ivTagMenu.setFocusableInTouchMode(false);
            ivTagMenu.setFocusable(false);
            ivTagMenu.setTag(item);
            List<Tag> currentTagList = getTags(item);
            List<String> currentTagListNames = getTagNames(item);


            ivTagMenu.setOnClickListener(arg0 -> {
                // Create alert dialog box for removing of tags
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(item.getTitle() + " tags list");

                // set the custom layout
                final View customLayout = getLayoutInflater().inflate(R.layout.tag_menu, null);
                builder.setView(customLayout);

                // create list view
                ListView tagsListView = (ListView) customLayout.findViewById(R.id.lvTags);
                ArrayAdapter<Tag> tagAdapter = new TagListAdapter(context, currentTagList);
                tagsListView.setAdapter(tagAdapter);

                AutoCompleteTextView autocomplete =
                        customLayout.findViewById(R.id.autoCompleteTextView);
                ArrayAdapter<String> allTagNamesAdapter =
                        new ArrayAdapter<>(context, android.R.layout.select_dialog_item,
                                getAllTagNamesExceptThisItem(item));
                autocomplete.setThreshold(2);
                autocomplete.setAdapter(allTagNamesAdapter);

                ImageButton addTagButton = (ImageButton) customLayout.findViewById(R.id.addTag);
                addTagButton.setOnClickListener(view1 -> {
                    String tagName = autocomplete.getText().toString();
                    // prevent an empty tag or an exisiting tag from being added
                    if (!tagName.equals("") && !currentTagListNames.contains(tagName)) {
                        Tag tagToAdd = addTag(item, tagName);
                        currentTagList.add(tagToAdd);
                        tagAdapter.notifyDataSetChanged();
                        currentTagListNames.add(tagName);
                        autocomplete.setText("");
                        Toast.makeText(context, "Added tag \"" + tagName + "\" to " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Please enter a nonempty tag name that already isn't on the list", Toast.LENGTH_LONG).show();
                    }
                    // hide keyboard once done so as to see toast messages
                    autocomplete.onEditorAction(EditorInfo.IME_ACTION_DONE);

                });

                builder.setNegativeButton("Back", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder.create();
                dialog.show();
            });


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

    private class TagListAdapter extends ArrayAdapter<Tag> {

        public TagListAdapter(Context context, List<Tag> tagList) {
            super(getActivity(), R.layout.listitem_tag, tagList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.listitem_tag, parent, false);
            }

            Tag currentTag = getItem(position);

            TextView tv = (TextView) itemView.findViewById(R.id.tvTitle);
            tv.setText(currentTag.getTagName());

            ImageView ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
            ivDelete.setFocusableInTouchMode(false);
            ivDelete.setFocusable(false);
            ivDelete.setTag(currentTag);
            ivDelete.setOnClickListener(arg0 -> {
                Tag item = (Tag) arg0.getTag();
                removeTag(item);
                Toast.makeText(getContext(), "Removed tag \"" + item.getTagName() + "\"", Toast.LENGTH_SHORT).show();
            });

            return itemView;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public void add(Tag object) {
            super.add(object);
        }

        public void removeTag(Tag object) {
            super.remove(object);
            StarredFragment.this.removeTag(object);
        }
    }
}
