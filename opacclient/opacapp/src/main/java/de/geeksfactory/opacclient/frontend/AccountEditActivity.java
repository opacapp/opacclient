/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.barcode.BarcodeScanIntegrator;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.reminder.ReminderHelper;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.utils.ErrorReporter;

public class AccountEditActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "id";
    public static final String EXTRA_EDITING = "editing";

    private Account account;
    private EditText etLabel;
    private EditText etName;
    private EditText etPassword;
    private View passwordContainer;
    private View usernameContainer;
    private TextInputLayout tilPassword;
    private TextInputLayout tilUsername;
    private RadioGroup rgType;
    private Library lib;

    @SuppressWarnings("SameReturnValue") // Plus Edition compatibility
    protected int getLayoutResource() {
        return R.layout.activity_account_edit;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView image = (ImageView) findViewById(R.id.ivBarcode);
        image.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BarcodeScanIntegrator integrator = new BarcodeScanIntegrator(AccountEditActivity.this);
                integrator.initiateScan();
            }

        });

        etLabel = (EditText) findViewById(R.id.etLabel);
        etName = (EditText) findViewById(R.id.etName);
        etPassword = (EditText) findViewById(R.id.etPassword);
        usernameContainer = findViewById(R.id.llBarcode);
        tilUsername = (TextInputLayout) findViewById(R.id.tilUsername);
        passwordContainer = findViewById(R.id.llPassword);
        tilPassword = (TextInputLayout) findViewById(R.id.tilPassword);
        rgType = (RadioGroup) findViewById(R.id.rgType);

        AccountDataSource data = new AccountDataSource(this);
        account = data.getAccount(getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -1));

        if (account == null) {
            finish();
            return;
        }

        if (account.getLabel().equals(getString(R.string.default_account_name))) {
            etLabel.setText("");
        } else {
            etLabel.setText(account.getLabel());
        }
        etName.setText(account.getName());
        etPassword.setText(account.getPassword());

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                saveAndCheck();
                return false;
            }
        });

        try {
            lib = ((OpacClient) getApplication()).getLibrary(account
                    .getLibrary());
            if (findViewById(R.id.tvCity) != null) {
                TextView tvCity = (TextView) findViewById(R.id.tvCity);
                tvCity.setText(lib.getDisplayName());
            }

            if (lib.getReplacedBy() != null && !"".equals(lib.getReplacedBy())
                    && findViewById(R.id.rlReplaced) != null && ((OpacClient) getApplication()).promotePlusApps()) {
                findViewById(R.id.rlReplaced).setVisibility(View.VISIBLE);
                findViewById(R.id.ivReplacedStore).setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(lib.getReplacedBy()
                                                         .replace("https://play.google.com/store/apps/details?id=", "market://details?id=")));
                                    startActivity(i);
                                } catch (ActivityNotFoundException e) {
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(lib.getReplacedBy()));
                                    startActivity(i);
                                }
                            }
                        });
            } else if (findViewById(R.id.rlReplaced) != null) {
                findViewById(R.id.rlReplaced).setVisibility(View.GONE);
            }
            refreshSslWarning();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            ErrorReporter.handleException(e);
            e.printStackTrace();
        }

        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.rbAnonymous) {
                    passwordContainer.setVisibility(View.GONE);
                    usernameContainer.setVisibility(View.GONE);
                } else if (i == R.id.rbWithCredentials) {
                    passwordContainer.setVisibility(View.VISIBLE);
                    usernameContainer.setVisibility(View.VISIBLE);
                }
                refreshSslWarning();
            }
        });
        if ((account.getName() == null || account.getPassword() == null
                || account.getName().equals("") || account.getPassword().equals(""))
                &&
                (getIntent().getBooleanExtra(EXTRA_EDITING, false) || !lib.isAccountSupported())) {
            ((RadioButton) findViewById(R.id.rbAnonymous)).setChecked(true);
        }

        if (account.getPassword() == null || account.getPassword().equals("")) {
            ((TextInputLayout) findViewById(R.id.tilPassword)).setPasswordVisibilityToggleEnabled(true);
        } else {
            ((TextInputLayout) findViewById(R.id.tilPassword)).setPasswordVisibilityToggleEnabled(false);
        }

    }

    public void refreshSslWarning() {
        if (lib != null && !lib.getData().optString("baseurl", "").contains("https")
                && findViewById(R.id.no_ssl) != null
                && lib.isAccountSupported()
                && rgType.getCheckedRadioButtonId() == R.id.rbWithCredentials) {
            findViewById(R.id.no_ssl).setVisibility(View.VISIBLE);
        } else if (findViewById(R.id.no_ssl) != null) {
            findViewById(R.id.no_ssl).setVisibility(View.GONE);
        }
    }

    private void saveAndCheck() {
        if (etLabel.getText().toString().equals("")) {
            account.setLabel(getString(R.string.default_account_name));
        } else {
            account.setLabel(etLabel.getText().toString().trim());
        }
        if (rgType.getCheckedRadioButtonId() == R.id.rbWithCredentials) {
            if (etPassword.getText().toString().trim().equals("")
                    || etName.getText().toString().trim().equals("")) {
                if (etPassword.getText().toString().trim().equals("")) {
                    tilPassword.setErrorEnabled(true);
                    tilPassword.setError(getString(R.string.please_enter_password));
                } else {
                    tilPassword.setErrorEnabled(false);
                    tilPassword.setError(null);
                }
                if (etName.getText().toString().trim().equals("")) {
                    tilUsername.setErrorEnabled(true);
                    tilUsername.setError(getString(R.string.please_enter_username));
                } else {
                    tilUsername.setErrorEnabled(false);
                    tilUsername.setError("");
                }
                return;
            }

            account.setName(etName.getText().toString().trim());
            account.setPassword(etPassword.getText().toString().trim());
            if (!lib.isAccountSupported()) {
                // Don't check user credentials if the library does not support account features
                save();
                close();
                return;
            }
            new CheckAccountDataTask().execute(account);
        } else {
            account.setName("");
            account.setPassword("");
            save();
            close();
        }
    }

    private void delete() {
        AccountDataSource data = new AccountDataSource(this);
        data.remove(account);

        // Check whether he deleted account was selected
        if (((OpacClient) getApplication()).getAccount().getId() == account
                .getId()) {
            List<Account> available_accounts = data.getAllAccounts();
            if (available_accounts.size() == 0) {
                ((OpacClient) getApplication()).setAccount(0);
                ((OpacClient) getApplication()).addFirstAccount(this);
            } else {
                ((OpacClient) getApplication()).setAccount(available_accounts
                        .get(0).getId());
            }
        }
        new ReminderHelper((OpacClient) getApplication()).generateAlarms();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent idata) {
        super.onActivityResult(requestCode, resultCode, idata);

        // Barcode
        BarcodeScanIntegrator.ScanResult scanResult = BarcodeScanIntegrator
                .parseActivityResult(requestCode, resultCode, idata);
        if (resultCode != RESULT_CANCELED && scanResult != null) {
            if (scanResult.getContents() == null) {
                return;
            } else if (scanResult.getContents().length() < 3) {
                return;
            } else {
                etName.setText(scanResult.getContents());
            }


        }
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_account_edit, menu);
        if (getIntent().hasExtra("adding")
                && getIntent().getBooleanExtra("adding", false)) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        } else if (item.getItemId() == R.id.action_accept) {
            saveAndCheck();
            return true;
        } else if (item.getItemId() == R.id.action_cancel) {
            if (getIntent().hasExtra("adding")
                    && getIntent().getBooleanExtra("adding", false)) {
                delete();
            }
            supportFinishAfterTransition();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.account_delete_confirm)
                   .setCancelable(true)
                   .setNegativeButton(R.string.no,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface d, int id) {
                                   d.cancel();
                               }
                           })
                   .setPositiveButton(R.string.delete,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface d, int id) {
                                   d.dismiss();
                                   delete();
                                   finish();
                               }
                           })
                   .setOnCancelListener(
                           new DialogInterface.OnCancelListener() {
                               @Override
                               public void onCancel(DialogInterface d) {
                                   if (d != null) {
                                       d.cancel();
                                   }
                               }
                           });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        AccountDataSource data = new AccountDataSource(AccountEditActivity.this);
        data.update(account);
        if (((OpacClient) getApplication()).getAccount().getId() == account
                .getId()) {
            ((OpacClient) getApplication()).resetCache();
        }
    }

    private void close() {
        if (getIntent().hasExtra("welcome")
                && getIntent().getBooleanExtra("welcome", false)) {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        } else {
            ActivityCompat.finishAfterTransition(this);
        }
    }

    public void setProgress(boolean show) {
        setProgress(show, true);
    }

    public void setProgress(boolean show, boolean animate) {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        View content = findViewById(R.id.svAccount);

        if (show) {
            if (animate) {
                progress.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
                content.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_out));
            } else {
                progress.clearAnimation();
                content.clearAnimation();
            }
            progress.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
        } else {
            if (animate) {
                progress.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_out));
                content.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
            } else {
                progress.clearAnimation();
                content.clearAnimation();
            }
            progress.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        }
    }

    protected class CheckAccountDataTask extends
            AsyncTask<Account, Void, Exception> {

        @Override
        protected void onPreExecute() {
            setProgress(true);
        }

        @Override
        protected Exception doInBackground(Account... params) {
            try {
                OpacApi api = ((OpacClient) getApplication()).getNewApi(lib);
                api.start();
                api.checkAccountData(account);
            } catch (IOException e) {
                return e;
            } catch (JSONException e) {
                return e;
            } catch (OpacErrorException e) {
                return e;
            } catch (OpacClient.LibraryRemovedException e) {
                return e;
            }
            return null;
        }

        protected void onPostExecute(Exception result) {
            if (AccountEditActivity.this.isFinishing()) return;

            setProgress(false);
            if (result == null) {
                account.setPasswordKnownValid(true);
                save();
                close();
            } else if (result instanceof OpacErrorException) {
                account.setPasswordKnownValid(false);
                OpacErrorException e = (OpacErrorException) result;
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        AccountEditActivity.this);
                builder.setMessage(
                        String.format(
                                getResources().getString(
                                        R.string.user_data_opac_message),
                                e.getMessage()))
                       .setPositiveButton(R.string.yes,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog,
                                           int which) {
                                       save();
                                       close();
                                   }
                               })
                       .setNegativeButton(R.string.no,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog,
                                           int which) {
                                   }
                               }).create().show();
            } else {
                account.setPasswordKnownValid(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        AccountEditActivity.this);
                builder.setMessage(R.string.user_data_connection_error)
                       .setPositiveButton(R.string.yes,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog,
                                           int which) {
                                       save();
                                       close();
                                   }
                               })
                       .setNegativeButton(R.string.no,
                               new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog,
                                           int which) {
                                   }
                               }).create().show();
            }
        }
    }

}
