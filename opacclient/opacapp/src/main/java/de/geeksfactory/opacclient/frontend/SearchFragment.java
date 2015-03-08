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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.SSLSecurityException;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator.ScanResult;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.searchfields.AndroidMeaningDetector;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.MeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;

public class SearchFragment extends Fragment implements AccountSelectedListener {
	protected SharedPreferences sp;

	public interface Callback {
		public void scanBarcode();
	}

	protected Callback mCallback;
	protected View view;
	protected OpacClient app;
	protected Bundle savedState;

	protected boolean advanced = false;
	protected List<SearchField> fields;

	protected String barcodeScanningField;
	protected ScanResult scanResult;
	private LoadSearchFieldsTask task;

	public SearchFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_search, container, false);

		setHasOptionsMenu(true);

		setRetainInstance(true);

		sp = PreferenceManager.getDefaultSharedPreferences((OpacActivity) getActivity());
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

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (!(app.getLibrary() == null)) {
			accountSelected(app.getAccount());
		}
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

	public void clear() {
		for (SearchField field : fields) {
			if (!field.isVisible())
				continue;
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
				} else {
					text = (EditText) v.findViewById(R.id.edittext);
				}
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

	protected void buildSearchForm() {
		if (app.getLibrary().getReplacedBy() != null
				&& sp.getInt("annoyed", 0) < 5) {
			view.findViewById(R.id.rlReplaced).setVisibility(View.VISIBLE);
			view.findViewById(R.id.ivReplacedStore).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							try {
								Intent i = new Intent(Intent.ACTION_VIEW, Uri
										.parse("market://details?id="
												+ app.getLibrary()
														.getReplacedBy()));
								startActivity(i);
							} catch (ActivityNotFoundException e) {
								Log.i("play", "no market installed");
							}
						}
					});
			sp.edit().putInt("annoyed", sp.getInt("annoyed", 0) + 1).commit();
		} else {
			view.findViewById(R.id.rlReplaced).setVisibility(View.GONE);
		}

		LinearLayout llFormFields = (LinearLayout) view
				.findViewById(R.id.llFormFields);
		llFormFields.removeAllViews();
		LinearLayout llAdvancedFields = (LinearLayout) view
				.findViewById(R.id.llAdvancedFields);
		llAdvancedFields.removeAllViews();

		LinearLayout llExpand = (LinearLayout) view.findViewById(R.id.llExpand);
		llExpand.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setAdvanced(!advanced);
			}

		});

		RelativeLayout rlSimpleSearch = (RelativeLayout) view
				.findViewById(R.id.rlSimpleSearch);
		TextView tvSearchAdvHeader = (TextView) view
				.findViewById(R.id.tvSearchAdvHeader);
		EditText etSimpleSearch = (EditText) view
				.findViewById(R.id.etSimpleSearch);
		rlSimpleSearch.setVisibility(View.GONE);
		tvSearchAdvHeader.setVisibility(View.GONE);

		int i = 0;
		if (fields == null)
			return;
		for (final SearchField field : fields) {
			if (!field.isVisible())
				continue;
			ViewGroup v = null;
			if (field instanceof TextSearchField) {
				TextSearchField textSearchField = (TextSearchField) field;
				if (textSearchField.isFreeSearch()) {
					rlSimpleSearch.setVisibility(View.VISIBLE);
					tvSearchAdvHeader.setVisibility(View.VISIBLE);
					etSimpleSearch.setHint(textSearchField.getHint());
				} else {
					v = (ViewGroup) getLayoutInflater(null).inflate(
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
							&& !(fields.get(i - 1) instanceof TextSearchField && ((TextSearchField) fields
									.get(i - 1)).isFreeSearch())) {
						ViewGroup before = (ViewGroup) view
								.findViewWithTag(fields.get(i - 1).getId());
						llFormFields.removeView(before);
						llAdvancedFields.removeView(before);
						v.setTag(field.getId());
						View together = makeHalfWidth(before, v);
						v = null;
						if (field.isAdvanced())
							llAdvancedFields.addView(together);
						else
							llFormFields.addView(together);
					}
				}
			} else if (field instanceof BarcodeSearchField) {
				BarcodeSearchField bcSearchField = (BarcodeSearchField) field;
				v = (ViewGroup) getLayoutInflater(null).inflate(
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
						mCallback.scanBarcode();
					}
				});
				if (((BarcodeSearchField) field).isHalfWidth()
						&& i >= 1
						&& !(fields.get(i - 1) instanceof TextSearchField && ((TextSearchField) fields
								.get(i - 1)).isFreeSearch())) {
					ViewGroup before = (ViewGroup) view.findViewWithTag(fields
							.get(i - 1).getId());
					llFormFields.removeView(before);
					llAdvancedFields.removeView(before);
					v = makeHalfWidth(before, v);
				}
			} else if (field instanceof DropdownSearchField) {
				DropdownSearchField ddSearchField = (DropdownSearchField) field;
				v = (ViewGroup) getLayoutInflater(null).inflate(
						R.layout.searchfield_dropdown, llFormFields, false);
				TextView title = (TextView) v.findViewById(R.id.title);
				title.setText(ddSearchField.getDisplayName());
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				spinner.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
						getActivity(), ddSearchField.getDropdownValues(),
						R.layout.simple_spinner_item));

				// Load saved home branch
				if (field.getMeaning() == Meaning.HOME_BRANCH) {
					String selection = "";
					if (sp.contains(OpacClient.PREF_HOME_BRANCH_PREFIX
							+ app.getAccount().getId()))
						selection = sp.getString(
								OpacClient.PREF_HOME_BRANCH_PREFIX
										+ app.getAccount().getId(), "");
					else {
						try {
							selection = app.getLibrary().getData()
									.getString("homebranch");
						} catch (JSONException e) {
							selection = "";
						}
					}
					if (!selection.equals("")) {
						int j = 0;
						for (Map<String, String> row : ddSearchField
								.getDropdownValues()) {
							if (row.get("key").equals(selection)) {
								spinner.setSelection(j);
							}
							j++;
						}
					}
				}
			} else if (field instanceof CheckboxSearchField) {
				CheckboxSearchField cbSearchField = (CheckboxSearchField) field;
				v = (ViewGroup) getLayoutInflater(null).inflate(
						R.layout.searchfield_checkbox, llFormFields, false);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				checkbox.setText(cbSearchField.getDisplayName());
			}
			if (v != null) {
				v.setTag(field.getId());
				if (field.isAdvanced())
					llAdvancedFields.addView(v);
				else
					llFormFields.addView(v);
			}
			i++;
		}
		llExpand.setVisibility(llAdvancedFields.getChildCount() == 0 ? View.GONE
				: View.VISIBLE);
	}

	protected void setAdvanced(boolean advanced) {
		this.advanced = advanced;
		final ScrollView scroll = (ScrollView) view.findViewById(R.id.scroll);
		final RelativeLayout rlOuter = (RelativeLayout) view
				.findViewById(R.id.rlOuter);
		final LinearLayout llExpand = (LinearLayout) view
				.findViewById(R.id.llExpand);
		LinearLayout llAdvancedFields = (LinearLayout) view
				.findViewById(R.id.llAdvancedFields);
		if (advanced) {
			((ImageView) view.findViewById(R.id.ivExpandIcon))
					.setImageResource(R.drawable.ic_action_collapse);
			((TextView) view.findViewById(R.id.tvExpandString))
					.setText(R.string.collapse);
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
			((ImageView) view.findViewById(R.id.ivExpandIcon))
					.setImageResource(R.drawable.ic_action_expand);
			((TextView) view.findViewById(R.id.tvExpandString))
					.setText(R.string.expand);
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
		ViewGroup errorView = (ViewGroup) view.findViewById(R.id.error_view);
		errorView.removeAllViews();
		progress(false);

		SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(app);
		int versionCode = 0;
		try {
			versionCode = app.getPackageManager().getPackageInfo(
					app.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// should not happen
			e.printStackTrace();
		}
		String language = getActivity().getResources().getConfiguration().locale
				.getLanguage();
		if (dataSource.hasSearchFields(app.getLibrary().getIdent())
				&& dataSource.getLastSearchFieldUpdateVersion(app.getLibrary()
						.getIdent()) == versionCode
				&& language.equals(dataSource.getSearchFieldLanguage(app
						.getLibrary().getIdent()))) {
			if (task != null && !task.isCancelled())
				task.cancel(true);
			fields = dataSource.getSearchFields(app.getLibrary().getIdent());
			buildSearchForm();
			if (savedState != null)
				loadQuery(savedState);
		} else {
			executeNewLoadSearchFieldsTask();
		}
		setAdvanced(false);
	}

	protected void progress(boolean on) {
		view.findViewById(R.id.progressBar).setVisibility(
				on ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.scroll).setVisibility(
				on ? View.GONE : View.VISIBLE);
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
					Collections.sort(fields, new SearchField.OrderComparator());
				}

				saveFields(fields);
				return fields;
			} catch (OpacErrorException e) {
				exception = e;
				e.printStackTrace();
			} catch (NotReachableException e) {
				exception = e;
				e.printStackTrace();
			} catch (IOException e) {
				exception = e;
				e.printStackTrace();
			} catch (JSONException e) {
				exception = e;
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<SearchField> fields) {
			if (getActivity() == null)
				return;
			progress(false);
			if (fields != null) {
				SearchFragment.this.fields = fields;
				buildSearchForm();
				if (savedState != null)
					loadQuery(savedState);
			} else {
				if (exception != null
						&& exception instanceof OpacErrorException)
					showConnectivityError(exception.getMessage());
				else if (exception != null
						&& exception instanceof SSLSecurityException)
					showConnectivityError(getString(R.string.connection_error_detail_security));
				else
					showConnectivityError();
			}
		}

	}

	public void showConnectivityError() {
		showConnectivityError(null);
	}

	public void showConnectivityError(String description) {
		if (getView() == null || getActivity() == null)
			return;
		final ViewGroup errorView = (ViewGroup) view
				.findViewById(R.id.error_view);
		errorView.removeAllViews();
		View connError = getActivity().getLayoutInflater().inflate(
				R.layout.error_connectivity, errorView);
		((Button) connError.findViewById(R.id.btRetry))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						errorView.removeAllViews();
						executeNewLoadSearchFieldsTask();
					}
				});

		if (description != null) {
			((TextView) connError.findViewById(R.id.tvErrBody))
					.setText(description);
		}

		view.findViewById(R.id.scroll).setVisibility(View.GONE);
		connError.setVisibility(View.VISIBLE);
	}

	public void saveFields(List<SearchField> fields) {
		SearchFieldDataSource dataSource = new JsonSearchFieldDataSource(app);
		dataSource.saveSearchFields(app.getLibrary().getIdent(), fields);
	}

	private void executeNewLoadSearchFieldsTask() {
		if (task != null && !task.isCancelled())
			task.cancel(true);
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
		if(app.getLibrary() == null)
			return null;
		
		saveHomeBranch();
		Map<String, String> query = new HashMap<String, String>();

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
				if (task != null && !task.isCancelled())
					task.cancel(true);
				fields = dataSource
						.getSearchFields(app.getLibrary().getIdent());
			} else {
				return null;
			}
		}

		for (SearchField field : fields) {
			if (!field.isVisible())
				continue;

			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (v == null)
				return null;
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
				} else {
					text = (EditText) v.findViewById(R.id.edittext);
				}
				query.put(field.getId(), text.getEditableText().toString());
			} else if (field instanceof BarcodeSearchField) {
				EditText text = (EditText) v.findViewById(R.id.edittext);
				query.put(field.getId(), text.getEditableText().toString());
			} else if (field instanceof DropdownSearchField) {
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				if (spinner.getSelectedItemPosition() > 0)
					query.put(field.getId(),
							((DropdownSearchField) field).getDropdownValues()
									.get(spinner.getSelectedItemPosition())
									.get("key"));
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				query.put(field.getId(), String.valueOf(checkbox.isChecked()));
			}
		}
		return query;
	}

	public List<SearchQuery> saveSearchQuery() {
		saveHomeBranch();
		List<SearchQuery> query = new ArrayList<SearchQuery>();
		if (fields == null)
			return null;
		for (SearchField field : fields) {
			if (!field.isVisible())
				continue;
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
				} else {
					text = (EditText) v.findViewById(R.id.edittext);
				}
				query.add(new SearchQuery(field, text.getEditableText()
						.toString()));
			} else if (field instanceof BarcodeSearchField) {
				EditText text = (EditText) v.findViewById(R.id.edittext);
				query.add(new SearchQuery(field, text.getEditableText()
						.toString()));
			} else if (field instanceof DropdownSearchField) {
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				if (spinner.getSelectedItemPosition() != -1) {
					String key = ((DropdownSearchField) field)
							.getDropdownValues()
							.get(spinner.getSelectedItemPosition()).get("key");
					if (!key.equals(""))
						query.add(new SearchQuery(field, key));
				}
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				query.add(new SearchQuery(field, String.valueOf(checkbox
						.isChecked())));
			}
		}
		return query;
	}

	private void saveHomeBranch() {
		if (fields == null)
			return;

		for (SearchField field : fields) {
			if (!field.isVisible())
				continue;
			if (field instanceof DropdownSearchField
					&& field.getMeaning() == Meaning.HOME_BRANCH) {
				ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
				Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				String homeBranch = ((DropdownSearchField) field)
						.getDropdownValues()
						.get(spinner.getSelectedItemPosition()).get("key");
				if (!homeBranch.equals("")) {
					sp.edit()
							.putString(
									OpacClient.PREF_HOME_BRANCH_PREFIX
											+ app.getAccount().getId(),
									homeBranch).commit();
				}
				return;
			}
		}
	}

	public void loadQuery(Bundle query) {
		for (SearchField field : fields) {
			if (!field.isVisible())
				continue;
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
			if (field instanceof TextSearchField) {
				EditText text;
				if (((TextSearchField) field).isFreeSearch()) {
					text = (EditText) view.findViewById(R.id.etSimpleSearch);
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
				for (Map<String, String> map : ((DropdownSearchField) field)
						.getDropdownValues()) {
					if (map.get("key").equals(query.getString(field.getId()))) {
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
			mCallback = (Callback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement SearchFragment.Callback");
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_search, menu);
        if (((OpacActivity) getActivity()).isTablet())
            // We have the floating action button for that
            menu.findItem(R.id.action_search_go).setVisible(false);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_search_go) {
			go();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		savedState = OpacClient.mapToBundle(saveQuery());
		outState.putBundle("query", savedState);
		if (barcodeScanningField != null)
			outState.putString("barcodeScanningField", barcodeScanningField);
		super.onSaveInstanceState(outState);
	}

	public void barcodeScanned(ScanResult scanResult) {
		this.scanResult = scanResult;
		if (barcodeScanningField != null) {
			ViewGroup v = (ViewGroup) view
					.findViewWithTag(barcodeScanningField);
			EditText text = (EditText) v.findViewById(R.id.edittext);
			text.setText(scanResult.getContents());
			barcodeScanningField = null;
			scanResult = null;
		}
	}

}
