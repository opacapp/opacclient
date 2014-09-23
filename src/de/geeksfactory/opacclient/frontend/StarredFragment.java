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

import java.util.ArrayList;
import java.util.List;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.preference.PreferenceManager;
import org.holoeverywhere.preference.SharedPreferences;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.OpacActivity.AccountSelectedListener;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Starred;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.storage.JsonSearchFieldDataSource;
import de.geeksfactory.opacclient.storage.StarDataSource;
import de.geeksfactory.opacclient.storage.StarDatabase;

public class StarredFragment extends Fragment implements
		LoaderCallbacks<Cursor>, AccountSelectedListener {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	private ItemListAdapter adapter;
	protected View view;
	protected OpacClient app;
	private Callback mCallback;
	private ListView listView;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	public interface Callback {
		public void showDetail(String mNr);

		public void removeFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		setHasOptionsMenu(true);

		view = inflater.inflate(R.layout.fragment_starred);
		app = (OpacClient) getActivity().getApplication();

		adapter = new ItemListAdapter();

		listView = (ListView) view.findViewById(R.id.lvStarred);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Starred item = (Starred) view.findViewById(R.id.ivDelete)
						.getTag();
				if (item.getMNr() == null || item.getMNr().equals("null")
						|| item.getMNr().equals("")) {

					SharedPreferences sp = PreferenceManager
							.getDefaultSharedPreferences(getActivity());
					List<SearchQuery> query = new ArrayList<SearchQuery>();
					List<SearchField> fields = new JsonSearchFieldDataSource(
							app).getSearchFields(app.getLibrary().getIdent());
					for (SearchField field : fields) {
						if (field.getMeaning() == Meaning.TITLE) {
							query.add(new SearchQuery(field, item.getTitle()));
						} else if (field.getMeaning() == Meaning.HOME_BRANCH) {
							query.add(new SearchQuery(field, sp.getString(
									OpacClient.PREF_HOME_BRANCH_PREFIX
											+ app.getAccount().getId(), null)));
						}
					}
					app.startSearch(getActivity(), query);
				} else {
					mCallback.showDetail(item.getMNr());
				}
			}
		});
		listView.setClickable(true);
		listView.setTextFilterEnabled(true);

		getSupportActivity().getSupportLoaderManager()
				.initLoader(0, null, this);
		listView.setAdapter(adapter);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}

		setActivateOnItemClick(((OpacActivity) getActivity()).isTablet());

		return view;
	}

	@Override
	public void onCreateOptionsMenu(android.view.Menu menu,
			MenuInflater inflater) {
		inflater.inflate(R.menu.activity_starred, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == R.id.action_export) {
			export();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void accountSelected(Account account) {
		getActivity().getSupportLoaderManager().restartLoader(0, null, this);
	}

	public void remove(Starred item) {
		StarDataSource data = new StarDataSource(getActivity());
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
					Starred item = (Starred) arg0.getTag();
					remove(item);
					mCallback.removeFragment();
				}
			});
		}

		public ItemListAdapter() {
			super(getActivity(), R.layout.listitem_starred, null,
					new String[] { "bib" }, null, 0);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (app.getLibrary() != null)
			return new CursorLoader(getActivity(),
					app.getStarProviderStarUri(), StarDatabase.COLUMNS,
					StarDatabase.STAR_WHERE_LIB, new String[] { app
							.getLibrary().getIdent() }, null);
		else
			return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
		if (cursor.getCount() == 0)
			view.findViewById(R.id.tvWelcome).setVisibility(View.VISIBLE);
		else
			view.findViewById(R.id.tvWelcome).setVisibility(View.GONE);
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

		StarDataSource data = new StarDataSource(getActivity());
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallback = (Callback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement SearchFragment.Callback");
		}
	}

	@Override
	public void onResume() {
		getActivity().getSupportLoaderManager().restartLoader(0, null, this);
		super.onResume();
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	private void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		listView.setChoiceMode(activateOnItemClick ? AbsListView.CHOICE_MODE_SINGLE
				: AbsListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == AdapterView.INVALID_POSITION) {
			listView.setItemChecked(mActivatedPosition, false);
		} else {
			listView.setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != AdapterView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}
}
