package de.geeksfactory.opacclient.frontend;

import org.json.JSONException;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;

public class InfoActivity extends OpacActivity {

	private WebView wvInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_activity);
		wvInfo = (WebView) findViewById(R.id.wvInfo);
		TextView tvErr = (TextView) findViewById(R.id.tvErr);
		wvInfo.loadData(getString(R.string.loading), "text/html", null);
		try {
			if (app.getLibrary().getData().getString("information") == null
					|| app.getLibrary().getData().getString("information").equals("null")) {
				wvInfo.setVisibility(View.GONE);
				tvErr.setVisibility(View.VISIBLE);
				tvErr.setText(R.string.info_unsupported);
			} else {
				wvInfo.loadUrl(app.getLibrary().getData().getString("baseurl") + app.getLibrary().getData().getString("information"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			wvInfo.setVisibility(View.GONE);
			tvErr.setVisibility(View.VISIBLE);
			tvErr.setText(R.string.info_error);
		}
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
