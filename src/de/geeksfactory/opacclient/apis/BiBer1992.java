/**
 * Copyright (C) 2013 by Rüdiger Wurth under the MIT license:
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.os.Bundle;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * @author Ruediger Wurth, 16.02.2013 
 * Web identification: "copyright 1992-2011 by BiBer GmbH"
 * 
 * BiBer gestartet mit Stadtbibliothek Offenburg
 * start URL: http://217.86.216.47/opac/de/qsim_frm.html.S
 * 
 *  open:
 *  issue #23: Basic support for library system "Biber" -> Essen
 *  issue #32: Integration of "BiBer" (copyright 2006) -> Karlsruhe https://opac.karlsruhe.de/ 
 *  issue #33: Integration of "BiBer" (copyright 1992) -> Essen
 * 
 * Features:
 * In getResult(), mixed table layout is supported: column-wise and row-wise
 * In getResult(), amazon bitmaps are supported
 * 
 * Katalogsuche tested with
 * 
 * Name					Media	amazon	copy	Media	Branch	Account	
 * 						type	Bitmaps	table	types			support
 * 						images			avail	search
 * --------------------------------------------------------------------
 * BaWü/Friedrichshafen	ok		yes		yes		yes		yes		-
 * BaWü/Offenburg		ok		n/a		no		yes		n/a		yes
 * Bay/Aschaffenburg	ok		n/a		no		yes		n/a		-
 * Bay/Würzburg			ok		yes		yes		yes		yes		-
 * NRW/Duisburg			ok		yes		yes		yes		n/a		-
 * NRW/Essen			n/a		n/a		no		yes		not sup.-
 * NRW/Gelsenkirchen	ok		yes		yes		yes		yes		-
 * NRW/Hagen       		ok		yes		yes		yes		yes		-
 * NRW/Herford			n/a		yes		yes		yes		n/a		-
 * NRW/L�nen			ok		yes		no		yes		n/a		-
 * 			 
 */
public class BiBer1992 implements OpacApi {

	private String 				m_opac_url = "";
	private String				m_opac_dir = "opac";  // sometimes also "opax"
	private String 				m_results;
	private JSONObject 			m_data;
	private DefaultHttpClient 	m_ahc;
	private MetaDataSource 		m_metadata;
	private boolean 			m_initialised = false;
	private String 				m_last_error;
	private Library 			m_library;
	private List<NameValuePair> m_nameValuePairs = new ArrayList<NameValuePair>(2);

	//private int 				m_resultcount = 10;
	//private long logged_in;
	//private Account logged_in_as;
	
	// we have to limit num of results because PUSH attribute SHOW=20 does not work:
	// number of results is always 50 which is too much
	final private int numOfResultsPerPage = 20;	
	
