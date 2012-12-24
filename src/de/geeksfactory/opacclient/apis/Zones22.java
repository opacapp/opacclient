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

public class Zones22 implements OpacApi {

	/*
	 * OpacApi für WebOpacs "Zones.2.2.45.04" z.B. Hamburg - TODO: Suche - Suche
	 * mit Medientypen - Suche blättern - Details - Details by id - Merkliste -
	 * Account - Vorbestellen - Zweigstellen
	 */

	private String opac_url = "";
	private String results;
	private JSONObject data;
	private DefaultHttpClient ahc;
	private Context context;
	private boolean initialised = false;
	private String last_error;
	private Library library;

	@Override
	public String getResults() {
		return results;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { "titel", "verfasser", "schlag_a", "zweigstelle",
				"isbn", "jahr" };
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
		MetaDataSource data = new MetaDataSource(context);
		data.open();
		data.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				data.addMeta("zst", library.getIdent(), opt.attr("for")
						.substring(opt.attr("for").indexOf(".", 8)).trim(), opt
						.text().trim());
		}

		data.close();
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
		if (!initialised)
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

		index = addParameters(query, "titel", "ti=", params, index);
		index = addParameters(query, "verfasser", "au=", params, index);
		index = addParameters(query, "isbn", "sb=", params, index);
		index = addParameters(query, "schlag_a", "su=", params, index);
		index = addParameters(query, "jahr", "dp=", params, index);

		if (index > 3) {
			last_error = "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.";
			return null;
		}

		HttpGet httpget = new HttpGet(opac_url + "/?"
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
		return null;
	}

	private List<SearchResult> parse_search(String html) {
		Document doc = Jsoup.parse(html);
		this.results = doc.select(".searchHits").first().text().trim();

		Elements table = doc.select("#BrowseList > tbody > tr");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();

			sr.setType(tr.select(".SummaryMaterialTypeField").text()
					.replace("\n", " ").trim());

			String desc = "";
			Elements children = tr.select(".SummaryDataCell tr");
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
		// https://www.buecherhallen.de/alswww2.dll/APS_PRESENT_BIB?Style=Portal3&Lang=GER&ResponseEncoding=utf-8&no=T014982642&QueryObject=Obj_375131356306224
		// What is no=?
		return null;
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		return null;
	}

	private DetailledItem parse_result(String html) throws IOException {
		return null;
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
