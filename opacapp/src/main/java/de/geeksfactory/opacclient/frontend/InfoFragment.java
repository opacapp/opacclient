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

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;

public class InfoFragment extends Fragment implements AccountSelectedListener {

	protected WebView wvInfo;
	protected OpacClient app;
	protected View view;

	public void load() {
		wvInfo = (WebView) view.findViewById(R.id.wvInfo);
		TextView tvErr = (TextView) view.findViewById(R.id.tvErr);
		wvInfo.loadData(getString(R.string.loading), "text/html", null);

		try {
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

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = inflater.inflate(R.layout.fragment_info, container, false);
		app = (OpacClient) getActivity().getApplication();

		setHasOptionsMenu(true);

		load();

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		wvInfo = (WebView) view.findViewById(R.id.wvInfo);

		wvInfo.getSettings().setSupportZoom(true);
		wvInfo.getSettings().setJavaScriptEnabled(true);
		wvInfo.getSettings().setAppCacheMaxSize(5 * 1024 * 1024);
		wvInfo.getSettings().setAppCacheEnabled(true);
		wvInfo.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

		wvInfo.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView v, int progress) {
				ProgressBar Pbar = (ProgressBar) view
						.findViewById(R.id.pbWebProgress);
				if (progress < 100 && Pbar.getVisibility() == View.GONE) {
					Pbar.setVisibility(View.VISIBLE);
				}
				Pbar.setProgress(progress);
				if (progress == 100) {
					Pbar.setVisibility(View.GONE);
				}
			}

		});
		wvInfo.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.contains(app.getLibrary().getData()
						.optString("webviewcontain", "NOPE"))) {
					return false;
				}
				if (getActivity() == null)
					return false;
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;
			}

		});

		return view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			wvInfo.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			load();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(android.view.Menu menu,
			MenuInflater inflater) {
		inflater.inflate(R.menu.activity_info, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void accountSelected(Account account) {
		load();
	}
}
