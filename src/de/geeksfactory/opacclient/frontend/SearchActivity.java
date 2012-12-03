package de.geeksfactory.opacclient.frontend;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.WazaBe.HoloEverywhere.widget.Spinner;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.zxing.IntentIntegrator;
import de.geeksfactory.opacclient.zxing.IntentResult;

public class SearchActivity extends OpacActivity {

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
	protected void onResume() {
		super.onResume();
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
		} else {
			findViewById(R.id.llJahr).setVisibility(View.GONE);
			findViewById(R.id.tvJahr).setVisibility(View.GONE);
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (getIntent().getBooleanExtra("barcode", false)) {
			IntentIntegrator integrator = new IntentIntegrator(
					SearchActivity.this);
			integrator.initiateScan();
		}

		// Fill combo boxes

		Spinner cbZst = (Spinner) findViewById(R.id.cbZweigstelle);
		String[] zst = sp.getString("opac_zst", ":Alle").split("~");
		if (zst[0].startsWith(": ")) {
			zst[0] = zst[0].substring(2);
		}
		ArrayAdapter<String> zst_adapter = new ArrayAdapter<String>(this,
				R.layout.simple_spinner_item, zst);
		zst_adapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		cbZst.setAdapter(zst_adapter);

		Spinner cbMg = (Spinner) findViewById(R.id.cbMediengruppe);
		String[] mg = sp.getString("opac_mg", ":Alle").split("~");
		if (mg[0].startsWith(": ")) {
			mg[0] = mg[0].substring(2);
		}
		ArrayAdapter<String> mg_adapter = new ArrayAdapter<String>(this,
				R.layout.simple_spinner_item, mg);
		mg_adapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		cbMg.setAdapter(mg_adapter);

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
		// Go

		Button btGo = (Button) findViewById(R.id.btStartsearch);
		btGo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String zst = ((String) ((Spinner) SearchActivity.this
						.findViewById(R.id.cbZweigstelle)).getSelectedItem());
				if (zst.contains(":")) {
					zst = zst.split(":", 2)[0];
				} else {
					zst = "";
				}
				String mg = ((String) ((Spinner) SearchActivity.this
						.findViewById(R.id.cbMediengruppe)).getSelectedItem());
				if (mg.contains(":")) {
					mg = mg.split(":", 2)[0];
				} else {
					mg = "";
				}
				Intent myIntent = new Intent(SearchActivity.this,
						SearchResultsActivity.class);
				Bundle query = new Bundle();
				query.putString("titel", ((EditText) SearchActivity.this
						.findViewById(R.id.etTitel)).getEditableText()
						.toString());
				query.putString("verfasser", ((EditText) SearchActivity.this
						.findViewById(R.id.etVerfasser)).getEditableText()
						.toString());
				query.putString("schlag_a", ((EditText) SearchActivity.this
						.findViewById(R.id.etSchlagA)).getEditableText()
						.toString());
				query.putString("schlag_b", ((EditText) SearchActivity.this
						.findViewById(R.id.etSchlagB)).getEditableText()
						.toString());
				query.putString("zweigstelle", zst);
				query.putString("mediengruppe", mg);
				query.putString("isbn", ((EditText) SearchActivity.this
						.findViewById(R.id.etISBN)).getEditableText()
						.toString());
				query.putString("jahr_von", ((EditText) SearchActivity.this
						.findViewById(R.id.etJahrVon)).getEditableText()
						.toString());
				query.putString("jahr_bis", ((EditText) SearchActivity.this
						.findViewById(R.id.etJahrBis)).getEditableText()
						.toString());
				query.putString("systematik", ((EditText) SearchActivity.this
						.findViewById(R.id.etSystematik)).getEditableText()
						.toString());
				query.putString("interessenkreis",
						((EditText) SearchActivity.this
								.findViewById(R.id.etInteressenkreis))
								.getEditableText().toString());
				query.putString("verlag", ((EditText) SearchActivity.this
						.findViewById(R.id.etVerlag)).getEditableText()
						.toString());
				query.putString(
						"order",
						(((Integer) ((Spinner) SearchActivity.this
								.findViewById(R.id.cbOrder))
								.getSelectedItemPosition()) + 1)
								+ "");
				myIntent.putExtra("query", query);
				startActivity(myIntent);
			}
		});

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_search, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_accounts:
			selectaccount();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
