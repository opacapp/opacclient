package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

import static java.net.URLDecoder.decode;

public class Primo extends ApacheBaseApi {
    protected static HashMap<String, String> languageCodes = new HashMap<>();
    protected static HashMap<String, SearchResult.MediaType> mediaTypeClasses = new HashMap<>();

    static {
        languageCodes.put("en", "en_US");
        languageCodes.put("de", "de_DE");
        languageCodes.put("es", "es_ES");
        languageCodes.put("ru", "ru_RU");
        languageCodes.put("fr", "fr_FR");
        languageCodes.put("nl", "nl_NL");
        languageCodes.put("cz", "cz_CZ");
        languageCodes.put("zh", "zh_ZH");
        languageCodes.put("it", "it_IT");
        languageCodes.put("pl", "pl_PL");

        mediaTypeClasses.put("EXLResultMediaTYPEbook", SearchResult.MediaType.BOOK);
        mediaTypeClasses.put("EXLResultMediaTYPEarticle", SearchResult.MediaType.BOOK);
        mediaTypeClasses.put("EXLResultMediaTYPEjournal", SearchResult.MediaType.MAGAZINE);
        mediaTypeClasses.put("EXLResultMediaTYPEvideo", SearchResult.MediaType.MOVIE);
        mediaTypeClasses.put("EXLResultMediaTYPEaudio", SearchResult.MediaType.CD_MUSIC);
        mediaTypeClasses.put("EXLResultMediaTYPEdatabase", SearchResult.MediaType.EDOC);
        mediaTypeClasses.put("EXLResultMediaTYPEimage", SearchResult.MediaType.ART);
        mediaTypeClasses.put("EXLResultMediaTYPEscore", SearchResult.MediaType.SCORE_MUSIC);
        mediaTypeClasses.put("EXLResultMediaTYPEmap", SearchResult.MediaType.MAP);
        mediaTypeClasses.put("EXLResultMediaTYPEwebsite", SearchResult.MediaType.URL);
        mediaTypeClasses.put("EXLResultMediaTYPEarchived_website", SearchResult.MediaType.URL);
        mediaTypeClasses.put("EXLResultMediaTYPEwork", SearchResult.MediaType.BOOK);
        mediaTypeClasses.put("EXLResultMediaTYPEwebsite", SearchResult.MediaType.URL);
        mediaTypeClasses.put("EXLResultMediaTYPEreference_entry", SearchResult.MediaType.EDOC);
    }

