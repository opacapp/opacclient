package de.geeksfactory.opacclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class OpacWebApi {

	private DefaultHttpClient ahc;
	public String opac_url = "";
	private Context context;
	public String results;
	private boolean initialised = false;
	private String last_error;
	public JSONArray bib;

	public String getResults() {
		return results;
	}

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

	public void extract_information(String html) {
		// Zweigstellen und Mediengruppen auslesen
		Document doc = Jsoup.parse(html);
		Editor spe = null;
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		spe = sp.edit();
		Elements zst_opts = doc.select("#zst option");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			sb.append(opt.val() + ": " + (opt.text().trim()) + "~");
		}
		spe.putString("opac_zst", sb.toString());
		Elements mg_opts = doc.select("#medigrp option");
		sb = new StringBuilder();
		for (int i = 0; i < mg_opts.size(); i++) {
			Element opt = mg_opts.get(i);
			sb.append(opt.val() + ": " + (opt.text().trim()) + "~");
		}
		spe.putString("opac_mg", sb.toString());
		spe.commit();
	}

	public OpacWebApi(String opac_url, Context context, JSONArray bib) {
		// ahc = AndroidHttpClient.newInstance("WebOpac Client / Android");
		ahc = new DefaultHttpClient();
		this.opac_url = opac_url;
		this.context = context;
		this.bib = bib;
	}

	public void init() throws ClientProtocolException, IOException,
			NotReachableException {
		initialised = true;
		HttpGet httpget = new HttpGet(opac_url + "/woload.asp?lkz=1&nextpage=");
		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		response.getEntity().consumeContent();

		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		response = ahc.execute(httppost);
		String html = convertStreamToString(response.getEntity().getContent());
		extract_information(html);
	}

	public List<SearchResult> search(String stichwort, String verfasser,
			String schlag_a, String schlag_b, String zweigstelle,
			String mediengruppe, String isbn, String jahr_von, String jahr_bis,
			String notation, String interessenkreis, String verlag, String order)
			throws IOException, NotReachableException {
		if (!initialised)
			init();

		HttpPost httppost = new HttpPost(opac_url + "/index.asp");

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("stichtit", "stich"));
		nameValuePairs.add(new BasicNameValuePair("stichwort", stichwort));
		nameValuePairs.add(new BasicNameValuePair("verfasser", verfasser));
		nameValuePairs.add(new BasicNameValuePair("schlag_a", schlag_a));
		nameValuePairs.add(new BasicNameValuePair("schlag_b", schlag_b));
		nameValuePairs.add(new BasicNameValuePair("zst", zweigstelle));
		nameValuePairs.add(new BasicNameValuePair("medigrp", mediengruppe));
		nameValuePairs.add(new BasicNameValuePair("isbn", isbn));
		nameValuePairs.add(new BasicNameValuePair("jahr_von", jahr_von));
		nameValuePairs.add(new BasicNameValuePair("jahr_bis", jahr_bis));
		nameValuePairs.add(new BasicNameValuePair("notation", notation));
		nameValuePairs.add(new BasicNameValuePair("ikr", interessenkreis));
		nameValuePairs.add(new BasicNameValuePair("verl", verlag));
		nameValuePairs.add(new BasicNameValuePair("orderselect", order));
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

	public List<SearchResult> search_page(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			init();

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
			sr.setType("type_"
					+ fparts[fparts.length - 1].replace(".jpg", ".png")
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

	public DetailledItem getResultById(String a) throws IOException,
			NotReachableException {
		if (!initialised)
			init();
		HttpGet httpget = new HttpGet(opac_url + "/index.asp?MedienNr=" + a);

		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

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
				String[] detail = { tr.child(0).text(), tr.child(1).text() };
				result.addDetail(detail);
			}
		}
		try {
			JSONArray copymap = bib.getJSONArray(1);
			Elements exemplartrs = doc
					.select(".exemplartab .tabExemplar, .exemplartab .tabExemplar_");
			for (int i = 0; i < exemplartrs.size(); i++) {
				Element tr = exemplartrs.get(i);

				String[] e = new String[7];

				for (int j = 0; j < 7; j++) {
					if (copymap.getInt(j) > -1) {
						e[j] = tr.child(copymap.getInt(j)).text();
					} else {
						e[j] = "?";
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

				String[] e = new String[2];
				e[0] = tr.attr("href").split("=")[1];
				e[1] = tr.text();
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

	public Boolean reservation(String zst, String ausw, String pwd)
			throws IOException {
		HttpGet httpget = new HttpGet(opac_url
				+ "/index.asp?target=vorbesttrans");
		HttpResponse response = ahc.execute(httpget);
		HttpPost httppost;

		if (response.getStatusLine().getStatusCode() == 200) {
			response.getEntity().consumeContent();
			// Login vonnöten
			httppost = new HttpPost(opac_url + "/index.asp");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("AUSWEIS", ausw));
			nameValuePairs.add(new BasicNameValuePair("PWD", pwd));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "zwstausw"));
			nameValuePairs.add(new BasicNameValuePair("type", "VT2"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);

			String html = convertStreamToString(response.getEntity()
					.getContent());
			Document doc = Jsoup.parse(html);
			response.getEntity().consumeContent();

			if (doc.getElementsByClass("kontomeldung").size() == 1) {
				last_error = doc.getElementsByClass("kontomeldung").get(0)
						.text();
				return null;
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

	public boolean prolong(String a) throws IOException, NotReachableException {
		if (!initialised)
			init();
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

	public boolean cancel(String a) throws IOException, NotReachableException {
		if (!initialised)
			init();
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

	public List<List<String[]>> account(String ausw, String pwd)
			throws IOException, NotReachableException {
		if (!initialised)
			init();
		HttpGet httpget;

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
			nameValuePairs.add(new BasicNameValuePair("AUSWEIS", ausw));
			nameValuePairs.add(new BasicNameValuePair("PWD", pwd));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "konto"));
			nameValuePairs.add(new BasicNameValuePair("type", "K"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			httpget = new HttpGet(
					opac_url
							+ "/inderesponse.getStatusLine().getStatusCode()x.asp?target=konto");
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

		List<String[]> medien = new ArrayList<String[]>();
		Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
				.select("tr.tabKonto");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			String[] e = { tr.child(0).text(), tr.child(1).text(),
					tr.child(2).text(), tr.child(3).text(), tr.child(4).text(),
					tr.child(5).text(), tr.child(6).text(),
					tr.child(7).child(0).attr("href") };
			medien.add(e);
		}

		List<String[]> reservations = new ArrayList<String[]>();
		exemplartrs = doc.select(".kontozeile_center table").get(1)
				.select("tr.tabKonto");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			String l = null;
			if (tr.child(5).children().size() != 0) {
				l = tr.child(5).child(0).attr("href");
			}
			String[] e = { tr.child(0).text(), tr.child(1).text(),
					tr.child(2).text(), tr.child(3).text(), l };
			reservations.add(e);
		}

		List<List<String[]>> res = new ArrayList<List<String[]>>();
		res.add(medien);
		res.add(reservations);

		return res;
	}
}
