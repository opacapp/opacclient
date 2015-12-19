package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import de.geeksfactory.opacclient.objects.SearchResult.Status;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.Base64;

//@formatter:off

/**
 * @author Johan von Forstner, 11.08.2014
 *         <p/>
 *         WinBIAP, Version 4.1.0 gestartet mit Bibliothek Unterföhring
 *         <p/>
 *         Unterstützt bisher nur Katalogsuche
 *         <p/>
 *         Example for a search query (parameter "data" in the URL, everything before the hyphen,
 *         base64 decoded, added formatting) as seen in Unterföhring:
 *         <p/>
 *         cmd=5&amp;				perform a search sC= c_0=1%%				unknown m_0=1%%
 *         unknown f_0=2%%				free
 *         search o_0=8%%				contains v_0=schule			"schule" ++ c_1=1%% unknown
 *         m_1=1%%				unknown
 *         f_1=3%%				author o_1=8%%				contains v_1=rowling "rowling" ++
 *         c_2=1%%				unknown
 *         m_2=1%%				unknown f_2=12%%			title o_2=8%%				contains
 *         v_2=potter			"potter" ++
 *         c_3=1%%				unknown m_3=1%%				unknown f_3=34%%			year
 *         o_3=6%%				newer or equal to
 *         v_3=2000			"2000" ++ c_4=1%%				unknown m_4=1%%				unknown
 *         f_4=34%%			year
 *         o_4=4%%				older or equal to v_4=2014			"2014" ++ c_5=1%% unknown
 *         m_5=1%%				unknown
 *         f_5=42%%			media category o_5=1%%				is equal to v_5=3 "Kinder- und
 *         Jugendbücher" ++
 *         c_6=1%%				unknown m_6=1%%				unknown f_6=48%%			location
 *         o_6=1%%				is equal to
 *         v_6=1				"Bibliothek Unterföhring" ++ c_7=3%%				unknown (now
 *         changed to 3)	-
 *         m_7=1%%				unknown						|	  This group has no f_7=45%%
 *         unknown						|---  effect on the
 *         result o_7=1%%				unknown						|	  and can be left out
 *         v_7=5|4|101|102		unknown						-
 *         <p/>
 *         &amp;Sort=Autor				Sort by Author (default)
 */
//@formatter:on

public class WinBiap extends BaseApi implements OpacApi {

    protected static final String QUERY_TYPE_CONTAINS = "8";
    protected static final String QUERY_TYPE_FROM = "6";
    protected static final String QUERY_TYPE_TO = "4";
    protected static final String QUERY_TYPE_STARTS_WITH = "7";
    protected static final String QUERY_TYPE_EQUALS = "1";
    protected static HashMap<String, SearchResult.MediaType> defaulttypes = new HashMap<>();

    static {
        defaulttypes.put("sb_buch", SearchResult.MediaType.BOOK);
        defaulttypes.put("sl_buch", SearchResult.MediaType.BOOK);
        defaulttypes.put("kj_buch", SearchResult.MediaType.BOOK);
        defaulttypes.put("hoerbuch", SearchResult.MediaType.AUDIOBOOK);
        defaulttypes.put("musik", SearchResult.MediaType.CD_MUSIC);
        defaulttypes.put("cdrom", SearchResult.MediaType.CD_SOFTWARE);
        defaulttypes.put("dvd", SearchResult.MediaType.DVD);
        defaulttypes.put("online", SearchResult.MediaType.EBOOK);
    }

    protected String opac_url = "";
    protected JSONObject data;
    protected List<List<NameValuePair>> query;

    public void init(Library lib) {
        super.init(lib);
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For documentation of the parameters, @see {@link #addParametersManual(String, String, String,
     * String, String, List, int)}
     */
    protected int addParameters(Map<String, String> query, String key,
            String searchkey, String type, List<List<NameValuePair>> params,
            int index) {
        if (!query.containsKey(key) || query.get(key).equals("")) {
            return index;
        }

        return addParametersManual("1", "1", searchkey, type, query.get(key), params, index);
    }

