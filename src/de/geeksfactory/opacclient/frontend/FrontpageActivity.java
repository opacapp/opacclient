package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;

public class FrontpageActivity extends OpacActivity {

	public void urlintent() {
		Uri d = getIntent().getData();

		if (d.getHost().equals("de.geeksfactory.opacclient")) {
			String medianr = d.getQueryParameter("id");

			if (medianr != null) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchResultDetailsActivity.class);
				intent.putExtra("item_id", medianr);
				startActivity(intent);
				finish();
				return;
			}

			String titel = d.getQueryParameter("titel");
			String verfasser = d.getQueryParameter("verfasser");
			String schlag_a = d.getQueryParameter("schlag_a");
			String schlag_b = d.getQueryParameter("schlag_b");
			String isbn = d.getQueryParameter("isbn");
			String jahr_von = d.getQueryParameter("jahr_von");
			String jahr_bis = d.getQueryParameter("jahr_bis");
			String verlag = d.getQueryParameter("verlag");
			Intent myIntent = new Intent(FrontpageActivity.this,
					SearchResultsActivity.class);
			myIntent.putExtra("titel", (titel != null ? titel : ""));
			myIntent.putExtra("verfasser", (verfasser != null ? verfasser : ""));
			myIntent.putExtra("schlag_a", (schlag_a != null ? schlag_a : ""));
			myIntent.putExtra("schlag_b", (schlag_b != null ? schlag_b : ""));
			myIntent.putExtra("isbn", (isbn != null ? isbn : ""));
			myIntent.putExtra("jahr_von", (jahr_von != null ? jahr_von : ""));
			myIntent.putExtra("jahr_bis", (jahr_bis != null ? jahr_bis : ""));
			myIntent.putExtra("verlag", (verlag != null ? verlag : ""));
			startActivity(myIntent);
			finish();
		} else if (d.getHost().equals("www.raphaelmichel.de")) {
			String bib;
			try {
				bib = java.net.URLDecoder.decode(d.getQueryParameter("bib"),
						"UTF-8");
			} catch (UnsupportedEncodingException e) {
				bib = d.getQueryParameter("bib");
			}
			if (!app.getLibrary().getIdent().equals(bib)) {
				Intent i = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("http://www.raphaelmichel.de/opacclient/bibproxy.php/web?"
								+ d.getQuery()));
				startActivity(i);
				return;
			}
			String medianr = d.getQueryParameter("id");
			Intent intent = new Intent(FrontpageActivity.this,
					SearchResultDetailsActivity.class);
			intent.putExtra("item_id", medianr);
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getSupportActionBar().hide();

		if (app.getLibrary() == null) {
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
			return;
		}

		setContentView(R.layout.frontpage_activity);

		TextView tvBn = (TextView) findViewById(R.id.tvBibname);
		if (app.getLibrary().getTitle() != null
				&& !app.getLibrary().getTitle().equals("null"))
			tvBn.setText(app.getLibrary().getCity() + "\n"
					+ app.getLibrary().getTitle());
		else
			tvBn.setText(app.getLibrary().getCity());

		try {
			if (app.getLibrary().getData().getString("information") != null) {
				if (!app.getLibrary().getData().getString("information")
						.equals("null")) {
					((ImageView) findViewById(R.id.ivMInfo))
							.setVisibility(View.VISIBLE);
				} else {
					((ImageView) findViewById(R.id.ivMInfo))
							.setVisibility(View.GONE);
				}
			} else {
				((ImageView) findViewById(R.id.ivMInfo))
						.setVisibility(View.GONE);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		ImageView ivSearch = (ImageView) findViewById(R.id.ivGoSearch);
		ImageView ivScan = (ImageView) findViewById(R.id.ivGoScan);
		ImageView ivAccount = (ImageView) findViewById(R.id.ivGoAccount);
		ImageView ivStarred = (ImageView) findViewById(R.id.ivGoStarred);

		ImageView ivMAccs = (ImageView) findViewById(R.id.ivMAcc);
		ImageView ivMPrefs = (ImageView) findViewById(R.id.ivMPrefs);
		ImageView ivMInfo = (ImageView) findViewById(R.id.ivMInfo);
		ImageView ivMAbout = (ImageView) findViewById(R.id.ivMAbout);

		ivSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchActivity.class);
				startActivity(intent);
			}
		});
		ivAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						AccountActivity.class);
				startActivity(intent);
			}
		});
		ivScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						SearchActivity.class);
				intent.putExtra("barcode", true);
				startActivity(intent);
			}
		});
		ivStarred.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						StarredActivity.class);
				startActivity(intent);
			}
		});
		ivMAccs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				selectaccount(new OpacActivity.AccountSelectedListener() {
					@Override
					public void accountSelected(Account account) {
						TextView tvBn = (TextView) findViewById(R.id.tvBibname);
						if (app.getLibrary().getTitle() != null
								&& !app.getLibrary().getTitle().equals("null"))
							tvBn.setText(app.getLibrary().getCity() + "\n"
									+ app.getLibrary().getTitle());
						else
							tvBn.setText(app.getLibrary().getCity());

						try {
							if (app.getLibrary().getData()
									.getString("information") != null) {
								if (!app.getLibrary().getData()
										.getString("information")
										.equals("null")) {
									((ImageView) findViewById(R.id.ivMInfo))
											.setVisibility(View.VISIBLE);
								} else {
									((ImageView) findViewById(R.id.ivMInfo))
											.setVisibility(View.GONE);
								}
							} else {
								((ImageView) findViewById(R.id.ivMInfo))
										.setVisibility(View.GONE);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		ivMPrefs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						MainPreferenceActivity.class);
				startActivity(intent);
			}
		});
		ivMInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						InfoActivity.class);
				startActivity(intent);
			}
		});
		ivMAbout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(FrontpageActivity.this,
						AboutActivity.class);
				startActivity(intent);
			}
		});

		if (getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}
	}

}
