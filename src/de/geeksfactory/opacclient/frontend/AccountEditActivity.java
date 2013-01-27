package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.acra.ACRA;
import org.json.JSONException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountEditActivity extends SherlockActivity {

	private Account account;
	private EditText etLabel;
	private EditText etName;
	private EditText etPassword;
	private Library lib;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_edit_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		etLabel = (EditText) findViewById(R.id.etLabel);
		etName = (EditText) findViewById(R.id.etName);
		etPassword = (EditText) findViewById(R.id.etPassword);

		AccountDataSource data = new AccountDataSource(this);
		data.open();
		account = data.getAccount(getIntent().getLongExtra("id", -1));
		data.close();

		etLabel.setText(account.getLabel());
		etName.setText(account.getName());
		etPassword.setText(account.getPassword());

		try {
			lib = ((OpacClient) getApplication()).getLibrary(account
					.getLibrary());
			TextView tvCity = (TextView) findViewById(R.id.tvCity);
			if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
				tvCity.setText(lib.getCity() + "\n" + lib.getTitle());
			} else {
				tvCity.setText(lib.getCity());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
			e.printStackTrace();
		}
	}

	private void save() {
		account.setLabel(etLabel.getText().toString());
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
				Intent intent = new Intent(this, WelcomeActivity.class);
				startActivity(intent);
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
		getSupportMenuInflater().inflate(R.menu.activity_account_edit, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_accept:
			save();
			finish();
			return true;
		case R.id.action_cancel:
			if (getIntent().hasExtra("adding")
					&& getIntent().getBooleanExtra("adding", false))
				delete();
			finish();
			return true;
		case R.id.action_delete:
			delete();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
