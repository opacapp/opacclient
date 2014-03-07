/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
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
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
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
public class Bibliotheca extends BaseApi {

	protected String opac_url = "";
	protected JSONObject data;
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
		defaulttypes.put("cd", MediaType.CD);
		defaulttypes.put("cdromkl", MediaType.CD_SOFTWARE);
		defaulttypes.put("mcdroms", MediaType.CD);
		defaulttypes.put("ekl", MediaType.EBOOK);
		defaulttypes.put("emedium", MediaType.EBOOK);
		defaulttypes.put("monleihe", MediaType.EBOOK);
		defaulttypes.put("mdivis", MediaType.EBOOK);
		defaulttypes.put("mbmonos", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("mbuechers", MediaType.PACKAGE_BOOKS);
		defaulttypes.put("mdvds", MediaType.DVD);
		defaulttypes.put("mdvd", MediaType.DVD);
		defaulttypes.put("blu-ray--disc_s_35x35", MediaType.BLURAY);
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
		httpGet(opac_url + "/woload.asp?lkz=1&nextpage=" + db,
				getDefaultEncoding());

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
			nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
			String html = httpPost(opac_url + "/index.asp",
					new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
			metadata.close();
			extract_meta(html);
		} else {
			metadata.close();
		}
	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		super.init(metadata, lib);
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
		else
			res = res.trim();
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
				new UrlEncodedFormEntity(nameValuePairs), getDefaultEncoding());
		return parse_search(html, 1);
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		String html = httpGet(opac_url + "/index.asp?scrollAction=" + page,
				getDefaultEncoding());
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
			int contentindex = 1;
			if (tr.select("td a img").size() > 0) {
				String[] fparts = tr.select("td a img").get(0).attr("src")
						.split("/");
				String fname = fparts[fparts.length - 1];
				if (data.has("mediatypes")) {
					try {
						sr.setType(MediaType.valueOf(data.getJSONObject(
								"mediatypes").getString(fname)));
					} catch (JSONException e) {
						sr.setType(defaulttypes.get(fname
								.toLowerCase(Locale.GERMAN).replace(".jpg", "")
								.replace(".gif", "").replace(".png", "")));
					} catch (IllegalArgumentException e) {
						sr.setType(defaulttypes.get(fname
								.toLowerCase(Locale.GERMAN).replace(".jpg", "")
								.replace(".gif", "").replace(".png", "")));
					}
				} else {
					sr.setType(defaulttypes.get(fname
							.toLowerCase(Locale.GERMAN).replace(".jpg", "")
							.replace(".gif", "").replace(".png", "")));
				}
			} else {
				if (tr.children().size() == 3)
					contentindex = 2;
			}
			sr.setInnerhtml(tr.child(contentindex).child(0).html());

