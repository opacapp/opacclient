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

import android.content.Context;
import android.view.View;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;

public class ResultsAdapterEndless extends EndlessAdapter {
    private List<SearchResult> objects;
    private OnLoadMoreListener listener;
    private int page = 1;
    private int maxPage;
    private boolean endReached = false;
    private int resultCount;
    private List<SearchResult> itemsToAppend;

    public ResultsAdapterEndless(Context context, SearchRequestResult result,
            OnLoadMoreListener listener, OpacApi api) {
        super(context, new ResultsAdapter(context,
                result.getResults(), api), R.layout.listitem_searchresult_loading);
        this.objects = result.getResults();
        this.listener = listener;
        this.maxPage = result.getPage_count();
        this.resultCount = result.getTotal_result_count();
    }

    @Override
    protected void appendCachedData() {
        listener.updateResultCount(resultCount);
        if (itemsToAppend != null) {
            objects.addAll(itemsToAppend);
            notifyDataSetChanged();
            if (itemsToAppend.size() == 0) {
                // We received zero results, so we suspect that we reached past the last
                // page. This is needed for OPACs where the number of total search results is
                // unknown (e.g. some VuFind installations with unusual templates).
                endReached = true;
            }
            itemsToAppend = null;
        }
    }

    @Override
    protected boolean onException(View pendingView, Exception e) {
        listener.onError(e);
        return false;
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        if (page < maxPage || getWrappedAdapter().getCount() < resultCount || (resultCount == -1 && !endReached)) {
            page++;
            SearchRequestResult result = listener.onLoadMore(page);
            itemsToAppend = result.getResults();

			/* When IOpac finds more than 200 results, the real result count is
            not known until the second page is loaded */
            maxPage = result.getPage_count();
            resultCount = result.getTotal_result_count();

            for (SearchResult item : itemsToAppend) {
                item.setPage(page);
            }
            return itemsToAppend != null;
        } else {
            endReached = true;
            return false;
        }
    }

    public int getPage() {
        return page;
    }

    public interface OnLoadMoreListener {
        public SearchRequestResult onLoadMore(int page) throws Exception;

        public void updateResultCount(int resultCount);

        public void onError(Exception e);
    }
}
