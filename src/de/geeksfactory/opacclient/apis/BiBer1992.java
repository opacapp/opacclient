/**
 * BiBer gestartet mit Stadtbibliothek Offenburg
 * start URL: http://217.86.216.47/opac/de/qsim_frm.html.S
 * 
 *  open:
 *  issue #23: Basic support for library system "Biber" -> Essen
 *  issue #32: Integration of "BiBer" (copyright 2006) -> Karlsruhe https://opac.karlsruhe.de/ 
 *  issue #33: Integration of "BiBer" (copyright 1992) -> Essen
 */
package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
 * Features:
 * In getResult(), mixed table layout is supported: column-wise and row-wise
 * In getResult(), amazon bitmaps are supported
 * 
 * Katalogsuche tested with
 * 
 * Name				Media	amazon	mixed	Media	
 * 					type	Bitmaps	Table	types
 * 					images			Layout	search
 * ---------------------------------------------------------
 * Offenburg		ok		no		no		yes
 * Essen			no		no		no		yes
 * Hagen        	ok		yes		yes		yes
 * Würzburg			ok		yes		yes		yes
 * Friedrichshafen: ok		yes		yes		yes
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
				KEY_SEARCH_QUERY_CATEGORY};
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
     * Example Essen:
	 *   <input type="radio" name="MT" checked value="MTYP0"><img src="../image/all.gif.S" title="Alles">
     *   <input type="radio" name="MT" value="MTYP7"><img src="../image/cdrom.gif.S" title="CD-ROM">
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
					// text is empty, check layout Essen: 
					// <input name="MT"><img title="mediatype">
					Element img = opt.nextElementSibling();
					if (img != null && img.tagName().equals("img")) {
						text = img.attr("title");
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
		
		HttpPost httppost = new HttpPost(m_opac_url + "/" + m_opac_dir + "/query.C");		
		httppost.setEntity(new UrlEncodedFormEntity(m_nameValuePairs));
		HttpResponse response = m_ahc.execute(httppost);

		if (response.getStatusLine().getStatusCode() == 500) {
			throw new NotReachableException();
		}

		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return parse_search(html);		
	}

	/*
	 * result table format: Two <tr> per hit
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

		// limit to 20 entries
		int numOfEntries = trList.size() / 2;	// two rows per entry
		if (numOfEntries > numOfResultsPerPage)
			numOfEntries = numOfResultsPerPage;

		for (int i = 0; i < numOfEntries; i++) {
			Element tr = trList.get(i*2);
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
			sr.setInnerhtml( desc );
			
			// number
			sr.setNr(i/2);
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
	 * Some libraries have mixed layout.
	 * Like above but additionally in the two last rows reverse layout.
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
	 * |         | Subject | Subject | Subject | Subject |
	 * |-------------------------------------------------|
	 * |         | Content | Content | Content | Content |
	 * |-------------------------------------------------|
	 */
	private DetailledItem parse_result(String html) {
		DetailledItem item = new DetailledItem();

		Document document = Jsoup.parse(html);

		Elements rows = document.select("html body form table tr");
		//Elements rows = document.select("html body div form table tr");
		
		Element rowReverseSubject = null;

		Detail detail = null;

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
				// here the data is suddenly column by column, instead row by row
				if (rowReverseSubject == null) {
					// store the reverse row with all the subjects
					rowReverseSubject = row;
				}
				else
				{
					// reverse row with all the content
					Elements subjectColumns = rowReverseSubject.select("td");
					Elements contentColumns = row.select("td");
					if (subjectColumns.size() == contentColumns.size())	{
						for (int i=0; i < subjectColumns.size(); i++) {
							String subjectText = subjectColumns.get(i).text().replace("\u00a0"," ").trim();
							String contentText = contentColumns.get(i).text().replace("\u00a0"," ").trim();
							if (subjectText.length() > 0 && contentText.length() > 0) {
								detail = new Detail(subjectText, contentText);
								item.getDetails().add(detail);
							}							
						}
					}
					// clean up
					rowReverseSubject = null;
					detail = null;
				}
			}
		}
		
		return item;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#reservation(java.lang.String, de.geeksfactory.opacclient.objects.Account, int, java.lang.String)
	 */
	@Override
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#prolong(de.geeksfactory.opacclient.objects.Account, java.lang.String)
	 */
	@Override
	public boolean prolong(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#cancel(de.geeksfactory.opacclient.objects.Account, java.lang.String)
	 */
	@Override
	public boolean cancel(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.geeksfactory.opacclient.apis.OpacApi#account(de.geeksfactory.opacclient.objects.Account)
	 */
	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		// TODO Auto-generated method stub
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
		return null;
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
