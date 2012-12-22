package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnRoutePNames;
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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import de.geeksfactory.opacclient.AccountUnsupportedException;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class OCLC2011 implements OpacApi {

	/*
	 * OpacApi für WebOpacs "Copyright 2011 OCLC" z.B. Bremen TODO -
	 * Vorbestellen - Account - ID für Merkliste - Redirect zu Detailsbei nur
	 * einem Ergebnis
	 */

	private String opac_url = "";
	private String results;
	private JSONObject data;
	private DefaultHttpClient ahc;
	private Context context;
	private boolean initialised = false;
	private String last_error;
	private Library library;

	private String CSId;
	private String identifier;
	private String reusehtml;

	@Override
	public String getResults() {
		return results;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { "titel", "verfasser", "schlag_a", "schlag_b",
				"zweigstelle", "isbn", "jahr", "notation", "interessenkreis",
				"verlag" };
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
		Elements zst_opts = doc.select("#selectedSearchBranchlib option");
		MetaDataSource data = new MetaDataSource(context);
		data.open();
		data.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				data.addMeta("zst", library.getIdent(), opt.val(), opt.text());
		}

		data.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {
		HttpGet httpget = new HttpGet(opac_url + "/start.do");
		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		initialised = true;

		String html = convertStreamToString(response.getEntity().getContent());
		Document doc = Jsoup.parse(html);
		CSId = doc.select("input[name=CSId]").val();

		MetaDataSource data = new MetaDataSource(context);
		data.open();
		if (!data.hasMeta(library.getIdent())) {
			data.close();
			extract_meta(doc);
		} else {
			data.close();
		}
	}

	@Override
	public void init(Context context, Library lib) {
		ahc = new DefaultHttpClient();

		this.context = context;
		this.library = lib;
		this.data = lib.getData();

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (sp.getBoolean("debug_proxy", false))
			ahc.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
					new HttpHost("192.168.0.173", 8000)); // TODO: DEBUG ONLY

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

		if (index != 0)
			params.add(new BasicNameValuePair("combinationOperator[" + index
					+ "]", "AND"));
		params.add(new BasicNameValuePair("searchCategories[" + index + "]",
				searchkey));
		params.add(new BasicNameValuePair("searchString[" + index + "]", query
				.getString(key)));
		return index + 1;

	}

	@Override
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		if (query.containsKey("volume")) {
			params.add(new BasicNameValuePair("methodToCall", "volumeSearch"));
			params.add(new BasicNameValuePair("dbIdentifier", query
					.getString("dbIdentifier")));
			params.add(new BasicNameValuePair("catKey", query
					.getString("catKey")));
			params.add(new BasicNameValuePair("periodical", "N"));
		} else {
			int index = 0;
			start();

			params.add(new BasicNameValuePair("CSId", CSId));
			params.add(new BasicNameValuePair("methodToCall", "submit"));
			params.add(new BasicNameValuePair("methodToCallParameter",
					"submitSearch"));
			params.add(new BasicNameValuePair("callingPage", "searchParameters"));
			params.add(new BasicNameValuePair("submitSearch", "Suchen"));

			index = addParameters(query, "titel", "331", params, index);
			index = addParameters(query, "verfasser", "100", params, index);
			index = addParameters(query, "isbn", "540", params, index);
			index = addParameters(query, "schlag_a", "902", params, index);
			index = addParameters(query, "schlag_b", "710", params, index);
			index = addParameters(query, "jahr", "425", params, index);
			index = addParameters(query, "verlag", "412", params, index);
			index = addParameters(query, "systematik", "700", params, index);
			index = addParameters(query, "interessenkreis", "1001", params,
					index);

			if (index > 4) {
				last_error = "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.";
				return null;
			}

			params.add(new BasicNameValuePair("selectedSearchBranchlib", query
					.getString("zweigstelle")));
		}

		HttpGet httpget = new HttpGet(opac_url + "/search.do?"
				+ URLEncodedUtils.format(params, "UTF-8"));

		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return parse_search(html);
	}

	@Override
	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		HttpGet httpget = new HttpGet(opac_url
				+ "/hitList.do?methodToCall=pos&identifier=" + identifier
				+ "&curPos=" + (((page - 1) * 10) + 1));
		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return parse_search(html);
	}

	private List<SearchResult> parse_search(String html) {
		Document doc = Jsoup.parse(html);
		this.results = doc.select(".box-header h2").first().text();
		Log.i("results", results);
		if (results.contains("(1/1)")) {
			reusehtml = html;
			last_error = "is_a_redirect";
			return null;
		}

		Elements table = doc.select("table.data tbody tr");
		identifier = null;
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			String[] fparts = tr.select("td a img").get(0).attr("src")
					.split("/");
			sr.setType(fparts[fparts.length - 1].replace(".jpg", ".png")
					.replace(".gif", ".png").toLowerCase());

			String desc = "";
			List<Node> children = tr.child(2).childNodes();
			int childrennum = children.size();
			boolean haslink = false;

			for (int ch = 0; ch < childrennum; ch++) {
				Node node = children.get(ch);
				if (node instanceof TextNode) {
					String text = ((TextNode) node).text().trim();
					if (!text.equals(""))
						desc += text + "<br />";
				} else if (node instanceof Element) {
					if (((Element) node).tag().getName().equals("a")) {
						if (node.hasAttr("href") && !haslink) {
							haslink = true;
							desc += ((Element) node).text() + "<br />";

							try {
								List<NameValuePair> anyurl = URLEncodedUtils
										.parse(new URI(((Element) node)
												.attr("href")), "UTF-8");
								for (NameValuePair nv : anyurl) {
									if (nv.getName().equals("identifier")) {
										identifier = nv.getValue();
										break;
									}
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}
				}
			}
			if (desc.endsWith("<br />"))
				desc = desc.substring(0, desc.length() - 6);
			sr.setInnerhtml(desc);
			sr.setNr(i);
			sr.setId(null);
			results.add(sr);
		}

		return results;
	}

	@Override
	public DetailledItem getResultById(String a) throws IOException,
			NotReachableException {
		start();

		if (a == null && reusehtml != null) {
			return parse_result(reusehtml);
		}

		HttpGet httpget = new HttpGet(opac_url + "/index.asp?MedienNr=" + a);

		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		HttpGet httpget = new HttpGet(
				opac_url
						+ "/singleHit.do?tab=showExemplarActive&methodToCall=showHit&curPos="
						+ (nr + 1) + "&identifier=" + identifier);

		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	private DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);

		HttpGet httpget = new HttpGet(opac_url
				+ "/singleHit.do?methodToCall=activateTab&tab=showTitleActive");

		HttpResponse response = ahc.execute(httpget);
		String html2 = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		Document doc2 = Jsoup.parse(html2);

		DetailledItem result = new DetailledItem();

		if (doc.select(".data td img").size() == 1) {
			result.setCover(doc.select(".data td img").first().attr("abs:src"));
		}

		result.setTitle(doc.select(".data td strong").first().text());

		String title = "";
		Element detailtrs = doc2.select("#tab-content .data td").first();
		for (Node node : detailtrs.childNodes()) {
			if (node instanceof Element) {
				if (((Element) node).tag().getName().equals("strong")) {
					title = ((Element) node).text().trim();
				}
			} else if (node instanceof TextNode && !title.equals("")) {
				String text = ((TextNode) node).text().trim();
				if (!text.equals(""))
					result.addDetail(new Detail(title, text));
				title = "";
			}
		}

		Elements exemplartrs = doc.select("#tab-content .data tr:not(#bg2)");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			try {
				ContentValues e = new ContentValues();
				e.put("barcode", tr.child(1).text().trim());
				e.put("zst", tr.child(3).text().trim());
				e.put("status", tr.child(4).text().trim());
				result.addCopy(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		try {
			Element isvolume = null;
			Bundle volume = new Bundle();
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
						volume.putString("catKey", nv.getValue());
					} else if (nv.getName().equals("dbIdentifier")) {
						volume.putString("dbIdentifier", nv.getValue());
					}
				}
				if (isvolume != null) {
					volume.putBoolean("volume", true);
					result.setVolumesearch(volume);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		result.setReservable(false);
		return result;
	}

	// No account support for now.

	@Override
	public boolean reservation(String zst, Account acc) throws IOException {
		return false;
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
