package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import android.util.Base64;
import android.util.Log;
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
 * 
 *         Unterstützt bisher nur Katalogsuche
 * 
 */

public class WinBiap extends BaseApi implements OpacApi {

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

	protected static final String CONTAINS = "8";
	protected static final String FROM = "6";
	protected static final String TO = "4";
	protected static final String STARTS_WITH = "7";
	protected static final String EQUALS = "1";

	protected int addParameters(Map<String, String> query, String key,
			String searchkey, String type, List<List<NameValuePair>> params,
			int index) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return index;

		List<NameValuePair> list = new ArrayList<NameValuePair>();
		list.add(new BasicNameValuePair("c_" + index, "1")); // ?
		list.add(new BasicNameValuePair("m_" + index, "1")); // ?
		list.add(new BasicNameValuePair("f_" + index, searchkey)); // query
																	// property
		list.add(new BasicNameValuePair("o_" + index, type)); // query type
		list.add(new BasicNameValuePair("v_" + index, query.get(key))); // value
		params.add(list);
		return index + 1;

	}

	protected int addParametersManual(String c, String m, String f, String o,
			String v, List<List<NameValuePair>> params, int index) {
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		list.add(new BasicNameValuePair("c_" + index, c)); // ?
		list.add(new BasicNameValuePair("m_" + index, m)); // ?
		list.add(new BasicNameValuePair("f_" + index, f)); // query property
		list.add(new BasicNameValuePair("o_" + index, o)); // query type
		list.add(new BasicNameValuePair("v_" + index, v)); // value
		params.add(list);
		return index + 1;
	}

	@Override
	public SearchRequestResult search(Map<String, String> query)
			throws IOException, NotReachableException, OpacErrorException {
		List<List<NameValuePair>> queryParams = new ArrayList<List<NameValuePair>>();

		int index = 0;
		index = addParameters(query, KEY_SEARCH_QUERY_FREE,
				data.optString("KEY_SEARCH_QUERY_FREE", "2"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR,
				data.optString("KEY_SEARCH_QUERY_AUTHOR", "3"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_TITLE,
				data.optString("KEY_SEARCH_QUERY_TITLE", "12"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA,
				data.optString("KEY_SEARCH_QUERY_KEYWORDA", "24"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUDIENCE,
				data.optString("KEY_SEARCH_QUERY_AUDIENCE", "25"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM,
				data.optString("KEY_SEARCH_QUERY_SYSTEM", "26"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_ISBN,
				data.optString("KEY_SEARCH_QUERY_ISBN", "29"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER,
				data.optString("KEY_SEARCH_QUERY_PUBLISHER", "32"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_BARCODE,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "46"), CONTAINS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_START,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "34"), FROM,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_END,
				data.optString("KEY_SEARCH_QUERY_BARCODE", "34"), TO,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_CATEGORY,
				data.optString("KEY_SEARCH_QUERY_CATEGORY", "42"), EQUALS,
				queryParams, index);
		index = addParameters(query, KEY_SEARCH_QUERY_BRANCH,
				data.optString("KEY_SEARCH_QUERY_BRANCH", "48"), EQUALS,
				queryParams, index);

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}
		// if (index > 4) {
		// throw new OpacErrorException(
		// "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.");
		// }

		this.query = queryParams;

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		start();
		params.add(new BasicNameValuePair("cmd", "5"));
		params.add(new BasicNameValuePair("sC", encode(queryParams, "=", "%%",
				"++")));
		params.add(new BasicNameValuePair("Sort", "Autor"));

		String text = encode(params, "=", "&amp;");
		Log.d("opac", text);
		String base64 = URLEncoder.encode(
				Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP),
				"UTF-8");
		Log.d("opac", opac_url + "/search.aspx?data=" + base64);

		String html = httpGet(opac_url + "/search.aspx?data=" + base64,
				getDefaultEncoding(), false);
		return parse_search(html, 1);
	}

	private SearchRequestResult parse_search(String html, int page)
			throws OpacErrorException, UnsupportedEncodingException {
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
			Log.d("opac", html);
			throw new OpacErrorException("Fehler beim Erkennen der Trefferzahl");
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
					+ author
					+ "<br />"
					+ title
					+ (titleAddition.equals("") ? "" : " - <i>" + titleAddition
							+ "</i>") + "</b><br /><small>" + desc + "</small>");

			String coverUrl = tr.select(".coverWrapper input").attr("src");
			if (!coverUrl.contains("leer.gif"))
				sr.setCover(coverUrl);

			String link = tr.select("a[href^=detail.aspx]").attr("href");
			String base64 = getQueryParamsFirst(link).get("data");
			if (base64.contains("-"))
				base64 = base64.substring(0, base64.indexOf("-") - 1);
			String decoded = new String(Base64.decode(base64, Base64.NO_WRAP),
					"UTF-8");
			pattern = Pattern.compile("CatalogueId=(\\d*)");
			matcher = pattern.matcher(decoded);
			if (matcher.find()) {
				sr.setId(matcher.group(1));
			} else {
				Log.d("opac", decoded);
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
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("cmd", "1"));
		params.add(new BasicNameValuePair("sC", encode(query, "=", "%%", "++")));
		params.add(new BasicNameValuePair("pI", String.valueOf(page - 1)));
		params.add(new BasicNameValuePair("Sort", "Autor"));

		String text = encode(params, "=", "&amp;");
		Log.d("opac", text);
		String base64 = URLEncoder.encode(
				Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP),
				"UTF-8");
		Log.d("opac", opac_url + "/search.aspx?data=" + base64);

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

		Elements trs = doc.select("#detail-center .DetailInformation tr");
		for (Element tr : trs) {
			String name = tr.select(".DetailInformationEntryName").text()
					.replace(":", "");
			String value = tr.select(".DetailInformationEntryContent").text();
			if (name.equals("Titel")) {
				item.setTitle(value);
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
			copy.put(DetailledItem.KEY_COPY_LOCATION,
					tr.select("#mediaItemLocationWrapper span").text());
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
	public String[] getSearchFields() {
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
