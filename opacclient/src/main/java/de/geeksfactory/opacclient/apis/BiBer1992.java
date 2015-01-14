/**
 * Copyright (C) 2013 by Rüdiger Wurth under the MIT license:
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HTTPClient;
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
 * @author Ruediger Wurth, 16.02.2013 Web identification:
 *         "copyright 1992-2011 by BiBer GmbH"
 * 
 *         BiBer gestartet mit Stadtbibliothek Offenburg start URL:
 *         http://217.86.216.47/opac/de/qsim_frm.html.S
 * 
 *         open: issue #23: Basic support for library system "Biber" -> Essen
 *         issue #32: Integration of "BiBer" (copyright 2006) -> Karlsruhe
 *         https://opac.karlsruhe.de/ issue #33: Integration of "BiBer"
 *         (copyright 1992) -> Essen
 * 
 *         Features: In getResult(), mixed table layout is supported:
 *         column-wise and row-wise In getResult(), amazon bitmaps are supported
 * 
 *         Katalogsuche tested with
 * 
 *         Name Media amazon copy Media Branch Account type Bitmaps table types
 *         support images avail search
 *         --------------------------------------------------------------------
 *         BaWu/Friedrichshafen ok yes yes yes yes - BaWu/Lahr ok yes yes yes no
 *         - BaWu/Offenburg ok n/a no yes n/a yes Bay/Aschaffenburg ok n/a no
 *         yes n/a - Bay/Wuerzburg ok yes yes yes yes - NRW/Duisburg ok yes yes
 *         yes n/a - NRW/Erkrath n/a yes no yes not sup.yes NRW/Essen n/a n/a no
 *         yes not sup.- NRW/Gelsenkirchen ok yes yes yes yes - NRW/Hagen ok yes
 *         yes yes yes yes NRW/Herford n/a yes yes yes n/a - NRW/Luenen ok yes
 *         no yes n/a - NRW/MuelheimRuhr ok yes yes yes yes yes
 */
public class BiBer1992 extends BaseApi {

