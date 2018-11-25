package de.geeksfactory.opacclient.apis;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.ISBNTools;

public class SRU extends ApacheBaseApi implements OpacApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("print", MediaType.BOOK);
        defaulttypes.put("large print", MediaType.BOOK);
        defaulttypes.put("braille", MediaType.UNKNOWN);
        defaulttypes.put("electronic", MediaType.EBOOK);
        defaulttypes.put("microfiche", MediaType.UNKNOWN);
        defaulttypes.put("microfilm", MediaType.UNKNOWN);
        defaulttypes.put("Tontraeger", MediaType.AUDIOBOOK);
    }

    protected String opac_url = "";
    protected JSONObject data;
    protected int resultcount = 10;
    protected String shareUrl;
    private String currentSearchParams;
    private Document searchDoc;
    private HashMap<String, String> searchQueries = new HashMap<>();
    private String idSearchQuery;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            JSONObject searchQueriesJson = data.getJSONObject("searchqueries");
            addSearchQueries(searchQueriesJson);
            if (data.has("sharelink")) {
                shareUrl = data.getString("sharelink");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSearchQueries(JSONObject searchQueriesJson) {
        String[] queries = {KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_TITLE,
                KEY_SEARCH_QUERY_AUTHOR, KEY_SEARCH_QUERY_KEYWORDA,
                KEY_SEARCH_QUERY_KEYWORDB, KEY_SEARCH_QUERY_BRANCH,
                KEY_SEARCH_QUERY_HOME_BRANCH, KEY_SEARCH_QUERY_ISBN,
                KEY_SEARCH_QUERY_YEAR, KEY_SEARCH_QUERY_YEAR_RANGE_START,
                KEY_SEARCH_QUERY_YEAR_RANGE_END, KEY_SEARCH_QUERY_SYSTEM,
                KEY_SEARCH_QUERY_AUDIENCE, KEY_SEARCH_QUERY_PUBLISHER,
                KEY_SEARCH_QUERY_CATEGORY, KEY_SEARCH_QUERY_BARCODE,
                KEY_SEARCH_QUERY_LOCATION, KEY_SEARCH_QUERY_DIGITAL};
        for (String query : queries) {
            if (searchQueriesJson.has(query)) {
                try {
                    searchQueries
                            .put(query, searchQueriesJson.getString(query));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (searchQueriesJson.has("id")) {
                idSearchQuery = searchQueriesJson.getString("id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected int addParameters(Map<String, String> query, String key,
            String searchkey, StringBuilder params, int index) {
        if (!query.containsKey(key) || query.get(key).equals("")) {
            return index;
        }
        if (index != 0) {
            params.append("%20and%20");
        }
        params.append(searchkey).append("%3D").append(query.get(key));
        return index + 1;

    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queryList)
            throws IOException, OpacErrorException {
        Map<String, String> query = searchQueryListToMap(queryList);
        StringBuilder params = new StringBuilder();

        int index = 0;
        start();

        for (String parameter : searchQueries.keySet()) {
            index = addParameters(query, parameter,
                    searchQueries.get(parameter), params, index);
        }

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        currentSearchParams = params.toString();
        String xml = httpGet(opac_url
                + "?version=1.1&operation=searchRetrieve&maximumRecords="
                + resultcount
                + "&recordSchema=mods&sortKeys=relevance,,1&query="
                + currentSearchParams, getDefaultEncoding());

        return parse_result(xml);
    }

    private SearchRequestResult parse_result(String xml)
            throws OpacErrorException {
        searchDoc = Jsoup.parse(xml, "", Parser.xmlParser());
        if (searchDoc.select("diag|diagnostic").size() > 0) {
            throw new OpacErrorException(searchDoc.select("diag|message")
                                                  .text());
        }

        int resultcount;
        List<SearchResult> results = new ArrayList<>();

        resultcount = Integer.valueOf(searchDoc.select("zs|numberOfRecords")
                                               .text());

        Elements records = searchDoc.select("zs|records > zs|record");
        int i = 0;
        for (Element record : records) {
            SearchResult sr = new SearchResult();
            String title = getDetail(record, "titleInfo title");
            String firstName = getDetail(record, "name > namePart[type=given]");
            String lastName = getDetail(record, "name > namePart[type=family]");
            String year = getDetail(record, "dateIssued");
            String mType = getDetail(record, "physicalDescription > form");
            String isbn = getDetail(record, "identifier[type=isbn]");
            String coverUrl = getDetail(record, "url[displayLabel=C Cover]");
            String additionalInfo = firstName + " " + lastName + ", " + year;
            sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);
            sr.setType(defaulttypes.get(mType));
            sr.setNr(i);
            sr.setId(getDetail(record, "recordIdentifier"));
            if (coverUrl.equals("")) {
                sr.setCover(ISBNTools.getAmazonCoverURL(isbn, false));
            } else {
                sr.setCover(coverUrl);
            }
            results.add(sr);
            i++;
        }

        return new SearchRequestResult(results, resultcount, 1);
    }

    private String getDetail(Element record, String selector) {
        if (record.select(selector).size() > 0) {
            return record.select(selector).first().text();
        } else {
            return "";
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        String xml = httpGet(opac_url
                + "?version=1.1&operation=searchRetrieve&maximumRecords="
                + resultcount
                + "&recordSchema=mods&sortKeys=relevance,,1&startRecord="
                + String.valueOf(page * resultcount + 1) + "&query="
                + currentSearchParams, getDefaultEncoding());
        return parse_result(xml);
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        if (idSearchQuery != null) {
            String xml = httpGet(opac_url
                    + "?version=1.1&operation=searchRetrieve&maximumRecords="
                    + resultcount
                    + "&recordSchema=mods&sortKeys=relevance,,1&query="
                    + idSearchQuery + "%3D" + id, getDefaultEncoding());
            searchDoc = Jsoup.parse(xml, "", Parser.xmlParser());
            if (searchDoc.select("diag|diagnostic").size() > 0) {
                throw new OpacErrorException(searchDoc.select("diag|message")
                                                      .text());
            }
            if (searchDoc.select("zs|record").size() != 1) { // should not
                // happen
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }
            return parse_detail(searchDoc.select("zs|record").first());
        } else {
            return null;
        }
    }

    private DetailedItem parse_detail(Element record) {
        String title = getDetail(record, "titleInfo title");
        String firstName = getDetail(record, "name > namePart[type=given]");
        String lastName = getDetail(record, "name > namePart[type=family]");
        String year = getDetail(record, "dateIssued");
        String desc = getDetail(record, "abstract");
        String isbn = getDetail(record, "identifier[type=isbn]");
        String coverUrl = getDetail(record, "url[displayLabel=C Cover]");

        DetailedItem item = new DetailedItem();
        item.setTitle(title);
        item.addDetail(new Detail("Autor", firstName + " " + lastName));
        item.addDetail(new Detail("Jahr", year));
        item.addDetail(new Detail("Beschreibung", desc));
        if (coverUrl.equals("") && isbn.length() > 0) {
            item.setCover(ISBNTools.getAmazonCoverURL(isbn, true));
        } else if (!coverUrl.equals("")) {
            item.setCover(coverUrl);
        }

        if (isbn.length() > 0) {
            item.addDetail(new Detail("ISBN", isbn));
        }

        return item;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException,
            OpacErrorException {
        return parse_detail(searchDoc.select("zs|records > zs|record").get(
                position));
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        return null;
    }

    @Override
    public List<SearchField> parseSearchFields() throws OpacErrorException,
            NotReachableException {
        List<SearchField> searchFields = new ArrayList<>();
        Set<String> fieldsCompat = searchQueries.keySet();

        if (fieldsCompat.contains(KEY_SEARCH_QUERY_FREE)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_FREE, "",
                    false, false, "Freie Suche", true, false);
            field.setMeaning(Meaning.FREE);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_TITLE)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_TITLE,
                    "Titel", false, false, "Stichwort", false, false);
            field.setMeaning(Meaning.TITLE);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUTHOR)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_AUTHOR,
                    "Verfasser", false, false, "Nachname, Vorname", false,
                    false);
            field.setMeaning(Meaning.AUTHOR);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_PUBLISHER)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_PUBLISHER,
                    "Verlag", false, false, "", false, false);
            field.setMeaning(Meaning.PUBLISHER);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_DIGITAL)) {
            SearchField field = new CheckboxSearchField(
                    KEY_SEARCH_QUERY_DIGITAL, "nur digitale Medien", false);
            field.setMeaning(Meaning.DIGITAL);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_AVAILABLE)) {
            SearchField field = new CheckboxSearchField(
                    KEY_SEARCH_QUERY_AVAILABLE, "nur verfügbare Medien", false);
            field.setMeaning(Meaning.AVAILABLE);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_ISBN)) {
            SearchField field = new BarcodeSearchField(KEY_SEARCH_QUERY_ISBN,
                    "Strichcode", false, false, "ISBN");
            field.setMeaning(Meaning.ISBN);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_BARCODE)) {
            SearchField field = new BarcodeSearchField(
                    KEY_SEARCH_QUERY_BARCODE, "Strichcode", false, true,
                    "Buchungsnr.");
            field.setMeaning(Meaning.BARCODE);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_YEAR,
                    "Jahr", false, false, "", false, true);
            field.setMeaning(Meaning.YEAR);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
            SearchField field = new TextSearchField(
                    KEY_SEARCH_QUERY_YEAR_RANGE_START, "Jahr", false, false,
                    "von", false, true);
            field.setMeaning(Meaning.YEAR);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
            SearchField field = new TextSearchField(
                    KEY_SEARCH_QUERY_YEAR_RANGE_END, "Jahr", false, true,
                    "bis", false, true);
            field.setMeaning(Meaning.YEAR);
            searchFields.add(field);
        }

        if (fieldsCompat.contains(KEY_SEARCH_QUERY_PUBLISHER)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_PUBLISHER,
                    "Verlag", false, false, "", false, false);
            field.setMeaning(Meaning.PUBLISHER);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDA)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_KEYWORDA,
                    "Schlagwort", true, false, "", false, false);
            field.setMeaning(Meaning.KEYWORD);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDB)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_KEYWORDB,
                    "Schlagwort", true, true, "", false, false);
            field.setMeaning(Meaning.KEYWORD);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_SYSTEM)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_SYSTEM,
                    "Systematik", true, false, "", false, false);
            field.setMeaning(Meaning.SYSTEM);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUDIENCE)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_AUDIENCE,
                    "Interessenkreis", true, false, "", false, false);
            field.setMeaning(Meaning.AUDIENCE);
            searchFields.add(field);
        }
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_LOCATION)) {
            SearchField field = new TextSearchField(KEY_SEARCH_QUERY_LOCATION,
                    "Ort", false, false, "", false, false);
            field.setMeaning(Meaning.LOCATION);
            searchFields.add(field);
        }
        //noinspection StatementWithEmptyBody
        if (fieldsCompat.contains(KEY_SEARCH_QUERY_ORDER)) {
            // TODO: Implement this (was this even usable before?)
        }

        return searchFields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        if (shareUrl != null) {
            return String.format(shareUrl, id);
        } else {
            return null;
        }
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        // TODO Auto-generated method stub

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
