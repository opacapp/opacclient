package de.geeksfactory.opacclient.frontend;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.reminder.Alarm;
import de.geeksfactory.opacclient.reminder.ReminderBroadcastReceiver;
import de.geeksfactory.opacclient.reminder.SyncAccountAlarmListener;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.DataIntegrityException;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;

public class MainActivity extends OpacActivity
        implements SearchFragment.Callback, StarredFragment.Callback,
        SearchResultDetailFragment.Callbacks {

    public static final String EXTRA_FRAGMENT = "fragment";
    public static final String ACTION_SEARCH = "de.geeksfactory.opacclient.SEARCH";
    public static final String ACTION_ACCOUNT = "de.geeksfactory.opacclient.ACCOUNT";
    private String[][] techListsArray;
    private IntentFilter[] intentFiltersArray;
    private PendingIntent nfcIntent;
    private boolean nfc_capable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private android.nfc.NfcAdapter mAdapter;
    private SharedPreferences sp;
    private Fragment rightFragment;
    private long account;

    /**
     * Reads the first four blocks of an ISO 15693 NFC tag as ASCII bytes into a string.
     *
     * @return String Tag memory as a string (bytes converted as ASCII) or <code>null</code>
     */
    @SuppressLint("NewApi")
    public static String readPageToString(android.nfc.Tag tag) {
        if (tag == null) {
            return null;
        }
        byte[] id = tag.getId();
        android.nfc.tech.NfcV tech = android.nfc.tech.NfcV.get(tag);
        byte[] readCmd = new byte[3 + id.length];
        readCmd[0] = 0x20; // set "address" flag (only send command to this tag)
        readCmd[1] = 0x20; // ISO 15693 Single Block Read command byte
        System.arraycopy(id, 0, readCmd, 2, id.length); // copy ID
        StringBuilder stringbuilder = new StringBuilder();
        try {
            tech.connect();
            for (int i = 0; i < 4; i++) {
                readCmd[2 + id.length] = (byte) i; // 1 byte payload: block address
                byte[] data;
                data = tech.transceive(readCmd);
                for (byte aData1 : data) {
                    if (aData1 > 32 && aData1 < 127) // We only want printable characters, there
                    // might be some nullbytes in it otherwise.
                    {
                        stringbuilder.append((char) aData1);
                    }
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

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        String itemToSelect = null;

        if (getIntent() != null && getIntent().getAction() != null) {
            if (getIntent().getAction().equals("android.intent.action.VIEW")) {
                urlintent();
            } else if (getIntent().getAction().equals(ACTION_SEARCH)
                    || getIntent().getAction().equals(ACTION_ACCOUNT)) {
                String lib = getIntent().getStringExtra("library");
                AccountDataSource adata = new AccountDataSource(this);
                List<Account> accounts = adata.getAllAccounts(lib);

                if (accounts.size() == 0) {
                    // Check if library exists (an IOException should be thrown otherwise, correct?)
                    try {
                        app.getLibrary(lib);
                    } catch (IOException | JSONException e) {
                        return;
                    }
                    Account account = new Account();
                    account.setLibrary(lib);
                    account.setLabel(getString(R.string.default_account_name));
                    account.setName("");
                    account.setPassword("");
                    long id = adata.addAccount(account);
                    selectaccount(id);
                } else if (accounts.size() == 1) {
                    selectaccount(accounts.get(0).getId());
                } else if (accounts.size() > 0) {
                    if (getIntent().getAction().equals(ACTION_ACCOUNT)) {
                        List<Account> accountsWithPassword = new ArrayList<>();
                        for (Account account : accounts) {
                            if (account.getName() != null && account.getPassword() != null
                                    && !account.getName().isEmpty()
                                    && !account.getPassword().isEmpty()) {
                                accountsWithPassword.add(account);
                            }
                        }
                        if (accountsWithPassword.size() == 1) {
                            selectaccount(accountsWithPassword.get(0).getId());
                        } else {
                            showAccountSelectDialog(accounts);
                        }
                    } else {
                        showAccountSelectDialog(accounts);
                    }
                }

                if (getIntent().getAction().equals(ACTION_SEARCH)) {
                    itemToSelect = "search";
                } else {
                    itemToSelect = "account";
                }
            }
        }

        if (savedInstanceState == null) {
            if (getIntent().hasExtra(EXTRA_FRAGMENT)) {
                selectItem(getIntent().getStringExtra(EXTRA_FRAGMENT));
            } else if (getIntent().hasExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID)) {
                AccountDataSource adata = new AccountDataSource(this);
                long alid = getIntent().getLongExtra(ReminderBroadcastReceiver.EXTRA_ALARM_ID, -1);
                Alarm alarm = adata.getAlarm(alid);
                if (alarm == null) {
                    throw new DataIntegrityException("Unknown alarm ID " + alid + " received.");
                }
                List<LentItem> items = adata.getLentItems(alarm.media);
                if (items.size() > 0) {
                    long firstAccount = items.get(0).getAccount();
                    boolean multipleAccounts = false;
                    for (LentItem item : items) {
                        if (item.getAccount() != firstAccount) {
                            multipleAccounts = true;
                            break;
                        }
                    }
                    if (multipleAccounts) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                drawerLayout.openDrawer(drawer);
                            }
                        }, 500);
                    } else {
                        selectaccount(firstAccount);
                        selectItem("account");
                    }
                }
            } else if (itemToSelect != null) {
                selectItem(itemToSelect);
            } else if (sp.contains("startup_fragment")) {
                selectItem(sp.getString("startup_fragment", "search"));
            } else {
                selectItem(0);
            }
        }
        try {
            if (nfc_capable) {
                if (!getPackageManager().hasSystemFeature("android.hardware.nfc")) {
                    nfc_capable = false;
                }
            }
            if (nfc_capable) {
                mAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this);
                nfcIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter ndef = new IntentFilter(android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED);
                try {
                    ndef.addDataType("*/*");
                } catch (MalformedMimeTypeException e) {
                    throw new RuntimeException("fail", e);
                }
                intentFiltersArray = new IntentFilter[]{ndef,};
                techListsArray = new String[][]{
                        new String[]{android.nfc.tech.NfcV.class.getName()}};
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (app.getLibrary() != null) {
            getSupportActionBar().setSubtitle(app.getLibrary().getDisplayName());
        }

        showUpdateInfoDialog();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(null);
        }
    }

    private void showAccountSelectDialog(final List<Account> accounts) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.account_select)
                .setAdapter(
                        new AccountListAdapter(this, accounts)
                                .setHighlightActiveAccount(false),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                selectaccount(accounts.get(which).getId());
                            }
                        }).create().show();
    }

    @Override
    public void accountSelected(Account account) {
        super.accountSelected(account);
        this.account = account.getId();
        getSupportActionBar().setSubtitle(app.getLibrary().getDisplayName());
        if (fragment instanceof OpacActivity.AccountSelectedListener) {
            ((OpacActivity.AccountSelectedListener) fragment).accountSelected(account);
        }

        nfcHint();

        //		try {
        //			List<SearchField> fields = app.getApi()
        //					.getSearchFields(new SQLMetaDataSource(app), app.getLibrary());
        //			if (fields.contains(OpacApi.KEY_SEARCH_QUERY_BARCODE)) //TODO: This won't work
        // with
        // the new implementation. But what is it for?
        //				nfc_capable = false;							   //  	   Shouldn't this
        // be set
        // to true if the library supports searching for barcodes?
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

            if (app.getLibrary() == null || !app.getLibrary().getIdent().equals(bib)) {
                AccountDataSource adata = new AccountDataSource(this);
                List<Account> accounts = adata.getAllAccounts(bib);
                if (accounts.size() > 0) {
                    app.setAccount(accounts.get(0).getId());
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://de.opacapp.net/" + d.getPath()));
                    startActivity(i);
                    return;
                }
            }
            String medianr = split[2];
            if (medianr.length() > 1) {
                Intent intent = new Intent(MainActivity.this, SearchResultDetailActivity.class);
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
                query.putString("title", title);  // TODO: This won't work with most modern APIs
                Intent intent = new Intent(MainActivity.this, SearchResultListActivity.class);
                intent.putExtra("query", query);
                startActivity(intent);
            }
            finish();
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent idata) {
        super.onActivityResult(requestCode, resultCode, idata);
        fragment.onActivityResult(requestCode, resultCode, idata);

        //TODO: Rewrite this for the new SearchField implementation
        // Barcode
        BarcodeScanIntegrator.ScanResult scanResult = BarcodeScanIntegrator
                .parseActivityResult(requestCode, resultCode, idata);
        if (resultCode != RESULT_CANCELED && scanResult != null) {
            if (scanResult.getContents() == null) {
                return;
            }
            if (scanResult.getContents().length() < 3) {
                return;
            }

            // We won't try to determine which type of barcode was
            // scanned anymore because of the new SearchField
            // implementation
            if (fragment instanceof SearchFragment) {
                // the fragment should be a Search Fragment, but check here just in case
                ((SearchFragment) fragment).barcodeScanned(scanResult);
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
            } catch (IllegalStateException | SecurityException e) {
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

        nfcHint();

        if (nfc_capable && sp.getBoolean("nfc_search", false)) {
            try {
                mAdapter.enableForegroundDispatch(this, nfcIntent, intentFiltersArray,
                        techListsArray);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(10)
    private void nfcHint() {
        if (nfc_capable && !sp.getBoolean("nfc_search", false) && Build.VERSION.SDK_INT >= 10 &&
                !sp.getBoolean("nfc_hint_shown", false) && app.getLibrary().isNfcSupported()) {
            new AlertDialog.Builder(this)
                    .setView(LayoutInflater.from(this).inflate(R.layout.dialog_nfc_hint, null))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sp.edit().putBoolean("nfc_search", true)
                                    .putBoolean("nfc_hint_shown", true).apply();
                            Toast.makeText(MainActivity.this, R.string.nfc_activated,
                                    Toast.LENGTH_LONG).show();
                            mAdapter.enableForegroundDispatch(MainActivity.this, nfcIntent,
                                    intentFiltersArray, techListsArray);
                        }
                    }).setNegativeButton(R.string.no_thanks, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit().putBoolean("nfc_hint_shown", true).apply();
                    Toast.makeText(MainActivity.this, R.string.nfc_not_activated, Toast.LENGTH_LONG)
                            .show();
                }
            }).show();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onNewIntent(Intent intent) {
        if (nfc_capable && sp.getBoolean("nfc_search", false)) {
            android.nfc.Tag tag = intent.getParcelableExtra(android.nfc.NfcAdapter.EXTRA_TAG);
            String scanResult = readPageToString(tag);
            if (scanResult != null) {
                if (scanResult.length() > 5) {
                    SearchFieldDataSource source = new JsonSearchFieldDataSource(this);
                    if (source.hasSearchFields(app.getLibrary().getIdent())) {
                        List<SearchField> fields = source
                                .getSearchFields(app.getLibrary().getIdent());
                        for (SearchField field : fields) {
                            if (field.getMeaning() == SearchField.Meaning.BARCODE) {
                                List<SearchQuery> queries = new ArrayList<>();
                                queries.add(new SearchQuery(field, scanResult));
                                app.startSearch(this, queries);
                                return;
                            }
                        }
                    }
                    Intent detailIntent = new Intent(this, SearchResultDetailActivity.class);
                    detailIntent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, scanResult);
                    startActivity(detailIntent);
                }
            }
        }
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
            fragmentManager.beginTransaction().replace(R.id.content_frame_right, rightFragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, SearchResultDetailActivity.class);
            intent.putExtra(SearchResultDetailFragment.ARG_ITEM_ID, mNr);
            startActivity(intent);
        }
    }

    @Override
    public void removeFragment() {
        if (rightFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(rightFragment).commit();
        }
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

    public void showUpdateInfoDialog() {
        if (!getApplicationContext().getPackageName().startsWith("de.geeksfactory.opacclient")) {
            return;  // Never show e.g. in plus edition
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Calendar cal = Calendar.getInstance();
        cal.set(2017, 7, 1, 0, 0, 0);
        if ((new Date()).after(cal.getTime())) {
            return;
        }
        if (!sp.contains("seen_update_dialog_5.1.1")) {
            DialogFragment newFragment = new UpdateInfoDialogFragment();
            newFragment.show(getSupportFragmentManager(), "updateinfo");
            sp.edit().putBoolean("seen_update_dialog_5.1.1", true).commit();
        }
    }

}
