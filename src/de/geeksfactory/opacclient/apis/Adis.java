package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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

public class Adis extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected Library library;

	protected int s_requestCount = 0;
	protected String s_service;
	protected String s_sid;
	protected String s_exts;
	protected List<NameValuePair> s_pageform;
	protected int s_lastpage;
	protected Document s_reusedoc;

	protected static HashMap<String, MediaType> types = new HashMap<String, MediaType>();

	static {
		types.put("Buch", MediaType.BOOK);
		types.put("Band", MediaType.BOOK);
		types.put("DVD-ROM", MediaType.CD_SOFTWARE);
		types.put("CD-ROM", MediaType.CD_SOFTWARE);
		types.put("Medienkombination", MediaType.PACKAGE);
		types.put("DVD-Video", MediaType.DVD);
		types.put("Noten", MediaType.SCORE_MUSIC);
		types.put("Konsolenspiel", MediaType.GAME_CONSOLE);
		types.put("CD", MediaType.CD);
		types.put("Zeitschrift", MediaType.MAGAZINE);
		types.put("Zeitung", MediaType.NEWSPAPER);
		types.put("Beitrag E-Book", MediaType.EBOOK);
		types.put("Karte", MediaType.MAP);
	}

	public Document htmlGet(String url) throws ClientProtocolException,
			IOException {

		if (!url.contains("requestCount")) {
			url = url + (url.contains("?") ? "&" : "?") + "requestCount="
					+ s_requestCount;
		}

		HttpGet httpget = new HttpGet(cleanUrl(url));
		HttpResponse response;

		response = http_client.execute(httpget);

		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent(),
				getDefaultEncoding());
		response.getEntity().consumeContent();
		Document doc = Jsoup.parse(html);
		Pattern patRequestCount = Pattern.compile("requestCount=([0-9]+)");
		for (Element a : doc.select("a")) {
			Matcher objid_matcher = patRequestCount.matcher(a.attr("href"));
			if (objid_matcher.matches()) {
				s_requestCount = Integer.parseInt(objid_matcher.group(1));
			}
		}
		doc.setBaseUri(url);
		return doc;
	}

	public Document htmlPost(String url, List<NameValuePair> data)
			throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(cleanUrl(url));

		boolean rcf = false;
		for (NameValuePair nv : data) {
			if (nv.getName().equals("requestCount")) {
				rcf = true;
				break;
			}
		}
		if (!rcf) {
			data.add(new BasicNameValuePair("requestCount", s_requestCount + ""));
		}

		httppost.setEntity(new UrlEncodedFormEntity(data));
		HttpResponse response = null;
		response = http_client.execute(httppost);

		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent(),
				getDefaultEncoding());
		response.getEntity().consumeContent();
		Document doc = Jsoup.parse(html);
		Pattern patRequestCount = Pattern
				.compile(".*requestCount=([0-9]+)[^0-9].*");
		for (Element a : doc.select("a")) {
			Matcher objid_matcher = patRequestCount.matcher(a.attr("href"));
			if (objid_matcher.matches()) {
				s_requestCount = Integer.parseInt(objid_matcher.group(1));
			}
		}
		doc.setBaseUri(url);
		return doc;
	}

	@Override
	public void start() throws IOException, NotReachableException {

		try {
			Document doc = htmlGet(opac_url + "?"
					+ data.getString("startparams"));

			Pattern padSid = Pattern
					.compile(".*;jsessionid=([0-9A-Fa-f]+)[^0-9A-Fa-f].*");
			for (Element navitem : doc.select("#unav li a")) {
				if (navitem.attr("href").contains("service")) {
					s_service = getQueryParams(navitem.attr("href")).get(
							"service").get(0);
				}
				if (navitem.text().contains("Erweiterte Suche")) {
					s_exts = getQueryParams(navitem.attr("href")).get("sp")
							.get(0);
				}
				Matcher objid_matcher = padSid.matcher(navitem.attr("href"));
				if (objid_matcher.matches()) {
					s_sid = objid_matcher.group(1);
				}
			}

			metadata.open();
			if (!metadata.hasMeta(library.getIdent())) {
				metadata.close();
				extract_meta();
			} else {
				metadata.close();
			}

		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
		}

		initialised = true;
	}

	protected void extract_meta() throws ClientProtocolException, IOException {
		metadata.open();
		metadata.clearMeta(library.getIdent());
		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=" + s_exts);

		for (Element opt : doc.select("#MEDIUM_1 option")) {
			metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
					library.getIdent(), opt.attr("value"), opt.text());
		}

		for (Element opt : doc.select("#AUSGAB_1 option")) {
			metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
					library.getIdent(), opt.attr("value"), opt.text());
		}

		metadata.close();
	}

	@Override
	protected String getDefaultEncoding() {
		return "UTF-8";
	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException, OpacErrorException {
		start();
		// TODO: There are also libraries with a different search form,
		// s_exts=SS2 instead of s_exts=SS6
		// e.g. munich. Treat them differently!
		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=" + s_exts);

		int cnt = 0;
		List<NameValuePair> nvpairs = new ArrayList<NameValuePair>();
		for (String field : getSearchFields()) {
			if (query.containsKey(field) && !query.getString(field).equals("")) {

				if (field.equals(KEY_SEARCH_QUERY_CATEGORY)) {
					doc.select("select#MEDIUM_1").val(
							query.getString(KEY_SEARCH_QUERY_CATEGORY));
					continue;
				} else if (field.equals(KEY_SEARCH_QUERY_BRANCH)) {
					doc.select("select#AUSGAB_1").val(
							query.getString(KEY_SEARCH_QUERY_BRANCH));
					continue;
				}

				cnt++;

				for (Element opt : doc.select("select#SUCH01_" + cnt
						+ " option")) {
					if (field.equals(KEY_SEARCH_QUERY_FREE)
							&& (opt.text().contains("Alle Felder") || opt
									.text().contains("Freie Suche"))) {
						doc.select("select#SUCH01_" + cnt).val(opt.val());
						break;
					} else if (field.equals(KEY_SEARCH_QUERY_TITLE)
							&& opt.text().contains("Titel")) {
						doc.select("select#SUCH01_" + cnt).val(opt.val());
						break;
					} else if (field.equals(KEY_SEARCH_QUERY_AUTHOR)
							&& (opt.text().contains("Autor") || opt.text()
									.contains("Person"))) {
						doc.select("select#SUCH01_" + cnt).val(opt.val());
						break;
					} else if (field.equals(KEY_SEARCH_QUERY_ISBN)
							&& opt.text().contains("ISBN")) {
						doc.select("select#SUCH01_" + cnt).val(opt.val());
						break;
					} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDA)
							&& opt.text().contains("Schlagwort")) {
						doc.select("select#SUCH01_" + cnt).val(opt.val());
						break;
					}
				}

				doc.select("input#FELD01_" + cnt).val(query.getString(field));

				if (cnt > 4) {
					throw new OpacErrorException(
							"Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.");
				}
			}
		}

		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				nvpairs.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		nvpairs.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
		nvpairs.add(new BasicNameValuePair("$Toolbar_0.y", "1"));

		if (cnt == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}

		Document docresults = htmlPost(opac_url + ";jsessionid=" + s_sid,
				nvpairs);

		return parse_search(docresults, 1);
	}

	private SearchRequestResult parse_search(Document doc, int page)
			throws OpacErrorException {

		if (doc.select(".message h1").size() > 0
				&& doc.select("#right #R06").size() == 0) {
			throw new OpacErrorException(doc.select(".message h1").text());
		}

		int total_result_count = -1;
		List<SearchResult> results = new ArrayList<SearchResult>();

		if (doc.select("#right #R06").size() > 0) {
			Pattern patNum = Pattern
					.compile(".*Treffer: .* von ([0-9]+)[^0-9]*");
			Matcher matcher = patNum.matcher(doc.select("#right #R06").text()
					.trim());
			if (matcher.matches()) {
				total_result_count = Integer.parseInt(matcher.group(1));
			}
		}

		if (doc.select("#right #R03").size() == 1
				&& doc.select("#right #R03").text().trim()
						.endsWith("Treffer: 1")) {
			s_reusedoc = doc;
			throw new OpacErrorException("is_a_redirect");
		}

		Pattern patId = Pattern
				.compile("javascript:.*htmlOnLink\\('([0-9A-Za-z]+)'\\)");

		for (Element tr : doc.select("table.rTable_table tbody tr")) {
			SearchResult res = new SearchResult();

			res.setInnerhtml(tr.select(".rTable_td_text a").first().html());
			res.setNr(Integer.parseInt(tr.child(0).text().trim()));

			Matcher matcher = patId.matcher(tr.select(".rTable_td_text a")
					.first().attr("href"));
			if (matcher.matches()) {
				res.setId(matcher.group(1));
			}

			String typetext = tr.select(".rTable_td_img img").first()
					.attr("title");
			if (types.containsKey(typetext))
				res.setType(types.get(typetext));

			results.add(res);
		}

		s_pageform = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				s_pageform.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		s_lastpage = page;

		return new SearchRequestResult(results, total_result_count, page);
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

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		SearchRequestResult res = null;
		while (page != s_lastpage) {
			List<NameValuePair> nvpairs = s_pageform;
			int i = 0;
			List<Integer> indexes = new ArrayList<Integer>();
			for (NameValuePair np : nvpairs) {
				if (np.getName().contains("$Toolbar_")) {
					indexes.add(i);
				}
				i++;
			}
			for (int j = indexes.size() - 1; j >= 0; j--) {
				nvpairs.remove((int) indexes.get(j));
			}
			int p;
			if (page > s_lastpage) {
				nvpairs.add(new BasicNameValuePair("$Toolbar_5.x", "1"));
				nvpairs.add(new BasicNameValuePair("$Toolbar_5.y", "1"));
				p = s_lastpage + 1;
			} else {
				nvpairs.add(new BasicNameValuePair("$Toolbar_4.x", "1"));
				nvpairs.add(new BasicNameValuePair("$Toolbar_4.y", "1"));
				p = s_lastpage - 1;
			}

			Document docresults = htmlPost(opac_url + ";jsessionid=" + s_sid,
					nvpairs);
			res = parse_search(docresults, p);
		}
		return res;
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {

		Document doc;
		List<NameValuePair> nvpairs;

		if (id == null && s_reusedoc != null) {
			doc = s_reusedoc;
		} else {
			nvpairs = s_pageform;
			int i = 0;
			List<Integer> indexes = new ArrayList<Integer>();
			for (NameValuePair np : nvpairs) {
				if (np.getName().contains("$Toolbar_")
						|| np.getName().contains("selected")) {
					indexes.add(i);
				}
				i++;
			}
			for (int j = indexes.size() - 1; j >= 0; j--) {
				nvpairs.remove((int) indexes.get(j));
			}
			nvpairs.add(new BasicNameValuePair("selected", "ZTEXT       " + id));
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);
			// Yep, two times.
		}
		DetailledItem res = new DetailledItem();

		if (doc.select("#R001 img").size() == 1)
			res.setCover(doc.select("#R001 img").first().absUrl("src"));

		for (Element tr : doc.select("#R06 .aDISListe table tbody tr")) {
			res.addDetail(new Detail(tr.child(0).text().trim(), tr.child(1)
					.text().trim()));

			if (tr.child(0).text().trim().contains("Titel")
					&& res.getTitle() == null) {
				res.setTitle(tr.child(1).text().split("[:/;]")[0].trim());
			}
		}
		for (Element tr : doc.select("#R08 table.rTable_table tbody tr")) {
			ContentValues line = new ContentValues();
			line.put(DetailledItem.KEY_COPY_BRANCH, tr.child(0).text().trim());
			line.put(DetailledItem.KEY_COPY_LOCATION, tr.child(1).text().trim());
			line.put(DetailledItem.KEY_COPY_SHELFMARK, tr.child(2).text()
					.trim());
			String status = tr.child(3).text().trim();
			if (status.contains(" am: ")) {
				line.put(DetailledItem.KEY_COPY_STATUS, status.split("-")[0]);
				line.put(DetailledItem.KEY_COPY_RETURN, status.split(": ")[1]);
			} else {
				line.put(DetailledItem.KEY_COPY_STATUS, status);
			}
			res.addCopy(line);
		}

		// Reset
		s_pageform = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				s_pageform.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		nvpairs = s_pageform;
		nvpairs.add(new BasicNameValuePair("$Toolbar_1.x", "1"));
		nvpairs.add(new BasicNameValuePair("$Toolbar_1.y", "1"));
		parse_search(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs), 1);
		nvpairs = s_pageform;
		nvpairs.add(new BasicNameValuePair("$Toolbar_3.x", "1"));
		nvpairs.add(new BasicNameValuePair("$Toolbar_3.y", "1"));
		parse_search(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs), 1);

		res.setId(""); // null would be overridden by the UI, because there _is_
						// an id,< we just can not use it.

		return res;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		if (s_reusedoc != null) {
			return getResultById(null, null);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
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
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_TITLE,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_BRANCH,
				KEY_SEARCH_QUERY_CATEGORY, };
	}

	@Override
	public boolean isAccountSupported(Library library) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAccountExtendable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_PAGECACHE_FORBIDDEN;
	}

	public static Map<String, List<String>> getQueryParams(String url) {
		try {
			Map<String, List<String>> params = new HashMap<String, List<String>>();
			String[] urlParts = url.split("\\?");
			if (urlParts.length > 1) {
				String query = urlParts[1];
				for (String param : query.split("&")) {
					String[] pair = param.split("=");
					String key = URLDecoder.decode(pair[0], "UTF-8");
					String value = "";
					if (pair.length > 1) {
						value = URLDecoder.decode(pair[1], "UTF-8");
					}

					List<String> values = params.get(key);
					if (values == null) {
						values = new ArrayList<String>();
						params.put(key, values);
					}
					values.add(value);
				}
			}

			return params;
		} catch (UnsupportedEncodingException ex) {
			throw new AssertionError(ex);
		}
	}

}
