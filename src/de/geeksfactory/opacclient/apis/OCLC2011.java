package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
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
import android.util.Log;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult;
import de.geeksfactory.opacclient.apis.OpacApi.ReservationResult.Status;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * Api für Web-Opacs zu SunRise von OCLC. Online nur zu erkennen an
 * "Copyright 2009/2011 OCLC"
 * 
 * TODO: Vorbestellen - Ansicht der Vorbestellungen - Verlängern
 * 
 * Einschränkungen: Die Merkliste wird nur in solchen Bibliotheken gut
 * unterstützt, die die Erweiterung Bibtip einsetzen. In anderen kann nur der
 * Titel in der Merkliste gespeichert werden.
 * 
 */
public class OCLC2011 implements OpacApi {

	/*
	 * OpacApi für WebOpacs "Copyright 2011 OCLC" (echter Name der Software
	 * scheint SISIS zu sein) z.B. Bremen TODO - Vorbestellen - Account
	 */

	/*
	 * setzt aktuell voraus, dass die Bibliothek die Bibtip Extension benutzt!
	 * Wir wissen nicht, wie wir anderweitig an die IDs der Medien kommen.
	 */
	private String opac_url = "";
	private String results;
	private JSONObject data;
	private DefaultHttpClient ahc;
	private MetaDataSource metadata;
	private boolean initialised = false;
	private String last_error;
	private Library library;

	private String CSId;
	private String identifier;
	private String reusehtml;
	private String last_id;
	private int resultcount = 10;

	private long logged_in;
	private Account logged_in_as;
	private final long SESSION_LIFETIME = 1000 * 60 * 3;

