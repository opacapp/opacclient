package de.geeksfactory.opacclient.frontend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.acra.ACRA;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.CheckBox;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.Spinner;
import org.json.JSONException;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
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
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

public class SearchFragment extends Fragment implements AccountSelectedListener {
	private SharedPreferences sp;

	public interface Callback {
		public void scanBarcode();
	}

	private Callback mCallback;
	private View view;
	private OpacClient app;
	private Bundle savedState;

	private boolean advanced = false;
	private Set<String> fields;
	
	private List<ContentValues> spinnerCategory_data;
	private List<ContentValues> spinnerBranch_data;
	private List<ContentValues> spinnerHomeBranch_data;
	
	private long last_meta_try = 0;
	public boolean metaDataLoading = false;
	private LoadMetaDataTask lmdt;

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

		ImageView ivBarcode = (ImageView) view.findViewById(R.id.ivBarcode);
		ivBarcode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mCallback.scanBarcode();
			}
		});

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
		((EditText) view.findViewById(R.id.etSimpleSearch)).setText("");
		((EditText) view.findViewById(R.id.etTitel)).setText("");
		((EditText) view.findViewById(R.id.etVerfasser)).setText("");
		((EditText) view.findViewById(R.id.etSchlagA)).setText("");
		((EditText) view.findViewById(R.id.etSchlagB)).setText("");
		((EditText) view.findViewById(R.id.etBarcode)).setText("");
		((EditText) view.findViewById(R.id.etISBN)).setText("");
		((EditText) view.findViewById(R.id.etJahr)).setText("");
		((EditText) view.findViewById(R.id.etJahrBis)).setText("");
		((EditText) view.findViewById(R.id.etJahrVon)).setText("");
		((EditText) view.findViewById(R.id.etSystematik)).setText("");
		((EditText) view.findViewById(R.id.etInteressenkreis)).setText("");
		((EditText) view.findViewById(R.id.etVerlag)).setText("");
		((CheckBox) view.findViewById(R.id.cbDigital)).setChecked(true);
		((Spinner) view.findViewById(R.id.cbBranch)).setSelection(0);
		((Spinner) view.findViewById(R.id.cbHomeBranch)).setSelection(0);
		((Spinner) view.findViewById(R.id.cbMediengruppe)).setSelection(0);
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

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_FREE)) {
			view.findViewById(R.id.tvSearchAdvHeader).setVisibility(
					View.VISIBLE);
			view.findViewById(R.id.rlSimpleSearch).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.tvSearchAdvHeader).setVisibility(View.GONE);
			view.findViewById(R.id.rlSimpleSearch).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_TITLE)) {
			view.findViewById(R.id.etTitel).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvTitel).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etTitel).setVisibility(View.GONE);
			view.findViewById(R.id.tvTitel).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_AUTHOR)) {
			view.findViewById(R.id.etVerfasser).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvVerfasser).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etVerfasser).setVisibility(View.GONE);
			view.findViewById(R.id.tvVerfasser).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_KEYWORDA) && advanced) {
			view.findViewById(R.id.llSchlag).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvSchlag).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.llSchlag).setVisibility(View.GONE);
			view.findViewById(R.id.tvSchlag).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_KEYWORDB) && advanced) {
			view.findViewById(R.id.etSchlagB).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etSchlagB).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BRANCH)) {
			view.findViewById(R.id.llBranch).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvZweigstelle).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.llBranch).setVisibility(View.GONE);
			view.findViewById(R.id.tvZweigstelle).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH)) {
			view.findViewById(R.id.llHomeBranch).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvHomeBranch).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.llHomeBranch).setVisibility(View.GONE);
			view.findViewById(R.id.tvHomeBranch).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_CATEGORY)) {
			view.findViewById(R.id.llMediengruppe).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvMediengruppe).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.llMediengruppe).setVisibility(View.GONE);
			view.findViewById(R.id.tvMediengruppe).setVisibility(View.GONE);
		}

		EditText etBarcode = (EditText) view.findViewById(R.id.etBarcode);
		String etBarcodeText = etBarcode.getText().toString();
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)
				&& (advanced || !etBarcodeText.equals(""))) {
			etBarcode.setVisibility(View.VISIBLE);
		} else {
			etBarcode.setVisibility(View.GONE);
		}

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_ISBN)) {
			view.findViewById(R.id.etISBN).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etISBN).setVisibility(View.GONE);
		}

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_DIGITAL)) {
			view.findViewById(R.id.cbDigital).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.cbDigital).setVisibility(View.GONE);
		}

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_ISBN)
				|| (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE) && (advanced || !etBarcodeText
						.equals("")))) {
			if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
				view.findViewById(R.id.ivBarcode).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.ivBarcode).setVisibility(View.GONE);
			}
			view.findViewById(R.id.tvBarcodes).setVisibility(View.VISIBLE);
			view.findViewById(R.id.llBarcodes).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.tvBarcodes).setVisibility(View.GONE);
			view.findViewById(R.id.llBarcodes).setVisibility(View.GONE);
		}

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_START)
				&& fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
			view.findViewById(R.id.llJahr).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
			view.findViewById(R.id.etJahr).setVisibility(View.GONE);
		} else if (fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR)) {
			view.findViewById(R.id.llJahr).setVisibility(View.GONE);
			view.findViewById(R.id.etJahr).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.llJahr).setVisibility(View.GONE);
			view.findViewById(R.id.tvJahr).setVisibility(View.GONE);
			view.findViewById(R.id.etJahr).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_SYSTEM) && advanced) {
			view.findViewById(R.id.etSystematik).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvSystematik).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etSystematik).setVisibility(View.GONE);
			view.findViewById(R.id.tvSystematik).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_AUDIENCE) && advanced) {
			view.findViewById(R.id.etInteressenkreis).setVisibility(
					View.VISIBLE);
			view.findViewById(R.id.tvInteressenkreis).setVisibility(
					View.VISIBLE);
		} else {
			view.findViewById(R.id.etInteressenkreis).setVisibility(View.GONE);
			view.findViewById(R.id.tvInteressenkreis).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_PUBLISHER) && advanced) {
			view.findViewById(R.id.etVerlag).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvVerlag).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.etVerlag).setVisibility(View.GONE);
			view.findViewById(R.id.tvVerlag).setVisibility(View.GONE);
		}
		if (fields.contains("order") && advanced) {
			view.findViewById(R.id.cbOrder).setVisibility(View.VISIBLE);
			view.findViewById(R.id.tvOrder).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.cbOrder).setVisibility(View.GONE);
			view.findViewById(R.id.tvOrder).setVisibility(View.GONE);
		}
	}

	private void fillComboBoxes() {
		Spinner cbZst = (Spinner) view.findViewById(R.id.cbBranch);
		Spinner cbZstHome = (Spinner) view.findViewById(R.id.cbHomeBranch);
		Spinner cbMg = (Spinner) view.findViewById(R.id.cbMediengruppe);

		String zst_home_before = "";
		String zst_before = "";
		String mg_before = "";
		String selection;
		int selected = 0, i = 0;

		if (spinnerHomeBranch_data != null && spinnerHomeBranch_data.size() > 0
				&& spinnerHomeBranch_data.size() > cbZstHome.getSelectedItemPosition()
				&& cbZstHome.getSelectedItemPosition() > 0) {
			zst_home_before = spinnerHomeBranch_data.get(
					cbZstHome.getSelectedItemPosition()).getAsString("key");
		}
		if (spinnerBranch_data != null
				&& spinnerBranch_data.size() > cbZst.getSelectedItemPosition()
				&& cbZst.getSelectedItemPosition() > 0) {
			zst_before = spinnerBranch_data.get(cbZst.getSelectedItemPosition())
					.getAsString("key");
		}
		if (spinnerCategory_data != null
				&& spinnerCategory_data.size() > cbMg.getSelectedItemPosition()
				&& cbMg.getSelectedItemPosition() > 0) {
			mg_before = spinnerCategory_data.get(cbMg.getSelectedItemPosition())
					.getAsString("key");
		}

		MetaDataSource data = new SQLMetaDataSource(app);
		data.open();

		ContentValues all = new ContentValues();
		all.put("key", "");
		all.put("value", getString(R.string.all));

		spinnerBranch_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_BRANCH);
		spinnerBranch_data.add(0, all);
		cbZst.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
				getActivity(), spinnerBranch_data, R.layout.simple_spinner_item));
		if (!"".equals(zst_before)) {
			for (ContentValues row : spinnerBranch_data) {
				if (row.getAsString("key").equals(zst_before)) {
					selected = i;
				}
				i++;
			}
			cbZst.setSelection(selected);
		}

		spinnerHomeBranch_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_HOME_BRANCH);
		selected = 0;
		i = 0;
		if (!"".equals(zst_home_before)) {
			selection = zst_home_before;
		} else {
			if (sp.contains(OpacClient.PREF_HOME_BRANCH_PREFIX
					+ app.getAccount().getId()))
				selection = sp.getString(OpacClient.PREF_HOME_BRANCH_PREFIX
						+ app.getAccount().getId(), "");
			else {
				try {
					selection = app.getLibrary().getData()
							.getString("homebranch");
				} catch (JSONException e) {
					selection = "";
				}
			}
		}

		for (ContentValues row : spinnerHomeBranch_data) {
			if (row.getAsString("key").equals(selection)) {
				selected = i;
			}
			i++;
		}
		cbZstHome.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
				getActivity(), spinnerHomeBranch_data, R.layout.simple_spinner_item));
		cbZstHome.setSelection(selected);

		spinnerCategory_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_CATEGORY);
		spinnerCategory_data.add(0, all);
		cbMg.setAdapter(((OpacActivity) getActivity()).new MetaAdapter(
				getActivity(), spinnerCategory_data, R.layout.simple_spinner_item));
		if (!"".equals(mg_before)) {
			selected = 0;
			i = 0;
			for (ContentValues row : spinnerBranch_data) {
				if (row.getAsString("key").equals(zst_before)) {
					selected = i;
				}
				i++;
			}
			cbZst.setSelection(selected);
		}

		if ((spinnerBranch_data.size() == 1 || !fields
				.contains(OpacApi.KEY_SEARCH_QUERY_BRANCH))
				&& (spinnerCategory_data.size() == 1 || !fields
						.contains(OpacApi.KEY_SEARCH_QUERY_CATEGORY))
				&& (spinnerHomeBranch_data.size() == 0 || !fields
						.contains(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH))) {
			loadMetaData(app.getLibrary().getIdent(), true);
			loadingIndicators();
		}

		data.close();
	}

	private void loadingIndicators() {
		int visibility = metaDataLoading ? View.VISIBLE : View.GONE;
		view.findViewById(R.id.pbBranch).setVisibility(visibility);
		view.findViewById(R.id.pbHomeBranch).setVisibility(visibility);
		view.findViewById(R.id.pbMediengruppe).setVisibility(visibility);
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
		data.open();
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
			} catch (Exception e) {
				ACRA.getErrorReporter().handleException(e);
				success = false;
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(getActivity() == null)
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
		fields = new HashSet<String>(Arrays.asList(app.getApi()
				.getSearchFields()));

		manageVisibility();
		fillComboBoxes();
		loadingIndicators();
	}

	public void go() {
		app.startSearch(getActivity(), saveQuery());
	}

	public Bundle saveQuery() {
		String zst = "";
		String mg = "";
		String zst_home = "";
		if (spinnerBranch_data.size() > 1)
			zst = spinnerBranch_data.get(
					((Spinner) view.findViewById(R.id.cbBranch))
							.getSelectedItemPosition()).getAsString("key");
		if (spinnerHomeBranch_data.size() > 0) {
			zst_home = spinnerHomeBranch_data.get(
					((Spinner) view.findViewById(R.id.cbHomeBranch))
							.getSelectedItemPosition()).getAsString("key");
			sp.edit()
					.putString(
							OpacClient.PREF_HOME_BRANCH_PREFIX
									+ app.getAccount().getId(), zst_home)
					.commit();
		}
		if (spinnerCategory_data.size() > 1)
			mg = spinnerCategory_data.get(
					((Spinner) view.findViewById(R.id.cbMediengruppe))
							.getSelectedItemPosition()).getAsString("key");

		Bundle query = new Bundle();
		query.putString(OpacApi.KEY_SEARCH_QUERY_FREE, ((EditText) view
				.findViewById(R.id.etSimpleSearch)).getEditableText()
				.toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE, ((EditText) view
				.findViewById(R.id.etTitel)).getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_AUTHOR, ((EditText) view
				.findViewById(R.id.etVerfasser)).getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_BRANCH, zst);
		query.putString(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH, zst_home);
		query.putString(OpacApi.KEY_SEARCH_QUERY_CATEGORY, mg);
		query.putString(OpacApi.KEY_SEARCH_QUERY_ISBN, ((EditText) view
				.findViewById(R.id.etISBN)).getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_BARCODE, ((EditText) view
				.findViewById(R.id.etBarcode)).getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR, ((EditText) view
				.findViewById(R.id.etJahr)).getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_START,
				((EditText) view.findViewById(R.id.etJahrVon))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_END,
				((EditText) view.findViewById(R.id.etJahrBis))
						.getEditableText().toString());
		query.putBoolean(OpacApi.KEY_SEARCH_QUERY_DIGITAL,
				((CheckBox) view.findViewById(R.id.cbDigital)).isChecked());
		if (advanced) {
			query.putString(OpacApi.KEY_SEARCH_QUERY_KEYWORDA, ((EditText) view
					.findViewById(R.id.etSchlagA)).getEditableText().toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_KEYWORDB, ((EditText) view
					.findViewById(R.id.etSchlagB)).getEditableText().toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_SYSTEM, ((EditText) view
					.findViewById(R.id.etSystematik)).getEditableText()
					.toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_AUDIENCE, ((EditText) view
					.findViewById(R.id.etInteressenkreis)).getEditableText()
					.toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_PUBLISHER,
					((EditText) view.findViewById(R.id.etVerlag))
							.getEditableText().toString());
			query.putString(
					"order",
					((((Spinner) view.findViewById(R.id.cbOrder))
							.getSelectedItemPosition()) + 1) + "");
		}
		return query;
	}

	public void loadQuery(Bundle query) {
		((EditText) view.findViewById(R.id.etSimpleSearch)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_FREE));
		((EditText) view.findViewById(R.id.etTitel)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_TITLE));
		((EditText) view.findViewById(R.id.etVerfasser)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_AUTHOR));
		((EditText) view.findViewById(R.id.etISBN)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_ISBN));
		((EditText) view.findViewById(R.id.etBarcode)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_BARCODE));
		((EditText) view.findViewById(R.id.etJahr)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_YEAR));
		((EditText) view.findViewById(R.id.etJahrVon)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_START));
		((EditText) view.findViewById(R.id.etJahrBis)).setText(query
				.getString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_END));
		((CheckBox) view.findViewById(R.id.cbDigital)).setChecked(query
				.getBoolean(OpacApi.KEY_SEARCH_QUERY_DIGITAL));

		Spinner spBranch = (Spinner) view.findViewById(R.id.cbBranch);
		int i = 0;
		for (ContentValues row : spinnerBranch_data) {
			if (row.getAsString("key").equals(
					query.getString(OpacApi.KEY_SEARCH_QUERY_BRANCH))) {
				spBranch.setSelection(i);
				break;
			}
			i++;
		}
		Spinner spHomeBranch = (Spinner) view.findViewById(R.id.cbHomeBranch);
		i = 0;
		for (ContentValues row : spinnerHomeBranch_data) {
			if (row.getAsString("key").equals(
					query.getString(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH))) {
				spHomeBranch.setSelection(i);
				break;
			}
			i++;
		}
		Spinner spCategory = (Spinner) view.findViewById(R.id.cbMediengruppe);
		i = 0;
		for (ContentValues row : spinnerCategory_data) {
			if (row.getAsString("key").equals(
					query.getString(OpacApi.KEY_SEARCH_QUERY_CATEGORY))) {
				spCategory.setSelection(i);
				break;
			}
			i++;
		}

		if (advanced) {
			((EditText) view.findViewById(R.id.etSchlagA)).setText(query
					.getString(OpacApi.KEY_SEARCH_QUERY_KEYWORDA));
			((EditText) view.findViewById(R.id.etSchlagB)).setText(query
					.getString(OpacApi.KEY_SEARCH_QUERY_KEYWORDB));
			((EditText) view.findViewById(R.id.etSystematik)).setText(query
					.getString(OpacApi.KEY_SEARCH_QUERY_SYSTEM));
			((EditText) view.findViewById(R.id.etInteressenkreis))
					.setText(query.getString(OpacApi.KEY_SEARCH_QUERY_AUDIENCE));
			((EditText) view.findViewById(R.id.etVerlag)).setText(query
					.getString(OpacApi.KEY_SEARCH_QUERY_PUBLISHER));
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
		savedState = saveQuery();
		outState.putBundle("query", savedState);
		super.onSaveInstanceState(outState);
	}

}
