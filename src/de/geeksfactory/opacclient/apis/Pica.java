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
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import de.geeksfactory.opacclient.ISBNTools;
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
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * @author Johan von Forstner, 16.09.2013
 * */

public class Pica extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected String https_url = "";
	protected JSONObject data;
	protected boolean initialised = false;
	protected Library library;
	protected int resultcount = 10;
	protected String reusehtml;
	protected Integer searchSet;
	protected String db;
	protected String pwEncoded;
	CookieStore cookieStore = new BasicCookieStore();

	protected String lor_reservations;

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("book", MediaType.BOOK);
		defaulttypes.put("article", MediaType.BOOK);
		defaulttypes.put("binary", MediaType.EBOOK);
		defaulttypes.put("periodical", MediaType.MAGAZINE);
		defaulttypes.put("onlineper", MediaType.EBOOK);
		defaulttypes.put("letter", MediaType.UNKNOWN);
		defaulttypes.put("handwriting", MediaType.UNKNOWN);
		defaulttypes.put("map", MediaType.MAP);
		defaulttypes.put("picture", MediaType.ART);
		defaulttypes.put("audiovisual", MediaType.MOVIE);
		defaulttypes.put("score", MediaType.SCORE_MUSIC);
		defaulttypes.put("sound", MediaType.CD_MUSIC);
		defaulttypes.put("software", MediaType.CD_SOFTWARE);
		defaulttypes.put("microfilm", MediaType.UNKNOWN);
		defaulttypes.put("empty", MediaType.UNKNOWN);
	}

	@Override
	public void start() throws IOException, NotReachableException {
		// String html = httpGet(opac_url
		// + "/DB=" + db + "/SET=1/TTL=1/ADVANCED_SEARCHFILTER",
		// getDefaultEncoding(), false, cookieStore);

		// Document doc = Jsoup.parse(html);

		// updateSearchSetValue(doc);
	}

	@Override
	public void init(Library lib) {
		super.init(lib);

		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
			this.db = data.getString("db");
			if (!library.getData().isNull("accountSupported")) {
				if (data.has("httpsbaseurl")) {
					this.https_url = data.getString("httpsbaseurl");
				} else {
					this.https_url = this.opac_url;
				}
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected int addParameters(SearchQuery query, List<NameValuePair> params, int index) throws JSONException {
		if (query.getValue().equals("") || query.getValue().equals("false"))
			return index;
		if (query.getSearchField() instanceof TextSearchField) {
			if (query.getSearchField().getData().getBoolean("ADI")) {
				params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
			} else {
				if (index == 0) {
					params.add(new BasicNameValuePair("ACT" + index, "SRCH"));
				} else {
					params.add(new BasicNameValuePair("ACT" + index, "*"));
				}
		
				params.add(new BasicNameValuePair("IKT" + index, query.getKey()));
				params.add(new BasicNameValuePair("TRM" + index, query.getValue()));
				return index + 1;
			}
		} else if (query.getSearchField() instanceof CheckboxSearchField) {
			boolean checked = Boolean.valueOf(query.getValue());
			if (checked) {
				params.add(new BasicNameValuePair(query.getKey(), "Y"));
			}
		} else if (query.getSearchField() instanceof DropdownSearchField) {
			params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
		}
		return index;
	}

	@Override
	public SearchRequestResult search(List<SearchQuery> query)
			throws IOException, NotReachableException, OpacErrorException, JSONException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		int index = 0;
		start();

		params.add(new BasicNameValuePair("ACT", "SRCHM"));
		params.add(new BasicNameValuePair("MATCFILTER", "Y"));
		params.add(new BasicNameValuePair("MATCSET", "Y"));
		params.add(new BasicNameValuePair("NOSCAN", "Y"));
		params.add(new BasicNameValuePair("PARSE_MNEMONICS", "N"));
		params.add(new BasicNameValuePair("PARSE_OPWORDS", "N"));
		params.add(new BasicNameValuePair("PARSE_OLDSETS", "N"));

		for (SearchQuery singleQuery:query) {
			index = addParameters(singleQuery, params, index);
		}

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}
		if (index > 4) {
			throw new OpacErrorException(
					"Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.");
		}

		String html = httpGet(opac_url + "/DB=" + db + "/SET=1/TTL=1/CMD?"
				+ URLEncodedUtils.format(params, getDefaultEncoding()),
				getDefaultEncoding(), false, cookieStore);

		return parse_search(html, 1);
	}

	protected SearchRequestResult parse_search(String html, int page)
			throws OpacErrorException {
		Document doc = Jsoup.parse(html);

		updateSearchSetValue(doc);

		if (doc.select(".error").size() > 0) {
			if (doc.select(".error").text().trim()
					.equals("Es wurde nichts gefunden.")) {
				// nothing found
				return new SearchRequestResult(new ArrayList<SearchResult>(),
						0, 1, 1);
			} else {
				// error
				throw new OpacErrorException(doc.select(".error").first()
						.text().trim());
			}
		}

		reusehtml = html;

		int results_total = -1;

		String resultnumstr = doc.select(".pages").first().text();
		Pattern p = Pattern.compile("[0-9]+$");
		Matcher m = p.matcher(resultnumstr);
		if (m.find()) {
			resultnumstr = m.group();
		}
		if (resultnumstr.contains("(")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*\\(([0-9]+)\\).*", "$1"));
		} else if (resultnumstr.contains(": ")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*: ([0-9]+)$", "$1"));
		} else {
			results_total = Integer.parseInt(resultnumstr);
		}

		List<SearchResult> results = new ArrayList<SearchResult>();

		if (results_total == 1) {
			// Only one result
			try {
				DetailledItem singleResult = parse_result(html);
				SearchResult sr = new SearchResult();
				sr.setType(getMediaTypeInSingleResult(html));
				sr.setInnerhtml("<b>" + singleResult.getTitle() + "</b><br>"
						+ singleResult.getDetails().get(0).getContent());
				results.add(sr);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Elements table = doc
				.select("table[summary=hitlist] tbody tr[valign=top]");
		// identifier = null;

		Elements links = doc.select("table[summary=hitlist] a");
		boolean haslink = false;
		for (int i = 0; i < links.size(); i++) {
			Element node = links.get(i);
			if (node.hasAttr("href") & node.attr("href").contains("SHW?")
					&& !haslink) {
				haslink = true;
				try {
					List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
							node.attr("href")), getDefaultEncoding());
					for (NameValuePair nv : anyurl) {
						if (nv.getName().equals("identifier")) {
							// identifier = nv.getValue();
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			if (tr.select("td.hit img").size() > 0) {
				String[] fparts = tr.select("td img").get(0).attr("src")
						.split("/");
				String fname = fparts[fparts.length - 1];
				if (data.has("mediatypes")) {
					try {
						sr.setType(MediaType.valueOf(data.getJSONObject(
								"mediatypes").getString(fname)));
					} catch (JSONException e) {
						sr.setType(defaulttypes.get(fname
								.toLowerCase(Locale.GERMAN).replace(".jpg", "")
								.replace(".gif", "").replace(".png", "")));
					} catch (IllegalArgumentException e) {
						sr.setType(defaulttypes.get(fname
								.toLowerCase(Locale.GERMAN).replace(".jpg", "")
								.replace(".gif", "").replace(".png", "")));
					}
				} else {
					sr.setType(defaulttypes.get(fname
							.toLowerCase(Locale.GERMAN).replace(".jpg", "")
							.replace(".gif", "").replace(".png", "")));
				}
			}
			Element middlething = tr.child(2);

			List<Node> children = middlething.childNodes();
			int childrennum = children.size();

			List<String[]> strings = new ArrayList<String[]>();
			for (int ch = 0; ch < childrennum; ch++) {
				Node node = children.get(ch);
				if (node instanceof TextNode) {
					String text = ((TextNode) node).text().trim();
					if (text.length() > 3)
						strings.add(new String[] { "text", "", text });
				} else if (node instanceof Element) {

					List<Node> subchildren = node.childNodes();
					for (int j = 0; j < subchildren.size(); j++) {
						Node subnode = subchildren.get(j);
						if (subnode instanceof TextNode) {
							String text = ((TextNode) subnode).text().trim();
							if (text.length() > 3)
								strings.add(new String[] {
										((Element) node).tag().getName(),
										"text", text,
										((Element) node).className(),
										((Element) node).attr("style") });
						} else if (subnode instanceof Element) {
							String text = ((Element) subnode).text().trim();
							if (text.length() > 3)
								strings.add(new String[] {
										((Element) node).tag().getName(),
										((Element) subnode).tag().getName(),
										text, ((Element) node).className(),
										((Element) node).attr("style") });
						}
					}
				}
			}

			StringBuilder description = new StringBuilder();

			int k = 0;
			for (String[] part : strings) {
				if (part[0] == "a" && k == 0) {
					description.append("<b>" + part[2] + "</b>");
				} else if (k < 3) {
					description.append("<br />" + part[2]);
				}
				k++;
			}
			sr.setInnerhtml(description.toString());

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

		String html = httpGet(opac_url + "/DB=" + db + "/SET=" + searchSet
				+ "/TTL=1/NXT?FRST=" + (((page - 1) * resultcount) + 1),
				getDefaultEncoding(), false, cookieStore);
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

		String html = httpGet(id, getDefaultEncoding());

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		String html = httpGet(opac_url + "/DB=" + db + "/SET=" + searchSet
				+ "/TTL=1/SHW?FRST=" + (position + 1), getDefaultEncoding(),
				false, cookieStore);

		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);

		DetailledItem result = new DetailledItem();

		if (doc.select("img[src*=permalink], img[src*=zitierlink]").size() > 0) {
			String id = opac_url
					+ doc.select("img[src*=permalink], img[src*=zitierlink]")
							.get(0).parent().absUrl("href");
			result.setId(id);
		} else {
			for (Element a : doc.select("a")) {
				if (a.attr("href").contains("PPN=")) {
					Map<String, String> hrefq = getQueryParamsFirst(a
							.absUrl("href"));
					String ppn = hrefq.get("PPN");
					try {
						result.setId(opac_url + "/DB=" + data.getString("db")
								+ "/PPNSET?PPN=" + ppn);
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
					break;
				}
			}

		}

		// GET COVER
		if (doc.select("td.preslabel:contains(ISBN) + td.presvalue").size() > 0) {
			Element isbnElement = doc.select(
					"td.preslabel:contains(ISBN) + td.presvalue").first();
			String isbn = "";
			for (Node child : isbnElement.childNodes()) {
				if (child instanceof TextNode) {
					isbn = ((TextNode) child).text().trim();
					break;
				}
			}
			result.setCover(ISBNTools.getAmazonCoverURL(isbn, true));
		}

		// GET TITLE AND SUBTITLE
		String titleAndSubtitle = "";
		if (doc.select("td.preslabel:contains(Titel) + td.presvalue").size() > 0) {
			titleAndSubtitle = doc
					.select("td.preslabel:contains(Titel) + td.presvalue")
					.first().text().trim();
			int slashPosition = titleAndSubtitle.indexOf("/");
			String title;
			String subtitle;
			if (slashPosition > 0) {
				title = titleAndSubtitle.substring(0, slashPosition).trim();
				subtitle = titleAndSubtitle.substring(slashPosition + 1).trim();
				result.addDetail(new Detail("Titelzusatz", subtitle));
			} else {
				title = titleAndSubtitle;
				subtitle = "";
			}
			result.setTitle(title);
		} else if (doc.select("td.preslabel:contains(Aufsatz) + td.presvalue")
				.size() > 0) {
			titleAndSubtitle = doc
					.select("td.preslabel:contains(Aufsatz) + td.presvalue")
					.first().text().trim();
			int slashPosition = titleAndSubtitle.indexOf("/");
			String title;
			String subtitle;
			if (slashPosition > 0) {
				title = titleAndSubtitle.substring(0, slashPosition).trim();
				subtitle = titleAndSubtitle.substring(slashPosition + 1).trim();
				result.addDetail(new Detail("Titelzusatz", subtitle));
			} else {
				title = titleAndSubtitle;
				subtitle = "";
			}
			result.setTitle(title);
		} else if (doc.select(
				"td.preslabel:contains(Zeitschrift) + td.presvalue").size() > 0) {
			titleAndSubtitle = doc
					.select("td.preslabel:contains(Zeitschrift) + td.presvalue")
					.first().text().trim();
			int slashPosition = titleAndSubtitle.indexOf("/");
			String title;
			String subtitle;
			if (slashPosition > 0) {
				title = titleAndSubtitle.substring(0, slashPosition).trim();
				subtitle = titleAndSubtitle.substring(slashPosition + 1).trim();
				result.addDetail(new Detail("Titelzusatz", subtitle));
			} else {
				title = titleAndSubtitle;
				subtitle = "";
			}
			result.setTitle(title);
		} else {
			result.setTitle("");
		}

		// GET OTHER INFORMATION
		Map<String, String> e = new HashMap<String, String>();
		String location = "";

		// reservation info will be stored as JSON, because it can get
		// complicated
		JSONArray reservationInfo = new JSONArray();

		for (Element element : doc.select("td.preslabel + td.presvalue")) {
			Element titleElem = element.firstElementSibling();
			String detail = element.text().trim();
			String title = titleElem.text().trim().replace("\u00a0", "");

			if (element.select("hr").size() == 0) { // no separator

				if (title.indexOf(":") != -1) {
					title = title.substring(0, title.indexOf(":")); // remove
																	// colon
				}

				if (title.contains("Standort")
						|| title.contains("Vorhanden in")) {
					location += detail;
				} else if (title.contains("Sonderstandort")) {
					location += " - " + detail;
				} else if (title.contains("Fachnummer")) {
					e.put(DetailledItem.KEY_COPY_LOCATION, detail);
				} else if (title.contains("Signatur")) {
					e.put(DetailledItem.KEY_COPY_SHELFMARK, detail);
				} else if (title.contains("Anmerkung")) {
					location += " (" + detail + ")";
				} else if (title.contains("Status")
						|| title.contains("Ausleihinfo")) {
					detail = detail.replace("Bestellen", "")
							.replace("verfuegbar", "verfügbar")
							.replace("Verfuegbar", "verfügbar")
							.replace("Vormerken", "")
							.replace("Liste der Exemplare", "").trim();
					e.put(DetailledItem.KEY_COPY_STATUS, detail);
					// Get reservation info
					if (element.select(
							"a:contains(Vormerken), a:contains(Exemplare)")
							.size() > 0) {
						Element a = element.select(
								"a:contains(Vormerken), a:contains(Exemplare)")
								.first();
						boolean multipleCopies = a.text().contains("Exemplare");
						JSONObject reservation = new JSONObject();
						try {
							reservation.put("multi", multipleCopies);
							reservation.put("link", opac_url + a.attr("href"));
							reservation.put("desc", location);
							reservationInfo.put(reservation);
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						result.setReservable(true);
					}
				} else if (!title.equals("Titel")) {
					result.addDetail(new Detail(title, detail));
				}
			} else if (e.size() > 0) {
				e.put(DetailledItem.KEY_COPY_BRANCH, location);
				result.addCopy(e);
				location = "";
				e = new HashMap<String, String>();
			}
		}

		e.put(DetailledItem.KEY_COPY_BRANCH, location);
		result.addCopy(e);

		result.setReservation_info(reservationInfo.toString());
		;

		return result;
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		try {
			if (selection == null || !selection.startsWith("{")) {
				JSONArray json = new JSONArray(item.getReservation_info());
				int selectedPos;
				if (selection != null) {
					selectedPos = Integer.parseInt(selection);
				} else if (json.length() == 1) {
					selectedPos = 0;
				} else {
					// a location must be selected
					ReservationResult res = new ReservationResult(
							MultiStepResult.Status.SELECTION_NEEDED);
					res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
					Map<String, String> selections = new HashMap<String, String>();
					for (int i = 0; i < json.length(); i++) {
						selections.put(String.valueOf(i), json.getJSONObject(i)
								.getString("desc"));
					}
					res.setSelection(selections);
					return res;
				}

				if (json.getJSONObject(selectedPos).getBoolean("multi")) {
					// A copy must be selected
					String html1 = httpGet(json.getJSONObject(selectedPos)
							.getString("link"), getDefaultEncoding());
					Document doc1 = Jsoup.parse(html1);

					Elements trs = doc1
							.select("table[summary=list of volumes header] tr:has(input[type=radio])");

					if (trs.size() > 0) {
						Map<String, String> selections = new HashMap<String, String>();
						for (Element tr : trs) {
							JSONObject values = new JSONObject();
							for (Element input : doc1
									.select("input[type=hidden]")) {
								values.put(input.attr("name"),
										input.attr("value"));
							}
							values.put(tr.select("input").attr("name"), tr
									.select("input").attr("value"));
							selections.put(values.toString(), tr.text());
						}

						ReservationResult res = new ReservationResult(
								MultiStepResult.Status.SELECTION_NEEDED);
						res.setActionIdentifier(ReservationResult.ACTION_USER);
						res.setMessage("Welches Exemplar soll reserviert/bestellt werden? (verfügbare Exemplare werden bestellt)");
						res.setSelection(selections);
						return res;
					} else {
						ReservationResult res = new ReservationResult(
								MultiStepResult.Status.ERROR);
						res.setMessage("Es ist kein Exemplar reservierbar.");
						return res;
					}

				} else {
					String html1 = httpGet(json.getJSONObject(selectedPos)
							.getString("link"), getDefaultEncoding());
					Document doc1 = Jsoup.parse(html1);

					List<NameValuePair> params = new ArrayList<NameValuePair>();

					for (Element input : doc1.select("input[type=hidden]")) {
						params.add(new BasicNameValuePair(input.attr("name"),
								input.attr("value")));
					}

					params.add(new BasicNameValuePair("BOR_U", account
							.getName()));
					params.add(new BasicNameValuePair("BOR_PW", account
							.getPassword()));

					return reservation_result(params, false);
				}
			} else {
				// A copy has been selected
				JSONObject values = new JSONObject(selection);
				List<NameValuePair> params = new ArrayList<NameValuePair>();

				Iterator<String> keys = values.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					String value = values.getString(key);
					params.add(new BasicNameValuePair(key, value));
				}

				params.add(new BasicNameValuePair("BOR_U", account.getName()));
				params.add(new BasicNameValuePair("BOR_PW", account
						.getPassword()));

				return reservation_result(params, true);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			ReservationResult res = new ReservationResult(
					MultiStepResult.Status.ERROR);
			res.setMessage("interner Fehler");
			return res;
		}
	}

	public ReservationResult reservation_result(List<NameValuePair> params,
			boolean multi) throws IOException {
		String html2 = httpPost(https_url + "/loan/LNG=DU/DB=" + db + "/SET="
				+ searchSet + "/TTL=1/" + (multi ? "REQCONT" : "RESCONT"),
				new UrlEncodedFormEntity(params, getDefaultEncoding()),
				getDefaultEncoding());
		Document doc2 = Jsoup.parse(html2);

		if (doc2.select(".alert").text().contains("ist fuer Sie vorgemerkt")) {
			return new ReservationResult(MultiStepResult.Status.OK);
		} else {
			ReservationResult res = new ReservationResult(
					MultiStepResult.Status.ERROR);
			res.setMessage(doc2.select(".cnt .alert").text());
			return res;
		}
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String Selection) throws IOException {
		if (pwEncoded == null)
			try {
				account(account);
			} catch (JSONException e1) {
				return new ProlongResult(MultiStepResult.Status.ERROR);
			} catch (OpacErrorException e1) {
				return new ProlongResult(MultiStepResult.Status.ERROR,
						e1.getMessage());
			}

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ACT", "UI_RENEWLOAN"));

		params.add(new BasicNameValuePair("BOR_U", account.getName()));
		params.add(new BasicNameValuePair("BOR_PW_ENC", URLDecoder.decode(
				pwEncoded, "UTF-8")));

		params.add(new BasicNameValuePair("VB", media));

		String html = httpPost(https_url + "/loan/DB=" + db + "/USERINFO",
				new UrlEncodedFormEntity(params, getDefaultEncoding()),
				getDefaultEncoding());
		Document doc = Jsoup.parse(html);

		if (doc.select("td.regular-text")
				.text()
				.contains(
						"Die Leihfrist Ihrer ausgeliehenen Publikationen ist ")
				|| doc.select("td.regular-text").text()
						.contains("Ihre ausgeliehenen Publikationen sind verl")) {
			return new ProlongResult(MultiStepResult.Status.OK);
		} else if (doc.select(".cnt").text().contains("identify")) {
			try {
				account(account);
				return prolong(media, account, useraction, Selection);
			} catch (JSONException e) {
				return new ProlongResult(MultiStepResult.Status.ERROR);
			} catch (OpacErrorException e) {
				return new ProlongResult(MultiStepResult.Status.ERROR,
						e.getMessage());
			}
		} else {
			ProlongResult res = new ProlongResult(MultiStepResult.Status.ERROR);
			res.setMessage(doc.select(".cnt").text());
			return res;
		}
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ACT", "UI_CANCELRES"));

		params.add(new BasicNameValuePair("BOR_U", account.getName()));
		params.add(new BasicNameValuePair("BOR_PW_ENC", URLDecoder.decode(
				pwEncoded, "UTF-8")));
		if (lor_reservations != null)
			params.add(new BasicNameValuePair("LOR_RESERVATIONS",
					lor_reservations));

		params.add(new BasicNameValuePair("VB", media));

		String html = httpPost(https_url + "/loan/DB=" + db + "/SET="
				+ searchSet + "/TTL=1/USERINFO", new UrlEncodedFormEntity(
				params, getDefaultEncoding()), getDefaultEncoding());
		Document doc = Jsoup.parse(html);

		if (doc.select("td.regular-text").text()
				.contains("Ihre Vormerkungen sind ")) {
			return new CancelResult(MultiStepResult.Status.OK);
		} else if (doc.select(".cnt .alert").text()
				.contains("identify yourself")) {
			try {
				account(account);
				return cancel(media, account, useraction, selection);
			} catch (JSONException e) {
				throw new OpacErrorException("Interner Fehler");
			}
		} else {
			CancelResult res = new CancelResult(MultiStepResult.Status.ERROR);
			res.setMessage(doc.select(".cnt").text());
			return res;
		}
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ACT", "UI_DATA"));
		params.add(new BasicNameValuePair("HOST_NAME", ""));
		params.add(new BasicNameValuePair("HOST_PORT", ""));
		params.add(new BasicNameValuePair("HOST_SCRIPT", ""));
		params.add(new BasicNameValuePair("LOGIN", "KNOWNUSER"));
		params.add(new BasicNameValuePair("STATUS", "HML_OK"));

		params.add(new BasicNameValuePair("BOR_U", account.getName()));
		params.add(new BasicNameValuePair("BOR_PW", account.getPassword()));

		String html = httpPost(https_url + "/loan/DB=" + db
				+ "/LNG=DU/USERINFO", new UrlEncodedFormEntity(params,
				getDefaultEncoding()), getDefaultEncoding());
		Document doc = Jsoup.parse(html);

		if (doc.select(".cnt .alert, .cnt .error").size() > 0) {
			throw new OpacErrorException(doc.select(".cnt .alert, .cnt .error")
					.text());
		}
		if (doc.select("input[name=BOR_PW_ENC]").size() > 0) {
			pwEncoded = URLEncoder.encode(doc.select("input[name=BOR_PW_ENC]")
					.attr("value"), "UTF-8");
		} else {
			// TODO: do something here to help fix bug #229
		}

		html = httpGet(https_url + "/loan/DB=" + db
				+ "/USERINFO?ACT=UI_LOL&BOR_U=" + account.getName()
				+ "&BOR_PW_ENC=" + pwEncoded, getDefaultEncoding());
		doc = Jsoup.parse(html);

		html = httpGet(https_url + "/loan/DB=" + db
				+ "/USERINFO?ACT=UI_LOR&BOR_U=" + account.getName()
				+ "&BOR_PW_ENC=" + pwEncoded, getDefaultEncoding());
		Document doc2 = Jsoup.parse(html);

		AccountData res = new AccountData(account.getId());

		List<Map<String, String>> medien = new ArrayList<Map<String, String>>();
		List<Map<String, String>> reserved = new ArrayList<Map<String, String>>();
		if (doc.select("table[summary^=list]").size() > 0) {
			parse_medialist(medien, doc, 1, account.getName());
		}
		if (doc2.select("table[summary^=list]").size() > 0) {
			parse_reslist(reserved, doc2, 1);
		}

		res.setLent(medien);
		res.setReservations(reserved);

		if (medien == null || reserved == null) {
			throw new OpacErrorException(
					"Unbekannter Fehler. Bitte prüfen Sie, ob ihre Kontodaten korrekt sind.");
			// Log.d("OPACCLIENT", html);
		}
		return res;

	}

	protected void parse_medialist(List<Map<String, String>> medien,
			Document doc, int offset, String accountName)
			throws ClientProtocolException, IOException {

		Elements copytrs = doc.select("table[summary^=list] > tbody > tr[valign=top]");

		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.GERMAN);

		int trs = copytrs.size();
		if (trs < 1) {
			medien = null;
			return;
		}
		assert (trs > 0);
		for (int i = 0; i < trs; i++) {
			Element tr = copytrs.get(i);
			if (tr.children().size() == 8) { // According to HTML code from Bug report
											   // (TU Darmstadt)
				Map<String, String> e = new HashMap<String, String>();
				//Check if there is a checkbox to prolong this item
				if (tr.select("input").size() > 0)
					e.put(AccountData.KEY_LENT_LINK,
							tr.select("input").attr("value"));
				else
					e.put(AccountData.KEY_LENT_RENEWABLE, "N");
				
				Elements datatrs = tr.select("table[summary=title data] tr");
				e.put(AccountData.KEY_LENT_TITLE, datatrs.get(0).text());
				
				List<TextNode> textNodes = datatrs.get(1).select("td").first().textNodes();
				List<TextNode> nodes = new ArrayList<TextNode>();
				Elements titles = datatrs.get(1).select("span.label-small");
				
				for (TextNode node:textNodes) {
					if (!node.text().equals(" "))
						nodes.add(node);
				}

				assert (nodes.size() == titles.size());
				for (int j = 0; j<nodes.size(); j++) {			
					String title = titles.get(j).text();
					String value = nodes.get(j).text().trim().replace(";", "");
					if (title.contains("Signatur")) {
						// not supported
					} else if (title.contains("Status")) {
						e.put(AccountData.KEY_LENT_STATUS, value);
					} else if (title.contains("Leihfristende")) {
						e.put(AccountData.KEY_LENT_DEADLINE, value);
						try {
							e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
									.valueOf(sdf.parse(value).getTime()));
						} catch (ParseException e1) {
							e1.printStackTrace();
						}
					} else if (title.contains("Vormerkungen")) {
						// not supported
					}
				}
				medien.add(e);
			} else { //like in Kiel
				String prolongCount = "";
				try {
					String html = httpGet(https_url + "/nr_renewals.php?U="
							+ accountName + "&DB=" + db + "&VBAR="
							+ tr.child(1).select("input").attr("value"),
							getDefaultEncoding());
					prolongCount = Jsoup.parse(html).text();
				} catch (IOException e) {
	
				}
				String reminderCount = tr.child(13).text().trim();
				if (reminderCount.indexOf(" Mahn") >= 0
						&& reminderCount.indexOf("(") >= 0
						&& reminderCount.indexOf("(") < reminderCount
								.indexOf(" Mahn"))
					reminderCount = reminderCount.substring(
							reminderCount.indexOf("(") + 1,
							reminderCount.indexOf(" Mahn"));
				else
					reminderCount = "";
				Map<String, String> e = new HashMap<String, String>();
	
				if (tr.child(4).text().trim().length() < 5
						&& tr.child(5).text().trim().length() > 4) {
					e.put(AccountData.KEY_LENT_TITLE, tr.child(5).text().trim());
				} else {
					e.put(AccountData.KEY_LENT_TITLE, tr.child(4).text().trim());
				}
				String status = "";
				if (!reminderCount.equals("0") && !reminderCount.equals("")) {
					status += reminderCount + " Mahnungen, ";
				}
				status += prolongCount + "x verl."; // + tr.child(25).text().trim()
													// + " Vormerkungen");
				e.put(AccountData.KEY_LENT_STATUS, status);
				e.put(AccountData.KEY_LENT_DEADLINE, tr.child(21).text().trim());
				try {
					e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
							.valueOf(sdf
									.parse(e.get(AccountData.KEY_LENT_DEADLINE))
									.getTime()));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
				e.put(AccountData.KEY_LENT_LINK,
						tr.child(1).select("input").attr("value"));
	
				medien.add(e);
			}
		}
		assert (medien.size() == trs - 1);
	}

	protected void parse_reslist(List<Map<String, String>> medien,
			Document doc, int offset) throws ClientProtocolException,
			IOException {

		if (doc.select("input[name=LOR_RESERVATIONS]").size() > 0) {
			lor_reservations = doc.select("input[name=LOR_RESERVATIONS]").attr(
					"value");
		}

		Elements copytrs = doc.select("table[summary^=list] tr[valign=top]");

		int trs = copytrs.size();
		if (trs < 1) {
			medien = null;
			return;
		}
		assert (trs > 0);
		for (int i = 0; i < trs; i++) {
			Element tr = copytrs.get(i);
			Map<String, String> e = new HashMap<String, String>();

			e.put(AccountData.KEY_RESERVATION_TITLE, tr.child(5).text().trim());
			e.put(AccountData.KEY_RESERVATION_READY, tr.child(17).text().trim());
			e.put(AccountData.KEY_RESERVATION_CANCEL,
					tr.child(1).select("input").attr("value"));

			medien.add(e);
		}
		assert (medien.size() == trs - 1);
	}

	@Override
	public List<SearchField> getSearchFields() throws ClientProtocolException,
			IOException, JSONException {
		String html = httpGet(opac_url + "/ADVANCED_SEARCHFILTER",
				getDefaultEncoding());
		Document doc = Jsoup.parse(html);
		List<SearchField> fields = new ArrayList<SearchField>();

		Elements options = doc.select("select[name=IKT0] option");
		for (Element option : options) {
			TextSearchField field = new TextSearchField();
			field.setDisplayName(option.text());
			field.setId(option.attr("value"));
			field.setHint("");
			field.setData(new JSONObject("{\"ADI\": false}"));
			
			Pattern pattern = Pattern.compile("\\[[A-Z:]+\\]|\\([A-Z:]+\\)");
			Matcher matcher = pattern.matcher(field.getDisplayName());
			if (matcher.find()) {
				field.getData().put("meaning", matcher.group().replace(":", ""));
				field.setDisplayName(matcher.replaceFirst("").trim());
			}
			
			fields.add(field);
		}

		Elements sort = doc.select("select[name=SRT]");
		if (sort.size() > 0) {
			DropdownSearchField field = new DropdownSearchField();
			field.setDisplayName(sort.first().parent().parent()
					.select(".longval").first().text());
			field.setId("SRT");
			List<Map<String, String>> sortOptions = new ArrayList<Map<String, String>>();
			for (Element option : sort.select("option")) {
				Map<String, String> sortOption = new HashMap<String, String>();
				sortOption.put("key", option.attr("value"));
				sortOption.put("value", option.text());
				sortOptions.add(sortOption);
			}
			field.setDropdownValues(sortOptions);
			fields.add(field);
		}

		for (Element input : doc.select("input[type=text][name^=ADI]")) {
			TextSearchField field = new TextSearchField();
			field.setDisplayName(input.parent().parent().select(".longkey").text());
			field.setId(input.attr("name"));
			field.setHint(input.parent().select("span").text());
			field.setData(new JSONObject("{\"ADI\": true}"));
			fields.add(field);
		}
		
		for (Element dropdown : doc.select("select[name^=ADI]")) {
			DropdownSearchField field = new DropdownSearchField();
			field.setDisplayName(dropdown.parent().parent()
					.select(".longkey").text());
			field.setId(dropdown.attr("name"));
			List<Map<String, String>> dropdownOptions = new ArrayList<Map<String, String>>();
			for (Element option : dropdown.select("option")) {
				Map<String, String> dropdownOption = new HashMap<String, String>();
				dropdownOption.put("key", option.attr("value"));
				dropdownOption.put("value", option.text());
				dropdownOptions.add(dropdownOption);
			}
			field.setDropdownValues(dropdownOptions);
			fields.add(field);
		}
		
		Elements fuzzy = doc.select("input[name=FUZZY]");
		if (fuzzy.size() > 0) {
			CheckboxSearchField field = new CheckboxSearchField();
			field.setDisplayName(fuzzy.first().parent().parent()
					.select(".longkey").first().text());
			field.setId("FUZZY");
			fields.add(field);
		}
		
		Elements mediatypes = doc.select("input[name=ADI_MAT]");
		if (mediatypes.size() > 0) {
			DropdownSearchField field = new DropdownSearchField();
			field.setDisplayName("Materialart");
			field.setId("ADI_MAT");
			
			List<Map<String, String>> values = new ArrayList<Map<String, String>>();
			Map<String, String> all = new HashMap<String, String>();
			all.put("key", "");
			all.put("value", "Alle");
			values.add(all);
			for (Element mt:mediatypes) {
				Map<String, String> value = new HashMap<String, String>();
				value.put("key", mt.attr("value"));
				value.put("value", mt.parent().nextElementSibling().text().replace("\u00a0", ""));
				values.add(value);
			}
			field.setDropdownValues(values);
			fields.add(field);
		}

		return fields;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		if (!library.getData().isNull("accountSupported")) {
			try {
				return library.getData().getBoolean("accountSupported");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
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
		return id;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
	}

	public void updateSearchSetValue(Document doc) {
		String url = doc.select("base").first().attr("href");
		Integer setPosition = url.indexOf("SET=") + 4;
		String searchSetString = url.substring(setPosition,
				url.indexOf("/", setPosition));
		searchSet = Integer.parseInt(searchSetString);
	}

	public MediaType getMediaTypeInSingleResult(String html) {
		Document doc = Jsoup.parse(html);
		MediaType mediatype = MediaType.UNKNOWN;

		if (doc.select("table[summary=presentation switch] img").size() > 0) {

			String[] fparts = doc
					.select("table[summary=presentation switch] img").get(0)
					.attr("src").split("/");
			String fname = fparts[fparts.length - 1];

			if (data.has("mediatypes")) {
				try {
					mediatype = MediaType.valueOf(data.getJSONObject(
							"mediatypes").getString(fname));
				} catch (JSONException e) {
					mediatype = defaulttypes.get(fname
							.toLowerCase(Locale.GERMAN).replace(".jpg", "")
							.replace(".gif", "").replace(".png", ""));
				} catch (IllegalArgumentException e) {
					mediatype = defaulttypes.get(fname
							.toLowerCase(Locale.GERMAN).replace(".jpg", "")
							.replace(".gif", "").replace(".png", ""));
				}
			} else {
				mediatype = defaulttypes.get(fname.toLowerCase(Locale.GERMAN)
						.replace(".jpg", "").replace(".gif", "")
						.replace(".png", ""));
			}
		}

		return mediatype;
	}

	@Override
	protected String getDefaultEncoding() {
		try {
			if (data.has("charset"))
				return data.getString("charset");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "UTF-8";
	}

}