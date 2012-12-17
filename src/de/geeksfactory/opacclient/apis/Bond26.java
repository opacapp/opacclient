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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

public class Bond26 implements OpacApi {

	/*
	 * OpacApi für OCLC WebOpacs V2.6 Meist kompatibel zu V2.7
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
		return new String[] { "titel", "verfasser", "schlag_a", "schlag_b",
				"zweigstelle", "mediengruppe", "isbn", "jahr_von", "jahr_bis",
				"notation", "interessenkreis", "verlag", "order" };
	}

	@Override
	public String getLast_error() {
		return last_error;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
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

	public void extract_meta(String html) {
		// Zweigstellen und Mediengruppen auslesen
		Document doc = Jsoup.parse(html);

		Elements zst_opts = doc.select("#zst option");
		MetaDataSource data = new MetaDataSource(context);
		data.open();
		data.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				data.addMeta("zst", library.getIdent(), opt.val(), opt.text());
		}

		Elements mg_opts = doc.select("#medigrp option");
		for (int i = 0; i < mg_opts.size(); i++) {
			Element opt = mg_opts.get(i);
			if (!opt.val().equals(""))
				data.addMeta("mg", library.getIdent(), opt.val(), opt.text());
		}

		data.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {
		initialised = true;
		HttpGet httpget = new HttpGet(opac_url + "/woload.asp?lkz=1&nextpage=");
		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		response.getEntity().consumeContent();

		MetaDataSource data = new MetaDataSource(context);
		data.open();
		if (!data.hasMeta(library.getIdent())) {
			HttpPost httppost = new HttpPost(opac_url + "/index.asp");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
			nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);
			String html = convertStreamToString(response.getEntity()
					.getContent());
			data.close();
			extract_meta(html);
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

	@Override
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		HttpPost httppost = new HttpPost(opac_url + "/index.asp");

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("stichtit", "stich"));
		nameValuePairs.add(new BasicNameValuePair("stichwort",
				getStringFromBundle(query, "titel")));
		nameValuePairs.add(new BasicNameValuePair("verfasser",
				getStringFromBundle(query, "verfasser")));
		nameValuePairs.add(new BasicNameValuePair("schlag_a",
				getStringFromBundle(query, "schlag_a")));
		nameValuePairs.add(new BasicNameValuePair("schlag_b",
				getStringFromBundle(query, "schlag_b")));
		nameValuePairs.add(new BasicNameValuePair("zst", getStringFromBundle(
				query, "zweigstelle")));
		nameValuePairs.add(new BasicNameValuePair("medigrp",
				getStringFromBundle(query, "mediengruppe")));
		nameValuePairs.add(new BasicNameValuePair("isbn", getStringFromBundle(
				query, "isbn")));
		nameValuePairs.add(new BasicNameValuePair("jahr_von",
				getStringFromBundle(query, "jahr_von")));
		nameValuePairs.add(new BasicNameValuePair("jahr_bis",
				getStringFromBundle(query, "jahr_bis")));
		nameValuePairs.add(new BasicNameValuePair("notation",
				getStringFromBundle(query, "notation")));
		nameValuePairs.add(new BasicNameValuePair("ikr", getStringFromBundle(
				query, "interessenkreis")));
		nameValuePairs.add(new BasicNameValuePair("verl", getStringFromBundle(
				query, "verlag")));
		nameValuePairs.add(new BasicNameValuePair("orderselect",
				getStringFromBundle(query, "order")));
		nameValuePairs.add(new BasicNameValuePair("suche_starten.x", "1"));
		nameValuePairs.add(new BasicNameValuePair("suche_starten.y", "1"));
		nameValuePairs.add(new BasicNameValuePair("QL_Nr", ""));

		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = ahc.execute(httppost);

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

		HttpGet httpget = new HttpGet(opac_url + "/index.asp?scrollAction="
				+ page);
		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return parse_search(html);
	}

	private List<SearchResult> parse_search(String html) {
		Document doc = Jsoup.parse(html);
		Elements table = doc
				.select(".resulttab tr.result_trefferX, .resulttab tr.result_treffer");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			String[] fparts = tr.select("td a img").get(0).attr("src")
					.split("/");
			sr.setType(fparts[fparts.length - 1].replace(".jpg", ".png")
					.replace(".gif", ".png").toLowerCase());
			try {
				Comment c = (Comment) tr.child(1).childNode(0);
				String comment = c.getData().trim();
				String id = comment.split(": ")[1];
				sr.setId(id);
			} catch (Exception e) {

			}
			sr.setInnerhtml(tr.child(1).child(0).html());
			sr.setNr(i);
			results.add(sr);
		}
		this.results = doc.select(".result_gefunden").text();
		return results;
	}

	@Override
	public DetailledItem getResultById(String a) throws IOException,
			NotReachableException {
		if (!initialised)
			start();
		HttpGet httpget = new HttpGet(opac_url + "/index.asp?MedienNr=" + a);

		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		HttpGet httpget = new HttpGet(opac_url + "/index.asp?detmediennr=" + nr);

		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	private DetailledItem parse_result(String html) {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();

		if (doc.select(".detail_cover a img").size() == 1) {
			result.setCover(doc.select(".detail_cover a img").get(0)
					.attr("src"));
		}

		result.setTitle(doc.select(".detail_titel").text());

		Elements detailtrs = doc.select(".detailzeile table tr");
		for (int i = 0; i < detailtrs.size(); i++) {
			Element tr = detailtrs.get(i);
			if (tr.child(0).hasClass("detail_feld")) {
				result.addDetail(new Detail(tr.child(0).text(), tr.child(1)
						.text()));
			}
		}

		String[] copy_keys = new String[] { "barcode", "zst", "abt", "ort",
				"status", "rueckgabe", "vorbestellt" };
		int copy_keynum = copy_keys.length;

		try {
			JSONArray copymap = data.getJSONArray("copiestable");
			Elements exemplartrs = doc
					.select(".exemplartab .tabExemplar, .exemplartab .tabExemplar_");
			for (int i = 0; i < exemplartrs.size(); i++) {
				Element tr = exemplartrs.get(i);

				ContentValues e = new ContentValues();

				for (int j = 0; j < copy_keynum; j++) {
					if (copymap.getInt(j) > -1) {
						e.put(copy_keys[j], tr.child(copymap.getInt(j)).text());
					}
				}
				result.addCopy(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Elements bandtrs = doc.select("table .tabBand a");
			for (int i = 0; i < bandtrs.size(); i++) {
				Element tr = bandtrs.get(i);

				ContentValues e = new ContentValues();
				e.put("id", tr.attr("href").split("=")[1]);
				e.put("titel", tr.text());
				result.addBand(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (doc.select(".detail_vorbest a").size() == 1) {
			result.setReservable(true);
		}
		return result;
	}

	@Override
	public boolean reservation(String zst, Account acc) throws IOException {
		HttpGet httpget = new HttpGet(opac_url
				+ "/index.asp?target=vorbesttrans");
		HttpResponse response = ahc.execute(httpget);
		HttpPost httppost;

		if (response.getStatusLine().getStatusCode() == 200) {
			response.getEntity().consumeContent();
			// Login vonnöten
			httppost = new HttpPost(opac_url + "/index.asp");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
			nameValuePairs
					.add(new BasicNameValuePair("PWD", acc.getPassword()));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "zwstausw"));
			nameValuePairs.add(new BasicNameValuePair("type", "VT2"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);

			String html = convertStreamToString(response.getEntity()
					.getContent());
			Document doc = Jsoup.parse(html);
			response.getEntity().consumeContent();

			if (doc.getElementsByClass("kontomeldung").size() == 1
					&& doc.select("select[name=zstauswahl]").size() == 0) {
				last_error = doc.getElementsByClass("kontomeldung").get(0)
						.text();
				return false;
			}
		} else if (response.getStatusLine().getStatusCode() == 302) {
			response.getEntity().consumeContent();
			// Bereits eingeloggt
			httpget = new HttpGet(opac_url + "/index.asp?target=zwstausw");
			response = ahc.execute(httpget);
		}

		httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs
				.add(new BasicNameValuePair("target", "vorbesttranskonto"));
		nameValuePairs.add(new BasicNameValuePair("zstauswahl", zst));
		nameValuePairs.add(new BasicNameValuePair("button2", "Bestätigung"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		response = ahc.execute(httppost);

		response.getEntity().consumeContent();

		httppost = new HttpPost(opac_url + "/index.asp");
		nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("target", "makevorbest"));
		nameValuePairs.add(new BasicNameValuePair("button1", "Bestätigung"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		response = ahc.execute(httppost);
		response.getEntity().consumeContent();

		return true;
	}

	@Override
	public boolean prolong(String a) throws IOException, NotReachableException {
		if (!initialised)
			start();
		HttpGet httpget = new HttpGet(opac_url + "/" + a);
		HttpResponse response = ahc.execute(httpget);
		String html = convertStreamToString(response.getEntity().getContent());
		Document doc = Jsoup.parse(html);
		response.getEntity().consumeContent();

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return false;
		}
		if (doc.select("#verlaengern").size() == 1) {

			HttpPost httppost = new HttpPost(opac_url + "/index.asp");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("target", "make_vl"));
			nameValuePairs.add(new BasicNameValuePair("verlaengern",
					"Bestätigung"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);
			response.getEntity().consumeContent();

			if (doc.getElementsByClass("kontomeldung").size() == 1) {
				last_error = doc.getElementsByClass("kontomeldung").get(0)
						.text();
			}

			return true;
		}
		last_error = "??";
		return false;
	}

	@Override
	public boolean cancel(String a) throws IOException, NotReachableException {
		if (!initialised)
			start();
		HttpGet httpget = new HttpGet(opac_url + "/" + a);
		HttpResponse response = ahc.execute(httpget);
		response.getEntity().consumeContent();

		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("target", "delvorbest"));
		nameValuePairs
				.add(new BasicNameValuePair("vorbdelbest", "Bestätigung"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		response = ahc.execute(httppost);
		response.getEntity().consumeContent();
		return true;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, AccountUnsupportedException,
			SocketException {
		if (!initialised)
			start();
		HttpGet httpget;

		if (acc.getName() == null || acc.getName().equals("null"))
			return null;

		// Login vonnöten
		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = ahc.execute(httppost);

		Log.i("response", "" + response.getStatusLine().getStatusCode());
		if (response.getStatusLine().getStatusCode() == 200) {
			// Login vonnöten
			response.getEntity().consumeContent();
			httppost = new HttpPost(opac_url + "/index.asp");
			nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
			nameValuePairs
					.add(new BasicNameValuePair("PWD", acc.getPassword()));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "konto"));
			nameValuePairs.add(new BasicNameValuePair("type", "K"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			httpget = new HttpGet(opac_url + "/index.asp?target=konto");
			response = ahc.execute(httpget);
		} else if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		Document doc = Jsoup.parse(html);
		response.getEntity().consumeContent();

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return null;
		}
		JSONArray copymap = null;

		try {
			copymap = data.getJSONArray("accounttable");
		} catch (JSONException e) {
			throw new AccountUnsupportedException(html);
		}

		String[] copymap_keys = new String[] { "barcode", "verfasser", "titel",
				"frist", "status", "zst", "ast", "link" };
		int copymap_num = copymap_keys.length;
		List<ContentValues> medien = new ArrayList<ContentValues>();

		Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
				.select("tr.tabKonto");

		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			ContentValues e = new ContentValues();

			for (int j = 0; j < copymap_num; j++) {
				if (copymap.getInt(j) > -1) {
					if (copymap_keys[j].equals("link")) {
						if (tr.child(copymap.getInt(j)).children().size() > 0) {
							e.put(copymap_keys[j], tr.child(copymap.getInt(j))
									.child(0).attr("href"));
						}
					} else {
						e.put(copymap_keys[j], tr.child(copymap.getInt(j))
								.text());
					}
				}
			}
			medien.add(e);
		}

		try {
			copymap = data.getJSONArray("reservationtable");
		} catch (JSONException e) {
			throw new AccountUnsupportedException(html);
		}
		copymap_keys = new String[] { "verfasser", "titel", "bereit", "zst",
				"cancel" };
		copymap_num = copymap_keys.length;

		List<ContentValues> reservations = new ArrayList<ContentValues>();
		exemplartrs = doc.select(".kontozeile_center table").get(1)
				.select("tr.tabKonto");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			ContentValues e = new ContentValues();

			for (int j = 0; j < copymap_num; j++) {
				if (copymap.getInt(j) > -1) {
					if (copymap_keys[j].equals("cancel")) {
						if (tr.child(copymap.getInt(j)).children().size() > 0) {
							e.put(copymap_keys[j], tr.child(copymap.getInt(j))
									.child(0).attr("href"));
						}
					} else {
						e.put(copymap_keys[j], tr.child(copymap.getInt(j))
								.text());
					}
				}
			}

			reservations.add(e);
		}

		AccountData res = new AccountData();
		res.setLent(medien);
		res.setReservations(reservations);
		return res;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return !library.getData().isNull("accounttable");
	}

	@Override
	public boolean isAccountExtendable() {
		return true;
	}

	@Override
	public String getAccountExtendableInfo(Account acc)
			throws ClientProtocolException, SocketException, IOException,
			NotReachableException {
		if (!initialised)
			start();
		HttpGet httpget;

		if (acc.getName() == null || acc.getName().equals("null"))
			return null;

		// Login vonnöten
		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = ahc.execute(httppost);

		Log.i("response", "" + response.getStatusLine().getStatusCode());
		if (response.getStatusLine().getStatusCode() == 200) {
			// Login vonnöten
			response.getEntity().consumeContent();
			httppost = new HttpPost(opac_url + "/index.asp");
			nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
			nameValuePairs
					.add(new BasicNameValuePair("PWD", acc.getPassword()));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "konto"));
			nameValuePairs.add(new BasicNameValuePair("type", "K"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			httpget = new HttpGet(opac_url + "/index.asp?target=konto");
			response = ahc.execute(httpget);
		} else if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		return html;

	}

	@Override
	public SimpleDateFormat getDateFormat() {
		return new SimpleDateFormat("dd.MM.yyyy");
	}
}
