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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.utils.ErrorReporter;

public class AccountEditActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "id";

    private Account account;
    private EditText etLabel;
    private EditText etName;
    private EditText etPassword;
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

        etLabel = (EditText) findViewById(R.id.etLabel);
        etName = (EditText) findViewById(R.id.etName);
        etPassword = (EditText) findViewById(R.id.etPassword);

        AccountDataSource data = new AccountDataSource(this);
        data.open();
        account = data.getAccount(getIntent()
                .getLongExtra(EXTRA_ACCOUNT_ID, -1));

        if (account == null) {
            finish();
            return;
        }

        data.close();

        if (account.getLabel().equals(getString(R.string.default_account_name))) {
            etLabel.setText("");
        } else {
            etLabel.setText(account.getLabel());
        }
        etName.setText(account.getName());
        etPassword.setText(account.getPassword());

        try {
            lib = ((OpacClient) getApplication()).getLibrary(account
                    .getLibrary());
            if (findViewById(R.id.tvCity) != null) {
                TextView tvCity = (TextView) findViewById(R.id.tvCity);
                tvCity.setText(lib.getDisplayName());
            }

            if (lib.getReplacedBy() != null
                    && findViewById(R.id.rlReplaced) != null) {
                findViewById(R.id.rlReplaced).setVisibility(View.VISIBLE);
                findViewById(R.id.ivReplacedStore).setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id="
                                                    + lib.getReplacedBy()));
                                    startActivity(i);
                                } catch (ActivityNotFoundException e) {
                                    Log.i("play", "no market installed");
                                }
                            }
                        });
            } else if (findViewById(R.id.rlReplaced) != null) {
                findViewById(R.id.rlReplaced).setVisibility(View.GONE);
            }
            try {
                if (!lib.getData().getString("baseurl").contains("https")
                        && findViewById(R.id.no_ssl) != null
                        && lib.isAccountSupported()) {
                    findViewById(R.id.no_ssl).setVisibility(View.VISIBLE);
                } else if (findViewById(R.id.no_ssl) != null) {
                    findViewById(R.id.no_ssl).setVisibility(View.GONE);
                }
            } catch (Exception e) {

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            ErrorReporter.handleException(e);
            e.printStackTrace();
        }
    }

    private void saveAndCheck() {
        if (etLabel.getText().toString().equals("")) {
            account.setLabel(getString(R.string.default_account_name));
        } else {
            account.setLabel(etLabel.getText().toString().trim());
        }
        account.setName(etName.getText().toString().trim());
        account.setPassword(etPassword.getText().toString().trim());
        if (etPassword.getText().toString().trim().equals("")) {
            // Don't check user credentials if there are no credentials
            save();
            close();
            return;
        }
        new CheckAccountDataTask().execute(account);
    }

    private void delete() {
        AccountDataSource data = new AccountDataSource(this);
        data.open();
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
        data.close();
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
        data.open();
        data.update(account);
        data.close();
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