	@Override
	public String getResults() {
		return results;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA, KEY_SEARCH_QUERY_KEYWORDB,
				KEY_SEARCH_QUERY_BRANCH, KEY_SEARCH_QUERY_ISBN,
				KEY_SEARCH_QUERY_YEAR, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_AUDIENCE, KEY_SEARCH_QUERY_PUBLISHER };
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
		metadata.open();
		metadata.clearMeta(library.getIdent());
		for (int i = 0; i < zst_opts.size(); i++) {
			Element opt = zst_opts.get(i);
			if (!opt.val().equals(""))
				metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
						library.getIdent(), opt.val(), opt.text());
		}

		metadata.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {

		// Some libraries require start parameters for start.do, like Login=foo
		String startparams = "";
		if (data.has("startparams")) {
			try {
				startparams = "?" + data.getString("startparams");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		HttpGet httpget = new HttpGet(opac_url + "/start.do" + startparams);
		HttpResponse response = ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		initialised = true;

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		Document doc = Jsoup.parse(html);
		CSId = doc.select("input[name=CSId]").val();

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
		HttpProtocolParams.setUserAgent(ahc.getParams(), "OpacApp.de");

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

			index = addParameters(query, KEY_SEARCH_QUERY_TITLE, "331", params,
					index);
			index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "100",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_ISBN, "540", params,
					index);
			index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA, "902",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDB, "710",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_YEAR, "425", params,
					index);
			index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, "412",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM, "700",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_AUDIENCE, "1001",
					params, index);

			if (index > 4) {
				last_error = "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.";
				return null;
			}
			if (index == 0) {
				last_error = "Es wurden keine Suchkriterien eingegeben.";
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
				+ "&curPos=" + (((page - 1) * resultcount) + 1));
		HttpResponse response = ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return parse_search(html);
	}

	private List<SearchResult> parse_search(String html) {
		Document doc = Jsoup.parse(html);

		if (doc.select(".error").size() > 0) {
			last_error = doc.select(".error").text().trim();
			return null;
		} else if (doc.select(".nohits").size() > 0) {
			last_error = doc.select(".nohits").text().trim();
			return null;
		}

		this.results = doc.select(".box-header h2").first().text();
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
			if (tr.select("td a img").size() > 0) {
				String[] fparts = tr.select("td a img").get(0).attr("src")
						.split("/");
				sr.setType(fparts[fparts.length - 1].replace(".jpg", ".png")
						.replace(".gif", ".png").toLowerCase());
			}

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
								// Not that important
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
		resultcount = results.size();
		return results;
	}

	@Override
	public DetailledItem getResultById(String a) throws IOException,
			NotReachableException {

		if (a == null && reusehtml != null) {
			return parse_result(reusehtml);
		}

		start();

		HttpGet httpget = new HttpGet(opac_url
				+ "/search.do?methodToCall=quickSearch&Kateg=0&Content=" + a
				+ "&fbt=" + a);

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

		httpget = new HttpGet(
				opac_url
						+ "/singleHit.do?methodToCall=activateTab&tab=showAvailabilityActive");

		response = ahc.execute(httpget);
		String html3 = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		Document doc3 = Jsoup.parse(html3);
		doc3.setBaseUri(opac_url);

		DetailledItem result = new DetailledItem();

		try {
			result.setId(doc.select("#bibtip_id").text().trim());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		List<String> reservationlinks = new ArrayList<String>();
		for (Element link : doc3.select("#tab-content a")) {
			Uri href = Uri.parse(link.absUrl("href"));
			if (result.getId() == null) {
				// ID retrieval
				String key = href.getQueryParameter("katkey");
				if (key != null) {
					result.setId(key);
				}
			}

			// Vormerken
			if (href.getQueryParameter("methodToCall") != null) {
				if (href.getQueryParameter("methodToCall").equals(
						"doVormerkung")
						|| href.getQueryParameter("methodToCall").equals(
								"doBestellung"))
					reservationlinks.add(href.getQuery());
			}
		}
		if (reservationlinks.size() == 1) {
			result.setReservable(true);
			result.setReservation_info(reservationlinks.get(0));
		} else if (reservationlinks.size() == 0) {
			result.setReservable(false);
		} else {
			// TODO: Multiple options - handle this case!
		}

		if (doc.select(".data td img").size() == 1) {
			result.setCover(doc.select(".data td img").first().attr("abs:src"));
		}

		result.setTitle(doc.select(".data td strong").first().text());

		String title = "";
		String text = "";
		Element detailtrs = doc2.select("#tab-content .data td").first();
		for (Node node : detailtrs.childNodes()) {
			if (node instanceof Element) {
				if (((Element) node).tag().getName().equals("strong")) {
					if (!text.equals("") && !title.equals("")) {
						result.addDetail(new Detail(title.trim(), text.trim()));
						if (title.equals("Titel:")) {
							result.setTitle(text.trim());
						}
						text = "";
					}

					title = ((Element) node).text().trim();
				} else {
					text = text + ((Element) node).text();
				}
			} else if (node instanceof Element) {
				text = text + ((Element) node).text();
			} else if (node instanceof TextNode) {
				text = text + ((TextNode) node).text();
			}
		}
		if (!text.equals("") && !title.equals("")) {
			result.addDetail(new Detail(title.trim(), text.trim()));
			if (title.equals("Titel:")) {
				result.setTitle(text.trim());
			}
		}

		Elements exemplartrs = doc.select("#tab-content .data tr:not(#bg2)");
		for (int i = 0; i < exemplartrs.size(); i++) {
			Element tr = exemplartrs.get(i);
			try {
				ContentValues e = new ContentValues();
				e.put(DetailledItem.KEY_COPY_BARCODE, tr.child(1).text().trim());
				e.put(DetailledItem.KEY_COPY_BRANCH, tr.child(3).text().trim());
				e.put(DetailledItem.KEY_COPY_STATUS, tr.child(4).text().trim());
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

		return result;
	}

	@Override
	public ReservationResult reservation(String reservation_info, Account acc,
			int useraction, String selection) throws IOException {
		final String branch_inputfield = "issuepoint";

		HttpPost httppost;
		HttpGet httpget;
		HttpResponse response;
		Document doc = null;

		String action = "reservation";
		if (reservation_info.contains("doBestellung")) {
			action = "order";
		}
		Log.i("res", "info = " + reservation_info + " action = " + useraction
				+ " selection = " + selection);

		if (useraction == ReservationResult.ACTION_CONFIRMATION) {
			httppost = new HttpPost(opac_url + "/" + action + ".do");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("methodToCall", action));
			nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);

			String html = convertStreamToString(response.getEntity()
					.getContent());
			doc = Jsoup.parse(html);
		} else if (selection == null || useraction == 0) {
			httpget = new HttpGet(opac_url + "/availability.do?"
					+ reservation_info);

			response = ahc.execute(httpget);
			String html = convertStreamToString(response.getEntity()
					.getContent());
			response.getEntity().consumeContent();
			doc = Jsoup.parse(html);

			if (doc.select("input[name=username]").size() > 0) {
				// Login vonnöten
				httppost = new HttpPost(opac_url + "/login.do");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("username", acc
						.getName()));
				nameValuePairs.add(new BasicNameValuePair("password", acc
						.getPassword()));
				nameValuePairs.add(new BasicNameValuePair("methodToCall",
						"submit"));
				nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
				nameValuePairs.add(new BasicNameValuePair("login_action",
						"Login"));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				response = ahc.execute(httppost);

				html = convertStreamToString(response.getEntity().getContent());
				doc = Jsoup.parse(html);

				if (doc.getElementsByClass("error").size() == 0) {
					logged_in = System.currentTimeMillis();
					logged_in_as = acc;
				}
			}
			if (doc.select("input[name=" + branch_inputfield + "]").size() > 0) {
				ContentValues branches = new ContentValues();
				for (Element option : doc
						.select("input[name=" + branch_inputfield + "]")
						.first().parent().parent().parent().select("td")) {
					if (option.select("input").size() != 1)
						continue;
					String value = option.text().trim();
					String key = option.select("input").val();
					branches.put(key, value);
				}
				ReservationResult result = new ReservationResult(
						Status.SELECTION_NEEDED);
				result.setActionIdentifier(ReservationResult.ACTION_BRANCH);
				result.setSelection(branches);
				return result;
			}
		} else if (useraction == ReservationResult.ACTION_BRANCH) {
			httppost = new HttpPost(opac_url + "/" + action + ".do");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair(branch_inputfield,
					selection));
			nameValuePairs.add(new BasicNameValuePair("methodToCall", action));
			nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = ahc.execute(httppost);

			String html = convertStreamToString(response.getEntity()
					.getContent());
			doc = Jsoup.parse(html);
		}

		if (doc == null)
			return new ReservationResult(Status.ERROR);

		if (doc.getElementsByClass("error").size() >= 1) {
			last_error = doc.getElementsByClass("error").get(0).text();
			return new ReservationResult(Status.ERROR);
		}

		if (doc.select("#CirculationForm p").size() > 0) {
			List<String[]> details = new ArrayList<String[]>();
			for (String row : doc.select("#CirculationForm p").first().html()
					.split("<br />")) {
				Document frag = Jsoup.parseBodyFragment(row);
				if (frag.text().contains(":")) {
					String[] split = frag.text().split(":");
					if (split.length >= 2)
						details.add(new String[] { split[0].trim() + ":",
								split[1].trim() });
				} else {
					details.add(new String[] { "", frag.text().trim() });
				}
			}
			ReservationResult result = new ReservationResult(
					Status.CONFIRMATION_NEEDED);
			result.setDetails(details);
			return result;
		}

		return new ReservationResult(Status.OK);
	}

	@Override
	public boolean prolong(Account account, String a) throws IOException,
			NotReachableException {
		// Internal convention: a is either a § followed by an error message or
		// the URI of the page this item was found on and the query string the
		// prolonging link links to, seperated by a $.
		if (a.startsWith("§")) {
			last_error = a.substring(1);
			return false;
		}

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

		Log.i("prolong", a);

		// We have to call the page we originally found the link on first...
		HttpGet httpget = new HttpGet(a.split("\\$")[0]);
		HttpResponse response = ahc.execute(httpget);
		response.getEntity().consumeContent();

		httpget = new HttpGet(opac_url + "/userAccount.do?" + a.split("\\$")[1]);
		response = ahc.execute(httpget);
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		Log.i("html", html);
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

		// We have to call the page we originally found the link on first...
		HttpGet httpget = new HttpGet(a.split("\\$")[0]);
		HttpResponse response = ahc.execute(httpget);
		response.getEntity().consumeContent();

		httpget = new HttpGet(opac_url + "/userAccount.do?" + a.split("\\$")[1]);
		response = ahc.execute(httpget);
		response.getEntity().consumeContent();
		return true;
	}

	private boolean login(Account acc) {
		HttpPost httppost = new HttpPost(opac_url + "/login.do");

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("username", acc.getName()));
		nameValuePairs
				.add(new BasicNameValuePair("password", acc.getPassword()));
		nameValuePairs.add(new BasicNameValuePair("CSId", CSId));
		nameValuePairs.add(new BasicNameValuePair("methodToCall", "submit"));
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		HttpResponse response;
		String html;
		try {
			response = ahc.execute(httppost);
			html = convertStreamToString(response.getEntity().getContent());
			response.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		Document doc = Jsoup.parse(html);

		if (doc.getElementsByClass("error").size() > 0) {
			last_error = doc.getElementsByClass("error").get(0).text();
			return false;
		}

		logged_in = System.currentTimeMillis();
		logged_in_as = acc;

		return true;
	}

	private void parse_medialist(String url, List<ContentValues> medien,
			String html, int page) throws ClientProtocolException, IOException {
		Document doc = Jsoup.parse(html);
		Elements copytrs = doc.select(".data tr");
		doc.setBaseUri(opac_url);

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

		int trs = copytrs.size();
		if (trs == 1)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			ContentValues e = new ContentValues();

			if (tr.text().contains("keine Daten")) {
				return;
			}

			e.put(AccountData.KEY_LENT_TITLE, tr.child(1).select("strong")
					.text().trim());
			try {
				e.put(AccountData.KEY_LENT_AUTHOR,
						tr.child(1).html().split("<br />")[1].trim());

				String frist = tr.child(2).html().split("<br />")[0].trim();
				if (frist.contains("-"))
					frist = frist.split("-")[1].trim();
				e.put(AccountData.KEY_LENT_DEADLINE, frist);
				e.put(AccountData.KEY_LENT_BRANCH,
						tr.child(2).html().split("<br />")[1].trim());

				if (!frist.equals("")) {
					try {
						e.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP,
								sdf.parse(
										e.getAsString(AccountData.KEY_LENT_DEADLINE))
										.getTime());
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
				}

				if (tr.select("a").size() > 0) {
					for (Element link : tr.select("a")) {
						Uri uri = Uri.parse(link.attr("abs:href"));
						if (uri.getQueryParameter("methodToCall").equals(
								"renewalPossible")) {
							e.put(AccountData.KEY_LENT_LINK,
									url + "$" + uri.getQuery());
							break;
						}
					}
				} else if (tr.select(".textrot").size() == 1) {
					e.put(AccountData.KEY_LENT_LINK, "§"
							+ tr.select(".textrot").text());
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}

			medien.add(e);
		}
		assert(medien.size() == trs-1);

		for (Element link : doc.select(".box-right a")) {
			if (link.text().contains("»")) {
				HttpGet httpget = new HttpGet(link.attr("abs:href"));
				HttpResponse response2 = ahc.execute(httpget);

				String html2 = convertStreamToString(response2.getEntity()
						.getContent());

				parse_medialist(link.attr("abs:href"), medien, html2, page + 1);
				break;
			}
		}

	}

	private void parse_reslist(String url, List<ContentValues> reservations,
			String html, int page) throws ClientProtocolException, IOException {
		Document doc = Jsoup.parse(html);
		Elements copytrs = doc.select(".data tr");
		doc.setBaseUri(opac_url);

		int trs = copytrs.size();
		if (trs == 1)
			return;
		assert (trs > 0);
		for (int i = 1; i < trs; i++) {
			Element tr = copytrs.get(i);
			ContentValues e = new ContentValues();

			if (tr.text().contains("keine Daten")) {
				return;
			}

			e.put(AccountData.KEY_RESERVATION_TITLE,
					tr.child(1).select("strong").text().trim());
			try {
				e.put(AccountData.KEY_RESERVATION_AUTHOR, tr.child(1).html()
						.split("<br />")[1].trim());

				e.put(AccountData.KEY_RESERVATION_BRANCH, tr.child(2).html()
						.split("<br />")[2].trim());

				if (tr.select("a").size() == 1 && page == 0)
					e.put(AccountData.KEY_RESERVATION_CANCEL, url
							+ "$"
							+ Uri.parse(tr.select("a").attr("abs:href"))
									.getQuery());

			} catch (Exception ex) {
				ex.printStackTrace();
			}

			reservations.add(e);
		}
		assert(reservations.size() == trs-1);

		for (Element link : doc.select(".box-right a")) {
			if (link.text().contains("»")) {
				HttpGet httpget = new HttpGet(link.attr("abs:href"));
				HttpResponse response2 = ahc.execute(httpget);

				String html2 = convertStreamToString(response2.getEntity()
						.getContent());

				parse_reslist(link.attr("abs:href"), reservations, html2,
						page + 1);
				break;
			}
		}

	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, SocketException {
		start(); // TODO: Is this necessary?

		if (!login(acc))
			return null;

		// Geliehene Medien
		HttpGet httpget = new HttpGet(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=1");
		HttpResponse response2 = ahc.execute(httpget);
		String html2 = convertStreamToString(response2.getEntity().getContent());
		List<ContentValues> medien = new ArrayList<ContentValues>();
		parse_medialist(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=1", medien,
				html2, 0);

		// Bestellte Medien
		httpget = new HttpGet(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=6");
		response2 = ahc.execute(httpget);
		html2 = convertStreamToString(response2.getEntity().getContent());
		List<ContentValues> reserved = new ArrayList<ContentValues>();
		parse_reslist(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=6", reserved,
				html2, 0);

		// Vorgemerkte Medien
		httpget = new HttpGet(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=7");
		response2 = ahc.execute(httpget);
		html2 = convertStreamToString(response2.getEntity().getContent());
		parse_reslist(opac_url
				+ "/userAccount.do?methodToCall=showAccount&typ=7", reserved,
				html2, 0);

		AccountData res = new AccountData(acc.getId());
		res.setLent(medien);
		res.setReservations(reserved);
		return res;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return true;
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
}
