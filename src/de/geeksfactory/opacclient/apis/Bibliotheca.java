package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult.Status;
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
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * OpacApi implementation for Bibliotheca Web Opacs, originally developed by
 * BOND, now owned by OCLC. Known to work well with Web Opac versions from 2.6,
 * maybe older, to 2.8
 */
public class Bibliotheca implements OpacApi {

	protected String opac_url = "";
	protected JSONObject data;
	protected DefaultHttpClient ahc;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected String last_error;
	protected Library library;
	protected long logged_in;
	protected Account logged_in_as;

	protected final long SESSION_LIFETIME = 1000 * 60 * 3;

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("mbuchs", MediaType.BOOK);
		defaulttypes.put("cdkl", MediaType.CD);
		defaulttypes.put("cdromkl", MediaType.CD_SOFTWARE);
		defaulttypes.put("mcdroms", MediaType.CD);
		defaulttypes.put("ekl", MediaType.EBOOK);
		defaulttypes.put("emedium", MediaType.EBOOK);
		defaulttypes.put("monleihe", MediaType.EBOOK);
		defaulttypes.put("mbmonos", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("mbuechers", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("mdvds", MediaType.DVD);
		defaulttypes.put("mdvd", MediaType.DVD);
		defaulttypes.put("mfilms", MediaType.MOVIE);
		defaulttypes.put("mvideos", MediaType.MOVIE);
		defaulttypes.put("mhoerbuchs", MediaType.AUDIOBOOK);
		defaulttypes.put("mmusikcds", MediaType.CD_MUSIC);
		defaulttypes.put("mcdns", MediaType.CD_MUSIC);
		defaulttypes.put("mnoten1s", MediaType.SCORE_MUSIC);
		defaulttypes.put("munselbs", MediaType.UNKNOWN);
		defaulttypes.put("mztgs", MediaType.NEWSPAPER);
		defaulttypes.put("zeitung", MediaType.NEWSPAPER);
		defaulttypes.put("spielekl", MediaType.BOARDGAME);
		defaulttypes.put("mspiels", MediaType.BOARDGAME);
		defaulttypes.put("tafelkl", MediaType.SCHOOL_VERSION);
		defaulttypes.put("spiel_konsol", MediaType.GAME_CONSOLE);
		defaulttypes.put("wii", MediaType.GAME_CONSOLE);
	}

	protected String httpGet(String url) throws ClientProtocolException,
			IOException {
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = ahc.execute(httpget);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return html;
	}

	protected String httpPost(String url, UrlEncodedFormEntity data)
			throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(data);
		HttpResponse response = ahc.execute(httppost);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return html;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_KEYWORDB,
				KEY_SEARCH_QUERY_BRANCH, KEY_SEARCH_QUERY_CATEGORY,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_YEAR_RANGE_START,
				KEY_SEARCH_QUERY_YEAR_RANGE_END, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_AUDIENCE, KEY_SEARCH_QUERY_PUBLISHER,
				KEY_SEARCH_QUERY_BARCODE, "order" };
	}

	@Override
	public String getLast_error() {
		return last_error;
	}

	protected String convertStreamToString(InputStream is) throws IOException {
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
		metadata.open();
		metadata.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
						library.getIdent(), opt.val(), opt.text());
		}

		Elements mg_opts = doc.select("#medigrp option");
		for (int i = 0; i < mg_opts.size(); i++) {
			Element opt = mg_opts.get(i);
			if (!opt.val().equals(""))
				metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
						library.getIdent(), opt.val(), opt.text());
		}

		mg_opts = doc.select("#mediart option");
		for (int i = 0; i < mg_opts.size(); i++) {
			Element opt = mg_opts.get(i);
			if (!opt.val().equals(""))
				metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
						library.getIdent(), "$" + opt.val(), opt.text());
		}

		metadata.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {
		initialised = true;
		String db = "";
		if (data.has("db")) {
			try {
				db = "&db=" + data.getString("db");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		httpGet(opac_url + "/woload.asp?lkz=1&nextpage=" + db);

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			HttpPost httppost = new HttpPost();
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
			nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			String html = httpPost(opac_url + "/index.asp",
					new UrlEncodedFormEntity(nameValuePairs));
			metadata.close();
			extract_meta(html);
		} else {
			metadata.close();
		}
	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		ahc = HTTPClient.getNewHttpClient(lib);

		this.metadata = metadata;
		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
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
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("stichtit", "stich"));
		nameValuePairs.add(new BasicNameValuePair("stichwort",
				getStringFromBundle(query, KEY_SEARCH_QUERY_TITLE)));
		nameValuePairs.add(new BasicNameValuePair("verfasser",
				getStringFromBundle(query, KEY_SEARCH_QUERY_AUTHOR)));
		nameValuePairs.add(new BasicNameValuePair("schlag_a",
				getStringFromBundle(query, KEY_SEARCH_QUERY_KEYWORDA)));
		nameValuePairs.add(new BasicNameValuePair("schlag_b",
				getStringFromBundle(query, KEY_SEARCH_QUERY_KEYWORDB)));
		nameValuePairs.add(new BasicNameValuePair("zst", getStringFromBundle(
				query, KEY_SEARCH_QUERY_BRANCH)));

		String cat = getStringFromBundle(query, KEY_SEARCH_QUERY_CATEGORY);
		if (cat.startsWith("$"))
			nameValuePairs.add(new BasicNameValuePair("mediart",
					getStringFromBundle(query, KEY_SEARCH_QUERY_CATEGORY)
							.substring(1)));
		else
			nameValuePairs.add(new BasicNameValuePair("medigrp",
					getStringFromBundle(query, KEY_SEARCH_QUERY_CATEGORY)));

		nameValuePairs.add(new BasicNameValuePair("isbn", getStringFromBundle(
				query, KEY_SEARCH_QUERY_ISBN)));
		nameValuePairs.add(new BasicNameValuePair("jahr_von",
				getStringFromBundle(query, KEY_SEARCH_QUERY_YEAR_RANGE_START)));
		nameValuePairs.add(new BasicNameValuePair("jahr_bis",
				getStringFromBundle(query, KEY_SEARCH_QUERY_YEAR_RANGE_END)));
		nameValuePairs.add(new BasicNameValuePair("notation",
				getStringFromBundle(query, KEY_SEARCH_QUERY_SYSTEM)));
		nameValuePairs.add(new BasicNameValuePair("ikr", getStringFromBundle(
				query, KEY_SEARCH_QUERY_AUDIENCE)));
		nameValuePairs.add(new BasicNameValuePair("verl", getStringFromBundle(
				query, KEY_SEARCH_QUERY_PUBLISHER)));

		if (!getStringFromBundle(query, KEY_SEARCH_QUERY_BARCODE).equals("")) {
			nameValuePairs.add(new BasicNameValuePair("feld1",
					"EXEMPLAR~BUCHUNGSNR~0"));
			nameValuePairs.add(new BasicNameValuePair("ifeld1",
					getStringFromBundle(query, KEY_SEARCH_QUERY_BARCODE)));
		}

		nameValuePairs.add(new BasicNameValuePair("orderselect",
				getStringFromBundle(query, "order")));

		nameValuePairs.add(new BasicNameValuePair("suche_starten.x", "1"));
		nameValuePairs.add(new BasicNameValuePair("suche_starten.y", "1"));
		nameValuePairs.add(new BasicNameValuePair("QL_Nr", ""));

		String html = httpPost(opac_url + "/index.asp",
				new UrlEncodedFormEntity(nameValuePairs));
		return parse_search(html, 1);
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		String html = httpGet(opac_url + "/index.asp?scrollAction=" + page);
		return parse_search(html, page);
	}

	protected SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);
		Elements table = doc
				.select(".resulttab tr.result_trefferX, .resulttab tr.result_treffer");
		List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			String[] fparts = tr.select("td a img").get(0).attr("src")
					.split("/");
			String fname = fparts[fparts.length - 1];
			if (data.has("mediatypes")) {
				try {
					sr.setType(MediaType.valueOf(data.getJSONObject(
							"mediatypes").getString(fname)));
				} catch (JSONException e) {
					sr.setType(defaulttypes.get(fname.toLowerCase()
							.replace(".jpg", "").replace(".gif", "")
							.replace(".png", "")));
				} catch (IllegalArgumentException e) {
					sr.setType(defaulttypes.get(fname.toLowerCase()
							.replace(".jpg", "").replace(".gif", "")
							.replace(".png", "")));
				}
			} else {
				sr.setType(defaulttypes.get(fname.toLowerCase()
						.replace(".jpg", "").replace(".gif", "")
						.replace(".png", "")));
			}
			try {
				Comment c = (Comment) tr.child(1).childNode(0);
				String comment = c.getData().trim();
				String id = comment.split(": ")[1];
				sr.setId(id);
			} catch (Exception e) {

			}
			sr.setInnerhtml(tr.child(1).child(0).html());

			sr.setNr(i);
			Element link = tr.child(1).select("a").first();
			if (link != null && link.attr("href").contains("detmediennr")) {
				Uri uri = Uri.parse(link.attr("abs:href"));
				String nr = uri.getQueryParameter("detmediennr");
				if (nr.length() > (Math.log10(i) + 1)) {
					// Scheint eine ID zu sein…
					if (uri.getQueryParameter("detDB") != null) {
						sr.setId("&detmediennr=" + nr + "&detDB="
								+ uri.getQueryParameter("detDB"));
					} else {
						sr.setId("&detmediennr=" + nr);
					}
				}
			} else {
			}
			results.add(sr);
		}
		int results_total = -1;
		if (doc.select(".result_gefunden").size() > 0) {
			try {
				results_total = Integer.parseInt(doc.select(".result_gefunden")
						.text().trim().replaceAll(".*[^0-9]+([0-9]+).*", "$1"));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				results_total = -1;
			}
		}
		return new SearchRequestResult(results, results_total, page);
	}

	@Override
	public DetailledItem getResultById(String a, String homebranch)
			throws IOException, NotReachableException {
		if (!initialised)
			start();
		String html = httpGet(opac_url + "/index.asp?MedienNr=" + a);
		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		String html = httpGet(opac_url + "/index.asp?detmediennr=" + nr);

		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();

		if (doc.select(".detail_cover img").size() == 1) {
			result.setCover(doc.select(".detail_cover img").get(0).attr("src"));
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

		String[] copy_keys = new String[] { DetailledItem.KEY_COPY_BARCODE,
				DetailledItem.KEY_COPY_BRANCH,
				DetailledItem.KEY_COPY_DEPARTMENT,
				DetailledItem.KEY_COPY_LOCATION, DetailledItem.KEY_COPY_STATUS,
				DetailledItem.KEY_COPY_RETURN,
				DetailledItem.KEY_COPY_RESERVATIONS };
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
				e.put(DetailledItem.KEY_CHILD_ID, tr.attr("href").split("=")[1]);
				e.put(DetailledItem.KEY_CHILD_TITLE, tr.text());
				result.addBand(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (doc.select(".detail_vorbest a").size() == 1) {
			result.setReservable(true);
			result.setReservation_info(doc.select(".detail_vorbest a").attr(
					"href"));
		}
		return result;
	}

	@Override
	public ReservationResult reservation(String reservation_info, Account acc,
			int useraction, String selection) throws IOException {
		String branch_inputfield = "zstauswahl";

		Document doc = null;

		if (useraction == ReservationResult.ACTION_CONFIRMATION) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("button1", "Bestaetigung"));
			nameValuePairs.add(new BasicNameValuePair("target", "makevorbest"));
			httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs));
			return new ReservationResult(Status.OK);
		} else if (selection == null || useraction == 0) {
			String html = httpGet(opac_url + "/" + reservation_info);
			doc = Jsoup.parse(html);

			if (doc.select("input[name=AUSWEIS]").size() > 0) {
				// Login vonnöten
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("AUSWEIS", acc
						.getName()));
				nameValuePairs.add(new BasicNameValuePair("PWD", acc
						.getPassword()));
				if (data.has("db")) {
					try {
						nameValuePairs.add(new BasicNameValuePair("vkontodb",
								data.getString("db")));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
				nameValuePairs.add(new BasicNameValuePair("target", doc.select(
						"input[name=target]").val()));
				nameValuePairs.add(new BasicNameValuePair("type", "VT2"));
				html = httpPost(opac_url + "/index.asp",
						new UrlEncodedFormEntity(nameValuePairs));
				doc = Jsoup.parse(html);
			}
			if (doc.select("select[name=" + branch_inputfield + "]").size() == 0) {
				if (doc.select("select[name=VZST]").size() > 0) {
					branch_inputfield = "VZST";
				}
			}
			if (doc.select("select[name=" + branch_inputfield + "]").size() > 0) {
				ContentValues branches = new ContentValues();
				for (Element option : doc
						.select("select[name=" + branch_inputfield + "]")
						.first().children()) {
					String value = option.text().trim();
					String key;
					if (option.hasAttr("value")) {
						key = option.attr("value");
					} else {
						key = value;
					}
					branches.put(key, value);
				}
				ReservationResult result = new ReservationResult(
						Status.SELECTION_NEEDED);
				result.setActionIdentifier(ReservationResult.ACTION_BRANCH);
				result.setSelection(branches);
				return result;
			}
		} else if (useraction == ReservationResult.ACTION_BRANCH) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair(branch_inputfield,
					selection));
			nameValuePairs.add(new BasicNameValuePair("button2", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target",
					"vorbesttranskonto"));
			String html = httpPost(opac_url + "/index.asp",
					new UrlEncodedFormEntity(nameValuePairs));
			doc = Jsoup.parse(html);
		}

		if (doc == null)
			return new ReservationResult(Status.ERROR);

		if (doc.select("input[name=target]").size() > 0) {
			if (doc.select("input[name=target]").attr("value")
					.equals("makevorbest")) {
				List<String[]> details = new ArrayList<String[]>();

				if (doc.getElementsByClass("kontomeldung").size() == 1) {
					details.add(new String[] { doc
							.getElementsByClass("kontomeldung").get(0).text()
							.trim() });
				}

				for (Element row : doc.select(".kontozeile_center table tr")) {
					if (row.select(".konto_feld").size() == 1
							&& row.select(".konto_feldinhalt").size() == 1) {
						details.add(new String[] {
								row.select(".konto_feld").text().trim(),
								row.select(".konto_feldinhalt").text().trim() });
					}
				}
				ReservationResult result = new ReservationResult(
						Status.CONFIRMATION_NEEDED);
				result.setDetails(details);
				return result;
			}
		}

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return new ReservationResult(Status.ERROR);
		}

		return new ReservationResult(Status.ERROR);
	}

	@Override
	public boolean prolong(Account account, String a) throws IOException,
			NotReachableException {
		if (!initialised)
			start();
		if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
				|| logged_in_as == null) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		} else if (logged_in_as.getId() != account.getId()) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		String html = httpGet(opac_url + "/" + a);
		Document doc = Jsoup.parse(html);

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return false;
		}
		if (doc.select("#verlaengern").size() == 1) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("target", "make_vl"));
			nameValuePairs.add(new BasicNameValuePair("verlaengern",
					"Bestätigung"));
			httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs));

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
	public boolean prolongAll(Account account) throws IOException {
		if (!initialised)
			start();
		if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
				|| logged_in_as == null) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		} else if (logged_in_as.getId() != account.getId()) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		String html = httpGet(opac_url + "/index.asp?target=alleverl");
		Document doc = Jsoup.parse(html);

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return false;
		}
		return true;
	}

	@Override
	public boolean cancel(Account account, String a) throws IOException,
			NotReachableException {
		if (!initialised)
			start();
		if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
				|| logged_in_as == null) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		} else if (logged_in_as.getId() != account.getId()) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		httpGet(opac_url + "/" + a);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("target", "delvorbest"));
		nameValuePairs
				.add(new BasicNameValuePair("vorbdelbest", "Bestätigung"));
		httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
				nameValuePairs));
		return true;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, SocketException {
		if (!initialised)
			start();

		if (acc.getName() == null || acc.getName().equals("null"))
			return null;

		// Login vonnöten
		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = ahc.execute(httppost);
		String html = "";

		if (response.getStatusLine().getStatusCode() == 200) {
			// Login vonnöten
			response.getEntity().consumeContent();
			nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
			nameValuePairs
					.add(new BasicNameValuePair("PWD", acc.getPassword()));
			if (data.has("db")) {
				nameValuePairs.add(new BasicNameValuePair("vkontodb", data
						.getString("db")));
			}
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "konto"));
			nameValuePairs.add(new BasicNameValuePair("type", "K"));
			html = httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs));
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			html = httpGet(opac_url + "/index.asp?target=konto");
		} else if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}

		Document doc = Jsoup.parse(html);

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return null;
		}

		logged_in = System.currentTimeMillis();
		logged_in_as = acc;

		JSONArray copymap = null;

		copymap = data.getJSONArray("accounttable");

		String[] copymap_keys = new String[] { AccountData.KEY_LENT_BARCODE,
				AccountData.KEY_LENT_AUTHOR, AccountData.KEY_LENT_TITLE,
				AccountData.KEY_LENT_DEADLINE, AccountData.KEY_LENT_STATUS,
				AccountData.KEY_LENT_BRANCH,
				AccountData.KEY_LENT_LENDING_BRANCH, AccountData.KEY_LENT_LINK };
		int copymap_num = copymap_keys.length;
		List<ContentValues> medien = new ArrayList<ContentValues>();

		Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
				.select("tr.tabKonto");

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

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
			if (e.containsKey(AccountData.KEY_LENT_DEADLINE)) {
				try {
					e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP,
							sdf.parse(
									e.getAsString(AccountData.KEY_LENT_DEADLINE))
									.getTime());
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
			medien.add(e);
		}
		assert (doc.select(".kontozeile_center table").get(0).select("tr")
				.size() > 0);
		assert (exemplartrs.size() == medien.size());

		copymap = data.getJSONArray("reservationtable");
		copymap_keys = new String[] { AccountData.KEY_RESERVATION_AUTHOR,
				AccountData.KEY_RESERVATION_TITLE,
				AccountData.KEY_RESERVATION_READY,
				AccountData.KEY_RESERVATION_BRANCH,
				AccountData.KEY_RESERVATION_CANCEL,
				AccountData.KEY_RESERVATION_EXPIRE };
		copymap_num = copymap_keys.length;

		List<ContentValues> reservations = new ArrayList<ContentValues>();
		exemplartrs = doc.select(".kontozeile_center table").get(1)
				.select("tr.tabKonto");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			ContentValues e = new ContentValues();

			for (int j = 0; j < copymap_num; j++) {
				try {
					if (copymap.getInt(j) > -1) {
						if (copymap_keys[j].equals("cancel")) {
							if (tr.child(copymap.getInt(j)).children().size() > 0) {
								e.put(copymap_keys[j],
										tr.child(copymap.getInt(j)).child(0)
												.attr("href"));
							}
						} else {
							e.put(copymap_keys[j], tr.child(copymap.getInt(j))
									.text());
						}
					}
				} catch (JSONException ex) {

				}
			}

			reservations.add(e);
		}
		assert (doc.select(".kontozeile_center table").get(1).select("tr")
				.size() > 0);
		assert (exemplartrs.size() == reservations.size());

		AccountData res = new AccountData(acc.getId());
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

		String html = "";

		if (acc.getName() == null || acc.getName().equals("null"))
			return null;

		// Login vonnöten
		HttpPost httppost = new HttpPost(opac_url + "/index.asp");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = ahc.execute(httppost);

		if (response.getStatusLine().getStatusCode() == 200) {
			// Login vonnöten
			response.getEntity().consumeContent();
			nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("AUSWEIS", acc.getName()));
			nameValuePairs
					.add(new BasicNameValuePair("PWD", acc.getPassword()));
			nameValuePairs.add(new BasicNameValuePair("B1", "weiter"));
			nameValuePairs.add(new BasicNameValuePair("target", "konto"));
			nameValuePairs.add(new BasicNameValuePair("type", "K"));
			html = httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs));
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			html = httpGet(opac_url + "/index.asp?target=konto");
		} else if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		return html;

	}

	@Override
	public String getShareUrl(String id, String title) {
		return "http://opacapp.de/:" + library.getIdent() + ":" + id + ":"
				+ title;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ACCOUNT_EXTENDABLE
				| SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option) {
		// TODO Auto-generated method stub
		return null;
	}
}