	private String m_opac_url = "";
	private String m_opac_dir = "opac"; // sometimes also "opax"
	private JSONObject m_data;
	private List<NameValuePair> m_nameValuePairs = new ArrayList<NameValuePair>(
			2);

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
	}

	// private int m_resultcount = 10;
	// private long logged_in;
	// private Account logged_in_as;

	// we have to limit num of results because PUSH attribute SHOW=20 does not
	// work:
	// number of results is always 50 which is too much
	final private int numOfResultsPerPage = 20;

	/*
	 * ----- media types ----- Example Wuerzburg: <td ...><input type="checkbox"
	 * name="MT" value="1" ...></td> <td ...><img src="../image/spacer.gif.S"
	 * title="Buch"><br>Buch</td>
	 * 
	 * Example Friedrichshafen: <td ...><input type="checkbox" name="MS"
	 * value="1" ...></td> <td ...><img src="../image/spacer.gif.S"
	 * title="Buch"><br>Buch</td>
	 * 
	 * Example Offenburg: <input type="radio" name="MT" checked
	 * value="MTYP0">Alles&nbsp;&nbsp; <input type="radio" name="MT"
	 * value="MTYP10">Belletristik&nbsp;&nbsp; Unfortunately Biber miss the end
	 * tag </input>, so opt.text() does not work! (at least Offenburg)
	 * 
	 * Example Essen, Aschaffenburg: <input type="radio" name="MT" checked
	 * value="MTYP0"><img src="../image/all.gif.S" title="Alles"> <input
	 * type="radio" name="MT" value="MTYP7"><img src="../image/cdrom.gif.S"
	 * title="CD-ROM">
	 * 
	 * ----- Branches ----- Example Essen,Erkrath: no closing </option> !!!
	 * cannot be parsed by Jsoup, so not supported <select name="AORT"> <option
	 * value="ZWST1">Altendorf </select>
	 * 
	 * Example Hagen, Würzburg, Friedrichshafen: <select name="ZW" class="sel1">
	 * <option selected value="ZWST0">Alle Bibliotheksorte</option> </select>
	 */
	@Override
	public List<SearchField> getSearchFields() throws IOException {
		List<SearchField> fields = new ArrayList<SearchField>();

		HttpGet httpget;
		if (m_opac_dir.equals("opax") || m_opac_dir.equals("opax13"))
			httpget = new HttpGet(m_opac_url + "/" + m_opac_dir
					+ "/de/qsel.html.S");
		else
			httpget = new HttpGet(m_opac_url + "/" + m_opac_dir
					+ "/de/qsel_main.S");

		HttpResponse response = http_client.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		Document doc = Jsoup.parse(html);

		// get text fields
		Elements text_opts = doc.select("form select[name=REG1] option");
		for (Element opt : text_opts) {
			TextSearchField field = new TextSearchField();
			field.setId(opt.attr("value"));
			field.setDisplayName(opt.text());
			field.setHint("");
			fields.add(field);
		}

		// get media types
		Elements mt_opts = doc.select("form input[name~=(MT|MS)]");
		if (mt_opts.size() > 0) {
			DropdownSearchField mtDropdown = new DropdownSearchField();
			mtDropdown.setId(mt_opts.get(0).attr("name"));
			mtDropdown.setDisplayName("Medientyp");
			List<Map<String, String>> mtOptions = new ArrayList<Map<String, String>>();
			for (Element opt : mt_opts) {
				if (!opt.val().equals("")) {
					String text = opt.text();
					if (text.length() == 0) {
						// text is empty, check layouts:
						// Essen: <input name="MT"><img title="mediatype">
						// Schaffenb: <input name="MT"><img alt="mediatype">
						Element img = opt.nextElementSibling();
						if (img != null && img.tagName().equals("img")) {
							text = img.attr("title");
							if (text.equals("")) {
								text = img.attr("alt");
							}
						}
					}
					if (text.length() == 0) {
						// text is still empty, check table layout, Example
						// Friedrichshafen
						// <td><input name="MT"></td> <td><img
						// title="mediatype"></td>
						Element td1 = opt.parent();
						Element td2 = td1.nextElementSibling();
						if (td2 != null) {
							Elements td2Children = td2.select("img[title]");
							if (td2Children.size() > 0) {
								text = td2Children.get(0).attr("title");
							}
						}
					}
					if (text.length() == 0) {
						// text is still empty: missing end tag like Offenburg
						text = parse_option_regex(opt);
					}
					Map<String, String> value = new HashMap<String, String>();
					value.put("key", opt.val());
					value.put("value", text);
					mtOptions.add(value);
				}
			}
			mtDropdown.setDropdownValues(mtOptions);
			fields.add(mtDropdown);
		}

		// get branches
		Elements br_opts = doc.select("form select[name=ZW] option");
		if (br_opts.size() > 0) {
			DropdownSearchField brDropdown = new DropdownSearchField();
			brDropdown.setId(br_opts.get(0).parent().attr("name"));
			brDropdown.setDisplayName(br_opts.get(0).parent().parent()
					.previousElementSibling().text().replace("\u00a0", "")
					.replace("?", "").trim());
			List<Map<String, String>> brOptions = new ArrayList<Map<String, String>>();
			for (Element opt : br_opts) {
				Map<String, String> value = new HashMap<String, String>();
				value.put("key", opt.val());
				value.put("value", opt.text());
				brOptions.add(value);
			}
			brDropdown.setDropdownValues(brOptions);
			fields.add(brDropdown);
		}

		return fields;
	}

	private void setMediaTypeFromImageFilename(SearchResult sr, String imagename) {
		String[] fparts1 = imagename.split("/"); // "images/31.gif.S"
		String[] fparts2 = fparts1[fparts1.length - 1].split("\\."); // "31.gif.S"
		String lookup = fparts2[0]; // "31"

		if (imagename.contains("amazon")) {
			sr.setCover(imagename);
		}
		
		if (m_data.has("mediatypes")) {
			try {
				String typeStr = m_data.getJSONObject("mediatypes").getString(
						lookup);
				sr.setType(MediaType.valueOf(typeStr));
			} catch (Exception e) {
				if (defaulttypes.containsKey(lookup)) {
					sr.setType(defaulttypes.get(lookup));
				}
			}
		} else {
			if (defaulttypes.containsKey(lookup)) {
				sr.setType(defaulttypes.get(lookup));
			}
		}
	}

	/*
	 * Parser for non XML compliant html part: (the crazy way) Get text from
	 * <input> without end tag </input>
	 * 
	 * Example Offenburg: <input type="radio" name="MT"
	 * value="MTYP10">Belletristik&nbsp;&nbsp; Regex1: value="MTYP10".*?>([^<]+)
	 */
	private String parse_option_regex(Element inputTag) {
		String optStr = inputTag.val();
		String html = inputTag.parent().html();
		String result = optStr;

		String regex1 = "value=\"" + optStr + "\".*?>([^<]+)";
		String[] regexList = new String[] { regex1 };

		for (String regex : regexList) {
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			if (matcher.find()) {
				result = matcher.group(1);
				result = result.replaceAll("&nbsp;", " ").trim();
				break;
			}
		}

		return result;
	}

	@Override
	public void init(Library lib) {
		super.init(lib);
		http_client = HTTPClient.getNewHttpClient(lib);

		m_data = lib.getData();

		try {
			m_opac_url = m_data.getString("baseurl");
			m_opac_dir = m_data.getString("opacdir");
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

	/*
	 * HTTP Push
	 */
	@Override
	public SearchRequestResult search(List<SearchQuery> queryList)
			throws IOException, NotReachableException {

		if (!initialised)
			start();

		m_nameValuePairs.clear();
		int count = 1;
		for (SearchQuery query : queryList) {
			if (query.getSearchField() instanceof TextSearchField
					&& !query.getValue().equals("")) {
				m_nameValuePairs.add(new BasicNameValuePair("CNN" + count,
						"AND"));
				m_nameValuePairs.add(new BasicNameValuePair("FLD" + count,
						query.getValue()));
				m_nameValuePairs.add(new BasicNameValuePair("REG" + count,
						query.getKey()));
				count++;
			} else if (query.getSearchField() instanceof DropdownSearchField) {
				m_nameValuePairs.add(new BasicNameValuePair(query.getKey(),
						query.getValue()));
			}
		}

		m_nameValuePairs.add(new BasicNameValuePair("FUNC", "qsel"));
		m_nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		m_nameValuePairs.add(new BasicNameValuePair("SHOW", "20")); // but
																	// result
																	// brings 50
		m_nameValuePairs.add(new BasicNameValuePair("SHOWSTAT", "N"));
		m_nameValuePairs.add(new BasicNameValuePair("FROMPOS", "1"));

		return searchGetPage(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.geeksfactory.opacclient.apis.OpacApi#searchGetPage(int)
	 */
	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {

		int startNum = (page - 1) * numOfResultsPerPage + 1;

		// remove last element = "FROMPOS", and add a new one
		m_nameValuePairs.remove(m_nameValuePairs.size() - 1);
		m_nameValuePairs.add(new BasicNameValuePair("FROMPOS", String
				.valueOf(startNum)));

		String html = httpPost(m_opac_url + "/" + m_opac_dir + "/query.C",
				new UrlEncodedFormEntity(m_nameValuePairs),
				getDefaultEncoding());
		return parse_search(html, page);
	}

	/*
	 * result table format: JSON "rows_per_hit" = 1: One <tr> per hit JSON
	 * "rows_per_hit" = 2: Two <tr> per hit (default) <form> <table> <tr
	 * valign="top"> <td class="td3" ...><a href=...><img ...></a></td> (row is
	 * optional, only in some bibs) <td class="td2" ...><input ...></td> <td
	 * width="34%">TITEL</td> <td width="34%">&nbsp;</td> <td width="6%"
	 * align="center">2009</td> <td width="*" align="left">DVD0 Seew</td> </tr>
	 * <tr valign="top"> <td class="td3" ...>&nbsp;...</td> <td class="td2"
	 * ...>&nbsp;...</td> <td colspan="4" ...><font size="-1"><font
	 * class="p1">Erwachsenenbibliothek</font></font><div
	 * class="hr4"></div></td> </tr>
	 */
	private SearchRequestResult parse_search(String html, int page) {
		List<SearchResult> results = new ArrayList<SearchResult>();
		Document doc = Jsoup.parse(html);

		if (doc.select("h3").text().contains("Es wurde nichts gefunden"))
			return new SearchRequestResult(results, 0, page);

		Elements trList = doc.select("form table tr[valign]"); // <tr
																// valign="top">
		Elements elem = null;
		int rows_per_hit = 2;
		if (trList.size() == 1
				|| (trList.size() > 1
						&& trList.get(0).select("input[type=checkbox]").size() > 0 && trList
						.get(1).select("input[type=checkbox]").size() > 0)) {
			rows_per_hit = 1;
		}

		try {
			int rows = m_data.getInt("rows_per_hit");
			rows_per_hit = rows;
		} catch (JSONException e) {
		}

		// Overall search results
		// are very differently layouted, but have always the text:
		// "....Treffer Gesamt (nnn)"
		int results_total;
		Pattern pattern = Pattern.compile("Treffer Gesamt \\(([0-9]+)\\)");
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			results_total = Integer.parseInt(matcher.group(1));
		} else {
			results_total = -1;
		}

		// limit to 20 entries
		int numOfEntries = trList.size() / rows_per_hit; // two rows per entry
		if (numOfEntries > numOfResultsPerPage)
			numOfEntries = numOfResultsPerPage;

		for (int i = 0; i < numOfEntries; i++) {
			Element tr = trList.get(i * rows_per_hit);
			SearchResult sr = new SearchResult();

			// ID as href tag
			elem = tr.select("td a");
			if (elem.size() > 0) {
				String hrefID = elem.get(0).attr("href");
				sr.setId(hrefID);
			} else {
				// no ID as href found, look for the ID in the input form
				elem = tr.select("td input");
				if (elem.size() > 0) {
					String nameID = elem.get(0).attr("name").trim();
					String hrefID = "/" + m_opac_dir
							+ "/ftitle.C?LANG=de&FUNC=full&" + nameID + "=YES";
					sr.setId(hrefID);
				}
			}

			// media type
			elem = tr.select("td img");
			if (elem.size() > 0) {
				setMediaTypeFromImageFilename(sr, elem.get(0).attr("src"));
			}

			// description
			String desc = "";
			try {
				// array "searchtable" list the column numbers of the
				// description
				JSONArray searchtable = m_data.getJSONArray("searchtable");
				for (int j = 0; j < searchtable.length(); j++) {
					int colNum = searchtable.getInt(j);
					if (j > 0)
						desc = desc + "<br />";
					desc = desc + tr.child(colNum).html();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// remove links "<a ...>...</a>
			// needed for Friedrichshafen: "Warenkorb", "Vormerkung"
			// Herford: "Medienkorb"
			desc = desc.replaceAll("<a .*?</a>", "");
			sr.setInnerhtml(desc);

			if (tr.select("font.p04x09b").size() > 0
					&& tr.select("font.p02x09b").size() == 0) {
				sr.setStatus(Status.GREEN);
			} else if (tr.select("font.p04x09b").size() == 0
					&& tr.select("font.p02x09b").size() > 0) {
				sr.setStatus(Status.RED);
			} else if (tr.select("font.p04x09b").size() > 0
					&& tr.select("font.p02x09b").size() > 0) {
				sr.setStatus(Status.YELLOW);
			}

			// number
			sr.setNr(i / rows_per_hit);
			results.add(sr);
		}

		// m_resultcount = results.size();
		return new SearchRequestResult(results, results_total, page);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.geeksfactory.opacclient.apis.OpacApi#getResultById(java.lang.String)
	 */
	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {
		if (!initialised)
			start();

		if (!id.contains("ftitle")) {
			id = "ftitle.C?LANG=de&FUNC=full&" + id + "=YES";
		}
		// normally full path like
		// "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
		// but sometimes (Wuerzburg) "ftitle.C?LANG=de&FUNC=full&331313252=YES"
		if (!id.startsWith("/")) {
			id = "/" + m_opac_dir + "/" + id;
		}

		HttpGet httpget = new HttpGet(m_opac_url + id);

		HttpResponse response = http_client.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.geeksfactory.opacclient.apis.OpacApi#getResult(int)
	 */
	@Override
	public DetailledItem getResult(int position) throws IOException {
		// not needed, normall all search results should have an ID,
		// so getResultById() is called
		return null;
	}

	/*
	 * Two-column table inside of a form 1st column is category, e.g.
	 * "Verfasser" 2nd column is content, e.g. "Bach, Johann Sebastian" In some
	 * rows, the 1st column is empty, then 2nd column is continued text from row
	 * above.
	 * 
	 * Some libraries have a second section for the copies in stock (Exemplare).
	 * This 2nd section has reverse layout.
	 * 
	 * |-------------------| | Subject | Content | |-------------------| |
	 * Subject | Content | |-------------------| | | Content |
	 * |-------------------| | Subject | Content |
	 * |-------------------------------------------------| | | Site | Signatur|
	 * ID | State | |-------------------------------------------------| | |
	 * Content | Content | Content | Content |
	 * |-------------------------------------------------|
	 */
	private DetailledItem parse_result(String html) {
		DetailledItem item = new DetailledItem();

		Document document = Jsoup.parse(html);

		Elements rows = document.select("html body form table tr");
		// Elements rows = document.select("html body div form table tr");

		// Element rowReverseSubject = null;
		Detail detail = null;

		// prepare copiestable
		Map<String, String> copy_last_content = null;
		int copy_row = 0;

		String[] copy_keys = new String[] { DetailledItem.KEY_COPY_BARCODE, // "barcode";
				DetailledItem.KEY_COPY_BRANCH, // "zst";
				DetailledItem.KEY_COPY_DEPARTMENT, // "abt";
				DetailledItem.KEY_COPY_LOCATION, // "ort";
				DetailledItem.KEY_COPY_STATUS, // "status";
				DetailledItem.KEY_COPY_RETURN, // "rueckgabe";
				DetailledItem.KEY_COPY_RESERVATIONS // "vorbestellt";
		};
		int[] copy_map = new int[] { 3, 1, -1, 1, 4, -1, -1 };

		try {
			JSONObject map = m_data.getJSONObject("copiestable");
			for (int i = 0; i < copy_keys.length; i++) {
				if (map.has(copy_keys[i]))
					copy_map[i] = map.getInt(copy_keys[i]);
			}
		} catch (Exception e) {
			// "copiestable" is optional
		}

		// go through all rows
		for (Element row : rows) {
			Elements columns = row.children();

			if (columns.size() == 2) {
				// HTML tag "&nbsp;" is encoded as 0xA0
				String firstColumn = columns.get(0).text()
						.replace("\u00a0", " ").trim();
				String secondColumn = columns.get(1).text()
						.replace("\u00a0", " ").trim();

				if (firstColumn.length() > 0) {
					// 1st column is category
					if (firstColumn.equalsIgnoreCase("titel")) {
						detail = null;
						item.setTitle(secondColumn);
					} else {

						if (secondColumn.contains("hier klicken")
								&& columns.get(1).select("a").size() > 0) {
							secondColumn += " "
									+ columns.get(1).select("a").first()
											.attr("href");
						}

						detail = new Detail(firstColumn, secondColumn);
						item.getDetails().add(detail);
					}
				} else {
					// 1st column is empty, so it is an extension to last
					// category
					if (detail != null) {
						String content = detail.getContent() + "\n"
								+ secondColumn;
						detail.setContent(content);
					} else {
						// detail==0, so it's the first row
						// check if there is an amazon image
						if (columns.get(0).select("a img[src]").size() > 0) {
							item.setCover(columns.get(0).select("a img")
									.first().attr("src"));
						}

					}
				}
			} else if (columns.size() > 3) {
				// This is the second section: the copies in stock ("Exemplare")
				// With reverse layout: first row is headline, skipped via
				// (copy_row > 0)
				if (copy_row > 0) {
					Map<String, String> e = new HashMap<String, String>();
					for (int j = 0; j < copy_keys.length; j++) {
						int col = copy_map[j];
						if (col > -1) {
							String text = "";
							if (copy_keys[j]
									.equals(DetailledItem.KEY_COPY_BRANCH)) {
								// for "Standort" only use ownText() to suppress
								// Link "Wegweiser"
								text = columns.get(col).ownText()
										.replace("\u00a0", " ").trim();
							}
							if (text.length() == 0) {
								// text of children
								text = columns.get(col).text()
										.replace("\u00a0", " ").trim();
							}
							if (text.length() == 0) {
								// empty table cell, take the one above
								// this is sometimes the case for "Standort"
								if (copy_keys[j]
										.equals(DetailledItem.KEY_COPY_STATUS)) {
									// but do it not for Status
									text = " ";
								} else {
									if (copy_last_content != null)
										text = copy_last_content
												.get(copy_keys[j]);
									else
										text = "";
								}
							}
							e.put(copy_keys[j], text);
						}
					}
					if (e.containsKey(DetailledItem.KEY_COPY_BRANCH)
							&& e.containsKey(DetailledItem.KEY_COPY_LOCATION)
							&& e.get(DetailledItem.KEY_COPY_LOCATION).equals(
									e.get(DetailledItem.KEY_COPY_BRANCH)))
						e.remove(DetailledItem.KEY_COPY_LOCATION);
					item.addCopy(e);
					copy_last_content = e;
				}// ignore 1st row
				copy_row++;

			}// if columns.size
		}// for rows

		item.setReservable(true); // We cannot check if media is reservable

		if (m_opac_dir.equals("opax")) {
			if (document.select("input[type=checkbox]").size() > 0) {
				item.setReservation_info(document
						.select("input[type=checkbox]").first().attr("name"));
			} else if (document.select("a[href^=reserv.C]").size() > 0) {
				String href = document.select("a[href^=reserv.C]").first()
						.attr("href");
				item.setReservation_info(href.substring(href.indexOf("resF_")));
			} else {
				item.setReservable(false);
			}
		} else {
			item.setReservation_info(document.select("input[name=ID]").attr(
					"value"));
		}
		return item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.geeksfactory.opacclient.apis.OpacApi#reservation(java.lang.String,
	 * de.geeksfactory.opacclient.objects.Account, int, java.lang.String)
	 */
	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		String resinfo = item.getReservation_info();
		if (selection == null) {
			// STEP 1: Check if reservable and select branch ("ID1")

			// Differences between opax and opac
			String func = m_opac_dir.equals("opax") ? "sigl" : "resF";
			String id = m_opac_dir.equals("opax") ? (resinfo.contains("resF") ? resinfo
					.substring(5) + "=" + resinfo
					: resinfo + "=resF_" + resinfo)
					: "ID=" + resinfo;

			String html = httpGet(m_opac_url + "/" + m_opac_dir
					+ "/reserv.C?LANG=de&FUNC=" + func + "&" + id,
					getDefaultEncoding());
			Document doc = Jsoup.parse(html);
			Elements optionsElements = doc.select("select[name=ID1] option");
			if (optionsElements.size() > 0) {
				Map<String, String> options = new HashMap<String, String>();
				for (Element option : optionsElements) {
					options.put(option.attr("value"), option.text());
				}
				if (options.size() > 1) {
					ReservationResult res = new ReservationResult(
							MultiStepResult.Status.SELECTION_NEEDED);
					res.setActionIdentifier(ReservationResult.ACTION_BRANCH);
					res.setSelection(options);
					return res;
				} else {
					return reservation(item, account, useraction, options
							.keySet().iterator().next());
				}
			} else {
				ReservationResult res = new ReservationResult(
						MultiStepResult.Status.ERROR);
				res.setMessage("Dieses Medium ist nicht reservierbar.");
				return res;
			}
		} else {
			// STEP 2: Reserve
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
			nameValuePairs.add(new BasicNameValuePair("BENUTZER", account
					.getName()));
			nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
					.getPassword()));
			nameValuePairs.add(new BasicNameValuePair("FUNC", "vors"));
			if (m_opac_dir.equals("opax"))
				nameValuePairs.add(new BasicNameValuePair(resinfo.replace(
						"resF_", ""), "vors"));
			nameValuePairs.add(new BasicNameValuePair("ID1", selection));

			String html = httpPost(m_opac_url + "/" + m_opac_dir
					+ "/setreserv.C", new UrlEncodedFormEntity(nameValuePairs),
					getDefaultEncoding());

			Document doc = Jsoup.parse(html);
			if (doc.select(".tab21 .p44b, .p2").text().contains("eingetragen")) {
				return new ReservationResult(MultiStepResult.Status.OK);
			} else {
				ReservationResult res = new ReservationResult(
						MultiStepResult.Status.ERROR);
				if (doc.select(".p1, .p22b").size() > 0)
					res.setMessage(doc.select(".p1, .p22b").text());
				return res;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.geeksfactory.opacclient.apis.OpacApi#prolong(de.geeksfactory.opacclient
	 * .objects.Account, java.lang.String)
	 * 
	 * Offenburg, prolong negative result: <table border="1" width="100%"> <tr>
	 * <th ...>Nr</th> <th ...>Signatur / Kurztitel</th> <th
	 * ...>F&auml;llig</th> <th ...>Status</th> </tr> <tr> <td
	 * ...>101103778</td> <td ...>Hyde / Hyde, Anthony: Der Mann aus </td> <td
	 * ...>09.04.2013</td> <td ...><font class="p1">verl&auml;ngerbar ab
	 * 03.04.13, nicht verl&auml;ngert</font> <br>Bitte wenden Sie sich an Ihre
	 * Bibliothek!</td> </tr> </table>
	 * 
	 * Offenburg, prolong positive result: TO BE DESCRIBED
	 */
	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String Selection) throws IOException {

		String command;

		// prolong media via http POST
		// Offenburg: URL is .../opac/verl.C
		// Hagen: URL is .../opax/renewmedia.C
		if (m_opac_dir.equals("opax")) {
			command = "/renewmedia.C";
		} else {
			command = "/verl.C";
		}

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair(media, "YES"));
		nameValuePairs
				.add(new BasicNameValuePair("BENUTZER", account.getName()));
		nameValuePairs.add(new BasicNameValuePair("FUNC", "verl"));
		nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
				.getPassword()));

		String html = httpPost(m_opac_url + "/" + m_opac_dir + command,
				new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
		if (html.contains("no such key")) {
			html = httpPost(
					m_opac_url + "/" + m_opac_dir + command.replace(".C", ".S"),
					new UrlEncodedFormEntity(nameValuePairs),
					getDefaultEncoding());
		}

		Document doc = Jsoup.parse(html);

		// Check result:
		// First we look for a cell with text "Status"
		// and store the column number
		// Then we look in the rows below at this column if
		// we find any text. Stop at first text we find.
		// This text must start with "verl�ngert"
		Elements rowElements = doc.select("table tr");

		int statusCol = -1; // Status column not yet found

		// rows loop
		for (int i = 0; i < rowElements.size(); i++) {
			Element tr = rowElements.get(i);
			Elements tdList = tr.children(); // <th> or <td>

			// columns loop
			for (int j = 0; j < tdList.size(); j++) {
				String cellText = tdList.get(j).text().trim();

				if (statusCol < 0) {
					// we look for cell with text "Status"
					if (cellText.equals("Status")) {
						statusCol = j;
						break; // next row
					}
				} else {
					// we look only at Status column
					// In "Hagen", there are some extra empty rows below
					if ((j == statusCol) && (cellText.length() > 0)) {
						// Status found
						if (cellText.matches("verl.ngert.*")) {
							return new ProlongResult(MultiStepResult.Status.OK);
						} else {
							return new ProlongResult(
									MultiStepResult.Status.ERROR, cellText);
						}
					}
				}
			}// for columns
		}// for rows

		return new ProlongResult(MultiStepResult.Status.ERROR, "unknown result");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.geeksfactory.opacclient.apis.OpacApi#cancel(de.geeksfactory.opacclient
	 * .objects.Account, java.lang.String)
	 */
	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		nameValuePairs.add(new BasicNameValuePair("FUNC", "vorl"));
		if (m_opac_dir.equals("opax")) {
			nameValuePairs.add(new BasicNameValuePair("BENUTZER", account
					.getName()));
			nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
					.getPassword()));
		}
		nameValuePairs.add(new BasicNameValuePair(media, "YES"));

		String action = m_opac_dir.equals("opax") ? "/delreserv.C" : "/vorml.C";

		String html = httpPost(m_opac_url + "/" + m_opac_dir + action,
				new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());

		Document doc = Jsoup.parse(html);
		if (doc.select(".tab21 .p44b, .p2").text().contains("Vormerkung wurde")) {
			return new CancelResult(MultiStepResult.Status.OK);
		} else {
			return new CancelResult(MultiStepResult.Status.ERROR);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.geeksfactory.opacclient.apis.OpacApi#account(de.geeksfactory.opacclient
	 * .objects.Account)
	 * 
	 * POST-format: BENUTZER xxxxxxxxx FUNC medk LANG de PASSWORD ddmmyyyy
	 */
	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {

		AccountData res = new AccountData(account.getId());

		// get media
		List<Map<String, String>> medien = accountGetMedia(account, res);
		res.setLent(medien);

		// get reservations
		List<Map<String, String>> reservations = accountGetReservations(account);
		res.setReservations(reservations);

		return res;
	}

	private List<Map<String, String>> accountGetMedia(Account account,
			AccountData res) throws IOException, JSONException,
			OpacErrorException {

		List<Map<String, String>> medien = new ArrayList<Map<String, String>>();

		// get media list via http POST
		Document doc = accountHttpPost(account, "medk");
		if (doc == null) {
			return medien;
		}

		// parse result list
		JSONObject copymap = m_data.getJSONObject("accounttable");

		Pattern expire = Pattern.compile("Ausweisg.ltigkeit: ([0-9.]+)");
		Pattern fees = Pattern.compile("([0-9,.]+) .");
		for (Element td : doc.select(".td01x09n")) {
			String text = td.text().trim();
			if (expire.matcher(text).matches()) {
				res.setValidUntil(expire.matcher(text).replaceAll("$1"));
			} else if (fees.matcher(text).matches()) {
				res.setPendingFees(text);
			}
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		Elements rowElements = doc.select("form[name=medkl] table tr");

		// rows: skip 1st row -> title row
		for (int i = 1; i < rowElements.size(); i++) {
			Element tr = rowElements.get(i);
			if (tr.child(0).tagName().equals("th"))
				continue;
			Map<String, String> e = new HashMap<String, String>();

			Pattern itemIdPat = Pattern
					.compile("javascript:smAcc\\('[a-z]+','[a-z]+','([A-Za-z0-9]+)'\\)");
			// columns: all elements of one media
			Iterator<?> keys = copymap.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				int index;
				try {
					index = copymap.has(key) ? copymap.getInt(key) : -1;
				} catch (JSONException e1) {
					index = -1;
				}
				if (index >= 0) {
					String value = tr.child(index).text().trim();

					// Signature, Author and Title is the same field:
					// "autor: title"
					// sometimes there is no ":" then only the title is given
					if (key.equals(AccountData.KEY_LENT_AUTHOR)) {
						if (value.contains(":")) {
							// Autor: remove everything starting at ":"
							value = value.replaceFirst("^[^:]*/", "").trim();
							value = value.replaceFirst("\\:.*", "").trim();
						} else {
							// no Autor given<
							value = "";
						}
					} else if (key.equals(AccountData.KEY_LENT_TITLE)) {
						if (value.contains(":")) {
							// Title: remove everything up to ":"
							value = value.replaceFirst(".*\\:", "").trim();
							value = value.replaceFirst("^(.*)/[^/]*$", "$1")
									.trim();
						} else {
							// Remove everything except the signature
							value = value.replaceFirst("^[^/]*/([^/]*)/[^/]*$",
									"$1").trim();
							value = value.replaceFirst("^[^/]*/([^/]*)$", "$1")
									.trim();
						}
					}

					if (tr.child(index).select("a").size() == 1) {
						Matcher matcher = itemIdPat.matcher(tr.child(index)
								.select("a").attr("href"));
						if (matcher.find()) {
							e.put(AccountData.KEY_LENT_ID, matcher.group(1));
						}
					}

					if (value.length() != 0) {
						e.put(key, value);
					}
				}
			}

			if (tr.select("input[type=checkbox][value=YES]").size() > 0)
				e.put(AccountData.KEY_LENT_LINK,
						tr.select("input[type=checkbox][value=YES]").attr(
								"name"));

			// calculate lent timestamp for notification purpose
			if (e.containsKey(AccountData.KEY_LENT_DEADLINE)) {
				try {
					e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
							.valueOf(sdf.parse(
									e.get(AccountData.KEY_LENT_DEADLINE))
									.getTime()));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}

			medien.add(e);
		}

		return medien;
	}

	private List<Map<String, String>> accountGetReservations(Account account)
			throws IOException, JSONException, OpacErrorException {

		List<Map<String, String>> reservations = new ArrayList<Map<String, String>>();

		if (!m_data.has("reservationtable")) {
			// reservations not specifically supported, let's just try it
			// with default values but fail silently
			JSONObject restblobj = new JSONObject();
			restblobj.put("author", 3);
			restblobj.put("availability", 6);
			restblobj.put("branch", -1);
			restblobj.put("cancelurl", -1);
			restblobj.put("expirationdate", 5);
			restblobj.put("title", 3);
			m_data.put("reservationtable", restblobj);
			try {
				return accountGetReservations(account);
			} catch (Exception e) {
				return reservations;
			}
		}

		// get reservations list via http POST
		Document doc = accountHttpPost(account, "vorm");
		if (doc == null) {
			// error message as html result
			return reservations;
		}

		// parse result list
		JSONObject copymap = m_data.getJSONObject("reservationtable");
		Elements rowElements = doc.select("form[name=vorml] table tr");

		// rows: skip 1st row -> title row
		for (int i = 1; i < rowElements.size(); i++) {
			Element tr = rowElements.get(i);
			if (tr.child(0).tagName().equals("th"))
				continue;
			Map<String, String> e = new HashMap<String, String>();

			e.put(AccountData.KEY_RESERVATION_CANCEL,
					tr.select("input[type=checkbox]").attr("name"));

			// columns: all elements of one media
			Iterator<?> keys = copymap.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				int index = copymap.getInt(key);
				if (index >= 0) {
					String value = tr.child(index).text();

					// Author and Title is the same field: "autor: title"
					// sometimes there is no ":" then only the title is given
					if (key.equals(AccountData.KEY_LENT_AUTHOR)) {
						if (value.contains(":")) {
							// Autor: remove everything starting at ":"
							value = value.replaceFirst("^[^:]*/", "").trim();
							value = value.replaceFirst("\\:.*", "").trim();
						} else {
							// no Autor given<
							value = "";
						}
					} else if (key.equals(AccountData.KEY_LENT_TITLE)) {
						if (value.contains(":")) {
							// Title: remove everything up to ":"
							value = value.replaceFirst(".*\\:", "").trim();
							value = value.replaceFirst("^(.*)/[^/]*$", "$1")
									.trim();
						} else {
							// Remove everything except the signature
							value = value.replaceFirst("^[^/]*/([^/]*)/[^/]*$",
									"$1").trim();
							value = value.replaceFirst("^[^/]*/([^/]*)$", "$1")
									.trim();
						}
					}

					if (value.length() != 0) {
						e.put(key, value);
					}
				}
			}
			reservations.add(e);
		}

		return reservations;
	}

	private Document accountHttpPost(Account account, String func)
			throws IOException, OpacErrorException {
		// get media list via http POST
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("FUNC", func));
		nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		nameValuePairs
				.add(new BasicNameValuePair("BENUTZER", account.getName()));
		nameValuePairs.add(new BasicNameValuePair("PASSWORD", account
				.getPassword()));

		String html = httpPost(m_opac_url + "/" + m_opac_dir + "/user.C",
				new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());

		Document doc = Jsoup.parse(html);

		// Error recognition
		// <title>OPAC Fehler</title>
		if (doc.title().contains("Fehler")
				|| (doc.select("h2").size() > 0 && doc.select("h2").text()
						.contains("Fehler"))) {
			String errText = "unknown error";
			Elements elTable = doc.select("table");
			if (elTable.size() > 0) {
				errText = elTable.get(0).text();
			}
			throw new OpacErrorException(errText);
		}
		if (doc.select("tr td font[color=red]").size() == 1) {
			throw new OpacErrorException(doc.select("font[color=red]").text());
		}
		if (doc.text().contains("No html file set")
				|| doc.text().contains(
						"Der BIBDIA Server konnte den Auftrag nicht")) {
			throw new OpacErrorException(
					stringProvider.getString(StringProvider.WRONG_LOGIN_DATA));
		}

		return doc;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return !library.getData().isNull("accounttable");
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
		// id is normally full path like
		// "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
		// but sometimes (Wuerzburg) "ftitle.C?LANG=de&FUNC=full&331313252=YES"
		if (!id.startsWith("/")) {
			id = "/" + m_opac_dir + "/" + id;
		}

		return m_opac_url + id;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkAccountData(Account account) throws IOException,
			JSONException, OpacErrorException {
		Document doc = accountHttpPost(account, "medk");
		if (doc == null)
			throw new NotReachableException();
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
