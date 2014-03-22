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

import com.commonsware.cwac.endless.EndlessAdapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

public class ResultsAdapterEndless extends EndlessAdapter {
	private List<SearchResult> objects;
	private OnLoadMoreListener listener;
	private int page = 1;
	private int maxPage;
	private int resultCount;
	private List<SearchResult> itemsToAppend;
	
	public interface OnLoadMoreListener {
		public List<SearchResult> onLoadMore(int page) throws Exception;
	}

	public ResultsAdapterEndless(Context context, SearchRequestResult result, OnLoadMoreListener listener) {
		super(context, new ResultsAdapter(context,
				result.getResults()), R.layout.listitem_searchresult_loading);
		this.objects = result.getResults();
		this.listener = listener;
		this.maxPage = result.getPage_count();
		this.resultCount = result.getTotal_result_count();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void appendCachedData() {
		if(itemsToAppend != null) {
			for(SearchResult item:itemsToAppend) {
				((ArrayAdapter<SearchResult>) getWrappedAdapter()).add(item);
			}
			objects.addAll(itemsToAppend);
			notifyDataSetChanged();
			itemsToAppend = null;
		}
	}
	
	@Override
	protected boolean onException(View pendingView, Exception e) {
		e.printStackTrace();
		return false;
	}

	@Override
	protected boolean cacheInBackground() throws Exception {
		if(page < maxPage || getWrappedAdapter().getCount() < resultCount) {
			page++;
			itemsToAppend = listener.onLoadMore(page);
			for(SearchResult item:itemsToAppend) {
				Log.d("Opac", item.getInnerhtml());
			}
			return itemsToAppend != null;
		} else {
			return false;
		}
	}
}
