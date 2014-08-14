package de.geeksfactory.opacclient.frontend;

import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.acra.ACRA;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.CheckBox;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.Spinner;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;
import org.json.JSONException;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

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

	protected List<Map<String, String>> spinnerCategory_data;
	protected List<Map<String, String>> spinnerBranch_data;
	protected List<Map<String, String>> spinnerHomeBranch_data;

	protected long last_meta_try = 0;
	public boolean metaDataLoading = false;
	protected LoadMetaDataTask lmdt;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_search, container, false);

		setHasOptionsMenu(true);

		setRetainInstance(true);

		sp = ((OpacActivity) getActivity()).getDefaultSharedPreferences();
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

//TODO:		ImageView ivBarcode = (ImageView) view.findViewById(R.id.ivBarcode);
//		ivBarcode.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				mCallback.scanBarcode();
//			}
//		});

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (!(app.getLibrary() == null)) {
			accountSelected(app.getAccount());
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("query")) {
			savedState = savedInstanceState.getBundle("query");
		}
		if (savedState != null)
			loadQuery(savedState);
	}

	public void clear() {
//		TODO: rewrite for new SearchField implementation
//		((EditText) view.findViewById(R.id.etSimpleSearch)).setText("");
//		((EditText) view.findViewById(R.id.etTitel)).setText("");
//		((EditText) view.findViewById(R.id.etVerfasser)).setText("");
//		((EditText) view.findViewById(R.id.etSchlagA)).setText("");
//		((EditText) view.findViewById(R.id.etSchlagB)).setText("");
//		((EditText) view.findViewById(R.id.etBarcode)).setText("");
//		((EditText) view.findViewById(R.id.etISBN)).setText("");
//		((EditText) view.findViewById(R.id.etJahr)).setText("");
//		((EditText) view.findViewById(R.id.etJahrBis)).setText("");
//		((EditText) view.findViewById(R.id.etJahrVon)).setText("");
//		((EditText) view.findViewById(R.id.etSystematik)).setText("");
//		((EditText) view.findViewById(R.id.etInteressenkreis)).setText("");
//		((EditText) view.findViewById(R.id.etVerlag)).setText("");
//		((CheckBox) view.findViewById(R.id.cbDigital)).setChecked(false);
//		((CheckBox) view.findViewById(R.id.cbAvailable)).setChecked(false);
//		((Spinner) view.findViewById(R.id.cbBranch)).setSelection(0);
//		((Spinner) view.findViewById(R.id.cbHomeBranch)).setSelection(0);
//		((Spinner) view.findViewById(R.id.cbMediengruppe)).setSelection(0);
	}

	protected void manageVisibility() {
		PackageManager pm = getActivity().getPackageManager();

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
		
		LinearLayout llFormFields = (LinearLayout) view.findViewById(R.id.llFormFields);
		llFormFields.removeAllViews();
		
		RelativeLayout rlSimpleSearch = (RelativeLayout) view.findViewById(R.id.rlSimpleSearch);
		TextView tvSearchAdvHeader = (TextView) view.findViewById(R.id.tvSearchAdvHeader);
		EditText etSimpleSearch = (EditText) view.findViewById(R.id.etSimpleSearch);
		rlSimpleSearch.setVisibility(View.GONE);
		tvSearchAdvHeader.setVisibility(View.GONE);
		
		for (SearchField field:fields) {
			View v = null;
			if (field instanceof TextSearchField) {
				TextSearchField textSearchField = (TextSearchField) field;
				if (textSearchField.isFreeSearch()) {
					rlSimpleSearch.setVisibility(View.VISIBLE);
					tvSearchAdvHeader.setVisibility(View.VISIBLE);
					etSimpleSearch.setHint(textSearchField.getHint());
				} else {
					v = getLayoutInflater().inflate(
							R.layout.searchfield_text, llFormFields, false);
					TextView title = (TextView) v.findViewById(R.id.title);
					title.setText(textSearchField.getDisplayName());
					EditText edittext = (EditText) v.findViewById(R.id.edittext);
					edittext.setHint(textSearchField.getHint());
					//TODO: Implementation for half-width search fields
				}
			} else if (field instanceof BarcodeSearchField) {
				BarcodeSearchField bcSearchField = (BarcodeSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_barcode, llFormFields, false);
				TextView title = (TextView) v.findViewById(R.id.title);
				title.setText(bcSearchField.getDisplayName());
				EditText edittext = (EditText) v.findViewById(R.id.edittext);
				edittext.setHint(bcSearchField.getHint());
				//TODO: Implementation for half-width search fields
			} else if (field instanceof DropdownSearchField) {
				DropdownSearchField ddSearchField = (DropdownSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_dropdown, llFormFields, false);
				TextView title = (TextView) v.findViewById(R.id.title);
				title.setText(ddSearchField.getDisplayName());
				//TODO: Dropdown implementation
			} else if (field instanceof CheckboxSearchField) {
				CheckboxSearchField cbSearchField = (CheckboxSearchField) field;
				v = getLayoutInflater().inflate(
						R.layout.searchfield_checkbox, llFormFields, false);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				checkbox.setText(cbSearchField.getDisplayName());
			}
			if (v != null) {
				v.setTag(field.getId());
				llFormFields.addView(v);
			}
		}
	}

	protected void fillComboBoxes() {
//		TODO: rewrite for new SearchField implementation
//		Spinner cbZst = (Spinner) view.findViewById(R.id.cbBranch);
//		Spinner cbZstHome = (Spinner) view.findViewById(R.id.cbHomeBranch);
//		Spinner cbMg = (Spinner) view.findViewById(R.id.cbMediengruppe);
//
//		String zst_home_before = "";
//		String zst_before = "";
//		String mg_before = "";
//		String selection;
//		int selected = 0, i = 0;
//
//		if (spinnerHomeBranch_data != null
//				&& spinnerHomeBranch_data.size() > 0
//				&& spinnerHomeBranch_data.size() > cbZstHome
//						.getSelectedItemPosition()
//				&& cbZstHome.getSelectedItemPosition() > 0) {
//			zst_home_before = spinnerHomeBranch_data.get(
//					cbZstHome.getSelectedItemPosition()).get("key");
//		}
//		if (spinnerBranch_data != null
//				&& spinnerBranch_data.size() > cbZst.getSelectedItemPosition()
//				&& cbZst.getSelectedItemPosition() > 0) {
//			zst_before = spinnerBranch_data
//					.get(cbZst.getSelectedItemPosition()).get("key");
//		}
//		if (spinnerCategory_data != null
//				&& spinnerCategory_data.size() > cbMg.getSelectedItemPosition()
//				&& cbMg.getSelectedItemPosition() > 0) {
//			mg_before = spinnerCategory_data
//					.get(cbMg.getSelectedItemPosition()).get("key");
//		}
//
//		MetaDataSource data = new SQLMetaDataSource(app);
//		try {
//			data.open();
//		} catch (Exception e1) {
//			throw new RuntimeException(e1);
//		}
//
//		Map<String, String> all = new HashMap<String, String>();
//		all.put("key", "");
//		all.put("value", getString(R.string.all));
//
//		spinnerBranch_data = data.getMeta(app.getLibrary().getIdent(),
//				MetaDataSource.META_TYPE_BRANCH);
//		spinnerBranch_data.add(0, all);
//		cbZst.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
//				getActivity(), spinnerBranch_data, R.layout.simple_spinner_item));
//		if (!"".equals(zst_before)) {
//			for (Map<String, String> row : spinnerBranch_data) {
//				if (row.get("key").equals(zst_before)) {
//					selected = i;
//				}
//				i++;
//			}
//			cbZst.setSelection(selected);
//		}
//
//		spinnerHomeBranch_data = data.getMeta(app.getLibrary().getIdent(),
//				MetaDataSource.META_TYPE_HOME_BRANCH);
//		selected = 0;
//		i = 0;
//		if (!"".equals(zst_home_before)) {
//			selection = zst_home_before;
//		} else {
//			if (sp.contains(OpacClient.PREF_HOME_BRANCH_PREFIX
//					+ app.getAccount().getId()))
//				selection = sp.getString(OpacClient.PREF_HOME_BRANCH_PREFIX
//						+ app.getAccount().getId(), "");
//			else {
//				try {
//					selection = app.getLibrary().getData()
//							.getString("homebranch");
//				} catch (JSONException e) {
//					selection = "";
//				}
//			}
//		}
//
//		for (Map<String, String> row : spinnerHomeBranch_data) {
//			if (row.get("key").equals(selection)) {
//				selected = i;
//			}
//			i++;
//		}
//		cbZstHome.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
//				getActivity(), spinnerHomeBranch_data,
//				R.layout.simple_spinner_item));
//		cbZstHome.setSelection(selected);
//
//		spinnerCategory_data = data.getMeta(app.getLibrary().getIdent(),
//				MetaDataSource.META_TYPE_CATEGORY);
//		spinnerCategory_data.add(0, all);
//		cbMg.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
//				getActivity(), spinnerCategory_data,
//				R.layout.simple_spinner_item));
//		if (!"".equals(mg_before)) {
//			selected = 0;
//			i = 0;
//			for (Map<String, String> row : spinnerBranch_data) {
//				if (row.get("key").equals(zst_before)) {
//					selected = i;
//				}
//				i++;
//			}
//			cbZst.setSelection(selected);
//		}
//
//		if ((spinnerBranch_data.size() == 1 || !fields
//				.contains(OpacApi.KEY_SEARCH_QUERY_BRANCH))
//				&& (spinnerCategory_data.size() == 1 || !fields
//						.contains(OpacApi.KEY_SEARCH_QUERY_CATEGORY))
//				&& (spinnerHomeBranch_data.size() == 0 || !fields
//						.contains(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH))) {
//			loadMetaData(app.getLibrary().getIdent(), true);
//			loadingIndicators();
//		}
//
//		data.close();
	}

	protected void loadingIndicators() {
//		int visibility = metaDataLoading ? View.VISIBLE : View.GONE;
//		view.findViewById(R.id.pbBranch).setVisibility(visibility);
//		view.findViewById(R.id.pbHomeBranch).setVisibility(visibility);
//		view.findViewById(R.id.pbMediengruppe).setVisibility(visibility);
	}

	public void loadMetaData(String lib) {
		loadMetaData(lib, false);
	}

	public void loadMetaData(String lib, boolean force) {
		if (metaDataLoading)
			return;
		if (System.currentTimeMillis() - last_meta_try < 3600) {
			return;
		}
		last_meta_try = System.currentTimeMillis();
		MetaDataSource data = new SQLMetaDataSource(getActivity());
		try {
			data.open();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		boolean fetch = !data.hasMeta(lib);
		data.close();
		if (fetch || force) {
			metaDataLoading = true;
			lmdt = new LoadMetaDataTask();
			lmdt.execute(getActivity().getApplication(), lib);
		}
	}

	public class LoadMetaDataTask extends OpacTask<Boolean> {
		private boolean success = true;
		private long account;

		@Override
		protected Boolean doInBackground(Object... arg0) {
			super.doInBackground(arg0);

			String lib = (String) arg0[1];
			account = app.getAccount().getId();

			try {
				if (lib.equals(app.getLibrary(lib).getIdent())) {
					app.getNewApi(app.getLibrary(lib)).start();
				} else {
					app.getApi().start();
				}
				success = true;
			} catch (java.net.UnknownHostException e) {
				success = false;
			} catch (java.net.SocketException e) {
				success = false;
			} catch (InterruptedIOException e) {
				success = false;
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (getActivity() == null)
				return;

			if (account == app.getAccount().getId()) {
				metaDataLoading = false;
				loadingIndicators();
				if (success)
					fillComboBoxes();
			}
		}
	}

	@Override
	public void accountSelected(Account account) {
		metaDataLoading = false;
		advanced = sp.getBoolean("advanced", false);
		fields = app.getApi().getSearchFields();

		manageVisibility();
		fillComboBoxes();
		loadingIndicators();
	}

	public void go() {
		app.startSearch(getActivity(), saveQuery());
	}

	public Map<String, String> saveQuery() {
		Map<String, String> query = new HashMap<String, String>();
		for (SearchField field:fields) {
			ViewGroup v = (ViewGroup) view.findViewWithTag(field.getId());
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
				//TODO: Spinner implementation
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				query.put(field.getId(), String.valueOf(checkbox.isChecked()));
			}
		}
		return query;
	}

	public void loadQuery(Bundle query) {
		for (SearchField field:fields) {
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
				//TODO: Spinner implementation
			} else if (field instanceof CheckboxSearchField) {
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
				checkbox.setChecked(Boolean.valueOf(query.getString(field.getId())));
			}
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
		super.onSaveInstanceState(outState);
	}

}
