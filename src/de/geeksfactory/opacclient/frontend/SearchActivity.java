package de.geeksfactory.opacclient.frontend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.holoeverywhere.app.ProgressDialog;
import org.holoeverywhere.widget.Spinner;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.zxing.IntentIntegrator;
import de.geeksfactory.opacclient.zxing.IntentResult;

public class SearchActivity extends OpacActivity {

	private List<ContentValues> cbMg_data;
	private List<ContentValues> cbZst_data;

	private ProgressDialog dialog;

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
			String bib = split[1];
			if (!app.getLibrary().getIdent().equals(bib)) {
				Intent i = new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://opacapp.de/web" + d.getPath()));
				startActivity(i);
				return;
			}
			String medianr = split[2];
			Intent intent = new Intent(SearchActivity.this,
					SearchResultDetailsActivity.class);
			intent.putExtra("item_id", medianr);
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent idata) {
		super.onActivityResult(requestCode, resultCode, idata);

		// Barcode
		IntentResult scanResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, idata);
		if (resultCode != RESULT_CANCELED && scanResult != null) {
			Log.i("scanned", scanResult.getContents());
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

        if(app.getLibrary() == null)
            return;

		Set<String> fields = new HashSet<String>(Arrays.asList(app.getApi()
				.getSearchFields()));

		if (fields.contains("titel")) {
			findViewById(R.id.etTitel).setVisibility(View.VISIBLE);
			findViewById(R.id.tvTitel).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etTitel).setVisibility(View.GONE);
			findViewById(R.id.tvTitel).setVisibility(View.GONE);
		}
		if (fields.contains("verfasser")) {
			findViewById(R.id.etVerfasser).setVisibility(View.VISIBLE);
			findViewById(R.id.tvVerfasser).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etVerfasser).setVisibility(View.GONE);
			findViewById(R.id.tvVerfasser).setVisibility(View.GONE);
		}
		if (fields.contains("schlag_a")) {
			findViewById(R.id.llSchlag).setVisibility(View.VISIBLE);
			findViewById(R.id.tvSchlag).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llSchlag).setVisibility(View.GONE);
			findViewById(R.id.tvSchlag).setVisibility(View.GONE);
		}
		if (fields.contains("schlag_b")) {
			findViewById(R.id.etSchlagB).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etSchlagB).setVisibility(View.GONE);
		}
		if (fields.contains("zweigstelle")) {
			findViewById(R.id.cbZweigstelle).setVisibility(View.VISIBLE);
			findViewById(R.id.tvZweigstelle).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.cbZweigstelle).setVisibility(View.GONE);
			findViewById(R.id.tvZweigstelle).setVisibility(View.GONE);
		}
		if (fields.contains("mediengruppe")) {
			findViewById(R.id.cbMediengruppe).setVisibility(View.VISIBLE);
			findViewById(R.id.tvMediengruppe).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.cbMediengruppe).setVisibility(View.GONE);
			findViewById(R.id.tvMediengruppe).setVisibility(View.GONE);
		}
		if (fields.contains("isbn")) {
			findViewById(R.id.llISBN).setVisibility(View.VISIBLE);
			findViewById(R.id.tvISBN).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llISBN).setVisibility(View.GONE);
			findViewById(R.id.tvISBN).setVisibility(View.GONE);
		}
		if (fields.contains("jahr_von") && fields.contains("jahr_bis")) {
			findViewById(R.id.llJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.etJahr).setVisibility(View.GONE);
		} else if (fields.contains("jahr")) {
			findViewById(R.id.llJahr).setVisibility(View.GONE);
			findViewById(R.id.etJahr).setVisibility(View.VISIBLE);
			findViewById(R.id.tvJahr).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.llJahr).setVisibility(View.GONE);
			findViewById(R.id.tvJahr).setVisibility(View.GONE);
			findViewById(R.id.etJahr).setVisibility(View.GONE);
		}
		if (fields.contains("notation")) {
			findViewById(R.id.etSystematik).setVisibility(View.VISIBLE);
			findViewById(R.id.tvSystematik).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etSystematik).setVisibility(View.GONE);
			findViewById(R.id.tvSystematik).setVisibility(View.GONE);
		}
		if (fields.contains("interessenkreis")) {
			findViewById(R.id.etInteressenkreis).setVisibility(View.VISIBLE);
			findViewById(R.id.tvInteressenkreis).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etInteressenkreis).setVisibility(View.GONE);
			findViewById(R.id.tvInteressenkreis).setVisibility(View.GONE);
		}
		if (fields.contains("verlag")) {
			findViewById(R.id.etVerlag).setVisibility(View.VISIBLE);
			findViewById(R.id.tvVerlag).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.etVerlag).setVisibility(View.GONE);
			findViewById(R.id.tvVerlag).setVisibility(View.GONE);
		}
		if (fields.contains("order")) {
			findViewById(R.id.cbOrder).setVisibility(View.VISIBLE);
			findViewById(R.id.tvOrder).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.cbOrder).setVisibility(View.GONE);
			findViewById(R.id.tvOrder).setVisibility(View.GONE);
		}
	}

	@Override
	public void accountSelected() {
		onStart();
		fillComboBoxes();
		super.accountSelected();
	}

	private void fillComboBoxes() {
		Spinner cbZst = (Spinner) findViewById(R.id.cbZweigstelle);

		MetaDataSource data = new MetaDataSource(this);
		data.open();

		ContentValues all = new ContentValues();
		all.put("key", "");
		all.put("value", getString(R.string.all));

		cbZst_data = data.getMeta(app.getLibrary().getIdent(), "zst");
		cbZst_data.add(0, all);
		cbZst.setAdapter(new MetaAdapter(this, cbZst_data,
				R.layout.simple_spinner_item));

		Spinner cbMg = (Spinner) findViewById(R.id.cbMediengruppe);
		cbMg_data = data.getMeta(app.getLibrary().getIdent(), "mg");
		cbMg_data.add(0, all);
		cbMg.setAdapter(new MetaAdapter(this, cbMg_data,
				R.layout.simple_spinner_item));

		data.close();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);
		setTitle(R.string.search);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (app.getLibrary() == null) {
			// Migrate
			if (!sp.getString("opac_bib", "").equals("")) {
				Library lib = null;
				try {
					lib = app.getLibrary(sp.getString("opac_bib", ""));
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (lib != null) {
					AccountDataSource data = new AccountDataSource(this);
					data.open();
					Account acc = new Account();
					acc.setLibrary(lib.getIdent());
					acc.setLabel(getString(R.string.default_account_name));
					if (!sp.getString("opac_usernr", "").equals("")) {
						acc.setName(sp.getString("opac_usernr", ""));
						acc.setPassword(sp.getString("opac_password", ""));
					}
					long insertedid = data.addAccount(acc);
					data.close();

					sp.edit()
							.putLong(OpacClient.PREF_SELECTED_ACCOUNT,
									insertedid).commit();

					dialog = ProgressDialog.show(this, "",
							getString(R.string.connecting_initially), true);
					dialog.show();

					new InitTask().execute(app);

					Toast.makeText(
							this,
							"Neue Version! Alte Accountdaten wurden wiederhergestellt.",
							Toast.LENGTH_LONG).show();

					return;

				} else {
					Toast.makeText(
							this,
							"Neue Version! Wiederherstellung alter Zugangsdaten ist fehlgeschlagen.",
							Toast.LENGTH_LONG).show();
				}
			}

			// Create new
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
			return;
		}

		if (getIntent().getBooleanExtra("barcode", false)) {
			IntentIntegrator integrator = new IntentIntegrator(
					SearchActivity.this);
			integrator.initiateScan();
		}

		// Fill combo boxes
		fillComboBoxes();

		ArrayAdapter<CharSequence> order_adapter = ArrayAdapter
				.createFromResource(this, R.array.orders,
						R.layout.simple_spinner_item);
		order_adapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		((Spinner) SearchActivity.this.findViewById(R.id.cbOrder))
				.setAdapter(order_adapter);

		ImageView ivBarcode = (ImageView) findViewById(R.id.ivBarcode);
		ivBarcode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				IntentIntegrator integrator = new IntentIntegrator(
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
	}

	public void go() {
		String zst = "";
		String mg = "";
		if (cbZst_data.size() > 0)
			zst = cbZst_data.get(
					((Spinner) SearchActivity.this
							.findViewById(R.id.cbZweigstelle))
							.getSelectedItemPosition()).getAsString("key");
		if (cbMg_data.size() > 0)
			mg = cbMg_data.get(
					((Spinner) SearchActivity.this
							.findViewById(R.id.cbMediengruppe))
							.getSelectedItemPosition()).getAsString("key");

		Intent myIntent = new Intent(SearchActivity.this,
				SearchResultsActivity.class);
		Bundle query = new Bundle();
		query.putString("titel", ((EditText) SearchActivity.this
				.findViewById(R.id.etTitel)).getEditableText().toString());
		query.putString("verfasser", ((EditText) SearchActivity.this
				.findViewById(R.id.etVerfasser)).getEditableText().toString());
		query.putString("schlag_a", ((EditText) SearchActivity.this
				.findViewById(R.id.etSchlagA)).getEditableText().toString());
		query.putString("schlag_b", ((EditText) SearchActivity.this
				.findViewById(R.id.etSchlagB)).getEditableText().toString());
		query.putString("zweigstelle", zst);
		query.putString("mediengruppe", mg);
		query.putString("isbn", ((EditText) SearchActivity.this
				.findViewById(R.id.etISBN)).getEditableText().toString());
		query.putString("jahr", ((EditText) SearchActivity.this
				.findViewById(R.id.etJahr)).getEditableText().toString());
		query.putString("jahr_von", ((EditText) SearchActivity.this
				.findViewById(R.id.etJahrVon)).getEditableText().toString());
		query.putString("jahr_bis", ((EditText) SearchActivity.this
				.findViewById(R.id.etJahrBis)).getEditableText().toString());
		query.putString("systematik", ((EditText) SearchActivity.this
				.findViewById(R.id.etSystematik)).getEditableText().toString());
		query.putString("interessenkreis", ((EditText) SearchActivity.this
				.findViewById(R.id.etInteressenkreis)).getEditableText()
				.toString());
		query.putString("verlag", ((EditText) SearchActivity.this
				.findViewById(R.id.etVerlag)).getEditableText().toString());
		query.putString(
				"order",
				(((Integer) ((Spinner) SearchActivity.this
						.findViewById(R.id.cbOrder)).getSelectedItemPosition()) + 1)
						+ "");
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

	public class InitTask extends OpacTask<Integer> {
		@Override
		protected Integer doInBackground(Object... arg0) {
			super.doInBackground(arg0);
			try {
				app.getApi().start();
			} catch (Exception e) {
				publishProgress(e, "ioerror");
			}
			return 0;
		}

		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			onStart();
		}
	}

}
