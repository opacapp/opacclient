package de.geeksfactory.opacclient.frontend;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator.ScanResult;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.networking.SSLSecurityException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;

public class SearchFragment extends Fragment implements AccountSelectedListener {
    protected SharedPreferences sp;
    protected Callback callback;
    protected View view;
    protected OpacClient app;
    protected Bundle savedState;
    protected boolean advanced = false;
    protected List<SearchField> fields;
    protected String barcodeScanningField;
    protected ScanResult scanResult;
    private LoadSearchFieldsTask task;

    protected LinearLayout llFormFields, llAdvancedFields, llExpand;
    protected EditText etSimpleSearch;
    protected RelativeLayout rlReplaced;
    protected ImageView ivReplacedStore, ivExpandIcon;
    protected ScrollView scroll;
    protected ProgressBar progressBar;
    protected RelativeLayout rlSimpleSearch, rlOuter;
    protected TextView tvSearchAdvHeader, tvExpandString;
    protected ViewGroup errorView;

    public SearchFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_search, container, false);
        findViews();

        setHasOptionsMenu(true);

        setRetainInstance(true);

        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        app = (OpacClient) getActivity().getApplication();

        // if (getIntent().getBooleanExtra("barcode", false)) {
        // BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(
        // SearchActivity.this);
        // integrator.initiateScan();
        // } else {
        // ArrayAdapter<CharSequence> order_adapter = ArrayAdapter
        // .createFromResource(this, R.array.orders,
        // R.layout.simple_spinner_item);
        // order_adapter
        // .setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        // ((Spinner) SearchActivity.this.findViewById(R.id.cbOrder))
        // .setAdapter(order_adapter);
        // }

        return view;
    }

    protected void findViews() {
        llFormFields = (LinearLayout) view.findViewById(R.id.llFormFields);
        llAdvancedFields = (LinearLayout) view.findViewById(R.id.llAdvancedFields);
        etSimpleSearch = (EditText) view.findViewById(R.id.etSimpleSearch);
        rlReplaced = (RelativeLayout) view.findViewById(R.id.rlReplaced);
        ivReplacedStore = (ImageView) view.findViewById(R.id.ivReplacedStore);
        llExpand = (LinearLayout) view.findViewById(R.id.llExpand);
        scroll = (ScrollView) view.findViewById(R.id.scroll);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        rlSimpleSearch = (RelativeLayout) view.findViewById(R.id.rlSimpleSearch);
        tvSearchAdvHeader = (TextView) view.findViewById(R.id.tvSearchAdvHeader);
        rlOuter = (RelativeLayout) view.findViewById(R.id.rlOuter);
        ivExpandIcon = (ImageView) view.findViewById(R.id.ivExpandIcon);
        tvExpandString = (TextView) view.findViewById(R.id.tvExpandString);
        errorView = (ViewGroup) view.findViewById(R.id.error_view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null
                && savedInstanceState.containsKey("query")) {
            savedState = savedInstanceState.getBundle("query");
        }
        if (savedInstanceState != null
                && savedInstanceState.containsKey("barcodeScanningField")) {
            barcodeScanningField = savedInstanceState
                    .getString("barcodeScanningField");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(app.getLibrary() == null)) {
            accountSelected(app.getAccount());
        }
    }

    public void clear() {
        if (fields == null) return;

        for (SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }
            if (field instanceof TextSearchField && ((TextSearchField) field).isFreeSearch()) {
                etSimpleSearch.setText("");
            }
            ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
            if (v == null) {
                continue;
            }
            if (field instanceof TextSearchField) {
                EditText text = (EditText) v.findViewById(R.id.edittext);
                text.setText("");
            } else if (field instanceof BarcodeSearchField) {
                EditText text = (EditText) v.findViewById(R.id.edittext);
                text.setText("");
            } else if (field instanceof DropdownSearchField) {
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                spinner.setSelection(0);
            } else if (field instanceof CheckboxSearchField) {
                CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
                checkbox.setChecked(false);
            }
        }
    }

    protected void buildSearchForm(Map<String, String> restoreQuery) {
        String skey = "annoyed_" + app.getLibrary().getIdent();
        if (app.getLibrary().getReplacedBy() != null && !"".equals(app.getLibrary().getReplacedBy())
                && sp.getInt(skey, 0) < 5 && app.promotePlusApps()) {
            rlReplaced.setVisibility(View.VISIBLE);
            ivReplacedStore.setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(app.getLibrary().getReplacedBy()
                                                     .replace("https://play.google.com/store/apps/details?id=", "market://details?id=")));
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                Intent i = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(app.getLibrary().getReplacedBy()));
                                startActivity(i);
                            }
                        }
                    });
            sp.edit().putInt(skey, sp.getInt(skey, 0) + 1).apply();
        } else {
            rlReplaced.setVisibility(View.GONE);
        }

        llFormFields.removeAllViews();
        llAdvancedFields.removeAllViews();
        llExpand.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setAdvanced(!advanced);
            }

        });
        rlSimpleSearch.setVisibility(View.GONE);
        tvSearchAdvHeader.setVisibility(View.GONE);

        int i = 0;
        if (fields == null) {
            return;
        }
        for (final SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }
            ViewGroup v = null;
            if (field instanceof TextSearchField) {
                TextSearchField textSearchField = (TextSearchField) field;
                if (textSearchField.isFreeSearch()) {
                    rlSimpleSearch.setVisibility(View.VISIBLE);
                    tvSearchAdvHeader.setVisibility(View.VISIBLE);
                    etSimpleSearch.setHint(textSearchField.getHint());
                } else {
                    v = (ViewGroup) getActivity().getLayoutInflater().inflate(
                            R.layout.searchfield_text, llFormFields, false);
                    TextView title = (TextView) v.findViewById(R.id.title);
                    title.setText(textSearchField.getDisplayName());
                    EditText edittext = (EditText) v
                            .findViewById(R.id.edittext);
                    edittext.setHint(textSearchField.getHint());
                    if (((TextSearchField) field).isNumber()) {
                        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
                    }
                    if (((TextSearchField) field).isHalfWidth()
                            && i >= 1
                            && !(fields.get(i - 1) instanceof TextSearchField &&
                            ((TextSearchField) fields
                                    .get(i - 1)).isFreeSearch())) {
                        ViewGroup before = (ViewGroup) view
                                .findViewWithTag(fields.get(i - 1).getId());
                        llFormFields.removeView(before);
                        llAdvancedFields.removeView(before);
                        v.setTag(field.getId());
                        View together = makeHalfWidth(before, v);
                        v = null;
                        if (field.isAdvanced()) {
                            llAdvancedFields.addView(together);
                        } else {
                            llFormFields.addView(together);
                        }
                    }
                }
            } else if (field instanceof BarcodeSearchField) {
                BarcodeSearchField bcSearchField = (BarcodeSearchField) field;
                v = (ViewGroup) getActivity().getLayoutInflater().inflate(
                        R.layout.searchfield_barcode, llFormFields, false);
                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(bcSearchField.getDisplayName());
                EditText edittext = (EditText) v.findViewById(R.id.edittext);
                edittext.setHint(bcSearchField.getHint());
                ImageView ivBarcode = (ImageView) v
                        .findViewById(R.id.ivBarcode);
                ivBarcode.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        barcodeScanningField = field.getId();
                        callback.scanBarcode();
                    }
                });
                if (((BarcodeSearchField) field).isHalfWidth()
                        && i >= 1
                        &&
                        !(fields.get(i - 1) instanceof TextSearchField && ((TextSearchField) fields
                                .get(i - 1)).isFreeSearch())) {
                    ViewGroup before = (ViewGroup) view.findViewWithTag(fields
                            .get(i - 1).getId());
                    llFormFields.removeView(before);
                    llAdvancedFields.removeView(before);
                    v = makeHalfWidth(before, v);
                }
            } else if (field instanceof DropdownSearchField) {
                DropdownSearchField ddSearchField = (DropdownSearchField) field;
                if (ddSearchField.getDropdownValues() == null) {
                    continue;
                }
                v = (ViewGroup) getActivity().getLayoutInflater().inflate(
                        R.layout.searchfield_dropdown, llFormFields, false);
                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(ddSearchField.getDisplayName());
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                spinner.setAdapter(
                        ((OpacActivity) getActivity()).new MetaAdapter<DropdownSearchField.Option>(
                                getActivity(), ddSearchField.getDropdownValues(),
                                R.layout.simple_spinner_item));

                // Load saved home branch
                if (field.getMeaning() == Meaning.HOME_BRANCH) {
                    String selection;
                    if (sp.contains(OpacClient.PREF_HOME_BRANCH_PREFIX
                            + app.getAccount().getId())) {
                        selection = sp.getString(
                                OpacClient.PREF_HOME_BRANCH_PREFIX
                                        + app.getAccount().getId(), "");
                    } else {
                        try {
                            selection = app.getLibrary().getData()
                                           .getString("homebranch");
                        } catch (JSONException e) {
                            selection = "";
                        }
                    }
                    if (!selection.equals("")) {
                        int j = 0;
                        for (DropdownSearchField.Option row : ddSearchField
                                .getDropdownValues()) {
                            if (row.getKey().equals(selection)) {
                                spinner.setSelection(j);
                            }
                            j++;
                        }
                    }
                }
            } else if (field instanceof CheckboxSearchField) {
                CheckboxSearchField cbSearchField = (CheckboxSearchField) field;
                v = (ViewGroup) getActivity().getLayoutInflater().inflate(
                        R.layout.searchfield_checkbox, llFormFields, false);
                CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
                checkbox.setText(cbSearchField.getDisplayName());
            }
            if (v != null) {
                v.setTag(field.getId());
                if (field.isAdvanced()) {
                    llAdvancedFields.addView(v);
                } else {
                    llFormFields.addView(v);
                }
            }
            i++;
        }
        llExpand.setVisibility(llAdvancedFields.getChildCount() == 0 ? View.GONE
                : View.VISIBLE);

        if (restoreQuery != null) {
            loadQuery(restoreQuery);
        }
    }

    protected void setAdvanced(boolean advanced) {
        this.advanced = advanced;
        if (advanced) {
            ivExpandIcon.setImageResource(R.drawable.ic_collapse_24dp);
            tvExpandString.setText(R.string.collapse);
            llAdvancedFields.setVisibility(View.VISIBLE);
            rlOuter.getViewTreeObserver().addOnGlobalLayoutListener(
                    new OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            rlOuter.getViewTreeObserver()
                                   .removeGlobalOnLayoutListener(this);
                            scroll.smoothScrollTo(0, llExpand.getTop());
                        }
                    });
        } else {
            ivExpandIcon.setImageResource(R.drawable.ic_expand_24dp);
            tvExpandString.setText(R.string.expand);
            llAdvancedFields.setVisibility(View.GONE);

        }
    }

    private ViewGroup makeHalfWidth(ViewGroup left, ViewGroup right) {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.addView(left);
        ll.addView(right);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LayoutParams.WRAP_CONTENT, 0.5f);
        left.setLayoutParams(params);
        right.setLayoutParams(params);

        TextView title = (TextView) right.findViewById(R.id.title);
        if (title != null) {
            title.setText("");
        }
        return ll;
    }

    @Override
    public void accountSelected(Account account) {
        errorView.removeAllViews();
        progress(false);

        if (!app.getLibrary().isActive()) {
            showConnectivityError(getString(R.string.library_removed_error), false);
            return;
        }

        SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(app);
        int versionCode = 0;
        try {
            versionCode = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }
        String language = getActivity().getResources().getConfiguration().locale
                .getLanguage();
        if (dataSource.hasSearchFields(app.getLibrary().getIdent())
                && dataSource.getLastSearchFieldUpdateVersion(app.getLibrary()
                                                                 .getIdent()) == versionCode
                && language.equals(dataSource.getSearchFieldLanguage(app
                .getLibrary().getIdent()))) {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
            }
            Map<String, String> saved = saveQuery();
            fields = dataSource.getSearchFields(app.getLibrary().getIdent());
            buildSearchForm(savedState != null ? OpacClient.bundleToMap(savedState) : saved);
            savedState = null;
        } else {
            executeNewLoadSearchFieldsTask();
        }
        setAdvanced(false);
    }

    protected void progress(boolean on) {
        progressBar.setVisibility(
                on ? View.VISIBLE : View.GONE);
        scroll.setVisibility(
                on ? View.GONE : View.VISIBLE);
    }

    public void showConnectivityError() {
        showConnectivityError(null, true);
    }

    public void showConnectivityError(String description, boolean retry) {
        if (getView() == null || getActivity() == null) {
            return;
        }
        errorView.removeAllViews();
        View connError = getActivity().getLayoutInflater().inflate(
                R.layout.error_connectivity, errorView);
        Button btnRetry = (Button) connError.findViewById(R.id.btRetry);
        if (retry) {
            btnRetry.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    errorView.removeAllViews();
                    executeNewLoadSearchFieldsTask();
                }
            });
        } else {
            btnRetry.setVisibility(View.GONE);
        }

        if (description != null) {
            ((TextView) connError.findViewById(R.id.tvErrBody))
                    .setText(description);
        }

        scroll.setVisibility(View.GONE);
        connError.setVisibility(View.VISIBLE);
    }

    public void saveFields(List<SearchField> fields) {
        SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(app);
        dataSource.saveSearchFields(app.getLibrary().getIdent(), fields);
    }

    private void executeNewLoadSearchFieldsTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
        }
        task = new LoadSearchFieldsTask();
        task.execute();
    }

    public void go() {
        go(null);
    }

    public void go(Bundle bundle) {
        app.startSearch(getActivity(), saveSearchQuery(), bundle);
    }

    public Map<String, String> saveQuery() {
        if (app.getLibrary() == null) {
            return null;
        }

        saveHomeBranch();
        Map<String, String> query = new HashMap<>();

        if (fields == null) {
            SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(
                    app);
            int versionCode = 0;
            try {
                versionCode = app.getPackageManager().getPackageInfo(
                        app.getPackageName(), 0).versionCode;
            } catch (NameNotFoundException e) {
                // should not happen
                e.printStackTrace();
            }
            if (dataSource.hasSearchFields(app.getLibrary().getIdent())
                    && dataSource.getLastSearchFieldUpdateVersion(app
                    .getLibrary().getIdent()) == versionCode) {
                if (task != null && !task.isCancelled()) {
                    task.cancel(true);
                }
                fields = dataSource
                        .getSearchFields(app.getLibrary().getIdent());
                if (fields == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        for (SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }

            if (field instanceof TextSearchField && ((TextSearchField) field).isFreeSearch()) {
                query.put(field.getId(), etSimpleSearch.getEditableText().toString());
                continue;
            }

            ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
            if (v == null) {
                continue;
            }
            if (field instanceof TextSearchField) {
                EditText text = (EditText) v.findViewById(R.id.edittext);
                query.put(field.getId(), text.getEditableText().toString());
            } else if (field instanceof BarcodeSearchField) {
                EditText text = (EditText) v.findViewById(R.id.edittext);
                query.put(field.getId(), text.getEditableText().toString());
            } else if (field instanceof DropdownSearchField) {
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                if (spinner.getSelectedItemPosition() > 0) {
                    query.put(field.getId(),
                            ((DropdownSearchField) field).getDropdownValues()
                                                         .get(spinner.getSelectedItemPosition())
                                                         .getKey());
                }
            } else if (field instanceof CheckboxSearchField) {
                CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
                query.put(field.getId(), String.valueOf(checkbox.isChecked()));
            }
        }
        return query;
    }

    public List<SearchQuery> saveSearchQuery() {
        saveHomeBranch();
        List<SearchQuery> query = new ArrayList<>();
        if (fields == null || view == null) {
            return null;
        }
        for (SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }
            ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
            if (field instanceof TextSearchField) {
                EditText text;
                if (((TextSearchField) field).isFreeSearch()) {
                    text = etSimpleSearch;
                } else {
                    if (v == null) continue;
                    text = (EditText) v.findViewById(R.id.edittext);
                }
                query.add(new SearchQuery(field, text.getEditableText()
                                                     .toString().trim()));
            } else if (field instanceof BarcodeSearchField) {
                if (v == null) continue;
                EditText text = (EditText) v.findViewById(R.id.edittext);
                query.add(new SearchQuery(field, text.getEditableText()
                                                     .toString().trim()));
            } else if (field instanceof DropdownSearchField) {
                if (v == null) continue;
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                if (spinner.getSelectedItemPosition() != -1) {
                    String key = ((DropdownSearchField) field)
                            .getDropdownValues()
                            .get(spinner.getSelectedItemPosition()).getKey();
                    if (!key.equals("")) {
                        query.add(new SearchQuery(field, key));
                    }
                }
            } else if (field instanceof CheckboxSearchField) {
                if (v == null) continue;
                CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
                query.add(new SearchQuery(field, String.valueOf(checkbox
                        .isChecked())));
            }
        }
        return query;
    }

    private void saveHomeBranch() {
        if (fields == null || view == null) {
            return;
        }

        for (SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }
            if (field instanceof DropdownSearchField
                    && field.getMeaning() == Meaning.HOME_BRANCH) {
                ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
                if (v == null) {
                    continue;
                }
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                String homeBranch = ((DropdownSearchField) field)
                        .getDropdownValues()
                        .get(spinner.getSelectedItemPosition()).getKey();
                if (!homeBranch.equals("")) {
                    sp.edit()
                      .putString(OpacClient.PREF_HOME_BRANCH_PREFIX + app.getAccount().getId(),
                              homeBranch).commit();
                }
                return;
            }
        }
    }

    public void loadQuery(Map<String, String> query) {
        loadQuery(OpacClient.mapToBundle(query));
    }

    public void loadQuery(Bundle query) {
        if (query == null) {
            return;
        }
        for (SearchField field : fields) {
            if (!field.isVisible()) {
                continue;
            }
            ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
            if (v == null) {
                continue;
            }
            if (field instanceof TextSearchField) {
                EditText text;
                if (((TextSearchField) field).isFreeSearch()) {
                    text = etSimpleSearch;
                } else {
                    text = (EditText) v.findViewById(R.id.edittext);
                }
                text.setText(query.getString(field.getId()));
            } else if (field instanceof BarcodeSearchField) {
                EditText text = (EditText) v.findViewById(R.id.edittext);
                text.setText(query.getString(field.getId()));
            } else if (field instanceof DropdownSearchField) {
                Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                int i = 0;
                if (((DropdownSearchField) field).getDropdownValues() == null) {
                    continue;
                }
                for (DropdownSearchField.Option map : ((DropdownSearchField) field)
                        .getDropdownValues()) {
                    if (map.getKey().equals(query.getString(field.getId()))) {
                        spinner.setSelection(i);
                        break;
                    }
                    i++;
                }
            } else if (field instanceof CheckboxSearchField) {
                CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
                checkbox.setChecked(Boolean.valueOf(query.getString(field
                        .getId())));
            }
        }

        if (barcodeScanningField != null && scanResult != null) {
            ViewGroup v = (ViewGroup) view
                    .findViewWithTag(barcodeScanningField);
            EditText text = (EditText) v.findViewById(R.id.edittext);
            text.setText(scanResult.getContents());
            barcodeScanningField = null;
            scanResult = null;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_search, menu);
        if (getActivity() != null && ((OpacActivity) getActivity()).isTablet()) {
            // We have the floating action button for that
            menu.findItem(R.id.action_search_go).setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search_go) {
            go();
            return true;
        } else if (item.getItemId() == R.id.action_search_clear) {
            clear();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        savedState = OpacClient.mapToBundle(saveQuery());
        outState.putBundle("query", savedState);
        if (barcodeScanningField != null) {
            outState.putString("barcodeScanningField", barcodeScanningField);
        }
        super.onSaveInstanceState(outState);
    }

    public void barcodeScanned(ScanResult scanResult) {
        this.scanResult = scanResult;
        loadQuery(new Bundle());
        savedState = OpacClient.mapToBundle(saveQuery());
    }

    public interface Callback {
        public void scanBarcode();
    }

    protected class LoadSearchFieldsTask extends
            AsyncTask<Void, Void, List<SearchField>> {

        private Exception exception;

        @Override
        protected void onPreExecute() {
            progress(true);
        }

        @Override
        protected List<SearchField> doInBackground(Void... arg0) {
            try {
                List<SearchField> fields = app.getApi().getSearchFields();
                if (getActivity() == null) {
                    return null;
                }
                if (fields.size() == 0) {
                    throw new OpacErrorException(
                            getString(R.string.no_fields_found));
                }

                saveFields(fields);
                return fields;
            } catch (OpacErrorException | IOException | JSONException | OpacClient
                    .LibraryRemovedException e) {
                exception = e;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<SearchField> fields) {
            if (getActivity() == null) {
                return;
            }
            progress(false);
            if (fields != null) {
                SearchFragment.this.fields = fields;
                buildSearchForm(savedState != null ? OpacClient.bundleToMap(savedState) : null);
                savedState = null;
            } else {
                if (exception != null
                        && exception instanceof OpacErrorException) {
                    showConnectivityError(exception.getMessage(), true);
                } else if (exception != null
                        && exception instanceof SSLSecurityException) {
                    showConnectivityError(getString(R.string.connection_error_detail_security),
                            true);
                } else {
                    showConnectivityError();
                }
            }
        }

    }

}
