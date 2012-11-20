package de.geeksfactory.opacclient.frontend;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountEditActivity extends SherlockActivity {

	private Account account;
	private EditText etLabel;
	private EditText etName;
	private EditText etPassword;
	
	
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
	}

	private void save() {
		account.setLabel(etLabel.getText().toString());
		account.setName(etName.getText().toString());
		account.setPassword(etPassword.getText().toString());
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		data.update(account);
		data.close();
	}

	@Override
	public void onBackPressed() {
		save();
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
