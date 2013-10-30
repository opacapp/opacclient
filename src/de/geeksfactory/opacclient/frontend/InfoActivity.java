/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import org.holoeverywhere.widget.ProgressBar;
import org.json.JSONException;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import de.geeksfactory.opacclient.R;

public class InfoActivity extends OpacActivity {

	private WebView wvInfo;

	public void load() {
		wvInfo = (WebView) findViewById(R.id.wvInfo);
		TextView tvErr = (TextView) findViewById(R.id.tvErr);
		wvInfo.loadData(getString(R.string.loading), "text/html", null);

		try {
			ConnectivityManager cm = (ConnectivityManager) this
					.getSystemService(Activity.CONNECTIVITY_SERVICE);
			String infoUrl = app.getLibrary().getData()
					.getString("information");
			if (infoUrl == null || infoUrl.equals("null")) {
				wvInfo.setVisibility(View.GONE);
				tvErr.setVisibility(View.VISIBLE);
				tvErr.setText(R.string.info_unsupported);
			} else if (infoUrl.startsWith("http")) {
				wvInfo.loadUrl(infoUrl);
			} else {
				wvInfo.loadUrl(app.getLibrary().getData().getString("baseurl")
						+ infoUrl);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			wvInfo.setVisibility(View.GONE);
			tvErr.setVisibility(View.VISIBLE);
			tvErr.setText(R.string.info_error);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_activity);

		load();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SlidingMenu sm = getSlidingMenu();
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

		wvInfo = (WebView) findViewById(R.id.wvInfo);

		wvInfo.getSettings().setSupportZoom(true);
		wvInfo.getSettings().setJavaScriptEnabled(true);
		wvInfo.getSettings().setAppCacheMaxSize(5 * 1024 * 1024);
		wvInfo.getSettings().setAppCacheEnabled(true);
		wvInfo.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

		wvInfo.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				ProgressBar Pbar = (ProgressBar) findViewById(R.id.pbWebProgress);
				if (progress < 100 && Pbar.getVisibility() == View.GONE) {
					Pbar.setVisibility(View.VISIBLE);
				}
				Pbar.setProgress(progress);
				if (progress == 100) {
					Pbar.setVisibility(View.GONE);
				}
			}
		});
	}

	@Override
	public void accountSelected() {
		super.accountSelected();
		load();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		}
		return super.onOptionsItemSelected(item);
	}
}
