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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.objects.SearchResult.Status;
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
	protected CookieStore cookieStore = new BasicCookieStore();

	@Override
	public void start() throws IOException, NotReachableException {
		String html = httpGet(opac_url + "/search.cgi?art=f", ENCODING, false,
				cookieStore);
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);
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
					+ "/zweigstelle.cgi?sess=" + sessid, ENCODING, false,
					cookieStore));
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

		if (sessid == null)
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
				&& !"".equals(query.getString(KEY_SEARCH_QUERY_BRANCH))) {
			params.add(new BasicNameValuePair("f[teil2]", query
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
		while (index < 3) {
			index++;
			if (index != 3)
				params.add(new BasicNameValuePair("op" + index, "AND"));
			params.add(new BasicNameValuePair("kat" + index, "freitext"));
			params.add(new BasicNameValuePair("var" + index, ""));
		}

		// Homebranch auswahl
		httpGet(opac_url + "/zweigstelle.cgi?sess=" + sessid + "&zweig="
				+ query.getString(KEY_SEARCH_QUERY_HOME_BRANCH), ENCODING,
				false, cookieStore);
		// Die eigentliche Suche
		String html = httpGet(
				opac_url + "/search.cgi?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING,
				false, cookieStore);
		if (query.containsKey("_heidi_page")) {
			return parse_search(html, query.getInt("_heidi_page"));
		}
		return parse_search(html, 1);
	}

	private SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);

		int results_total = 0;
		if (doc.select("#heiditreffer").size() > 0) {
			String resstr = doc.select("#heiditreffer").text();
			String resnum = resstr.replaceAll("\\(([0-9.]+)([^0-9]*)\\)", "$1")
					.replace(".", "");
			results_total = Integer.parseInt(resnum);
		}

		Elements table = doc.select("table.treffer tr");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();

			StringBuilder description = null;
			String author = "";

			for (Element link : tr.select("a")) {
				Uri href = Uri.parse(link.absUrl("href"));
				String kk = href.getQueryParameter("katkey");
				if (kk != null) {
					sr.setId(kk);
					break;
				}
			}

			if (tr.select("span.Z3988").size() == 1) {
				// Luckily there is a <span class="Z3988"> item which provides
				// data in a standardized format.
				List<NameValuePair> z3988data;
				boolean hastitle = false;
				try {
					description = new StringBuilder();
					z3988data = URLEncodedUtils.parse(new URI("http://dummy/?"
							+ tr.select("span.Z3988").attr("title")), "UTF-8");
					for (NameValuePair nv : z3988data) {
						if (nv.getValue() != null) {
							if (!nv.getValue().trim().equals("")) {
								if (nv.getName().equals("rft.btitle")
										&& !hastitle) {
									description.append("<b>" + nv.getValue()
											+ "</b>");
									hastitle = true;
								} else if (nv.getName().equals("rft.atitle")
										&& !hastitle) {
									description.append("<b>" + nv.getValue()
											+ "</b>");
									hastitle = true;
								} else if (nv.getName().equals("rft.au")) {
									author = nv.getValue();
								} else if (nv.getName().equals("rft.aufirst")) {
									author = author + ", " + nv.getValue();
								} else if (nv.getName().equals("rft.aulast")) {
									author = nv.getValue();
								} else if (nv.getName().equals("rft.date")) {
									description
											.append("<br />" + nv.getValue());
								}
							}
						}
					}
				} catch (URISyntaxException e) {
					description = null;
				}
			}
			if (!"".equals(author))
				author = author + "<br />";
			sr.setInnerhtml(author + description.toString());

			if (tr.select(".kurzstat").size() > 0) {
				String stattext = tr.select(".kurzstat").first().text();
				if (stattext.contains("ausleihbar"))
					sr.setStatus(Status.GREEN);
				else if (stattext.contains("online"))
					sr.setStatus(Status.GREEN);
				else if (stattext.contains("entliehen"))
					sr.setStatus(Status.RED);
				else if (stattext.contains("Präsenznutzung"))
					sr.setStatus(Status.YELLOW);
				else if (stattext.contains("bestellen"))
					sr.setStatus(Status.YELLOW);
			}
			if (tr.select(".typbild").size() > 0) {
				String typtext = tr.select(".typbild").first().text();
				if (typtext.contains("Buch"))
					sr.setType(MediaType.BOOK);
				else if (typtext.contains("DVD-ROM"))
					sr.setType(MediaType.CD_SOFTWARE);
				else if (typtext.contains("Online-Ressource"))
					sr.setType(MediaType.EDOC);
				else if (typtext.contains("DVD"))
					sr.setType(MediaType.DVD);
				else if (typtext.contains("Film"))
					sr.setType(MediaType.MOVIE);
				else if (typtext.contains("Zeitschrift"))
					sr.setType(MediaType.MAGAZINE);
				else if (typtext.contains("Musiknoten"))
					sr.setType(MediaType.SCORE_MUSIC);
				else if (typtext.contains("Bildliche Darstellung"))
					sr.setType(MediaType.ART);
				else if (typtext.contains("Zeitung"))
					sr.setType(MediaType.NEWSPAPER);
				else if (typtext.contains("Karte"))
					sr.setType(MediaType.MAP);
				else if (typtext.contains("Mehrteilig"))
					sr.setType(MediaType.PACKAGE_BOOKS);
			}

			results.add(sr);
		}
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
	public DetailledItem getResultById(String id, final String homebranch)
			throws IOException, NotReachableException {

		if (sessid == null)
			start();

		// Homebranch
		if (homebranch != null && !"".equals(homebranch))
			cookieStore.addCookie(new BasicClientCookie("zweig", homebranch));

		String html = httpGet(opac_url + "/titel.cgi?katkey=" + id + "&sess="
				+ sessid, ENCODING, false, cookieStore);
		Document doc = Jsoup.parse(html);

		DetailledItem item = new DetailledItem();
		item.setId(id);

		Elements table = doc.select(".titelsatz tr");
		for (Element tr : table) {
			if(tr.select("th").size() == 0 || tr.select("td").size() == 0)
				continue;
			String d = tr.select("th").first().text();
			String c = tr.select("td").first().text();
			if (d.equals("Titel:")) {
				item.setTitle(c);
			} else if (d.contains("URL") || d.contains("Link")) {
				item.addDetail(new Detail(d, tr.select("td").first().html(),
						true));
			} else {
				item.addDetail(new Detail(d, c));
			}
		}

		if (doc.select(".ex table tr").size() > 0) {
			table = doc.select(".ex table tr");
			for (Element tr : table) {
				if (tr.hasClass("exueber") || tr.select(".exsig").size() == 0
						|| tr.select(".exso").size() == 0
						|| tr.select(".exstatus").size() == 0)
					continue;
				ContentValues e = new ContentValues();
				e.put(DetailledItem.KEY_COPY_SHELFMARK, tr.select(".exsig")
						.first().text());
				e.put(DetailledItem.KEY_COPY_BRANCH, tr.select(".exso").first()
						.text());
				String status = tr.select(".exstatus").first().text();
				if (status.contains("entliehen bis")) {
					e.put(DetailledItem.KEY_COPY_RETURN, status.replaceAll(
							"entliehen bis ([0-9.]+) .*", "$1"));
					e.put(DetailledItem.KEY_COPY_RESERVATIONS, status
							.replaceAll(".*\\(.*Vormerkungen: ([0-9]+)\\)",
									"$1"));
					e.put(DetailledItem.KEY_COPY_STATUS, "entliehen");
				} else {
					e.put(DetailledItem.KEY_COPY_STATUS, status);
				}
				item.addCopy(e);
			}
		}

		return item;
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