	private String httpPost(String url, UrlEncodedFormEntity data)
			throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(data);
		HttpResponse response = m_ahc.execute(httppost);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return html;
	}

	@Override
	public String getResults() {
		return m_results;
	}

	// from HTML:
	//    <option value="AW">Autor</option>
	//    <option value="TW">Titelwort</option>
	//    <option value="DW">Thema</option>
	//    <option value="PP">Standort</option>
	//    <option value="IS">ISBN/ISSN</option>
	//    <option value="PU">Verlag</option>
	//    <option value="PY">Ersch.-Jahr</option>
	//    <option value="LA">Sprache</option>	
	@Override
	public String[] getSearchFields() {
		return new String[] { 
				KEY_SEARCH_QUERY_TITLE, 
				KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_ISBN,
				KEY_SEARCH_QUERY_YEAR, 
				KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_PUBLISHER,
				KEY_SEARCH_QUERY_CATEGORY,
				KEY_SEARCH_QUERY_BRANCH};
	}

	@Override
	public String getLast_error() {
		return m_last_error;
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

	private void setMediaTypeFromImageFilename(SearchResult sr, String imagename) {
		String[] fparts1 = imagename.split("/");	                  // "images/31.gif.S"
		String[] fparts2 = fparts1[fparts1.length - 1].split("\\.");  // "31.gif.S"
		String lookup = fparts2[0];									  // "31"
		
		if (m_data.has("mediatypes")) {
			try {
				String typeStr = m_data.getJSONObject("mediatypes").getString(lookup);
				sr.setType(MediaType.valueOf(typeStr));
			} catch (Exception e) {
				// set no mediatype
			}
		}
	}
	
	/*
	 * Parser for non XML compliant html part: (the crazy way)
	 * Get text from <input> without end tag </input>
	 * 
	 * Example Offenburg:
	 *   <input type="radio" name="MT" value="MTYP10">Belletristik&nbsp;&nbsp;
	 *   Regex1: value="MTYP10".*?>([^<]+)
	 */
	private String parse_option_regex(Element inputTag) {
		String optStr = inputTag.val();
		String html = inputTag.parent().html();
		String result = optStr;
		
		String regex1 = "value=\"" + optStr + "\".*?>([^<]+)";
		String[] regexList = new String[]{regex1};
		
		for (String regex: regexList) {		
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			if (matcher.find()) {
			   result = matcher.group(1);
			   result = result.replaceAll("&nbsp;", " ").trim();
			   break;
			}
		}
		
		return result;
	}	
	/*
	 * ----- media types -----
     * Example Wuerzburg:
     *    <td ...><input type="checkbox" name="MT" value="1" ...></td>
     *    <td ...><img src="../image/spacer.gif.S" title="Buch"><br>Buch</td>
     *    
     * Example Friedrichshafen:
     *    <td ...><input type="checkbox" name="MS" value="1" ...></td>
     *    <td ...><img src="../image/spacer.gif.S" title="Buch"><br>Buch</td>
     *    
	 * Example Offenburg:
	 *   <input type="radio" name="MT" checked value="MTYP0">Alles&nbsp;&nbsp;
     *   <input type="radio" name="MT" value="MTYP10">Belletristik&nbsp;&nbsp;
     * Unfortunately Biber miss the end tag </input>, so opt.text() does not work!
     * (at least Offenburg)
     * 
     * Example Essen, Aschaffenburg:
	 *   <input type="radio" name="MT" checked value="MTYP0"><img src="../image/all.gif.S" title="Alles">
     *   <input type="radio" name="MT" value="MTYP7"><img src="../image/cdrom.gif.S" title="CD-ROM">
     *   
     * ----- Branches -----
     * Example Essen:  no closing </option> !!!
     *   <select name="AORT">
     *     <option value="ZWST1">Altendorf
     *   </select>
     * 
     * Example Hagen, W�rzburg, Friedrichshafen:
     *   <select name="ZW" class="sel1">
     *     <option selected value="ZWST0">Alle Bibliotheksorte</option>
     *   </select>
     *   
	 */
	private void extract_meta(Document doc) {
		m_metadata.open();
		m_metadata.clearMeta(m_library.getIdent());

		// get media types
		Elements mt_opts = doc.select("form input[name~=(MT|MS)]");
		for (int i = 0; i < mt_opts.size(); i++) {
			Element opt = mt_opts.get(i);
			if (!opt.val().equals("")) {
				String text = opt.text();
				if (text.length() == 0) {
					// text is empty, check layouts: 
					// Essen:	  <input name="MT"><img title="mediatype">
					// Schaffenb: <input name="MT"><img alt="mediatype">
					Element img = opt.nextElementSibling();
					if (img != null && img.tagName().equals("img")) {
						text = img.attr("title");
						if (text.equals("")) {
							text = img.attr("alt");
						}
					}
				}
				if (text.length() == 0) {
					// text is still empty, check table layout, Example Friedrichshafen
					// <td><input name="MT"></td> <td><img title="mediatype"></td>
					Element td1 = opt.parent();
					Element td2 = td1.nextElementSibling();
					if (td2 != null) {
						Elements td2Children = td2.select("img[title]");
						if (td2Children.size() > 0) {
							text = td2Children.get(0).attr("title");
						}
					}
				}
				if (text.length() == 0) {
					// text is still empty: missing end tag like Offenburg
					text = parse_option_regex(opt);
				}
				// ignore "all" because this is anyway added by this app
				if ((text.length() > 0) 
				  && !text.equalsIgnoreCase("alle") 
				  && !text.equalsIgnoreCase("alles")) {
					m_metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
							m_library.getIdent(), opt.val(), text);
				}
			}
		}
		
		// get branches
		Elements br_opts = doc.select("form select[name=ZW] option");
		for (int i = 0; i < br_opts.size(); i++) {
			Element opt = br_opts.get(i);
			// suppress "Alle Standorte", because "all" is added anyway by this app 
			if (!opt.val().equals("") && 
				!opt.text().equals("") && 
				!opt.text().startsWith("Alle")) {
				m_metadata.addMeta(MetaDataSource.META_TYPE_BRANCH,
						m_library.getIdent(), opt.val(), opt.text());
			}
		}
		
		m_metadata.close();
	}
	
	/* 
	 * Check connection to OPAC and get media types
	 * 
	 */
	@Override
	public void start() throws IOException, NotReachableException {
		HttpGet httpget;
		if (m_opac_dir.equals("opax")) 
			httpget = new HttpGet(m_opac_url + "/" + m_opac_dir + "/de/qsim.html.S");
		else
			httpget = new HttpGet(m_opac_url + "/" + m_opac_dir + "/de/qsim_main.S");
		
		HttpResponse response = m_ahc.execute(httpget);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}
		
		m_initialised = true;

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		Document doc = Jsoup.parse(html);

		m_metadata.open();
		if (!m_metadata.hasMeta(m_library.getIdent())) {
			m_metadata.close();
			extract_meta(doc);
		} else {
			m_metadata.close();
		}

	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		m_ahc = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(m_ahc.getParams(), "OpacApp.de");

		m_metadata = metadata;
		m_library = lib;
		m_data = lib.getData();


		try {
			m_opac_url = m_data.getString("baseurl");
			m_opac_dir = m_data.getString("opacdir");
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

	/* 
	 * HTTP Push
	 */
	@Override
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException {

		if (!m_initialised)
			start();
		
		String mediaType = getStringFromBundle(query, KEY_SEARCH_QUERY_CATEGORY);
		if (mediaType.equals("")) {
			mediaType = "MTYP0";	// key for "All"
		}
		
		String branch = getStringFromBundle(query, KEY_SEARCH_QUERY_BRANCH);
		if (branch.equals("")) {
			branch = "ZWST0";	// key for "All"
		}
		
		m_nameValuePairs.clear();
		m_nameValuePairs.add(new BasicNameValuePair("CNN1", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN2", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN3", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN4", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN5", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN6", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("CNN7", "AND"));
		m_nameValuePairs.add(new BasicNameValuePair("FLD1", getStringFromBundle(query, KEY_SEARCH_QUERY_AUTHOR)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD2", getStringFromBundle(query, KEY_SEARCH_QUERY_TITLE)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD3", getStringFromBundle(query, KEY_SEARCH_QUERY_KEYWORDA)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD4", getStringFromBundle(query, KEY_SEARCH_QUERY_SYSTEM)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD5", getStringFromBundle(query, KEY_SEARCH_QUERY_ISBN)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD6", getStringFromBundle(query, KEY_SEARCH_QUERY_PUBLISHER)));
		m_nameValuePairs.add(new BasicNameValuePair("FLD7", getStringFromBundle(query, KEY_SEARCH_QUERY_YEAR)));
		m_nameValuePairs.add(new BasicNameValuePair("FUNC", "qsel"));
		m_nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		m_nameValuePairs.add(new BasicNameValuePair("MT",   mediaType));
		m_nameValuePairs.add(new BasicNameValuePair("REG1", "AW"));
		m_nameValuePairs.add(new BasicNameValuePair("REG2", "TW"));
		m_nameValuePairs.add(new BasicNameValuePair("REG3", "DW"));
		m_nameValuePairs.add(new BasicNameValuePair("REG4", "PP"));
		m_nameValuePairs.add(new BasicNameValuePair("REG5", "IS"));
		m_nameValuePairs.add(new BasicNameValuePair("REG6", "PU"));
		m_nameValuePairs.add(new BasicNameValuePair("REG7", "PY"));
		m_nameValuePairs.add(new BasicNameValuePair("SHOW", "20")); // but result brings 50
		m_nameValuePairs.add(new BasicNameValuePair("SHOWSTAT", "N"));
		m_nameValuePairs.add(new BasicNameValuePair("ZW",   branch));
		m_nameValuePairs.add(new BasicNameValuePair("FROMPOS", "1"));

		return searchGetPage(1);
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#searchGetPage(int)
	 */
	@Override
	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException {
		
		int startNum = (page - 1) * numOfResultsPerPage + 1;

		// remove last element = "FROMPOS", and add a new one
		m_nameValuePairs.remove(m_nameValuePairs.size()-1);
		m_nameValuePairs.add(new BasicNameValuePair("FROMPOS", String.valueOf(startNum)));
		
		String html = httpPost(m_opac_url + "/" + m_opac_dir + "/query.C", 
							   new UrlEncodedFormEntity(m_nameValuePairs));
		return parse_search(html);		
	}

	/*
	 * result table format: 
	 * 		JSON "rows_per_hit" = 1: One <tr> per hit
	 * 		JSON "rows_per_hit" = 2: Two <tr> per hit  (default)
	 * <form>
	 * <table>
	 * <tr valign="top">
	 * 	 <td class="td3" ...><a href=...><img ...></a></td>  (row is optional, only in some bibs)
	 * 	 <td class="td2" ...><input ...></td>
	 * 	 <td width="34%">TITEL</td>
	 * 	 <td width="34%">&nbsp;</td>
	 * 	 <td width="6%" align="center">2009</td>
	 * 	 <td width="*" align="left">DVD0 Seew</td>
	 * </tr>
	 * <tr valign="top">
	 * 	 <td class="td3" ...>&nbsp;...</td>
	 *   <td class="td2" ...>&nbsp;...</td>
	 *   <td colspan="4" ...><font size="-1"><font class="p1">Erwachsenenbibliothek</font></font><div class="hr4"></div></td>
	 * </tr>
	 */
	private List<SearchResult> parse_search(String html) {
		List<SearchResult> results = new ArrayList<SearchResult>();
		Document doc = Jsoup.parse(html);
		Elements trList = doc.select("form table tr[valign]");  // <tr valign="top">
		Elements elem = null;
		int rows_per_hit = 2;
		
		try {
			int rows = m_data.getInt("rows_per_hit");
			rows_per_hit = rows;
		} catch (JSONException e) {			
		}

		// Overall search results
		// are very differently layouted, but have always the text:  
		// "....Treffer Gesamt (nnn)"
		Pattern pattern = Pattern.compile("Treffer Gesamt \\(([0-9]+)\\)");
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			m_results = "Treffer Gesamt: " + matcher.group(1);
		} else {
			m_results = "";
		}

		
		// limit to 20 entries
		int numOfEntries = trList.size() / rows_per_hit;	// two rows per entry
		if (numOfEntries > numOfResultsPerPage)
			numOfEntries = numOfResultsPerPage;

		for (int i = 0; i < numOfEntries; i++) {
			Element tr = trList.get(i * rows_per_hit);
			SearchResult sr = new SearchResult();
			
			// ID as href tag
			elem = tr.select("td a");
			if (elem.size() > 0) {
				String hrefID = elem.get(0).attr("href");
				sr.setId(hrefID);
			} else {
				// no ID as href found, look for the ID in the input form
				elem = tr.select("td input");
				if (elem.size() > 0) {
					String nameID = elem.get(0).attr("name").trim();
					String hrefID = "/" + m_opac_dir + "/ftitle.C?LANG=de&FUNC=full&" + nameID + "=YES";
					sr.setId(hrefID);
				}
			}
			
			// media type
			try {
				elem = tr.select("td img");
				if (elem.size() > 0) {
					setMediaTypeFromImageFilename(sr,elem.get(0).attr("src"));
				}
			} catch (NumberFormatException e) {
				
			}

			// description
			String desc = "";
			try {
				// array "searchtable" list the column numbers of the description
				JSONArray searchtable = m_data.getJSONArray("searchtable");
				for (int j=0; j < searchtable.length(); j++)
				{
					int colNum = searchtable.getInt(j);
					if (j>0)
						desc = desc + "<br />";
					desc = desc + tr.child(colNum).html();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// remove links "<a ...>...</a>
			// needed for Friedrichshafen: "Warenkorb", "Vormerkung"
			//            Herford: "Medienkorb"
			desc = desc.replaceAll("<a .*?</a>", ""); 
			sr.setInnerhtml( desc );
			
			// number
			sr.setNr(i / rows_per_hit);
			results.add(sr);
		}
		
		//m_resultcount = results.size();
		return results;
	}

	
	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#getResultById(java.lang.String)
	 */
	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException {
		if (!m_initialised)
			start();

		// normally full path like   "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
		// but sometimes (Wuerzburg)       "ftitle.C?LANG=de&FUNC=full&331313252=YES"
		if (! id.startsWith("/")) {
			id = "/" + m_opac_dir + "/" + id;
		}
		
		HttpGet httpget = new HttpGet(m_opac_url + id);

		HttpResponse response = m_ahc.execute(httpget);

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();

		return parse_result(html);
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#getResult(int)
	 */
	@Override
	public DetailledItem getResult(int position) throws IOException {
		// not needed, normall all search results should have an ID,
		// so getResultById() is called
		return null;
	}

	/*
	 * Two-column table inside of a form
	 * 		1st column is category, e.g. "Verfasser"
	 * 		2nd column is content, e.g.  "Bach, Johann Sebastian"
	 * In some rows, the 1st column is empty, 
	 * then 2nd column is continued text from row above.
	 * 
	 * Some libraries have a second section for the copies in stock (Exemplare).
	 * This 2nd section has reverse layout.
	 * 
	 * |-------------------|
	 * | Subject | Content |
	 * |-------------------|
	 * | Subject | Content |
	 * |-------------------|
	 * |         | Content |
	 * |-------------------|
	 * | Subject | Content |
	 * |-------------------------------------------------|
	 * |         | Site    | Signatur| ID      | State   |
	 * |-------------------------------------------------|
	 * |         | Content | Content | Content | Content |
	 * |-------------------------------------------------|
	 */
	private DetailledItem parse_result(String html) {
		DetailledItem item = new DetailledItem();

		Document document = Jsoup.parse(html);

		Elements rows = document.select("html body form table tr");
		//Elements rows = document.select("html body div form table tr");
		
		//Element rowReverseSubject = null;
		Detail detail = null;
		
		// prepare copiestable
		ContentValues copy_last_content = null;
		int copy_row = 0;
		
		String[] copy_keys = new String[] { 
				DetailledItem.KEY_COPY_BARCODE,		// "barcode";
				DetailledItem.KEY_COPY_BRANCH,		// "zst";
				DetailledItem.KEY_COPY_DEPARTMENT,	// "abt";
				DetailledItem.KEY_COPY_LOCATION,	// "ort"; 
				DetailledItem.KEY_COPY_STATUS,		// "status";
				DetailledItem.KEY_COPY_RETURN,		// "rueckgabe";
				DetailledItem.KEY_COPY_RESERVATIONS // "vorbestellt";
			};
		int[] copy_map = new int[] { -1, -1, -1, -1, -1, -1, -1 };

		try {
			JSONArray map = m_data.getJSONArray("copiestable");
			for (int i=0; i < copy_keys.length; i++) {
				copy_map[i] = map.getInt(i);
			}
		} catch (Exception e) {
			// "copiestable" is optional
		}

		// go through all rows
		for (Element row : rows) {
			Elements columns = row.select("td");

			if (columns.size() == 2) {
				// HTML tag "&nbsp;" is encoded as 0xA0
				String firstColumn = columns.get(0).text().replace("\u00a0"," ").trim();
				String secondColumn = columns.get(1).text().replace("\u00a0"," ").trim();
				
				if (firstColumn.length() > 0) {
					// 1st column is category
					if (firstColumn.equalsIgnoreCase("titel")) {
						detail = null;
						item.setTitle(secondColumn);
					} else {
						detail = new Detail(firstColumn, secondColumn);
						item.getDetails().add(detail);
					}
				} else {
					// 1st column is empty, so it is an extension to last category
					if (detail != null) {
						String content = detail.getContent() + "\n" + secondColumn;
						detail.setContent(content);
					} else {
						// detail==0, so it's the first row
						// check if there is an amazon image
						if (columns.get(0).select("a img[src]").size() > 0) {
							item.setCover(columns.get(0).select("a img").first().attr("src"));
						}

					}
				}
			} else if (columns.size() > 2) {
				// This is the second section: the copies in stock ("Exemplare")
				// With reverse layout: first row is headline, skipped via (copy_row > 0)
				if (copy_row > 0) {
					ContentValues e = new ContentValues();
					for (int j = 0; j < copy_keys.length; j++) {
						int col = copy_map[j];
						if (col > -1) {
							String text = "";
							if (copy_keys[j].equals(DetailledItem.KEY_COPY_BRANCH))
							{
								// for "Standort" only use ownText() to suppress Link "Wegweiser"
								text = columns.get(col).ownText().replace("\u00a0"," ").trim();
							}							
							if (text.length() == 0) {
								// text of children
								text = columns.get(col).text().replace("\u00a0"," ").trim();
							}
							if (text.length() == 0) {
								// empty table cell, take the one above
								// this is sometimes the case for "Standort"
								if (copy_keys[j].equals(DetailledItem.KEY_COPY_STATUS)) {
									// but do it not for Status
									text = " ";
								} else {
									text = copy_last_content.getAsString(copy_keys[j]);
								} 
							}
							e.put(copy_keys[j], text);
						}
					}
					item.addCopy(e);
					copy_last_content = e;
				}//ignore 1st row
				copy_row++;
				
			}//if columns.size
		}//for rows
		
		return item;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#reservation(java.lang.String, de.geeksfactory.opacclient.objects.Account, int, java.lang.String)
	 */
	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		// TODO reservations not yet supported
		return null;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#prolong(de.geeksfactory.opacclient.objects.Account, java.lang.String)
	 * 
	 * Offenburg, prolong negative result:
	 * <table border="1" width="100%">
	 *   <tr>
	 *   	<th ...>Nr</th>
	 *   	<th ...>Signatur / Kurztitel</th>
	 *   	<th ...>F&auml;llig</th>
	 *   	<th ...>Status</th>
	 *   </tr>
	 *   <tr>
	 *   	<td ...>101103778</td>
	 *   	<td ...>Hyde / Hyde, Anthony: Der Mann aus </td>
	 *   	<td ...>09.04.2013</td>
	 *   	<td ...><font class="p1">verl&auml;ngerbar ab 03.04.13, nicht verl&auml;ngert</font>
	 *   	  <br>Bitte wenden Sie sich an Ihre Bibliothek!</td>
	 *   </tr>
	 * </table>
	 * 
	 * Offenburg, prolong positive result:
	 * TO BE DESCRIBED
	 * 
	 */
	@Override
	public boolean prolong(Account account, String media) throws IOException {

		// prolong media via http POST
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair(media, "YES"));
		nameValuePairs.add(new BasicNameValuePair("DUM1", ""));
		nameValuePairs.add(new BasicNameValuePair("DUM2", ""));
		nameValuePairs.add(new BasicNameValuePair("DUM3", ""));
		nameValuePairs.add(new BasicNameValuePair("FUNC", "verl"));
		nameValuePairs.add(new BasicNameValuePair("LANG", "de"));

		String html = httpPost(m_opac_url + "/" + m_opac_dir + "/verl.C", 
				   new UrlEncodedFormEntity(nameValuePairs));

		Document doc = Jsoup.parse(html);
		
		// Check result:		
		// Search cell with content "Status", then take text from cell below.
		// Hopefully this works also with other libraries.
		Elements rowElements = doc.select("table tr");
		
		// rows: skip last row because below we will look forward one row 
		for (int i = 0; i < rowElements.size() - 1; i++) {
			Element tr = rowElements.get(i);
			Elements tdList = tr.children();  // <th> or <td>
			
			// columns: look for "Status"
			for (int j = 0; j < tdList.size(); j++) {
				String cellText = tdList.get(j).text().trim();
				if (cellText.equals("Status")) {
					// "Status" found, check cell below
					String resultText = rowElements.get(i+1).child(j).text().trim();
					if (resultText.startsWith("verlängert")) {
						return true;
					} else {
						m_last_error = resultText;
						return false;
					}
				}
			}
		}
		m_last_error = "unknown result";  // should not occur

		return false;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#cancel(de.geeksfactory.opacclient.objects.Account, java.lang.String)
	 */
	@Override
	public boolean cancel(Account account, String media) throws IOException {
		// TODO reservations not yet supported
		return false;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#account(de.geeksfactory.opacclient.objects.Account)
	 * 
	 * POST-format:
	 * BENUTZER	xxxxxxxxx
	 * FUNC		medk
	 * LANG		de
	 * PASSWORD	ddmmyyyy
	 */
	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		
		// get media list via http POST
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("FUNC", "medk"));
		nameValuePairs.add(new BasicNameValuePair("LANG", "de"));
		nameValuePairs.add(new BasicNameValuePair("BENUTZER", account.getName()));
		nameValuePairs.add(new BasicNameValuePair("PASSWORD", account.getPassword()));

		String html = httpPost(m_opac_url + "/" + m_opac_dir + "/user.C", 
				   new UrlEncodedFormEntity(nameValuePairs));

		Document doc = Jsoup.parse(html);

		// Error recognition
		// <title>OPAC Fehler</title>
		if (doc.title().contains("Fehler")) {
			String errText = "unknown error";
			Elements elTable = doc.select("table");
			if (elTable.size() > 0) {
				errText = elTable.get(0).text();
			}
			m_last_error = errText;
			return null;
		}
		
		// parse result list
		List<ContentValues> medien = new ArrayList<ContentValues>();

		JSONArray copymap = m_data.getJSONArray("accounttable");

		String[] copymap_keys = new String[] { 
				AccountData.KEY_LENT_BARCODE,
				AccountData.KEY_LENT_AUTHOR, 
				AccountData.KEY_LENT_TITLE,
				AccountData.KEY_LENT_DEADLINE, 
				AccountData.KEY_LENT_STATUS,
				AccountData.KEY_LENT_BRANCH,
				AccountData.KEY_LENT_LENDING_BRANCH, 
				AccountData.KEY_LENT_LINK };
		int copymap_num = copymap_keys.length;
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		Elements rowElements = doc.select("form[name=medkl] table tr");
		
		// rows: skip 1st row -> title row
		for (int i = 1; i < rowElements.size(); i++) {
			Element tr = rowElements.get(i);
			ContentValues e = new ContentValues();
		
			// columns: all elements of one media
			for (int j = 0; j < copymap_num; j++) {
				if (copymap.getInt(j) > -1) {
					String key = copymap_keys[j];
					String value = tr.child(copymap.getInt(j)).text();
					// Author and Title is the same field: "autor: title"
					if (key.equals(AccountData.KEY_LENT_AUTHOR)) {
						// Autor: remove everything starting at ":"
						value = value.replaceFirst("\\:.*", "").trim();
					} else if (key.equals(AccountData.KEY_LENT_TITLE)) {
						// Title: remove everything up to ":"
						value = value.replaceFirst(".*\\:", "").trim();
					}
					e.put(key, value);
				}
			}
			// calculate lent timestamp for notification purpose
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

		// reservations not yet supported (not supported for "Offenburg")
		List<ContentValues> reservations = new ArrayList<ContentValues>();


		AccountData res = new AccountData(account.getId());
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
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		// id is normally full path like   "/opac/ftitle.C?LANG=de&FUNC=full&331313252=YES"
		// but sometimes (Wuerzburg)       "ftitle.C?LANG=de&FUNC=full&331313252=YES"
		if (! id.startsWith("/")) {
			id = "/" + m_opac_dir + "/" + id;
		}
		
		return m_opac_url + id;
	}
	
	@Override
	public int getSupportFlags() {
		return 0;
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		return false;
	}
}
