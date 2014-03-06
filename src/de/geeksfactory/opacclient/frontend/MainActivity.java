package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.R.layout;
import de.geeksfactory.opacclient.R.menu;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends OpacActivity implements SearchFragment.Callback {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}
		
		
		selectItem(1);	
	}
	
	
	@Override
	public void accountSelected(Account account) {
		if(fragment instanceof OpacActivity.AccountSelectedListener){
			((OpacActivity.AccountSelectedListener) fragment).accountSelected(account);
		}
	}


	@Override
	public void scanBarcode() {
		BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(MainActivity.this);
		integrator.initiateScan();
	}
	
	public void urlintent() {
		Uri d = getIntent().getData();

		if (d.getHost().equals("de.geeksfactory.opacclient")) {
			String medianr = d.getQueryParameter("id");

			if (medianr != null) {
//TODO:				Intent intent = new Intent(MainActivity.this, SearchResultDetailsActivity.class);
//				intent.putExtra("item_id", medianr);
//				startActivity(intent);
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
//TODO:			Intent myIntent = new Intent(MainActivity.this,
//					SearchResultsActivity.class);
//			myIntent.putExtra("titel", (titel != null ? titel : ""));
//			myIntent.putExtra("verfasser", (verfasser != null ? verfasser : ""));
//			myIntent.putExtra("schlag_a", (schlag_a != null ? schlag_a : ""));
//			myIntent.putExtra("schlag_b", (schlag_b != null ? schlag_b : ""));
//			myIntent.putExtra("isbn", (isbn != null ? isbn : ""));
//			myIntent.putExtra("jahr_von", (jahr_von != null ? jahr_von : ""));
//			myIntent.putExtra("jahr_bis", (jahr_bis != null ? jahr_bis : ""));
//			myIntent.putExtra("verlag", (verlag != null ? verlag : ""));
//			startActivity(myIntent);
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
//TODO:				Intent intent = new Intent(SearchActivity.this,
//						SearchResultDetailsActivity.class);
//				intent.putExtra("item_id", medianr);
//				startActivity(intent);
			} else {
				String title;
				try {
					title = URLDecoder.decode(split[3], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					title = URLDecoder.decode(split[3]);
				}
				Bundle query = new Bundle();
				query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE, title);
//TODO:				Intent intent = new Intent(SearchActivity.this,
//						SearchResultsActivity.class);
//				intent.putExtra("query", query);
//				startActivity(intent);
			}
			finish();
			return;
		}
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_main;
	}
	
//TODO:	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent idata) {
//		super.onActivityResult(requestCode, resultCode, idata);
//
//		// Barcode
//		BarcodeScanIntegrator.ScanResult scanResult = BarcodeScanIntegrator
//				.parseActivityResult(requestCode, resultCode, idata);
//		if (resultCode != RESULT_CANCELED && scanResult != null) {
//			if (scanResult.getContents() == null)
//				return;
//			if (scanResult.getContents().length() < 3)
//				return;
//
//			// Try to determine whether it is an ISBN number or something
//			// library
//			// internal
//			int target_field = 0;
//			if (scanResult.getFormatName() != null) {
//				if (scanResult.getFormatName().equals("EAN_13")
//						&& scanResult.getContents().startsWith("97")) {
//					target_field = R.id.etISBN;
//				} else if (scanResult.getFormatName().equals("CODE_39")) {
//					target_field = R.id.etBarcode;
//				}
//			}
//			if (target_field == 0) {
//				if (scanResult.getContents().length() == 13
//						&& (scanResult.getContents().startsWith("978") || scanResult
//								.getContents().startsWith("979"))) {
//					target_field = R.id.etISBN;
//				} else if (scanResult.getContents().length() == 10
//						&& is_valid_isbn10(scanResult.getContents()
//								.toCharArray())) {
//					target_field = R.id.etISBN;
//				} else {
//					target_field = R.id.etBarcode;
//				}
//			}
//			if (target_field == R.id.etBarcode
//					&& !fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)) {
//				Toast.makeText(this, R.string.barcode_internal_not_supported,
//						Toast.LENGTH_LONG).show();
//			} else {
//				clear();
//				((EditText) SearchActivity.this.findViewById(target_field))
//						.setText(scanResult.getContents());
//				manageVisibility();
//				go();
//			}
//
//		}
//	}
	
//TODO:	@SuppressLint("NewApi")
//	@Override
//	public void onPause() {
//		super.onPause();
//		if (nfc_capable && sp.getBoolean("nfc_search", false)) {
//			mAdapter.disableForegroundDispatch(this);
//		}
//	}
//
//	@SuppressLint("NewApi")
//	@Override
//	public void onResume() {
//		super.onResume();
//		if (nfc_capable && sp.getBoolean("nfc_search", false)) {
//			mAdapter.enableForegroundDispatch(this, nfcIntent,
//					intentFiltersArray, techListsArray);
//		}
//	}
	
//TODO:	@SuppressLint("NewApi")
//	@Override
//	public void onNewIntent(Intent intent) {
//		if (nfc_capable && sp.getBoolean("nfc_search", false)) {
//			android.nfc.Tag tag = intent
//					.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG);
//			String scanResult = readPageToString(tag);
//			if (scanResult != null) {
//				if (scanResult.length() > 5) {
//					if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)) {
//						((EditText) SearchActivity.this
//								.findViewById(R.id.etBarcode))
//								.setText(scanResult);
//						manageVisibility();
//					} else {
//						Toast.makeText(this,
//								R.string.barcode_internal_not_supported,
//								Toast.LENGTH_LONG).show();
//					}
//				}
//			}
//		}
// 	}
	
//	/**
//	 * Reads the first four blocks of an ISO 15693 NFC tag as ASCII bytes into a
//	 * string.
//	 * 
//	 * @return String Tag memory as a string (bytes converted as ASCII) or
//	 *         <code>null</code>
//	 */
//	@SuppressLint("NewApi")
//TODO:	public static String readPageToString(android.nfc.Tag tag) {
//		byte[] id = tag.getId();
//		android.nfc.tech.NfcV tech = android.nfc.tech.NfcV.get(tag);
//		byte[] readCmd = new byte[3 + id.length];
//		readCmd[0] = 0x20; // set "address" flag (only send command to this
//		// tag)
//		readCmd[1] = 0x20; // ISO 15693 Single Block Read command byte
//		System.arraycopy(id, 0, readCmd, 2, id.length); // copy ID
//		StringBuilder stringbuilder = new StringBuilder();
//		try {
//			tech.connect();
//			for (int i = 0; i < 4; i++) {
//				readCmd[2 + id.length] = (byte) i; // 1 byte payload: block
//													// address
//				byte[] data;
//				data = tech.transceive(readCmd);
//				for (int j = 0; j < data.length; j++) {
//					if (data[j] > 32 && data[j] < 127) // We only want printable
//														// characters, there
//														// might be some
//														// nullbytes in it
//														// otherwise.
//						stringbuilder.append((char) data[j]);
//				}
//			}
//			tech.close();
//		} catch (IOException e) {
//			try {
//				tech.close();
//			} catch (IOException e1) {
//			}
//			return null;
//		}
//		return stringbuilder.toString().trim();
//	}

}
