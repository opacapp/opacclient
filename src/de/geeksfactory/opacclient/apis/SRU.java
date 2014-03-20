package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class SRU extends BaseApi implements OpacApi {
	
	protected String opac_url = "";
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected Library library;
	protected int resultcount = 10;
	private String currentSearchParams;
	private Document searchDoc;
	
	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();
	static {
		defaulttypes.put("print", MediaType.BOOK);
		defaulttypes.put("large print", MediaType.BOOK);
		defaulttypes.put("braille", MediaType.UNKNOWN);
		defaulttypes.put("electronic", MediaType.EBOOK);
		defaulttypes.put("microfiche", MediaType.UNKNOWN);
		defaulttypes.put("microfilm", MediaType.UNKNOWN);
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

	@Override
	public void start() throws IOException, NotReachableException {
		metadata.open();
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			//extract_meta();
		} else {
			metadata.close();
		}
	}
	
	protected int addParameters(Bundle query, String key, String searchkey,
			StringBuilder params, int index) {
		if (!query.containsKey(key) || query.getString(key).equals(""))
			return index;
		params.append(searchkey + "%3D" + query.getString(key));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException, OpacErrorException {
		StringBuilder params = new StringBuilder();

		int index = 0;
		start();

		index = addParameters(query, KEY_SEARCH_QUERY_FREE, "pica.ALL",
				params, index);
		
		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}
		currentSearchParams = params.toString();
		String xml = httpGet(opac_url +
				"?version=1.1&operation=searchRetrieve&maximumRecords=" + resultcount +
				"&recordSchema=mods&sortKeys=relevance,,1&query=" + currentSearchParams,
				getDefaultEncoding());
		Log.d("Opac", xml);
		
		return parse_result(xml);
	}

	private SearchRequestResult parse_result(String xml) {
		searchDoc = Jsoup.parse(xml, "", Parser.xmlParser());
		int resultcount = 0;
		List<SearchResult> results = new ArrayList<SearchResult>();
		
		resultcount = Integer.valueOf(searchDoc.select("zs|numberOfRecords").text());
		
		Elements records = searchDoc.select("zs|records > zs|record");
		int i = 0;
		for(Element record:records) {
			SearchResult sr = new SearchResult();
			String title = getDetail(record, "titleInfo title");
			String firstName = getDetail(record, "name > namePart[type=given]");
			String lastName = getDetail(record, "name > namePart[type=family]");
			String year = getDetail(record, "dateIssued");
			String mType = getDetail(record, "physicalDescription > form");
			String isbn =  getDetail(record, "identifier[type=isbn]").replaceAll("[^\\d.]", ""); //Remove all characters that aren't digits
			String coverUrl = getDetail(record, "url[displayLabel=C Cover]");
			String additionalInfo = firstName + " " + lastName + ", " + year;
			sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);
			sr.setType(defaulttypes.get(mType));
			sr.setNr(i);
			if (coverUrl.equals(""))
				sr.setCover("http://images.amazon.com/images/P/" + isbn + ".01.THUMBZZZ");
			else
				sr.setCover(coverUrl);
			results.add(sr);
			i++;
		}
		
		return new SearchRequestResult(results, resultcount, 1);
	}

	private String getDetail(Element record, String selector) {
		if(record.select(selector).size() > 0) {
			return record.select(selector).first().text();
		} else {
			return "";
		}
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		if (!initialised)
			start();
		
		String xml = httpGet(opac_url +
				"?version=1.1&operation=searchRetrieve&maximumRecords=" + resultcount +
				"&recordSchema=mods&sortKeys=relevance,,1&startRecord=" +
				String.valueOf(page*resultcount + 1) + "&query=" + currentSearchParams,
				getDefaultEncoding());
		return parse_result(xml);
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	private DetailledItem parse_detail(Element record) {
		String title = getDetail(record, "titleInfo title");
		String firstName = getDetail(record, "name > namePart[type=given]");
		String lastName = getDetail(record, "name > namePart[type=family]");
		String year = getDetail(record, "dateIssued");
		String desc = getDetail(record, "abstract");
		String isbn = getDetail(record, "identifier[type=isbn]").replaceAll("[^\\d.]", ""); //Remove all characters that aren't digits
		String coverUrl = getDetail(record, "url[displayLabel=C Cover]");
		
		DetailledItem item = new DetailledItem();
		item.setTitle(title);
		item.addDetail(new Detail("Autor", firstName + " " + lastName));
		item.addDetail(new Detail("Jahr", year));
		item.addDetail(new Detail("Beschreibung", desc));
		if (coverUrl.equals("") && isbn.length() > 0)
			item.setCover("http://images.amazon.com/images/P/" + isbn + ".01.L");
		else if (!coverUrl.equals(""))
			item.setCover(coverUrl);
		
		if(isbn.length() > 0) {
			item.addDetail(new Detail("ISBN", isbn));
		}
		
		return item;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		Log.d("Opac", String.valueOf(searchDoc.select("zs|records > zs|record").size()));
		Log.d("Opac", String.valueOf(position));
		return parse_detail(searchDoc.select("zs|records > zs|record").get(position));
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
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_FREE };
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

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

}
