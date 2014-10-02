package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.holoeverywhere.app.Fragment;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class MainActivity extends OpacActivity implements
		SearchFragment.Callback, StarredFragment.Callback,
		SearchResultDetailFragment.Callbacks {

	private String[][] techListsArray;
	private IntentFilter[] intentFiltersArray;
	private PendingIntent nfcIntent;
	private boolean nfc_capable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	private android.nfc.NfcAdapter mAdapter;
	private SharedPreferences sp;
	private Fragment rightFragment;
	private long account;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() != null && getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}

		sp = PreferenceManager.getDefaultSharedPreferences(this);

		if (savedInstanceState == null) {
			if (getIntent().hasExtra("fragment")) {
				selectItem(getIntent().getStringExtra("fragment"));
			} else if (sp.contains("startup_fragment")) {
				selectItem(sp.getString("startup_fragment", "search"));
			} else {
				selectItem(1);
			}
		}
		try {
			if (nfc_capable) {
				if (!getPackageManager().hasSystemFeature(
						"android.hardware.nfc")) {
					nfc_capable = false;
				}
			}
			if (nfc_capable) {
				mAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this);
				nfcIntent = PendingIntent.getActivity(this, 0, new Intent(this,
						getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
						0);
				IntentFilter ndef = new IntentFilter(
						android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED);
				try {
					ndef.addDataType("*/*");
				} catch (MalformedMimeTypeException e) {
					throw new RuntimeException("fail", e);
				}
				intentFiltersArray = new IntentFilter[] { ndef, };
				techListsArray = new String[][] { new String[] { android.nfc.tech.NfcV.class
						.getName() } };
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}

		if (app.getLibrary() != null) {
			getSupportActionBar().setSubtitle(
					app.getLibrary().getCity() + " · "
							+ app.getLibrary().getTitle());
		}
	}

	@Override
	public void accountSelected(Account account) {
		this.account = account.getId(); 
		getSupportActionBar().setSubtitle(
				app.getLibrary().getCity() + " · "
						+ app.getLibrary().getTitle());
		if (fragment instanceof OpacActivity.AccountSelectedListener) {
			((OpacActivity.AccountSelectedListener) fragment)
					.accountSelected(account);
		}

//		try {
//			List<SearchField> fields = app.getApi()
//					.getSearchFields(new SQLMetaDataSource(app), app.getLibrary());
//			if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)) //TODO: This won't work with the new implementation. But what is it for?
//				nfc_capable = false;							   //  	   Shouldn't this be set to true if the library supports searching for barcodes?
//		} catch (OpacErrorException e) {
//			e.printStackTrace();
//		}
	}

	public void urlintent() {
		Uri d = getIntent().getData();

		if (d.getHost().equals("opacapp.de")) {
			String[] split = d.getPath().split(":");
			String bib;
			try {
				bib = URLDecoder.decode(split[1], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError("UTF-8 is unknown");
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
				Intent intent = new Intent(MainActivity.this,
						SearchResultDetailActivity.class);
				intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, medianr);
				startActivity(intent);
			} else {
				String title;
				try {
					title = URLDecoder.decode(split[3], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new AssertionError("UTF-8 is unknown");
				}
				Bundle query = new Bundle();
				query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE, title);
				Intent intent = new Intent(MainActivity.this,
						SearchResultListActivity.class);
				intent.putExtra("query", query);
				startActivity(intent);
			}
			finish();
			return;
		}
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_main;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent idata) {
		super.onActivityResult(requestCode, resultCode, idata);

		//TODO: Rewrite this for the new SearchField implementation
		// Barcode
		BarcodeScanIntegrator.ScanResult scanResult = BarcodeScanIntegrator
				.parseActivityResult(requestCode, resultCode, idata);
		if (resultCode != RESULT_CANCELED && scanResult != null) {
			if (scanResult.getContents() == null)
				return;
			if (scanResult.getContents().length() < 3)
				return;
			
			// We won't try to determine which type of barcode was
			// scanned anymore because of the new SearchField
			// implementation
			if (fragment instanceof SearchFragment) {
				((SearchFragment) fragment).barcodeScanned(scanResult);
			} else {
				// this should not happen, but do nothing here just in case
			}

		}
	}

	@Override
	public void scanBarcode() {
		BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(this);
		integrator.initiateScan();
	}

	@SuppressLint("NewApi")
	@Override
	public void onPause() {
		super.onPause();
		if (nfc_capable && sp.getBoolean("nfc_search", false)) {
			try {
				mAdapter.disableForegroundDispatch(this);
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		if (app.getAccount().getId() != account) {
			accountSelected(app.getAccount());
		}
		if (nfc_capable && sp.getBoolean("nfc_search", false)) {
			try {
				mAdapter.enableForegroundDispatch(this, nfcIntent,
						intentFiltersArray, techListsArray);
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onNewIntent(Intent intent) {
		// TODO: Rewrite this for the new SearchField implementation
		/*if (nfc_capable && sp.getBoolean("nfc_search", false)) {
			android.nfc.Tag tag = intent
					.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG);
			String scanResult = readPageToString(tag);
			if (scanResult != null) {
				if (scanResult.length() > 5) {
					Set<String> fields = new HashSet<String>(Arrays.asList(app
							.getApi().getSearchFields()));
					if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)) {
						Map<String, String> query = new HashMap<String, String>();
						query.put(OpacApi.KEY_SEARCH_QUERY_BARCODE, scanResult);
						app.startSearch(this, query);
					} else {
						Toast.makeText(this,
								R.string.barcode_internal_not_supported,
								Toast.LENGTH_LONG).show();
					}
				}
			}
		} */
	}

	/**
	 * Reads the first four blocks of an ISO 15693 NFC tag as ASCII bytes into a
	 * string.
	 * 
	 * @return String Tag memory as a string (bytes converted as ASCII) or
	 *         <code>null</code>
	 */
	@SuppressLint("NewApi")
	public static String readPageToString(android.nfc.Tag tag) {
		if(tag == null)
			return null;
		byte[] id = tag.getId();
		android.nfc.tech.NfcV tech = android.nfc.tech.NfcV.get(tag);
		byte[] readCmd = new byte[3 + id.length];
		readCmd[0] = 0x20; // set "address" flag (only send command to this
		// tag)
		readCmd[1] = 0x20; // ISO 15693 Single Block Read command byte
		System.arraycopy(id, 0, readCmd, 2, id.length); // copy ID
		StringBuilder stringbuilder = new StringBuilder();
		try {
			tech.connect();
			for (int i = 0; i < 4; i++) {
				readCmd[2 + id.length] = (byte) i; // 1 byte payload: block
													// address
				byte[] data;
				data = tech.transceive(readCmd);
				for (int j = 0; j < data.length; j++) {
					if (data[j] > 32 && data[j] < 127) // We only want printable
														// characters, there
														// might be some
														// nullbytes in it
														// otherwise.
						stringbuilder.append((char) data[j]);
				}
			}
			tech.close();
		} catch (IOException e) {
			try {
				tech.close();
			} catch (IOException e1) {
			}
			return null;
		}
		return stringbuilder.toString().trim();
	}

	@Override
	public void showDetail(String mNr) {
		if (isTablet()) {
			rightFragment = new SearchResultDetailFragment();
			Bundle args = new Bundle();
			args.putString(SearchResultDetailFragment.ARG_ITEM_ID, mNr);
			rightFragment.setArguments(args);

			// Insert the fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame_right, rightFragment).commit();
		} else {
			Intent intent = new Intent(this, SearchResultDetailActivity.class);
			intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, mNr);
			startActivity(intent);
		}
	}

	@Override
	public void removeFragment() {
		if(rightFragment != null)
			getSupportFragmentManager().beginTransaction().remove(rightFragment)
					.commit();
	}

	@Override
	protected void setTwoPane(boolean active) {
		super.setTwoPane(active);
		if (!active && rightFragment != null) {
			try {
				removeFragment();
			} catch (Exception e) {

			}
		}
	}

}
