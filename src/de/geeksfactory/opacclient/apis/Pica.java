package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
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
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * @author Johan von Forstner, 04.09.2013
 *  */

public class Pica extends BaseApi implements OpacApi {
	
	protected String opac_url = "";
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected String last_error;
	protected Library library;
	protected String ENCODING = "UTF-8";
	protected int resultcount = 10;
	protected String reusehtml;
	
	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("book", MediaType.BOOK);
		defaulttypes.put("article", MediaType.BOOK);
		defaulttypes.put("binary", MediaType.EBOOK);
		defaulttypes.put("periodical", MediaType.MAGAZINE);
		defaulttypes.put("onlineper", MediaType.EBOOK);
		defaulttypes.put("letter", MediaType.UNKNOWN);
		defaulttypes.put("handwriting", MediaType.UNKNOWN);
		defaulttypes.put("map", MediaType.MAP);
		defaulttypes.put("picture", MediaType.ART);
		defaulttypes.put("audiovisual", MediaType.MOVIE);
		defaulttypes.put("score", MediaType.SCORE_MUSIC);
		defaulttypes.put("sound", MediaType.CD_MUSIC);
		defaulttypes.put("software", MediaType.CD_SOFTWARE);
		defaulttypes.put("microfilm", MediaType.UNKNOWN);
		defaulttypes.put("empty", MediaType.UNKNOWN);
	}

	@Override
	public void start() throws IOException, NotReachableException {
		String html = httpGet(opac_url
				+ "/DB=1/SET=1/TTL=1/ADVANCED_SEARCHFILTER");

		Document doc = Jsoup.parse(html);

		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			//extract_meta(doc);
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
	
	protected int addParameters(Bundle query, String key, String searchkey,
			List<NameValuePair> params, int index) {
		if (!query.containsKey(key) || query.getString(key).equals(""))
			return index;

		if (index == 0) {
			params.add(new BasicNameValuePair("ACT" + index, "SRCH"));
		} else {
			params.add(new BasicNameValuePair("ACT" + index, "*"));
		}
		
		params.add(new BasicNameValuePair("IKT" + index, searchkey));
		params.add(new BasicNameValuePair("TRM" + index, query.getString(key)));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
			int index = 0;
			start();

			params.add(new BasicNameValuePair("ACT", "SRCHM"));
			params.add(new BasicNameValuePair("MATCFILTER", "Y"));
			params.add(new BasicNameValuePair("MATCSET", "Y"));
			params.add(new BasicNameValuePair("NOSCAN", "Y"));
			params.add(new BasicNameValuePair("PARSE_MNEMONICS", "N"));
			params.add(new BasicNameValuePair("PARSE_OPWORDS", "N"));
			params.add(new BasicNameValuePair("PARSE_OLDSETS", "N"));

			index = addParameters(query, KEY_SEARCH_QUERY_TITLE, "1016", params,
					index);
			index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "1004",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA, "46",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDB, "46",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, "1004",
					params, index);
			index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM, "20",
					params, index);
			
			params.add(new BasicNameValuePair("SRT", "YOP"));
			
			//year has a special command
			params.add(new BasicNameValuePair("ADI_JVU", query.getString(KEY_SEARCH_QUERY_YEAR)));

			if (index == 0) {
				last_error = "Es wurden keine Suchkriterien eingegeben.";
				return null;
			}
			if (index > 4) {
				last_error = "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.";
				return null;
			}

			//params.add(new BasicNameValuePair("submitSearch", "Suchen"));
			//params.add(new BasicNameValuePair("callingPage", "searchParameters"));
			//params.add(new BasicNameValuePair("numberOfHits", "10"));

			//params.add(new BasicNameValuePair("selectedSearchBranchlib", query
			//		.getString(KEY_SEARCH_QUERY_BRANCH)));