			sr.setNr(i);
			Element link = tr.child(contentindex).select("a").first();
			try {
				if (link != null && link.attr("href").contains("detmediennr")) {
					Uri uri = Uri.parse(link.attr("abs:href"));
					String nr = uri.getQueryParameter("detmediennr");
					if (Integer.parseInt(nr) > i + 1) {
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
			} catch (Exception e) {
			}
			try {
				Comment c = (Comment) tr.child(1).childNode(0);
				String comment = c.getData().trim();
				String id = comment.split(": ")[1];
				sr.setId(id);
			} catch (Exception e) {
				e.printStackTrace();
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
		String html = httpGet(opac_url + "/index.asp?MedienNr=" + a,
				getDefaultEncoding());
		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		String html = httpGet(opac_url + "/index.asp?detmediennr=" + nr,
				getDefaultEncoding());

		return parse_result(html);
	}

	protected DetailledItem parse_result(String html) {
		Document doc = Jsoup.parse(html);
		doc.setBaseUri(opac_url);

		DetailledItem result = new DetailledItem();

		if (doc.select(".detail_cover img").size() == 1) {
			result.setCover(doc.select(".detail_cover img").get(0).attr("src"));
		}

		result.setTitle(doc.select(".detail_titel").text());

		Elements detailtrs = doc.select(".detailzeile table tr");
		for (int i = 0; i < detailtrs.size(); i++) {
			Element tr = detailtrs.get(i);
			if (tr.child(0).hasClass("detail_feld")) {
				String title = tr.child(0).text();
				String content = tr.child(1).text();
				if (title.equals("Gesamtwerk:")) {
					try {
						if (tr.child(1).select("a").size() > 0) {
							Element link = tr.child(1).select("a").first();
							List<NameValuePair> query = URLEncodedUtils.parse(
									new URI(link.absUrl("href")), "UTF-8");
							for (NameValuePair q : query) {
								if (q.getName().equals("MedienNr")) {
									result.setCollectionId(q.getValue());
								}
							}
						}
					} catch (URISyntaxException e) {
					}
				} else {

					if (content.contains("hier klicken")
							&& tr.child(1).select("a").size() > 0) {
						content += " "
								+ tr.child(1).select("a").first().attr("href");
					}

					result.addDetail(new Detail(title, content));
				}
			}
		}

		Elements detailcenterlinks = doc
				.select(".detailzeile_center a.detail_link");
		for (int i = 0; i < detailcenterlinks.size(); i++) {
			Element a = detailcenterlinks.get(i);
			result.addDetail(new Detail(a.text().trim(), a.absUrl("href")));
		}

		try {
			JSONObject copymap = new JSONObject();
			if (data.has("copiestable")) {
				copymap = data.getJSONObject("copiestable");
			} else {
				Elements ths = doc.select(".exemplartab .exemplarmenubar th");
				for (int i = 0; i < ths.size(); i++) {
					Element th = ths.get(i);
					String head = th.text().trim();
					if (head.equals("Zweigstelle")) {
						copymap.put(DetailledItem.KEY_COPY_BRANCH, i);
					} else if (head.equals("Abteilung")) {
						copymap.put(DetailledItem.KEY_COPY_DEPARTMENT, i);
					} else if (head.equals("Bereich")) {
						copymap.put(DetailledItem.KEY_COPY_LOCATION, i);
					} else if (head.equals("Standort")) {
						copymap.put(DetailledItem.KEY_COPY_LOCATION, i);
					} else if (head.equals("Signatur")) {
						copymap.put(DetailledItem.KEY_COPY_SHELFMARK, i);
					} else if (head.equals("Barcode")
							|| head.equals("Medien-Nummer")) {
						copymap.put(DetailledItem.KEY_COPY_BARCODE, i);
					} else if (head.equals("Status")) {
						copymap.put(DetailledItem.KEY_COPY_STATUS, i);
					} else if (head.equals("Frist")
							|| head.matches("Verf.+gbar")) {
						copymap.put(DetailledItem.KEY_COPY_RETURN, i);
					} else if (head.equals("Vorbestellungen")
							|| head.equals("Reservierungen")) {
						copymap.put(DetailledItem.KEY_COPY_RESERVATIONS, i);
					}
				}
			}
			Elements exemplartrs = doc
					.select(".exemplartab .tabExemplar, .exemplartab .tabExemplar_");
			for (int i = 0; i < exemplartrs.size(); i++) {
				Element tr = exemplartrs.get(i);

				ContentValues e = new ContentValues();

				Iterator<?> keys = copymap.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					int index;
					try {
						index = copymap.has(key) ? copymap.getInt(key) : -1;
					} catch (JSONException e1) {
						index = -1;
					}
					if (index >= 0)
						e.put(key, tr.child(index).text());
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
	public ReservationResult reservation(DetailledItem item, Account acc,
			int useraction, String selection) throws IOException {
		String reservation_info = item.getReservation_info();
		String branch_inputfield = "zstauswahl";

		Document doc = null;

		if (useraction == ReservationResult.ACTION_CONFIRMATION) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs
					.add(new BasicNameValuePair("button1", "Bestaetigung"));
			nameValuePairs.add(new BasicNameValuePair("target", "makevorbest"));
			httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs), getDefaultEncoding());
			return new ReservationResult(MultiStepResult.Status.OK);
		} else if (selection == null || useraction == 0) {
			String html = httpGet(opac_url + "/" + reservation_info, getDefaultEncoding());
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
						new UrlEncodedFormEntity(nameValuePairs),
						getDefaultEncoding());
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
						MultiStepResult.Status.SELECTION_NEEDED);
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
					new UrlEncodedFormEntity(nameValuePairs),
					getDefaultEncoding());
			doc = Jsoup.parse(html);
		}

		if (doc == null)
			return new ReservationResult(MultiStepResult.Status.ERROR);