    /**
     * @param combination "Combination" (probably And, Or, ...): Meaning unknown, seems to always be
     *                    "1" except in some mysterious queries the website adds every time that
     *                    don't change the result
     * @param mode        "Mode": Meaning unknown, seems to always be "1" except in some mysterious
     *                    queries the website adds every time that don't change the result
     * @param field       "Field": The key for the property that is queried, for example "12" for
     *                    "title"
     * @param operator    "Operator": The type of search that is made (one of the QUERY_TYPE_
     *                    constants above), for example "8" for "contains"
     * @param value       "Value": The value that was input by the user
     */
    @SuppressWarnings("SameParameterValue")
    protected int addParametersManual(String combination, String mode,
            String field, String operator, String value,
            List<List<NameValuePair>> params, int index) {
        List<NameValuePair> list = new ArrayList<>();
        if (data.optBoolean("longParameterNames")) {
            // A few libraries use longer names for the parameters (e.g. Hohen Neuendorf)
            list.add(new BasicNameValuePair("Combination_" + index, combination));
            list.add(new BasicNameValuePair("Mode_" + index, mode));
            list.add(new BasicNameValuePair("Searchfield_" + index, field));
            list.add(new BasicNameValuePair("Searchoperator_" + index, operator));
            list.add(new BasicNameValuePair("Searchvalue_" + index, value));
        } else {
            list.add(new BasicNameValuePair("c_" + index, combination));
            list.add(new BasicNameValuePair("m_" + index, mode));
            list.add(new BasicNameValuePair("f_" + index, field));
            list.add(new BasicNameValuePair("o_" + index, operator));
            list.add(new BasicNameValuePair("v_" + index, value));
        }
        params.add(list);
        return index + 1;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> queries)
            throws IOException, OpacErrorException {
        Map<String, String> query = searchQueryListToMap(queries);

        List<List<NameValuePair>> queryParams = new ArrayList<>();

        int index = 0;
        index = addParameters(query, KEY_SEARCH_QUERY_FREE,
                data.optString("KEY_SEARCH_QUERY_FREE", "2"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR,
                data.optString("KEY_SEARCH_QUERY_AUTHOR", "3"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_TITLE,
                data.optString("KEY_SEARCH_QUERY_TITLE", "12"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_KEYWORDA,
                data.optString("KEY_SEARCH_QUERY_KEYWORDA", "24"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_AUDIENCE,
                data.optString("KEY_SEARCH_QUERY_AUDIENCE", "25"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_SYSTEM,
                data.optString("KEY_SEARCH_QUERY_SYSTEM", "26"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_ISBN,
                data.optString("KEY_SEARCH_QUERY_ISBN", "29"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER,
                data.optString("KEY_SEARCH_QUERY_PUBLISHER", "32"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_BARCODE,
                data.optString("KEY_SEARCH_QUERY_BARCODE", "46"),
                QUERY_TYPE_CONTAINS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_START,
                data.optString("KEY_SEARCH_QUERY_BARCODE", "34"),
                QUERY_TYPE_FROM, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_YEAR_RANGE_END,
                data.optString("KEY_SEARCH_QUERY_BARCODE", "34"),
                QUERY_TYPE_TO, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_CATEGORY,
                data.optString("KEY_SEARCH_QUERY_CATEGORY", "42"),
                QUERY_TYPE_EQUALS, queryParams, index);
        index = addParameters(query, KEY_SEARCH_QUERY_BRANCH,
                data.optString("KEY_SEARCH_QUERY_BRANCH", "48"),
                QUERY_TYPE_EQUALS, queryParams, index);

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        // if (index > 4) {
        // throw new OpacErrorException(
        // "Diese Bibliothek unterstützt nur bis zu vier benutzte Suchkriterien.");
        // }

        this.query = queryParams;
        String encodedQueryParams = encode(queryParams, "=", "%%", "++");

        List<NameValuePair> params = new ArrayList<>();
        start();
        params.add(new BasicNameValuePair("cmd", "5"));
        if (data.optBoolean("longParameterNames"))
        // A few libraries use longer names for the parameters
        // (e.g. Hohen Neuendorf)
        {
            params.add(new BasicNameValuePair("searchConditions",
                    encodedQueryParams));
        } else {
            params.add(new BasicNameValuePair("sC", encodedQueryParams));
        }
        params.add(new BasicNameValuePair("Sort", "Autor"));

        String text = encode(params, "=", "&amp;");
        String base64 = URLEncoder.encode(
                Base64.encodeBytes(text.getBytes("UTF-8")), "UTF-8");

        String html = httpGet(opac_url + "/search.aspx?data=" + base64,
                getDefaultEncoding(), false);
        return parse_search(html, 1);
    }

    private SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException, IOException {
        Document doc = Jsoup.parse(html);

        if (doc.select(".alert h4").text().contains("Keine Treffer gefunden")) {
            // no media found
            return new SearchRequestResult(new ArrayList<SearchResult>(), 0,
                    page);
        }
        if (doc.select("errortype").size() > 0) {
            // Error (e.g. 404)
            throw new OpacErrorException(doc.select("errortype").text());
        }

        // Total count
        String header = doc.select(".ResultHeader").text();
        Pattern pattern = Pattern.compile("Die Suche ergab (\\d*) Treffer");
        Matcher matcher = pattern.matcher(header);
        int results_total;
        if (matcher.find()) {
            results_total = Integer.parseInt(matcher.group(1));
        } else {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.INTERNAL_ERROR));
        }

        // Results
        Elements trs = doc.select("#listview .ResultItem");
        List<SearchResult> results = new ArrayList<>();
        for (Element tr : trs) {
            SearchResult sr = new SearchResult();
            String author = tr.select(".autor").text();
            String title = tr.select(".title").text();
            String titleAddition = tr.select(".titleZusatz").text();
            String desc = tr.select(".smallDescription").text();
            sr.setInnerhtml("<b>"
                    + (author.equals("") ? "" : author + "<br />")
                    + title
                    + (titleAddition.equals("") ? "" : " - <i>" + titleAddition
                    + "</i>") + "</b><br /><small>" + desc + "</small>");

            if (tr.select(".coverWrapper input, .coverWrapper img").size() > 0) {
                Element cover = tr.select(".coverWrapper input, .coverWrapper img").first();
                if (cover.hasAttr("data-src")) {
                    sr.setCover(cover.attr("data-src"));
                } else if (cover.hasAttr("src") && !cover.attr("src").contains("empty.gif")
                        && !cover.attr("src").contains("leer.gif")) {
                    sr.setCover(cover.attr("src"));
                }
                sr.setType(getMediaType(cover, data));
            }

            String link = tr.select("a[href*=detail.aspx]").attr("href");
            String base64 = getQueryParamsFirst(link).get("data");
            if (base64.contains("-")) // Most of the time, the base64 string is
            // followed by a hyphen and some
            // mysterious
            // letters that we don't want
            {
                base64 = base64.substring(0, base64.indexOf("-") - 1);
            }
            String decoded = new String(Base64.decode(base64), "UTF-8");
            pattern = Pattern.compile("CatalogueId=(\\d*)");
            matcher = pattern.matcher(decoded);
            if (matcher.find()) {
                sr.setId(matcher.group(1));
            } else {
                throw new OpacErrorException(
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }

            if (tr.select(".mediaStatus").size() > 0) {
                Element status = tr.select(".mediaStatus").first();
                if (status.hasClass("StatusNotAvailable")) {
                    sr.setStatus(Status.RED);
                } else if (status.hasClass("StatusAvailable")) {
                    sr.setStatus(Status.GREEN);
                } else {
                    sr.setStatus(Status.YELLOW);
                }
            } else if (tr.select(".showCopies").size() > 0) { // Multiple copies
                if (tr.nextElementSibling().select(".StatusNotAvailable")
                      .size() == 0) {
                    sr.setStatus(Status.GREEN);
                } else if (tr.nextElementSibling().select(".StatusAvailable")
                             .size() == 0) {
                    sr.setStatus(Status.RED);
                } else {
                    sr.setStatus(Status.YELLOW);
                }
            }

            results.add(sr);
        }
        return new SearchRequestResult(results, results_total, page);
    }

    private String encode(List<List<NameValuePair>> list, String equals,
            String separator, String separator2) {
        if (list.size() > 0) {
            String encoded = encode(list.get(0), equals, separator);
            for (int i = 1; i < list.size(); i++) {
                encoded += separator2;
                encoded += encode(list.get(i), equals, separator);
            }
            return encoded;
        } else {
            return "";
        }
    }

    private String encode(List<NameValuePair> list, String equals,
            String separator) {
        if (list.size() > 0) {
            String encoded = list.get(0).getName() + equals
                    + list.get(0).getValue();
            for (int i = 1; i < list.size(); i++) {
                encoded += separator;
                encoded += list.get(i).getName() + equals
                        + list.get(i).getValue();
            }
            return encoded;
        } else {
            return "";
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        String encodedQueryParams = encode(query, "=", "%%", "++");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cmd", "1"));
        if (data.optBoolean("longParameterNames")) {
            // A few libraries use longer names for the parameters
            // (e.g. Hohen Neuendorf)
            params.add(new BasicNameValuePair("searchConditions",
                    encodedQueryParams));
            params.add(new BasicNameValuePair("PageIndex", String
                    .valueOf(page - 1)));
        } else {
            params.add(new BasicNameValuePair("sC", encodedQueryParams));
            params.add(new BasicNameValuePair("pI", String.valueOf(page - 1)));
        }
        params.add(new BasicNameValuePair("Sort", "Autor"));

        String text = encode(params, "=", "&amp;");
        String base64 = URLEncoder.encode(
                Base64.encodeBytes(text.getBytes("UTF-8")), "UTF-8");

        String html = httpGet(opac_url + "/search.aspx?data=" + base64,
                getDefaultEncoding(), false);
        return parse_search(html, page);
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        String html = httpGet(opac_url + "/detail.aspx?Id=" + id,
                getDefaultEncoding(), false);
        return parse_result(html);
    }

    private DetailledItem parse_result(String html) {
        Document doc = Jsoup.parse(html);
        DetailledItem item = new DetailledItem();

        if (doc.select(".cover").size() > 0) {
            Element cover = doc.select(".cover").first();
            if (cover.hasAttr("data-src")) {
                item.setCover(cover.attr("data-src"));
            } else if (cover.hasAttr("src") && !cover.attr("src").equals("images/empty.gif")) {
                item.setCover(cover.attr("src"));
            }
            item.setMediaType(getMediaType(cover, data));
        }

        String permalink = doc.select(".PermalinkTextarea").text();
        item.setId(getQueryParamsFirst(permalink).get("Id"));

        Elements trs = doc.select(".DetailInformation").first().select("tr");
        for (Element tr : trs) {
            String name = tr.select(".DetailInformationEntryName").text()
                            .replace(":", "");
            String value = tr.select(".DetailInformationEntryContent").text();
            switch (name) {
                case "Titel":
                    item.setTitle(value);
                    break;
                case "Stücktitel":
                    item.setTitle(item.getTitle() + " " + value);
                    break;
                default:
                    item.addDetail(new Detail(name, value));
                    break;
            }
        }

        trs = doc.select(".detailCopies .tableCopies > tbody > tr:not(.headerCopies)");
        for (Element tr : trs) {
            Map<String, String> copy = new HashMap<>();
            copy.put(DetailledItem.KEY_COPY_BARCODE, tr.select(".mediaBarcode")
                                                       .text().replace("#", ""));
            copy.put(DetailledItem.KEY_COPY_STATUS, tr.select(".mediaStatus")
                                                      .text());
            if (tr.select(".mediaBranch").size() > 0) {
                copy.put(DetailledItem.KEY_COPY_BRANCH,
                        tr.select(".mediaBranch").text());
            }
            copy.put(DetailledItem.KEY_COPY_LOCATION,
                    tr.select(".cellMediaItemLocation span").text());
            if (tr.select("#HyperLinkReservation").size() > 0) {
                copy.put(DetailledItem.KEY_COPY_RESINFO, tr.select("#HyperLinkReservation")
                                                           .attr("href"));
                item.setReservable(true);
                item.setReservation_info("reservable");

            }
            item.addCopy(copy);
        }

        return item;
    }

    private static SearchResult.MediaType getMediaType(Element cover, JSONObject data) {
        if (cover.hasAttr("grp")) {
            String[] parts = cover.attr("grp").split("/");
            String fname = parts[parts.length - 1];
            if (data.has("mediatypes")) {
                try {
                    return SearchResult.MediaType.valueOf(data.getJSONObject(
                            "mediatypes").getString(fname));
                } catch (JSONException | IllegalArgumentException e) {
                    return defaulttypes.get(fname
                            .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                            .replace(".gif", "").replace(".png", ""));
                }
            } else {
                return defaulttypes.get(fname
                        .toLowerCase(Locale.GERMAN).replace(".jpg", "")
                        .replace(".gif", "").replace(".png", ""));
            }
        }
        return null;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException,
            OpacErrorException {
        // Should not be called because every media has an ID
        return null;
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account account,
            int useraction, String selection) throws IOException {
        if (selection == null) {
            // Which copy?
            List<Map<String, String>> options = new ArrayList<>();
            for (Map<String, String> copy : item.getCopies()) {
                if (!copy.containsKey(DetailledItem.KEY_COPY_RESINFO)) continue;

                Map<String, String> option = new HashMap<>();
                option.put("key", copy.get(DetailledItem.KEY_COPY_RESINFO));
                option.put("value", copy.get(DetailledItem.KEY_COPY_BARCODE) + " - "
                        + copy.get(DetailledItem.KEY_COPY_BRANCH) + " - "
                        + copy.get(DetailledItem.KEY_COPY_RETURN));
                options.add(option);
            }
            if (options.size() == 0) {
                return new ReservationResult(MultiStepResult.Status.ERROR,
                        stringProvider.getString(StringProvider.NO_COPY_RESERVABLE));
            } else if (options.size() == 1) {
                return reservation(item, account, useraction, options.get(0).get("key"));
            } else {
                ReservationResult res = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                res.setSelection(options);
                return res;
            }
        } else {
            // Reservation

            // the URL stored in selection contains "=" and other things inside params
            // and will be messed up by our cleanUrl function
            Document doc = Jsoup.parse(convertStreamToString(
                    http_client.execute(new HttpGet(opac_url + "/" + selection))
                               .getEntity().getContent()));
            if (doc.select("[id$=LabelLoginMessage]").size() > 0) {
                doc.select("[id$=TextBoxLoginName]").val(account.getName());
                doc.select("[id$=TextBoxLoginPassword]").val(account.getPassword());
                FormElement form = (FormElement) doc.select("form").first();

                List<Connection.KeyVal> formData = form.formData();
                List<NameValuePair> params = new ArrayList<>();
                for (Connection.KeyVal kv : formData) {
                    if (!kv.key().contains("Button") || kv.key().endsWith("ButtonLogin")) {
                        params.add(new BasicNameValuePair(kv.key(), kv.value()));
                    }
                }
                doc = Jsoup.parse(httpPost(opac_url + "/user/" + form.attr("action"),
                        new UrlEncodedFormEntity(params), getDefaultEncoding()));
            }
            FormElement confirmationForm = (FormElement) doc.select("form").first();
            List<Connection.KeyVal> formData = confirmationForm.formData();
            List<NameValuePair> params = new ArrayList<>();
            for (Connection.KeyVal kv : formData) {
                if (!kv.key().contains("Button") || kv.key().endsWith("ButtonVorbestOk")) {
                    params.add(new BasicNameValuePair(kv.key(), kv.value()));
                }
            }
            httpPost(opac_url + "/user/" + confirmationForm.attr("action"),
                    new UrlEncodedFormEntity(params), getDefaultEncoding());

            // TODO: handle errors (I did not encounter any)

            return new ReservationResult(MultiStepResult.Status.OK);
        }
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        try {
            login(account);
        } catch (OpacErrorException e) {
            return new ProlongResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
        Document lentPage = Jsoup.parse(
                httpGet(opac_url + "/user/borrow.aspx", getDefaultEncoding()));
        lentPage.select("input[name=" + media + "]").first().attr("checked", true);
        List<Connection.KeyVal> formData =
                ((FormElement) lentPage.select("form").first()).formData();
        List<NameValuePair> params = new ArrayList<>();
        for (Connection.KeyVal kv : formData) {
            params.add(new BasicNameValuePair(kv.key(), kv.value()));
        }

        String html = httpPost(opac_url + "/user/borrow.aspx", new UrlEncodedFormEntity
                (params), getDefaultEncoding());
        Document confirmationPage = Jsoup.parse(html);

        FormElement confirmationForm = (FormElement) confirmationPage.select("form").first();
        List<Connection.KeyVal> formData2 = confirmationForm.formData();
        List<NameValuePair> params2 = new ArrayList<>();
        for (Connection.KeyVal kv : formData2) {
            if (!kv.key().contains("Button") || kv.key().endsWith("ButtonProlongationOk")) {
                params2.add(new BasicNameValuePair(kv.key(), kv.value()));
            }
        }
        httpPost(opac_url + "/user/" + confirmationForm.attr("action"),
                new UrlEncodedFormEntity(params2), getDefaultEncoding());

        // TODO: handle errors (I did not encounter any)

        return new ProlongResult(MultiStepResult.Status.OK);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        try {
            login(account);
        } catch (OpacErrorException e) {
            return new CancelResult(MultiStepResult.Status.ERROR, e.getMessage());
        }
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("action", "reservationdelete"));
        params.add(new BasicNameValuePair("data", media));
        String response = httpPost(opac_url + "/service/UserService.ashx",
                new UrlEncodedFormEntity(params), getDefaultEncoding());
        System.out.println("Antwort: " + response);
        // Response: [number of reservations deleted];[number of remaining reservations]
        String[] parts = response.split(";");
        if (parts[0].equals("1")) {
            return new CancelResult(MultiStepResult.Status.OK);
        } else {
            return new CancelResult(MultiStepResult.Status.ERROR,
                    stringProvider.getString(StringProvider.UNKNOWN_ERROR));
        }
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        Document startPage = login(account);
        AccountData adata = new AccountData(account.getId());

        if (startPage.select("#ctl00_ContentPlaceHolderMain_LabelCharges").size() > 0) {
            String fees = startPage.select("#ctl00_ContentPlaceHolderMain_LabelCharges").text()
                                   .replace("Kontostand:", "").trim();
            if (!fees.equals("ausgeglichen")) adata.setPendingFees(fees);
        }

        Document lentPage = Jsoup.parse(
                httpGet(opac_url + "/user/borrow.aspx", getDefaultEncoding()));
        adata.setLent(parseMediaList(lentPage));

        Document reservationsPage = Jsoup.parse(
                httpGet(opac_url + "/user/reservations.aspx", getDefaultEncoding()));
        adata.setReservations(parseResList(reservationsPage, stringProvider, data));


        return adata;
    }

    static List<Map<String, String>> parseMediaList(Document doc) {
        List<Map<String, String>> lent = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

        // the account page differs between WinBiap versions 4.2 and 4.3
        boolean winBiap43;
        if (doc.select(".GridView_RowStyle").size() > 0) {
            winBiap43 = false;
        } else {
            winBiap43 = true;
        }

        for (Element tr : doc.select(winBiap43 ? ".detailTable tr[id$=DetailItemMain_rowBorrow]" :
                ".GridView_RowStyle")) {
            Map<String, String> item = new HashMap<>();
            Element detailsTr = winBiap43 ? tr.nextElementSibling() : tr;

            // the second column contains an img tag with the cover
            if (tr.select(".cover").size() > 0) {
                // find media ID using cover URL
                Map<String, String> params = getQueryParamsFirst(tr.select(".cover").attr("src"));
                if (params.containsKey("catid")) {
                    item.put(AccountData.KEY_LENT_ID, params.get("catid"));
                }
            }

            putIfNotEmpty(item, AccountData.KEY_LENT_AUTHOR,
                    tr.select("[id$=LabelAutor]").text());
            putIfNotEmpty(item, AccountData.KEY_LENT_TITLE,
                    tr.select("[id$=LabelTitel], [id$=LabelTitle]").text());
            putIfNotEmpty(item, AccountData.KEY_LENT_BARCODE,
                    detailsTr.select("[id$=Label_Mediennr], [id$=labelMediennr]").text());
            putIfNotEmpty(item, AccountData.KEY_LENT_FORMAT,
                    detailsTr.select("[id$=Label_Mediengruppe], [id$=labelMediagroup]").text());
            putIfNotEmpty(item, AccountData.KEY_LENT_BRANCH,
                    detailsTr.select("[id$=Label_Zweigstelle], [id$=labelBranch]").text());
            // Label_Entliehen contains the date when the medium was lent
            putIfNotEmpty(item, AccountData.KEY_LENT_DEADLINE,
                    tr.select("[id$=LabelFaellig], [id$=LabelMatureDate]").text());
            try {
                item.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String.valueOf(
                        sdf.parse(tr.select("[id$=LabelFaellig], [id$=LabelMatureDate]").text())
                           .getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (tr.select("input[id$=chkSelect]").size() > 0) {
                item.put(AccountData.KEY_LENT_LINK, tr.select("input[id$=chkSelect]").attr("name"));
            } else {
                item.put(AccountData.KEY_LENT_RENEWABLE, "N");
            }
            lent.add(item);
        }

        return lent;
    }

    private static void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.equals("")) {
            map.put(key, value);
        }
    }

    static List<Map<String, String>> parseResList(Document doc, StringProvider stringProvider,
            JSONObject data) {
        List<Map<String, String>> reservations = new ArrayList<>();

        for (Element tr : doc.select("tr[id*=GridViewReservation]")) {
            Map<String, String> item = new HashMap<>();

            // the second column contains an img tag with the cover
            if (tr.select(".cover").size() > 0) {
                // find media ID using cover URL
                Map<String, String> params = getQueryParamsFirst(tr.select(".cover").attr("src"));
                if (params.containsKey("catid")) {
                    item.put(AccountData.KEY_RESERVATION_ID, params.get("catid"));
                }
                // find media type
                SearchResult.MediaType mt = getMediaType(tr.select(".cover").first(), data);
                if (mt != null) {
                    item.put(AccountData.KEY_RESERVATION_FORMAT,
                            stringProvider.getMediaTypeName(mt));
                }
            }

            item.put(AccountData.KEY_RESERVATION_READY,
                    tr.select("[id$=ImageBorrow]").attr("title"));
            putIfNotEmpty(item, AccountData.KEY_RESERVATION_AUTHOR,
                    tr.select("[id$=LabelAutor]").text());
            putIfNotEmpty(item, AccountData.KEY_RESERVATION_TITLE,
                    tr.select("[id$=LabelTitle]").text());
            putIfNotEmpty(item, AccountData.KEY_RESERVATION_BRANCH,
                    tr.select("[id$=LabelBranch]").text());
            // Label_Vorbestelltam contains the date when the medium was reserved

            if (tr.select("a[id$=ImageReservationDelete]").size() > 0) {
                String javascript = tr.select("a[id$=ImageReservationDelete]").attr("onclick");
                /*
                    Javascript example:

                    javascript:DeleteReservation(
                    '#ctl00_ContentPlaceHolderMain_GridViewReservation_ctl02',
                    '#ctl00_ContentPlaceHolderMain_GridViewReservation_ctl02_ImageReservationDelete',
                    'cmVzZXJ2YXRpb25JZD00MDk1JmFtcDtyZWFkZXJJZD05MzIwJmFtcDttb2RlPTE=-f2yu2300+t4=',
                    '../service/UserService.ashx',
                    'Vorbestellung: \'Beck, Rufus - Harry Potter Folge 4. Harry Potter und der
                    Feuerkelch\' wirklich löschen?',
                    '#ctl00_ContentPlaceHolderMain_LabelAccountTableResult',
                    'Sie haben derzeit $ Medien vorbestellt!');
                    return false;

                    We need the 3rd parameter (Base64 string) and will find it
                    using the following massive RegEx.
                 */
                Pattern regex = Pattern.compile("javascript:DeleteReservation\\('" +
                        "(?:\\\\[\\\\']|[^\\\\'])*'\\s*,\\s*'(?:\\\\[\\\\']|[^\\\\'])*'\\s*,\\s*'" +
                        "((?:\\\\[\\\\']|[^\\\\'])*)'\\s*,\\s*'(?:\\\\[\\\\']|[^\\\\'])*'\\s*," +
                        "\\s*'(?:\\\\[\\\\']|[^\\\\'])*'\\s*,\\s*'(?:\\\\[\\\\']|[^\\\\'])*'\\s*," +
                        "\\s*'(?:\\\\[\\\\']|[^\\\\'])*'\\s*\\);");
                Matcher matcher = regex.matcher(javascript);
                if (matcher.find()) {
                    String base64 = matcher.group(1);
                    item.put(AccountData.KEY_RESERVATION_CANCEL, base64);
                }
            }
            reservations.add(item);
        }

        return reservations;
    }

    @Override
    public List<SearchField> getSearchFields() throws IOException {
        // extract branches and categories
        String html = httpGet(opac_url + "/search.aspx", getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        Elements mediaGroupOptions = doc
                .select("#ctl00_ContentPlaceHolderMain_searchPanel_ListBoxMediagroups_ListBoxMultiselection option");
        Elements branchOptions = doc
                .select("#ctl00_ContentPlaceHolderMain_searchPanel_MultiSelectionBranch_ListBoxMultiselection option");

        final DropdownSearchField mediaGroups =
                new DropdownSearchField(KEY_SEARCH_QUERY_CATEGORY, "Mediengruppe", false, null);
        mediaGroups.setMeaning(Meaning.CATEGORY);

        final DropdownSearchField branches =
                new DropdownSearchField(KEY_SEARCH_QUERY_BRANCH, "Zweigstelle", false, null);
        branches.setMeaning(Meaning.BRANCH);

        mediaGroups.addDropdownValue("", "Alle");
        branches.addDropdownValue("", "Alle");

        for (Element option : mediaGroupOptions) {
            mediaGroups.addDropdownValue(option.attr("value"), option.text());
        }
        for (Element option : branchOptions) {
            branches.addDropdownValue(option.attr("value"), option.text());
        }

        List<SearchField> searchFields = new ArrayList<>();

        SearchField field = new TextSearchField(KEY_SEARCH_QUERY_FREE, "",
                false, false, "Beliebig", true, false);
        field.setMeaning(Meaning.FREE);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_AUTHOR, "Autor", false,
                false, "Nachname, Vorname", false, false);
        field.setMeaning(Meaning.AUTHOR);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_TITLE, "Titel", false,
                false, "Stichwort", false, false);
        field.setMeaning(Meaning.TITLE);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_KEYWORDA, "Schlagwort",
                true, false, "", false, false);
        field.setMeaning(Meaning.KEYWORD);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_AUDIENCE,
                "Interessenkreis", true, false, "", false, false);
        field.setMeaning(Meaning.AUDIENCE);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_SYSTEM, "Systematik",
                true, false, "", false, false);
        field.setMeaning(Meaning.SYSTEM);
        searchFields.add(field);

        field = new BarcodeSearchField(KEY_SEARCH_QUERY_ISBN, "Strichcode",
                false, false, "ISBN");
        field.setMeaning(Meaning.ISBN);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_PUBLISHER, "Verlag",
                false, false, "", false, false);
        field.setMeaning(Meaning.PUBLISHER);
        searchFields.add(field);

        field = new BarcodeSearchField(KEY_SEARCH_QUERY_BARCODE, "Strichcode",
                false, true, "Mediennummer");
        field.setMeaning(Meaning.BARCODE);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_YEAR_RANGE_START, "Jahr",
                false, false, "von", false, true);
        field.setMeaning(Meaning.YEAR);
        searchFields.add(field);

        field = new TextSearchField(KEY_SEARCH_QUERY_YEAR_RANGE_END, "Jahr",
                false, true, "bis", false, true);
        field.setMeaning(Meaning.YEAR);
        searchFields.add(field);

        searchFields.add(branches);
        searchFields.add(mediaGroups);

        return searchFields;
    }

    @Override
    public boolean shouldUseMeaningDetector() {
        return false;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return library.isAccountSupported();
    }

    @Override
    public boolean isAccountExtendable() {
        return false;
    }

    @Override
    public String getAccountExtendableInfo(Account account) throws IOException {
        return null;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/detail.aspx?Id=" + id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING;
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        login(account);
    }

    protected Document login(Account account) throws IOException, OpacErrorException {
        Document loginPage = Jsoup.parse(
                httpGet(opac_url + "/user/login.aspx", getDefaultEncoding()));
        List<NameValuePair> data = new ArrayList<>();

        data.add(new BasicNameValuePair("__LASTFOCUS", ""));
        data.add(new BasicNameValuePair("__EVENTTARGET", ""));
        data.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
        data.add(new BasicNameValuePair("__VIEWSTATE", loginPage.select("#__VIEWSTATE").val()));
        data.add(new BasicNameValuePair("__VIEWSTATEGENERATOR",
                loginPage.select("#__VIEWSTATEGENERATOR").val()));
        data.add(new BasicNameValuePair("__EVENTVALIDATION",
                loginPage.select("#__EVENTVALIDATION").val()));

        data.add(new BasicNameValuePair("ctl00$ContentPlaceHolderMain$TextBoxLoginName",
                account.getName()));
        data.add(new BasicNameValuePair("ctl00$ContentPlaceHolderMain$TextBoxLoginPassword",
                account.getPassword()));
        data.add(new BasicNameValuePair("ctl00$ContentPlaceHolderMain$ButtonLogin", "Anmelden"));

        String html = httpPost(opac_url + "/user/login.aspx", new UrlEncodedFormEntity(data),
                "UTF-8");
        Document doc = Jsoup.parse(html);
        if (doc.select("#ctl00_ContentPlaceHolderMain_LabelLoginMessage").size() > 0) {
            throw new OpacErrorException(
                    doc.select("#ctl00_ContentPlaceHolderMain_LabelLoginMessage").text());
        }
        return doc;
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