//			if (query.getString(KEY_SEARCH_QUERY_HOME_BRANCH) != null) {
//				if (!query.getString(KEY_SEARCH_QUERY_HOME_BRANCH).equals(""))
//					params.add(new BasicNameValuePair("selectedViewBranchlib",
//							query.getString(KEY_SEARCH_QUERY_HOME_BRANCH)));
//			}

		String html = httpGet(
				opac_url + "/DB=1/SET=1/TTL=1/CMD?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING);
		return parse_search(html, 1);
	}
	
	protected SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);

		if (doc.select(".error").size() > 0) {
			if (doc.select(".error").text().trim().equals("Es wurde nichts gefunden.")) {
				//nothing found
				return new SearchRequestResult(new ArrayList<SearchResult>(), 0, 1, 1);
			} else {
				//error
				last_error = doc.select(".error").text().trim();
				return null;
			}
		}
		
		reusehtml = html;

		int results_total = -1;

		String resultnumstr = doc.select(".pages").first().text();
		Pattern p = Pattern.compile("[0-9]+$");
		Matcher m = p.matcher(resultnumstr);
		if(m.find()) {
		    resultnumstr = m.group();
		}
//		if (resultnumstr.contains("(1/1)") || resultnumstr.contains(" 1/1")) {
//			reusehtml = html;
//			last_error = "is_a_redirect";
//			return null;
//		} else
		if (resultnumstr.contains("(")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*\\(([0-9]+)\\).*", "$1"));
		} else if (resultnumstr.contains(": ")) {
			results_total = Integer.parseInt(resultnumstr.replaceAll(
					".*: ([0-9]+)$", "$1"));
		} else {
			results_total = Integer.parseInt(resultnumstr);
		}

		List<SearchResult> results = new ArrayList<SearchResult>();		
		
		if (results_total == 1) {
			//Only one result
			try {
				//TODO: Fix single item result!
				DetailledItem singleResult = parse_result(html);
				SearchResult sr = new SearchResult();
				sr.setType(MediaType.UNKNOWN); //TODO: implement getting MediaTypes for single items
				sr.setInnerhtml(singleResult.getTitle());
				results.add(sr);
				
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}

		Elements table = doc.select("table[summary=hitlist] tbody tr[valign=top]");
		//identifier = null;

		Elements links = doc.select("table[summary=hitlist] a");
		boolean haslink = false;
		for (int i = 0; i < links.size(); i++) {
			Element node = links.get(i);
			if (node.hasAttr("href")
					& node.attr("href").contains("SHW?") && !haslink) {
				haslink = true;
				try {
					List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
							((Element) node).attr("href")), ENCODING);
					for (NameValuePair nv : anyurl) {
						if (nv.getName().equals("identifier")) {
							//identifier = nv.getValue();
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		for (int i = 0; i < table.size(); i++) {
			Element tr = table.get(i);
			SearchResult sr = new SearchResult();
			if (tr.select("td.hit img").size() > 0) {
				String[] fparts = tr.select("td img").get(0).attr("src")
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
			}
			Element middlething = tr.child(2);

			List<Node> children = middlething.childNodes();
			int childrennum = children.size();

			List<String[]> strings = new ArrayList<String[]>();
			for (int ch = 0; ch < childrennum; ch++) {
				Node node = children.get(ch);
				if (node instanceof TextNode) {
					String text = ((TextNode) node).text().trim();
					if (text.length() > 3)
						strings.add(new String[] { "text", "", text });
				} else if (node instanceof Element) {

					List<Node> subchildren = node.childNodes();
					for (int j = 0; j < subchildren.size(); j++) {
						Node subnode = subchildren.get(j);
						if (subnode instanceof TextNode) {
							String text = ((TextNode) subnode).text().trim();
							if (text.length() > 3)
								strings.add(new String[] {
										((Element) node).tag().getName(),
										"text", text,
										((Element) node).className(),
										((Element) node).attr("style") });
						} else if (subnode instanceof Element) {
							String text = ((Element) subnode).text().trim();
							if (text.length() > 3)
								strings.add(new String[] {
										((Element) node).tag().getName(),
										((Element) subnode).tag().getName(),
										text, ((Element) node).className(),
										((Element) node).attr("style") });
						}
					}
				}
			}

			StringBuilder description = new StringBuilder();
			
			int k = 0;
			boolean yearfound = false;
			for (String[] part : strings) {
					if (part[0] == "a" && k == 0) {
						description.append("<b>" + part[2] + "</b>");
					} else if (part[2].matches("\\D*[0-9]{4}\\D*")
							&& part[2].length() <= 10) {
						yearfound = true;
						description.append("<br />" + part[2]);
					} else if (k == 1 && !yearfound
							&& part[2].matches("^\\s*\\([0-9]{4}\\)$")) {
						description.append("<br />" + part[2]);
					} else if (k < 3 && !yearfound) {
						description.append("<br />" + part[2]);
					}
				if (part.length == 4) {
					if (part[0].equals("span") && part[3].equals("textgruen")) {
						sr.setStatus(SearchResult.Status.GREEN);
					} else if (part[0].equals("span")
							&& part[3].equals("textrot")) {
						sr.setStatus(SearchResult.Status.RED);
					}
				} else if (part.length == 5) {
					if (part[4].contains("purple")) {
						sr.setStatus(SearchResult.Status.YELLOW);
					}
				}
				if (sr.getStatus() == null) {
					if (part[2].contains("entliehen")
							&& part[2]
									.startsWith("Vormerkung ist leider nicht möglich")) {
						sr.setStatus(SearchResult.Status.RED);
					} else if (part[2].startsWith("entliehen")
							|| part[2]
									.contains("Ein Exemplar finden Sie in einer anderen Zweigstelle")) {
						sr.setStatus(SearchResult.Status.YELLOW);
					} else if ((part[2].startsWith("bestellbar") && !part[2]
							.contains("nicht bestellbar"))
							|| (part[2].startsWith("vorbestellbar") && !part[2]
									.contains("nicht vorbestellbar"))
							|| (part[2].startsWith("vormerkbar") && !part[2]
									.contains("nicht vormerkbar"))
							|| (part[2].contains("ausleihbar") && !part[2]
									.contains("nicht ausleihbar"))) {
						sr.setStatus(SearchResult.Status.GREEN);
					}
				}
				k++;
			}
			sr.setInnerhtml(description.toString());

			sr.setNr(10 * (page - 1) + i);
			sr.setId(null);
			results.add(sr);
		}
		resultcount = results.size();
		return new SearchRequestResult(results, results_total, page);
	}
	
	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		if (!initialised)
			start();

		String html = httpGet(opac_url
				+ "/DB=1/SET=1/TTL=1/NXT?FRST=" + (((page - 1) * resultcount) + 1), ENCODING);
		return parse_search(html, page);
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {

//		if (id == null && reusehtml != null) {
//			return parse_result(reusehtml);
//		}
//
//		String html = httpGet(opac_url + "/start.do?" + startparams
//				+ "searchType=1&Query=0%3D%22" + id + "%22" + hbp, ENCODING);
//
//		return parse_result(html);
		return null;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		String html = httpGet(
				opac_url
						+ "DB=1/SET=1/TTL=1/SHW?FRST="
						+ (position + 1), ENCODING);

		return parse_result(html);
	}
	
	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();

		try {
			result.setId(doc.select("td.preslabel:contains(Signatur) + td.presvalue").text().trim());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
//		List<String> reservationlinks = new ArrayList<String>();
//		for (Element link : doc3.select("#vormerkung a, #tab-content a")) {
//			Uri href = Uri.parse(link.absUrl("href"));
//			if (result.getId() == null) {
//				// ID retrieval
//				String key = href.getQueryParameter("katkey");
//				if (key != null) {
//					result.setId(key);
//					break;
//				}
//			}
//
//			// Vormerken
//			if (href.getQueryParameter("methodToCall") != null) {
//				if (href.getQueryParameter("methodToCall").equals(
//						"doVormerkung")
//						|| href.getQueryParameter("methodToCall").equals(
//								"doBestellung"))
//					reservationlinks.add(href.getQuery());
//			}
//		}
//		if (reservationlinks.size() == 1) {
//			result.setReservable(true);
//			result.setReservation_info(reservationlinks.get(0));
//		} else if (reservationlinks.size() == 0) {
//			result.setReservable(false);
//		} else {
//			// Multiple options - handle this case!
//		}

//		TODO: There seem to be no cover images in Kiel Uni Library, so covers are not implemented (the code below is copy from SISIS)
//		if (doc.select(".data td img").size() == 1) {
//			result.setCover(doc.select(".data td img").first().attr("abs:src"));
//		}

		String titleAndSubtitle = "";
		if (doc.select("td.preslabel:contains(Titel) + td.presvalue").size() == 1) {
			titleAndSubtitle = doc.select("td.preslabel:contains(Titel) + td.presvalue").first().text().trim();
			String title = titleAndSubtitle.substring(0, titleAndSubtitle.indexOf("/")).trim();
			result.setTitle(title);
			
			String subtitle = titleAndSubtitle.substring(titleAndSubtitle.indexOf("/")).trim();
			result.addDetail(new Detail("Titelzusatz", subtitle));
		} else {
			result.setTitle("");
		}

		String title = "";
		String text = "";
		
		Element detailtrs = doc.select("table[summary=layout] table tbody").first();
		if (detailtrs != null) {
			for (Node node : detailtrs.childNodes()) {
				if (node instanceof Element) {
					if (((Element) node).tagName().equals("strong")) {
						if (!text.equals("") && !title.equals("")) {
							result.addDetail(new Detail(title.trim(), text
									.trim()));
							if (title.equals("Titel:")) {
								result.setTitle(text.trim());
							}
							text = "";
						}

						title = ((Element) node).text().trim();
					} else {
						if (((Element) node).tagName().equals("a")
								&& ((Element) node).text().trim()
										.equals("hier klicken")) {
							text = text + ((Element) node).attr("href");
						} else {
							text = text + ((Element) node).text();
						}
					}
				} else if (node instanceof TextNode) {
					text = text + ((TextNode) node).text();
				}
			}
		} else {
			result.addDetail(new Detail("Fehler",
					"Details konnten nicht abgerufen werden, bitte erneut probieren!"));
		}
		if (!text.equals("") && !title.equals("")) {
			result.addDetail(new Detail(title.trim(), text.trim()));
			if (title.equals("Titel:")) {
				result.setTitle(text.trim());
			}
		}

//		Map<String, Integer> copy_columnmap = new HashMap<String, Integer>();
//		// Default values
//		copy_columnmap.put(DetailledItem.KEY_COPY_BARCODE, 1);
//		copy_columnmap.put(DetailledItem.KEY_COPY_BRANCH, 3);
//		copy_columnmap.put(DetailledItem.KEY_COPY_STATUS, 4);
//		Elements copy_columns = doc.select("#tab-content .data tr#bg2 th");
//		for (int i = 0; i < copy_columns.size(); i++) {
//			Element th = copy_columns.get(i);
//			String head = th.text().trim();
//			if (head.contains("Status")) {
//				copy_columnmap.put(DetailledItem.KEY_COPY_STATUS, i);
//			}
//			if (head.contains("Zweigstelle")) {
//				copy_columnmap.put(DetailledItem.KEY_COPY_BRANCH, i);
//			}
//			if (head.contains("Mediennummer")) {
//				copy_columnmap.put(DetailledItem.KEY_COPY_BARCODE, i);
//			}
//			if (head.contains("Standort")) {
//				copy_columnmap.put(DetailledItem.KEY_COPY_LOCATION, i);
//			}
//			if (head.contains("Signatur")) {
//				copy_columnmap.put(DetailledItem.KEY_COPY_SHELFMARK, i);
//			}
//		}
//
//		Pattern status_lent = Pattern
//				.compile("^(entliehen) bis ([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4}) \\(gesamte Vormerkungen: ([0-9]+)\\)$");
//		Pattern status_and_barcode = Pattern.compile("^(.*) ([0-9A-Za-z]+)$");
//
//		Elements exemplartrs = doc.select("#tab-content .data tr").not("#bg2");
//		for (Element tr : exemplartrs) {
//			try {
//				ContentValues e = new ContentValues();
//				Element status = tr.child(copy_columnmap
//						.get(DetailledItem.KEY_COPY_STATUS));
//				Element barcode = tr.child(copy_columnmap
//						.get(DetailledItem.KEY_COPY_BARCODE));
//				String barcodetext = barcode.text().trim()
//						.replace(" Wegweiser", "");
//
//				// STATUS
//				String statustext = "";
//				if (status.getElementsByTag("b").size() > 0) {
//					statustext = status.getElementsByTag("b").text().trim();
//				} else {
//					statustext = status.text().trim();
//				}
//				if (copy_columnmap.get(DetailledItem.KEY_COPY_STATUS) == copy_columnmap
//						.get(DetailledItem.KEY_COPY_BARCODE)) {
//					Matcher matcher1 = status_and_barcode.matcher(statustext);
//					if (matcher1.matches()) {
//						statustext = matcher1.group(1);
//						barcodetext = matcher1.group(2);
//					}
//				}
//
//				Matcher matcher = status_lent.matcher(statustext);
//				if (matcher.matches()) {
//					e.put(DetailledItem.KEY_COPY_STATUS, matcher.group(1));
//					e.put(DetailledItem.KEY_COPY_RETURN, matcher.group(2));
//					e.put(DetailledItem.KEY_COPY_RESERVATIONS, matcher.group(3));
//				} else {
//					e.put(DetailledItem.KEY_COPY_STATUS, statustext);
//				}
//				e.put(DetailledItem.KEY_COPY_BARCODE, barcodetext);
//
//				String branchtext = tr
//						.child(copy_columnmap
//								.get(DetailledItem.KEY_COPY_BRANCH)).text()
//						.trim().replace(" Wegweiser", "");
//				e.put(DetailledItem.KEY_COPY_BRANCH, branchtext);
//
//				if (copy_columnmap.containsKey(DetailledItem.KEY_COPY_LOCATION)) {
//					e.put(DetailledItem.KEY_COPY_LOCATION,
//							tr.child(
//									copy_columnmap
//											.get(DetailledItem.KEY_COPY_LOCATION))
//									.text().trim().replace(" Wegweiser", ""));
//				}
//
//				if (copy_columnmap
//						.containsKey(DetailledItem.KEY_COPY_SHELFMARK)) {
//					e.put(DetailledItem.KEY_COPY_SHELFMARK,
//							tr.child(
//									copy_columnmap
//											.get(DetailledItem.KEY_COPY_SHELFMARK))
//									.text().trim().replace(" Wegweiser", ""));
//				}
//
//				result.addCopy(e);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//
//		try {
//			Element isvolume = null;
//			Bundle volume = new Bundle();
//			Elements links = doc.select(".data td a");
//			int elcount = links.size();
//			for (int eli = 0; eli < elcount; eli++) {
//				List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
//						links.get(eli).attr("href")), "UTF-8");
//				for (NameValuePair nv : anyurl) {
//					if (nv.getName().equals("methodToCall")
//							&& nv.getValue().equals("volumeSearch")) {
//						isvolume = links.get(eli);
//					} else if (nv.getName().equals("catKey")) {
//						volume.putString("catKey", nv.getValue());
//					} else if (nv.getName().equals("dbIdentifier")) {
//						volume.putString("dbIdentifier", nv.getValue());
//					}
//				}
//				if (isvolume != null) {
//					volume.putBoolean("volume", true);
//					result.setVolumesearch(volume);
//					break;
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		return result;
	}

	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean prolong(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cancel(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_TITLE,
				KEY_SEARCH_QUERY_AUTHOR, KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_KEYWORDB, KEY_SEARCH_QUERY_YEAR, 
				KEY_SEARCH_QUERY_SYSTEM, KEY_SEARCH_QUERY_PUBLISHER };
	}

	@Override
	public String getLast_error() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

}
