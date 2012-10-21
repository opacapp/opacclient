package de.geeksfactory.opacclient;

import org.json.JSONException;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public class InfoActivity extends OpacActivity {

	private WebView wvInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		wvInfo = (WebView) findViewById(R.id.wvInfo);
		TextView tvErr = (TextView) findViewById(R.id.tvErr);
		wvInfo.loadData(getString(R.string.loading), "text/html", null);
		try {
			if (app.ohc.bib.getString(4) == null || app.ohc.bib.getString(4).equals("null")) {
				wvInfo.setVisibility(View.GONE);
				tvErr.setVisibility(View.VISIBLE);
				tvErr.setText(R.string.info_unsupported);
			} else {
				wvInfo.loadUrl(app.ohc.opac_url + app.ohc.bib.getString(4));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			wvInfo.setVisibility(View.GONE);
			tvErr.setVisibility(View.VISIBLE);
			tvErr.setText(R.string.info_error);
		}
	}
}
