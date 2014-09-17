package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.geeksfactory.opacclient.Base64;
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
import de.geeksfactory.opacclient.objects.SearchResult.Status;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * 
 * @author Johan von Forstner, 11.08.2014
 * 
 *         WinBIAP, Version 4.1.0 gestartet mit Bibliothek Unterföhring
 * 
 *         Unterstützt bisher nur Katalogsuche
 * 
 *         Example for a search query (parameter "data" in the URL, everything
 *         before the hyphen, base64 decoded, added formatting) as seen in
 *         Unterföhring:
 * 
 *        cmd=5&amp;				perform a search
 *         sC=
 *         		c_0=1%%				unknown
 *         		m_0=1%%				unknown
 *         		f_0=2%%				free search
 *         		o_0=8%%				contains
 *         		v_0=schule			"schule"
 *         ++
 *         		c_1=1%%				unknown
 *         		m_1=1%%				unknown
 *         		f_1=3%%				author
 *         		o_1=8%%				contains
 *         		v_1=rowling			"rowling"
 *         ++
 *         		c_2=1%%				unknown
 *         		m_2=1%%				unknown
 *         		f_2=12%%			title
 *         		o_2=8%%				contains
 *         		v_2=potter			"potter"
 *         ++
 *         		c_3=1%%				unknown
 *         		m_3=1%%				unknown
 *         		f_3=34%%			year
 *         		o_3=6%%				newer or equal to
 *         		v_3=2000			"2000"
 *         ++
 *         		c_4=1%%				unknown
 *         		m_4=1%%				unknown
 *         		f_4=34%%			year
 *         		o_4=4%%				older or equal to
 *         		v_4=2014			"2014"
 *         ++
 *         		c_5=1%%				unknown
 *         		m_5=1%%				unknown
 *         		f_5=42%%			media category
 *         		o_5=1%%				is equal to
 *         		v_5=3				"Kinder- und Jugendbücher"
 *         ++
 *         		c_6=1%%				unknown
 *         		m_6=1%%				unknown
 *         		f_6=48%%			location
 *         		o_6=1%%				is equal to
 *         		v_6=1				"Bibliothek Unterföhring"
 *         ++
 *         		c_7=3%%				unknown (now changed to 3)	-
 *         		m_7=1%%				unknown						|	  This group has no
 *         		f_7=45%%			unknown						|---  effect on the result
 *         		o_7=1%%				unknown						|	  and can be left out
 *         		v_7=5|4|101|102		unknown						-
 *         
 *     &amp;Sort=Autor				Sort by Author (default)
 * 
 */

public class WinBiap extends BaseApiCompat implements OpacApi {

	protected String opac_url = "";
	protected MetaDataSource metadata;
	protected JSONObject data;
	protected Library library;
	protected List<List<NameValuePair>> query;

	@Override
	public void start() throws IOException, NotReachableException {
		try {
			metadata.open();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			extract_meta();
		} else {
			metadata.close();
		}
	}

