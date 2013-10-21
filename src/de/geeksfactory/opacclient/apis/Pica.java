/**
 * Copyright (C) 2013 by Johan von Forstner under the MIT license:
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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
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
 * @author Johan von Forstner, 16.09.2013
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
	protected Integer searchSet;
	protected String db;
	CookieStore cookieStore = new BasicCookieStore();
	
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
		//String html = httpGet(opac_url
		//		+ "/DB=" + db + "/SET=1/TTL=1/ADVANCED_SEARCHFILTER", getDefaultEncoding(), false, cookieStore);

		//Document doc = Jsoup.parse(html);
		
		//updateSearchSetValue(doc);

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
			this.db = data.getString("db");
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

			index = addParameters(query, KEY_SEARCH_QUERY_FREE, "1016", params,
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

		String html = httpGet(
				opac_url + "/DB="+ db + "/SET=1/TTL=1/CMD?"
						+ URLEncodedUtils.format(params, "UTF-8"), ENCODING, false, cookieStore);
		
		return parse_search(html, 1);
	}
	
	protected SearchRequestResult parse_search(String html, int page) {
		Document doc = Jsoup.parse(html);
		
		updateSearchSetValue(doc);

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
				DetailledItem singleResult = parse_result(html);
				SearchResult sr = new SearchResult();
				sr.setType(getMediaTypeInSingleResult(html));
				sr.setInnerhtml("<b>" + singleResult.getTitle() +  "</b><br>" + singleResult.getDetails().get(0).getContent());
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
			for (String[] part : strings) {
					if (part[0] == "a" && k == 0) {
						description.append("<b>" + part[2] + "</b>");
					} else if (k < 3) {
						description.append("<br />" + part[2]);
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
				+ "/DB=" + db + "/SET=" + searchSet + "/TTL=1/NXT?FRST=" + (((page - 1) * resultcount) + 1), ENCODING, false, cookieStore);
		return parse_search(html, page);
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		return null;
	}


	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {

		if (id == null && reusehtml != null) {
			return parse_result(reusehtml);
		}

		String html = httpGet(id, ENCODING);

		return parse_result(html);
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		String html = httpGet(
				opac_url
						+ "/DB=" + db + "/SET=" + searchSet + "/TTL=1/SHW?FRST="
						+ (position + 1), ENCODING, false, cookieStore);

		return parse_result(html);
	}
	
	protected DetailledItem parse_result(String html) throws IOException {
		Document doc = Jsoup.parse(html);

		DetailledItem result = new DetailledItem();
		
		String id = opac_url + doc.select("img[src*=permalink], img[src*=zitierlink]").get(0).parent().attr("href");
		result.setId(id);		
		
//		TODO: There seem to be no cover images in Kiel Uni Library, so covers are not implemented

		//GET TITLE AND SUBTITLE
		String titleAndSubtitle = "";
		if (doc.select("td.preslabel:contains(Titel) + td.presvalue").size() > 0) {
			titleAndSubtitle = doc.select("td.preslabel:contains(Titel) + td.presvalue").first().text().trim();
			String title = titleAndSubtitle.substring(0, titleAndSubtitle.indexOf("/")).trim();
			result.setTitle(title);
			
			String subtitle = titleAndSubtitle.substring(titleAndSubtitle.indexOf("/") + 1).trim();
			result.addDetail(new Detail("Titelzusatz", subtitle));
		} else if (doc.select("td.preslabel:contains(Aufsatz) + td.presvalue").size() > 0) {
			titleAndSubtitle = doc.select("td.preslabel:contains(Aufsatz) + td.presvalue").first().text().trim();
			String title = titleAndSubtitle.substring(0, titleAndSubtitle.indexOf("/")).trim();
			result.setTitle(title);
			
			String subtitle = titleAndSubtitle.substring(titleAndSubtitle.indexOf("/") + 1).trim();
			result.addDetail(new Detail("Titelzusatz", subtitle));
		} else {
			result.setTitle("");
		}
		
		//GET OTHER INFORMATION
		ContentValues e = new ContentValues();		
		String location = "";
		
		for (Element element : doc.select("td.preslabel + td.presvalue")) {
			String detail = element.text().trim();
			String title = element.firstElementSibling().text().trim().replace("\u00a0", "");
			
			if (element.select("hr").size() > 0 && location != "") { //multiple copies
				e.put(DetailledItem.KEY_COPY_BRANCH, location);				
				result.addCopy(e);
				location = "";
				e = new ContentValues();
			}
			
			if (!title.equals("")) {
				
				if (title.indexOf(":") != -1) {
					title = title.substring(0, title.indexOf(":")); //remove colon
				}
				
				if (title.contains("Status")) {
					e.put(DetailledItem.KEY_COPY_STATUS, detail);
				} else if (title.contains("Standort")) {
					location += detail;
				} else if (title.contains("Sonderstandort")) {
					location += " - " + detail;
				} else if (title.contains("Fachnummer")) {
					e.put(DetailledItem.KEY_COPY_LOCATION, detail);
				} else if (title.contains("Signatur")) {
					e.put(DetailledItem.KEY_COPY_SHELFMARK, detail);
				} else if (title.contains("Status")) {
					detail = detail.replace("Bestellen", "").trim();
					detail = detail.replace("verfuegbar", "verf�gbar");
					detail = detail.replace("Verfuegbar", "verf�gbar");
					e.put(DetailledItem.KEY_COPY_STATUS, detail);
				} else if (!title.contains("Titel")) {
					result.addDetail(new Detail(title, detail));
				}
			}
		}
		
		e.put(DetailledItem.KEY_COPY_BRANCH, location);				
		result.addCopy(e);

		return result;
	}

	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String Selection) throws IOException {
		return new ProlongResult(MultiStepResult.Status.UNSUPPORTED);
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		return false;
	}

	@Override
	public boolean cancel(Account account, String media) throws IOException {
		return false;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_FREE,
				KEY_SEARCH_QUERY_AUTHOR, KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_KEYWORDB, KEY_SEARCH_QUERY_YEAR, 
				KEY_SEARCH_QUERY_SYSTEM, KEY_SEARCH_QUERY_PUBLISHER };
	}

	@Override
	public String getLast_error() {
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
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		return id;
	}

	@Override
	public int getSupportFlags() {
		return 0;
		
	}
	
	public void updateSearchSetValue(Document doc) {
		String url = doc.select("base").first().attr("href");
		Integer setPosition = url.indexOf("SET=") + 4;
		String searchSetString = url.substring(setPosition, url.indexOf("/", setPosition));
		searchSet = Integer.parseInt(searchSetString);
	}
	
	public MediaType getMediaTypeInSingleResult(String html) {
		Document doc = Jsoup.parse(html);		
		MediaType mediatype = MediaType.UNKNOWN;
		
		if (doc.select("table[summary=presentation switch] img").size() > 0) {
			
			String[] fparts = doc.select("table[summary=presentation switch] img").get(0).attr("src")
					.split("/");
			String fname = fparts[fparts.length - 1];
			
			if (data.has("mediatypes")) {
				try {
					mediatype = MediaType.valueOf(data.getJSONObject(
							"mediatypes").getString(fname));
				} catch (JSONException e) {
					mediatype = defaulttypes.get(fname.toLowerCase()
							.replace(".jpg", "").replace(".gif", "")
							.replace(".png", ""));
				} catch (IllegalArgumentException e) {
					mediatype = defaulttypes.get(fname.toLowerCase()
							.replace(".jpg", "").replace(".gif", "")
							.replace(".png", ""));
				}
			} else {
				mediatype = defaulttypes.get(fname.toLowerCase()
						.replace(".jpg", "").replace(".gif", "")
						.replace(".png", ""));
			}
		}
		
		return mediatype;
	}

}
