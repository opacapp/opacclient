package de.geeksfactory.opacclient.frontend;

import java.util.List;

import org.holoeverywhere.app.AlertDialog;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public abstract class OpacActivity extends SlidingFragmentActivity {
	protected OpacClient app;
	protected AlertDialog adialog;
	protected Fragment mFrag;

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
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_opac, menu);
		return super.onCreateOptionsMenu(menu);
	}

	protected void dialog_no_user() {
		dialog_no_user(false);
	}

	protected void dialog_no_user(final boolean finish) {
		setContentView(R.layout.answer_error);
		((Button) findViewById(R.id.btPrefs))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(OpacActivity.this,
								AccountListActivity.class);
						startActivity(intent);
					}
				});
		((TextView) findViewById(R.id.tvErrHead)).setText("");
		((TextView) findViewById(R.id.tvErrBody))
				.setText(R.string.status_nouser);
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		setContentView(R.layout.answer_error);
		((Button) findViewById(R.id.btPrefs))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(OpacActivity.this,
								AccountListActivity.class);
						startActivity(intent);
					}
				});
		((TextView) findViewById(R.id.tvErrBody)).setText(s);
	}

	public interface AccountSelectedListener {
		void accountSelected(Account account);
	}

	public void selectaccount() {
		selectaccount(null);
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

	public void selectaccount(final AccountSelectedListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Get the layout inflater
		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.account_add_liblist_dialog, null);

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

				onResume();

				if (listener != null) {
					listener.accountSelected(accounts.get(position));
				}
			}
		});
		builder.setTitle(R.string.account_select)
				.setView(view)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								adialog.cancel();
							}
						})
				.setNeutralButton(R.string.accounts_edit,
						new DialogInterface.OnClickListener() {
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
