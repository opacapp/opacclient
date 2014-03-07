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

import java.io.IOException;
import java.util.List;

import org.acra.ACRA;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.json.JSONException;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountEditActivity extends Activity {

	public static final String EXTRA_ACCOUNT_ID = "id";

	private Account account;
	private EditText etLabel;
	private EditText etName;
	private EditText etPassword;
	private Library lib;

	protected int getLayoutResource() {
		return R.layout.activity_account_edit;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutResource());
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		etLabel = (EditText) findViewById(R.id.etLabel);
		etName = (EditText) findViewById(R.id.etName);
		etPassword = (EditText) findViewById(R.id.etPassword);

		AccountDataSource data = new AccountDataSource(this);
		data.open();
		account = data.getAccount(getIntent()
				.getLongExtra(EXTRA_ACCOUNT_ID, -1));
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
				if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
					tvCity.setText(lib.getCity() + "\n" + lib.getTitle());
				} else {
					tvCity.setText(lib.getCity());
				}
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

			if (!lib.getData().getString("baseurl").contains("https")
					&& findViewById(R.id.no_ssl) != null && lib.getSupport().contains("Konto")) {
				findViewById(R.id.no_ssl).setVisibility(View.VISIBLE);
			} else if (findViewById(R.id.no_ssl) != null) {
				findViewById(R.id.no_ssl).setVisibility(View.GONE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
			e.printStackTrace();
		}
	}

	private void save() {
		if (etLabel.getText().toString().equals("")) {
			account.setLabel(getString(R.string.default_account_name));
		} else {
			account.setLabel(etLabel.getText().toString());
		}
		account.setName(etName.getText().toString());
		account.setPassword(etPassword.getText().toString());
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		data.update(account);
		data.close();
		if (((OpacClient) getApplication()).getAccount().getId() == account
				.getId()) {
			((OpacClient) getApplication()).resetCache();
		}
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
		} else if (item.getItemId() == R.id.action_accept) {
			if (getIntent().hasExtra("welcome")
					&& getIntent().getBooleanExtra("welcome", false)) {
				save();
				Intent i = new Intent(this, MainActivity.class);
				startActivity(i);
			} else {
				save();
				finish();
			}
			return true;
		} else if (item.getItemId() == R.id.action_cancel) {
			if (getIntent().hasExtra("adding")
					&& getIntent().getBooleanExtra("adding", false))
				delete();
			finish();
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
									if (d != null)
										d.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
