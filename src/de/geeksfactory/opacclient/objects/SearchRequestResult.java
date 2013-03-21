package de.geeksfactory.opacclient.objects;

import java.util.List;

import android.os.Bundle;

/**
 * Object representing a search result
 * 
 * @since 2.0.6
 * 
 * @author Raphael Michel
 */
public class SearchRequestResult {
	private List<SearchResult> results;
	private List<Filter> filters;
	private int total_result_count;
	private int page_count;
	private int page_index;

	/**
	 * @return The total number of results for the search request.
	 */
	public int getTotal_result_count() {
		return total_result_count;
	}

	/**
	 * @param total_result_count
	 *            The total number of results for the search request.
	 */
	public void setTotal_result_count(int total_result_count) {
		this.total_result_count = total_result_count;
	}

	/**
	 * @return The total number of result pages for the search request.
	 */
	public int getPage_count() {
		return page_count;
	}

	/**
	 * @param page_count
	 *            The total number of result pages for the search request.
	 */
	public void setPage_count(int page_count) {
		this.page_count = page_count;
	}

	/**
	 * @return The index of the result page requested.
	 */
	public int getPage_index() {
		return page_index;
	}

	/**
	 * @param page_index
	 *            The index of the result page requested.
	 */
	public void setPage_number(int page_index) {
		this.page_index = page_index;
	}

	public List<SearchResult> getResults() {
		return results;
	}

	/**
	 * @param results
	 *            The results found for this search.
	 */
	public void setResults(List<SearchResult> results) {
		this.results = results;
	}

	/**
	 * Some libraries support a "refined search": On the search results page you
	 * have a list of options to refine your search, for example "Media type"
	 * with the options "book", "cd" or "movie" or "Language" with "German" or
	 * "English". If a library does support this, this returns the filters
	 * available for the search results last displayed by
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#search(Bundle)}. If a
	 * library does not, this is <code>null</code> .
	 * 
	 * @return List of filters available
	 * @see de.geeksfactory.opacclient.objects.Filter
	 * @since 2.0.6
	 */
	public List<Filter> getFilters() {
		return filters;
	}

	/**
	 * Some libraries support a "refined search": On the search results page you
	 * have a list of options to refine your search, for example "Media type"
	 * with the options "book", "cd" or "movie" or "Language" with "German" or
	 * "English". If your library does support this, this must return the
	 * filters available for the search results last displayed by
	 * {@link de.geeksfactory.opacclient.apis.OpacApi#search(Bundle)}. If your
	 * library does not, just set <code>null</code>.
	 * 
	 * @param filters
	 *            List of filters available
	 * @see de.geeksfactory.opacclient.objects.Filter
	 * @since 2.0.6
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * @param results
	 *            The results found for this search.
	 * @param total_result_count
	 *            The total number of results for the search request.
	 * @param page_count
	 *            The total number of result pages for the search request.
	 * @param page_index
	 *            The index of the result page requested.
	 */
	public SearchRequestResult(List<SearchResult> results,
			int total_result_count, int page_count, int page_index) {
		super();
		this.results = results;
		this.total_result_count = total_result_count;
		this.page_count = page_count;
		this.page_index = page_index;
	}

	/**
	 * @param results
	 *            The results found for this search.
	 * @param total_result_count
	 *            The total number of results for the search request.
	 * @param page_index
	 *            The index of the result page requested.
	 */
	public SearchRequestResult(List<SearchResult> results,
			int total_result_count, int page_index) {
		super();
		this.results = results;
		this.total_result_count = total_result_count;
		this.page_index = page_index;
	}

}
