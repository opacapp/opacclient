package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.acra.ACRA;
import org.holoeverywhere.widget.Spinner;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.OpacTask;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;

public class SearchActivity extends OpacActivity {

	private SharedPreferences sp;
	private List<ContentValues> cbMg_data;
	private List<ContentValues> cbZst_data;
	private List<ContentValues> cbZstHome_data;
	private boolean advanced = false;
	private Set<String> fields;
	private LoadMetaDataTask lmdt;
	public boolean metaDataLoading = false;

	public void urlintent() {
		Uri d = getIntent().getData();

		if (d.getHost().equals("de.geeksfactory.opacclient")) {
			String medianr = d.getQueryParameter("id");

			if (medianr != null) {
				Intent intent = new Intent(SearchActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item_id", medianr);
				startActivity(intent);
				finish();
				return;
			}

			String titel = d.getQueryParameter("titel");
			String verfasser = d.getQueryParameter("verfasser");
			String schlag_a = d.getQueryParameter("schlag_a");
			String schlag_b = d.getQueryParameter("schlag_b");
			String isbn = d.getQueryParameter("isbn");
			String jahr_von = d.getQueryParameter("jahr_von");
			String jahr_bis = d.getQueryParameter("jahr_bis");
			String verlag = d.getQueryParameter("verlag");
			Intent myIntent = new Intent(SearchActivity.this,
					SearchResultsActivity.class);
			myIntent.putExtra("titel", (titel != null ? titel : ""));
			myIntent.putExtra("verfasser", (verfasser != null ? verfasser : ""));
			myIntent.putExtra("schlag_a", (schlag_a != null ? schlag_a : ""));
			myIntent.putExtra("schlag_b", (schlag_b != null ? schlag_b : ""));
			myIntent.putExtra("isbn", (isbn != null ? isbn : ""));
			myIntent.putExtra("jahr_von", (jahr_von != null ? jahr_von : ""));
			myIntent.putExtra("jahr_bis", (jahr_bis != null ? jahr_bis : ""));
			myIntent.putExtra("verlag", (verlag != null ? verlag : ""));
			startActivity(myIntent);
			finish();
		} else if (d.getHost().equals("opacapp.de")) {
			String[] split = d.getPath().split(":");
			String bib;
			try {
				bib = URLDecoder.decode(split[1], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				bib = URLDecoder.decode(split[1]);
			}

			if (!app.getLibrary().getIdent().equals(bib)) {
				AccountDataSource adata = new AccountDataSource(this);
				adata.open();
				List<Account> accounts = adata.getAllAccounts(bib);
				adata.close();
				if (accounts.size() > 0) {
					app.setAccount(accounts.get(0).getId());
				} else {
					Intent i = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://opacapp.de/web" + d.getPath()));
					startActivity(i);
					return;
				}
			}
			String medianr = split[2];
			if (medianr.length() > 1) {
				Intent intent = new Intent(SearchActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item_id", medianr);
				startActivity(intent);
			} else {
				String title;
				try {
					title = URLDecoder.decode(split[3], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					title = URLDecoder.decode(split[3]);
				}
				Bundle query = new Bundle();
				query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE, title);
				Intent intent = new Intent(SearchActivity.this,
						SearchResultsActivity.class);
				intent.putExtra("query", query);
				startActivity(intent);
			}
			finish();
			return;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent idata) {
		super.onActivityResult(requestCode, resultCode, idata);

		// Barcode
		BarcodeScanIntegrator.ScanResult scanResult = BarcodeScanIntegrator
				.parseActivityResult(requestCode, resultCode, idata);
		if (resultCode != RESULT_CANCELED && scanResult != null) {
			if (scanResult.getContents() == null)
				return;
			if (scanResult.getContents().length() < 3)
				return;
			((EditText) SearchActivity.this.findViewById(R.id.etISBN))
					.setText(scanResult.getContents());
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (app.getLibrary() == null)
			return;

		metaDataLoading = false;

		advanced = sp.getBoolean("advanced", false);

		fields = new HashSet<String>(Arrays.asList(app.getApi()
				.getSearchFields()));

		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_FREE)) {
			findViewById(R.id.tvSearchAdvHeader).setVisibility(View.VISIBLE);
			findViewById(R.id.rlSimpleSearch).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.tvSearchAdvHeader).setVisibility(View.GONE);
			findViewById(R.id.rlSimpleSearch).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_TITLE)) {
			findViewById(R.id.etTitel).setVisibility(View.VISIBLE);
			findViewById(R.id.tvTitel).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etTitel).setVisibility(View.GONE);
			findViewById(R.id.tvTitel).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_AUTHOR)) {
			findViewById(R.id.etVerfasser).setVisibility(View.VISIBLE);
			findViewById(R.id.tvVerfasser).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etVerfasser).setVisibility(View.GONE);
			findViewById(R.id.tvVerfasser).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_KEYWORDA) && advanced) {
			findViewById(R.id.llSchlag).setVisibility(View.VISIBLE);
			findViewById(R.id.tvSchlag).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llSchlag).setVisibility(View.GONE);
			findViewById(R.id.tvSchlag).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_KEYWORDB) && advanced) {
			findViewById(R.id.etSchlagB).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etSchlagB).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BRANCH)) {
			findViewById(R.id.llBranch).setVisibility(View.VISIBLE);
			findViewById(R.id.tvZweigstelle).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llBranch).setVisibility(View.GONE);
			findViewById(R.id.tvZweigstelle).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH)) {
			findViewById(R.id.llHomeBranch).setVisibility(View.VISIBLE);
			findViewById(R.id.tvHomeBranch).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llHomeBranch).setVisibility(View.GONE);
			findViewById(R.id.tvHomeBranch).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_CATEGORY)) {
			findViewById(R.id.llMediengruppe).setVisibility(View.VISIBLE);
			findViewById(R.id.tvMediengruppe).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llMediengruppe).setVisibility(View.GONE);
			findViewById(R.id.tvMediengruppe).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_ISBN)) {
			findViewById(R.id.llISBN).setVisibility(View.VISIBLE);
			findViewById(R.id.tvISBN).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llISBN).setVisibility(View.GONE);
			findViewById(R.id.tvISBN).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_START)
				&& fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
			findViewById(R.id.llJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.etJahr).setVisibility(View.GONE);
		} else if (fields.contains(OpacApi.KEY_SEARCH_QUERY_YEAR)) {
			findViewById(R.id.llJahr).setVisibility(View.GONE);
			findViewById(R.id.etJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llJahr).setVisibility(View.GONE);
			findViewById(R.id.tvJahr).setVisibility(View.GONE);
			findViewById(R.id.etJahr).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_SYSTEM) && advanced) {
			findViewById(R.id.etSystematik).setVisibility(View.VISIBLE);
			findViewById(R.id.tvSystematik).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etSystematik).setVisibility(View.GONE);
			findViewById(R.id.tvSystematik).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_AUDIENCE) && advanced) {
			findViewById(R.id.etInteressenkreis).setVisibility(View.VISIBLE);
			findViewById(R.id.tvInteressenkreis).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etInteressenkreis).setVisibility(View.GONE);
			findViewById(R.id.tvInteressenkreis).setVisibility(View.GONE);
		}
		if (fields.contains(OpacApi.KEY_SEARCH_QUERY_PUBLISHER) && advanced) {
			findViewById(R.id.etVerlag).setVisibility(View.VISIBLE);
			findViewById(R.id.tvVerlag).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etVerlag).setVisibility(View.GONE);
			findViewById(R.id.tvVerlag).setVisibility(View.GONE);
		}
		if (fields.contains("order") && advanced) {
			findViewById(R.id.cbOrder).setVisibility(View.VISIBLE);
			findViewById(R.id.tvOrder).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.cbOrder).setVisibility(View.GONE);
			findViewById(R.id.tvOrder).setVisibility(View.GONE);
		}

		fillComboBoxes();
		loadingIndicators();
	}

	@Override
	public void accountSelected() {
		onStart();
		fillComboBoxes();
		super.accountSelected();
	}

	private void fillComboBoxes() {
		// TODO: Preserve previously selected values
		Spinner cbZst = (Spinner) findViewById(R.id.cbBranch);
		Spinner cbZstHome = (Spinner) findViewById(R.id.cbHomeBranch);

		MetaDataSource data = new SQLMetaDataSource(this);
		data.open();

		ContentValues all = new ContentValues();
		all.put("key", "");
		all.put("value", getString(R.string.all));

		cbZst_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_BRANCH);
		cbZst_data.add(0, all);
		cbZst.setAdapter(new MetaAdapter(this, cbZst_data,
				R.layout.simple_spinner_item));

		cbZstHome_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_HOME_BRANCH);
		int selected = 0;
		String selection = sp.getString(OpacClient.PREF_HOME_BRANCH_PREFIX
				+ app.getAccount().getId(), "");
		int i = 0;
		for (ContentValues row : cbZstHome_data) {
			if (row.getAsString("key").equals(selection)) {
				selected = i;
			}
			i++;
		}
		cbZstHome.setAdapter(new MetaAdapter(this, cbZstHome_data,
				R.layout.simple_spinner_item));
		cbZstHome.setSelection(selected);

		Spinner cbMg = (Spinner) findViewById(R.id.cbMediengruppe);
		cbMg_data = data.getMeta(app.getLibrary().getIdent(),
				MetaDataSource.META_TYPE_CATEGORY);
		cbMg_data.add(0, all);
		cbMg.setAdapter(new MetaAdapter(this, cbMg_data,
				R.layout.simple_spinner_item));

		if ((cbZst_data.size() == 1 || !fields
				.contains(OpacApi.KEY_SEARCH_QUERY_BRANCH))
				&& (cbMg_data.size() == 1 || !fields
						.contains(OpacApi.KEY_SEARCH_QUERY_CATEGORY))
				&& (cbZstHome_data.size() == 0 || !fields
						.contains(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH))) {
			loadMetaData(app.getLibrary().getIdent(), true);
			loadingIndicators();
		}

		data.close();
	}

	private void loadingIndicators() {
		int visibility = metaDataLoading ? View.VISIBLE : View.GONE;
		findViewById(R.id.pbBranch).setVisibility(visibility);
		findViewById(R.id.pbHomeBranch).setVisibility(visibility);
		findViewById(R.id.pbMediengruppe).setVisibility(visibility);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);
		setTitle(R.string.search);
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		if (getIntent().getBooleanExtra("barcode", false)) {
			BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(
					SearchActivity.this);
			integrator.initiateScan();
		} else {
			ArrayAdapter<CharSequence> order_adapter = ArrayAdapter
					.createFromResource(this, R.array.orders,
							R.layout.simple_spinner_item);
			order_adapter
					.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
			((Spinner) SearchActivity.this.findViewById(R.id.cbOrder))
					.setAdapter(order_adapter);
		}

		ImageView ivBarcode = (ImageView) findViewById(R.id.ivBarcode);
		ivBarcode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(
						SearchActivity.this);
				integrator.initiateScan();
			}
		});

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}

		if (!sp.getBoolean("version2.0.0-introduced", false)) {
			final Handler handler = new Handler();
			// Just show the menu to explain that is there if people start
			// version 2 for the first time.
			// We need a handler because if we just put this in onCreate nothing
			// happens. I don't have any idea, why.
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					SharedPreferences sp = PreferenceManager
							.getDefaultSharedPreferences(SearchActivity.this);
					getSlidingMenu().showMenu(true);
					sp.edit().putBoolean("version2.0.0-introduced", true)
							.commit();
				}
			}, 500);
		}
	}

	public void go() {
		String zst = "";
		String mg = "";
		String zst_home = "";
		if (cbZst_data.size() > 1)
			zst = cbZst_data.get(
					((Spinner) SearchActivity.this.findViewById(R.id.cbBranch))
							.getSelectedItemPosition()).getAsString("key");
		if (cbZstHome_data.size() > 0) {
			zst_home = cbZstHome_data.get(
					((Spinner) SearchActivity.this
							.findViewById(R.id.cbHomeBranch))
							.getSelectedItemPosition()).getAsString("key");
			sp.edit()
					.putString(
							OpacClient.PREF_HOME_BRANCH_PREFIX
									+ app.getAccount().getId(), zst_home)
					.commit();
		}
		if (cbMg_data.size() > 1)
			mg = cbMg_data.get(
					((Spinner) SearchActivity.this
							.findViewById(R.id.cbMediengruppe))
							.getSelectedItemPosition()).getAsString("key");

		Intent myIntent = new Intent(SearchActivity.this,
				SearchResultsActivity.class);
		Bundle query = new Bundle();
		query.putString(OpacApi.KEY_SEARCH_QUERY_FREE,
				((EditText) SearchActivity.this
						.findViewById(R.id.etSimpleSearch)).getEditableText()
						.toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE,
				((EditText) SearchActivity.this.findViewById(R.id.etTitel))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_AUTHOR,
				((EditText) SearchActivity.this.findViewById(R.id.etVerfasser))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_BRANCH, zst);
		query.putString(OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH, zst_home);
		query.putString(OpacApi.KEY_SEARCH_QUERY_CATEGORY, mg);
		query.putString(OpacApi.KEY_SEARCH_QUERY_ISBN,
				((EditText) SearchActivity.this.findViewById(R.id.etISBN))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR,
				((EditText) SearchActivity.this.findViewById(R.id.etJahr))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_START,
				((EditText) SearchActivity.this.findViewById(R.id.etJahrVon))
						.getEditableText().toString());
		query.putString(OpacApi.KEY_SEARCH_QUERY_YEAR_RANGE_END,
				((EditText) SearchActivity.this.findViewById(R.id.etJahrBis))
						.getEditableText().toString());
		if (advanced) {
			query.putString(OpacApi.KEY_SEARCH_QUERY_KEYWORDA,
					((EditText) SearchActivity.this
							.findViewById(R.id.etSchlagA)).getEditableText()
							.toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_KEYWORDB,
					((EditText) SearchActivity.this
							.findViewById(R.id.etSchlagB)).getEditableText()
							.toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_SYSTEM,
					((EditText) SearchActivity.this
							.findViewById(R.id.etSystematik)).getEditableText()
							.toString());
			query.putString(OpacApi.KEY_SEARCH_QUERY_AUDIENCE,
					((EditText) SearchActivity.this
							.findViewById(R.id.etInteressenkreis))
							.getEditableText().toString());
			query.putString(
					OpacApi.KEY_SEARCH_QUERY_PUBLISHER,
					((EditText) SearchActivity.this.findViewById(R.id.etVerlag))
							.getEditableText().toString());
			query.putString(
					"order",
					(((Integer) ((Spinner) SearchActivity.this
							.findViewById(R.id.cbOrder))
							.getSelectedItemPosition()) + 1)
							+ "");
		}
		myIntent.putExtra("query", query);
		startActivity(myIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_search, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_search_go:
			go();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void loadMetaData(String lib) {
		loadMetaData(lib, false);
	}

	public void loadMetaData(String lib, boolean force) {
		if (metaDataLoading)
			return;
		MetaDataSource data = new SQLMetaDataSource(this);
		data.open();
		boolean fetch = !data.hasMeta(lib);
		data.close();
		if (fetch || force) {
			metaDataLoading = true;
			lmdt = new LoadMetaDataTask();
			lmdt.execute(getApplication(), lib);
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
			if (account == app.getAccount().getId()) {
				metaDataLoading = false;
				loadingIndicators();
				if (success)
					fillComboBoxes();
			}
		}
	}

}
