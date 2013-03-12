package de.geeksfactory.opacclient.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.holoeverywhere.app.AlertDialog;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;

public abstract class OpacActivity extends SlidingFragmentActivity {
	protected OpacClient app;
	protected AlertDialog adialog;
	protected NavigationFragment mFrag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getSupportActionBar().setHomeButtonEnabled(true);

		app = (OpacClient) getApplication();

		setContentView(R.layout.empty_workaround);
		setBehindContentView(R.layout.menu_frame);
		FragmentTransaction t = this.getSupportFragmentManager()
				.beginTransaction();
		mFrag = new NavigationFragment();
		t.replace(R.id.menu_frame, mFrag);
		t.commit();
		// Sliding Menu
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		sm.setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				if (getCurrentFocus() != null) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getCurrentFocus()
							.getWindowToken(), 0);
				}
			}
		});
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}
	
	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (app.getAccount() == null || app.getLibrary() == null) {
			if (!sp.getString("opac_bib", "").equals("")) {
				// Migrate
				Map<String, String> renamed_libs = new HashMap<String, String>();
				renamed_libs.put("Trier (Palais Walderdorff)", "Trier");
				renamed_libs.put("Ludwigshafen (Rhein)", "Ludwigshafen Rhein");
				renamed_libs.put("Neu-Ulm", "NeuUlm");
				renamed_libs.put("Hann. Münden", "HannMünden");
				renamed_libs.put("Münster", "Munster");
				renamed_libs.put("Tübingen", "Tubingen");
				renamed_libs.put("Göttingen", "Gottingen");
				renamed_libs.put("Schwäbisch Hall", "Schwabisch Hall");

				StarDataSource stardata = new StarDataSource(this);
				stardata.renameLibraries(renamed_libs);

				Library lib = null;
				try {
					if (renamed_libs.containsKey(sp.getString("opac_bib", "")))
						lib = app.getLibrary(renamed_libs.get(sp.getString(
								"opac_bib", "")));
					else
						lib = app.getLibrary(sp.getString("opac_bib", ""));
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (lib != null) {
					AccountDataSource data = new AccountDataSource(this);
					data.open();
					Account acc = new Account();
					acc.setLibrary(lib.getIdent());
					acc.setLabel(getString(R.string.default_account_name));
					if (!sp.getString("opac_usernr", "").equals("")) {
						acc.setName(sp.getString("opac_usernr", ""));
						acc.setPassword(sp.getString("opac_password", ""));
					}
					long insertedid = data.addAccount(acc);
					data.close();
					app.setAccount(insertedid);

					Toast.makeText(
							this,
							"Neue Version! Alte Accountdaten wurden wiederhergestellt.",
							Toast.LENGTH_LONG).show();

				} else {
					Toast.makeText(
							this,
							"Neue Version! Wiederherstellung alter Zugangsdaten ist fehlgeschlagen.",
							Toast.LENGTH_LONG).show();
				}
			}
		}
		if (app.getLibrary() == null) {
			// Create new
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		showContent();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_opac, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public interface AccountSelectedListener {
		void accountSelected(Account account);
	}

	public class MetaAdapter extends ArrayAdapter<ContentValues> {

		private List<ContentValues> objects;
		private int spinneritem;

		@Override
		public View getDropDownView(int position, View contentView,
				ViewGroup viewGroup) {
			View view = null;

			if (objects.get(position) == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater
						.inflate(R.layout.simple_spinner_dropdown_item,
								viewGroup, false);
				return view;
			}

			ContentValues item = objects.get(position);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater
						.inflate(R.layout.simple_spinner_dropdown_item,
								viewGroup, false);
			} else {
				view = contentView;
			}

			TextView tvText = (TextView) view.findViewById(android.R.id.text1);
			tvText.setText(item.getAsString("value"));
			return view;
		}

		@Override
		public View getView(int position, View contentView, ViewGroup viewGroup) {
			View view = null;

			if (objects.get(position) == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(spinneritem, viewGroup, false);
				return view;
			}

			ContentValues item = objects.get(position);

			if (contentView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(spinneritem, viewGroup, false);
			} else {
				view = contentView;
			}

			TextView tvText = (TextView) view.findViewById(android.R.id.text1);
			tvText.setText(item.getAsString("value"));
			return view;
		}

		public MetaAdapter(Context context, List<ContentValues> objects,
				int spinneritem) {
			super(context, R.layout.simple_spinner_item, objects);
			this.objects = objects;
			this.spinneritem = spinneritem;
		}

	}

	public void accountSelected() {

	}

	public void selectaccount() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Get the layout inflater
		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.simple_list_dialog, null);

		ListView lv = (ListView) view.findViewById(R.id.lvBibs);
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		final List<Account> accounts = data.getAllAccounts();
		data.close();
		AccountListAdapter adapter = new AccountListAdapter(this, accounts);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				app.setAccount(accounts.get(position).getId());

				adialog.dismiss();

				accountSelected();
			}
		});
		builder.setTitle(R.string.account_select)
				.setView(view)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						})
				.setNeutralButton(R.string.accounts_edit,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								Intent intent = new Intent(OpacActivity.this,
										AccountListActivity.class);
								startActivity(intent);
							}
						});
		adialog = builder.create();
		adialog.show();
	}

	protected void unbindDrawables(View view) {
		if (view == null)
			return;
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			if (!(view instanceof AdapterView)) {
				((ViewGroup) view).removeAllViews();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