    protected String languageCode = "en_US";
    protected String opac_url = "";
    protected String vid = "";
    protected JSONObject data;
    protected List<SearchQuery> last_query;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);

        this.library = lib;
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            this.vid = data.getString("db");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<NameValuePair> buildSearchParams(List<SearchQuery> query)
            throws IOException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<>();

        String tab = "";
        if (data.has("searchtab")) {
            tab = "&tab=" + data.optString("searchtab", "default_tab");
        }
        String html =
                httpGet(opac_url + "/action/search.do?mode=Advanced&ct=AdvancedSearch&vid=" + vid +
                                tab,
                        getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        for (Element input : doc.select("form[name=searchForm] input[type=hidden]")) {
            if (!input.attr("name").trim().equals("")) {
                params.add(new BasicNameValuePair(input.attr("name"), input.val()));
            }
        }
        params.add(new BasicNameValuePair("fn", "search"));

        int i = 1;
        for (SearchQuery q : query) {
            if (q.getValue().equals("")) {
                continue;
            }
            if (q.getKey().startsWith("#")) {
                params.add(
                        new BasicNameValuePair(doc.select(q.getKey()).attr("name"), q.getValue()));
            } else {
                if (i > 3) {
                    throw new OpacErrorException(stringProvider.getQuantityString(
                            StringProvider.LIMITED_NUM_OF_CRITERIA, 3, 3));
                }
                params.add(
                        new BasicNameValuePair(doc.select("#exlidInput_scope_" + i).attr("name"),
                                q.getKey()));
                params.add(new BasicNameValuePair(
                        doc.select("#input_freeText" + (i - 1)).attr("name"), q.getValue()));
                params.add(new BasicNameValuePair(
                        doc.select("#exlidInput_precisionOperator_" + i).attr("name"),
                        "contains"));
                params.add(new BasicNameValuePair(
                        doc.select("#exlidInput_boolOperator_" + i).attr("name"), "AND"));
                i++;
            }
        }

        return params;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        if (!initialised) start();
        last_query = query;
        String html;
        if (query.size() == 1 && query.get(0).getKey().equals("url")) {
            html = httpGet(query.get(0).getValue(), getDefaultEncoding());
        } else {
            html = httpGet(opac_url + "/action/search.do" +
                            buildHttpGetParams(buildSearchParams(query)),
                    getDefaultEncoding());
        }
        Document doc = Jsoup.parse(html);

        return parse_search(doc, 1);
    }

    protected SearchRequestResult parse_search(Document doc, int page)
            throws OpacErrorException, IOException {
        doc.setBaseUri(opac_url + "/action/search.do");
        List<SearchResult> reslist = new ArrayList<>();

        int resnum = -1;
        if (doc.select(".EXLResultsNumbers").size() > 0) {
            try {
                resnum = Integer.valueOf(
                        doc.select(".EXLResultsNumbers em, .PaginationLabel strong").first().text()
                           .trim().replace(".", "")
                           .replace(",", "").replace(" ", "").replace("Ergebnisse", ""));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        for (Element resrow : doc.select(".EXLResult")) {
            SearchResult res = new SearchResult();
            StringBuilder description = new StringBuilder();
            description.append("<b>").append(resrow.select(".EXLResultTitle").text())
                       .append("</b>");
            if (resrow.select(".EXLResultAuthor").size() > 0) {
                description.append("<br />").append(resrow.select(".EXLResultAuthor").text());
            }
            if (resrow.select(".EXLResultDetails").size() > 0) {
                description.append("<br />").append(resrow.select(".EXLResultDetails").text());
            }
            String availSelect = ".EXLResultAvailability span, .EXLResultAvailability em";
            if (resrow.select(availSelect).size() > 0) {
                description.append("<br />").append(resrow.select(availSelect).first().ownText());
            }
            res.setInnerhtml(description.toString());
            if (resrow.select(".EXLResultStatusAvailable").size() > 0) {
                res.setStatus(SearchResult.Status.GREEN);
            } else if (resrow.select(".EXLResultStatusNotAvailable").size() > 0) {
                res.setStatus(SearchResult.Status.RED);
            } else if (resrow.select(".EXLResultStatusMaybeAvailable").size() > 0) {
                res.setStatus(SearchResult.Status.YELLOW);
            }
            res.setPage(page);

            for (Element a : resrow
                    .select(".EXLResultTitle a, a.EXLThumbnailLinkMarker, .EXLDetailsTab a")) {
                Map<String, String> q = getQueryParamsFirst(a.absUrl("href"));
                if (q.containsKey("doc")) {
                    res.setId(q.get("doc"));
                    break;
                }
            }
            if (res.getId() == null) {
                continue;
            }


            if (resrow.select("img.EXLBriefResultsCover").size() > 0) {
                String src = resrow.select("img.EXLBriefResultsCover").first().absUrl("src");
                if (!src.contains("pixel.png")) {
                    res.setCover(src);
                }
            }

            for (Map.Entry<String, SearchResult.MediaType> cls : mediaTypeClasses.entrySet()) {
                if (resrow.hasClass(cls.getKey()) || resrow.select("." + cls.getKey()).size() > 0) {
                    res.setType(cls.getValue());
                    break;
                }
            }

            if (resrow.select("a.EXLBriefResultsDisplayMultipleLink").size() > 0) {
                String url = resrow.select("a.EXLBriefResultsDisplayMultipleLink").first()
                                   .absUrl("href");
                List<SearchQuery> query = new ArrayList<>();
                TextSearchField field =
                        new TextSearchField("url", "url", false, false, "url", false, false);
                field.setVisible(false);
                query.add(new SearchQuery(field, url));
                res.setChildQuery(query);
            }


            reslist.add(res);
        }

        return new SearchRequestResult(reslist, resnum, page);
    }

    public static Map<String, String> getQueryParamsFirst(String url) {
        try {
            Map<String, String> params = new HashMap<>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = decode(pair[1], "UTF-8");
                    }

                    String values = params.get(key);
                    if (values == null) {
                        params.put(key, value);
                    }
                }
            }

            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        List<NameValuePair> params = buildSearchParams(last_query);
        params.add(new BasicNameValuePair("indx", String.valueOf(((page - 1) * 10) + 1)));
        params.add(new BasicNameValuePair("pag", "cur"));

        String html = httpGet(opac_url + "/action/search.do" +
                        buildHttpGetParams(params),
                getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        return parse_search(doc, page);
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        if (!initialised) start();
        String html =
                httpGet(opac_url + "/action/display.do?ct=display&fn=search&vid=" + vid + "&doc=" +
                                id + "&tabs=detailsTab",
                        getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        return parse_detail(id, doc);
    }

    protected DetailedItem parse_detail(String id, Document doc)
            throws OpacErrorException, IOException {
        DetailedItem res = new DetailedItem();
        res.setId(id);

        res.setTitle(doc.select(".EXLResultTitle").text());

        for (Element detrow : doc.select(".EXLDetailsContent li")) {
            String title = null;
            String value = "";
            for (Node node : detrow.childNodes()) {
                if (node instanceof Element && (((Element) node).tagName().equals("strong") ||
                        ((Element) node).hasClass("bib-EXLDetailsContent-item-title"))) {
                    title = ((Element) node).text();
                } else if (node instanceof Element && title != null) {
                    value += ((Element) node).text();
                } else if (node instanceof TextNode && title != null) {
                    value += ((TextNode) node).text();
                }
            }
            if (title != null) {
                res.addDetail(new Detail(title, value.trim()));
            }
        }

        String html2 =
                httpGet(opac_url + "/action/display.do?ct=display&fn=search&vid=" + vid + "&doc=" +
                                id + "&tabs=locationsTab",
                        getDefaultEncoding());
        Document doc2 = Jsoup.parse(html2);

        if (doc2.select(".EXLLocationTitlesRow").size() > 0) {
            Map<Integer, String> copymap = new HashMap<>();
            int i = 0;
            for (Element th : doc2.select(".EXLLocationTitlesRow th")) {
                String title = th.text().toLowerCase(Locale.GERMAN).trim();
                if (title.contains("library") || title.contains("bibliothek") ||
                        title.contains("branch")) {
                    copymap.put(i, "branch");
                } else if (title.contains("location") || title.contains("ort")) {
                    copymap.put(i, "location");
                } else if (title.contains("call number") || title.contains("signatur")) {
                    copymap.put(i, "signature");
                } else if (title.contains("due date") || title.contains("llig am") ||
                        title.contains("ausgeliehen bis") || title.contains("lligkeit")
                        || title.contains("ausleihstatus")) {
                    copymap.put(i, "returndate");
                } else if (title.contains("loan to") || title.contains("bezugsmodalit") ||
                        title.contains("ausleihm") || title.contains("status")) {
                    copymap.put(i, "status");
                } else if (title.contains("queue") || title.contains("vormerker")) {
                    copymap.put(i, "reservations");
                }
                i++;
            }

            DateTimeFormatter fmt =
                    DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.GERMAN);
            DateTimeFormatter fmt2 =
                    DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.GERMAN);

            for (Element tr : doc2
                    .select(".EXLLocationTable tr:not(.EXLLocationTitlesRow):not(" +
                            ".EXLAdditionalFieldsRow)")) {
                int j = 0;
                Copy copy = new Copy();
                for (Element td : tr.children()) {
                    String value = td.text().replace("\u00a0", " ").trim();
                    if (copymap.containsKey(j) && !value.equals("")) {
                        try {
                            copy.set(copymap.get(j), value, fmt);
                        } catch (IllegalArgumentException e) {
                            try {
                                copy.set(copymap.get(j), value, fmt2);
                            } catch (IllegalArgumentException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                    j++;
                }
                res.addCopy(copy);
            }
        }

        if (res.getCopies().size() == 0) {
            // Online-Medium?
            String html3 =
                    httpGet(opac_url + "/action/display.do?ct=display&fn=search&vid=" + vid +
                                    "&doc=" +
                                    id + "&tabs=viewOnlineTab",
                            getDefaultEncoding());
            Document doc3 = Jsoup.parse(html3);
            doc3.setBaseUri(opac_url + "/action/display.do");
            if (doc3.select(".EXLTabHeaderContent a").size() > 0) {
                Element link = doc3.select(".EXLTabHeaderContent a").first();
                res.addDetail(new Detail(link.text().trim(), cleanUrl(link.absUrl("href"))));
            }
            for (Element link : doc3.select(".EXLViewOnlineLinksTitle a")) {
                res.addDetail(new Detail(link.text().trim(), cleanUrl(link.absUrl("href"))));
            }
        }

        return res;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException, OpacErrorException {
        return null;
    }

    public void start() throws IOException {
        super.start();
        httpGet(opac_url + "/action/preferences.do?fn=change_lang&vid=" + vid + "&prefLang=" +
                        languageCode,
                getDefaultEncoding());
    }

    @Override
    public List<SearchField> parseSearchFields()
            throws IOException, OpacErrorException, JSONException {
        start();
        String html =
                httpGet(opac_url + "/action/search.do?mode=Advanced&ct=AdvancedSearch&vid=" + vid,
                        getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        List<SearchField> fields = new ArrayList<>();

        if (doc.select("select#exlidInput_scope_1").size() < 1) {
            throw new NotReachableException();
        }
        Elements options = doc.select("select#exlidInput_scope_1").first().select("option");
        for (Element option : options) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(option.text());
            field.setId(option.val());
            field.setHint("");
            field.setData(new JSONObject());
            field.getData().put("meaning", option.val());
            fields.add(field);
        }
        if (fields.size() == 0) {
            // Weird JavaScript, e.g. view-source:http://vopac.nlg.gr/Search/Advanced
            Pattern pattern_key = Pattern
                    .compile("searchFields\\[\"([^\"]+)\"\\] = \"([^\"]+)\";");
            for (Element script : doc.select("script")) {
                if (!script.html().contains("searchFields")) continue;
                for (String line : script.html().split("\n")) {
                    Matcher matcher = pattern_key.matcher(line);
                    if (matcher.find()) {
                        TextSearchField field = new TextSearchField();
                        field.setDisplayName(matcher.group(2));
                        field.setId(matcher.group(1));
                        field.setHint("");
                        field.setData(new JSONObject());
                        field.getData().put("meaning", field.getId());
                        fields.add(field);
                    }
                }
            }
        }

        Elements selects = doc.select("#exlidInput_mediaType_, #exlidInput_publicationDate_, " +
                "#exlidInput_language_, #exlidSearchIn");
        for (Element select : selects) {
            DropdownSearchField field = new DropdownSearchField();
            if (select.parent().select("label").size() > 0) {
                field.setDisplayName(select.parent().select("label").first()
                                           .text());
            } else {
                continue;
            }
            field.setId("#" + select.attr("id"));
            for (Element option : select.select("option")) {
                if (option.val().equals("all_items")) {
                    field.addDropdownValue(0, option.val(), option.text());
                } else {
                    field.addDropdownValue(option.val(), option.text());
                }
            }
            field.setData(new JSONObject());
            field.getData().put("meaning", field.getId());
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return opac_url + "/action/display.do?ct=display&fn=search&vid=" + vid + "&doc=" +
                id + "&tabs=detailsTab";
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        // Not that easy, as some libaries do and some don't include the
        // default language in their language chooser. We go for configuration flags in the
        // meantime.
        // With full chooser: http://primo.kobv.de/primo_library/libweb/action/search.do?vid=FUB
        // With toggle only: http://primo.kobv.de/primo_library/libweb/action/search.do?vid=hub_ub
        // Without any chooser: http://explore.bl.uk/primo_library/libweb/action/search.do?vid=BLVU1
        if (data.has("languages")) {
            try {
                for (int i = 0; i < data.getJSONArray("languages").length(); i++) {
                    langs.add(data.getJSONArray("languages").getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            langs.add("de");
            langs.add("en");
            langs.add("it");
            langs.add("zh");
            langs.add("cz");
            langs.add("ru");
            langs.add("nl");
            langs.add("es");
            langs.add("fr");
        }
        return langs;
    }

    protected String getDefaultEncoding() {
        return "UTF-8";
    }

    @Override
    public void setLanguage(String language) {
        if (data.has("languages")) {
            try {
                for (int i = 0; i < data.getJSONArray("languages").length(); i++) {
                    if (data.getJSONArray("languages").getString(i).equals(language)) {
                        languageCode =
                                languageCodes.containsKey(language) ? languageCodes.get(language) :
                                        language;
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            languageCode =
                    languageCodes.containsKey(language) ? languageCodes.get(language) : language;
        }
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
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        return null;
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {
    }

    public String httpGet(String url, String encoding, boolean ignore_errors,
            CookieStore cookieStore) throws
            IOException {
        String html = super.httpGet(url, encoding, ignore_errors, cookieStore);
        if (html.contains("id=\"connect\"")) {
            // British Library and Uni Duesburg-Essen do weird JavaScript redirects
            Document doc = Jsoup.parse(html);
            doc.setBaseUri(url);
            return httpGet(doc.select("#connect a").first().attr("href").substring(6), encoding,
                    ignore_errors, cookieStore);
        }
        return html;
    }
}
