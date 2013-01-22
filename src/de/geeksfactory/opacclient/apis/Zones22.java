package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import de.geeksfactory.opacclient.AccountUnsupportedException;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class Zones22 implements OpacApi {

	/*
	 * OpacApi für WebOpacs "Zones.2.2.45.04" z.B. Hamburg - TODO: Suche mit
	 * Medientypen - Account - Vorbestellen - Zweigstellen
	 */

	private String opac_url = "";
	private String results;
	private JSONObject data;
	private DefaultHttpClient ahc;
	private MetaDataSource metadata;
	private boolean initialised = false;
	private String last_error;
	private Library library;
	private int page;
	private String searchobj;

	@Override
	public String getResults() {
		return results;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_BRANCH,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_YEAR };
	}

	@Override
	public String getLast_error() {
		return last_error;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			reader = new BufferedReader(new InputStreamReader(is));
		}
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append((line + "\n"));
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public void extract_meta(Document doc) {
		// Zweigstellen auslesen
		Elements zst_opts = doc.select(".TabRechAv .limitChoice label");
		metadata.open();
		metadata.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
					library.getIdent(), opt.attr("for"), opt.text().trim());
		}

		metadata.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {
		HttpGet httpget = new HttpGet(
				opac_url
						+ "/APS_ZONES?fn=AdvancedSearch&Style=Portal3&SubStyle=&Lang=GER&ResponseEncoding=utf-8");
		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		initialised = true;

		String html = convertStreamToString(response.getEntity().getContent());
		Document doc = Jsoup.parse(html);

		searchobj = doc.select("#ExpertSearch").attr("action");

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			extract_meta(doc);
		} else {
			metadata.close();
		}
	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		ahc = new DefaultHttpClient();

		this.metadata = metadata;
		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getStringFromBundle(Bundle bundle, String key) {
		// Workaround for Bundle.getString(key, default) being available not
		// before API 12
		String res = bundle.getString(key);
		if (res == null)
			res = "";
		return res;
	}

	private int addParameters(Bundle query, String key, String searchkey,
			List<NameValuePair> params, int index) {
		if (!query.containsKey(key) || query.getString(key).equals(""))
			return index;

		if (index != 1)
			params.add(new BasicNameValuePair(".form.t" + index + ".logic",
					"and"));
		params.add(new BasicNameValuePair("q.form.t" + index + ".term",
				searchkey));
		params.add(new BasicNameValuePair("q.form.t" + index + ".expr", query
				.getString(key)));
		return index + 1;

	}

	@Override
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException {
		start();

		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("Style", "Portal3"));
		params.add(new BasicNameValuePair("SubStyle", ""));
		params.add(new BasicNameValuePair("Lang", "GER"));
		params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
		params.add(new BasicNameValuePair("Method", "QueryWithLimits"));
		params.add(new BasicNameValuePair("SearchType", "AdvancedSearch"));
		params.add(new BasicNameValuePair("TargetSearchType", "AdvancedSearch"));
		params.add(new BasicNameValuePair("DB", "SearchServer"));
		params.add(new BasicNameValuePair("q.PageSize", "10"));

		int index = 1;

		index = addParameters(query, KEY_SEARCH_QUERY_TITLE, "ti=", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "au=", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_ISBN, "sb=", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA, "su=", params,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_YEAR, "dp=", params,
				index);

		if (query.containsKey(KEY_SEARCH_QUERY_BRANCH)
				&& !query.getString(KEY_SEARCH_QUERY_BRANCH).equals(""))
			params.add(new BasicNameValuePair("q.limits.limit", query
					.getString(KEY_SEARCH_QUERY_BRANCH)));

		if (index > 3) {
			last_error = "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.";
			return null;
		} else if (index == 1) {
			last_error = "Die Suchanfrage war leer.";
			return null;
		}

		HttpGet httpget = new HttpGet(opac_url + "/" + searchobj + "?"
				+ URLEncodedUtils.format(params, "UTF-8"));

		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		page = 1;

		return parse_search(html);
	}

	@Override
	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("Style", "Portal3"));
		params.add(new BasicNameValuePair("SubStyle", ""));
		params.add(new BasicNameValuePair("Lang", "GER"));
		params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
		if (page > this.page) {
			params.add(new BasicNameValuePair("Method", "PageDown"));
		} else {
			params.add(new BasicNameValuePair("Method", "PageUp"));
		}
		params.add(new BasicNameValuePair("PageSize", "10"));

		HttpGet httpget = new HttpGet(opac_url + "/" + searchobj + "?"
				+ URLEncodedUtils.format(params, "UTF-8"));

		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		this.page = page;

		return parse_search(html);
	}

	private List<SearchResult> parse_search(String html) {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url + "/APS_PRESENT_BIB");

		results = doc.select(".searchHits").first().text().trim();

		if (doc.select(".pageNavLink").size() > 0) {
			searchobj = doc.select(".pageNavLink").first().attr("href")
					.split("\\?")[0];
		}

		Elements table = doc.select("#BrowseList > tbody > tr");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();

			sr.setType(tr.select(".SummaryMaterialTypeField").text()
					.replace("\n", " ").trim());

			String desc = "";
			Elements children = tr
					.select(".SummaryDataCell tr, .SummaryDataCellStripe tr");
			int childrennum = children.size();
			boolean haslink = false;

			for (int ch = 0; ch < childrennum; ch++) {
				Element node = children.get(ch);
				if (node.select(".SummaryFieldLegend").text().equals("Titel")) {
					desc += "<b>"
							+ node.select(".SummaryFieldData").text().trim()
							+ "</b><br />";

				} else if (node.select(".SummaryFieldLegend").text()
						.equals("Verfasser")
						|| node.select(".SummaryFieldLegend").text()
								.equals("Jahr")) {
					desc += node.select(".SummaryFieldData").text().trim()
							+ "<br />";
				}

				if (node.select(".SummaryFieldData a.SummaryFieldLink").size() > 0
						&& haslink == false) {
					sr.setId(Uri.parse(
							node.select(".SummaryFieldData a.SummaryFieldLink")
									.attr("abs:href")).getQueryParameter("no"));
					haslink = true;
				}
			}
			if (desc.endsWith("<br />"))
				desc = desc.substring(0, desc.length() - 6);
			sr.setInnerhtml(desc);
			sr.setNr(i);

			results.add(sr);
		}

		return results;
	}

	@Override
	public DetailledItem getResultById(String id) throws IOException,
			NotReachableException {

		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("Style", "Portal3"));
		params.add(new BasicNameValuePair("SubStyle", ""));
		params.add(new BasicNameValuePair("Lang", "GER"));
		params.add(new BasicNameValuePair("ResponseEncoding", "utf-8"));
		params.add(new BasicNameValuePair("no", id));

		HttpGet httpget = new HttpGet(opac_url + "/APS_PRESENT_BIB?"
				+ URLEncodedUtils.format(params, "UTF-8"));

		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(id, html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		return null;
	}

	private DetailledItem parse_result(String id, String html)
			throws IOException {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();
		boolean title_is_set = false;

		result.setId(id);

		Elements detailtrs1 = doc
				.select(".DetailDataCell table table:not(.inRecordHeader) tr");
		for (int i = 0; i < detailtrs1.size(); i++) {
			Element tr = detailtrs1.get(i);
			int s = tr.children().size();
			if (tr.child(0).text().trim().equals("Titel") && !title_is_set) {
				result.setTitle(tr.child(s - 1).text().trim());
				title_is_set = true;
			} else if (s > 1) {
				result.addDetail(new Detail(tr.child(0).text().trim(), tr
						.child(s - 1).text().trim()));
			}
		}

		Elements copydivs = doc.select(".DetailDataCell div[id^=stock_]");
		String pop = "";
		for (int i = 0; i < copydivs.size(); i++) {
			Element div = copydivs.get(i);

			if (div.attr("id").startsWith("stock_head")) {
				pop = div.text().trim();
				continue;
			}

			ContentValues copy = new ContentValues();

			// This is getting very ugly - check if it is valid for libraries
			// which are not
			// Hamburg.
			int j = 0;
			for (Node node : div.childNodes()) {
				try {
					if (node instanceof Element) {
						if (((Element) node).tag().getName().equals("br")) {
							copy.put(DetailledItem.KEY_COPY_BRANCH, pop);
							result.addCopy(copy);
							j = -1;
						} else if (((Element) node).tag().getName().equals("b")
								&& j == 1) {
							copy.put(DetailledItem.KEY_COPY_LOCATION,
									((Element) node).text());
						} else if (((Element) node).tag().getName().equals("b")
								&& j > 1) {
							copy.put(DetailledItem.KEY_COPY_STATUS,
									((Element) node).text());
						}
						j++;
					} else if (node instanceof TextNode) {
						if (j == 0)
							copy.put(DetailledItem.KEY_COPY_DEPARTMENT,
									((TextNode) node).text());
						if (j == 2)
							copy.put(DetailledItem.KEY_COPY_BARCODE,
									((TextNode) node).getWholeText().trim()
											.split("\n")[0].trim());
						if (j == 6) {
							String text = ((TextNode) node).text().trim();
							copy.put(DetailledItem.KEY_COPY_RETURN,
									text.substring(text.length() - 10));
						}
						j++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	// No account support for now.

	@Override
	public ReservationResult reservation(String zst, Account acc)
			throws IOException {
		return ReservationResult.ERROR;
	}

	@Override
	public boolean prolong(String a) throws IOException, NotReachableException {
		return false;
	}

	@Override
	public boolean cancel(String a) throws IOException, NotReachableException {
		return false;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, AccountUnsupportedException,
			SocketException {
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
	public SimpleDateFormat getDateFormat() {
		return new SimpleDateFormat("dd.MM.yyyy");
	}
}
