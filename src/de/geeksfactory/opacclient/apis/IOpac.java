/**
 * Copyright (C) 2013 by Johan von Forstner under the MIT license:
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
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
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * @author Johan von Forstner, 17.09.2013
 * */

// Implementation of Fleischmann iOpac, currently only works with Stadtbücherei
// Schleswig, including account support

public class IOpac extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected String last_error;
	protected Library library;
	protected int resultcount = 10;
	protected String reusehtml;
	protected String rechnr;
	protected int results_total;
	CookieStore cookieStore = new BasicCookieStore();

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("b", MediaType.BOOK);
		defaulttypes.put("o", MediaType.BOOK);
		defaulttypes.put("e", MediaType.BOOK);
		defaulttypes.put("p", MediaType.BOOK);
		defaulttypes.put("j", MediaType.BOOK);
		defaulttypes.put("g", MediaType.BOOK);
		defaulttypes.put("k", MediaType.BOOK);
		defaulttypes.put("a", MediaType.BOOK);
		defaulttypes.put("c", MediaType.AUDIOBOOK);
		defaulttypes.put("u", MediaType.AUDIOBOOK);
		defaulttypes.put("l", MediaType.AUDIOBOOK);
		defaulttypes.put("q", MediaType.CD_SOFTWARE);
		defaulttypes.put("r", MediaType.CD_SOFTWARE);
		defaulttypes.put("v", MediaType.MOVIE);
		defaulttypes.put("d", MediaType.CD_MUSIC);
		defaulttypes.put("n", MediaType.SCORE_MUSIC);
		defaulttypes.put("s", MediaType.BOARDGAME);
		defaulttypes.put("z", MediaType.MAGAZINE);
		defaulttypes.put("x", MediaType.MAGAZINE);
	}

	public void extract_meta() {
		// Extract available media types.
		// We have to parse JavaScript. Doing this with RegEx is evil.
		// But not as evil as including a JavaScript VM into the app.
		// And I honestly do not see another way.
		String html;

		Pattern pattern_key = Pattern
				.compile("mtyp\\[[0-9]+\\]\\[\"typ\"\\] = \"([^\"]+)\";");
		Pattern pattern_value = Pattern
				.compile("mtyp\\[[0-9]+\\]\\[\"bez\"\\] = \"([^\"]+)\";");

		try {
			html = httpGet(opac_url + "/iopac/mtyp.js");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		metadata.open();
		metadata.clearMeta(library.getIdent());
		String[] parts = html.split("new Array\\(\\);");
		for (String part : parts) {
			Matcher matcher1 = pattern_key.matcher(part);
			String key = "";
			String value = "";
			if (matcher1.find()) {
				key = matcher1.group(1);
			}
			Matcher matcher2 = pattern_value.matcher(part);
			if (matcher2.find()) {
				value = matcher2.group(1);
			}
			if (value != "")
				metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
						library.getIdent(), key, value);
		}

		metadata.close();
	}

	@Override
	public void start() throws IOException, NotReachableException {

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			extract_meta();
		} else {
			metadata.close();
		}
	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		super.init(metadata, lib);

		this.metadata = metadata;
		this.library = lib;
		this.data = lib.getData();

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

		params.add(new BasicNameValuePair(searchkey, query.getString(key)));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		int index = 0;
		start();

		index = addParameters(query, KEY_SEARCH_QUERY_FREE, "sleStichwort",
				params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA,
				"sleSchlagwort", params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "sleAutor",
				params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_TITLE, "sleTitel",
				params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, "sleVerlag",
				params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM, "sleDeweyNr",
				params, index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR, "sleJahr", params,
				index);

		if (query.containsKey(KEY_SEARCH_QUERY_CATEGORY)
				&& !"".equals(query.getString(KEY_SEARCH_QUERY_CATEGORY))) {
			params.add(new BasicNameValuePair("Medientyp", query
					.getString(KEY_SEARCH_QUERY_CATEGORY)));
		} else {
			params.add(new BasicNameValuePair("Medientyp",
					"A, B, C, D, E, G, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Z, 9"));
		}

		params.add(new BasicNameValuePair("Anzahl", "10"));
		params.add(new BasicNameValuePair("pshStart", "Suchen"));

		if (index == 0) {
			last_error = "Es wurden keine Suchkriterien eingegeben.";
			return null;
		}

		String html = httpPost(opac_url + "/cgi-bin/di.exe",
				new UrlEncodedFormEntity(params, "iso-8859-1"));

		return parse_search(html, 1);
	}

	protected SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);

		updateRechnr(doc);

		if (doc.select("h4").size() > 0) {
			if (doc.select("h4").text().trim().contains("0 gefundene Medien")) {
				// nothing found
				return new SearchRequestResult(new ArrayList<SearchResult>(),
						0, 1, 1);
			} else if (!doc.select("h4").text().trim()
					.contains("gefundene Medien")
					&& !doc.select("h4").text().trim()
							.contains("Es wurden mehr als")) {
				// error
				last_error = doc.select("h4").text().trim();
				return null;
			}
		}

		reusehtml = html;

		results_total = -1;

		if (doc.select("h4").text().trim().contains("Es wurden mehr als")) {
			results_total = 200;
		} else {
			String resultnumstr = doc.select("h4").first().text();
			resultnumstr = resultnumstr.substring(0, resultnumstr.indexOf(" "))
					.trim();
			results_total = Integer.parseInt(resultnumstr);
		}

		List<SearchResult> results = new ArrayList<SearchResult>();

		Elements tables = doc.select("table").first().select("tr:has(td)");

		for (int i = 0; i < tables.size(); i++) {
			Element tr = tables.get(i);
			SearchResult sr = new SearchResult();

			// This library system can show cover images on the search results
			// page. If the API would
			// support it, cover URLs could be saved like this:
			//
			// if(tr.select("td").first().select("img").size() > 0) {
			// String imgUrl =
			// tr.select("td").first().select("img").first().attr("src");
			// sr.setCoverUrl(imgUrl);
			// }

			// Media Type
			String mType = tr.select("td").last().text().trim()
					.replace("\u00a0", "");

			if (data.has("mediatypes")) {
				try {
					sr.setType(MediaType.valueOf(data.getJSONObject(
							"mediatypes").getString(mType.toLowerCase())));
				} catch (JSONException e) {
					sr.setType(defaulttypes.get(mType.toLowerCase()));
				} catch (IllegalArgumentException e) {
					sr.setType(defaulttypes.get(mType.toLowerCase()));
				}
			} else {
				sr.setType(defaulttypes.get(mType.toLowerCase()));
			}

			// Title and additional info
			String title = tr.select("td").get(1).text().trim()
					.replace("\u00a0", "");
			String additionalInfo = "";
			if (title.contains("(")) {
				additionalInfo += title.substring(title.indexOf("("));
				title = title.substring(0, title.indexOf("(") - 1).trim();
			}

			// Author
			String author = tr.select("td").get(2).text().trim()
					.replace("\u00a0", "");
			additionalInfo += " - " + author;

			// Publisher
			String publisher = tr.select("td").get(3).text().trim()
					.replace("\u00a0", "");
			additionalInfo += " (" + publisher;

			// Year
			String year = tr.select("td").get(4).text().trim()
					.replace("\u00a0", "");
			additionalInfo += ", " + year + ")";

			sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);

			sr.setNr(10 * (page - 1) + i);
			sr.setId(null);
			results.add(sr);
		}
		resultcount = results.size();
		return new SearchRequestResult(results, results_total, page);
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
				+ "&rechnr=" + rechnr + "&Anzahl=10&FilNr=");
		return parse_search(html, page);
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		return null;
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {

		if (id == null && reusehtml != null) {
			return parse_result(reusehtml);
		}

		String html = httpGet(opac_url + "/cgi-bin/di.exe?cMedNr=" + id
				+ "&mode=23");

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		int page = Double.valueOf(Math.floor(position / 10)).intValue() + 1;

		String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
				+ "&rechnr=" + rechnr + "&Anzahl=10&recno=" + (position + 1)
				+ "&FilNr=");

		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();

		String id = doc.select("table").last().select("td a").first()
				.attr("href");
		Integer idPosition = id.indexOf("mednr=") + 6;
		id = id.substring(idPosition, id.indexOf("&", idPosition)).trim();

		result.setId(id);

		Elements table = doc.select("table").get(1).select("tr");

		// GET COVER IMAGE
		String imgUrl = table.get(0)
				.select("img[src^=http://images-eu.amazon.com]").attr("src")
				.replace("TZZZZZZZ", "L");
		result.setCover(imgUrl);

		// GET INFORMATION
		ContentValues e = new ContentValues();

		for (Element element : table) {
			String detail = element.select("td").text().trim()
					.replace("\u00a0", "");
			String title = element.select("th").text().trim()
					.replace("\u00a0", "");

			if (!title.equals("")) {

				if (title.contains("verliehen bis")) {
					if (detail.equals("")) {
						e.put(DetailledItem.KEY_COPY_STATUS, "verfügbar");
					} else {
						e.put(DetailledItem.KEY_COPY_STATUS, "verliehen bis "
								+ detail);
					}
				} else if (title.contains("Abteilung")) {
					e.put(DetailledItem.KEY_COPY_DEPARTMENT, detail);
				} else if (title.contains("Signatur")) {
					e.put(DetailledItem.KEY_COPY_SHELFMARK, detail);
				} else if (title.contains("Titel")) {
					result.setTitle(detail);
				} else if (!title.contains("Cover")) {
					result.addDetail(new Detail(title, detail));
				}
			}
		}

		// GET RESERVATION INFO
		if (e.getAsString(DetailledItem.KEY_COPY_STATUS).equals("verfügbar")) {
			result.setReservable(false);
		} else {
			result.setReservable(true);
			result.setReservation_info(doc
					.select("a[href^=/cgi-bin/di.exe?mode=10]").first()
					.attr("href").substring(1).replace(" ", ""));
		}

		result.addCopy(e);

		return result;
	}

	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		String html = httpGet(opac_url + "/" + reservation_info);
		Document doc = Jsoup.parse(html);
		if (doc.select("form[name=form1]").size() > 0) {
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
			params.add(new BasicNameValuePair("sleKndNr", account.getName()));
			params.add(new BasicNameValuePair("slePw", account.getPassword()));
			params.add(new BasicNameValuePair("mode", "11"));
			params.add(new BasicNameValuePair("mednr", doc.select(
					"input[name=mednr]").attr("value")));
			params.add(new BasicNameValuePair("page", doc.select(
					"input[name=page]").attr("value")));
			params.add(new BasicNameValuePair("Anzahl", "10"));
			params.add(new BasicNameValuePair("recno", doc.select(
					"input[name=recno]").attr("value")));
			params.add(new BasicNameValuePair("pshLogin", "Reservieren"));

			httpPost(opac_url + "/cgi-bin/di.exe", new UrlEncodedFormEntity(
					params));
			return new ReservationResult(MultiStepResult.Status.OK);
		}
		return new ReservationResult(MultiStepResult.Status.ERROR);
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String Selection) throws IOException {
		String html = httpGet(opac_url + "/" + media);
		Document doc = Jsoup.parse(html);
		if (doc.select("table th").size() > 0) {
			if (doc.select("table th").first().text()
					.contains("da die maximale Anzahl.")) {
				// Comparing to the full string would be cleaner, but because I
				// can't test it I only want the part without äöü in there.
				return new ProlongResult(MultiStepResult.Status.ERROR, doc.select("table th").first().text());
			} else {
				try {
					Element form = doc.select("form[name=form1]").first();
					String sessionid = form.select("input[name=sessionid]")
							.attr("value");
					String mednr = form.select("input[name=mednr]").attr(
							"value");
					httpGet(opac_url + "/cgi-bin/di.exe?mode=8&kndnr="
							+ account.getName() + "&mednr=" + mednr
							+ "&sessionid=" + sessionid
							+ "&psh100=Verl%C3%A4ngern");
					return new ProlongResult(MultiStepResult.Status.OK);
				} catch (Throwable e) {
					e.printStackTrace();
					return new ProlongResult(MultiStepResult.Status.ERROR);
				}
			}
		}
		return new ProlongResult(MultiStepResult.Status.ERROR);
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		return false;
	}

	@Override
	public boolean cancel(Account account, String media) throws IOException {
		String html = httpGet(opac_url + "/" + media);
		Document doc = Jsoup.parse(html);
		try {
			Element form = doc.select("form[name=form1]").first();
			String sessionid = form.select("input[name=sessionid]").attr(
					"value");
			String mednr = form.select("input[name=mednr]").attr("value");
			httpGet(opac_url + "/cgi-bin/di.exe?mode=9&kndnr="
					+ account.getName() + "&mednr=" + mednr + "&sessionid="
					+ sessionid + "&psh100=Stornieren");
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sleKndNr", account.getName()));
		params.add(new BasicNameValuePair("slePw", account.getPassword()));
		params.add(new BasicNameValuePair("pshLogin", "Login"));

		String html = httpPost(opac_url + "/cgi-bin/di.exe",
				new UrlEncodedFormEntity(params, "iso-8859-1"));
		Document doc = Jsoup.parse(html);

		List<ContentValues> medien = new ArrayList<ContentValues>();
		parse_medialist(medien, doc, 1);

		List<ContentValues> reserved = new ArrayList<ContentValues>();
		parse_reslist(reserved, doc, 1);

		AccountData res = new AccountData(account.getId());
		res.setLent(medien);
		res.setReservations(reserved);

		return res;
	}

	protected void parse_medialist(List<ContentValues> medien, Document doc,
			int offset) throws ClientProtocolException, IOException {
		Elements copytrs = doc.select("a[name=AUS] ~ table").first()
				.select("tr");
		doc.setBaseUri(opac_url);

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

		int trs = copytrs.size();
		if (trs < 2)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			ContentValues e = new ContentValues();

			e.put(AccountData.KEY_LENT_TITLE, tr.child(0).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_LENT_AUTHOR, tr.child(1).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_LENT_STATUS, tr.child(3).text().trim()
					.replace("\u00a0", "")
					+ "x verl.");
			e.put(AccountData.KEY_LENT_DEADLINE, tr.child(4).text().trim()
					.replace("\u00a0", ""));
			try {
				e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP,
						sdf.parse(e.getAsString(AccountData.KEY_LENT_DEADLINE))
								.getTime());
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			e.put(AccountData.KEY_LENT_LINK,
					tr.child(5).select("a").attr("href"));

			medien.add(e);
		}
		assert (medien.size() == trs - 1);

	}

	protected void parse_reslist(List<ContentValues> medien, Document doc,
			int offset) throws ClientProtocolException, IOException {
		Elements copytrs = doc.select("a[name=RES] ~ table tr");
		doc.setBaseUri(opac_url);

		int trs = copytrs.size();
		if (trs < 2)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			ContentValues e = new ContentValues();

			e.put(AccountData.KEY_RESERVATION_TITLE, tr.child(0).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_RESERVATION_AUTHOR, tr.child(1).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_RESERVATION_READY, tr.child(4).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_RESERVATION_CANCEL, tr.child(5).select("a")
					.attr("href"));

			medien.add(e);
		}
		assert (medien.size() == trs - 1);

	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_TITLE,
				KEY_SEARCH_QUERY_YEAR, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_PUBLISHER, KEY_SEARCH_QUERY_CATEGORY };
	}

	@Override
	public String getLast_error() {
		return null;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return true;
	}

	@Override
	public boolean isAccountExtendable() {
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		return opac_url + "/cgi-bin/di.exe?cMedNr=" + id + "&mode=23";
	}

	@Override
	public int getSupportFlags() {
		return 0;

	}

	public void updateRechnr(Document doc) {
		String url = doc.select("table th a").first().attr("href");
		Integer rechnrPosition = url.indexOf("rechnr=") + 7;
		rechnr = url
				.substring(rechnrPosition, url.indexOf("&", rechnrPosition));
	}

}