	public void extract_meta() {
		String html;
		try {
			html = httpGet(opac_url + "/search.aspx", getDefaultEncoding());
			Document doc = Jsoup.parse(html);
			Elements mediaGroupOptions = doc
					.select("#ctl00_ContentPlaceHolderMain_searchPanel_ListBoxMediagroups_ListBoxMultiselection option");
			Elements branchOptions = doc
					.select("#ctl00_ContentPlaceHolderMain_searchPanel_MultiSelectionBranch_ListBoxMultiselection option");
			try {
				metadata.open();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			metadata.clearMeta(library.getIdent());

			for (Element option : mediaGroupOptions) {
				metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
						library.getIdent(), option.attr("value"), option.text());
			}
			for (Element option : branchOptions) {
				metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
						library.getIdent(), option.attr("value"), option.text());
			}
			metadata.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void init(MetaDataSource metadata, Library lib) {
		super.init(metadata, lib);
		this.metadata = metadata;
		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * For documentation of the parameters, @see
	 * {@link #addParametersManual(String, String, String, String, String, List, int)}
	 */
	protected int addParameters(Map<String, String> query, String key,
			String searchkey, String type, List<List<NameValuePair>> params,
			int index) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return index;

		List<NameValuePair> list = new ArrayList<NameValuePair>();
		if (data.optBoolean("longParameterNames")) {
			// A few libraries use longer names for the parameters
			// (e.g. Hohen Neuendorf)
			list.add(new BasicNameValuePair("Combination_" + index, "1"));
			list.add(new BasicNameValuePair("Mode_" + index, "1"));
			list.add(new BasicNameValuePair("Searchfield_" + index, searchkey));
			list.add(new BasicNameValuePair("Searchoperator_" + index, type));
			list.add(new BasicNameValuePair("Searchvalue_" + index, query
					.get(key)));
		} else {
			list.add(new BasicNameValuePair("c_" + index, "1"));
			list.add(new BasicNameValuePair("m_" + index, "1"));
			list.add(new BasicNameValuePair("f_" + index, searchkey));
			list.add(new BasicNameValuePair("o_" + index, type));
			list.add(new BasicNameValuePair("v_" + index, query.get(key)));
		}
		params.add(list);
		return index + 1;

	}

	protected static final String QUERY_TYPE_CONTAINS = "8";
	protected static final String QUERY_TYPE_FROM = "6";
	protected static final String QUERY_TYPE_TO = "4";
	protected static final String QUERY_TYPE_STARTS_WITH = "7";
	protected static final String QUERY_TYPE_EQUALS = "1";

	/**
	 * @param c
	 *            "Combination" (probably And, Or, ...): Meaning unknown, seems
	 *            to always be "1" except in some mysterious queries the website
	 *            adds every time that don't change the result
	 * @param m
	 *            "Mode": Meaning unknown, seems to always be "1" except in some
	 *            mysterious queries the website adds every time that don't
	 *            change the result
	 * @param f
	 *            "Field": The key for the property that is queried, for example
	 *            "12" for "title"
	 * @param o
	 *            "Operator": The type of search that is made (one of the
	 *            QUERY_TYPE_ constants above), for example "8" for "contains"
	 * @param v
	 *            "Value": The value that was input by the user
	 */
	protected int addParametersManual(String combination, String mode,
			String field, String operator, String value,
			List<List<NameValuePair>> params, int index) {
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		if (data.optBoolean("longParameterNames")) {
			// A few libraries use longer names for the parameters
			// (e.g. Hohen Neuendorf)
			list.add(new BasicNameValuePair("Combination_" + index, combination));
			list.add(new BasicNameValuePair("Mode_" + index, mode));
			list.add(new BasicNameValuePair("Searchfield_" + index, field));
			list.add(new BasicNameValuePair("Searchoperator_" + index, operator));
			list.add(new BasicNameValuePair("Searchvalue_" + index, value));
		} else {
			list.add(new BasicNameValuePair("c_" + index, combination));
			list.add(new BasicNameValuePair("m_" + index, mode));
			list.add(new BasicNameValuePair("f_" + index, field));
			list.add(new BasicNameValuePair("o_" + index, operator));
			list.add(new BasicNameValuePair("v_" + index, value));
		}
		params.add(list);
		return index + 1;
	}

	@Override
	public SearchRequestResult search(Map<String, String> query)
			throws IOException, NotReachableException, OpacErrorException {
		List<List<NameValuePair>> queryParams = new ArrayList<List<NameValuePair>>();

		int index = 0;
		index = addParameters(query, KEY_SEARCH_QUERY_FREE,
				data.optString("KEY_SEARCH_QUERY_FREE", "2"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR,
				data.optString("KEY_SEARCH_QUERY_AUTHOR", "3"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_TITLE,
				data.optString("KEY_SEARCH_QUERY_TITLE", "12"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA,
				data.optString("KEY_SEARCH_QUERY_KEYWORDA", "24"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUDIENCE,
				data.optString("KEY_SEARCH_QUERY_AUDIENCE", "25"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM,
				data.optString("KEY_SEARCH_QUERY_SYSTEM", "26"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_ISBN,
				data.optString("KEY_SEARCH_QUERY_ISBN", "29"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER,
				data.optString("KEY_SEARCH_QUERY_PUBLISHER", "32"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_BARCODE,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "46"),
				QUERY_TYPE_CONTAINS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_START,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "34"),
				QUERY_TYPE_FROM, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_END,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "34"),
				QUERY_TYPE_TO, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_CATEGORY,
				data.optString("KEY_SEARCH_QUERY_CATEGORY", "42"),
				QUERY_TYPE_EQUALS, queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_BRANCH,
				data.optString("KEY_SEARCH_QUERY_BRANCH", "48"),
				QUERY_TYPE_EQUALS, queryParams, index);

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}
		// if (index > 4) {
		// throw new OpacErrorException(
		// "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.");
		// }

		this.query = queryParams;
		String encodedQueryParams = encode(queryParams, "=", "%%", "++");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		start();
		params.add(new BasicNameValuePair("cmd", "5"));
		if (data.optBoolean("longParameterNames"))
			// A few libraries use longer names for the parameters
			// (e.g. Hohen Neuendorf)
			params.add(new BasicNameValuePair("searchConditions",
					encodedQueryParams));
		else
			params.add(new BasicNameValuePair("sC", encodedQueryParams));
		params.add(new BasicNameValuePair("Sort", "Autor"));

		String text = encode(params, "=", "&amp;");
		String base64 = URLEncoder.encode(
				Base64.encodeBytes(text.getBytes("UTF-8")), "UTF-8");

		String html = httpGet(opac_url + "/search.aspx?data=" + base64,
				getDefaultEncoding(), false);
		return parse_search(html, 1);
	}

	private SearchRequestResult parse_search(String html, int page)
			throws OpacErrorException, IOException {
		Document doc = Jsoup.parse(html);

		if (doc.select(".alert h4").text().contains("Keine Treffer gefunden")) {
			// no media found
			return new SearchRequestResult(new ArrayList<SearchResult>(), 0,
					page);
		}
		if (doc.select("errortype").size() > 0) {
			// Error (e.g. 404)
			throw new OpacErrorException(doc.select("errortype").text());
		}

		// Total count
		String header = doc.select(".ResultHeader").text();
		Pattern pattern = Pattern.compile("Die Suche ergab (\\d*) Treffer");
		Matcher matcher = pattern.matcher(header);
		int results_total = 0;
		if (matcher.find()) {
			results_total = Integer.parseInt(matcher.group(1));
		} else {
			throw new OpacErrorException("Fehler bei der Suche");
		}

		// Results
		Elements trs = doc.select("#listview .ResultItem");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (Element tr : trs) {
			SearchResult sr = new SearchResult();
			String author = tr.select(".autor").text();
			String title = tr.select(".title").text();
			String titleAddition = tr.select(".titleZusatz").text();
			String desc = tr.select(".smallDescription").text();
			sr.setInnerhtml("<b>"
					+ (author.equals("") ? "" : author + "<br />")
					+ title
					+ (titleAddition.equals("") ? "" : " - <i>" + titleAddition
							+ "</i>") + "</b><br /><small>" + desc + "</small>");

			String coverUrl = tr.select(".coverWrapper input").attr("src");
			if (!coverUrl.contains("leer.gif"))
				sr.setCover(coverUrl);

			String link = tr.select("a[href^=detail.aspx]").attr("href");
			String base64 = getQueryParamsFirst(link).get("data");
			if (base64.contains("-")) // Most of the time, the base64 string is
										// followed by a hyphen and some
										// mysterious
										// letters that we don't want
				base64 = base64.substring(0, base64.indexOf("-") - 1);
			String decoded = new String(Base64.decode(base64), "UTF-8");
			pattern = Pattern.compile("CatalogueId=(\\d*)");
			matcher = pattern.matcher(decoded);
			if (matcher.find()) {
				sr.setId(matcher.group(1));
			} else {
				throw new OpacErrorException("Fehler beim Erkennen eines Links");
			}

			if (tr.select(".mediaStatus").size() > 0) {
				Element status = tr.select(".mediaStatus").first();
				if (status.hasClass("StatusNotAvailable")) {
					sr.setStatus(Status.RED);
				} else if (status.hasClass("StatusAvailable")) {
					sr.setStatus(Status.GREEN);
				} else {
					sr.setStatus(Status.YELLOW);
				}
			} else if (tr.select(".showCopies").size() > 0) { // Multiple copies
				if (tr.nextElementSibling().select(".StatusNotAvailable")
						.size() == 0) {
					sr.setStatus(Status.GREEN);
				} else if (tr.nextElementSibling().select(".StatusAvailable")
						.size() == 0) {
					sr.setStatus(Status.RED);
				} else {
					sr.setStatus(Status.YELLOW);
				}
			}

			results.add(sr);
		}
		return new SearchRequestResult(results, results_total, page);
	}

	private String encode(List<List<NameValuePair>> list, String equals,
			String separator, String separator2) {
		if (list.size() > 0) {
			String encoded = encode(list.get(0), equals, separator);
			for (int i = 1; i < list.size(); i++) {
				encoded += separator2;
				encoded += encode(list.get(i), equals, separator);
			}
			return encoded;
		} else {
			return "";
		}
	}

	private String encode(List<NameValuePair> list, String equals,
			String separator) {
		if (list.size() > 0) {
			String encoded = list.get(0).getName() + equals
					+ list.get(0).getValue();
			for (int i = 1; i < list.size(); i++) {
				encoded += separator;
				encoded += list.get(i).getName() + equals
						+ list.get(i).getValue();
			}
			return encoded;
		} else {
			return "";
		}
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException, OpacErrorException {
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		String encodedQueryParams = encode(query, "=", "%%", "++");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("cmd", "1"));
		if (data.optBoolean("longParameterNames")) {
			// A few libraries use longer names for the parameters
			// (e.g. Hohen Neuendorf)
			params.add(new BasicNameValuePair("searchConditions",
					encodedQueryParams));
			params.add(new BasicNameValuePair("PageIndex", String
					.valueOf(page - 1)));
		} else {
			params.add(new BasicNameValuePair("sC", encodedQueryParams));
			params.add(new BasicNameValuePair("pI", String.valueOf(page - 1)));
		}
		params.add(new BasicNameValuePair("Sort", "Autor"));

		String text = encode(params, "=", "&amp;");
		String base64 = URLEncoder.encode(
				Base64.encodeBytes(text.getBytes("UTF-8")), "UTF-8");

		String html = httpGet(opac_url + "/search.aspx?data=" + base64,
				getDefaultEncoding(), false);
		return parse_search(html, page);
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {
		String html = httpGet(opac_url + "/detail.aspx?Id=" + id,
				getDefaultEncoding(), false);
		return parse_result(html);
	}

	private DetailledItem parse_result(String html) {
		Document doc = Jsoup.parse(html);
		DetailledItem item = new DetailledItem();

		item.setCover(doc.select(".cover").attr("src"));
		String permalink = doc.select(".PermalinkTextarea").text();
		item.setId(getQueryParamsFirst(permalink).get("Id"));

		Elements trs = doc.select(".DetailInformation").first().select("tr");
		for (Element tr : trs) {
			String name = tr.select(".DetailInformationEntryName").text()
					.replace(":", "");
			String value = tr.select(".DetailInformationEntryContent").text();
			if (name.equals("Titel")) {
				item.setTitle(value);
			} else if (name.equals("Stücktitel")) {
				item.setTitle(item.getTitle() + " " + value);
			} else {
				item.addDetail(new Detail(name, value));
			}
		}

		trs = doc.select(".detailCopies .tableCopies tr:not(.headerCopies)");
		for (Element tr : trs) {
			Map<String, String> copy = new HashMap<String, String>();
			copy.put(DetailledItem.KEY_COPY_BARCODE, tr.select(".mediaBarcode")
					.text().replace("#", ""));
			copy.put(DetailledItem.KEY_COPY_STATUS, tr.select(".mediaStatus")
					.text());
			if (tr.select(".mediaBranch").size() > 0)
				copy.put(DetailledItem.KEY_COPY_BRANCH,
						tr.select(".mediaBranch").text());
			copy.put(DetailledItem.KEY_COPY_LOCATION,
					tr.select(".cellMediaItemLocation span").text());
			item.addCopy(copy);
		}

		return item;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		// Should not be called because every media has an ID
		return null;
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		return null;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		return null;
	}

	@Override
	public String[] getSearchFieldsCompat() {
		return new String[] { KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_AUDIENCE, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_PUBLISHER,
				KEY_SEARCH_QUERY_BARCODE, KEY_SEARCH_QUERY_YEAR_RANGE_START,
				KEY_SEARCH_QUERY_YEAR_RANGE_END, KEY_SEARCH_QUERY_BRANCH,
				KEY_SEARCH_QUERY_CATEGORY };
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
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		return opac_url + "/detail.aspx?Id=" + id;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING;
	}

}
