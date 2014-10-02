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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * Implementation of Fleischmann iOpac, including account support Seems to work
 * in all the libraries currently supported without any modifications.
 * 
 * @author Johan von Forstner, 17.09.2013
 * */

public class IOpac extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected String dir = "/iopac";
	protected JSONObject data;
	protected boolean initialised = false;
	protected Library library;
	protected int resultcount = 10;
	protected String reusehtml;
	protected String rechnr;
	protected int results_total;
	protected int maxProlongCount = -1;
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

	@Override
	public void start() throws IOException, NotReachableException {

	}

	@Override
	public void init(Library lib) {
		super.init(lib);

		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
			if (data.has("maxprolongcount"))
				this.maxProlongCount = data.getInt("maxprolongcount");
			if (data.has("dir"))
				this.dir = data.getString("dir");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected int addParameters(SearchQuery query, List<NameValuePair> params,
			int index) {
		if (query.getValue().equals(""))
			return index;

		params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(List<SearchQuery> queries)
			throws IOException, NotReachableException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		int index = 0;
		start();

		for (SearchQuery query : queries) {
			index = addParameters(query, params, index);
		}

		params.add(new BasicNameValuePair("Anzahl", "10"));
		params.add(new BasicNameValuePair("pshStart", "Suchen"));

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}

		String html = httpPost(opac_url + "/cgi-bin/di.exe",
				new UrlEncodedFormEntity(params, "iso-8859-1"),
				getDefaultEncoding());

		return parse_search(html, 1);
	}

	protected SearchRequestResult parse_search(String html, int page)
			throws OpacErrorException {
		Document doc = Jsoup.parse(html);

		if (doc.select("h4").size() > 0) {
			if (doc.select("h4").text().trim().startsWith("0 gefundene Medien")) {
				// nothing found
				return new SearchRequestResult(new ArrayList<SearchResult>(),
						0, 1, 1);
			} else if (!doc.select("h4").text().trim()
					.contains("gefundene Medien")
					&& !doc.select("h4").text().trim()
							.contains("Es wurden mehr als")) {
				// error
				throw new OpacErrorException(doc.select("h4").text().trim());
			}
		} else if (doc.select("h1").size() > 0) {
			if (doc.select("h1").text().trim().contains("RUNTIME ERROR")) {
				// Server Error
				throw new OpacErrorException(
						"Serverfehler. Bitte probieren Sie es später noch einmal.");
			} else {
				throw new OpacErrorException("Unbekannter Fehler: "
						+ doc.select("h1").text().trim());
			}
		} else {
			return null;
		}

		updateRechnr(doc);

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

		Map<String, Integer> colmap = new HashMap<String, Integer>();
		Element thead = doc.select("table").first().select("tr:has(th)")
				.first();
		int j = 0;
		for (Element th : thead.select("th")) {
			String text = th.text().trim().toLowerCase(Locale.GERMAN);
			if (text.contains("cover"))
				colmap.put("cover", j);
			else if (text.contains("titel"))
				colmap.put("title", j);
			else if (text.contains("verfasser"))
				colmap.put("author", j);
			else if (text.contains("mtyp"))
				colmap.put("category", j);
			else if (text.contains("jahr"))
				colmap.put("year", j);
			else if (text.contains("signatur"))
				colmap.put("shelfmark", j);
			else if (text.contains("info"))
				colmap.put("info", j);
			else if (text.contains("abteilung"))
				colmap.put("department", j);
			else if (text.contains("verliehen") || text.contains("verl."))
				colmap.put("returndate", j);
			else if (text.contains("anz.res"))
				colmap.put("reservations", j);
			j++;
		}
		if (colmap.size() == 0) {
			colmap.put("cover", 0);
			colmap.put("title", 1);
			colmap.put("author", 2);
			colmap.put("publisher", 3);
			colmap.put("year", 4);
			colmap.put("department", 5);
			colmap.put("shelfmark", 6);
			colmap.put("returndate", 7);
			colmap.put("category", 8);
		}

		for (int i = 0; i < tables.size(); i++) {
			Element tr = tables.get(i);
			SearchResult sr = new SearchResult();

			if (tr.select("td").get(colmap.get("cover")).select("img").size() > 0) {
				String imgUrl = tr.select("td").get(colmap.get("cover"))
						.select("img").first().attr("src");
				sr.setCover(imgUrl);
			}

			// Media Type
			if (colmap.get("category") != null) {
				String mType = tr.select("td").get(colmap.get("category"))
						.text().trim().replace("\u00a0", "");
				if (data.has("mediatypes")) {
					try {
						sr.setType(MediaType.valueOf(data.getJSONObject(
								"mediatypes").getString(
								mType.toLowerCase(Locale.GERMAN))));
					} catch (JSONException e) {
						sr.setType(defaulttypes.get(mType
								.toLowerCase(Locale.GERMAN)));
					} catch (IllegalArgumentException e) {
						sr.setType(defaulttypes.get(mType
								.toLowerCase(Locale.GERMAN)));
					}
				} else {
					sr.setType(defaulttypes.get(mType
							.toLowerCase(Locale.GERMAN)));
				}
			}

			// Title and additional info
			String title = "";
			String additionalInfo = "";
			if (colmap.get("info") != null) {
				Element info = tr.select("td").get(colmap.get("info"));
				title = info.select("a[title=Details-Info]").text().trim();
				String authorIn = info.text().substring(0,
						info.text().indexOf(title));
				if (authorIn.contains(":")) {
					authorIn = authorIn.replaceFirst("^([^:]*):(.*)$", "$1");
					additionalInfo += " - " + authorIn;
				}
			} else {
				title = tr.select("td").get(colmap.get("title")).text().trim()
						.replace("\u00a0", "");
				if (title.contains("(")) {
					additionalInfo += title.substring(title.indexOf("("));
					title = title.substring(0, title.indexOf("(") - 1).trim();
				}

				// Author
				if (colmap.containsKey("author")) {
					String author = tr.select("td").get(colmap.get("author"))
							.text().trim().replace("\u00a0", "");
					additionalInfo += " - " + author;
				}
			}

			// Publisher
			if (colmap.containsKey("publisher")) {
				String publisher = tr.select("td").get(colmap.get("publisher"))
						.text().trim().replace("\u00a0", "");
				additionalInfo += " (" + publisher;
			}

			// Year
			if (colmap.containsKey("year")) {
				String year = tr.select("td").get(colmap.get("year")).text()
						.trim().replace("\u00a0", "");
				additionalInfo += ", " + year + ")";
			}

			sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);

			// Status
			String status = tr.select("td").get(colmap.get("returndate"))
					.text().trim().replace("\u00a0", "");
			if (status.equals("") || status.contains("Onleihe")
					|| status.contains("verleihbar")
					|| status.contains("entleihbar")) {
				sr.setStatus(Status.GREEN);
			} else {
				sr.setStatus(Status.RED);
				sr.setInnerhtml(sr.getInnerhtml() + "<br><i>verliehen bis "
						+ status + "</i>");
			}

			sr.setNr(10 * (page - 1) + i);
			sr.setId(null);

			results.add(sr);
		}
		resultcount = results.size();
		return new SearchRequestResult(results, results_total, page);
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		if (!initialised)
			start();

		String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
				+ "&rechnr=" + rechnr + "&Anzahl=10&FilNr=",
				getDefaultEncoding());
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
				+ "&mode=23", getDefaultEncoding());

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		int page = Double.valueOf(Math.floor(position / 10)).intValue() + 1;

		String html = httpGet(opac_url + "/cgi-bin/di.exe?page=" + page
				+ "&rechnr=" + rechnr + "&Anzahl=10&recno=" + (position + 1)
				+ "&FilNr=", getDefaultEncoding());

		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();

		String id = null;

		if (doc.select("input[name=mednr]").size() > 0)
			id = doc.select("input[name=mednr]").first().val().trim();
		else {
			for (Element a : doc.select("table").last().select("td a")) {
				if (a.attr("href").contains("mednr=")) {
					id = a.attr("href");
					break;
				}
			}
			Integer idPosition = id.indexOf("mednr=") + 6;
			id = id.substring(idPosition, id.indexOf("&", idPosition)).trim();
		}

		result.setId(id);

		Elements table = doc.select("table").get(1).select("tr");

		// GET COVER IMAGE
		String imgUrl = table.get(0)
				.select("img[src^=http://images-eu.amazon.com]").attr("src")
				.replace("TZZZZZZZ", "L");
		result.setCover(imgUrl);

		// GET INFORMATION
		Map<String, String> e = new HashMap<String, String>();

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
		if ("verfügbar".equals(e.get(DetailledItem.KEY_COPY_STATUS))
				|| doc.select("a[href^=/cgi-bin/di.exe?mode=10]").size() == 0) {
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
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		String reservation_info = item.getReservation_info();
		String html = httpGet(opac_url + "/" + reservation_info,
				getDefaultEncoding());
		Document doc = Jsoup.parse(html);
		if (doc.select("table").first().text().contains("kann nicht")) {
			return new ReservationResult(MultiStepResult.Status.ERROR, doc
					.select("table").first().text().trim());
		}
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

			html = httpPost(opac_url + "/cgi-bin/di.exe",
					new UrlEncodedFormEntity(params), getDefaultEncoding());
			doc = Jsoup.parse(html);
			if (doc.select("h1").text().contains("fehlgeschlagen")) {
				return new ReservationResult(MultiStepResult.Status.ERROR, doc
						.select("table").first().text().trim());
			}
			return new ReservationResult(MultiStepResult.Status.OK);
		}
		return new ReservationResult(MultiStepResult.Status.ERROR);
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String Selection) throws IOException {
		String html = httpGet(opac_url + "/" + media, getDefaultEncoding());
		Document doc = Jsoup.parse(html);
		if (doc.select("table th").size() > 0) {
			if (doc.select("h1").size() > 0) {
				if (doc.select("h1").first().text().contains("Hinweis")) {
					return new ProlongResult(MultiStepResult.Status.ERROR, doc
							.select("table th").first().text());
				}
			}
			try {
				Element form = doc.select("form[name=form1]").first();
				String sessionid = form.select("input[name=sessionid]").attr(
						"value");
				String mednr = form.select("input[name=mednr]").attr("value");
				httpGet(opac_url + "/cgi-bin/di.exe?mode=8&kndnr="
						+ account.getName() + "&mednr=" + mednr + "&sessionid="
						+ sessionid + "&psh100=Verl%C3%A4ngern",
						getDefaultEncoding());
				return new ProlongResult(MultiStepResult.Status.OK);
			} catch (Throwable e) {
				e.printStackTrace();
				return new ProlongResult(MultiStepResult.Status.ERROR);
			}
		}
		return new ProlongResult(MultiStepResult.Status.ERROR);
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		String html = httpGet(opac_url + "/" + media, getDefaultEncoding());
		Document doc = Jsoup.parse(html);
		try {
			Element form = doc.select("form[name=form1]").first();
			String sessionid = form.select("input[name=sessionid]").attr(
					"value");
			String mednr = form.select("input[name=mednr]").attr("value");
			httpGet(opac_url + "/cgi-bin/di.exe?mode=9&kndnr="
					+ account.getName() + "&mednr=" + mednr + "&sessionid="
					+ sessionid + "&psh100=Stornieren", getDefaultEncoding());
			return new CancelResult(MultiStepResult.Status.OK);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new OpacErrorException("Verbindungsfehler.");
		}
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sleKndNr", account.getName()));
		params.add(new BasicNameValuePair("slePw", account.getPassword()));
		params.add(new BasicNameValuePair("pshLogin", "Login"));

		String html = httpPost(opac_url + "/cgi-bin/di.exe",
				new UrlEncodedFormEntity(params, "iso-8859-1"),
				getDefaultEncoding());
		Document doc = Jsoup.parse(html);

		AccountData res = new AccountData(account.getId());

		List<Map<String, String>> medien = new ArrayList<Map<String, String>>();
		List<Map<String, String>> reserved = new ArrayList<Map<String, String>>();
		if (doc.select("a[name=AUS]").size() > 0) {
			parse_medialist(medien, doc, 1);
		}
		if (doc.select("a[name=RES]").size() > 0) {
			parse_reslist(reserved, doc, 1);
		}

		res.setLent(medien);
		res.setReservations(reserved);

		if (medien.isEmpty() && reserved.isEmpty()) {
			if (doc.select("h1").size() > 0) {
				if (doc.select("h4").text().trim()
						.contains("keine ausgeliehenen Medien")) {
					// There is no lent media, but the server is working
					// correctly
				} else if (doc.select("h1").text().trim()
						.contains("RUNTIME ERROR")) {
					// Server Error
					throw new OpacErrorException(
							"Serverfehler. Bitte probieren Sie es später noch einmal.");
				} else {
					throw new OpacErrorException(
							"Unbekannter Fehler: "
									+ doc.select("h1").text().trim()
									+ " Bitte prüfen Sie, ob ihre Kontodaten korrekt sind.");
				}
			} else {
				throw new OpacErrorException(
						"Unbekannter Fehler. Bitte prüfen Sie, ob ihre Kontodaten korrekt sind.");
			}
		}
		return res;

	}

	protected void parse_medialist(List<Map<String, String>> medien,
			Document doc, int offset) throws ClientProtocolException,
			IOException {

		Elements copytrs = doc.select("a[name=AUS] ~ table").first()
				.select("tr");
		doc.setBaseUri(opac_url);

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

		int trs = copytrs.size();
		if (trs < 2)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			Map<String, String> e = new HashMap<String, String>();

			e.put(AccountData.KEY_LENT_TITLE, tr.child(0).text().trim()
					.replace("\u00a0", ""));
			e.put(AccountData.KEY_LENT_AUTHOR, tr.child(1).text().trim()
					.replace("\u00a0", ""));
			int prolongCount = Integer.parseInt(tr.child(3).text().trim()
					.replace("\u00a0", ""));
			/*
			 * not needed currently, because in Schleswig books can only be pro-
			 * longed once, so the prolong count is visible from the renewable
			 * status e.put(AccountData.KEY_LENT_STATUS,
			 * String.valueOf(prolongCount) + "x verl.");
			 */
			if (maxProlongCount != -1) {
				e.put(AccountData.KEY_LENT_RENEWABLE,
						prolongCount < maxProlongCount ? "Y" : "N");
			}
			e.put(AccountData.KEY_LENT_DEADLINE, tr.child(4).text().trim()
					.replace("\u00a0", ""));
			try {
				e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
						.valueOf(sdf
								.parse(e.get(AccountData.KEY_LENT_DEADLINE))
								.getTime()));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			String link = tr.child(5).select("a").attr("href");
			e.put(AccountData.KEY_LENT_LINK, link);
			// find media number with regex
			Pattern pattern = Pattern.compile("mednr=([^&]*)&");
			Matcher matcher = pattern.matcher(link);
			matcher.find();
			if (matcher.group() != null) {
				e.put(AccountData.KEY_LENT_ID, matcher.group(1));
			}

			medien.add(e);
		}
		assert (medien.size() == trs - 1);

	}

	protected void parse_reslist(List<Map<String, String>> medien,
			Document doc, int offset) throws ClientProtocolException,
			IOException {
		Elements copytrs = doc.select("a[name=RES] ~ table:contains(Titel)")
				.first().select("tr");
		doc.setBaseUri(opac_url);

		int trs = copytrs.size();
		if (trs < 2)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			Map<String, String> e = new HashMap<String, String>();

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

	private SearchField createSearchField(Element descTd, Element inputTd) {
		String name = descTd.select("span, blockquote").text().replace(":", "").trim()
				.replace("\u00a0", "");
		if (inputTd.select("select").size() > 0
				&& !name.equals("Treffer/Seite") && !name.equals("Medientypen")
				&& !name.equals("Treffer pro Seite")) {
			Element select = inputTd.select("select").first();
			DropdownSearchField field = new DropdownSearchField();
			field.setDisplayName(name);
			field.setId(select.attr("name"));
			List<Map<String, String>> options = new ArrayList<Map<String, String>>();
			for (Element option : select.select("option")) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("key", option.attr("value"));
				map.put("value", option.text());
				options.add(map);
			}
			field.setDropdownValues(options);
			return field;
		} else if (inputTd.select("input").size() > 0) {
			TextSearchField field = new TextSearchField();
			Element input = inputTd.select("input").first();
			field.setDisplayName(name);
			field.setId(input.attr("name"));
			field.setHint("");
			return field;
		} else {
			return null;
		}
	}

	@Override
	public List<SearchField> getSearchFields() throws IOException {
		List<SearchField> fields = new ArrayList<SearchField>();

		// Extract all search fields, except media types
		String html;
		try {
			html = httpGet(opac_url + dir + "/search_expert.htm",
					getDefaultEncoding());
		} catch (NotReachableException e) {
			html = httpGet(opac_url + dir + "/iopacie.htm",
					getDefaultEncoding());
		}
		Document doc = Jsoup.parse(html);
		Elements trs = doc.select("form tr:has(input:not([type=submit], [type=reset])), form tr:has(select)");
		for (Element tr : trs) {
			Elements tds = tr.select("td");
			if (tds.size() == 4) {
				// Two search fields next to each other in one row
				SearchField field1 = createSearchField(tds.get(0), tds.get(1));
				SearchField field2 = createSearchField(tds.get(2), tds.get(3));
				if (field1 != null)
					fields.add(field1);
				if (field2 != null)
					fields.add(field2);
			} else if (tds.size() == 2 || (tds.size() == 3 && tds.get(2).children().size() == 0) ) {
				SearchField field = createSearchField(tds.get(0), tds.get(1));
				if (field != null)
					fields.add(field);
			}
		}

		// Extract available media types.
		// We have to parse JavaScript. Doing this with RegEx is evil.
		// But not as evil as including a JavaScript VM into the app.
		// And I honestly do not see another way.
		Pattern pattern_key = Pattern
				.compile("mtyp\\[[0-9]+\\]\\[\"typ\"\\] = \"([^\"]+)\";");
		Pattern pattern_value = Pattern
				.compile("mtyp\\[[0-9]+\\]\\[\"bez\"\\] = \"([^\"]+)\";");

		List<Map<String, String>> mediatypes = new ArrayList<Map<String, String>>();
		try {
			html = httpGet(opac_url + dir + "/mtyp.js", getDefaultEncoding());

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
				if (value != "") {
					Map<String, String> mediatype = new HashMap<String, String>();
					mediatype.put("key", key);
					mediatype.put("value", value);
					mediatypes.add(mediatype);
				}
			}
		} catch (IOException e) {
			try {
				html = httpGet(opac_url + dir
						+ "/frames/search_form.php?bReset=1?bReset=1",
						getDefaultEncoding());
				doc = Jsoup.parse(html);

				for (Element opt : doc.select("#imtyp option")) {
					Map<String, String> mediatype = new HashMap<String, String>();
					mediatype.put("key", opt.attr("value"));
					mediatype.put("value", opt.text());
					mediatypes.add(mediatype);
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
		if (mediatypes.size() > 0) {
			DropdownSearchField mtyp = new DropdownSearchField();
			mtyp.setDisplayName("Medientypen");
			mtyp.setId("Medientyp");
			mtyp.setDropdownValues(mediatypes);
			fields.add(mtyp);
		}
		return fields;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		if (data.has("account")) {
			try {
				return data.getBoolean("account");
			} catch (JSONException e) {
				return true;
			}
		}
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
		return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;

	}

	public void updateRechnr(Document doc) {
		String url = null;
		for (Element a : doc.select("table a")) {
			if (a.attr("href").contains("rechnr=")) {
				url = a.attr("href");
				break;
			}
		}
		if (url == null)
			return;

		Integer rechnrPosition = url.indexOf("rechnr=") + 7;
		rechnr = url
				.substring(rechnrPosition, url.indexOf("&", rechnrPosition));
	}

}
