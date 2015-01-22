/**
 * Copyright (C) 2014 by Johan von Forstner under the MIT license:
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

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.i18n.StringProvider;
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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

/**
 * OpacApi implementation for Web Opacs of the TouchPoint product, developed by
 * OCLC.
 */
public class TouchPoint extends BaseApi implements OpacApi {
	protected String opac_url = "";
	protected JSONObject data;
	protected Library library;
	protected String CSId;
	protected String identifier;
	protected String reusehtml;
	protected int resultcount = 10;

	protected long logged_in;
	protected Account logged_in_as;
	protected final long SESSION_LIFETIME = 1000 * 60 * 3;

	protected String ENCODING = "UTF-8";

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("g", MediaType.EBOOK);
		defaulttypes.put("d", MediaType.CD);
		defaulttypes.put("buch", MediaType.BOOK);
		defaulttypes.put("bücher", MediaType.BOOK);
		defaulttypes.put("printmedien", MediaType.BOOK);
		defaulttypes.put("zeitschrift", MediaType.MAGAZINE);
		defaulttypes.put("zeitschriften", MediaType.MAGAZINE);
		defaulttypes.put("zeitung", MediaType.NEWSPAPER);
		defaulttypes.put(
				"Einzelband einer Serie, siehe auch übergeordnete Titel",
				MediaType.BOOK);
		defaulttypes.put("0", MediaType.BOOK);
		defaulttypes.put("1", MediaType.BOOK);
		defaulttypes.put("2", MediaType.BOOK);
		defaulttypes.put("3", MediaType.BOOK);
		defaulttypes.put("4", MediaType.BOOK);
		defaulttypes.put("5", MediaType.BOOK);
		defaulttypes.put("6", MediaType.SCORE_MUSIC);
		defaulttypes.put("7", MediaType.CD_MUSIC);
		defaulttypes.put("8", MediaType.CD_MUSIC);
		defaulttypes.put("tonträger", MediaType.CD_MUSIC);
		defaulttypes.put("12", MediaType.CD);
		defaulttypes.put("13", MediaType.CD);
		defaulttypes.put("cd", MediaType.CD);
		defaulttypes.put("dvd", MediaType.DVD);
		defaulttypes.put("14", MediaType.CD);
		defaulttypes.put("15", MediaType.DVD);
		defaulttypes.put("16", MediaType.CD);
		defaulttypes.put("audiocd", MediaType.CD);
		defaulttypes.put("film", MediaType.MOVIE);
		defaulttypes.put("filme", MediaType.MOVIE);
		defaulttypes.put("17", MediaType.MOVIE);
		defaulttypes.put("18", MediaType.MOVIE);
		defaulttypes.put("19", MediaType.MOVIE);
		defaulttypes.put("20", MediaType.DVD);
		defaulttypes.put("dvd", MediaType.DVD);
		defaulttypes.put("21", MediaType.SCORE_MUSIC);
		defaulttypes.put("noten", MediaType.SCORE_MUSIC);
		defaulttypes.put("22", MediaType.BLURAY);
		defaulttypes.put("23", MediaType.GAME_CONSOLE_PLAYSTATION);
		defaulttypes.put("26", MediaType.CD);
		defaulttypes.put("27", MediaType.CD);
		defaulttypes.put("28", MediaType.EBOOK);
		defaulttypes.put("31", MediaType.BOARDGAME);
		defaulttypes.put("35", MediaType.MOVIE);
		defaulttypes.put("36", MediaType.DVD);
		defaulttypes.put("37", MediaType.CD);
		defaulttypes.put("29", MediaType.AUDIOBOOK);
		defaulttypes.put("41", MediaType.GAME_CONSOLE);
		defaulttypes.put("42", MediaType.GAME_CONSOLE);
		defaulttypes.put("46", MediaType.GAME_CONSOLE_NINTENDO);
		defaulttypes.put("52", MediaType.EBOOK);
		defaulttypes.put("56", MediaType.EBOOK);
		defaulttypes.put("91", MediaType.EBOOK);
		defaulttypes.put("96", MediaType.EBOOK);
		defaulttypes.put("97", MediaType.EBOOK);
		defaulttypes.put("99", MediaType.EBOOK);
		defaulttypes.put("eb", MediaType.EBOOK);
		defaulttypes.put("ebook", MediaType.EBOOK);
		defaulttypes.put("buch01", MediaType.BOOK);
		defaulttypes.put("buch02", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("medienpaket", MediaType.PACKAGE);
		defaulttypes.put("datenbank", MediaType.PACKAGE);
		defaulttypes
				.put("medienpaket, lernkiste, lesekiste", MediaType.PACKAGE);
		defaulttypes.put("buch03", MediaType.BOOK);
		defaulttypes.put("buch04", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("buch05", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("web-link", MediaType.URL);
		defaulttypes.put("ejournal", MediaType.EDOC);
		defaulttypes.put("karte", MediaType.MAP);
	}

	public List<SearchField> getSearchFields() throws IOException,
			JSONException {
		if (!initialised)
			start();

		String html = httpGet(opac_url
				+ "/search.do?methodToCall=switchSearchPage&SearchType=2",
				ENCODING);
		Document doc = Jsoup.parse(html);
		List<SearchField> fields = new ArrayList<SearchField>();

		Elements options = doc
				.select("select[name=searchCategories[0]] option");
		for (Element option : options) {
			TextSearchField field = new TextSearchField();
			field.setDisplayName(option.text());
			field.setId(option.attr("value"));
			field.setHint("");
			fields.add(field);
		}

		for (Element dropdown : doc.select(".accordion-body select")) {
			parseDropdown(dropdown, fields, doc);
		}

		return fields;
	}

	private void parseDropdown(Element dropdownElement,
			List<SearchField> fields, Document doc) throws JSONException {
		Elements options = dropdownElement.select("option");
		DropdownSearchField dropdown = new DropdownSearchField();
		List<Map<String, String>> values = new ArrayList<Map<String, String>>();
		dropdown.setId(dropdownElement.attr("name"));
		// Some fields make no sense or are not supported in the app
		if (dropdown.getId().equals("numberOfHits")
				|| dropdown.getId().equals("timeOut")
				|| dropdown.getId().equals("rememberList"))
			return;
		for (Element option : options) {
			Map<String, String> value = new HashMap<String, String>();
			value.put("key", option.attr("value"));
			value.put("value", option.text());
			values.add(value);
		}
		dropdown.setDropdownValues(values);
		dropdown.setDisplayName(dropdownElement.parent().select("label").text());
		fields.add(dropdown);
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {

		// Some libraries require start parameters for start.do, like Login=foo
		String startparams = "";
		if (data.has("startparams")) {
			try {
				startparams = "?" + data.getString("startparams");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		String html = httpGet(opac_url + "/start.do" + startparams, ENCODING);

		initialised = true;

		Document doc = Jsoup.parse(html);
		CSId = doc.select("input[name=CSId]").val();

		super.start();
	}

	@Override
	public void init(Library lib) {
		super.init(lib);

		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getStringFromBundle(Map<String, String> bundle,
			String key) {
		// Workaround for Bundle.getString(key, default) being available not
		// before API 12
		String res = bundle.get(key);
		if (res == null)
			res = "";
		return res;
	}

	protected int addParameters(Map<String, String> query, String key,
			String searchkey, List<NameValuePair> params, int index) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return index;

		if (index != 0)
			params.add(new BasicNameValuePair("combinationOperator[" + index
					+ "]", "AND"));
		params.add(new BasicNameValuePair("searchCategories[" + index + "]",
				searchkey));
		params.add(new BasicNameValuePair("searchString[" + index + "]", query
				.get(key)));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(List<SearchQuery> query)
			throws IOException, NotReachableException, OpacErrorException,
			JSONException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		int index = 0;
		start();

		params.add(new BasicNameValuePair("methodToCall", "submitButtonCall"));
		params.add(new BasicNameValuePair("CSId", CSId));
		params.add(new BasicNameValuePair("methodToCallParameter",
				"submitSearch"));
		params.add(new BasicNameValuePair("refine", "false"));

		for (SearchQuery entry : query) {
			if (entry.getValue().equals(""))
				continue;
			if (entry.getSearchField() instanceof DropdownSearchField) {
				params.add(new BasicNameValuePair(entry.getKey(), entry
						.getValue()));
			} else {
				if (index != 0)
					params.add(new BasicNameValuePair("combinationOperator["
							+ index + "]", "AND"));
				params.add(new BasicNameValuePair("searchCategories[" + index
						+ "]", entry.getKey()));
				params.add(new BasicNameValuePair(
						"searchString[" + index + "]", entry.getValue()));
				index++;
			}
		}

		if (index == 0) {
			throw new OpacErrorException(
					stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
		}
		if (index > 4) {
			throw new OpacErrorException(stringProvider.getFormattedString(
					StringProvider.LIMITED_NUM_OF_CRITERIA, 4));
		}

		params.add(new BasicNameValuePair("submitButtonCall_submitSearch",
				"Suchen"));
		params.add(new BasicNameValuePair("numberOfHits", "10"));

		String html = httpGet(
				opac_url + "/search.do?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING);
		return parse_search(html, 1);
	}

	public SearchRequestResult volumeSearch(Map<String, String> query)
			throws IOException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("methodToCall", "volumeSearch"));
		params.add(new BasicNameValuePair("dbIdentifier", query
				.get("dbIdentifier")));
		params.add(new BasicNameValuePair("catKey", query.get("catKey")));
		params.add(new BasicNameValuePair("periodical", "N"));
		String html = httpGet(
				opac_url + "/search.do?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING);
		return parse_search(html, 1);
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		if (!initialised)
			start();

		String html = httpGet(opac_url
				+ "/hitList.do?methodToCall=pos&identifier=" + identifier
				+ "&curPos=" + (((page - 1) * resultcount) + 1), ENCODING);
		return parse_search(html, page);
	}

	protected SearchRequestResult parse_search(String html, int page)
			throws OpacErrorException, IOException {
		Document doc = Jsoup.parse(html);

		if (doc.select("#RefineHitListForm").size() > 0) {
			// the results are located on a different page loaded via AJAX
			html = httpGet(
					opac_url + "/speedHitList.do?_="
							+ String.valueOf(System.currentTimeMillis() / 1000)
							+ "&hitlistindex=0&exclusionList=", ENCODING);
			doc = Jsoup.parse(html);
		}

		if (doc.select(".nodata").size() > 0) {
			return new SearchRequestResult(new ArrayList<SearchResult>(), 0, 1,
					1);
		}

		doc.setBaseUri(opac_url + "/searchfoo");

		int results_total = -1;

		String resultnumstr = doc.select(".box-header h2").first().text();
		if (resultnumstr.contains("(1/1)") || resultnumstr.contains(" 1/1")) {
			reusehtml = html;
			throw new OpacErrorException("is_a_redirect");
		} else if (resultnumstr.contains("(")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*\\(([0-9]+)\\).*", "$1"));
		} else if (resultnumstr.contains(": ")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*: ([0-9]+)$", "$1"));
		}

		Elements table = doc.select("table.data > tbody > tr");
		identifier = null;

		Elements links = doc.select("table.data a");
		boolean haslink = false;
		for (Element node : links) {
			if (node.hasAttr("href")
					& node.attr("href").contains("singleHit.do") && !haslink) {
				haslink = true;
				try {
					List<NameValuePair> anyurl = URLEncodedUtils.parse(
							new URI(node.attr("href").replace(" ", "%20")
									.replace("&amp;", "&")), ENCODING);
					for (NameValuePair nv : anyurl) {
						if (nv.getName().equals("identifier")) {
							identifier = nv.getValue();
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			if (tr.select(".icn, img[width=32]").size() > 0) {
				String[] fparts = tr.select(".icn, img[width=32]").first()
						.attr("src").split("/");
				String fname = fparts[fparts.length - 1];
				String changedFname = fname.toLowerCase(Locale.GERMAN)
						.replace(".jpg", "").replace(".gif", "")
						.replace(".png", "");

				// File names can look like this: "20_DVD_Video.gif"
				Pattern pattern = Pattern.compile("(\\d+)_.*");
				Matcher matcher = pattern.matcher(changedFname);
				if (matcher.find())
					changedFname = matcher.group(1);

				MediaType defaulttype = defaulttypes.get(changedFname);
				if (data.has("mediatypes")) {
					try {
						sr.setType(MediaType.valueOf(data.getJSONObject(
								"mediatypes").getString(fname)));
					} catch (JSONException e) {
						sr.setType(defaulttype);
					} catch (IllegalArgumentException e) {
						sr.setType(defaulttype);
					}
				} else {
					sr.setType(defaulttype);
				}
			}
			String title;
			String text;
			if (tr.select(".results table").size() > 0) { // e.g. RWTH Aachen
				title = tr.select(".title a").text();
				text = tr.select(".title div").text();
			} else { // e.g. Schaffhausen, BSB München
				title = tr.select(".title, .hitlistTitle").text();
				text = tr.select(".results, .hitlistMetadata").first()
						.ownText();
			}

			// we need to do some evil javascript parsing here to get the cover
			// and loan status of the item

			// get cover
			if (tr.select(".cover script").size() > 0) {
				String js = tr.select(".cover script").first().html();
				String isbn = matchJSVariable(js, "isbn");
				String ajaxUrl = matchJSVariable(js, "ajaxUrl");
				if (!"".equals(isbn) && !"".equals(ajaxUrl)) {
					String url = new URL(new URL(opac_url + "/"), ajaxUrl)
							.toString();
					String coverUrl = httpGet(url + "?isbn=" + isbn
							+ "&size=small", ENCODING);
					if (!"".equals(coverUrl))
						sr.setCover(coverUrl.replace("\r\n", "").trim());
				}
			}
			// get loan status and media ID
			if (tr.select("div[id^=loanstatus] + script").size() > 0) {
				String js = tr.select("div[id^=loanstatus] + script").first()
						.html();
				String[] variables = new String[] { "loanstateDBId",
						"itemIdentifier", "hitlistIdentifier",
						"hitlistPosition", "duplicateHitlistIdentifier",
						"itemType", "titleStatus", "typeofHit", "context" };
				String ajaxUrl = matchJSVariable(js, "ajaxUrl");
				if (!"".equals(ajaxUrl)) {
					JSONObject id = new JSONObject();
					List<NameValuePair> map = new ArrayList<NameValuePair>();
					for (String variable : variables) {
						String value = matchJSVariable(js, variable);
						if (!"".equals(value)) {
							map.add(new BasicNameValuePair(variable, value));
						}
						try {
							if (variable.equals("itemIdentifier")) {
								id.put("id", value);
							} else if (variable.equals("loanstateDBId")) {
								id.put("db", value);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					sr.setId(id.toString());
					String url = new URL(new URL(opac_url + "/"), ajaxUrl)
							.toString();
					String loanStatusHtml = httpGet(
							url + "?" + URLEncodedUtils.format(map, "UTF-8"),
							ENCODING).replace("\r\n", "").trim();
					Document loanStatusDoc = Jsoup.parse(loanStatusHtml);
					String loanstatus = loanStatusDoc.text()
							.replace("\u00bb", "").trim();

					if ((loanstatus.startsWith("entliehen")
							&& loanstatus.contains("keine Vormerkung möglich") || loanstatus
								.contains("Keine Exemplare verfügbar"))) {
						sr.setStatus(SearchResult.Status.RED);
					} else if (loanstatus.startsWith("entliehen")
							|| loanstatus.contains("andere Zweigstelle")) {
						sr.setStatus(SearchResult.Status.YELLOW);
					} else if ((loanstatus.startsWith("bestellbar") && !loanstatus
							.contains("nicht bestellbar"))
							|| (loanstatus.startsWith("vorbestellbar") && !loanstatus
									.contains("nicht vorbestellbar"))
							|| (loanstatus.startsWith("vorbestellbar") && !loanstatus
									.contains("nicht vorbestellbar"))
							|| (loanstatus.startsWith("vormerkbar") && !loanstatus
									.contains("nicht vormerkbar"))
							|| (loanstatus.contains("heute zurückgebucht"))
							|| (loanstatus.contains("ausleihbar") && !loanstatus
									.contains("nicht ausleihbar"))) {
						sr.setStatus(SearchResult.Status.GREEN);
					}
					if (sr.getType() != null) {
						if (sr.getType().equals(MediaType.EBOOK)
								|| sr.getType().equals(MediaType.EVIDEO)
								|| sr.getType().equals(MediaType.MP3))
							// Especially Onleihe.de ebooks are often marked
							// green though they are not available.
							sr.setStatus(SearchResult.Status.UNKNOWN);
					}
				}
			}

			StringBuilder description = new StringBuilder();
			description.append("<b>" + title + "</b><br/>");
			description.append(text);
			sr.setInnerhtml(description.toString());

			sr.setNr(10 * (page - 1) + i + 1);
			results.add(sr);
		}
		resultcount = results.size();
		return new SearchRequestResult(results, results_total, page);
	}

	private String matchJSVariable(String js, String varName) {
		Pattern pattern = Pattern.compile("var \\s*" + varName
				+ "\\s*=\\s*\"([^\"]*)\"\\s*;");
		Matcher matcher = pattern.matcher(js);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {

		if (id == null && reusehtml != null) {
			DetailledItem r = parse_result(reusehtml);
			reusehtml = null;
			return r;
		}
		String html;
		try {
			JSONObject json = new JSONObject(id);
			html = httpGet(
					opac_url
							+ "/perma.do?q="
							+ URLEncoder.encode("0=\"" + json.getString("id")
									+ "\" IN [" + json.getString("db") + "]",
									"UTF-8"), ENCODING);
		} catch (JSONException e) {
			// backwards compatibility
			html = httpGet(
					opac_url
							+ "/perma.do?q="
							+ URLEncoder.encode("0=\"" + id + "\" IN [2]",
									"UTF-8"), ENCODING);
		}

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		if (reusehtml != null) {
			return getResultById(null, null);
		}
		String html = httpGet(opac_url
				+ "/singleHit.do?methodToCall=showHit&curPos=" + nr
				+ "&identifier=" + identifier, ENCODING);
		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);

		DetailledItem result = new DetailledItem();

		if (doc.select("#cover script").size() > 0) {
			String js = doc.select("#cover script").first().html();
			String isbn = matchJSVariable(js, "isbn");
			String ajaxUrl = matchJSVariable(js, "ajaxUrl");
			if (!"".equals(isbn) && !"".equals(ajaxUrl)) {
				String url = new URL(new URL(opac_url + "/"), ajaxUrl)
						.toString();
				String coverUrl = httpGet(url + "?isbn=" + isbn
						+ "&size=medium", ENCODING);
				if (!"".equals(coverUrl))
					result.setCover(coverUrl.replace("\r\n", "").trim());
			}
		}

		result.setTitle(doc.select("h1").first().text());
		for (Element tr : doc.select(".titleinfo tr")) {
			// Sometimes there is one th and one td, sometimes two tds
			String detailName = tr.select("th, td").first().text().trim();
			String detailValue = tr.select("td").last().text().trim();
			result.addDetail(new Detail(detailName, detailValue));
			if (detailName.contains("ID in diesem Katalog")) {
				result.setId(detailValue);
			}
		}

		// Copies
		String copiesParameter = doc.select("div[id^=ajax_holdings_url")
				.attr("ajaxParameter").replace("&amp;", "");
		if (!"".equals(copiesParameter)) {
			String copiesHtml = httpGet(opac_url + "/" + copiesParameter,
					ENCODING);
			Document copiesDoc = Jsoup.parse(copiesHtml);
			List<String> table_keys = new ArrayList<String>();
			for (Element th : copiesDoc.select(".data tr th")) {
				if (th.text().contains("Zweigstelle"))
					table_keys.add(DetailledItem.KEY_COPY_BRANCH);
				else if (th.text().contains("Status"))
					table_keys.add(DetailledItem.KEY_COPY_STATUS);
				else if (th.text().contains("Signatur"))
					table_keys.add(DetailledItem.KEY_COPY_SHELFMARK);
				else
					table_keys.add(null);
			}
			for (Element tr : copiesDoc.select(".data tr:has(td)")) {
				Map<String, String> copy = new HashMap<String, String>();
				int i = 0;
				for (Element td : tr.select("td")) {
					if (table_keys.get(i) != null)
						copy.put(table_keys.get(i), td.text().trim());
					i++;
				}
				result.addCopy(copy);
			}
		}

		// TODO: Volumes

		try {
			Element isvolume = null;
			Map<String, String> volume = new HashMap<String, String>();
			Elements links = doc.select(".data td a");
			int elcount = links.size();
			for (int eli = 0; eli < elcount; eli++) {
				List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
						links.get(eli).attr("href")), "UTF-8");
				for (NameValuePair nv : anyurl) {
					if (nv.getName().equals("methodToCall")
							&& nv.getValue().equals("volumeSearch")) {
						isvolume = links.get(eli);
					} else if (nv.getName().equals("catKey")) {
						volume.put("catKey", nv.getValue());
					} else if (nv.getName().equals("dbIdentifier")) {
						volume.put("dbIdentifier", nv.getValue());
					}
				}
				if (isvolume != null) {
					volume.put("volume", "true");
					result.setVolumesearch(volume);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account acc,
			int useraction, String selection) throws IOException {
		return null;
	}

	@Override
	public ProlongResult prolong(String a, Account account, int useraction,
			String Selection) throws IOException {
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		return null;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, SocketException,
			OpacErrorException {
		return null;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return false;
	}

	@Override
	public boolean isAccountExtendable() {
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account acc)
			throws ClientProtocolException, SocketException, IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		try {
			try {
				JSONObject json = new JSONObject(id);
				return opac_url
						+ "/perma.do?q="
						+ URLEncoder.encode("0=\"" + json.getString("id")
								+ "\" IN [" + json.getString("db") + "]",
								"UTF-8");
			} catch (JSONException e) {
				// backwards compatibility
				return opac_url + "/perma.do?q="
						+ URLEncoder.encode("0=\"" + id + "\" IN [2]", "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int getSupportFlags() {
		int flags = SUPPORT_FLAG_CHANGE_ACCOUNT;
		flags |= SUPPORT_FLAG_ENDLESS_SCROLLING;
		return flags;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkAccountData(Account account) throws IOException,
			JSONException, OpacErrorException {
	}

	@Override
	public void setLanguage(String language) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<String> getSupportedLanguages() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
