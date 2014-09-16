/**
 * Copyright (C) 2014 by Johan von Forstner under the MIT license:
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * 
 * @author Johan von Forstner, 06.04.2014
 * 
 *         WebOPAC.net, Version 2.2.70 gestartet mit Gemeindebibliothek
 *         Nürensdorf (erstes Google-Suchergebnis)
 * 
 *         weitere kompatible Bibliotheken:
 *         https://www.google.de/search?q=webOpac
 *         .net%202.1.30%20powered%20by%20winMedio
 *         .net&qscrl=1#q=%22webOpac.net+2.2
 *         .70+powered+by+winMedio.net%22+inurl%3Awinmedio&qscrl=1&start=0
 * 
 *         Unterstützt bisher nur Katalogsuche, Accountunterstüzung könnte (wenn
 *         keine Kontodaten verfügbar sind) über den Javascript-Code
 *         reverse-engineered werden:
 *         http://www.winmedio.net/nuerensdorf/de/mobile
 *         /GetScript.ashx?id=mobile.de.min.js&v=20140122
 * 
 */

public class WebOpacNet extends BaseApiCompat implements OpacApi {

	protected String opac_url = "";
	protected MetaDataSource metadata;
	protected JSONObject data;
	protected Library library;
	protected Map<String, String> query;

	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();

	static {
		defaulttypes.put("1", MediaType.BOOK);
		defaulttypes.put("2", MediaType.CD_MUSIC);
		defaulttypes.put("3", MediaType.AUDIOBOOK);
		defaulttypes.put("4", MediaType.DVD);
		defaulttypes.put("5", MediaType.CD_SOFTWARE);
		defaulttypes.put("8", MediaType.MAGAZINE);
	}

	@Override
	public void start() throws IOException, NotReachableException {
		String text = httpGet(opac_url + "/de/mobile/GetRestrictions.ashx",
				getDefaultEncoding());
		try {
			JSONArray filters = new JSONObject(text)
					.getJSONArray("restrcontainers");
			JSONArray mediatypes = null;
			int i = 0;
			while (mediatypes == null && i < filters.length()) {
				JSONObject filter = filters.getJSONObject(i);
				if (filter.getString("querytyp").equals("XM"))
					mediatypes = filter.getJSONArray("restrictions");
				i++;
			}
			try {
				metadata.open();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			metadata.clearMeta(library.getIdent());

			for (i = 0; i < mediatypes.length(); i++) {
				JSONObject mediatype = mediatypes.getJSONObject(i);
				String id = mediatype.getString("id");
				String name = mediatype.getString("bez");
				metadata.addMeta(MetaDataSource.META_TYPE_CATEGORY,
						library.getIdent(), id, name);
			}

			metadata.close();
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void extract_meta() {
		try {
			metadata.open();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			extract_meta();
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
			throw new RuntimeException(e);
		}
	}

	@Override
	public SearchRequestResult search(Map<String, String> query)
			throws IOException, NotReachableException, OpacErrorException {
		this.query = query;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		start();

		int index = buildParams(query, params, 1);

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}

		String json = httpGet(opac_url + "/de/mobile/GetMedien.ashx"
				+ buildHttpGetParams(params, getDefaultEncoding()),
				getDefaultEncoding());

		return parse_search(json, 1);
	}

	protected int addParameters(Map<String, String> query, String key,
			int searchkey, StringBuilder params, int index) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return index;

		params.append("|" + String.valueOf(searchkey) + "|" + query.get(key));
		return index + 1;
	}

	protected void addFilters(Map<String, String> query, String key,
			String searchkey, StringBuilder params) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return;

		params.append("&" + String.valueOf(searchkey) + "=" + query.get(key));
	}

	private SearchRequestResult parse_search(String text, int page)
			throws OpacErrorException {
		if (!text.equals("")) {
			try {
				List<SearchResult> results = new ArrayList<SearchResult>();
				JSONObject json = new JSONObject(text);
				int total_result_count = Integer.parseInt(json
						.getString("totalcount"));

				JSONArray resultList = json.getJSONArray("mobmeds");
				for (int i = 0; i < resultList.length(); i++) {
					JSONObject resultJson = resultList.getJSONObject(i);
					SearchResult result = new SearchResult();
					result.setId(resultJson.getString("medid"));

					String title = resultJson.getString("titel");
					String publisher = resultJson.getString("verlag");
					String series = resultJson.getString("reihe");
					String html = "<b>" + title + "</b><br />" + publisher
							+ ", " + series;

					String type = resultJson.getString("iconurl").substring(12,
							13);
					result.setType(defaulttypes.get(type));

					result.setInnerhtml(html);

					if (resultJson.getString("imageurl").length() > 0)
						result.setCover(resultJson.getString("imageurl"));

					results.add(result);
				}

				return new SearchRequestResult(results, total_result_count,
						page);
			} catch (JSONException e) {
				e.printStackTrace();
				throw new OpacErrorException("Fehler beim Parsen: "
						+ e.getMessage());
			}
		} else {
			return new SearchRequestResult(new ArrayList<SearchResult>(), 0,
					page);
		}

	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		start();

		int index = buildParams(query, params, page);

		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}

		String json = httpGet(opac_url + "/de/mobile/GetMedien.ashx"
				+ buildHttpGetParams(params, getDefaultEncoding()),
				getDefaultEncoding());

