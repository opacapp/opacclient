package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.apis.OpacApi.MultiStepResult.Status;
import de.geeksfactory.opacclient.i18n.StringProvider;
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
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class Adis extends BaseApi implements OpacApi {

	protected String opac_url = "";
	protected JSONObject data;
	protected Library library;

	protected int s_requestCount = 0;
	protected String s_service;
	protected String s_sid;
	protected String s_exts;
	protected String s_alink;
	protected List<NameValuePair> s_pageform;
	protected int s_lastpage;
	protected Document s_reusedoc;
	protected boolean s_accountcalled;

	protected static HashMap<String, MediaType> types = new HashMap<String, MediaType>();
	protected static HashSet<String> ignoredFieldNames = new HashSet<String>();

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
		types.put("Zeitschriftenheft", MediaType.MAGAZINE);
		types.put("Zeitung", MediaType.NEWSPAPER);
		types.put("Beitrag E-Book", MediaType.EBOOK);
		types.put("Elektronische Ressource", MediaType.EBOOK);
		types.put("E-Book", MediaType.EBOOK);
		types.put("Karte", MediaType.MAP);

		// TODO: The following fields from Berlin make no sense and don't work
		// when they are displayed alone.
		// We can only include them if we automatically deselect the "Verbund"
		// checkbox
		// when one of these dropdowns has a value other than "".
		ignoredFieldNames.add("oder Bezirk");
		ignoredFieldNames.add("oder Bibliothek");
	}

	public Document htmlGet(String url) throws ClientProtocolException,
			IOException {

		if (!url.contains("requestCount")) {
			url = url + (url.contains("?") ? "&" : "?") + "requestCount="
					+ s_requestCount;
		}

		HttpGet httpget = new HttpGet(cleanUrl(url));
		HttpResponse response;

		try {
			response = http_client.execute(httpget);
		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (NoHttpResponseException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (MalformedChunkCodingException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (javax.net.ssl.SSLPeerUnverifiedException e) {
			// TODO: Handly this well
			throw e;
		} catch (javax.net.ssl.SSLException e) {
			// TODO: Handly this well
			// Can be "Not trusted server certificate" or can be a
			// aborted/interrupted handshake/connection
			if (e.getMessage().contains("timed out")
					|| e.getMessage().contains("reset by")) {
				e.printStackTrace();
				throw new NotReachableException();
			} else {
				throw e;
			}
		} catch (InterruptedIOException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (IOException e) {
			if (e.getMessage().contains("Request aborted")) {
				e.printStackTrace();
				throw new NotReachableException();
			} else {
				throw e;
			}
		}

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

		try {
			response = http_client.execute(httppost);

		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (NoHttpResponseException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (MalformedChunkCodingException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (javax.net.ssl.SSLPeerUnverifiedException e) {
			// TODO: Handle this well
			throw e;
		} catch (javax.net.ssl.SSLException e) {
			// TODO: Handly this well
			// Can be "Not trusted server certificate" or can be a
			// aborted/interrupted handshake/connection
			if (e.getMessage().contains("timed out")
					|| e.getMessage().contains("reset by")) {
				e.printStackTrace();
				throw new NotReachableException();
			} else {
				throw e;
			}
		} catch (InterruptedIOException e) {
			e.printStackTrace();
			throw new NotReachableException();
		} catch (IOException e) {
			if (e.getMessage().contains("Request aborted")) {
				e.printStackTrace();
				throw new NotReachableException();
			} else {
				throw e;
			}
		}

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
		s_accountcalled = false;

		try {
			Document doc = htmlGet(opac_url + "?"
					+ data.getString("startparams"));

			Pattern padSid = Pattern
					.compile(".*;jsessionid=([0-9A-Fa-f]+)[^0-9A-Fa-f].*");
			for (Element navitem : doc.select("#unav li a")) {
				if (navitem.attr("href").contains("service=")) {
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
			if (s_exts == null)
				s_exts = "SS6";

		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		super.start();
	}

	@Override
	protected String getDefaultEncoding() {
		return "UTF-8";
	}

	@Override
	public SearchRequestResult search(List<SearchQuery> queries)
			throws IOException, NotReachableException, OpacErrorException {
		start();
		// TODO: There are also libraries with a different search form,
		// s_exts=SS2 instead of s_exts=SS6
		// e.g. munich. Treat them differently!
		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=" + s_exts);

		int cnt = 0;
		List<NameValuePair> nvpairs = new ArrayList<NameValuePair>();
		for (SearchQuery query : queries) {
			if (!query.getValue().equals("")) {

				if (query.getSearchField() instanceof DropdownSearchField) {
					doc.select("select#" + query.getKey())
							.val(query.getValue());
					continue;
				}

				cnt++;

				if (s_exts.equals("SS2")
						|| (query.getSearchField().getData() != null && !query
								.getSearchField().getData()
								.optBoolean("selectable", true))) {
					doc.select("input#" + query.getKey()).val(query.getValue());
				} else {
					doc.select("select#SUCH01_" + cnt).val(query.getKey());
					doc.select("input#FELD01_" + cnt).val(query.getValue());
				}

				if (cnt > 4) {
					throw new OpacErrorException(
							stringProvider.getFormattedString(
									StringProvider.LIMITED_NUM_OF_CRITERIA, 4));
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
					stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
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
			else if (typetext.contains("+")
					&& types.containsKey(typetext.split("\\+")[0].trim()))
				res.setType(types.get(typetext.split("\\+")[0].trim()));

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
	public void init(Library library) {
		super.init(library);
		this.library = library;
		this.data = library.getData();
		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			throw new RuntimeException(e);
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

			List<NameValuePair> form = new ArrayList<NameValuePair>();
			for (Element input : doc.select("input, select")) {
				if (!"image".equals(input.attr("type"))
						&& !"submit".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))
						&& !"selected".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			form.add(new BasicNameValuePair("selected", "ZTEXT       " + id));
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
			// Yep, two times.
		}
		DetailledItem res = new DetailledItem();

		if (doc.select("#R001 img").size() == 1) {
			res.setCover(doc.select("#R001 img").first().absUrl("src"));
		}

		for (Element tr : doc.select("#R06 .aDISListe table tbody tr")) {
			if (tr.children().size() < 2)
				continue;
			if (tr.child(1).text().contains("hier klicken")) {
				res.addDetail(new Detail(tr.child(0).text().trim(), tr.child(1)
						.select("a").first().absUrl("href")));
			} else {
				res.addDetail(new Detail(tr.child(0).text().trim(), tr.child(1)
						.text().trim()));
			}

			if (tr.child(0).text().trim().contains("Titel")
					&& res.getTitle() == null) {
				res.setTitle(tr.child(1).text().split("[:/;]")[0].trim());
			}
		}

		if (doc.select("input[value*=Reservieren], input[value*=Vormerken]")
				.size() > 0) {
			res.setReservable(true);
			res.setReservation_info(id);
		}

		Map<Integer, String> colmap = new HashMap<Integer, String>();
		int i = 0;
		for (Element th : doc.select("#R08 table.rTable_table thead tr th")) {
			String head = th.text().trim();
			if (head.contains("Bibliothek")) {
				colmap.put(i, DetailledItem.KEY_COPY_BRANCH);
			} else if (head.contains("Standort")) {
				colmap.put(i, DetailledItem.KEY_COPY_LOCATION);
			} else if (head.contains("Signatur")) {
				colmap.put(i, DetailledItem.KEY_COPY_SHELFMARK);
			} else if (head.contains("Status") || head.contains("Hinweis")
					|| head.matches(".*Verf.+gbarkeit.*")) {
				colmap.put(i, DetailledItem.KEY_COPY_STATUS);
			}
			i++;
		}

		for (Element tr : doc.select("#R08 table.rTable_table tbody tr")) {
			Map<String, String> line = new HashMap<String, String>();
			for (Entry<Integer, String> entry : colmap.entrySet()) {
				if (entry.getValue().equals(DetailledItem.KEY_COPY_STATUS)) {
					String status = tr.child(entry.getKey()).text().trim();
					if (status.contains(" am: ")) {
						line.put(DetailledItem.KEY_COPY_STATUS,
								status.split("-")[0]);
						line.put(DetailledItem.KEY_COPY_RETURN,
								status.split(": ")[1]);
					} else {
						line.put(DetailledItem.KEY_COPY_STATUS, status);
					}
				} else {
					line.put(entry.getValue(), tr.child(entry.getKey()).text()
							.trim());
				}
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

		Document doc;
		List<NameValuePair> nvpairs;
		ReservationResult res = null;

		if (selection != null && selection.equals(""))
			selection = null;

		if (s_pageform == null)
			return new ReservationResult(Status.ERROR);

		// Load details
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
		nvpairs.add(new BasicNameValuePair("selected", "ZTEXT       "
				+ item.getReservation_info()));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs);
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs); // Yep, two
																	// times.

		List<NameValuePair> form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& (!"submit".equals(input.attr("type"))
							|| input.val().contains("Reservieren") || input
							.val().contains("Vormerken"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
		if (doc.select(".message h1").size() > 0) {
			String msg = doc.select(".message h1").text().trim();
			res = new ReservationResult(MultiStepResult.Status.ERROR, msg);
			form = new ArrayList<NameValuePair>();
			for (Element input : doc.select("input")) {
				if (!"image".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
		} else {
			try {
				doc = handleLoginForm(doc, account);
			} catch (OpacErrorException e1) {
				return new ReservationResult(MultiStepResult.Status.ERROR,
						e1.getMessage());
			}

			if (useraction == 0 && selection == null
					&& doc.select("#AUSGAB_1").size() == 0) {
				res = new ReservationResult(
						MultiStepResult.Status.CONFIRMATION_NEEDED);
				List<String[]> details = new ArrayList<String[]>();
				details.add(new String[] { doc.select("#F23").text() });
				res.setDetails(details);
			} else if (doc.select("#AUSGAB_1").size() > 0 && selection == null) {
				Map<String, String> sel = new HashMap<String, String>();
				for (Element opt : doc.select("#AUSGAB_1 option")) {
					if (opt.text().trim().length() > 0)
						sel.put(opt.val(), opt.text());
				}
				res = new ReservationResult(
						MultiStepResult.Status.SELECTION_NEEDED, doc.select(
								"#F23").text());
				res.setSelection(sel);
			} else if (selection != null || doc.select("#AUSGAB_1").size() == 0) {
				if (doc.select("#AUSGAB_1").size() > 0) {
					doc.select("#AUSGAB_1").attr("value", selection);
				}
				form = new ArrayList<NameValuePair>();
				for (Element input : doc.select("input, select")) {
					if (!"image".equals(input.attr("type"))
							&& !"submit".equals(input.attr("type"))
							&& !"checkbox".equals(input.attr("type"))
							&& !"".equals(input.attr("name"))) {
						form.add(new BasicNameValuePair(input.attr("name"),
								input.attr("value")));
					}
				}
				form.add(new BasicNameValuePair("textButton",
						"Reservation abschicken"));
				res = new ReservationResult(MultiStepResult.Status.OK);
				doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

				if (doc.select(".message h1").size() > 0) {
					String msg = doc.select(".message h1").text().trim();
					form = new ArrayList<NameValuePair>();
					for (Element input : doc.select("input")) {
						if (!"image".equals(input.attr("type"))
								&& !"checkbox".equals(input.attr("type"))
								&& !"".equals(input.attr("name"))) {
							form.add(new BasicNameValuePair(input.attr("name"),
									input.attr("value")));
						}
					}
					doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
					if (!msg.contains("Reservation ist erfolgt")) {
						res = new ReservationResult(
								MultiStepResult.Status.ERROR, msg);
					} else {
						res = new ReservationResult(MultiStepResult.Status.OK,
								msg);
					}
				} else if (doc.select("#R01").text()
						.contains("Informationen zu Ihrer Reservation")) {
					String msg = doc.select("#OPACLI").text().trim();
					form = new ArrayList<NameValuePair>();
					for (Element input : doc.select("input")) {
						if (!"image".equals(input.attr("type"))
								&& !"checkbox".equals(input.attr("type"))
								&& !"".equals(input.attr("name"))) {
							form.add(new BasicNameValuePair(input.attr("name"),
									input.attr("value")));
						}
					}
					doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
					if (!msg.contains("Reservation ist erfolgt")) {
						res = new ReservationResult(
								MultiStepResult.Status.ERROR, msg);
					} else {
						res = new ReservationResult(MultiStepResult.Status.OK,
								msg);
					}
				}
			}
		}

		if (res == null
				|| res.getStatus() == MultiStepResult.Status.SELECTION_NEEDED
				|| res.getStatus() == MultiStepResult.Status.CONFIRMATION_NEEDED) {
			form = new ArrayList<NameValuePair>();
			for (Element input : doc.select("input, select")) {
				if (!"image".equals(input.attr("type"))
						&& !"submit".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			form.add(new BasicNameValuePair("textButton$0", "Abbrechen"));
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
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
		try {
			nvpairs = s_pageform;
			nvpairs.add(new BasicNameValuePair("$Toolbar_1.x", "1"));
			nvpairs.add(new BasicNameValuePair("$Toolbar_1.y", "1"));
			parse_search(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs),
					1);
			nvpairs = s_pageform;
			nvpairs.add(new BasicNameValuePair("$Toolbar_3.x", "1"));
			nvpairs.add(new BasicNameValuePair("$Toolbar_3.y", "1"));
			parse_search(htmlPost(opac_url + ";jsessionid=" + s_sid, nvpairs),
					1);
		} catch (OpacErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		String alink = null;
		Document doc;
		if (s_accountcalled) {
			alink = media.split("\\|")[1].replace("requestCount=", "fooo=");
		} else {
			start();
			doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
					+ s_service + "&sp=SBK");
			try {
				doc = handleLoginForm(doc, account);
			} catch (OpacErrorException e) {
				return new ProlongResult(Status.ERROR, e.getMessage());
			}
			for (Element tr : doc.select(".rTable_div tr")) {
				if (tr.select("a").size() == 1) {
					if (tr.select("a").first().absUrl("href")
							.contains("sp=SZA")) {
						alink = tr.select("a").first().absUrl("href");
					}
				}
			}
			if (alink == null)
				return new ProlongResult(Status.ERROR);
		}

		doc = htmlGet(alink);

		List<NameValuePair> form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		for (Element tr : doc.select(".rTable_div tr")) {
			if (tr.select("input").attr("name").equals(media.split("\\|")[0])
					&& tr.select("input").hasAttr("disabled")) {
				form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
				form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
				doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
				return new ProlongResult(Status.ERROR, tr.child(4).text()
						.trim());
			}
		}
		form.add(new BasicNameValuePair(media.split("\\|")[0], "on"));
		form.add(new BasicNameValuePair("textButton$1",
				"Markierte Titel verlängern"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
		form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		return new ProlongResult(Status.OK);
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		String alink = null;
		Document doc;
		if (s_accountcalled) {
			alink = s_alink.replace("requestCount=", "fooo=");
		} else {
			start();
			doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
					+ s_service + "&sp=SBK");
			try {
				doc = handleLoginForm(doc, account);
			} catch (OpacErrorException e) {
				return new ProlongAllResult(Status.ERROR, e.getMessage());
			}
			for (Element tr : doc.select(".rTable_div tr")) {
				if (tr.select("a").size() == 1) {
					if (tr.select("a").first().absUrl("href")
							.contains("sp=SZA")) {
						alink = tr.select("a").first().absUrl("href");
					}
				}
			}
			if (alink == null)
				return new ProlongAllResult(Status.ERROR);
		}

		doc = htmlGet(alink);

		List<NameValuePair> form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
			if ("checkbox".equals(input.attr("type"))
					&& !input.hasAttr("disabled")) {
				form.add(new BasicNameValuePair(input.attr("name"), "on"));
			}
		}
		form.add(new BasicNameValuePair("textButton$1",
				"Markierte Titel verlängern"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		for (Element tr : doc.select(".rTable_div tbody tr")) {
			Map<String, String> line = new HashMap<String, String>();
			line.put(ProlongAllResult.KEY_LINE_TITLE,
					tr.child(3).text().split("[:/;]")[0].trim());
			line.put(ProlongAllResult.KEY_LINE_NEW_RETURNDATE, tr.child(1)
					.text());
			line.put(ProlongAllResult.KEY_LINE_MESSAGE, tr.child(4).text());
			result.add(line);
		}

		form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
		form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		return new ProlongAllResult(Status.OK, result);
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		String rlink = null;
		Document doc;
		if (s_accountcalled) {
			rlink = media.split("\\|")[1].replace("requestCount=", "fooo=");
		} else {
			start();
			doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
					+ s_service + "&sp=SBK");
			try {
				doc = handleLoginForm(doc, account);
			} catch (OpacErrorException e) {
				return new CancelResult(Status.ERROR, e.getMessage());
			}
			for (Element tr : doc.select(".rTable_div tr")) {
				if (tr.select("a").size() == 1) {
					if (tr.text().contains("Reservationen")
							&& !tr.child(0).text().trim().equals("")
							&& tr.select("a").first().attr("href")
									.toUpperCase(Locale.GERMAN)
									.contains("SP=SZM")) {
						rlink = tr.select("a").first().absUrl("href");
					}
				}
			}
			if (rlink == null)
				return new CancelResult(Status.ERROR);
		}

		doc = htmlGet(rlink);

		List<NameValuePair> form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		form.add(new BasicNameValuePair(media.split("\\|")[0], "on"));
		form.add(new BasicNameValuePair("textButton$0",
				"Markierte Titel löschen"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"submit".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !"".equals(input.attr("name"))) {
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
		form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		return new CancelResult(Status.OK);
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		start();
		s_accountcalled = true;

		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=SBK");
		doc = handleLoginForm(doc, account);

		AccountData adata = new AccountData(account.getId());
		for (Element tr : doc.select(".aDISListe tr")) {
			if (tr.child(0).text().matches(".*F.+llige Geb.+hren.*")) {
				adata.setPendingFees(tr.child(1).text().trim());
			}
			if (tr.child(0).text().matches(".*Ausweis g.+ltig bis.*")) {
				adata.setValidUntil(tr.child(1).text().trim());
			}
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

		// Ausleihen
		String alink = null;
		int anum = 0;
		List<Map<String, String>> lent = new ArrayList<Map<String, String>>();
		for (Element tr : doc.select(".rTable_div tr")) {
			if (tr.select("a").size() == 1) {
				if (tr.select("a").first().absUrl("href").contains("sp=SZA")) {
					alink = tr.select("a").first().absUrl("href");
					anum = Integer.parseInt(tr.child(0).text().trim());
				}
			}
		}
		if (alink != null) {
			Document adoc = htmlGet(alink);
			s_alink = alink;
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			String prolongTest = null;
			for (Element input : adoc.select("input, select")) {
				if (!"image".equals(input.attr("type"))
						&& !"submit".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					if (input.attr("type").equals("checkbox")
							&& !input.hasAttr("value")) {
						input.val("on");
					}
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				} else if (input.val().matches(".+verl.+ngerbar.+")) {
					prolongTest = input.attr("name");
				}
			}
			if (prolongTest != null) {
				form.add(new BasicNameValuePair(prolongTest,
						"Markierte Titel verlängerbar?"));
				adoc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
			}
			for (Element tr : adoc.select(".rTable_div tbody tr")) {
				Map<String, String> line = new HashMap<String, String>();
				line.put(AccountData.KEY_LENT_TITLE,
						tr.child(3).text().split("[:/;]")[0].trim());
				line.put(AccountData.KEY_LENT_DEADLINE, tr.child(1).text()
						.trim());
				try {
					line.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String
							.valueOf(sdf.parse(tr.child(1).text().trim())
									.getTime()));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				line.put(AccountData.KEY_LENT_BRANCH, tr.child(2).text().trim());
				line.put(AccountData.KEY_LENT_LINK,
						tr.select("input[type=checkbox]").attr("name") + "|"
								+ alink);
				// line.put(AccountData.KEY_LENT_RENEWABLE, tr.child(4).text()
				// .matches(".*nicht verl.+ngerbar.*") ? "N" : "Y");
				lent.add(line);
			}
			assert (lent.size() == anum);
			form = new ArrayList<NameValuePair>();
			for (Element input : adoc.select("input, select")) {
				if (!"image".equals(input.attr("type"))
						&& !"submit".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
			form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
		} else {
			assert (anum == 0);
		}

		adata.setLent(lent);

		List<String> rlinks = new ArrayList<String>();
		int rnum = 0;
		List<Map<String, String>> res = new ArrayList<Map<String, String>>();
		for (Element tr : doc.select(".rTable_div tr")) {
			if (tr.select("a").size() == 1) {
				if (tr.text().contains("Reservationen")
						&& !tr.child(0).text().trim().equals("")) {
					rlinks.add(tr.select("a").first().absUrl("href"));
					rnum += Integer.parseInt(tr.child(0).text().trim());
				}
			}
		}
		for (String rlink : rlinks) {
			Document rdoc = htmlGet(rlink);
			boolean error = false;
			for (Element tr : rdoc.select(".rTable_div tbody tr")) {
				if (tr.children().size() >= 4) {
					Map<String, String> line = new HashMap<String, String>();
					line.put(AccountData.KEY_RESERVATION_TITLE, tr.child(2)
							.text().split("[:/;]")[0].trim());
					line.put(AccountData.KEY_RESERVATION_READY, tr.child(3)
							.text().trim().substring(0, 10));
					line.put(AccountData.KEY_RESERVATION_BRANCH, tr.child(1)
							.text().trim());
					if (tr.select("input[type=checkbox]").size() > 0
							&& rlink.toUpperCase(Locale.GERMAN).contains(
									"SP=SZM"))
						line.put(AccountData.KEY_RESERVATION_CANCEL,
								tr.select("input[type=checkbox]").attr("name")
										+ "|" + rlink);
					res.add(line);
				} else {
					// This is a strange bug where sometimes there is only three
					// columns
					error = true;
				}
			}
			if (error) {
				// Maybe we should send a bug report here, but using ACRA breaks
				// the unit tests
				adata.setWarning("Beim Abrufen der Reservationen ist ein Problem aufgetreten");
			}

			List<NameValuePair> form = new ArrayList<NameValuePair>();
			for (Element input : rdoc.select("input, select")) {
				if (!"image".equals(input.attr("type"))
						&& !"submit".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			form.add(new BasicNameValuePair("$Toolbar_0.x", "1"));
			form.add(new BasicNameValuePair("$Toolbar_0.y", "1"));
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
		}

		assert (res.size() == rnum);

		adata.setReservations(res);

		return adata;
	}

	protected Document handleLoginForm(Document doc, Account account)
			throws IOException, OpacErrorException {

		if (doc.select("#LPASSW_1").size() == 0) {
			return doc;
		}

		doc.select("#LPASSW_1").val(account.getPassword());

		List<NameValuePair> form = new ArrayList<NameValuePair>();
		for (Element input : doc.select("input, select")) {
			if (!"image".equals(input.attr("type"))
					&& !"checkbox".equals(input.attr("type"))
					&& !input.attr("value").contains("vergessen")
					&& !"".equals(input.attr("name"))) {
				if (input.attr("id").equals("L#AUSW_1")
						|| input.attr("id").equals("IDENT_1")
						|| input.attr("id").equals("LMATNR_1")) {
					input.attr("value", account.getName());
				}
				form.add(new BasicNameValuePair(input.attr("name"), input
						.attr("value")));
			}
		}
		doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);

		if (doc.select(".message h1").size() > 0) {
			String msg = doc.select(".message h1").text().trim();
			form = new ArrayList<NameValuePair>();
			for (Element input : doc.select("input")) {
				if (!"image".equals(input.attr("type"))
						&& !"checkbox".equals(input.attr("type"))
						&& !"".equals(input.attr("name"))) {
					form.add(new BasicNameValuePair(input.attr("name"), input
							.attr("value")));
				}
			}
			doc = htmlPost(opac_url + ";jsessionid=" + s_sid, form);
			if (!msg.contains("Sie sind angemeldet")) {
				throw new OpacErrorException(msg);
			}
			return doc;
		} else {
			return doc;
		}
	}

	@Override
	public List<SearchField> getSearchFields() throws IOException,
			JSONException {
		if (!initialised)
			start();

		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=" + s_exts);

		List<SearchField> fields = new ArrayList<SearchField>();
		// dropdown to select which field you want to search in
		for (Element opt : doc.select("#SUCH01_1 option")) {
			TextSearchField field = new TextSearchField();
			field.setId(opt.attr("value"));
			field.setDisplayName(opt.text());
			field.setHint("");
			fields.add(field);
		}

		// Save data so that the search() function knows that this
		// is not a selectable search field
		JSONObject selectableData = new JSONObject();
		selectableData.put("selectable", false);

		for (Element row : doc.select("div[id~=F\\d+]")) {
			if (row.select("input[type=text]").size() == 1
					&& row.select("input, select").first().tagName()
							.equals("input")) {
				// A single text search field
				Element input = row.select("input[type=text]").first();
				TextSearchField field = new TextSearchField();
				field.setId(input.attr("id"));
				field.setDisplayName(row.select("label").first().text());
				field.setHint("");
				field.setData(selectableData);
				fields.add(field);
			} else if (row.select("select").size() == 1
					&& row.select("input[type=text]").size() == 0) {
				// Things like language, media type, etc.
				Element select = row.select("select").first();
				DropdownSearchField field = new DropdownSearchField();
				field.setId(select.id());
				field.setDisplayName(row.select("label").first().text());
				List<Map<String, String>> values = new ArrayList<Map<String, String>>();
				for (Element opt : select.select("option")) {
					Map<String, String> value = new HashMap<String, String>();
					value.put("key", opt.attr("value"));
					value.put("value", opt.text());
					values.add(value);
				}
				field.setDropdownValues(values);
				fields.add(field);
			} else if (row.select("select").size() == 0
					&& row.select("input[type=text]").size() == 3
					&& row.select("label").size() == 3) {
				// Three text inputs.
				// Year single/from/to or things like Band-/Heft-/Satznummer
				String name1 = row.select("label").get(0).text();
				String name2 = row.select("label").get(1).text();
				String name3 = row.select("label").get(2).text();
				Element input1 = row.select("input[type=text]").get(0);
				Element input2 = row.select("input[type=text]").get(1);
				Element input3 = row.select("input[type=text]").get(2);

				if (name2.contains("von") && name3.contains("bis")) {
					TextSearchField field1 = new TextSearchField();
					field1.setId(input1.id());
					field1.setDisplayName(name1);
					field1.setHint("");
					field1.setData(selectableData);
					fields.add(field1);

					TextSearchField field2 = new TextSearchField();
					field2.setId(input2.id());
					field2.setDisplayName(name2.replace("von", "").trim());
					field2.setHint("von");
					field2.setData(selectableData);
					fields.add(field2);

					TextSearchField field3 = new TextSearchField();
					field3.setId(input3.id());
					field3.setDisplayName(name3.replace("bis", "").trim());
					field3.setHint("bis");
					field3.setHalfWidth(true);
					field3.setData(selectableData);
					fields.add(field3);
				} else {
					TextSearchField field1 = new TextSearchField();
					field1.setId(input1.id());
					field1.setDisplayName(name1);
					field1.setHint("");
					field1.setData(selectableData);
					fields.add(field1);

					TextSearchField field2 = new TextSearchField();
					field2.setId(input2.id());
					field2.setDisplayName(name2);
					field2.setHint("");
					field2.setData(selectableData);
					fields.add(field2);

					TextSearchField field3 = new TextSearchField();
					field3.setId(input3.id());
					field3.setDisplayName(name3);
					field3.setHint("");
					field3.setData(selectableData);
					fields.add(field3);
				}
			}
		}

		for (Iterator<SearchField> iterator = fields.iterator(); iterator
				.hasNext();) {
			SearchField field = iterator.next();
			if (ignoredFieldNames.contains(field.getDisplayName())) {
				iterator.remove();
			}
		}

		return fields;
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
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getShareUrl(String id, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ACCOUNT_PROLONG_ALL
				| SUPPORT_FLAG_ENDLESS_SCROLLING;
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

	@Override
	public void checkAccountData(Account account) throws IOException,
			JSONException, OpacErrorException {
		start();
		s_accountcalled = true;

		Document doc = htmlGet(opac_url + ";jsessionid=" + s_sid + "?service="
				+ s_service + "&sp=SBK");
		doc = handleLoginForm(doc, account);
	}

	@Override
	public void setLanguage(String language) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getSupportedLanguages() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
