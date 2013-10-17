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

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.Starred;
import de.geeksfactory.opacclient.storage.StarDataSource;
import de.geeksfactory.opacclient.storage.StarDatabase;

public class StarredActivity extends OpacActivity implements
		LoaderCallbacks<Cursor> {

	private ItemListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.starred_activity);

		String bib = app.getLibrary().getIdent();

		final StarDataSource data = new StarDataSource(this);
		List<Starred> items = data.getAllItems(bib);

		adapter = new ItemListAdapter();

		ListView lv = (ListView) findViewById(R.id.lvStarred);

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Starred item = (Starred) view.findViewById(R.id.ivDelete)
						.getTag();
				if (item.getMNr() == null || item.getMNr().equals("null")
						|| item.getMNr().equals("")) {

					SharedPreferences sp = PreferenceManager
							.getDefaultSharedPreferences(StarredActivity.this);
					Bundle query = new Bundle();
					query.putString(OpacApi.KEY_SEARCH_QUERY_TITLE,
							item.getTitle());
					query.putString(
							OpacApi.KEY_SEARCH_QUERY_HOME_BRANCH,
							sp.getString(OpacClient.PREF_HOME_BRANCH_PREFIX
									+ app.getAccount().getId(), null));
					app.startSearch(StarredActivity.this, query);
				} else {
					Intent intent = new Intent(StarredActivity.this,
							SearchResultDetailsActivity.class);
					intent.putExtra("item_id", item.getMNr());
					startActivity(intent);
				}
			}
		});
		lv.setClickable(true);
		lv.setTextFilterEnabled(true);

		getSupportLoaderManager().initLoader(0, null, this);
		lv.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_starred, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_export) {
			export();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void accountSelected() {
		getSupportLoaderManager().restartLoader(0, null, this);
		super.accountSelected();
	}

	public void remove(Starred item) {
		StarDataSource data = new StarDataSource(this);
		data.remove(item);
	}

	private class ItemListAdapter extends SimpleCursorAdapter {

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Starred item = StarDataSource.cursorToItem(cursor);

			TextView tv = (TextView) view.findViewById(R.id.tvTitle);
			if (item.getTitle() != null)
				tv.setText(Html.fromHtml(item.getTitle()));
			else
				tv.setText("");

			ImageView iv = (ImageView) view.findViewById(R.id.ivDelete);
			iv.setFocusableInTouchMode(false);
			iv.setFocusable(false);
			iv.setTag(item);
			iv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					remove((Starred) arg0.getTag());
				}
			});
		}

		public ItemListAdapter() {
			super(StarredActivity.this, R.layout.starred_item, null,
					new String[] { "bib" }, null, 0);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this, app.getStarProviderStarUri(),
				StarDatabase.COLUMNS, StarDatabase.STAR_WHERE_LIB,
				new String[] { app.getLibrary().getIdent() }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
		if (cursor.getCount() == 0)
			findViewById(R.id.tvWelcome).setVisibility(View.VISIBLE);
		else
			findViewById(R.id.tvWelcome).setVisibility(View.GONE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.swapCursor(null);
	}

	protected void export() {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

		StringBuilder text = new StringBuilder();

		StarDataSource data = new StarDataSource(this);
		List<Starred> items = data.getAllItems(app.getLibrary().getIdent());
		for (Starred item : items) {
			text.append(item.getTitle());
			text.append("\n");
			String shareUrl = app.getApi().getShareUrl(item.getMNr(),
					item.getTitle());
			if (shareUrl != null) {
				text.append(shareUrl);
				text.append("\n");
			}
			text.append("\n");
		}

		intent.putExtra(Intent.EXTRA_TEXT, text.toString().trim());
		startActivity(Intent.createChooser(intent,
				getResources().getString(R.string.share)));
	}
}