		return parse_search(json, page);
	}

	private int buildParams(Map<String, String> query,
			List<NameValuePair> params, int page) {
		int index = 0;

		StringBuilder queries = new StringBuilder();
		queries.append("erw:0");

		index = addParameters(query, KEY_SEARCH_QUERY_FREE, 1, queries, index);
		index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, 2, queries, index);
		index = addParameters(query, KEY_SEARCH_QUERY_TITLE, 3, queries, index);
		index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA, 6, queries,
				index);
		index = addParameters(query, KEY_SEARCH_QUERY_ISBN, 9, queries, index);
		index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, 8, queries,
				index);
		addFilters(query, KEY_SEARCH_QUERY_YEAR, "EJ", queries);
		addFilters(query, KEY_SEARCH_QUERY_CATEGORY, "XM", queries);

		params.add(new BasicNameValuePair("q", queries.toString()));
		params.add(new BasicNameValuePair("p", String.valueOf(page - 1)));
		params.add(new BasicNameValuePair("s", "2"));
		params.add(new BasicNameValuePair("asc", "1"));

		return index;
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id", id));
		params.add(new BasicNameValuePair("orientation", "1"));

		String json = httpGet(opac_url + "/de/mobile/GetDetail.ashx"
				+ buildHttpGetParams(params, getDefaultEncoding()),
				getDefaultEncoding());

		return parse_detail(json);
	}

	private DetailledItem parse_detail(String text) throws OpacErrorException {
		try {
			DetailledItem result = new DetailledItem();
			JSONObject json = new JSONObject(text);

			result.setTitle(json.getString("titel"));
			result.setCover(json.getString("imageurl"));
			result.setId(json.getString("medid"));

			// Details
			JSONArray info = json.getJSONArray("medium");
			for (int i = 0; i < info.length(); i++) {
				JSONObject detailJson = info.getJSONObject(i);
				String name = detailJson.getString("bez");
				String value = "";

				JSONArray values = detailJson.getJSONArray("values");
				for (int j = 0; j < values.length(); j++) {
					JSONObject valJson = values.getJSONObject(j);
					if (j != 0)
						value += ", ";
					String content = valJson.getString("dval");
					content = content.replaceAll("<span[^>]*>", "");
					content = content.replaceAll("</span>", "");
					value += content;
				}
				Detail detail = new Detail(name, value);
				result.addDetail(detail);
			}

			// Copies
			JSONArray copies = json.getJSONArray("exemplare");
			for (int i = 0; i < copies.length(); i++) {
				JSONObject copyJson = copies.getJSONObject(i);
				Map<String, String> copy = new HashMap<String, String>();

				JSONArray values = copyJson.getJSONArray("rows");
				for (int j = 0; j < values.length(); j++) {
					JSONObject valJson = values.getJSONObject(j);
					String name = valJson.getString("bez");
					String value = valJson.getJSONArray("values")
							.getJSONObject(0).getString("dval");
					if (!value.equals("")) {
						if (name.equals("Exemplarstatus"))
							copy.put(DetailledItem.KEY_COPY_STATUS, value);
						else if (name.equals("Signatur"))
							copy.put(DetailledItem.KEY_COPY_SHELFMARK, value);
						else if (name.equals("Standort"))
							copy.put(DetailledItem.KEY_COPY_LOCATION, value);
						else if (name.equals("Themenabteilung")) {
							if (copy.containsKey(DetailledItem.KEY_COPY_DEPARTMENT))
								value = copy
										.get(DetailledItem.KEY_COPY_DEPARTMENT)
										+ value;
							copy.put(DetailledItem.KEY_COPY_DEPARTMENT, value);
						} else if (name.equals("Themenbereich")) {
							if (copy.containsKey(DetailledItem.KEY_COPY_DEPARTMENT))
								value = copy
										.get(DetailledItem.KEY_COPY_DEPARTMENT)
										+ value;
							copy.put(DetailledItem.KEY_COPY_DEPARTMENT, value);
						}
					}
				}
				result.addCopy(copy);
			}

			return result;

		} catch (JSONException e) {
			e.printStackTrace();
			throw new OpacErrorException("Fehler beim Parsen: "
					+ e.getMessage());
		}

	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSearchFieldsCompat() {
		return new String[] { KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_AUTHOR,
				KEY_SEARCH_QUERY_TITLE, KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_ISBN, KEY_SEARCH_QUERY_YEAR,
				KEY_SEARCH_QUERY_CATEGORY, KEY_SEARCH_QUERY_PUBLISHER };
	}

	@Override
	public boolean isAccountSupported(Library library) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	@Deprecated
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
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id", id));

		String url;
		try {
			url = opac_url + "/default.aspx"
					+ buildHttpGetParams(params, getDefaultEncoding());
			return url;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
	}

	private String buildHttpGetParams(List<NameValuePair> params,
			String encoding) throws UnsupportedEncodingException {
		String string = "?";
		for (NameValuePair pair : params) {
			String name = URLEncoder.encode(pair.getName(), encoding);
			String value = URLEncoder.encode(pair.getValue(), encoding);
			string += name + "=" + value + "&";
		}
		string = string.substring(0, string.length() - 1);
		return string;
	}

	@Override
	protected String getDefaultEncoding() {
		return "UTF-8";
	}

}
