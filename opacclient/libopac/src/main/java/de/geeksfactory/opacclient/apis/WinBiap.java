package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
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
import okhttp3.FormBody;

//@formatter:off

/**
 * @author Johan von Forstner, 11.08.2014
 *
 *         WinBIAP, Version 4.1.0 gestartet mit Bibliothek Unterföhring
 *
 *         Unterstützt bisher nur Katalogsuche
 *
 *         Example for a search query (parameter "data" in the URL, everything before the hyphen,
 *         base64 decoded, added formatting) as seen in Unterföhring:
 *
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
 *
 *         &amp;Sort=Autor				Sort by Author (default)
 */
//@formatter:on

public class WinBiap extends OkHttpBaseApi implements OpacApi {

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
        defaulttypes.put("buch", SearchResult.MediaType.BOOK);
        defaulttypes.put("hoerbuch", SearchResult.MediaType.AUDIOBOOK);
        defaulttypes.put("musik", SearchResult.MediaType.CD_MUSIC);
        defaulttypes.put("cdrom", SearchResult.MediaType.CD_SOFTWARE);
        defaulttypes.put("dvd", SearchResult.MediaType.DVD);
        defaulttypes.put("online", SearchResult.MediaType.EBOOK);
        defaulttypes.put("konsole", SearchResult.MediaType.GAME_CONSOLE);
        defaulttypes.put("zschrift", SearchResult.MediaType.MAGAZINE);
    }

    protected String opac_url = "";
    protected JSONObject data;
    protected List<List<NameValuePair>> query;

    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);
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

        String html = httpGet(opac_url + "/search.aspx?data=" + base64, getDefaultEncoding(),
                false);
        return parse_search(html, page);
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        String html = httpGet(opac_url + "/detail.aspx?Id=" + id, getDefaultEncoding(), false);
        return parse_result(html);
    }

    private DetailedItem parse_result(String html) {
        Document doc = Jsoup.parse(html);
        DetailedItem item = new DetailedItem();

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

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        trs = doc.select(".detailCopies .tableCopies > tbody > tr:not(.headerCopies)");
        for (Element tr : trs) {
            Copy copy = new Copy();
            copy.setBarcode(tr.select(".mediaBarcode").text().replace("#", ""));
            copy.setStatus(tr.select(".mediaStatus").text());
            if (tr.select(".DateofReturn .borrowUntil").size() > 0) {
                String returntime = tr.select(".DateofReturn .borrowUntil").text();
                try {
                    copy.setReturnDate(fmt.parseLocalDate(returntime));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

            }
            if (tr.select(".mediaBranch").size() > 0) {
                copy.setBranch(tr.select(".mediaBranch").text());
            }
            copy.setLocation(tr.select(".cellMediaItemLocation span").text());
            if (tr.select("#HyperLinkReservation").size() > 0) {
                copy.setResInfo(tr.select("#HyperLinkReservation").attr("href"));
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
    public DetailedItem getResult(int position) throws IOException,
            OpacErrorException {
        // Should not be called because every media has an ID
        return null;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        if (selection == null) {
            // Which copy?
            List<Map<String, String>> options = new ArrayList<>();
            for (Copy copy : item.getCopies()) {
                if (copy.getResInfo() == null) continue;

                Map<String, String> option = new HashMap<>();
                option.put("key", copy.getResInfo());
                option.put("value", copy.getBarcode() + " - "
                        + copy.getBranch() + " - "
                        + copy.getReturnDate());
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
            // the URL stored in selection might be absolute (WinBiap 4.3) or relative (4.2)
            String reservationUrl = new URL(new URL(opac_url), selection).toString();
            // the URL stored in selection contains "=" and other things inside params
            // and will be messed up by our cleanUrl function, therefore we use a direct HttpGet
            Document doc = Jsoup.parse(httpGet(reservationUrl, getDefaultEncoding()));
            if (doc.select("[id$=LabelLoginMessage]").size() > 0) {
                doc.select("[id$=TextBoxLoginName]").val(account.getName());
                doc.select("[id$=TextBoxLoginPassword]").val(account.getPassword());
                FormElement form = (FormElement) doc.select("form").first();

                List<Connection.KeyVal> formData = form.formData();
                FormBody.Builder paramBuilder = new FormBody.Builder();
                for (Connection.KeyVal kv : formData) {
                    if (!kv.key().contains("Button") || kv.key().endsWith("ButtonLogin")) {
                        paramBuilder.add(kv.key(), kv.value());
                    }
                }
                doc = Jsoup.parse(httpPost(opac_url + "/user/" + form.attr("action"),
                        paramBuilder.build(), getDefaultEncoding()));
            }
            FormElement confirmationForm = (FormElement) doc.select("form").first();
            List<Connection.KeyVal> formData = confirmationForm.formData();
            FormBody.Builder paramBuilder = new FormBody.Builder();
            for (Connection.KeyVal kv : formData) {
                if (!kv.key().contains("Button") || kv.key().endsWith("ButtonVorbestOk")) {
                    paramBuilder.add(kv.key(), kv.value());
                }
            }
            httpPost(opac_url + "/user/" + confirmationForm.attr("action"),
                    paramBuilder.build(), getDefaultEncoding());

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
        FormBody.Builder paramBuilder = new FormBody.Builder();
        for (Connection.KeyVal kv : formData) {
            paramBuilder.add(kv.key(), kv.value());
        }

        if (lentPage.select("a[id$=ButtonBorrowChecked][href^=javascript]").size() > 0) {
            String href =
                    lentPage.select("a[id$=ButtonBorrowChecked][href^=javascript]").attr("href");
            Pattern pattern = Pattern.compile("javascript:__doPostBack\\('([^,]*)','([^\\)]*)'\\)");
            Matcher matcher = pattern.matcher(href);
            if (!matcher.find()) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        StringProvider.INTERNAL_ERROR);
            }
            paramBuilder.add("__EVENTTARGET", matcher.group(1));
            paramBuilder.add("__EVENTARGUMENT", matcher.group(2));
        }

        String html = httpPost(opac_url + "/user/borrow.aspx", paramBuilder.build(), getDefaultEncoding());
        Document confirmationPage = Jsoup.parse(html);

        FormElement confirmationForm = (FormElement) confirmationPage.select("form").first();
        List<Connection.KeyVal> formData2 = confirmationForm.formData();
        FormBody.Builder params2 = new FormBody.Builder();
        for (Connection.KeyVal kv : formData2) {
            if (!kv.key().contains("Button") || kv.key().endsWith("ButtonProlongationOk")) {
                params2.add(kv.key(), kv.value());
            }
        }
        httpPost(opac_url + "/user/" + confirmationForm.attr("action"),
                params2.build(), getDefaultEncoding());

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
        FormBody.Builder params = new FormBody.Builder();
        params.add("action", "reservationdelete");
        params.add("data", media);
        String response = httpPost(opac_url + "/service/UserService.ashx",
                params.build(), getDefaultEncoding());
        if (response.startsWith("{")) {
            // new system (starting with 4.4.0?): JSON response
            // e.g. {"Success":true,"Count":0} (Count = number of remaining reservations)
            try {
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.optBoolean("Success")) {
                    return new CancelResult(MultiStepResult.Status.OK);
                } else {
                    return new CancelResult(MultiStepResult.Status.ERROR,
                            stringProvider.getString(StringProvider.UNKNOWN_ERROR));
                }
            } catch (JSONException e) {
                return new CancelResult(MultiStepResult.Status.ERROR,
                        stringProvider.getString(StringProvider.INTERNAL_ERROR));
            }
        } else {
            // Old system
            // Response: [number of reservations deleted];[number of remaining reservations]
            String[] parts = response.split(";");
            if (parts[0].equals("1")) {
                return new CancelResult(MultiStepResult.Status.OK);
            } else {
                return new CancelResult(MultiStepResult.Status.ERROR,
                        stringProvider.getString(StringProvider.UNKNOWN_ERROR));
            }
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

        String lentUrl = opac_url + "/user/borrow.aspx";
        Document lentPage = Jsoup.parse(httpGet(lentUrl, getDefaultEncoding()));
        lentPage.setBaseUri(lentUrl);
        adata.setLent(parseMediaList(lentPage, data));

        String resUrl = opac_url + "/user/reservations.aspx";
        Document reservationsPage = Jsoup.parse(httpGet(resUrl, getDefaultEncoding()));
        reservationsPage.setBaseUri(resUrl);
        adata.setReservations(parseResList(reservationsPage, stringProvider, data));


        return adata;
    }

    static List<LentItem> parseMediaList(Document doc, JSONObject data) {
        List<LentItem> lent = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);

        // the account page differs between WinBiap versions 4.2 and >= 4.3
        boolean winBiap43;
        if (doc.select(".GridView_RowStyle").size() > 0) {
            winBiap43 = false;
        } else {
            winBiap43 = true;
        }

        // 4.2: .GridView_RowStyle
        // 4.3: id=...DetailItemMain_rowBorrow
        // 4.4: id=...DetailItemMain_0_rowBorrow_0
        for (Element tr : doc.select(winBiap43 ? ".detailTable tr[id*=_rowBorrow]" :
                ".GridView_RowStyle")) {
            LentItem item = new LentItem();
            Element detailsTr = winBiap43 ? tr.nextElementSibling() : tr;

            // the second column contains an img tag with the cover
            if (detailsTr.select(".cover, img[id*=ImageCover]").size() > 0) {
                // find media ID using cover URL
                Element cover = detailsTr.select(".cover, img[id*=ImageCover]").first();
                String src = cover.attr("abs:data-src");
                if (src.equals("")) src = cover.attr("abs:src");
                Map<String, String> params = getQueryParamsFirst(src);
                if (params.containsKey("catid")) item.setId(params.get("catid"));

                // find media type
                SearchResult.MediaType mt = getMediaType(cover, data);
                item.setMediaType(mt);

                // set cover if it's not the media type image
                if (!src.equals(cover.attr("grp"))) item.setCover(src);
            }

            item.setAuthor(nullIfEmpty(tr.select("[id$=LabelAutor]").text()));
            item.setTitle(
                    nullIfEmpty(tr.select("[id$=LabelTitel], [id$=LabelTitle], .title").text()));
            item.setBarcode(nullIfEmpty(detailsTr
                    .select("[id$=Label_Mediennr], [id$=labelMediennr], [id*=labelMediennr_]")
                    .text()));
            item.setFormat(nullIfEmpty(detailsTr
                    .select("[id$=Label_Mediengruppe], [id$=labelMediagroup], " +
                            "[id*=labelMediagroup_]")
                    .text()));
            item.setHomeBranch(nullIfEmpty(detailsTr
                    .select("[id$=Label_Zweigstelle], [id$=labelBranch], [id*=labelBranch_]")
                    .text()));
            // Label_Entliehen contains the date when the medium was lent
            try {
                item.setDeadline(fmt.parseLocalDate(tr.select("[id$=LabelFaellig], [id$=LabelMatureDate], .matureDate").text()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            if (tr.select("input[id*=_chkSelect]").size() > 0) {
                item.setProlongData(tr.select("input[id*=_chkSelect]").attr("name"));
            } else {
                item.setRenewable(false);
            }
            lent.add(item);
        }

        return lent;
    }

    private static String nullIfEmpty(String text) {
        if (text == null || text.equals("")) {
            return null;
        } else {
            return text;
        }
    }

    static List<ReservedItem> parseResList(Document doc, StringProvider stringProvider,
            JSONObject data) {
        List<ReservedItem> reservations = new ArrayList<>();

        // the account page differs between WinBiap versions 4.2 and 4.3
        boolean winBiap43;
        if (doc.select("tr[id*=GridViewReservation]").size() > 0) {
            winBiap43 = false;
        } else {
            winBiap43 = true;
        }

        // 4.2: id=...GridViewReservation_ct...
        // 4.3: id=...DetailItemMain_rowBorrow
        // 4.4: id=...DetailItemMain_0_rowBorrow_0
        for (Element tr : doc
                .select(winBiap43 ? ".detailTable tr[id*=_rowBorrow]" :
                        "tr[id*=GridViewReservation]")) {
            ReservedItem item = new ReservedItem();

            Element detailsTr = winBiap43 ? tr.nextElementSibling() : tr;

            // the second column contains an img tag with the cover
            if (detailsTr.select(".cover, img[id*=ImageCover]").size() > 0) {
                // find media ID using cover URL
                Element cover = detailsTr.select(".cover, img[id*=ImageCover]").first();
                String src = cover.attr("abs:data-src");
                if (src.equals("")) src = cover.attr("abs:src");
                Map<String, String> params = getQueryParamsFirst(src);
                if (params.containsKey("catid")) item.setId(params.get("catid"));

                // find media type
                SearchResult.MediaType mt = getMediaType(cover, data);
                if (mt != null) {
                    item.setFormat(stringProvider.getMediaTypeName(mt));
                    item.setMediaType(mt);
                }

                // set cover if it's not the media type image
                if (!src.equals(cover.attr("grp"))) item.setCover(src);
            }

            item.setStatus(nullIfEmpty(winBiap43 ? detailsTr.select("[id$=labelStatus], [id*=labelStatus_]").text() :
                    tr.select("[id$=ImageBorrow]").attr("title")));
            item.setAuthor(nullIfEmpty(tr.select("[id$=LabelAutor], .autor").text()));
            item.setTitle(nullIfEmpty(tr.select("[id$=LabelTitle], .title").text()));
            item.setBranch(nullIfEmpty(
                    detailsTr.select("[id$=LabelBranch], [id$=labelBranch], [id*=labelBranch_]")
                             .text()));
            item.setFormat(nullIfEmpty(detailsTr
                    .select("[id$=Label_Mediengruppe], [id$=labelMediagroup], [id*=labelMediagroup_]")
                    .text()));
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
                    item.setCancelData(base64);
                }
            } else if (detailsTr.select("input[id*=_hiddenValueDetail][value]").size() > 0) {
                item.setCancelData(
                        detailsTr.select("input[id*=_hiddenValueDetail][value]").attr("value"));
            }
            reservations.add(item);
        }

        return reservations;
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException {
        // extract branches and categories
        String html = httpGet(opac_url + "/search.aspx", getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        Elements mediaGroupOptions = doc
                .select("[id$=ListBoxMediagroups_ListBoxMultiselection] option");
        Elements branchOptions = doc
                .select("[id$=MultiSelectionBranch_ListBoxMultiselection] option");

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
        FormBody.Builder data = new FormBody.Builder();

        String formAction = loginPage.select("form").attr("action");
        boolean homePage = formAction.endsWith("index.aspx");

        /* pass all input fields beginning with two underscores to login url */
        Elements inputFields = loginPage.select("input[id^=__]");
        for (Element inputField : inputFields) {
            data.add(inputField.attr("name"), inputField.val());
        }

        // Some WinBiap 4.4 installations (such as Neufahrn) redirect user/login.aspx to index.aspx
        // This page then also has a login form, but it has different text box IDs:
        // TextBoxLoginName -> TextBoxUsername and TextBoxLoginPassword -> TextBoxPassword

        data.add(loginPage.select("input[id$=TextBoxLoginName], input[id$=TextBoxUsername]")
                         .attr("name"), account.getName());
        data.add(loginPage.select("input[id$=TextBoxLoginPassword], input[id$=TextBoxPassword]")
                         .attr("name"), account.getPassword());
        data.add(loginPage.select("input[id$=ButtonLogin]").attr("name"),
                "Anmelden");

        // We also need to POST our data to the correct page.
        String postUrl = opac_url + (homePage ? "/index.aspx" : "/user/login.aspx");
        String html = httpPost(postUrl, data.build(), "UTF-8");
        Document doc = Jsoup.parse(html);
        handleLoginErrors(doc);
        return doc;
    }

    static void handleLoginErrors(Document doc) throws OpacErrorException {
        String errorSelector = "span[id$=LabelLoginMessage]";
        if (doc.select(errorSelector).size() > 0) {
            throw new OpacErrorException(doc.select(errorSelector).text());
        }
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
