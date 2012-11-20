package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountListActivity extends SherlockActivity {

	List<Account> accounts;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_list);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		ListView lvAccounts = (ListView) findViewById(R.id.lvAccounts);
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		List<Account> accounts = data.getAllAccounts();
		data.close();
		AccountListAdapter adapter = new AccountListAdapter(this, accounts);
		lvAccounts.setAdapter(adapter);
	}

	public class AccountListAdapter extends ArrayAdapter<Account> {
		private List<Account> objects;

		@Override
		public View getView(int position, View contentView, ViewGroup viewGroup) {
			View view = null;

			// position always 0-7
			if (objects.get(position) == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.account_listitem,
						viewGroup, false);
				return view;
			}

			Account item = objects.get(position);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.account_listitem,
						viewGroup, false);
			} else {
				view = contentView;
			}

			Library lib;
			try {
				lib = ((OpacClient) getApplication()).getLibrary(item.getBib());
				TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
				if (lib.getTitle() != null && !lib.getTitle().equals("null")) {
					tvCity.setText(lib.getCity() + "\n" + lib.getTitle());
				} else {
					tvCity.setText(lib.getCity());
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			TextView tvName = (TextView) view.findViewById(R.id.tvName);
			if (item.getName() != null)
				tvName.setText(item.getName());
			TextView tvLabel = (TextView) view.findViewById(R.id.tvLabel);
			if (item.getLabel() != null)
				tvLabel.setText(item.getLabel());
			return view;
		}

		public AccountListAdapter(Context context, List<Account> objects) {
			super(context, R.layout.account_listitem, objects);
			this.objects = objects;
		}
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
