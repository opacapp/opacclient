package de.geeksfactory.opacclient.frontend;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Library;

public abstract class OpacActivity extends SherlockActivity {
	protected OpacClient app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getSupportActionBar().setHomeButtonEnabled(true);
		app = (OpacClient) getApplication();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = new MenuInflater(this);
		mi.inflate(R.menu.activity_opac, menu);
		try {
			if (app.ohc.bib.getString(4) == null
					|| app.ohc.bib.getString(4).equals("null")) {
				menu.removeItem(R.id.menu_info);
			}
		} catch (Exception e) {
			menu.removeItem(R.id.menu_info);
		}
		return super.onCreateOptionsMenu(menu);
	}

	protected void dialog_no_user() {
		dialog_no_user(false);
	}

	protected void dialog_no_user(final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.status_nouser)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (finish)
									finish();
							}
						})
				.setPositiveButton(R.string.prefs,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(OpacActivity.this,
										MainPreferenceActivity.class);
								startActivity(intent);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void dialog_wrong_credentials(String s, final boolean finish) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.opac_error) + " " + s)
				.setCancelable(false)
				.setNegativeButton(R.string.dismiss,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if (finish)
									finish();
							}
						})
				.setPositiveButton(R.string.prefs,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(OpacActivity.this,
										MainPreferenceActivity.class);
								startActivity(intent);
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void unbindDrawables(View view) {
		if(view == null) return;
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
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, FrontpageActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_about:
			Intent iAbout = new Intent(OpacActivity.this, AboutActivity.class);
			startActivity(iAbout);
			return true;
		case R.id.menu_info:
			Intent iInfo = new Intent(OpacActivity.this, InfoActivity.class);
			startActivity(iInfo);
			return true;
		case R.id.menu_prefs:
			Intent iPrefs = new Intent(OpacActivity.this,
					MainPreferenceActivity.class);
			startActivity(iPrefs);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
