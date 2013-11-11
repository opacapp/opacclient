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

package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.Uri;
import android.os.Bundle;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class Heidi extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected Library library;
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected String sessid;
	protected String ENCODING = "UTF-8";
	protected String last_error;
	protected int pagesize = 20;
	protected Bundle lastBundle;

	@Override
	public void start() throws IOException, NotReachableException {
		String html = httpGet(opac_url + "/search.cgi?art=f", ENCODING);
		Document doc = Jsoup.parse(html);
		sessid = null;
		for (Element link : doc.select("a")) {
			Uri href = Uri.parse(link.absUrl("href"));
			String sid = href.getQueryParameter("sess");
			if (sid != null) {
				sessid = sid;
				break;
			}
		}

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			extract_meta(html);
		} else {
			metadata.close();
		}
	}

	private void extract_meta(String html) {
		Document doc = Jsoup.parse(html);

		metadata.open();
		metadata.clearMeta(library.getIdent());

		Elements zst_opts = doc.select("#teilk2 option");
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
						library.getIdent(), opt.val(), opt.text());
		}

		try {
			Document doc2 = Jsoup.parse(httpGet(opac_url
					+ "/zweigstelle.cgi?sess=" + sessid, ENCODING));
			Elements home_opts = doc2.select("#zweig option");
			for (int i = 0; i < home_opts.size(); i++) {
				Element opt = home_opts.get(i);
				if (!opt.val().equals(""))
					metadata.addMeta(MetaDataSource.META_TYPE_HOME_BRANCH,
							library.getIdent(), opt.val(), opt.text());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		metadata.close();
	}

	@Override
	protected String getDefaultEncoding() {
		return ENCODING;
	}

	@Override
	public void init(MetaDataSource metadata, Library library) {
		super.init(metadata, library);
		this.metadata = metadata;
		this.library = library;
		this.data = library.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}

	protected int addParameters(Bundle query, String key, String searchkey,
			List<NameValuePair> params, int index) {
		if (!query.containsKey(key) || query.getString(key).equals(""))
			return index;

		index++;

		if (index != 3)
			params.add(new BasicNameValuePair("op" + index, "AND"));
		params.add(new BasicNameValuePair("kat" + index, searchkey));
		params.add(new BasicNameValuePair("var" + index, query.getString(key)));
		return index;

	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException {

		lastBundle = query;

		List<NameValuePair> params = new ArrayList<NameValuePair>();

		start();
		int index = 0;

		params.add(new BasicNameValuePair("fsubmit", "1"));
		params.add(new BasicNameValuePair("sess", sessid));
		params.add(new BasicNameValuePair("art", "f"));
		params.add(new BasicNameValuePair("pagesize", String.valueOf(pagesize)));
		if (query.containsKey("_heidi_page")) {
			params.add(new BasicNameValuePair("start", String.valueOf((query
					.getInt("_heidi_page") - 1) * pagesize + 1)));
		}
		params.add(new BasicNameValuePair("vr", "1"));

		index = addParameters(query, KEY_SEARCH_QUERY_FREE, "freitext", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_TITLE, "ti", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "au", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_ISBN, "is", params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA, "sw", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR, "ej", params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, "vl", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM, "nt", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_BARCODE, "li", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_LOCATION, "714", params,
				index);

		if (query.containsKey(KEY_SEARCH_QUERY_BRANCH)
				&& "".equals(query.getString(KEY_SEARCH_QUERY_BRANCH))) {
			params.add(new BasicNameValuePair("f[tiel2]", query
					.getString(KEY_SEARCH_QUERY_BRANCH)));
		}

		if (index == 0) {
			last_error = "Es wurden keine Suchkriterien eingegeben.";
			return null;
		}
		if (index > 3) {
			last_error = "Diese Bibliothek unterstützt nur bis zu drei benutzte Suchkriterien.";
			return null;
		}

		// Homebranch auswahl
		httpGet(opac_url + "/zweigstelle.cgi?sess=" + sessid + "&zweig="
				+ query.getString(KEY_SEARCH_QUERY_HOME_BRANCH), ENCODING);
		// Die eigentliche Suche
		String html = httpGet(
				opac_url + "/search.cgi?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING);
		if (query.containsKey("_heidi_page")) {
			return parse_search(html, query.getInt("_heidi_page"));
		}
		return parse_search(html, 1);
	}

	private SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);
		List<SearchResult> results = new ArrayList<SearchResult>();
		int results_total = 0;
		// TODO
		return new SearchRequestResult(results, results_total, page);
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		// Not implemented
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		lastBundle.putInt("_heidi_page", page);
		return search(lastBundle);
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {
		// TODO: Homebranch?
		if (sessid == null)
			start();
		String html = httpGet(opac_url + "/titel.cgi?katkey=" + id + "&sess="
				+ sessid, ENCODING);
		Document doc = Jsoup.parse(html);
		// TODO
		return null;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		// Not implemented
		return null;
	}

	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cancel(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_AUTHOR, KEY_SEARCH_QUERY_FREE,
				KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_YEAR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_PUBLISHER,
				KEY_SEARCH_QUERY_BARCODE, KEY_SEARCH_QUERY_BRANCH,
				KEY_SEARCH_QUERY_HOME_BRANCH };
	}

	@Override
	public String getLast_error() {
		return last_error;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return false; // TODO
	}

	@Override
	public boolean isAccountExtendable() {
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		// Not implemented
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		return opac_url + "/titel.cgi?katkey=" + id;
	}

	@Override
	public int getSupportFlags() {
		return 0;
	}

}
