package de.geeksfactory.opacclient.frontend;

import java.io.UnsupportedEncodingException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

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
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(this);
			String bib;
			try {
				bib = java.net.URLDecoder.decode(d.getQueryParameter("bib"),
						"UTF-8");
			} catch (UnsupportedEncodingException e) {
				bib = d.getQueryParameter("bib");
			}
			if (!sp.getString("opac_bib", "").equals(bib)) {
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getSupportActionBar().hide();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (sp.getString("opac_url", "").equals("")
				|| sp.getString("opac_bib", "").equals("")) {
			Intent intent = new Intent(this, WelcomeActivity.class);
			startActivity(intent);
			return;
		}

		setContentView(R.layout.frontpage_activity);

		TextView tvBn = (TextView) findViewById(R.id.tvBibname);
		tvBn.setText(sp.getString("opac_bib", "Mannheim"));

		ImageView ivSearch = (ImageView) findViewById(R.id.ivGoSearch);
		ImageView ivScan = (ImageView) findViewById(R.id.ivGoScan);
		ImageView ivAccount = (ImageView) findViewById(R.id.ivGoAccount);
		ImageView ivStarred = (ImageView) findViewById(R.id.ivGoStarred);

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

		if (getIntent().getAction() != null) {
			if (getIntent().getAction().equals("android.intent.action.VIEW")) {
				urlintent();
				return;
			}
		}
	}

}