		if (doc.select("input[name=target]").size() > 0) {
			if (doc.select("input[name=target]").attr("value")
					.equals("makevorbest")) {
				List<String[]> details = new ArrayList<String[]>();

				if (doc.getElementsByClass("kontomeldung").size() == 1) {
					details.add(new String[] { doc
							.getElementsByClass("kontomeldung").get(0).text()
							.trim() });
				}
				Pattern p = Pattern.compile("geb.hr", Pattern.MULTILINE);
				for (Element div : doc.select(".kontozeile_center")) {
					String text = div.text();
					if (p.matcher(text).find() && !text.contains("usstehend")
							&& text.contains("orbestellung")) {
						details.add(new String[] { text.trim() });
					}
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
						MultiStepResult.Status.CONFIRMATION_NEEDED);
				result.setDetails(details);
				return result;
			}
		}

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			return new ReservationResult(MultiStepResult.Status.ERROR, doc
					.getElementsByClass("kontomeldung").get(0).text());
		}

		return new ReservationResult(MultiStepResult.Status.ERROR,
				"Unbekannter Fehler");
	}

	@Override
	public ProlongResult prolong(String a, Account account, int useraction,
			String selection) throws IOException, NotReachableException {
		if (!initialised)
			start();
		if (System.currentTimeMillis() - logged_in > SESSION_LIFETIME
				|| logged_in_as == null) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return new ProlongResult(MultiStepResult.Status.ERROR,
						"Konto konnte nicht geladen werden");
			}
		} else if (logged_in_as.getId() != account.getId()) {
			try {
				account(account);
			} catch (JSONException e) {
				e.printStackTrace();
				return new ProlongResult(MultiStepResult.Status.ERROR,
						"Konto konnte nicht geladen werden");
			}
		}

		if (useraction == ReservationResult.ACTION_CONFIRMATION) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("target", "make_vl"));
			nameValuePairs.add(new BasicNameValuePair("verlaengern",
					"Bestätigung"));
			httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
					nameValuePairs), getDefaultEncoding());

			return new ProlongResult(MultiStepResult.Status.OK);
		} else {

			String html = httpGet(opac_url + "/" + a, getDefaultEncoding());
			Document doc = Jsoup.parse(html);

			if (doc.getElementsByClass("kontomeldung").size() == 1) {
				return new ProlongResult(MultiStepResult.Status.ERROR, doc
						.getElementsByClass("kontomeldung").get(0).text());
			}
			if (doc.select("#verlaengern").size() == 1) {
				if (doc.select(".kontozeile_center table").size() == 1) {
					Element table = doc.select(".kontozeile_center table")
							.first();
					ProlongResult res = new ProlongResult(
							MultiStepResult.Status.CONFIRMATION_NEEDED);
					List<String[]> details = new ArrayList<String[]>();

					for (Element row : table.select("tr")) {
						if (row.select(".konto_feld").size() == 1
								&& row.select(".konto_feldinhalt").size() == 1) {
							details.add(new String[] {
									row.select(".konto_feld").text().trim(),
									row.select(".konto_feldinhalt").text()
											.trim() });
						}
					}
					res.setDetails(details);
					return res;
				} else {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
							2);
					nameValuePairs.add(new BasicNameValuePair("target",
							"make_vl"));
					nameValuePairs.add(new BasicNameValuePair("verlaengern",
							"Bestätigung"));
					httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
							nameValuePairs), getDefaultEncoding());

					return new ProlongResult(MultiStepResult.Status.OK);
				}
			}
		}
		return new ProlongResult(MultiStepResult.Status.ERROR, "??");
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
		String html = httpGet(opac_url + "/index.asp?target=alleverl", getDefaultEncoding());
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
		httpGet(opac_url + "/" + a, getDefaultEncoding());

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("target", "delvorbest"));
		nameValuePairs
				.add(new BasicNameValuePair("vorbdelbest", "Bestätigung"));
		httpPost(opac_url + "/index.asp", new UrlEncodedFormEntity(
				nameValuePairs), getDefaultEncoding());
		return true;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, SocketException {
		if (!initialised)
			start();

		if (acc.getName() == null || acc.getName().equals("null"))
			return null;

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("link_konto.x", "0"));
		nameValuePairs.add(new BasicNameValuePair("link_konto.y", "0"));
		String html = httpPost(opac_url + "/index.asp",
				new UrlEncodedFormEntity(nameValuePairs), "ISO-8859-1");
		Document doc = Jsoup.parse(html);

		if (doc.select("input[name=AUSWEIS]").size() > 0) {
			// Login vonnöten
			nameValuePairs = new ArrayList<NameValuePair>();
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
					nameValuePairs), "ISO-8859-1", true);
			doc = Jsoup.parse(html);
		}
		// } else if (response.getStatusLine().getStatusCode() == 302) {
		// Bereits eingeloggt
		// html = httpGet(opac_url + "/index.asp?target=konto",
		// "ISO-8859-1",
		// true);
		// } else if (response.getStatusLine().getStatusCode() >= 400) {
		// throw new NotReachableException();
		// }

		if (doc.getElementsByClass("kontomeldung").size() == 1) {
			last_error = doc.getElementsByClass("kontomeldung").get(0).text();
			return null;
		}

		logged_in = System.currentTimeMillis();
		logged_in_as = acc;

		JSONObject copymap = null;

		copymap = data.getJSONObject("accounttable");

		List<ContentValues> medien = new ArrayList<ContentValues>();

		if (doc.select(".kontozeile_center table").size() == 0)
			return null;

		Elements exemplartrs = doc.select(".kontozeile_center table").get(0)
				.select("tr.tabKonto");

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			ContentValues e = new ContentValues();

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
					if (key.equals(AccountData.KEY_LENT_LINK)) {
						if (tr.child(index).children().size() > 0)
							e.put(key, tr.child(index).child(0).attr("href"));
					} else {
						e.put(key, tr.child(index).text());
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

		copymap = data.getJSONObject("reservationtable");

		List<ContentValues> reservations = new ArrayList<ContentValues>();
		exemplartrs = doc.select(".kontozeile_center table").get(1)
				.select("tr.tabKonto");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			ContentValues e = new ContentValues();

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
					if (key.equals(AccountData.KEY_RESERVATION_CANCEL)) {
						if (tr.child(index).children().size() > 0)
							e.put(key, tr.child(index).child(0).attr("href"));
					} else {
						e.put(key, tr.child(index).text());
					}
				}
			}

			reservations.add(e);
		}
		assert (doc.select(".kontozeile_center table").get(1).select("tr")
				.size() > 0);
		assert (exemplartrs.size() == reservations.size());

		AccountData res = new AccountData(acc.getId());

		for (Element row : doc.select(".kontozeile_center")) {
			String text = row.text().trim();
			if (text.matches("Ausstehende Geb.+hren:[^0-9]+([0-9.,]+)[^0-9€A-Z]*(€|EUR|CHF|Fr.)")) {
				text = text
						.replaceAll(
								"Ausstehende Geb.+hren:[^0-9]+([0-9.,]+)[^0-9€A-Z]*(€|EUR|CHF|Fr.)",
								"$1 $2");
				res.setPendingFees(text);
			}
			if (text.matches("Ihr Ausweis ist g.ltig bis:.*")) {
				text = text.replaceAll(
						"Ihr Ausweis ist g.ltig bis:[^A-Za-z0-9]+", "");
				res.setValidUntil(text);
			} else if (text.matches("Ausweis g.ltig bis:.*")) {
				text = text.replaceAll("Ausweis g.ltig bis:[^A-Za-z0-9]+", "");
				res.setValidUntil(text);
			}
		}

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
		HttpResponse response = http_client.execute(httppost);

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
					nameValuePairs), getDefaultEncoding());
		} else if (response.getStatusLine().getStatusCode() == 302) {
			// Bereits eingeloggt
			response.getEntity().consumeContent();
			html = httpGet(opac_url + "/index.asp?target=konto",
					getDefaultEncoding());
		} else if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		return html;

	}

	@Override
	public String getShareUrl(String id, String title) {
		try {
			return "http://opacapp.de/:" + library.getIdent() + ":" + id + ":"
					+ URLEncoder.encode(title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "http://opacapp.de/:" + library.getIdent() + ":" + id + ":"
					+ title;
		}
	}

	@Override
	public int getSupportFlags() {
		int flags = SUPPORT_FLAG_ACCOUNT_EXTENDABLE;
		if (!data.has("disableProlongAll")) {
			flags |= SUPPORT_FLAG_ACCOUNT_PROLONG_ALL;
		}
		return flags;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option) {
		// TODO Auto-generated method stub
		return null;
	}
}
